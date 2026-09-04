package com.zkteco.recruit.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求（§8.3）：仅 4 项 + 1 个勾选，不含任何验证码/验证环节。
 * 请求体中的 role 一律被忽略，注册结果只能是求职者。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "请填写姓名")
    @Size(min = 2, max = 20, message = "姓名长度需为 2-20 位")
    private String name;

    @NotBlank(message = "请填写邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 120)
    private String email;

    @NotBlank(message = "请填写密码")
    @Size(min = 8, max = 20, message = "密码长度需为 8-20 位")
    private String password;

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    @AssertTrue(message = "请先同意隐私政策")
    private Boolean agreePrivacy;
}
