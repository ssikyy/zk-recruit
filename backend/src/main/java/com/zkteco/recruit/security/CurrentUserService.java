package com.zkteco.recruit.security;

import org.springframework.stereotype.Component;

import com.zkteco.recruit.common.BizException;
import com.zkteco.recruit.common.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 读取当前会话用户。请求作用域代理由 Spring 注入，不使用 ThreadLocal。
 */
@Component
public class CurrentUserService {

    private final HttpServletRequest request;

    public CurrentUserService(HttpServletRequest request) {
        this.request = request;
    }

    public SessionUser current() {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SessionUser.SESSION_KEY);
        return value instanceof SessionUser user ? user : null;
    }

    public SessionUser require() {
        SessionUser user = current();
        if (user == null) {
            throw BizException.of(ErrorCode.UNAUTHENTICATED);
        }
        return user;
    }

    public SessionUser requireCandidate() {
        SessionUser user = require();
        if (!user.isCandidate()) {
            throw BizException.of(ErrorCode.FORBIDDEN, "该操作仅求职者可用");
        }
        return user;
    }

    public SessionUser requireHr() {
        SessionUser user = require();
        if (!user.isHr()) {
            throw BizException.of(ErrorCode.FORBIDDEN, "该操作仅 HR 可用");
        }
        return user;
    }

    public SessionUser requireAdmin() {
        SessionUser user = requireHr();
        if (!user.isHrAdmin()) {
            throw BizException.of(ErrorCode.NEED_ADMIN);
        }
        return user;
    }

    public Long currentUserId() {
        SessionUser user = current();
        return user == null ? null : user.getUserId();
    }

    public void login(SessionUser user) {
        // 先失效旧会话，防止会话固定攻击
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(SessionUser.SESSION_KEY, user);
    }

    public void logout() {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public void refresh(SessionUser user) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(SessionUser.SESSION_KEY, user);
        }
    }
}
