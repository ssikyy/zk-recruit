package com.zkteco.recruit.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zkteco.recruit.domain.enums.ApplicationStatus;

import lombok.Data;

/**
 * 投递操作记录。operator_type 用于区分候选人撤回与 HR 操作（§14）。
 */
@Data
@TableName("application_log")
public class ApplicationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    private String action;
    private ApplicationStatus fromStatus;
    private ApplicationStatus toStatus;
    private Long operatorId;
    private String operatorType;
    private String remark;
    private LocalDateTime createdAt;
}
