package com.zkteco.recruit.common;

/**
 * 统一响应结构（§15.8）：{ code, message, data }
 */
public class ApiResult<T> {

    private int code;
    private String message;
    private T data;

    public ApiResult() {
    }

    public ApiResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(ErrorCode.OK.getCode(), "ok", data);
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<>(ErrorCode.OK.getCode(), "ok", null);
    }

    public static <T> ApiResult<T> error(ErrorCode errorCode, String message, T data) {
        return new ApiResult<>(errorCode.getCode(), message, data);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
