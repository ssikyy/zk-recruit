package com.zkteco.recruit.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zkteco.recruit.common.ApiResult;
import com.zkteco.recruit.common.BizException;
import com.zkteco.recruit.common.ErrorCode;
import com.zkteco.recruit.common.PageQuery;
import com.zkteco.recruit.common.PageResult;
import com.zkteco.recruit.domain.enums.EnableStatus;
import com.zkteco.recruit.dto.dict.DictItemRequest;
import com.zkteco.recruit.dto.dict.DictItemVO;
import com.zkteco.recruit.dto.hruser.HrUserRequest;
import com.zkteco.recruit.security.CurrentUserService;
import com.zkteco.recruit.service.DictService;
import com.zkteco.recruit.service.HrUserService;

import jakarta.validation.Valid;

/**
 * 管理员专属接口（§15.5）。拦截器已保证 hr_admin，越权返回 1007。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final DictService dictService;
    private final HrUserService hrUserService;
    private final CurrentUserService currentUserService;

    public AdminController(DictService dictService,
                           HrUserService hrUserService,
                           CurrentUserService currentUserService) {
        this.dictService = dictService;
        this.hrUserService = hrUserService;
        this.currentUserService = currentUserService;
    }

    private Long me() {
        return currentUserService.requireAdmin().getUserId();
    }

    /* ---------------- 职位类别 ---------------- */

    @GetMapping("/job-categories")
    public ApiResult<List<DictItemVO>> categories() {
        me();
        return ApiResult.ok(dictService.adminCategories());
    }

    @PostMapping("/job-categories")
    public ApiResult<Map<String, Object>> createCategory(@Valid @RequestBody DictItemRequest request) {
        return ApiResult.ok(Map.of("id", dictService.createCategory(me(), request)));
    }

    @PutMapping("/job-categories/{id}")
    public ApiResult<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody DictItemRequest request) {
        dictService.updateCategory(me(), id, request);
        return ApiResult.ok();
    }

    @PutMapping("/job-categories/{id}/status")
    public ApiResult<Void> updateCategoryStatus(@PathVariable Long id, @RequestParam EnableStatus status) {
        dictService.updateCategoryStatus(me(), id, status);
        return ApiResult.ok();
    }

    /** 字典项不提供物理删除，被引用时明确拒绝（§10.7、错误码 5001） */
    @DeleteMapping("/job-categories/{id}")
    public ApiResult<Void> deleteCategory(@PathVariable Long id) {
        me();
        throw BizException.of(ErrorCode.DICT_IN_USE, "字典项不支持删除，请改为停用");
    }

    /* ---------------- 工作地点 ---------------- */

    @GetMapping("/job-locations")
    public ApiResult<List<DictItemVO>> locations() {
        me();
        return ApiResult.ok(dictService.adminLocations());
    }

    @PostMapping("/job-locations")
    public ApiResult<Map<String, Object>> createLocation(@Valid @RequestBody DictItemRequest request) {
        return ApiResult.ok(Map.of("id", dictService.createLocation(me(), request)));
    }

    @PutMapping("/job-locations/{id}")
    public ApiResult<Void> updateLocation(@PathVariable Long id, @Valid @RequestBody DictItemRequest request) {
        dictService.updateLocation(me(), id, request);
        return ApiResult.ok();
    }

    @PutMapping("/job-locations/{id}/status")
    public ApiResult<Void> updateLocationStatus(@PathVariable Long id, @RequestParam EnableStatus status) {
        dictService.updateLocationStatus(me(), id, status);
        return ApiResult.ok();
    }

    @DeleteMapping("/job-locations/{id}")
    public ApiResult<Void> deleteLocation(@PathVariable Long id) {
        me();
        throw BizException.of(ErrorCode.DICT_IN_USE, "字典项不支持删除，请改为停用");
    }

    /* ---------------- HR 账号 ---------------- */

    @GetMapping("/hr-users")
    public ApiResult<PageResult<Map<String, Object>>> hrUsers(@RequestParam(required = false) String status,
                                                              @ModelAttribute PageQuery pageQuery) {
        me();
        return ApiResult.ok(hrUserService.page(pageQuery, status));
    }

    @PostMapping("/hr-users")
    public ApiResult<Map<String, Object>> createHrUser(@Valid @RequestBody HrUserRequest request) {
        return ApiResult.ok(hrUserService.create(me(), request));
    }

    @PutMapping("/hr-users/{id}")
    public ApiResult<Void> updateHrUser(@PathVariable Long id, @Valid @RequestBody HrUserRequest request) {
        hrUserService.update(me(), id, request);
        return ApiResult.ok();
    }

    @PutMapping("/hr-users/{id}/status")
    public ApiResult<Void> updateHrUserStatus(@PathVariable Long id, @RequestParam EnableStatus status) {
        hrUserService.updateStatus(me(), id, status);
        return ApiResult.ok();
    }

    /** 重置 HR 密码，临时密码只返回一次（§10.8） */
    @PostMapping("/hr-users/{id}/reset-password")
    public ApiResult<Map<String, Object>> resetHrPassword(@PathVariable Long id) {
        return ApiResult.ok(Map.of("temporaryPassword", hrUserService.resetHrPassword(me(), id)));
    }

    /** 按邮箱重置求职者密码，替代自助找回（§8.5） */
    @PostMapping("/candidates/reset-password")
    public ApiResult<Map<String, Object>> resetCandidatePassword(@RequestBody Map<String, String> body) {
        String email = body == null ? null : body.get("email");
        return ApiResult.ok(Map.of("temporaryPassword", hrUserService.resetCandidatePassword(me(), email)));
    }
}
