package com.oneforlogis.notification.infrastructure.client.user;

import com.oneforlogis.common.model.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * User Service로부터 받는 사용자 정보 응답 DTO
 * user-service의 User 엔티티 정보를 담는 DTO
 */
@Getter
@Builder
public class UserResponse {

    private Long userId;
    private String username;
    private String name;
    private String slackId;
    private Role role;
    private UUID hubId;         // nullable (COMPANY_MANAGER는 null)
    private UUID companyId;     // nullable (HUB_MANAGER, DELIVERY_MANAGER는 null)
    private String status;      // PENDING, APPROVED
}
