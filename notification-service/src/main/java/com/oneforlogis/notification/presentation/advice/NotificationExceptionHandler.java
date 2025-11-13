package com.oneforlogis.notification.presentation.advice;

import com.oneforlogis.common.api.ApiResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class NotificationExceptionHandler {

    /**
     * FeignClient 호출 실패 처리
     * - user-service 등 다른 서비스 호출 실패 시 적절한 HTTP 상태 코드 반환
     */
    @ExceptionHandler(FeignException.class)
    protected ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException e) {
        int status = e.status();
        String message = extractFeignErrorMessage(e);

        log.error("[FeignException] status={}, message={}", status, message);

        // FeignException의 status를 그대로 사용
        HttpStatus httpStatus = HttpStatus.valueOf(status);
        ApiResponse<Void> response = new ApiResponse<>(false, status, message, null);

        return new ResponseEntity<>(response, httpStatus);
    }

    /**
     * FeignException에서 의미 있는 에러 메시지 추출
     */
    private String extractFeignErrorMessage(FeignException e) {
        int status = e.status();

        // HTTP 상태 코드별로 사용자 친화적인 메시지 반환
        return switch (status) {
            case 400 -> "외부 서비스 요청 형식이 올바르지 않습니다. (user-service 연동 실패)";
            case 401 -> "외부 서비스 인증에 실패했습니다. (user-service 연동 실패)";
            case 403 -> "요청한 리소스에 접근할 수 없습니다. (user-service 연동 실패)";
            case 404 -> "요청한 리소스를 찾을 수 없습니다. (user-service 연동 실패)";
            case 408 -> "외부 서비스 요청 시간이 초과되었습니다. (user-service 연동 실패)";
            case 500 -> "외부 서비스에서 오류가 발생했습니다. (user-service 연동 실패)";
            case 503 -> "외부 서비스를 일시적으로 사용할 수 없습니다. (user-service 연동 실패)";
            default -> {
                if (status >= 400 && status < 500) {
                    yield "외부 서비스 요청 처리에 실패했습니다. (user-service 연동 실패)";
                } else if (status >= 500) {
                    yield "외부 서비스에서 오류가 발생했습니다. (user-service 연동 실패)";
                }
                yield "외부 서비스 연동 중 오류가 발생했습니다.";
            }
        };
    }
}
