package com.oneforlogis.user.presentation.request;

import com.oneforlogis.common.model.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "회원가입 요청 DTO")
public record UserSignupRequest(

	@Schema(description = "회원이름", example = "asdfg25655")
	// 아이디: 4~10자의 소문자와 숫자로만 구성
	@Pattern(regexp = "^[a-z0-9]{4,10}$", message = "아이디는 4~10자의 소문자와 숫자로만 구성되어야 합니다.")
	@NotBlank(message = "아이디는 필수 입력 항목입니다.")
	String name,

	@Schema(description = "슬랙 아이디", example = "U24CAKY1N2O")
	@NotBlank(message = "Slack ID는 필수 입력 항목입니다.")
	String slack_id,

	@Schema(description = "비밀번호", example = "Aqw2343!")
	// 비밀번호: 8~15자의 대소문자, 숫자, 특수문자 포함
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,15}$", message = "비밀번호는 8~15자의 대소문자, 숫자, 특수문자로 구성되어야 합니다.")
	@NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
	String password,

	@Schema(description = "닉네임", example = "Spring")
	@NotBlank(message = "닉네임은 필수 입력 항목입니다.")
	String nickname,

	@Schema(description = "업체명(회사 or 허브명)", example = "스파르타클럽")
	@NotBlank(message = "업체명은 필수 입력 항목입니다.")
	String company_name,

	@Schema(description = "이메일", example = "asdfg2454@naver.com")
	@Email(message = "유효하지 않은 이메일 형식입니다.")
	@NotBlank(message = "이메일은 필수 입력 항목입니다.")
	String email,

	Role role,

	// MASTER 권한 회원가입 시 사용할 인증 키
	String roleAuthKey
){}

