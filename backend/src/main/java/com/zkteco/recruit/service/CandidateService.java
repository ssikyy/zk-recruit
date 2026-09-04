package com.zkteco.recruit.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.zkteco.recruit.common.BizException;
import com.zkteco.recruit.common.ErrorCode;
import com.zkteco.recruit.common.JsonUtils;
import com.zkteco.recruit.config.AppProperties;
import com.zkteco.recruit.domain.entity.CandidateProfile;
import com.zkteco.recruit.domain.entity.Resume;
import com.zkteco.recruit.domain.entity.ResumeFile;
import com.zkteco.recruit.domain.entity.SysUser;
import com.zkteco.recruit.dto.candidate.ProfileRequest;
import com.zkteco.recruit.mapper.CandidateProfileMapper;
import com.zkteco.recruit.mapper.ResumeFileMapper;
import com.zkteco.recruit.mapper.ResumeMapper;
import com.zkteco.recruit.mapper.SysUserMapper;
import com.zkteco.recruit.security.RateLimiter;
import com.zkteco.recruit.service.support.HtmlSanitizer;
import com.zkteco.recruit.service.support.StorageService;

/**
 * 求职者基本资料、在线简历与附件简历（§9.1–§9.3、§9.5）。
 */
@Service
public class CandidateService {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final int MAX_LIST_ITEMS = 10;

    private final CandidateProfileMapper profileMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final SysUserMapper userMapper;
    private final StorageService storageService;
    private final RateLimiter rateLimiter;
    private final AppProperties appProperties;

    public CandidateService(CandidateProfileMapper profileMapper,
                            ResumeMapper resumeMapper,
                            ResumeFileMapper resumeFileMapper,
                            SysUserMapper userMapper,
                            StorageService storageService,
                            RateLimiter rateLimiter,
                            AppProperties appProperties) {
        this.profileMapper = profileMapper;
        this.resumeMapper = resumeMapper;
        this.resumeFileMapper = resumeFileMapper;
        this.userMapper = userMapper;
        this.storageService = storageService;
        this.rateLimiter = rateLimiter;
        this.appProperties = appProperties;
    }

    /* ---------------- 基本资料 ---------------- */

    public Map<String, Object> profile(Long candidateId) {
        CandidateProfile profile = requireProfile(candidateId);
        SysUser user = userMapper.selectById(candidateId);
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("name", profile.getName());
        vo.put("email", user == null ? null : user.getEmail());
        vo.put("phone", profile.getPhone());
        vo.put("gender", profile.getGender());
        vo.put("city", profile.getCity());
        return vo;
    }

    @Transactional
    public void updateProfile(Long candidateId, ProfileRequest request) {
        CandidateProfile profile = requireProfile(candidateId);
        SysUser user = userMapper.selectById(candidateId);
        if (user == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "账号不存在");
        }

        String email = request.getEmail().trim().toLowerCase();
        SysUser sameEmail = userMapper.findByEmail(email);
        if (sameEmail != null && !sameEmail.getId().equals(candidateId)) {
            throw BizException.of(ErrorCode.EMAIL_DUPLICATED);
        }

        String name = HtmlSanitizer.plainText(request.getName().trim());
        profile.setName(name);
        profile.setPhone(emptyToNull(request.getPhone()));
        profile.setGender(emptyToNull(request.getGender()));
        profile.setCity(HtmlSanitizer.plainText(emptyToNull(request.getCity())));
        profileMapper.updateById(profile);

