package com.zkteco.recruit.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zkteco.recruit.common.BizException;
import com.zkteco.recruit.common.ErrorCode;
import com.zkteco.recruit.common.JsonUtils;
import com.zkteco.recruit.common.PageQuery;
import com.zkteco.recruit.common.PageResult;
import com.zkteco.recruit.config.AppProperties;
import com.zkteco.recruit.domain.entity.ApplicationLog;
import com.zkteco.recruit.domain.entity.CandidateProfile;
import com.zkteco.recruit.domain.entity.InterviewInfo;
import com.zkteco.recruit.domain.entity.Job;
import com.zkteco.recruit.domain.entity.JobApplication;
import com.zkteco.recruit.domain.entity.Resume;
import com.zkteco.recruit.domain.entity.ResumeFile;
import com.zkteco.recruit.domain.entity.SysUser;
import com.zkteco.recruit.domain.enums.ApplicationStatus;
import com.zkteco.recruit.domain.enums.JobStatus;
import com.zkteco.recruit.domain.vo.ApplicationRowVO;
import com.zkteco.recruit.domain.vo.JobRowVO;
import com.zkteco.recruit.dto.application.InterviewRequest;
import com.zkteco.recruit.dto.application.NoteRequest;
import com.zkteco.recruit.dto.application.StatusChangeRequest;
import com.zkteco.recruit.dto.application.WithdrawRequest;
import com.zkteco.recruit.mapper.ApplicationLogMapper;
import com.zkteco.recruit.mapper.InterviewInfoMapper;
import com.zkteco.recruit.mapper.JobApplicationMapper;
import com.zkteco.recruit.mapper.JobMapper;
import com.zkteco.recruit.mapper.SysUserMapper;
import com.zkteco.recruit.security.RateLimiter;
import com.zkteco.recruit.security.SessionUser;
import com.zkteco.recruit.service.support.HtmlSanitizer;

/**
 * 投递全流程：投递（含快照）、撤回与重投、HR 处理、面试、工作台。
 * 对应需求文档 §9.4–§9.6、§10.2、§10.4–§10.6、§12.2、§13.2。
 */
@Service
public class ApplicationService {

    private static final String OPERATOR_CANDIDATE = "CANDIDATE";
    private static final String OPERATOR_HR = "HR";

    private final JobApplicationMapper applicationMapper;
    private final ApplicationLogMapper logMapper;
    private final InterviewInfoMapper interviewMapper;
    private final JobMapper jobMapper;
    private final SysUserMapper userMapper;
    private final CandidateService candidateService;
    private final JobService jobService;
    private final RateLimiter rateLimiter;
    private final AppProperties appProperties;

    public ApplicationService(JobApplicationMapper applicationMapper,
                              ApplicationLogMapper logMapper,
                              InterviewInfoMapper interviewMapper,
                              JobMapper jobMapper,
                              SysUserMapper userMapper,
                              CandidateService candidateService,
                              JobService jobService,
                              RateLimiter rateLimiter,
                              AppProperties appProperties) {
        this.applicationMapper = applicationMapper;
        this.logMapper = logMapper;
        this.interviewMapper = interviewMapper;
        this.jobMapper = jobMapper;
        this.userMapper = userMapper;
        this.candidateService = candidateService;
        this.jobService = jobService;
        this.rateLimiter = rateLimiter;
        this.appProperties = appProperties;
    }

    /* =====================================================================
     * 投递资格与投递
     * ===================================================================== */

