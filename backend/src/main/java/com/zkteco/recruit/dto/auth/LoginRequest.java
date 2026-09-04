package com.zkteco.recruit.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "请填写邮箱")
    private String email;

    @NotBlank(message = "请填写密码")
    private String password;
}
