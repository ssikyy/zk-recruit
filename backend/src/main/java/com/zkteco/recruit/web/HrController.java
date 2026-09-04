package com.zkteco.recruit.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zkteco.recruit.common.ApiResult;
import com.zkteco.recruit.common.BizException;
import com.zkteco.recruit.common.ErrorCode;
import com.zkteco.recruit.common.PageQuery;
import com.zkteco.recruit.common.PageResult;
import com.zkteco.recruit.domain.entity.Job;
import com.zkteco.recruit.domain.entity.JobApplication;
import com.zkteco.recruit.domain.entity.ResumeFile;
import com.zkteco.recruit.domain.enums.RecruitmentType;
import com.zkteco.recruit.domain.vo.JobRowVO;
import com.zkteco.recruit.dto.application.InterviewRequest;
import com.zkteco.recruit.dto.application.NoteRequest;
import com.zkteco.recruit.dto.application.StatusChangeRequest;
import com.zkteco.recruit.dto.job.JobOwnerRequest;
import com.zkteco.recruit.dto.job.JobRequest;
import com.zkteco.recruit.dto.job.JobStatusRequest;
import com.zkteco.recruit.security.CurrentUserService;
import com.zkteco.recruit.security.SessionUser;
import com.zkteco.recruit.service.ApplicationService;
import com.zkteco.recruit.service.CandidateService;
import com.zkteco.recruit.service.HrUserService;
import com.zkteco.recruit.service.JobService;
import com.zkteco.recruit.service.support.StorageService;

import jakarta.validation.Valid;

/**
 * HR 接口（§15.4）。角色由拦截器保证，写操作的归属校验在 Service 内完成（D10）。
 */
@RestController
@RequestMapping("/api/hr")
public class HrController {

    private final JobService jobService;
    private final ApplicationService applicationService;
    private final CandidateService candidateService;
    private final HrUserService hrUserService;
    private final StorageService storageService;
    private final CurrentUserService currentUserService;

    public HrController(JobService jobService,
                        ApplicationService applicationService,
                        CandidateService candidateService,
                        HrUserService hrUserService,
                        StorageService storageService,
                        CurrentUserService currentUserService) {
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.candidateService = candidateService;
        this.hrUserService = hrUserService;
        this.storageService = storageService;
        this.currentUserService = currentUserService;
    }

    private SessionUser me() {
        return currentUserService.requireHr();
    }

    /* ---------------- 工作台 ---------------- */

    @GetMapping("/dashboard")
    public ApiResult<Map<String, Object>> dashboard(@RequestParam(required = false) String scope) {
        return ApiResult.ok(applicationService.dashboard(me(), scope));
    }

    /* ---------------- 职位管理 ---------------- */

    @GetMapping("/jobs")
    public ApiResult<PageResult<JobRowVO>> jobs(@RequestParam(required = false) String scope,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) RecruitmentType type,
                                                @RequestParam(required = false) String keyword,
                                                @ModelAttribute PageQuery pageQuery) {
        return ApiResult.ok(jobService.hrPage(me(), scope, status, type, keyword, pageQuery));
    }

    @PostMapping("/jobs")
    public ApiResult<Map<String, Object>> createJob(@Valid @RequestBody JobRequest request) {
        Long id = jobService.create(me(), request);
        return ApiResult.ok(Map.of("id", id));
    }

    @GetMapping("/jobs/{id}")
    public ApiResult<Map<String, Object>> jobDetail(@PathVariable Long id) {
        SessionUser current = me();
        JobRowVO row = jobService.hrDetail(id);
        Job job = jobService.requireJob(id);
        return ApiResult.ok(Map.of(
                "job", row,
                "duty", job.getDuty(),
                "requirement", job.getRequirement(),
                "canWrite", jobService.canWrite(current, row.getOwnerHrId())
        ));
    }

    @PutMapping("/jobs/{id}")
    public ApiResult<Void> updateJob(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        jobService.update(me(), id, request);
        return ApiResult.ok();
    }

    @PutMapping("/jobs/{id}/status")
    public ApiResult<Void> updateJobStatus(@PathVariable Long id, @Valid @RequestBody JobStatusRequest request) {
        jobService.updateStatus(me(), id, request.getTargetStatus(), request.getVersion());
        return ApiResult.ok();
    }

    /** 转移负责人，服务层校验管理员权限（§10.3） */
    @PutMapping("/jobs/{id}/owner")
    public ApiResult<Void> transferOwner(@PathVariable Long id, @Valid @RequestBody JobOwnerRequest request) {
        jobService.transferOwner(me(), id, request.getOwnerHrId(), request.getVersion());
        return ApiResult.ok();
    }

    /* ---------------- 投递管理 ---------------- */

    @GetMapping("/applications")
    public ApiResult<PageResult<Map<String, Object>>> applications(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") boolean includeWithdrawn,
            @ModelAttribute PageQuery pageQuery) {
        return ApiResult.ok(applicationService.hrPage(me(), scope, keyword, jobId, type, status,
                includeWithdrawn, pageQuery));
    }

    @GetMapping("/applications/{id}")
    public ApiResult<Map<String, Object>> applicationDetail(@PathVariable Long id) {
        return ApiResult.ok(applicationService.hrDetail(me(), id));
    }

    @GetMapping("/applications/{id}/logs")
    public ApiResult<List<Map<String, Object>>> logs(@PathVariable Long id) {
        return ApiResult.ok(applicationService.logs(id));
    }

    @GetMapping("/applications/{id}/history")
    public ApiResult<List<Map<String, Object>>> history(@PathVariable Long id) {
        return ApiResult.ok(applicationService.historyOf(id));
    }

    @PutMapping("/applications/{id}/status")
    public ApiResult<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        applicationService.changeStatus(me(), id, request);
        return ApiResult.ok();
    }

    @PutMapping("/applications/{id}/note")
    public ApiResult<Void> updateNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
        applicationService.updateNote(me(), id, request);
        return ApiResult.ok();
    }

    @PutMapping("/applications/{id}/interview")
    public ApiResult<Map<String, Object>> saveInterview(@PathVariable Long id,
                                                        @Valid @RequestBody InterviewRequest request) {
        return ApiResult.ok(applicationService.saveInterview(me(), id, request));
    }

    /**
     * 下载候选人投递时的附件快照。必须经 applicationId 关联查询，
     * 不允许按 fileId 直接下载（§15.6）。
     */
    @GetMapping("/applications/{id}/resume/download")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long id) {
        me();
        JobApplication application = applicationService.requireApplication(id);
        ResumeFile file = candidateService.findFile(application.getResumeFileId());
        if (file == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "该次投递未附带附件简历");
        }
        Path path = storageService.resolve(file.getStorageKey());
        if (!Files.exists(path)) {
            throw BizException.of(ErrorCode.NOT_FOUND, "原始附件不可用");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, CandidateController.contentDisposition(file.getFileName()))
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(new FileSystemResource(path));
    }

    /* ---------------- 负责人下拉 ---------------- */

    @GetMapping("/hr-users/options")
    public ApiResult<List<Map<String, Object>>> hrOptions() {
        me();
        return ApiResult.ok(hrUserService.options());
    }
}
