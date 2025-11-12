package com.oneforlogis.common.exception;

import com.oneforlogis.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * HTTP 상태 코드가 실제 Response에 반영되도록
     */
    // CustomException 처리
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        HttpStatus status = errorCode.getHttpStatus();
        log.warn("[BusinessException] {} (status: {})", errorCode.getMessage(), status.value());

        ApiResponse<Void> response = new ApiResponse<>(false, status.value(), errorCode.getMessage(), null);
        return new ResponseEntity<>(response, status);
    }

    // 요청 값 유효성 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "요청 값이 유효하지 않습니다.";
        log.warn("[ValidationError] {}", message);
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiResponse<Void> response = new ApiResponse<>(false, status.value(), message, null);
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "요청 파라미터가 유효하지 않습니다.";
        log.warn("[BindError] {}", message);
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiResponse<Void> response = new ApiResponse<>(false, status.value(), message, null);
        return new ResponseEntity<>(response, status);
    }

    // HTTP 메서드 지원 안됨
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("[METHOD_NOT_ALLOWED]", e);
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;

        ApiResponse<Void> response = new ApiResponse<>(false, status.value(), e.getMessage(), null);
        return new ResponseEntity<>(response, status);
    }

    // 핸들러 없음 (404)
    @ExceptionHandler(NoHandlerFoundException.class)
    protected ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException e) {
        log.warn("[NOT_FOUND]", e);
        HttpStatus status = HttpStatus.NOT_FOUND;

        ApiResponse<Void> response = new ApiResponse<>(false, status.value(), e.getMessage(), null);
        return new ResponseEntity<>(response, status);
    }

    // 권한 없음
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        ErrorCode errorCode = ErrorCode.FORBIDDEN_ACCESS;
        HttpStatus status = errorCode.getHttpStatus();

        ApiResponse<Void> response = new ApiResponse<>(false, status.value(), errorCode.getMessage(), null);
        return new ResponseEntity<>(response, status);
    }

    // 그 외 서버 에러
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[INTERNAL_SERVER_ERROR] {}", e.getMessage(), e);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ApiResponse<Void> response = new ApiResponse<>(false, status.value(), e.getMessage(), null);
        return new ResponseEntity<>(response, status);
    }
}
