package com.oneforlogis.gateway.global.cofig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity  // 기존에 원래 일반 WebSecurity를 사용했는데, 지금 reactive이기 때문에 WebFluxSecurity 사용해야 함.
class SecurityConfig {
	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		return http
            .cors(Customizer.withDefaults())
			.csrf(ServerHttpSecurity.CsrfSpec::disable) // CSRF 비활성화
			.authorizeExchange(exchanges -> exchanges
                    .pathMatchers("/**").permitAll() // Security 필터는 무시
                    .anyExchange().permitAll() // 특정 경로는 허용
			)
			.formLogin(ServerHttpSecurity.FormLoginSpec::disable) // 폼 로그인 비활성화
			.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable) // HTTP Basic 인증 비활성화
			.logout(ServerHttpSecurity.LogoutSpec::disable) // 로그아웃 비활성화 (필요에 따라 설정)
			.build();
	}
}