    /**
     * 供前端按钮状态使用（§15.3）：是否可投、缺失项、已投次数与剩余次数。
     */
    public Map<String, Object> eligibility(Long candidateId, Long jobId) {
        Job job = jobService.requireJob(jobId);
        CandidateService.EligibilityResult resume = candidateService.eligibility(candidateId);
        int attempts = applicationMapper.countAttempts(candidateId, jobId);
        int active = applicationMapper.countActive(candidateId, jobId);
        int maxAttempts = appProperties.getApply().getMaxAttempts();

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("jobPublished", job.getStatus() == JobStatus.PUBLISHED);
        vo.put("resumeReady", resume.canApply());
        vo.put("missing", resume.missing());
        vo.put("hasResumeFile", resume.hasResumeFile());
        vo.put("onlineResumeComplete", resume.onlineResumeComplete());
        vo.put("attemptCount", attempts);
        vo.put("maxAttempts", maxAttempts);
        vo.put("remainingAttempts", Math.max(0, maxAttempts - attempts));
        vo.put("hasActiveApplication", active > 0);
        vo.put("canApply", job.getStatus() == JobStatus.PUBLISHED
                && resume.canApply() && active == 0 && attempts < maxAttempts);
        return vo;
    }

    /**
     * 创建投递（§11.3）。快照在同一事务内冻结，写入后永不修改（D6）。
     */
    @Transactional
    public Map<String, Object> apply(Long candidateId, Long jobId) {
        Job job = jobService.requireJob(jobId);
        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw BizException.of(ErrorCode.JOB_NOT_PUBLISHED);
        }
        if (applicationMapper.countActive(candidateId, jobId) > 0) {
            throw BizException.of(ErrorCode.APPLY_DUPLICATED);
        }
        int attempts = applicationMapper.countAttempts(candidateId, jobId);
        int maxAttempts = appProperties.getApply().getMaxAttempts();
        if (attempts >= maxAttempts) {
            throw new BizException(ErrorCode.APPLY_LIMIT_EXCEEDED,
                    "该职位最多可投递 " + maxAttempts + " 次，您已用完投递机会");
        }

        CandidateService.EligibilityResult eligibility = candidateService.eligibility(candidateId);
        if (!eligibility.canApply()) {
            throw new BizException(ErrorCode.RESUME_INCOMPLETE,
                    "请先完善资料与简历后再投递", eligibility.missing());
        }

        CandidateProfile profile = candidateService.requireProfile(candidateId);
        SysUser user = userMapper.selectById(candidateId);
        Resume resume = candidateService.findResume(candidateId);
        ResumeFile currentFile = candidateService.findCurrentFileEntity(candidateId);
        JobRowVO jobRow = jobMapper.selectJobRow(jobId);

        JobApplication application = new JobApplication();
        application.setCandidateId(candidateId);
        application.setJobId(jobId);
        application.setAttemptNo(attempts + 1);
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setVersion(0);
        application.setAppliedAt(LocalDateTime.now());
        // 三份快照：在线简历、附件版本、职位内容；以及联系信息快照
        application.setResumeSnapshot(resume == null ? null : resume.getContent());
        application.setResumeFileId(currentFile == null ? null : currentFile.getId());
        application.setJobSnapshot(JsonUtils.toJson(jobService.buildSnapshot(jobRow)));
        application.setCandidateSnapshot(JsonUtils.toJson(Map.of(
                "name", nullToEmpty(profile.getName()),
                "phone", nullToEmpty(profile.getPhone()),
                "email", user == null ? "" : nullToEmpty(user.getEmail()),
                "city", nullToEmpty(profile.getCity())
        )));
        applicationMapper.insert(application);

