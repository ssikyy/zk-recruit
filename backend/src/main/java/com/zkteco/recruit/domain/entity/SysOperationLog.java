package com.zkteco.recruit.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 管理类操作审计（§16.2）：字典、HR 账号、归属转移、密码重置。
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String module;
    private String action;
    private String targetId;
    private String detail;
    private LocalDateTime createdAt;
}
