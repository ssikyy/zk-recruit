package com.zkteco.recruit.service.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.zkteco.recruit.common.BizException;
import com.zkteco.recruit.common.ErrorCode;
import com.zkteco.recruit.config.AppProperties;

/**
 * 简历附件的本地磁盘存储（D7）。
 * <p>
 * 系统中不存在任何公开可访问的文件目录：简历只能通过鉴权接口下载，
 * 数据库只保存相对 storageKey，任何对外响应都不得暴露该值。
 * 首页配图属于前端静态资源，不经过本服务。
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private static final List<String> RESUME_EXTENSIONS = List.of("pdf", "doc", "docx");

    private final AppProperties appProperties;
    private final Path root;

    public StorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.root = Paths.get(appProperties.getStorage().getRoot()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root.resolve(appProperties.getStorage().getResumeDir()));
        } catch (IOException e) {
            throw new IllegalStateException("初始化存储目录失败: " + root, e);
        }
        log.info("文件存储根目录: {}", root);
    }

    /* ---------------- 简历附件（鉴权访问） ---------------- */

    public StoredFile saveResume(Long candidateId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请选择要上传的文件");
        }
        if (file.getSize() > appProperties.getStorage().getResumeMaxSize()) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE, "附件简历不能超过 10MB");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!RESUME_EXTENSIONS.contains(ext)) {
            throw new BizException(ErrorCode.FILE_TYPE_UNSUPPORTED, "仅支持 PDF、DOC、DOCX 格式");
        }
        byte[] head = readHead(file);
        if (!matchesResumeMagic(ext, head)) {
            throw new BizException(ErrorCode.FILE_TYPE_UNSUPPORTED, "文件实际类型与扩展名不一致，已拒绝");
        }
        String key = appProperties.getStorage().getResumeDir() + "/" + candidateId + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        write(file, key);
        return new StoredFile(key, safeFileName(file.getOriginalFilename()), file.getSize(),
                file.getContentType() == null ? "application/octet-stream" : file.getContentType());
    }

    /* ---------------- 读取与删除 ---------------- */

    /**
     * 把 storageKey 解析成绝对路径，并强制限制在存储根目录内，防止目录穿越。
     */
    public Path resolve(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new BizException(ErrorCode.FORBIDDEN, "非法的文件路径");
        }
        return target;
    }

    public boolean exists(String storageKey) {
        return Files.exists(resolve(storageKey));
    }

    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            log.warn("删除文件失败 key={} err={}", storageKey, e.getMessage());
        }
    }

    /* ---------------- 内部方法 ---------------- */

    private void write(MultipartFile file, String key) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_IO_ERROR, "文件写入失败: " + e.getMessage());
        }
    }

    private byte[] readHead(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = new byte[16];
            int read = in.read(head);
            return read <= 0 ? new byte[0] : Arrays.copyOf(head, read);
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_IO_ERROR, "文件读取失败");
        }
    }

    private boolean matchesResumeMagic(String ext, byte[] head) {
        return switch (ext) {
            // %PDF
            case "pdf" -> startsWith(head, new int[]{0x25, 0x50, 0x44, 0x46});
            // OLE2 复合文档
            case "doc" -> startsWith(head, new int[]{0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1});
            // docx 实为 zip
            case "docx" -> startsWith(head, new int[]{0x50, 0x4B, 0x03, 0x04});
            default -> false;
        };
    }

    private boolean startsWith(byte[] data, int[] expected) {
        if (data.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((data[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 去掉路径分隔符与穿越字符，只保留文件名本身。
     */
    private String safeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "unnamed";
        }
        String name = original.replace("\\", "/");
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\r\\n\"]", "").replace("..", "");
        return name.isBlank() ? "unnamed" : name;
    }

    /**
     * 存储结果，供上层写入数据库。
     */
    public record StoredFile(String storageKey, String fileName, long size, String contentType) {
    }
}
