package com.oneforlogis.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 경로를 찾을 수 없습니다."),
    NOT_RESOURCE_OWNER(HttpStatus.FORBIDDEN, "해당 리소스 소유자가 아닙니다."),

    // Valid
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation Error"),

    // Auth
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "잘못된 토큰 값입니다."),
    EMPTY_TOKEN(HttpStatus.UNAUTHORIZED, "JWT 토큰이 비어 있습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT 토큰이 만료되었습니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "지원하지 않는 JWT 토큰입니다."),

	// user
	DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
	DUPLICATE_SLACK_ID(HttpStatus.CONFLICT, "이미 존재하는 슬렉 아이디입니다."),

	NOT_FOUND_NAME(HttpStatus.NOT_FOUND, "아이디가 존재하지 않습니다."),
	INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
	NOT_APPROVED_STATUS(HttpStatus.UNAUTHORIZED, "관리자의 승인을 기다려주세요.");
    private final HttpStatus httpStatus;
    private final String message;
}