package com.zkteco.recruit.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zkteco.recruit.domain.enums.EnableStatus;

import lombok.Data;

@Data
@TableName("job_category")
public class JobCategory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer sortOrder;
    private EnableStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
