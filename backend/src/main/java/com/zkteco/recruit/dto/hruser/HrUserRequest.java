package com.zkteco.recruit.dto.hruser;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HrUserRequest {

    @NotBlank(message = "请填写姓名")
    @Size(min = 2, max = 20, message = "姓名长度需为 2-20 位")
    private String name;

    @NotBlank(message = "请填写邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 120)
    private String email;

    /** 新增时可留空，由系统生成临时密码 */
    @Size(min = 8, max = 20, message = "密码长度需为 8-20 位")
    private String password;

    private Boolean hrAdmin;
}
