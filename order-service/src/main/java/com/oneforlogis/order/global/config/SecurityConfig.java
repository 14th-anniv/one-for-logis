package com.oneforlogis.order.global.config;

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
        // TODO: 1차 구현 테스트용 - 추후 Security/JWT 스펙 확정되면 제거
        auth.requestMatchers("/api/v1/orders/**").permitAll();
    }
}
