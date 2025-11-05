package com.oneforlogis.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        @Schema(description = "응답 성공 여부", example = "true")
        boolean isSuccess,
        @Schema(description = "HTTP 상태 코드", example = "200")
        int code,
        @Schema(description = "응답 메시지", example = "Created.")
        String message,
        @Schema(description = "응답 데이터")
        T data
) {

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), null);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), message, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), data);
    }

    public static <T> ApiResponse<T> created() {
        return new ApiResponse<>(true, HttpStatus.CREATED.value(), HttpStatus.CREATED.getReasonPhrase(), null);
    }

    public static <T> ApiResponse<T> created(String message) {
        return new ApiResponse<>(true, HttpStatus.CREATED.value(), message, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, HttpStatus.CREATED.value(), HttpStatus.CREATED.getReasonPhrase(), data);
    }

    public static <T> ApiResponse<T> accepted(T data) {
        return new ApiResponse<>(true, HttpStatus.ACCEPTED.value(), HttpStatus.ACCEPTED.getReasonPhrase(), data);
    }

    public static <T> ApiResponse<T> noContent() {
        return new ApiResponse<>(true, HttpStatus.NO_CONTENT.value(), HttpStatus.NO_CONTENT.getReasonPhrase(), null);
    }

}