package com.oneforlogis.notification.infrastructure.client.user;

import com.oneforlogis.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/**
 * User Service FeignClient
 * 사용자 정보 조회를 위한 서비스간 통신 클라이언트
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * 사용자 ID로 사용자 정보 조회
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    @GetMapping("/api/v1/users/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable("userId") Long userId);

    /**
     * 마이페이지 조회 (인증된 사용자 자신의 정보)
     * - Gateway에서 전달한 X-User-Id 헤더로 사용자 식별
     * - user-service의 GET /api/v1/users/me 호출
     * @param userId 사용자 ID (Gateway 헤더에서 전달)
     * @return 사용자 정보
     */
    @GetMapping("/api/v1/users/me")
    ApiResponse<UserResponse> getMyInfo(@RequestHeader("X-User-Id") UUID userId);
}
