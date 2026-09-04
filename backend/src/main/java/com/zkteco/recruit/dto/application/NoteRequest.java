package com.zkteco.recruit.dto.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 内部备注（§10.5）。独立操作，不改变投递状态。
 */
@Data
public class NoteRequest {

    @Size(max = 2000, message = "备注不能超过 2000 字")
    private String note;

    @NotNull(message = "缺少版本号")
    private Integer version;
}
