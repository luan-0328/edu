package com.educore.controller.advice;

import com.educore.common.ApiErrorCode;
import com.educore.common.BusinessException;
import com.educore.common.RequestIdFilter;
import com.educore.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException e, HttpServletRequest r) { return ResponseEntity.status(e.getStatus()).body(Result.failure(e.getCode(), e.getMessage(), id(r))); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, ConstraintViolationException.class})
    public ResponseEntity<Result<Void>> validation(Exception e, HttpServletRequest r) {
        String message = e instanceof MethodArgumentNotValidException m ? m.getBindingResult().getFieldErrors().stream().map(x -> x.getField()+" "+x.getDefaultMessage()).collect(Collectors.joining("; ")) : "请求参数格式不正确";
        return ResponseEntity.badRequest().body(Result.failure(ApiErrorCode.VALIDATION_ERROR, message, id(r)));
    }
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> duplicate(DuplicateKeyException e, HttpServletRequest r) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.failure(ApiErrorCode.DUPLICATE_RESOURCE, "资源已存在", id(r))); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> denied(Exception e, HttpServletRequest r) { return ResponseEntity.status(403).body(Result.failure(ApiErrorCode.FORBIDDEN, "无权执行此操作", id(r))); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception e, HttpServletRequest r) { return ResponseEntity.status(500).body(Result.failure(ApiErrorCode.INTERNAL_ERROR, "系统内部错误", id(r))); }
    private String id(HttpServletRequest r) { Object id = r.getAttribute(RequestIdFilter.ATTRIBUTE); return id == null ? "unknown" : id.toString(); }
}
