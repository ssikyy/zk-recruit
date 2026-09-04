package com.zkteco.recruit.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 附件简历，不可变多版本（§9.3）。
 * 记录一旦写入不允许修改或覆盖，只切换 is_current 标记。
 */
@Data
@TableName("resume_file")
public class ResumeFile {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long candidateId;
    private String fileName;
    /** 相对存储路径，禁止出现在任何响应体中（D7） */
    private String storageKey;
    private Long fileSize;
    private String contentType;
    private Boolean isCurrent;
    private LocalDateTime uploadedAt;
}
