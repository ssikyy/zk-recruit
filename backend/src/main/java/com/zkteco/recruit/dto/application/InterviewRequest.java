package com.zkteco.recruit.dto.application;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zkteco.recruit.domain.enums.InterviewMethod;
import com.zkteco.recruit.domain.enums.InterviewResult;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 面试安排与评价（§10.6）。改期为更新原记录；填写结果不会自动变更投递状态（D16）。
 */
@Data
public class InterviewRequest {

    @NotNull(message = "请选择面试时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime interviewTime;

    @NotNull(message = "请选择面试方式")
    private InterviewMethod method;

    @NotBlank(message = "请填写面试地点或会议链接")
    @Size(max = 300)
    private String address;

    @Size(max = 500)
    private String contactNote;

    /** 仅 HR 可见 */
    @Size(max = 2000)
    private String evaluation;

    private InterviewResult result;

    @NotNull(message = "缺少版本号")
    private Integer version;
}
