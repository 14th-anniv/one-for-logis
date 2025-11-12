package com.oneforlogis.user.presentation.request;

import com.oneforlogis.user.domain.model.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원가입 요청 승인 or 거부 DTO")
public record UserStatusRequest(

	@Schema(description = "회원상태", example = "PENDING")
	Status status
) {}
