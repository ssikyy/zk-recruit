package com.zkteco.recruit.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.zkteco.recruit.common.BizException;
import com.zkteco.recruit.common.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 权限校验第一、二层（§16.6）：按路径前缀判定角色与管理员位。
 * <p>
 * 采用"路径前缀 fail-closed"而不是注解，任何新增在受保护前缀下的接口都会自动受控，
 * 不会因为忘记加注解而裸奔。第三层（数据归属）在各 Service 中完成。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final CurrentUserService currentUserService;

    public AuthInterceptor(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (path.startsWith("/api/public/") || path.startsWith("/api/auth/")) {
            return true;
        }

        if (path.startsWith("/api/admin/")) {
            currentUserService.requireAdmin();
            return true;
        }
        if (path.startsWith("/api/hr/")) {
            currentUserService.requireHr();
            return true;
        }
        if (path.startsWith("/api/candidate/")) {
            currentUserService.requireCandidate();
            return true;
        }
        if (path.startsWith("/api/")) {
            // 未归类的接口默认要求登录，避免遗漏
            currentUserService.require();
            return true;
        }
        return true;
    }

    /**
     * 供 Controller 层显式声明用途，保持与拦截器一致的错误码。
     */
    public static void assertTrue(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw BizException.of(errorCode);
        }
    }
}
