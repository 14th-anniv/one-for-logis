package com.oneforlogis.common.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@EnableWebSecurity
@EnableMethodSecurity // 메서드 보안 활성화(@PreAuthorize 활성화)
@RequiredArgsConstructor
public abstract class SecurityConfigBase {

    @Autowired
    protected CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public HeaderAuthFilter headerAuthFilter() {
        return new HeaderAuthFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HeaderAuthFilter headerAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
			.sessionManagement(sessionManagement ->  // 세션 비활성화 -> JWT 사용
				sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/api/v1/*/swagger-ui/**",
                            "/api/v1/*/v3/api-docs/**",
                            "/actuator/**",
                            "/health/**",
                            "/api/v1/internal/**" // 내부 호출용
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