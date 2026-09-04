package com.zkteco.recruit.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zkteco.recruit.domain.enums.InterviewMethod;
import com.zkteco.recruit.domain.enums.InterviewResult;

import lombok.Data;

/**
 * 面试信息，与投递 1:1（D15）。改期为更新原记录，不新增。
 */
@Data
@TableName("interview_info")
public class InterviewInfo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    private LocalDateTime interviewTime;
    private InterviewMethod method;
    private String address;
    private String contactNote;
    /** 仅 HR 可见 */
    private String evaluation;
    /** 为空表示"已约未面"，工作台待面试口径依赖此字段（§10.2） */
    private InterviewResult result;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
