package com.zkteco.recruit.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zkteco.recruit.domain.enums.JobStatus;
import com.zkteco.recruit.domain.enums.RecruitmentType;
import com.zkteco.recruit.domain.enums.TargetAudience;

import lombok.Data;

@Data
@TableName("job")
public class Job {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private RecruitmentType recruitmentType;
    private Long categoryId;
    private Long locationId;
    /** 职位负责人，写权限的判定依据（D10） */
    private Long ownerHrId;
    private Integer headcount;
    private String education;
    /** 社招必填 */
    private String experience;
    /** 校招必填 */
    private String graduationYear;
    /** 校招必填 */
    private TargetAudience targetAudience;
    private String duty;
    private String requirement;
    private JobStatus status;
    /** 乐观锁，同时作为快照版本标识 */
    private Integer version;
    private LocalDateTime publishedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
