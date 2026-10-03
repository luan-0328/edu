package com.educore.common;

public record Result<T>(String code, String message, T data, String requestId) {
    public static <T> Result<T> success(T data, String requestId) { return new Result<>("SUCCESS", "操作成功", data, requestId); }
    public static Result<Void> failure(ApiErrorCode code, String message, String requestId) { return new Result<>(code.name(), message, null, requestId); }
}
