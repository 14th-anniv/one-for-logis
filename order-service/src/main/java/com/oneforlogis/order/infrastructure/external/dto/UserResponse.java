package com.oneforlogis.order.infrastructure.external.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private String username;
    private String slackUsername;
    private String password;
    private String nickname;
    private String email;
    private String companyName;
}
