package com.zkteco.recruit.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zkteco.recruit.common.ApiResult;
import com.zkteco.recruit.dto.auth.LoginRequest;
import com.zkteco.recruit.dto.auth.MeResponse;
import com.zkteco.recruit.dto.auth.RegisterRequest;
import com.zkteco.recruit.security.CurrentUserService;
import com.zkteco.recruit.security.SessionUser;
import com.zkteco.recruit.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    /**
     * 供 SPA 首次进入时获取 CSRF Token（§15.7）。
     * Spring Security 的 CsrfFilter 会在此请求上下发 XSRF-TOKEN Cookie。
     */
    @GetMapping("/csrf")
    public ApiResult<Map<String, String>> csrf(HttpServletRequest request) {
        Object token = request.getAttribute("org.springframework.security.web.csrf.CsrfToken");
        String value = "";
        if (token instanceof org.springframework.security.web.csrf.CsrfToken csrfToken) {
            value = csrfToken.getToken();
        }
        return ApiResult.ok(Map.of("token", value, "headerName", "X-XSRF-TOKEN"));
    }

    @PostMapping("/register")
    public ApiResult<MeResponse> register(@Valid @RequestBody RegisterRequest request,
                                          HttpServletRequest httpRequest) {
        SessionUser user = authService.register(request, clientIp(httpRequest));
        return ApiResult.ok(MeResponse.from(user));
    }

    @PostMapping("/login")
    public ApiResult<MeResponse> login(@Valid @RequestBody LoginRequest request) {
        SessionUser user = authService.login(request);
        return ApiResult.ok(MeResponse.from(user));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        authService.logout();
        return ApiResult.ok();
    }

    @GetMapping("/me")
    public ApiResult<MeResponse> me() {
        return ApiResult.ok(MeResponse.from(currentUserService.require()));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
