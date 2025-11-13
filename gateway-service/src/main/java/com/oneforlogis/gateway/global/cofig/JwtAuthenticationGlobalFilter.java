package com.oneforlogis.gateway.global.cofig;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.gateway.global.util.JwtUtil;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j(topic = "Gateway JWT Filter")
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

	private final JwtUtil jwtUtil;
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();


	//화이트리스트(인증 건너뛰는 경로) 목록
	private static final List<String> WHITELIST = List.of(
            "/api/v1/internal/**",
            "/api/v1/users/login",
            "/api/v1/users/signup",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/v1/*/swagger-ui/**",
            "/api/v1/*/v3/api-docs/**",
            "/webjars/**",
            "/swagger-resources/**",
            "/actuator/**",
            "/health/**"
	);

	// 요청별 로깅 상태 저장 (비동기 안전)
	private static final ConcurrentHashMap<String, Boolean> loggedRequests = new ConcurrentHashMap<>();

	@Override // Mono<Void>: 리턴 X, 비동기 처리
	// (exchange: 요청/응답 비동기 컨텍스트(WebFlux), chain: 다음 필터로 요청 전달)
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		// 요청 컨텍스트에서 경로 가져오기
		String path = exchange.getRequest().getURI().getPath();
		String requestId = exchange.getRequest().getId(); // Gateway가 자동 생성함

		// path 로그: 요청당 1회만
		loggedRequests.computeIfAbsent(requestId, id -> {
			log.info("path: {}", path);
			return true;
		});

		// 화이트리스트 경로는 필터 스킵
		if (isWhitelisted(path)) {
			return chain.filter(exchange);
		}

		String token = getTokenFromHeader(exchange);

		// 토큰 검증
		if (!(StringUtils.hasText(token) && jwtUtil.validateToken(token))) {
			return Mono.error(new CustomException(ErrorCode.INVALID_TOKEN));
		}

		Claims claims = jwtUtil.getUserInfoFromToken(token);

			// claims에서 Header에 보낼 정보 추출
			String username = claims.getSubject();
			String role = claims.get("role", String.class);
			String userId = claims.get("userId", String.class);
			String userName = claims.get("userName", String.class);

		// JWT 인증 성공 로그도 요청당 1회만 출력
		loggedRequests.computeIfAbsent(requestId + "_AUTH", id -> {
			log.info("Gateway JWT 인증 성공: {}", username);
			return true;
		});

			// 각 서비스로 전달할 Header 추가(Gateway 내부(서버)에서 추가한 헤더는 포스트맨에서 안보임(클라이언트)
			// 요청 헤더는 직접 볼 수 없음
			HttpHeaders headers = exchange.getRequest().getHeaders();
			exchange = exchange.mutate()
				.request(r -> r
					.header("X-User-Id", userId)
					.header("X-User-Name", userName)
					.header("X-User-Role", role))
				.build();

		// 응답 헤더에 추가 (beforeCommit 사용: WebFlux 비동기 체인 때문에 중복 호출되므로 사용)
		ServerWebExchange finalExchange = exchange;
		exchange.getResponse().beforeCommit(() -> {
			finalExchange.getResponse().getHeaders().set("X-User-Id", userId);
			finalExchange.getResponse().getHeaders().set("X-User-Name", userName);
			finalExchange.getResponse().getHeaders().set("X-User-Role", role);
			return Mono.empty();
		});

		// 토큰 검증 후 Authentication 객체를 만들어 SecurityContext에 저장 후 반환
		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
		UsernamePasswordAuthenticationToken auth =
			new UsernamePasswordAuthenticationToken(username, null, authorities);

		return chain.filter(exchange)
			.contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

	}

	// Header에서 Access Token 추출
	private String getTokenFromHeader(ServerWebExchange exchange) {
		String bearerToken = exchange.getRequest().getHeaders().getFirst(JwtUtil.AUTHORIZATION_HEADER);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(JwtUtil.BEARER_PREFIX)) {
			return bearerToken.substring(JwtUtil.BEARER_PREFIX.length());
		}
		return null;
	}

	// 화이트리스트 경로 검사(List에 들어있는 경로인지 확인)
    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

	// 가장 먼저 실행
	@Override
	public int getOrder() {
		return -1;
	}
}

