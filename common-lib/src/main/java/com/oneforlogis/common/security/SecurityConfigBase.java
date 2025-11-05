package com.oneforlogis.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@RequiredArgsConstructor
public abstract class SecurityConfigBase {

    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public HeaderAuthFilter headerAuthFilter() {
        return new HeaderAuthFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HeaderAuthFilter headerAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/actuator/**",
                            "/health/**"
                    ).permitAll();

                    configureAuthorization(auth);

                    auth.anyRequest().authenticated();
                });
        return http.build();
    }

    // 서브클래스에서 오버라이드하여 추가 설정
    protected void configureAuthorization(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
    }
}