        writeLog(application.getId(), "APPLY", null, ApplicationStatus.SUBMITTED,
                candidateId, OPERATOR_CANDIDATE, "第 " + application.getAttemptNo() + " 次投递");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applicationId", application.getId());
        result.put("attemptNo", application.getAttemptNo());
        result.put("remainingAttempts", Math.max(0, maxAttempts - application.getAttemptNo()));
        return result;
    }

    /* =====================================================================
     * 撤回（§9.6）
     * ===================================================================== */

    @Transactional
    public void withdraw(Long candidateId, Long applicationId, WithdrawRequest request) {
        rateLimiter.assertHourlyLimit("withdraw", String.valueOf(candidateId),
                appProperties.getSecurity().getWithdrawLimitPerHour(),
                "操作过于频繁，请稍后再试");

        JobApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "投递记录不存在");
        }
        // 归属校验：只能撤回自己的投递
        if (!application.getCandidateId().equals(candidateId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权操作该投递");
        }
        ApplicationStatus from = application.getStatus();
        if (!from.withdrawable()) {
            throw new BizException(ErrorCode.WITHDRAW_NOT_ALLOWED,
                    from == ApplicationStatus.WITHDRAWN
                            ? "该投递已撤回"
                            : "该投递已有结论，无法撤回");
        }

        int rows = applicationMapper.withdraw(applicationId, application.getVersion(), from.name());
        if (rows == 0) {
            throw BizException.of(ErrorCode.VERSION_CONFLICT);
        }
        writeLog(applicationId, "WITHDRAW", from, ApplicationStatus.WITHDRAWN,
                candidateId, OPERATOR_CANDIDATE, buildWithdrawRemark(request));
    }

    private String buildWithdrawRemark(WithdrawRequest request) {
        if (request == null) {
            return null;
        }
        String reasonLabel = switch (request.getReason() == null ? "" : request.getReason()) {
            case "FOUND_OTHER_JOB" -> "已找到其他工作";
            case "WRONG_APPLY" -> "投递错误";
            case "NOT_INTERESTED" -> "暂不考虑该岗位";
            case "OTHER" -> "其他";
            default -> null;
        };
        String remark = HtmlSanitizer.plainText(request.getRemark());
        if (reasonLabel == null && (remark == null || remark.isBlank())) {
            return null;
        }
        if (reasonLabel == null) {
            return remark;
        }
        return remark == null || remark.isBlank() ? reasonLabel : reasonLabel + "：" + remark;
    }

    /* =====================================================================
     * 求职者视角（§9.4）
     * ===================================================================== */

    public PageResult<Map<String, Object>> candidatePage(Long candidateId, String status, PageQuery pageQuery) {
        QueryWrapper<JobApplication> wrapper = new QueryWrapper<>();
        wrapper.eq("a.candidate_id", candidateId);
        if (status != null && !status.isBlank()) {
            wrapper.eq("a.status", status);
        }
        wrapper.last("ORDER BY a.applied_at DESC, a.id DESC");

        Page<ApplicationRowVO> page = new Page<>(pageQuery.normalizedPage(), pageQuery.normalizedSize());
        Page<ApplicationRowVO> result = (Page<ApplicationRowVO>) applicationMapper.selectRows(page, wrapper);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ApplicationRowVO row : result.getRecords()) {
            Map<String, Object> snapshot = JsonUtils.parseMap(row.getJobSnapshot());
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", row.getId());
            // 列表展示快照中的职位名称（§9.4）
            vo.put("jobTitle", snapshot.getOrDefault("title", row.getJobTitle()));
            vo.put("jobId", row.getJobId());
            vo.put("recruitmentType", snapshot.get("recruitmentType"));
            vo.put("locationName", snapshot.getOrDefault("locationName", row.getLocationName()));
            vo.put("attemptNo", row.getAttemptNo());
            vo.put("status", row.getStatus());
            vo.put("statusLabel", row.getStatus().getCandidateLabel());
            vo.put("appliedAt", row.getAppliedAt());
            vo.put("withdrawnAt", row.getWithdrawnAt());
            vo.put("canWithdraw", row.getStatus().withdrawable());
            vo.put("interviewTime", row.getInterviewTime());
            rows.add(vo);
        }
        return PageResult.of(rows, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 求职者查看投递详情：只返回对外状态与面试的公开字段，
     * 内部备注与面试评价不出现在响应体中（§13.3）。
     */
    public Map<String, Object> candidateDetail(Long candidateId, Long applicationId) {
        JobApplication application = requireOwnedApplication(candidateId, applicationId);
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", application.getId());
        vo.put("jobId", application.getJobId());
        vo.put("attemptNo", application.getAttemptNo());
        vo.put("status", application.getStatus());
        vo.put("statusLabel", application.getStatus().getCandidateLabel());
        vo.put("appliedAt", application.getAppliedAt());
        vo.put("withdrawnAt", application.getWithdrawnAt());
        vo.put("canWithdraw", application.getStatus().withdrawable());
        vo.put("jobSnapshot", JsonUtils.parseMap(application.getJobSnapshot()));
        vo.put("resumeSnapshot", JsonUtils.parseMap(application.getResumeSnapshot()));

        ResumeFile file = candidateService.findFile(application.getResumeFileId());
        vo.put("resumeFile", file == null ? null : Map.of(
                "id", file.getId(),
                "fileName", file.getFileName(),
                "fileSize", file.getFileSize()
        ));

        InterviewInfo interview = interviewMapper.findByApplicationId(applicationId);
        vo.put("interview", interview == null ? null : publicInterview(interview));
        return vo;
    }

    public JobApplication requireOwnedApplication(Long candidateId, Long applicationId) {
        JobApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "投递记录不存在");
        }
        if (!application.getCandidateId().equals(candidateId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权访问该投递");
        }
        return application;
    }

    /* =====================================================================
     * HR 视角（§10.4、§10.5）
     * ===================================================================== */

    public PageResult<Map<String, Object>> hrPage(SessionUser current, String scope, String keyword, Long jobId,
                                                  String type, String status, boolean includeWithdrawn,
                                                  PageQuery pageQuery) {
        QueryWrapper<JobApplication> wrapper = new QueryWrapper<>();
        if (jobService.isMineScope(current, scope)) {
            wrapper.eq("j.owner_hr_id", current.getUserId());
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("cp.name", keyword.trim());
        }
        if (jobId != null) {
            wrapper.eq("a.job_id", jobId);
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq("j.recruitment_type", type);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("a.status", status);
        } else if (!includeWithdrawn) {
            // 默认不显示已撤回记录（§9.6）
            wrapper.ne("a.status", ApplicationStatus.WITHDRAWN.name());
        }
        wrapper.last("ORDER BY " + pageQuery.orderByOrDefault("a.applied_at DESC",
                "applied_at", "last_handled_at", "attempt_no") + ", a.id DESC");

        Page<ApplicationRowVO> page = new Page<>(pageQuery.normalizedPage(), pageQuery.normalizedSize());
        Page<ApplicationRowVO> result = (Page<ApplicationRowVO>) applicationMapper.selectRows(page, wrapper);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ApplicationRowVO row : result.getRecords()) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", row.getId());
            vo.put("candidateId", row.getCandidateId());
            vo.put("candidateName", row.getCandidateName());
            // 列表页手机号脱敏（§10.4）
            vo.put("candidatePhone", maskPhone(row.getCandidatePhone()));
            vo.put("jobId", row.getJobId());
            vo.put("jobTitle", row.getJobTitle());
            vo.put("recruitmentType", row.getRecruitmentType());
            vo.put("ownerHrId", row.getOwnerHrId());
            vo.put("ownerName", row.getOwnerName());
            vo.put("attemptNo", row.getAttemptNo());
            vo.put("status", row.getStatus());
            vo.put("statusLabel", row.getStatus().getHrLabel());
            vo.put("appliedAt", row.getAppliedAt());
            vo.put("lastHandledAt", row.getLastHandledAt());
            vo.put("interviewTime", row.getInterviewTime());
            vo.put("interviewResult", row.getInterviewResult());
            // 普通 HR 对他人负责的记录只读（D10）
            vo.put("canWrite", jobService.canWrite(current, row.getOwnerHrId())
                    && row.getStatus() != ApplicationStatus.WITHDRAWN);
            rows.add(vo);
        }
        return PageResult.of(rows, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public Map<String, Object> hrDetail(SessionUser current, Long applicationId) {
        JobApplication application = requireApplication(applicationId);
        Job job = jobMapper.selectById(application.getJobId());
        Map<String, Object> jobSnapshot = JsonUtils.parseMap(application.getJobSnapshot());
        CandidateProfile profile = candidateService.requireProfile(application.getCandidateId());
        SysUser candidateUser = userMapper.selectById(application.getCandidateId());

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", application.getId());
        vo.put("attemptNo", application.getAttemptNo());
        vo.put("status", application.getStatus());
        vo.put("statusLabel", application.getStatus().getHrLabel());
        vo.put("version", application.getVersion());
        vo.put("appliedAt", application.getAppliedAt());
        vo.put("withdrawnAt", application.getWithdrawnAt());
        vo.put("lastHandledAt", application.getLastHandledAt());
        vo.put("hrNote", application.getHrNote());
        vo.put("readOnly", application.getStatus() == ApplicationStatus.WITHDRAWN);
        vo.put("canWrite", job != null && jobService.canWrite(current, job.getOwnerHrId())
                && application.getStatus() != ApplicationStatus.WITHDRAWN);

        // 候选人当前资料（详情页手机号明文，§10.5）
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("id", application.getCandidateId());
        candidate.put("name", profile.getName());
        candidate.put("phone", profile.getPhone());
        candidate.put("email", candidateUser == null ? null : candidateUser.getEmail());
        candidate.put("city", profile.getCity());
        vo.put("candidateCurrent", candidate);
        vo.put("candidateSnapshot", JsonUtils.parseMap(application.getCandidateSnapshot()));

        vo.put("jobSnapshot", jobSnapshot);
        vo.put("resumeSnapshot", JsonUtils.parseMap(application.getResumeSnapshot()));

        ResumeFile file = candidateService.findFile(application.getResumeFileId());
        vo.put("resumeFile", file == null ? null : Map.of(
                "id", file.getId(),
                "fileName", file.getFileName(),
                "fileSize", file.getFileSize(),
                "available", true
        ));

        // 职位当前信息与快照版本对比，前端据此显示"职位已被修改"（§10.5）
        Map<String, Object> currentJob = new LinkedHashMap<>();
        boolean jobModified = false;
        if (job != null) {
            currentJob.put("id", job.getId());
            currentJob.put("title", job.getTitle());
            currentJob.put("status", job.getStatus());
            currentJob.put("ownerHrId", job.getOwnerHrId());
            currentJob.put("version", job.getVersion());
            Object snapshotVersion = jobSnapshot.get("jobVersion");
            jobModified = snapshotVersion != null
                    && !String.valueOf(snapshotVersion).equals(String.valueOf(job.getVersion()));
        }
        vo.put("jobCurrent", currentJob);
        vo.put("jobModified", jobModified);

        InterviewInfo interview = interviewMapper.findByApplicationId(applicationId);
        vo.put("interview", interview == null ? null : fullInterview(interview));

        vo.put("logs", logs(applicationId));
        vo.put("history", history(application.getCandidateId(), application.getJobId(), applicationId));
        return vo;
    }

    public List<Map<String, Object>> logs(Long applicationId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ApplicationLogMapper.LogRow log : logMapper.listByApplication(applicationId)) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", log.getId());
            vo.put("action", log.getAction());
            vo.put("fromStatus", log.getFromStatus());
            vo.put("toStatus", log.getToStatus());
            vo.put("operatorId", log.getOperatorId());
            vo.put("operatorType", log.getOperatorType());
            vo.put("operatorName", log.getOperatorName());
            vo.put("remark", log.getRemark());
            vo.put("createdAt", log.getCreatedAt());
            rows.add(vo);
        }
        return rows;
    }

    /**
     * 同一候选人对同一职位的历史投递（含撤回），便于 HR 判断（§9.6）。
     */
    public List<Map<String, Object>> history(Long candidateId, Long jobId, Long currentId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ApplicationRowVO row : applicationMapper.selectHistory(candidateId, jobId)) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", row.getId());
            vo.put("attemptNo", row.getAttemptNo());
            vo.put("status", row.getStatus());
            vo.put("statusLabel", row.getStatus().getHrLabel());
            vo.put("appliedAt", row.getAppliedAt());
            vo.put("withdrawnAt", row.getWithdrawnAt());
            vo.put("current", row.getId().equals(currentId));
            rows.add(vo);
        }
        return rows;
    }

    public List<Map<String, Object>> historyOf(Long applicationId) {
        JobApplication application = requireApplication(applicationId);
        return history(application.getCandidateId(), application.getJobId(), applicationId);
    }

    /**
     * HR 变更投递状态（§12.2）。撤回记录只读；越权返回 3008；版本冲突返回 3004。
     */
    @Transactional
    public void changeStatus(SessionUser current, Long applicationId, StatusChangeRequest request) {
        JobApplication application = requireApplication(applicationId);
        assertHrWritable(current, application);

        ApplicationStatus from = application.getStatus();
        ApplicationStatus to = request.getTargetStatus();
        if (to == ApplicationStatus.WITHDRAWN) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "撤回只能由候选人本人发起");
        }
        if (!request.getVersion().equals(application.getVersion())) {
            throw BizException.of(ErrorCode.VERSION_CONFLICT);
        }
        if (!from.canTransitTo(to)) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID,
                    "不允许从" + from.getHrLabel() + "变更为" + to.getHrLabel());
        }

        int rows = applicationMapper.updateStatus(applicationId, application.getVersion(), from.name(), to.name());
        if (rows == 0) {
            throw BizException.of(ErrorCode.VERSION_CONFLICT);
        }
        boolean undo = (from == ApplicationStatus.PASSED || from == ApplicationStatus.REJECTED)
                && to == ApplicationStatus.VIEWED;
        writeLog(applicationId, undo ? "UNDO_CONCLUSION" : "CHANGE_STATUS", from, to,
                current.getUserId(), OPERATOR_HR, HtmlSanitizer.plainText(request.getRemark()));
    }

    @Transactional
    public void updateNote(SessionUser current, Long applicationId, NoteRequest request) {
        JobApplication application = requireApplication(applicationId);
        assertHrWritable(current, application);
        if (!request.getVersion().equals(application.getVersion())) {
            throw BizException.of(ErrorCode.VERSION_CONFLICT);
        }
        String note = HtmlSanitizer.plainText(request.getNote());
        int rows = applicationMapper.updateNote(applicationId, application.getVersion(), note);
        if (rows == 0) {
            throw BizException.of(ErrorCode.VERSION_CONFLICT);
        }
        writeLog(applicationId, "UPDATE_NOTE", application.getStatus(), application.getStatus(),
                current.getUserId(), OPERATOR_HR, "更新内部备注");
    }

    /**
     * 面试安排（§10.6）。改期为更新原记录；保存后状态自动流转为待面试；
     * 填写面试结果不会自动改变投递状态（D16）。
     */
    @Transactional
    public Map<String, Object> saveInterview(SessionUser current, Long applicationId, InterviewRequest request) {
        JobApplication application = requireApplication(applicationId);
        assertHrWritable(current, application);
        if (!request.getVersion().equals(application.getVersion())) {
            throw BizException.of(ErrorCode.VERSION_CONFLICT);
        }

        ApplicationStatus from = application.getStatus();
        boolean needTransit = from != ApplicationStatus.INTERVIEW;
        if (needTransit && !from.canTransitTo(ApplicationStatus.INTERVIEW)) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID,
                    from == ApplicationStatus.SUBMITTED
                            ? "请先标记已查看，再安排面试"
                            : "当前状态不允许安排面试");
        }

        InterviewInfo interview = interviewMapper.findByApplicationId(applicationId);
        boolean isNew = interview == null;
        if (isNew) {
            interview = new InterviewInfo();
            interview.setApplicationId(applicationId);
        }
        LocalDateTime previousTime = interview.getInterviewTime();
        interview.setInterviewTime(request.getInterviewTime());
        interview.setMethod(request.getMethod());
        interview.setAddress(HtmlSanitizer.plainText(request.getAddress()));
        interview.setContactNote(HtmlSanitizer.plainText(request.getContactNote()));
        interview.setEvaluation(HtmlSanitizer.plainText(request.getEvaluation()));
        interview.setResult(request.getResult());
        if (isNew) {
            interviewMapper.insert(interview);
        } else {
            interviewMapper.updateById(interview);
        }

        if (needTransit) {
            int rows = applicationMapper.updateStatus(applicationId, application.getVersion(),
                    from.name(), ApplicationStatus.INTERVIEW.name());
            if (rows == 0) {
                throw BizException.of(ErrorCode.VERSION_CONFLICT);
            }
            writeLog(applicationId, "ARRANGE_INTERVIEW", from, ApplicationStatus.INTERVIEW,
                    current.getUserId(), OPERATOR_HR, "安排面试");
        } else {
            String remark = isNew ? "安排面试"
                    : (previousTime != null && !previousTime.equals(request.getInterviewTime())
                    ? "面试改期" : "更新面试信息");
            writeLog(applicationId, isNew ? "ARRANGE_INTERVIEW" : "UPDATE_INTERVIEW", from, from,
                    current.getUserId(), OPERATOR_HR, remark);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("interview", fullInterview(interview));
        // 第一版不发送任何通知，前端据此提示 HR 电话联系（D5）
        result.put("noticeHint", "系统不会通知候选人，请电话联系");
        result.put("candidatePhone", candidateService.requireProfile(application.getCandidateId()).getPhone());
        return result;
    }

    /* =====================================================================
     * 工作台（§10.2）
     * ===================================================================== */

    public Map<String, Object> dashboard(SessionUser current, String scope) {
        boolean mine = jobService.isMineScope(current, scope);
        Long ownerFilter = mine ? current.getUserId() : null;

        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("scope", mine ? "MINE" : "ALL");
        vo.put("publishedJobs", mine
                ? jobMapper.countPublishedByOwner(current.getUserId())
                : jobMapper.countPublished());
        vo.put("pendingApplications",
                applicationMapper.countByStatus(ApplicationStatus.SUBMITTED.name(), ownerFilter));
        vo.put("pendingInterviews", applicationMapper.countPendingInterview(ownerFilter));
        vo.put("passedApplications",
                applicationMapper.countByStatus(ApplicationStatus.PASSED.name(), ownerFilter));
        if (current.isHrAdmin()) {
            vo.put("jobsWithDisabledOwner", applicationMapper.countJobsWithDisabledOwner());
        }
        return vo;
    }

    /* =====================================================================
     * 内部方法
     * ===================================================================== */

    public JobApplication requireApplication(Long applicationId) {
        JobApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "投递记录不存在");
        }
        return application;
    }

    /**
     * HR 写权限：撤回记录一律只读（3005），其余按职位归属校验（3008）。
     */
    private void assertHrWritable(SessionUser current, JobApplication application) {
        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "候选人已撤回该投递，记录为只读");
        }
        Job job = jobMapper.selectById(application.getJobId());
        if (job == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "职位不存在");
        }
        jobService.assertWritable(current, job);
    }

    public Job jobOf(JobApplication application) {
        return jobMapper.selectById(application.getJobId());
    }

    private void writeLog(Long applicationId, String action, ApplicationStatus from, ApplicationStatus to,
                          Long operatorId, String operatorType, String remark) {
        ApplicationLog log = new ApplicationLog();
        log.setApplicationId(applicationId);
        log.setAction(action);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperatorId(operatorId);
        log.setOperatorType(operatorType);
        log.setRemark(remark);
        logMapper.insert(log);
    }

    /** 求职者可见的面试字段：不含评价（§10.6） */
    private Map<String, Object> publicInterview(InterviewInfo interview) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("interviewTime", interview.getInterviewTime());
        vo.put("method", interview.getMethod());
        vo.put("methodLabel", interview.getMethod() == null ? null : interview.getMethod().getLabel());
        vo.put("address", interview.getAddress());
        vo.put("contactNote", interview.getContactNote());
        return vo;
    }

    /** HR 可见的完整面试字段 */
    private Map<String, Object> fullInterview(InterviewInfo interview) {
        Map<String, Object> vo = publicInterview(interview);
        vo.put("evaluation", interview.getEvaluation());
        vo.put("result", interview.getResult());
        vo.put("updatedAt", interview.getUpdatedAt());
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
