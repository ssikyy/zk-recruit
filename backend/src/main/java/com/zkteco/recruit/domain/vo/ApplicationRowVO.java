package com.zkteco.recruit.domain.vo;

import java.time.LocalDateTime;

import com.zkteco.recruit.domain.enums.ApplicationStatus;
import com.zkteco.recruit.domain.enums.InterviewResult;
import com.zkteco.recruit.domain.enums.RecruitmentType;

import lombok.Data;

/**
 * 投递查询行。HR 列表与求职者列表共用，展示层各自裁剪字段。
 */
@Data
public class ApplicationRowVO {

    private Long id;
    private Long candidateId;
    private String candidateName;
    private String candidatePhone;
    private Long jobId;
    /** 职位当前名称 */
    private String jobTitle;
    private RecruitmentType recruitmentType;
    private String locationName;
    private Long ownerHrId;
    private String ownerName;
    private Integer attemptNo;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime lastHandledAt;
    private LocalDateTime interviewTime;
    private InterviewResult interviewResult;
    /** 投递时的职位快照 JSON，求职者端按 §9.4 展示快照名称 */
    private String jobSnapshot;
}
