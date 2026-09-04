package com.zkteco.recruit.common;

/**
 * 业务异常，统一携带错误码，由 {@link GlobalExceptionHandler} 转成标准响应。
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object data;

    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null);
    }

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BizException(ErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode);
    }

    public static BizException of(ErrorCode errorCode, String message) {
        return new BizException(errorCode, message);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object getData() {
        return data;
    }
}
