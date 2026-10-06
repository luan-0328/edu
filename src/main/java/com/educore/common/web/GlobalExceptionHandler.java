package com.educore.common.web;

import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.common.RequestIdFilter;
import com.educore.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.validation.FieldError;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException e, HttpServletRequest r) {
        log.warn("Business request rejected: {} {} code={} status={}", r.getMethod(), r.getRequestURI(), e.getCode(), e.getStatus().value());
        return ResponseEntity.status(e.getStatus()).body(Result.failure(e.getCode(), e.getMessage(), id(r)));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, ConstraintViolationException.class,
            HandlerMethodValidationException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Result<Void>> validation(Exception e, HttpServletRequest r) {
        String message = e instanceof MethodArgumentNotValidException m ? fieldErrorMessage(m.getBindingResult().getFieldErrors()) : "请求参数格式不正确";
        return ResponseEntity.badRequest().body(Result.failure(ApiErrorCode.VALIDATION_ERROR, message, id(r)));
    }
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> duplicate(DuplicateKeyException e, HttpServletRequest r) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.failure(ApiErrorCode.DUPLICATE_RESOURCE, "资源已存在", id(r))); }
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<Result<Void>> concurrent(ConcurrencyFailureException e, HttpServletRequest r) {
        log.warn("Concurrent update rejected: {} {}", r.getMethod(), r.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.failure(ApiErrorCode.INVALID_STATE, "数据正在被修改，请刷新后重试", id(r)));
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> method(HttpRequestMethodNotSupportedException e, HttpServletRequest r) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Result.failure(ApiErrorCode.VALIDATION_ERROR, "该接口不支持此请求方法", id(r)));
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> denied(Exception e, HttpServletRequest r) { return ResponseEntity.status(403).body(Result.failure(ApiErrorCode.FORBIDDEN, "无权执行此操作", id(r))); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception e, HttpServletRequest r) {
        log.error("Unhandled request failure: {} {}", r.getMethod(), r.getRequestURI(), e);
        return ResponseEntity.status(500).body(Result.failure(ApiErrorCode.INTERNAL_ERROR, "系统内部错误", id(r)));
    }
    private String fieldErrorMessage(List<FieldError> errors) {
        Set<String> requiredFields = errors.stream()
                .filter(error -> isRequiredError(error.getCode()))
                .map(FieldError::getField)
                .collect(Collectors.toSet());
        return errors.stream()
                .filter(error -> !requiredFields.contains(error.getField()) || isRequiredError(error.getCode()))
                .map(this::formatFieldError)
                .distinct()
                .collect(Collectors.joining("；"));
    }
    private String formatFieldError(FieldError error) {
        String field = switch (error.getField()) {
            case "username" -> "用户名";
            case "password" -> "密码";
            case "realName" -> "姓名";
            default -> error.getField();
        };
        String detail = error.getDefaultMessage();
        if (isRequiredError(error.getCode())) {
            if (detail == null || detail.contains("must not be blank") || detail.contains("must not be null")) return "请输入" + field;
            return detail;
        }
        if (detail == null) return field + "格式不正确";
        if (detail.startsWith(field)) return detail;
        if (detail.contains("must match") || detail.contains("must be between")) return field + "格式不符合要求";
        return field + "：" + detail;
    }
    private boolean isRequiredError(String code) {
        return "NotBlank".equals(code) || "NotNull".equals(code) || "NotEmpty".equals(code);
    }
    private String id(HttpServletRequest r) { Object id = r.getAttribute(RequestIdFilter.ATTRIBUTE); return id == null ? "unknown" : id.toString(); }
}
