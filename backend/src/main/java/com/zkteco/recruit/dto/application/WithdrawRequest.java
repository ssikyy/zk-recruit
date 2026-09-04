package com.zkteco.recruit.dto.application;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 撤回投递（§9.6）。原因选填，写入 application_log.remark。
 */
@Data
public class WithdrawRequest {

    /** FOUND_OTHER_JOB / WRONG_APPLY / NOT_INTERESTED / OTHER */
    @Size(max = 40)
    private String reason;

    @Size(max = 200, message = "备注不能超过 200 字")
    private String remark;
}
