package com.zkteco.recruit.dto.application;

import com.zkteco.recruit.domain.enums.ApplicationStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * HR 变更投递状态（§10.5）。version 必填，用于条件更新（§12.2）。
 */
@Data
public class StatusChangeRequest {

    @NotNull(message = "请指定目标状态")
    private ApplicationStatus targetStatus;

    @NotNull(message = "缺少版本号")
    private Integer version;

    @Size(max = 200)
    private String remark;
}
