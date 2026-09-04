package com.zkteco.recruit.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zkteco.recruit.common.BizException;
import com.zkteco.recruit.common.ErrorCode;
import com.zkteco.recruit.common.PageQuery;
import com.zkteco.recruit.common.PageResult;
import com.zkteco.recruit.domain.entity.Job;
import com.zkteco.recruit.domain.entity.JobCategory;
import com.zkteco.recruit.domain.entity.JobLocation;
import com.zkteco.recruit.domain.enums.JobFieldOptions;
import com.zkteco.recruit.domain.enums.JobStatus;
import com.zkteco.recruit.domain.enums.RecruitmentType;
import com.zkteco.recruit.domain.vo.JobRowVO;
import com.zkteco.recruit.dto.job.JobRequest;
import com.zkteco.recruit.mapper.JobMapper;
import com.zkteco.recruit.security.SessionUser;
import com.zkteco.recruit.service.support.HtmlSanitizer;
import com.zkteco.recruit.service.support.OperationLogService;

@Service
public class JobService {

    private final JobMapper jobMapper;
    private final DictService dictService;
    private final HrUserService hrUserService;
    private final OperationLogService operationLogService;

    public JobService(JobMapper jobMapper,
                      DictService dictService,
                      HrUserService hrUserService,
                      OperationLogService operationLogService) {
        this.jobMapper = jobMapper;
        this.dictService = dictService;
        this.hrUserService = hrUserService;
        this.operationLogService = operationLogService;
    }

    /* ---------------- 官网查询（§7.3、§7.4） ---------------- */

    public PageResult<JobRowVO> publicPage(RecruitmentType type, String keyword, Long categoryId,
                                           Long locationId, PageQuery pageQuery) {
        QueryWrapper<Job> wrapper = new QueryWrapper<>();
        wrapper.eq("j.status", JobStatus.PUBLISHED.name());
        if (type != null) {
            wrapper.eq("j.recruitment_type", type.name());
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("j.title", keyword.trim());
        }
        if (categoryId != null) {
            wrapper.eq("j.category_id", categoryId);
        }
        if (locationId != null) {
            wrapper.eq("j.location_id", locationId);
        }
        wrapper.last("ORDER BY j.published_at DESC, j.id DESC");

        Page<JobRowVO> page = new Page<>(pageQuery.normalizedPage(), pageQuery.normalizedSize());
        return PageResult.of(jobMapper.selectJobRows(page, wrapper), row -> row);
    }

    /**
     * 官网职位详情。已关闭的职位仍可访问（历史链接），但前端不再提供投递操作（§13.1）。
     */
    public JobRowVO publicDetail(Long id) {
        JobRowVO row = jobMapper.selectJobRow(id);
        if (row == null || row.getStatus() == JobStatus.DRAFT) {
            throw BizException.of(ErrorCode.NOT_FOUND, "职位不存在或未发布");
        }
        return row;
    }

    /* ---------------- HR 查询（§10.3） ---------------- */

    public PageResult<JobRowVO> hrPage(SessionUser current, String scope, String status, RecruitmentType type,
                                       String keyword, PageQuery pageQuery) {
        QueryWrapper<Job> wrapper = new QueryWrapper<>();
        if (isMineScope(current, scope)) {
            wrapper.eq("j.owner_hr_id", current.getUserId());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("j.status", status);
        }
        if (type != null) {
            wrapper.eq("j.recruitment_type", type.name());
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("j.title", keyword.trim());
        }
        wrapper.last("ORDER BY j.id DESC");

        Page<JobRowVO> page = new Page<>(pageQuery.normalizedPage(), pageQuery.normalizedSize());
        return PageResult.of(jobMapper.selectJobRows(page, wrapper), row -> row);
    }

