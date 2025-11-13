package com.oneforlogis.user.presentation.request;

import com.oneforlogis.common.model.Role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 권한 변경 DTO")
public record UserRoleUpdateRequest(

	@Schema(description = "회원이름", example = "asdfg25655")
	Role role
) {}
