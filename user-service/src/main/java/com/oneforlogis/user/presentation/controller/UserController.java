package com.oneforlogis.user.presentation.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.common.security.UserPrincipal;
import com.oneforlogis.user.application.service.UserService;
import com.oneforlogis.user.global.util.JwtUtil;
import com.oneforlogis.user.presentation.request.UserLoginRequest;
import com.oneforlogis.user.presentation.request.UserRoleUpdateRequest;
import com.oneforlogis.user.presentation.request.UserSignupRequest;
import com.oneforlogis.user.presentation.request.UserStatusRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Users", description = "회원 관련 API")
@RestController
@RequestMapping("/api/v1/users/")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final JwtUtil jwtUtil;

	@Operation(summary = "회원가입", description = "회원가입을 진행합니다.")
	@PostMapping("/signup")
	public ApiResponse<Void> signup(@Valid @RequestBody UserSignupRequest request) {

		userService.signup(request);

		return ApiResponse.success("회원가입에 성공했습니다!");
	}

	@Operation(summary = "로그인", description = "로그인을 진행합니다.")
	@PostMapping("/login")
	public ApiResponse<Void> login(
		@Valid @RequestBody UserLoginRequest request
		, HttpServletRequest httpRequest
		, HttpServletResponse httpResponse) {

		userService.login(request, httpRequest, httpResponse);

		return ApiResponse.success("로그인에 성공했습니다!");
	}

	@Operation(summary = "회원가입 요청 승인 or 거부", description = "최종 관리자 아니면 허브 관리자가 회원가입 요청을 승인 또는 거부합니다.")
	@PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER')")
	@PatchMapping("/{userId}/status")
	public ApiResponse<Void> updateStatus(@PathVariable UUID userId,
		@RequestBody UserStatusRequest request
		, @AuthenticationPrincipal UserPrincipal userPrincipal) {

		userService.updateStatus(userId, request);

		return ApiResponse.success("회원가입 요청을 변경하였습니다.");
	}

	@Operation(summary = "권한 설정", description = "최종 관리자가 회원의 권한을 설정합니다.")
	@PreAuthorize("hasRole('MASTER')")
	@PatchMapping("/{userId}/role")
	public ApiResponse<Void> updateRole(@PathVariable UUID userId,
		@RequestBody UserRoleUpdateRequest request,
		@AuthenticationPrincipal UserPrincipal userPrincipal){

		userService.updateRole(userId, request);

		return ApiResponse.success("권한 변경에 성공하였습니다.");
	}

	// @Operation(summary = "권한 설정", description = "최종 관리자가 회원의 권한을 설정합니다.")
	// @PreAuthorize("hasRole('MASTER')")
	// @PatchMapping("/{userId}/role"){
	// 	public ApiResponse<Void> updateRole(@PathVariable UserRoleUpdateRequest){
	//
	// 	}
}
