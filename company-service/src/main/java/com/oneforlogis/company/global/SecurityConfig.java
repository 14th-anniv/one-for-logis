package com.oneforlogis.company.global;

import com.oneforlogis.common.security.SecurityConfigBase;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@Configuration
@EnableMethodSecurity
public class SecurityConfig extends SecurityConfigBase {

    // todo: 기본 설정 사용. 인가 과정 필요 시 configureAuthorization 오버라이딩 후 로직 추가
    @Override
    protected void configureAuthorization(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers("/api/v1/**").permitAll();
    }
}
