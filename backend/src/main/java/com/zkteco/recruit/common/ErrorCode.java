package com.zkteco.recruit.common;

import org.springframework.http.HttpStatus;

/**
 * 错误码表，与需求文档 §15.9 一一对应。
 */
public enum ErrorCode {

    OK(0, HttpStatus.OK, "ok"),

    PARAM_INVALID(1001, HttpStatus.BAD_REQUEST, "参数校验失败"),
    UNAUTHENTICATED(1002, HttpStatus.UNAUTHORIZED, "未登录或会话已失效"),
    FORBIDDEN(1003, HttpStatus.FORBIDDEN, "无权限访问"),
    NOT_FOUND(1004, HttpStatus.NOT_FOUND, "资源不存在"),
    INTERNAL_ERROR(1005, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误"),
    CSRF_INVALID(1006, HttpStatus.FORBIDDEN, "CSRF Token 缺失或不匹配"),
    NEED_ADMIN(1007, HttpStatus.FORBIDDEN, "需要管理员 HR 权限"),

    EMAIL_DUPLICATED(2001, HttpStatus.CONFLICT, "该邮箱已被注册或被其他账号占用"),
    PASSWORD_NOT_MATCH(2002, HttpStatus.BAD_REQUEST, "两次输入的密码不一致"),
    LOGIN_FAILED(2003, HttpStatus.UNAUTHORIZED, "邮箱或密码错误"),
    LOGIN_LOCKED(2004, HttpStatus.TOO_MANY_REQUESTS, "登录失败次数过多，账号已临时锁定"),
    TOO_MANY_REQUESTS(2005, HttpStatus.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试"),
    ACCOUNT_DISABLED(2006, HttpStatus.FORBIDDEN, "账号已停用，请联系管理员"),

    JOB_NOT_PUBLISHED(3001, HttpStatus.CONFLICT, "该职位已停止招聘"),
    APPLY_DUPLICATED(3002, HttpStatus.CONFLICT, "您已投递该职位，不可重复投递"),
    RESUME_INCOMPLETE(3003, HttpStatus.PRECONDITION_FAILED, "资料或简历不完整"),
    VERSION_CONFLICT(3004, HttpStatus.CONFLICT, "该记录已被更新，请刷新后重试"),
    STATUS_TRANSITION_INVALID(3005, HttpStatus.CONFLICT, "状态转换不合法或记录已撤回不可操作"),
    WITHDRAW_NOT_ALLOWED(3006, HttpStatus.CONFLICT, "当前状态不允许撤回"),
    APPLY_LIMIT_EXCEEDED(3007, HttpStatus.CONFLICT, "该职位投递次数已达上限"),
    NOT_JOB_OWNER(3008, HttpStatus.FORBIDDEN, "仅职位负责人可操作"),

    FILE_TYPE_UNSUPPORTED(4001, HttpStatus.BAD_REQUEST, "文件类型不支持"),
    FILE_TOO_LARGE(4002, HttpStatus.PAYLOAD_TOO_LARGE, "文件大小超过限制"),
    FILE_IO_ERROR(4003, HttpStatus.INTERNAL_SERVER_ERROR, "文件存储或读取失败"),

    DICT_IN_USE(5001, HttpStatus.CONFLICT, "该字典项已被职位引用，不可删除"),
    DICT_NAME_DUPLICATED(5002, HttpStatus.CONFLICT, "字典项名称重复"),

    HR_EMAIL_DUPLICATED(6001, HttpStatus.CONFLICT, "该 HR 邮箱已存在"),
    HR_STILL_OWNS_JOBS(6002, HttpStatus.CONFLICT, "该 HR 仍负责职位，请先转移后再停用"),
    ADMIN_REQUIRED_AT_LEAST_ONE(6003, HttpStatus.CONFLICT, "必须保留至少一个启用状态的管理员 HR");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
