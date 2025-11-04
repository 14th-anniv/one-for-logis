package com.oneforlogis.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(

        @Schema(description = "응답 성공 여부", example = "true")
        boolean success,
        @Schema(description = "HTTP 상태 코드", example = "200")
        int code,
        @Schema(description = "응답 데이터")
        T data
) {

    public ApiResponse(HttpStatus status, T data) {
        this(true, status.value(), data);
    }

    public ApiResponse(HttpStatus status) {
        this(true, status.value(), null);
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok() {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(HttpStatus.CREATED, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(HttpStatus.CREATED));
    }

    public static <T> ResponseEntity<ApiResponse<T>> accepted(T data) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(HttpStatus.ACCEPTED, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> noContent() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(new ApiResponse<>(HttpStatus.NO_CONTENT));
    }
}