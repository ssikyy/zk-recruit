package com.zkteco.recruit.service;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zkteco.recruit.common.BizException;
import com.zkteco.recruit.common.ErrorCode;
import com.zkteco.recruit.common.PageQuery;
import com.zkteco.recruit.common.PageResult;
import com.zkteco.recruit.domain.entity.SysUser;
import com.zkteco.recruit.domain.enums.EnableStatus;
import com.zkteco.recruit.domain.enums.UserRole;
import com.zkteco.recruit.dto.hruser.HrUserRequest;
import com.zkteco.recruit.mapper.JobMapper;
import com.zkteco.recruit.mapper.SysUserMapper;
import com.zkteco.recruit.service.support.OperationLogService;

/**
 * HR 账号管理（D9、§10.8）。仅管理员 HR 可访问。
 * <p>
 * 三条硬规则：
 * 1. 停用前必须先转移其名下全部职位（6002）；
 * 2. 系统必须始终保留至少一个启用状态的管理员（6003）；
 * 3. 管理员不能取消自己的管理员位。
 */
@Service
public class HrUserService {

    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SysUserMapper userMapper;
    private final JobMapper jobMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    public HrUserService(SysUserMapper userMapper,
                         JobMapper jobMapper,
                         PasswordEncoder passwordEncoder,
                         OperationLogService operationLogService) {
        this.userMapper = userMapper;
        this.jobMapper = jobMapper;
        this.passwordEncoder = passwordEncoder;
        this.operationLogService = operationLogService;
    }

    public PageResult<Map<String, Object>> page(PageQuery query, String status) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<SysUser>()
                .eq("role", UserRole.HR.name())
                .orderByDesc("hr_admin")
                .orderByAsc("id");
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        Page<SysUser> page = userMapper.selectPage(
                new Page<>(query.normalizedPage(), query.normalizedSize()), wrapper);
        List<Map<String, Object>> rows = page.getRecords().stream().map(this::toVo).toList();
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 负责人下拉，只返回启用状态的 HR（§15.4） */
    public List<Map<String, Object>> options() {
        return userMapper.selectList(new QueryWrapper<SysUser>()
                        .eq("role", UserRole.HR.name())
                        .eq("status", EnableStatus.ENABLED.name())
                        .orderByAsc("id"))
                .stream()
                .map(user -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", user.getId());
                    item.put("name", user.getName());
                    item.put("hrAdmin", user.isAdmin());
                    return item;
                })
                .toList();
    }

    @Transactional
    public Map<String, Object> create(Long operatorId, HrUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userMapper.findByEmail(email) != null) {
            throw BizException.of(ErrorCode.HR_EMAIL_DUPLICATED);
        }
        String rawPassword = request.getPassword() == null || request.getPassword().isBlank()
                ? generatePassword()
                : request.getPassword();

        SysUser user = new SysUser();
        user.setEmail(email);
        user.setName(request.getName().trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.HR);
        user.setHrAdmin(Boolean.TRUE.equals(request.getHrAdmin()));
        user.setStatus(EnableStatus.ENABLED);
        userMapper.insert(user);

        operationLogService.record(operatorId, OperationLogService.MODULE_HR_USER, "CREATE",
                user.getId(), Map.of("email", email, "hrAdmin", user.isAdmin()));

        Map<String, Object> result = toVo(user);
        // 临时密码只在本次响应中返回一次，不落库明文（§10.8）
        result.put("temporaryPassword", rawPassword);
        return result;
    }

    @Transactional
    public void update(Long operatorId, Long id, HrUserRequest request) {
        SysUser user = requireHr(id);
        String email = request.getEmail().trim().toLowerCase();
        SysUser sameEmail = userMapper.findByEmail(email);
        if (sameEmail != null && !sameEmail.getId().equals(id)) {
            throw BizException.of(ErrorCode.EMAIL_DUPLICATED);
        }

        boolean targetAdmin = Boolean.TRUE.equals(request.getHrAdmin());
        if (user.isAdmin() && !targetAdmin) {
            if (id.equals(operatorId)) {
                throw BizException.of(ErrorCode.ADMIN_REQUIRED_AT_LEAST_ONE, "不能取消自己的管理员权限");
            }
            assertOtherAdminExists(id);
        }

        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setHrAdmin(targetAdmin);
        userMapper.updateById(user);

        operationLogService.record(operatorId, OperationLogService.MODULE_HR_USER, "UPDATE",
                id, Map.of("email", email, "hrAdmin", targetAdmin));
    }

    @Transactional
    public void updateStatus(Long operatorId, Long id, EnableStatus status) {
        SysUser user = requireHr(id);
        if (status == EnableStatus.DISABLED) {
            if (id.equals(operatorId)) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "不能停用自己的账号");
            }
            int ownedJobs = jobMapper.countByOwner(id);
            if (ownedJobs > 0) {
                throw new BizException(ErrorCode.HR_STILL_OWNS_JOBS,
                        "该 HR 仍负责 " + ownedJobs + " 个职位，请先转移后再停用");
            }
            if (user.isAdmin()) {
                assertOtherAdminExists(id);
            }
        }
        user.setStatus(status);
        userMapper.updateById(user);
        operationLogService.record(operatorId, OperationLogService.MODULE_HR_USER, "STATUS",
                id, Map.of("status", status.name()));
    }

    /**
     * 重置 HR 密码，返回一次性明文（§10.8）。
     */
    @Transactional
    public String resetHrPassword(Long operatorId, Long id) {
        SysUser user = requireHr(id);
        String rawPassword = generatePassword();
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userMapper.updateById(user);
        operationLogService.record(operatorId, OperationLogService.MODULE_HR_USER, "RESET_PASSWORD",
                id, Map.of("email", user.getEmail()));
        return rawPassword;
    }

    /**
     * 按邮箱重置求职者密码，替代自助找回（D4、§8.5）。
     */
    @Transactional
    public String resetCandidatePassword(Long operatorId, String email) {
        SysUser user = userMapper.findByEmail(email == null ? "" : email.trim().toLowerCase());
        if (user == null || user.getRole() != UserRole.CANDIDATE) {
            throw BizException.of(ErrorCode.NOT_FOUND, "该邮箱对应的求职者账号不存在");
        }
        String rawPassword = generatePassword();
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userMapper.updateById(user);
        operationLogService.record(operatorId, OperationLogService.MODULE_HR_USER, "RESET_CANDIDATE_PASSWORD",
                user.getId(), Map.of("email", user.getEmail()));
        return rawPassword;
    }

    public SysUser requireEnabledHr(Long id) {
        SysUser user = requireHr(id);
        if (!user.isEnabled()) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "所选 HR 已停用，不能作为职位负责人");
        }
        return user;
    }

    private SysUser requireHr(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null || user.getRole() != UserRole.HR) {
            throw BizException.of(ErrorCode.NOT_FOUND, "HR 账号不存在");
        }
        return user;
    }

    private void assertOtherAdminExists(Long excludeId) {
        if (userMapper.countOtherEnabledAdmins(excludeId) <= 0) {
            throw BizException.of(ErrorCode.ADMIN_REQUIRED_AT_LEAST_ONE);
        }
    }

    private Map<String, Object> toVo(SysUser user) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", user.getId());
        vo.put("name", user.getName());
        vo.put("email", user.getEmail());
        vo.put("hrAdmin", user.isAdmin());
        vo.put("status", user.getStatus());
        vo.put("ownedJobCount", jobMapper.countByOwner(user.getId()));
        vo.put("lastLoginAt", user.getLastLoginAt());
        vo.put("createdAt", user.getCreatedAt());
        return vo;
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }
}
