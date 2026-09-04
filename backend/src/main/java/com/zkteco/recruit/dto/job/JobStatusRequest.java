package com.zkteco.recruit.dto.job;

import com.zkteco.recruit.domain.enums.JobStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobStatusRequest {

    @NotNull(message = "请指定目标状态")
    private JobStatus targetStatus;

    @NotNull(message = "缺少版本号")
    private Integer version;
}
