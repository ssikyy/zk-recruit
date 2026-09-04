package com.zkteco.recruit.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zkteco.recruit.domain.enums.EnableStatus;
import com.zkteco.recruit.domain.enums.UserRole;

import lombok.Data;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String passwordHash;
    private String name;
    private UserRole role;
    private Boolean hrAdmin;
    private EnableStatus status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isEnabled() {
        return status == EnableStatus.ENABLED;
    }

    public boolean isAdmin() {
        return Boolean.TRUE.equals(hrAdmin);
    }
}
