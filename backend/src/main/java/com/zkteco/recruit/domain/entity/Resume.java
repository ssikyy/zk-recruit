package com.zkteco.recruit.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 在线简历（可变）。投递时会被复制成快照，之后的修改不影响历史投递（§13.2）。
 */
@Data
@TableName("resume")
public class Resume {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long candidateId;
    /** 在线简历 JSON */
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
