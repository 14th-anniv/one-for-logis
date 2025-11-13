package com.oneforlogis.gateway.global.cofig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter; // WebFilter import
import org.springframework.cloud.gateway.filter.GatewayFilterChain; // 👈 GatewayFilterChain import
import org.springframework.web.server.ServerWebExchange; // ServerWebExchange import
import reactor.core.publisher.Mono;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
class SecurityConfig {

	private final JwtAuthenticationGlobalFilter jwtAuthenticationGlobalFilter;

	// GlobalFilter를 WebFilter로 변환하는 Bean 정의 (타입 브릿지)
	@Bean
	public WebFilter jwtWebFilter() {
		return (exchange, chain) -> { // chain은 WebFilterChain 타입

			// 1. GlobalFilter가 요구하는 GatewayFilterChain의 익명 구현체를 생성
			// 이 구현체의 filter() 메소드는 Spring Security 체인(WebFilterChain)의 다음 필터(chain.filter())를 호출
			GatewayFilterChain gatewayChainDelegate = new GatewayFilterChain() {
				@Override
				public Mono<Void> filter(ServerWebExchange exchange) {
					// GlobalFilter가 이 delegate를 호출하면, WebSecurity 체인으로 흐름을 넘김
					return chain.filter(exchange);
				}
			};

			// 2. GlobalFilter의 filter() 메소드를 호출하고, Gateway 체인 대신 delegate를 전달
			return jwtAuthenticationGlobalFilter.filter(exchange, gatewayChainDelegate);
		};
	}

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, WebFilter jwtWebFilter) { // WebFilter 주입
		return http
			.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.authorizeExchange(exchanges -> exchanges
				.pathMatchers("/api/v1/users/login",
					"/api/v1/users/signup",
					"/swagger-ui/**",
					"/v3/api-docs/**",
					"/actuator/**",
					"/health/**"
				)
				.permitAll()
				.anyExchange().authenticated()
			)
			.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
			.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
			.logout(ServerHttpSecurity.LogoutSpec::disable)

			// WebFilter 타입의 jwtWebFilter를 AUTHENTICATION 이전에 등록하여 인증 객체 주입
			.addFilterBefore(jwtWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
			.build();
	}
}
