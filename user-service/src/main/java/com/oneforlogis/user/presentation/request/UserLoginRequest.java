package com.oneforlogis.user.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청 DTO")
public record UserLoginRequest(

	@Schema(description = "회원이름", example = "asdfg25655")
	@NotBlank(message = "아이디를 입력해주세요.")
	String name,

	@Schema(description = "비밀번호", example = "Asdf123!")
	@NotBlank(message = "비밀번호를 입력해주세요.")
	String password
) {}
