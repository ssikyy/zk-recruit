package com.zkteco.recruit.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zkteco.recruit.domain.enums.ApplicationStatus;

import lombok.Data;

/**
 * 投递记录。快照字段一旦写入永不修改（D6、§13.2）。
 */
@Data
@TableName("job_application")
public class JobApplication {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long candidateId;
    private Long jobId;
    /** 第 N 次投递，与 (candidate_id, job_id) 组成唯一约束 */
    private Integer attemptNo;
    private ApplicationStatus status;
    private Integer version;
    private LocalDateTime appliedAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime lastHandledAt;
    /** 在线简历快照 JSON */
    private String resumeSnapshot;
    /** 附件快照，指向不可变的 resume_file */
    private Long resumeFileId;
    /** 职位快照 JSON */
    private String jobSnapshot;
    /** 候选人联系信息快照 JSON */
    private String candidateSnapshot;
    /** 内部备注，仅 HR 可见 */
    private String hrNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
