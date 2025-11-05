package com.oneforlogis.hub.global.config;

import com.oneforlogis.common.security.SecurityConfigBase;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class SecurityConfig extends SecurityConfigBase {
//    @Override
//    protected void configureAuthorization(
//            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
//        auth
//                .requestMatchers("/api/v1/**").permitAll();
//    }
}