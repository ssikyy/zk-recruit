package com.zkteco.recruit.domain.vo;

import java.time.LocalDateTime;

import com.zkteco.recruit.domain.enums.JobStatus;
import com.zkteco.recruit.domain.enums.RecruitmentType;
import com.zkteco.recruit.domain.enums.TargetAudience;

import lombok.Data;

/**
 * 职位查询行（官网列表与 HR 列表共用），已带出字典名与负责人姓名。
 */
@Data
public class JobRowVO {

    private Long id;
    private String title;
    private RecruitmentType recruitmentType;
    private Long categoryId;
    private String categoryName;
    private Long locationId;
    private String locationName;
    private Long ownerHrId;
    private String ownerName;
    private Integer headcount;
    private String education;
    private String experience;
    private String graduationYear;
    private TargetAudience targetAudience;
    private JobStatus status;
    private Integer version;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    /** 有效投递数（排除已撤回） */
    private Integer applicationCount;
    /** 富文本，仅详情查询返回 */
    private String duty;
    private String requirement;
}
