package com.oneforlogis.notification.global.config;

import com.oneforlogis.common.security.SecurityConfigBase;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@Configuration
@EnableMethodSecurity
public class SecurityConfig extends SecurityConfigBase {
    
    @Override
    protected void configureAuthorization(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        // 내부 서비스 간 통신 엔드포인트는 인증 없이 허용
        auth.requestMatchers("/api/v1/notifications/order").permitAll();
    }
}
