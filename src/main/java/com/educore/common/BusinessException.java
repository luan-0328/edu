package com.educore.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final ApiErrorCode code;
    private final HttpStatus status;
    public BusinessException(ApiErrorCode code, String message, HttpStatus status) { super(message); this.code = code; this.status = status; }
    public BusinessException(ApiErrorCode code, String message) { this(code, message, HttpStatus.BAD_REQUEST); }
    public ApiErrorCode getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
