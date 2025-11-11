package com.oneforlogis.user.presentation.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.user.application.service.UserService;
import com.oneforlogis.user.global.util.JwtUtil;
import com.oneforlogis.user.presentation.request.UserLoginRequest;
import com.oneforlogis.user.presentation.request.UserSignupRequest;

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
}
