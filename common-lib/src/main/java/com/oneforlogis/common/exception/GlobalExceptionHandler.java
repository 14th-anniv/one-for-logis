package com.oneforlogis.common.exception;

import com.oneforlogis.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    protected ApiResponse<Void> handleBusinessException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        HttpStatus status = errorCode.getHttpStatus();
        log.warn("[BusinessException] {} (status: {})", errorCode.getMessage(), status.value());
        return new ApiResponse<>(false, status.value(), errorCode.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ApiResponse<Void> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "요청 값이 유효하지 않습니다.";
        log.warn("[ValidationError] {}", message);
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return new ApiResponse<>(false, status.value(), message, null);
    }

    @ExceptionHandler(BindException.class)
    protected ApiResponse<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "요청 파라미터가 유효하지 않습니다.";
        log.warn("[BindError] {}", message);
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return new ApiResponse<>(false, status.value(), message, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ApiResponse<Void> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e) {
        log.warn("[METHOD_NOT_ALLOWED]", e);
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        return new ApiResponse<>(false, status.value(), e.getMessage(), null);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    protected ApiResponse<Void> handleNotFound(NoHandlerFoundException e) {
        log.warn("[NOT_FOUND]", e);
        HttpStatus status = HttpStatus.NOT_FOUND;
        return new ApiResponse<>(false, status.value(), e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    protected ApiResponse<Void> handleException(Exception e) {
        log.error("[INTERNAL_SERVER_ERROR] {}", e.getMessage(), e);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return new ApiResponse<>(false, status.value(), e.getMessage(), null);
    }
}