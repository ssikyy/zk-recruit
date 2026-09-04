package com.zkteco.recruit.dto.candidate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 基本资料（§9.1）。手机号允许分次填写，但投递前必填（§9.5）。
 */
@Data
public class ProfileRequest {

    @NotBlank(message = "请填写姓名")
    @Size(min = 2, max = 20, message = "姓名长度需为 2-20 位")
    private String name;

    @NotBlank(message = "请填写邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 20)
    private String gender;

    @Size(max = 50)
    private String city;
}
