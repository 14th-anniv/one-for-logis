package com.oneforlogis.notification.presentation.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.core.GrantedAuthorityDefaults;

/**
 * 테스트용 Security 설정
 * - @PreAuthorize를 테스트 환경에서 작동시키기 위한 설정
 */
@TestConfiguration
@EnableMethodSecurity
public class TestSecurityConfig {
    
    /**
     * "ROLE_" 접두사 제거
     * - @PreAuthorize("hasRole('MASTER')") 작동을 위함
     */
    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults("");
    }
}
