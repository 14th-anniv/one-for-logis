package com.oneforlogis.user.application.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.common.api.PageResponse;
import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.common.model.Role;
import com.oneforlogis.user.domain.model.User;
import com.oneforlogis.user.domain.repository.UserRepository;
import com.oneforlogis.user.global.util.JwtUtil;
import com.oneforlogis.user.infrastructure.config.RedisService;
import com.oneforlogis.user.presentation.request.UserLoginRequest;
import com.oneforlogis.user.presentation.request.UserRoleUpdateRequest;
import com.oneforlogis.user.presentation.request.UserSignupRequest;
import com.oneforlogis.user.presentation.request.UserStatusRequest;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final RedisService redisService;

	@Value("${jwt.admin.token}")
	private String ADMIN_TOKEN;

	// 회원가입
	public void signup(UserSignupRequest request) {

		// 중복 검사
		if (userRepository.existsByName((request.name()))) {
			throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
		}

		if (userRepository.existsByEmail((request.email()))) {
			throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
		}

		if (userRepository.countBySlackId(request.slack_id()) > 0) {
			throw new CustomException(ErrorCode.DUPLICATE_SLACK_ID);
		}

		// 비밀번호 암호화
		String rawPassword = request.password();
		String encodedPassword = passwordEncoder.encode(rawPassword);

		Role role = request.role();

		if (role == null || request.roleAuthKey() == null) {
			// PENDING인 사용자 회원가입
			User user = User.createUser(request, encodedPassword);
			userRepository.save(user);
			return;
		}

		// 관리자 회원가입: 권한검사(MASTER 권한 & 인증키 필요), 나머지는 전부 PENDING(승인 대기 상태)
		if (role == Role.MASTER) {
			if (ADMIN_TOKEN.equals(request.roleAuthKey())) {
				User user = User.createAdmin(request, encodedPassword);
				userRepository.save(user);
			}
		} else {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
	}


	// 로그인
	public void login(
		UserLoginRequest request,
		HttpServletRequest httpReqeust,
		HttpServletResponse httpResponse) {
		

		User user = userRepository.findByName(request.name())
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_NAME));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new CustomException(ErrorCode.INVALID_PASSWORD);
		}

		// PENDING(승인 대기) 상태거나 REJECTED(승인거부) 상태라면 로그인 실패
		if(user.getStatus().isPending() || user.getStatus().isRejected()){
			throw new CustomException(ErrorCode.NOT_APPROVED_STATUS);
		}

		// Access Token 생성(만료기간: 30분)
		String newAccessToken = jwtUtil.createAccessToken(user, user.getRole()); // Header에 Access Token 저장
		httpResponse.addHeader(JwtUtil.AUTHORIZATION_HEADER, newAccessToken);

		// Refresh Token 생성(만료기간: 7일)
		String newRefreshToken = jwtUtil.createRefreshToken(user, user.getRole());

		// 토큰으로부터 사용자 정보(Claims) 추출: Redis에 Refresh Token 저장
		Claims claims = jwtUtil.getUserInfoFromToken(newRefreshToken);
		String rtJti = claims.get("jti", String.class);
		long expiration = jwtUtil.getExpirationRemainingTime(newRefreshToken);

		// Redis에 저장(username, Refresh Token의 JTI, 현재 남은 만료기간)
		redisService.setRefreshToken(user.getName(), rtJti, Duration.ofMillis(expiration));
		// HttpOnly 쿠키에 저장
		Cookie newRefreshTokenCookie = jwtUtil.createRefreshTokenCookie(newRefreshToken);
		httpResponse.addCookie(newRefreshTokenCookie);

		log.info("로그인 성공: AT, RT 발급 및 Redis 저장 완료. User: {}", user.getName());
	}

	// 회원가입 요청 승인 or 거부
	public void updateStatus(UUID userId, UserStatusRequest request) {

		User user = userRepository.findById(userId).orElseThrow(
			() -> new CustomException(ErrorCode.NOT_FOUND_NAME)
		);

		user.updateStatus(request.status());
	}

	// 권한 변경
	public void updateRole(UUID userId, UserRoleUpdateRequest request) {

		User user = userRepository.findById(userId).orElseThrow(
			() -> new CustomException(ErrorCode.NOT_FOUND_NAME)
		);

		user.updateRole(request.role());
	}

	// 마이페이지 조회
	public User getMyPage(UUID id) {
		return userRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(
			() -> new CustomException(ErrorCode.NOT_FOUND_NAME)
		);
	}

	// 관리자 전용 조회
	@Cacheable(value = "adminUsers", key = "#keyword + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
	public PageResponse<User> adminSearch(String keyword, Pageable pageable) {
		Page<User> userPage = userRepository.searchAllIncludingDeleted(keyword, pageable);

		return PageResponse.fromPage(userPage);
	}

	// 이전 토큰 무효화
	private void invalidatePreviousTokens(String accessToken, String refreshToken, HttpServletResponse httpResponse) {

		if (StringUtils.hasText(accessToken)) {
			try {
				// 이전 AT의 JTI를 추출하고 블랙리스트에 등록(Access Token의 TTL은 해당 토큰의 남은 만료기간)
				String atJti = jwtUtil.getJtiFromToken(accessToken);
				long ttl = jwtUtil.getExpirationRemainingTime(accessToken);
				if (ttl > 0) {
					redisService.setBlacklist(atJti, Duration.ofMillis(ttl));
					log.warn("새 로그인 성공: 이전 Access Token 블랙리스트 등록 완료. JTI: {}", atJti);
				}
			} catch (CustomException e) {
				// AT가 이미 만료되었거나 유효하지 않아 JTI 추출에 실패하면 무시
				log.debug("이전 Access Token 무효화 중 오류 발생 (이미 만료 또는 손상): {}", e.getMessage());
			}
		}
		// 2. 이전 Refresh Token 무효화 (쿠키에서 추출 및 JTI 블랙리스트 등록)

		if (StringUtils.hasText(refreshToken)) {
			try {
				String username = jwtUtil.getUsernameFromToken(refreshToken);

				// 이전 RT의 JTI를 추출하고 블랙리스트에 등록(Refresh Token의 TTL은 해당 토큰의 남은 만료기간)
				String rtJti = jwtUtil.getJtiFromToken(refreshToken);
				long ttl = jwtUtil.getExpirationRemainingTime(refreshToken);

				// Redis에 저장되어있는 Refresh Token 삭제
				redisService.deleteRefreshToken(username);
				// 쿠키에 저장되어있는 Refresh Token 삭제
				jwtUtil.deleteCookie(httpResponse, JwtUtil.REFRESH_TOKEN_COOKIE_NAME);

				if (ttl > 0) {
					redisService.setBlacklist(rtJti, Duration.ofMillis(ttl));
					log.warn("새 로그인 성공: 이전 Refresh Token 블랙리스트 등록 완료. JTI: {}", rtJti);
				}
			} catch (CustomException e) {
				// RT가 이미 만료되었거나 유효하지 않아 JTI 추출에 실패하면 무시
				log.debug("이전 Refresh Token 무효화 중 오류 발생 (이미 만료 또는 손상): {}", e.getMessage());
			}
		}
	}
}
