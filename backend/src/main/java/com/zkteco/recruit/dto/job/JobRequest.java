package com.zkteco.recruit.dto.job;

import com.zkteco.recruit.domain.enums.RecruitmentType;
import com.zkteco.recruit.domain.enums.TargetAudience;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 职位新增/编辑请求（§10.3）。
 * 按招聘类型的差异必填由 Service 校验：社招必填 experience，
 * 校招必填 graduationYear 与 targetAudience。
 */
@Data
public class JobRequest {

    @NotBlank(message = "请填写职位名称")
    @Size(max = 120, message = "职位名称不能超过 120 字")
    private String title;

    @NotNull(message = "请选择招聘类型")
    private RecruitmentType recruitmentType;

    @NotNull(message = "请选择职位类别")
    private Long categoryId;

    @NotNull(message = "请选择工作地点")
    private Long locationId;

    @NotNull(message = "请填写招聘人数")
    @Min(value = 1, message = "招聘人数至少为 1")
    private Integer headcount;

    @NotBlank(message = "请选择学历要求")
    private String education;

    /** 社招必填 */
    private String experience;

    /** 校招必填 */
    private String graduationYear;

    /** 校招必填 */
    private TargetAudience targetAudience;

    @NotBlank(message = "请填写岗位职责")
    @Size(max = 5000, message = "岗位职责不能超过 5000 字")
    private String duty;

    @NotBlank(message = "请填写任职要求")
    @Size(max = 5000, message = "任职要求不能超过 5000 字")
    private String requirement;

    /** 仅管理员 HR 可指定负责人，普通 HR 传值被忽略 */
    private Long ownerHrId;
}
