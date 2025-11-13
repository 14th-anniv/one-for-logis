package com.oneforlogis.gateway.global.util;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.oneforlogis.common.model.Role;
import com.oneforlogis.gateway.infrastructure.config.RedisService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "JwtUtil")
@Component
@RequiredArgsConstructor
public class JwtUtil {

	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String REFRESH_TOKEN_COOKIE_NAME = "RefreshToken";
	public static final String AUTHORIZATION_KEY = "auth";
	public static final String BEARER_PREFIX = "Bearer ";

	private static final long ACCESS_TOKEN_TIME = 30 * 60 * 1000L; // 30분
	private static final long REFRESH_TOKEN_TIME = 14 * 24 * 60 * 60 * 1000L; // 14일
	private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 14 * 24 * 60 * 60; // HttpOnly 쿠키 만료 기간: 14일

	@Value("${jwt.secret.key}")
	private String secretKey;
	private Key key;
	private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

	private final RedisService redisService; // Blacklist, RT 관리를 위해 RedisService 주입

	@PostConstruct
	public void init() {
		byte[] bytes = Base64.getDecoder().decode(secretKey);
		key = Keys.hmacShaKeyFor(bytes);
	}

	/**
	 * Access Token 생성 (30분)
	 */
	public String createAccessToken(String username, Role role, UUID userId) {
		Date date = new Date();
		String jti = UUID.randomUUID().toString(); // JTI (JWT ID) 추가

		return BEARER_PREFIX +
			Jwts.builder()
				.setSubject(username)
				.claim("role", role.getAuthority())
				.claim("userId", String.valueOf(userId))
				.claim("userName", username)
				.claim("jti", jti) // Blacklist 관리를 위한 JTI
				.setExpiration(new Date(date.getTime() + ACCESS_TOKEN_TIME))
				.setIssuedAt(date)
				.signWith(key, signatureAlgorithm)
				.compact();
	}

	/**
	 * Refresh Token 생성 (14일)
	 */
	public String createRefreshToken(String username, Role role) {
		Date date = new Date();
		String jti = UUID.randomUUID().toString(); // JTI (JWT ID) 추가

		return Jwts.builder()
			.setSubject(username)
			.claim("role", role.getAuthority())
			.claim("jti", jti)
			.setExpiration(new Date(date.getTime() + REFRESH_TOKEN_TIME))
			.setIssuedAt(date)
			.signWith(key, signatureAlgorithm)
			.compact();
	}

	/**
	 * HttpServletRequest Header에서 Access Token 값 가져오기
	 */
	public static String getJwtFromHeader(HttpServletRequest request) {
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		log.info("JwtUtil - Authorization Header Raw Value: {}", bearerToken);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
			return bearerToken.substring(7);
		}
		return null;
	}

	/**
	 * Refresh Token을 HttpOnly 쿠키에 담아 반환
	 */
	public Cookie createRefreshTokenCookie(String refreshToken) {
		Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
		cookie.setHttpOnly(true); // HttpOnly 설정
		// cookie.setSecure(true); // HTTPS 환경에서만 전송 (운영 환경 권장, 테스트 할 땐 비활성화)
		cookie.setPath("/"); // 모든 경로에서 쿠키 접근 가능
		cookie.setMaxAge(REFRESH_TOKEN_COOKIE_MAX_AGE); // 쿠키 만료 기간 14일
		return cookie;
	}

	// HttpOnly쿠키 만료
	public void deleteCookie(HttpServletResponse response, String cookieName) {
		Cookie cookie = new Cookie(cookieName, null);
		cookie.setMaxAge(0); // 즉시 만료
		cookie.setPath("/");

		// HttpOnly 설정 (Refresh Token과 같은 민감 정보는 항상 HttpOnly)
		cookie.setHttpOnly(true);

		// HTTPS 환경에서만 전송 (운영 환경 권장, 테스트 할 땐 비활성화)
		// cookie.setSecure(true);

		response.addCookie(cookie);
	}

	/**
	 * 토큰 검증 (만료 여부, 서명 유효성 등)
	 */
	public boolean validateToken(String token) {
		try {
			// 서명 및 만료 검증
			Claims claims = Jwts.parserBuilder()
				.setSigningKey(key)
				.setAllowedClockSkewSeconds(60) // 60초의 시간 오차 허용
				.build()
				.parseClaimsJws(token)
				.getBody();

			// 블랙리스트에 등록되어있는지 확인 (JTI 사용)
			String jti = claims.get("jti", String.class);
			if (redisService.isTokenBlacklisted(jti)) {
				log.error("Blacklisted Token: {}", jti);
				throw new SecurityException("Blacklisted Token");
			}
			return true;
		} catch (SecurityException | MalformedJwtException | SignatureException e) {
			log.error("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.");
		} catch (ExpiredJwtException e) {
			log.error("Expired JWT token, 만료된 JWT token 입니다.");
			throw e; // 만료된 토큰은 재발급 로직을 위해 던짐
		} catch (UnsupportedJwtException e) {
			log.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
		} catch (IllegalArgumentException e) {
			log.error("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
		} catch (Exception e) { // 블랙리스트 예외 처리
			log.error("Token Validation Error: {}", e.getMessage());
			throw new SecurityException("Invalid or Blacklisted Token");
		}
		return false;
	}

	/**
	 * 토큰에서 사용자 정보(Claims) 추출
	 */
	public Claims getUserInfoFromToken(String token) {
		try {
			return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
		} catch (ExpiredJwtException e) {
			// 만료된 토큰이라도 재발급/로그아웃 처리를 위해 Claim은 반환
			return e.getClaims();
		}
	}

	/**
	 * 토큰에서 JTI(JWT ID) 추출
	 */
	public String getJtiFromToken(String token) {
		return getUserInfoFromToken(token).get("jti", String.class);
	}

	/**
	 * 토큰의 남은 만료 시간 (ms) 계산
	 */
	public long getExpirationRemainingTime(String token) {
		Date expiration = getUserInfoFromToken(token).getExpiration();
		return expiration.getTime() - new Date().getTime();
	}
}
