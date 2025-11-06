package com.oneforlogis.notification.global.config;

import com.oneforlogis.common.security.SecurityConfigBase;
import org.springframework.context.annotation.Configuration;

// Spring Security 설정
// common-lib의 SecurityConfigBase를 상속받아 기본 보안 설정 적용
@Configuration
public class SecurityConfig extends SecurityConfigBase {
    // 기본 설정만 사용, 추가 인가 규칙 필요시 configureAuthorization 오버라이드
}