package com.zkteco.recruit.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("candidate_profile")
public class CandidateProfile {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** = sys_user.id */
    private Long userId;
    private String name;
    private String phone;
    private String gender;
    private String city;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
