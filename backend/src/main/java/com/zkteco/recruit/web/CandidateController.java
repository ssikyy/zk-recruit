package com.zkteco.recruit.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.zkteco.recruit.common.ApiResult;
import com.zkteco.recruit.common.BizException;
import com.zkteco.recruit.common.ErrorCode;
import com.zkteco.recruit.common.PageQuery;
import com.zkteco.recruit.common.PageResult;
import com.zkteco.recruit.domain.entity.JobApplication;
import com.zkteco.recruit.domain.entity.ResumeFile;
import com.zkteco.recruit.dto.application.WithdrawRequest;
import com.zkteco.recruit.dto.candidate.ProfileRequest;
import com.zkteco.recruit.security.CurrentUserService;
import com.zkteco.recruit.service.ApplicationService;
import com.zkteco.recruit.service.CandidateService;
import com.zkteco.recruit.service.support.StorageService;

import jakarta.validation.Valid;

/**
 * 求职者接口（§15.3）。所有数据按会话中的 userId 过滤，不接受前端传入的 candidateId。
 */
@RestController
@RequestMapping("/api/candidate")
public class CandidateController {

    private final CandidateService candidateService;
    private final ApplicationService applicationService;
    private final StorageService storageService;
    private final CurrentUserService currentUserService;

    public CandidateController(CandidateService candidateService,
                               ApplicationService applicationService,
                               StorageService storageService,
                               CurrentUserService currentUserService) {
        this.candidateService = candidateService;
        this.applicationService = applicationService;
        this.storageService = storageService;
        this.currentUserService = currentUserService;
    }

    private Long me() {
        return currentUserService.requireCandidate().getUserId();
    }

    /* ---------------- 基本资料与在线简历 ---------------- */

    @GetMapping("/profile")
    public ApiResult<Map<String, Object>> profile() {
        return ApiResult.ok(candidateService.profile(me()));
    }

    @PutMapping("/profile")
    public ApiResult<Void> updateProfile(@Valid @RequestBody ProfileRequest request) {
        candidateService.updateProfile(me(), request);
        return ApiResult.ok();
    }

    @GetMapping("/resume")
    public ApiResult<Map<String, Object>> resume() {
        return ApiResult.ok(candidateService.resume(me()));
    }

    @PutMapping("/resume")
    public ApiResult<Void> saveResume(@RequestBody Map<String, Object> body) {
        candidateService.saveResume(me(), body);
        return ApiResult.ok();
    }

    /* ---------------- 附件简历 ---------------- */

    @GetMapping("/resume/file")
    public ApiResult<Map<String, Object>> currentFile() {
        return ApiResult.ok(candidateService.currentResumeFile(me()));
    }

    @PostMapping("/resume/file")
    public ApiResult<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        return ApiResult.ok(candidateService.uploadResumeFile(me(), file));
    }

    @DeleteMapping("/resume/file")
    public ApiResult<Void> clearFile() {
        candidateService.clearCurrentResumeFile(me());
        return ApiResult.ok();
    }

    /** 下载自己的当前附件 */
    @GetMapping("/resume/file/download")
    public ResponseEntity<Resource> downloadCurrent() {
        Long candidateId = me();
        ResumeFile file = candidateService.findCurrentFileEntity(candidateId);
        if (file == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "尚未上传附件简历");
        }
        return download(file);
    }

    /* ---------------- 投递 ---------------- */

    @GetMapping("/jobs/{jobId}/apply-eligibility")
    public ApiResult<Map<String, Object>> eligibility(@PathVariable Long jobId) {
        return ApiResult.ok(applicationService.eligibility(me(), jobId));
    }

    @PostMapping("/jobs/{jobId}/apply")
    public ApiResult<Map<String, Object>> apply(@PathVariable Long jobId) {
        return ApiResult.ok(applicationService.apply(me(), jobId));
    }

    @GetMapping("/applications")
    public ApiResult<PageResult<Map<String, Object>>> applications(@RequestParam(required = false) String status,
                                                                   @ModelAttribute PageQuery pageQuery) {
        return ApiResult.ok(applicationService.candidatePage(me(), status, pageQuery));
    }

    @GetMapping("/applications/{id}")
    public ApiResult<Map<String, Object>> applicationDetail(@PathVariable Long id) {
        return ApiResult.ok(applicationService.candidateDetail(me(), id));
    }

    /** 撤回投递（§9.6），不可逆 */
    @PostMapping("/applications/{id}/withdraw")
    public ApiResult<Void> withdraw(@PathVariable Long id, @Valid @RequestBody(required = false) WithdrawRequest request) {
        applicationService.withdraw(me(), id, request);
        return ApiResult.ok();
    }

    /** 下载该次投递的快照附件（不随当前附件变化） */
    @GetMapping("/applications/{id}/resume/download")
    public ResponseEntity<Resource> downloadSnapshot(@PathVariable Long id) {
        Long candidateId = me();
        JobApplication application = applicationService.requireOwnedApplication(candidateId, id);
        ResumeFile file = candidateService.findFile(application.getResumeFileId());
        if (file == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "该次投递未附带附件简历");
        }
        return download(file);
    }

    private ResponseEntity<Resource> download(ResumeFile file) {
        Path path = storageService.resolve(file.getStorageKey());
        if (!Files.exists(path)) {
            throw BizException.of(ErrorCode.NOT_FOUND, "原始附件不可用");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(file.getFileName()))
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(new FileSystemResource(path));
    }

    static String contentDisposition(String fileName) {
        String encoded = URLEncoder.encode(fileName == null ? "resume" : fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded;
    }
}
