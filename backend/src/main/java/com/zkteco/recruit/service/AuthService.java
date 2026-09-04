package com.zkteco.recruit.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zkteco.recruit.common.BizException;
import com.zkteco.recruit.common.ErrorCode;
import com.zkteco.recruit.config.AppProperties;
import com.zkteco.recruit.domain.entity.CandidateProfile;
import com.zkteco.recruit.domain.entity.SysUser;
import com.zkteco.recruit.domain.enums.EnableStatus;
import com.zkteco.recruit.domain.enums.UserRole;
import com.zkteco.recruit.dto.auth.LoginRequest;
import com.zkteco.recruit.dto.auth.RegisterRequest;
import com.zkteco.recruit.mapper.CandidateProfileMapper;
import com.zkteco.recruit.mapper.SysUserMapper;
import com.zkteco.recruit.security.CurrentUserService;
import com.zkteco.recruit.security.RateLimiter;
import com.zkteco.recruit.security.SessionUser;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final CandidateProfileMapper profileMapper;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final RateLimiter rateLimiter;
    private final AppProperties appProperties;

    public AuthService(SysUserMapper userMapper,
                       CandidateProfileMapper profileMapper,
                       PasswordEncoder passwordEncoder,
                       CurrentUserService currentUserService,
                       RateLimiter rateLimiter,
                       AppProperties appProperties) {
        this.userMapper = userMapper;
        this.profileMapper = profileMapper;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.rateLimiter = rateLimiter;
        this.appProperties = appProperties;
    }

    /**
     * 公开注册只能创建求职者（§8.3）。注册成功后自动登录。
     */
    @Transactional
    public SessionUser register(RegisterRequest request, String clientIp) {
        rateLimiter.assertHourlyLimit("register", clientIp,
                appProperties.getSecurity().getRegisterIpLimitPerHour(),
                "注册过于频繁，请稍后再试");

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw BizException.of(ErrorCode.PASSWORD_NOT_MATCH);
        }
        String email = normalizeEmail(request.getEmail());
        if (userMapper.findByEmail(email) != null) {
            throw BizException.of(ErrorCode.EMAIL_DUPLICATED);
        }

        SysUser user = new SysUser();
        user.setEmail(email);
        user.setName(request.getName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        // 角色强制为求职者，忽略请求中任何 role 字段
        user.setRole(UserRole.CANDIDATE);
        user.setHrAdmin(false);
        user.setStatus(EnableStatus.ENABLED);
        userMapper.insert(user);

        CandidateProfile profile = new CandidateProfile();
        profile.setUserId(user.getId());
        profile.setName(user.getName());
        profileMapper.insert(profile);

        SessionUser sessionUser = toSessionUser(user);
        currentUserService.login(sessionUser);
        return sessionUser;
    }

    /**
     * 登录（§8.2）。邮箱不存在与密码错误返回同一提示，避免账号枚举。
     */
    public SessionUser login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        rateLimiter.assertLoginNotLocked(email);

        SysUser user = userMapper.findByEmail(email);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            rateLimiter.recordLoginFailure(email);
            throw BizException.of(ErrorCode.LOGIN_FAILED);
        }
        if (!user.isEnabled()) {
            throw BizException.of(ErrorCode.ACCOUNT_DISABLED);
        }

        rateLimiter.clearLoginFailure(email);
        userMapper.touchLastLogin(user.getId(), LocalDateTime.now());

        SessionUser sessionUser = toSessionUser(user);
        currentUserService.login(sessionUser);
        return sessionUser;
    }

    public void logout() {
        currentUserService.logout();
    }

    private SessionUser toSessionUser(SysUser user) {
        return new SessionUser(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.isAdmin());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
