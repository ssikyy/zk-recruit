package com.zkteco.recruit.dto.auth;

import com.zkteco.recruit.domain.enums.UserRole;
import com.zkteco.recruit.security.SessionUser;

import lombok.Data;

/**
 * {@code GET /api/auth/me} 响应（§15.2）。
 */
@Data
public class MeResponse {

    private Long userId;
    private String name;
    private String email;
    private UserRole role;
    private boolean hrAdmin;

    public static MeResponse from(SessionUser user) {
        MeResponse response = new MeResponse();
        response.setUserId(user.getUserId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setHrAdmin(user.isHrAdmin());
        return response;
    }
}
