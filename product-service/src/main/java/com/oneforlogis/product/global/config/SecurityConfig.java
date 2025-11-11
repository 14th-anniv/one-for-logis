package com.oneforlogis.product.global.config;

import com.oneforlogis.common.security.SecurityConfigBase;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@Configuration
@EnableMethodSecurity
public class SecurityConfig extends SecurityConfigBase {

    // 모듈 통신 개발용 주석 제거
    @Override
    protected void configureAuthorization(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers("/api/v1/**").permitAll();
    }
}
