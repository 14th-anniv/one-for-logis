package com.oneforlogis.notification.infrastructure.client.user;

import com.oneforlogis.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
     * 사용자명으로 사용자 정보 조회
     * @param username 사용자명
     * @return 사용자 정보
     */
    @GetMapping("/api/v1/users/username/{username}")
    ApiResponse<UserResponse> getUserByUsername(@PathVariable("username") String username);
}