        // 显示名与基本资料姓名保持一致，避免两处数据分叉
        user.setName(name);
        user.setEmail(email);
        userMapper.updateById(user);
    }

    /* ---------------- 在线简历 ---------------- */

    public Map<String, Object> resume(Long candidateId) {
        Resume resume = resumeMapper.findByCandidateId(candidateId);
        Map<String, Object> content = resume == null ? new LinkedHashMap<>() : JsonUtils.parseMap(resume.getContent());
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("content", content);
        vo.put("complete", onlineResumeComplete(content));
        vo.put("updatedAt", resume == null ? null : resume.getUpdatedAt());
        return vo;
    }

    @Transactional
    public void saveResume(Long candidateId, Map<String, Object> rawContent) {
        Map<String, Object> normalized = normalizeResume(rawContent);
        Resume resume = resumeMapper.findByCandidateId(candidateId);
        if (resume == null) {
            resume = new Resume();
            resume.setCandidateId(candidateId);
            resume.setContent(JsonUtils.toJson(normalized));
            resumeMapper.insert(resume);
        } else {
            resume.setContent(JsonUtils.toJson(normalized));
            resumeMapper.updateById(resume);
        }
    }

    /**
     * 在线简历完成度判定（§9.2），投递前置校验的唯一标准之一。
     */
    @SuppressWarnings("unchecked")
    public boolean onlineResumeComplete(Map<String, Object> content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        Object intentionNode = content.get("intention");
        if (!(intentionNode instanceof Map<?, ?> intentionMap)) {
            return false;
        }
        Map<String, Object> intention = (Map<String, Object>) intentionMap;
        boolean hasCategory = notBlank(intention.get("expectCategory"));
        boolean hasCity = notBlank(intention.get("expectCity"));

        Object educations = content.get("educations");
        boolean hasEducation = educations instanceof List<?> list && !list.isEmpty();

        return hasCategory && hasCity && hasEducation;
    }

    /* ---------------- 附件简历 ---------------- */

    public Map<String, Object> currentResumeFile(Long candidateId) {
        ResumeFile file = resumeFileMapper.findCurrent(candidateId);
        if (file == null) {
            return null;
        }
        return resumeFileVo(file);
    }

    @Transactional
    public Map<String, Object> uploadResumeFile(Long candidateId, MultipartFile multipartFile) {
        rateLimiter.assertHourlyLimit("resume-upload", String.valueOf(candidateId),
                appProperties.getSecurity().getResumeUploadLimitPerHour(),
                "简历上传过于频繁，请稍后再试");

        StorageService.StoredFile stored = storageService.saveResume(candidateId, multipartFile);
        // 旧记录只把 is_current 置 0，文件与记录都保留，保证历史投递快照可用（§9.3）
        resumeFileMapper.clearCurrent(candidateId);

        ResumeFile file = new ResumeFile();
        file.setCandidateId(candidateId);
        file.setFileName(stored.fileName());
        file.setStorageKey(stored.storageKey());
        file.setFileSize(stored.size());
        file.setContentType(stored.contentType());
        file.setIsCurrent(true);
        resumeFileMapper.insert(file);
        return resumeFileVo(file);
    }

    /**
     * 取消当前附件：逻辑取消，不删除已被投递引用的文件实体（§9.3）。
     */
    @Transactional
    public void clearCurrentResumeFile(Long candidateId) {
        ResumeFile file = resumeFileMapper.findCurrent(candidateId);
        if (file == null) {
            return;
        }
        resumeFileMapper.clearCurrent(candidateId);
        if (resumeFileMapper.countReferences(file.getId()) == 0) {
            resumeFileMapper.deleteById(file.getId());
            storageService.delete(file.getStorageKey());
        }
    }

    public ResumeFile requireOwnedFile(Long candidateId, Long fileId) {
        ResumeFile file = resumeFileMapper.selectById(fileId);
        if (file == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "附件不存在");
        }
        if (!file.getCandidateId().equals(candidateId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权访问该附件");
        }
        return file;
    }

    public ResumeFile findFile(Long fileId) {
        return fileId == null ? null : resumeFileMapper.selectById(fileId);
    }

    /* ---------------- 投递前置校验（§9.5） ---------------- */

    public EligibilityResult eligibility(Long candidateId) {
        CandidateProfile profile = profileMapper.findByUserId(candidateId);
        List<String> missing = new ArrayList<>();

        boolean nameOk = profile != null && notBlank(profile.getName());
        boolean phoneOk = profile != null && profile.getPhone() != null && PHONE.matcher(profile.getPhone()).matches();
        if (!nameOk) {
            missing.add("NAME");
        }
        if (!phoneOk) {
            missing.add("PHONE");
        }

        boolean hasFile = resumeFileMapper.findCurrent(candidateId) != null;
        Resume resume = resumeMapper.findByCandidateId(candidateId);
        boolean onlineOk = resume != null && onlineResumeComplete(JsonUtils.parseMap(resume.getContent()));
        if (!hasFile && !onlineOk) {
            missing.add("RESUME");
        }

        boolean profileComplete = nameOk && phoneOk;
        boolean canApply = profileComplete && (hasFile || onlineOk);
        return new EligibilityResult(canApply, missing, hasFile, onlineOk);
    }

    public CandidateProfile requireProfile(Long candidateId) {
        CandidateProfile profile = profileMapper.findByUserId(candidateId);
        if (profile == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "求职者资料不存在");
        }
        return profile;
    }

    public Resume findResume(Long candidateId) {
        return resumeMapper.findByCandidateId(candidateId);
    }

    public ResumeFile findCurrentFileEntity(Long candidateId) {
        return resumeFileMapper.findCurrent(candidateId);
    }

    /* ---------------- 内部方法 ---------------- */

    private Map<String, Object> resumeFileVo(ResumeFile file) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", file.getId());
        vo.put("fileName", file.getFileName());
        vo.put("fileSize", file.getFileSize());
        vo.put("contentType", file.getContentType());
        vo.put("uploadedAt", file.getUploadedAt());
        // 不返回 storageKey（D7）
        return vo;
    }

    /**
     * 归一化在线简历：只保留已知结构，所有文本做 XSS 过滤，数组限制条数。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeResume(Map<String, Object> raw) {
        Map<String, Object> input = raw == null ? Map.of() : raw;
        Map<String, Object> out = new LinkedHashMap<>();

        Map<String, Object> intentionIn = input.get("intention") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();
        Map<String, Object> intention = new LinkedHashMap<>();
        intention.put("expectCategory", text(intentionIn.get("expectCategory"), 40));
        intention.put("expectCity", text(intentionIn.get("expectCity"), 40));
        intention.put("expectSalary", text(intentionIn.get("expectSalary"), 40));
        intention.put("remark", text(intentionIn.get("remark"), 200));
        out.put("intention", intention);

        out.put("educations", normalizeList(input.get("educations"),
                List.of("school", "major", "degree", "startDate", "endDate"), 60));
        out.put("experiences", normalizeList(input.get("experiences"),
                List.of("company", "position", "startDate", "endDate", "description"), 1000));
        out.put("projects", normalizeList(input.get("projects"),
                List.of("name", "role", "startDate", "endDate", "description"), 1000));

        out.put("skills", text(input.get("skills"), 1000));
        out.put("certificates", text(input.get("certificates"), 1000));
        out.put("selfEvaluation", text(input.get("selfEvaluation"), 1000));
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeList(Object rawList, List<String> fields, int maxTextLength) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!(rawList instanceof List<?> list)) {
            return result;
        }
        if (list.size() > MAX_LIST_ITEMS) {
            throw new BizException(ErrorCode.PARAM_INVALID, "每类经历最多填写 " + MAX_LIST_ITEMS + " 条");
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> source = (Map<String, Object>) map;
            Map<String, Object> row = new LinkedHashMap<>();
            boolean hasValue = false;
            for (String field : fields) {
                String value = text(source.get(field), maxTextLength);
                row.put(field, value);
                if (value != null && !value.isBlank()) {
                    hasValue = true;
                }
            }
            if (hasValue) {
                result.add(row);
            }
        }
        return result;
    }

    private String text(Object value, int maxLength) {
        if (value == null) {
            return "";
        }
        String cleaned = HtmlSanitizer.plainText(String.valueOf(value).trim());
        if (cleaned == null) {
            return "";
        }
        if (cleaned.length() > maxLength) {
            throw new BizException(ErrorCode.PARAM_INVALID, "字段长度不能超过 " + maxLength + " 个字符");
        }
        return cleaned;
    }

    private boolean notBlank(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 投递资格校验结果，接口按 §15.3 返回给前端用于按钮状态与提示。
     */
    public record EligibilityResult(boolean canApply, List<String> missing,
                                    boolean hasResumeFile, boolean onlineResumeComplete) {
    }
}
