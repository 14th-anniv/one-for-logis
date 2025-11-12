package com.oneforlogis.user.infrastructure.config;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {

	private final RedisTemplate<String, String> redisTemplate;

	// Refresh Token 저장(Key: username, Value: RefreshToken(JTI으로 저장))
	public void setRefreshToken(String username, String rtJti, Duration expiration) {
		redisTemplate.opsForValue().set(username, rtJti, expiration);
	}

	// Refresh Token 조회
	public String getRefreshToken(String username) {
		return redisTemplate.opsForValue().get(username);
	}

	// Refresh Token 삭제 (로그아웃, 재발급 성공 시)
	public void deleteRefreshToken(String username) {
		redisTemplate.delete(username);
	}

	// Access / Refresh Token Blacklist 등록(Key: jti 값(UUID), Value: blacklist)
	public void setBlacklist(String tokenKey, Duration expiration) {
		redisTemplate.opsForValue().set(tokenKey, "blacklist", expiration);
	}

	// Blacklist 조회 (토큰이 폐기되었는지 확인)
	public boolean isTokenBlacklisted(String tokenKey) {
		return redisTemplate.hasKey(tokenKey);
	}
}
