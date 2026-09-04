package com.zkteco.recruit.common;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResult<Object>> handleBiz(BizException ex) {
        ErrorCode code = ex.getErrorCode();
        if (code.getHttpStatus().is5xxServerError()) {
            log.error("业务异常 code={} msg={}", code.getCode(), ex.getMessage(), ex);
        } else {
            log.debug("业务拦截 code={} msg={}", code.getCode(), ex.getMessage());
        }
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResult.error(code, ex.getMessage(), ex.getData()));
    }

    /**
     * 并发重复投递由唯一约束兜底，必须返回 3002 而不是 500（§13.2）。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResult<Object>> handleDuplicateKey(DuplicateKeyException ex) {
        log.debug("唯一约束拦截: {}", ex.getMessage());
        ErrorCode code = ErrorCode.APPLY_DUPLICATED;
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResult.error(code, code.getMessage(), null));
    }

    @ExceptionHandler(CsrfException.class)
    public ResponseEntity<ApiResult<Object>> handleCsrf(CsrfException ex) {
        ErrorCode code = ErrorCode.CSRF_INVALID;
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResult.error(code, code.getMessage(), null));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResult<Object>> handleValidation(BindException ex) {
        List<String> fields = new ArrayList<>();
        String firstMessage = null;
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.add(error.getField());
            if (firstMessage == null) {
                firstMessage = error.getField() + " " + error.getDefaultMessage();
            }
        }
        ErrorCode code = ErrorCode.PARAM_INVALID;
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResult.error(code, firstMessage == null ? code.getMessage() : firstMessage, fields));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Object>> handleConstraint(ConstraintViolationException ex) {
        List<String> fields = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fields.add(violation.getPropertyPath().toString());
        }
        ErrorCode code = ErrorCode.PARAM_INVALID;
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResult.error(code, ex.getMessage(), fields));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResult<Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        ErrorCode code = ErrorCode.PARAM_INVALID;
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResult.error(code, "缺少参数: " + ex.getParameterName(), List.of(ex.getParameterName())));
    }

    /**
     * 路径不存在时返回 1004，而不是落到兜底的 500。
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResult<Object>> handleNotFound(Exception ex) {
        ErrorCode code = ErrorCode.NOT_FOUND;
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResult.error(code, code.getMessage(), null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<Object>> handleUploadSize(MaxUploadSizeExceededException ex) {
        ErrorCode code = ErrorCode.FILE_TOO_LARGE;
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResult.error(code, code.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Object>> handleOther(Exception ex) {
        log.error("未预期异常", ex);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResult.error(code, code.getMessage(), null));
    }
}
