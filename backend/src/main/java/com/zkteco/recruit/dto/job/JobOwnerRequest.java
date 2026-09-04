package com.zkteco.recruit.dto.job;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 转移职位负责人（§10.3），仅管理员 HR 可用。
 */
@Data
public class JobOwnerRequest {

    @NotNull(message = "请选择新的负责人")
    private Long ownerHrId;

    @NotNull(message = "缺少版本号")
    private Integer version;
}