    public JobRowVO hrDetail(Long id) {
        JobRowVO row = jobMapper.selectJobRow(id);
        if (row == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "职位不存在");
        }
        return row;
    }

    /* ---------------- 写操作 ---------------- */

    @Transactional
    public Long create(SessionUser current, JobRequest request) {
        JobCategory category = dictService.requireCategory(request.getCategoryId());
        JobLocation location = dictService.requireLocation(request.getLocationId());
        dictService.assertSelectable(category, location, null, null);
        validateTypeFields(request);

        Job job = new Job();
        applyRequest(job, request);
        job.setStatus(JobStatus.DRAFT);
        job.setVersion(0);
        job.setCreatedBy(current.getUserId());
        // 普通 HR 新建后自动成为负责人；管理员可指定（§10.3）
        job.setOwnerHrId(resolveOwner(current, request.getOwnerHrId(), current.getUserId()));
        jobMapper.insert(job);
        return job.getId();
    }

    @Transactional
    public void update(SessionUser current, Long id, JobRequest request) {
        Job job = requireJob(id);
        assertWritable(current, job);

        JobCategory category = dictService.requireCategory(request.getCategoryId());
        JobLocation location = dictService.requireLocation(request.getLocationId());
        // 编辑时若沿用原有的已停用字典项则放行（§10.7）
        dictService.assertSelectable(category, location, job.getCategoryId(), job.getLocationId());
        validateTypeFields(request);

        if (job.getStatus() == JobStatus.CLOSED) {
            throw BizException.of(ErrorCode.STATUS_TRANSITION_INVALID, "已关闭的职位需先重新发布才能编辑");
        }

        applyRequest(job, request);
        job.setVersion(job.getVersion() + 1);
        jobMapper.updateById(job);
    }

    /**
     * 状态变更（§12.1）。PUBLISHED → DRAFT 仅当该职位没有任何投递（含已撤回）。
     */
    @Transactional
    public void updateStatus(SessionUser current, Long id, JobStatus target, Integer version) {
        Job job = requireJob(id);
        assertWritable(current, job);
        if (version != null && !version.equals(job.getVersion())) {
            throw BizException.of(ErrorCode.VERSION_CONFLICT);
        }
        JobStatus from = job.getStatus();
        if (from == target) {
            return;
        }
        if (!from.canTransitTo(target)) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID,
                    "不允许从" + from.getLabel() + "变更为" + target.getLabel());
        }
        if (from == JobStatus.PUBLISHED && target == JobStatus.DRAFT
                && jobMapper.countAllApplications(id) > 0) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID,
                    "该职位已产生投递，不能撤回发布");
        }

        int rows = jobMapper.updateStatus(id, job.getVersion(), from.name(), target.name());
        if (rows == 0) {
            throw BizException.of(ErrorCode.VERSION_CONFLICT);
        }
        operationLogService.record(current.getUserId(), OperationLogService.MODULE_JOB, "STATUS",
                id, Map.of("from", from.name(), "to", target.name()));
    }

    /**
     * 转移负责人，仅管理员 HR（§10.3）。转移后原负责人写权限立即失效。
     */
    @Transactional
    public void transferOwner(SessionUser current, Long id, Long newOwnerId, Integer version) {
        if (!current.isHrAdmin()) {
            throw BizException.of(ErrorCode.NEED_ADMIN);
        }
        Job job = requireJob(id);
        if (version != null && !version.equals(job.getVersion())) {
            throw BizException.of(ErrorCode.VERSION_CONFLICT);
        }
        hrUserService.requireEnabledHr(newOwnerId);

        int rows = jobMapper.updateOwner(id, job.getVersion(), newOwnerId);
        if (rows == 0) {
            throw BizException.of(ErrorCode.VERSION_CONFLICT);
        }
        operationLogService.record(current.getUserId(), OperationLogService.MODULE_JOB, "TRANSFER_OWNER",
                id, Map.of("from", job.getOwnerHrId(), "to", newOwnerId));
    }

    /* ---------------- 归属校验（权限第三层，D10） ---------------- */

    public Job requireJob(Long id) {
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "职位不存在");
        }
        return job;
    }

    /**
     * 读全局、写按归属：管理员不受限制，普通 HR 只能写自己负责的职位。
     */
    public void assertWritable(SessionUser current, Job job) {
        if (current.isHrAdmin()) {
            return;
        }
        if (!current.getUserId().equals(job.getOwnerHrId())) {
            throw BizException.of(ErrorCode.NOT_JOB_OWNER);
        }
    }

    public boolean canWrite(SessionUser current, Long ownerHrId) {
        return current.isHrAdmin() || current.getUserId().equals(ownerHrId);
    }

    public boolean isMineScope(SessionUser current, String scope) {
        if ("ALL".equalsIgnoreCase(scope)) {
            return false;
        }
        if ("MINE".equalsIgnoreCase(scope)) {
            return true;
        }
        // 未指定时：普通 HR 默认只看自己负责的，管理员默认看全部（§10.3、§10.4）
        return !current.isHrAdmin();
    }

    /* ---------------- 内部方法 ---------------- */

    private Long resolveOwner(SessionUser current, Long requestedOwner, Long fallback) {
        if (current.isHrAdmin() && requestedOwner != null) {
            hrUserService.requireEnabledHr(requestedOwner);
            return requestedOwner;
        }
        return fallback;
    }

    private void applyRequest(Job job, JobRequest request) {
        job.setTitle(request.getTitle().trim());
        job.setRecruitmentType(request.getRecruitmentType());
        job.setCategoryId(request.getCategoryId());
        job.setLocationId(request.getLocationId());
        job.setHeadcount(request.getHeadcount());
        job.setEducation(request.getEducation());
        job.setDuty(HtmlSanitizer.richText(request.getDuty()));
        job.setRequirement(HtmlSanitizer.richText(request.getRequirement()));

        // 与招聘类型无关的字段一律置空，不报错（§10.3）
        if (request.getRecruitmentType() == RecruitmentType.SOCIAL) {
            job.setExperience(request.getExperience());
            job.setGraduationYear(null);
            job.setTargetAudience(null);
        } else {
            job.setExperience(null);
            job.setGraduationYear(request.getGraduationYear());
            job.setTargetAudience(request.getTargetAudience());
        }
    }

    private void validateTypeFields(JobRequest request) {
        if (!JobFieldOptions.validEducation(request.getEducation())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "学历要求取值不合法", java.util.List.of("education"));
        }
        if (request.getRecruitmentType() == RecruitmentType.SOCIAL) {
            if (request.getExperience() == null || request.getExperience().isBlank()) {
                throw new BizException(ErrorCode.PARAM_INVALID, "社会招聘必须填写工作经验要求",
                        java.util.List.of("experience"));
            }
            if (!JobFieldOptions.validExperience(request.getExperience())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "工作经验要求取值不合法",
                        java.util.List.of("experience"));
            }
        } else {
            if (request.getGraduationYear() == null || request.getGraduationYear().isBlank()) {
                throw new BizException(ErrorCode.PARAM_INVALID, "校园招聘必须填写毕业年份要求",
                        java.util.List.of("graduationYear"));
            }
            if (request.getTargetAudience() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "校园招聘必须选择招聘对象",
                        java.util.List.of("targetAudience"));
            }
        }
    }

    /**
     * 生成投递用的职位快照（D6、§13.2）。
     */
    public Map<String, Object> buildSnapshot(JobRowVO row) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("jobId", row.getId());
        snapshot.put("title", row.getTitle());
        snapshot.put("recruitmentType", row.getRecruitmentType() == null ? null : row.getRecruitmentType().name());
        snapshot.put("categoryName", row.getCategoryName());
        snapshot.put("locationName", row.getLocationName());
        snapshot.put("headcount", row.getHeadcount());
        snapshot.put("education", row.getEducation());
        snapshot.put("experience", row.getExperience());
        snapshot.put("graduationYear", row.getGraduationYear());
        snapshot.put("targetAudience", row.getTargetAudience() == null ? null : row.getTargetAudience().name());
        snapshot.put("jobVersion", row.getVersion());
        Job job = jobMapper.selectById(row.getId());
        snapshot.put("duty", job == null ? null : job.getDuty());
        snapshot.put("requirement", job == null ? null : job.getRequirement());
        return snapshot;
    }
}
