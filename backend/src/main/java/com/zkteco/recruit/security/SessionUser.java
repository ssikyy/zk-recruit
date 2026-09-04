package com.zkteco.recruit.security;

import java.io.Serializable;

import com.zkteco.recruit.domain.enums.UserRole;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录态载体，保存在 HttpSession 中（D1）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionUser implements Serializable {

    public static final String SESSION_KEY = "ZK_LOGIN_USER";

    private Long userId;
    private String email;
    private String name;
    private UserRole role;
    private boolean hrAdmin;

    public boolean isHr() {
        return role == UserRole.HR;
    }

    public boolean isCandidate() {
        return role == UserRole.CANDIDATE;
    }
}
