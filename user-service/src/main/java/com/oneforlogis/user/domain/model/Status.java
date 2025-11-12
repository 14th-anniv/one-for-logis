package com.oneforlogis.user.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Status {

	// 사용자가 회원 가입하면 승인 대기 상태, 마스터 관리자는 이를 승인 or 거부 가능
	PENDING("ROLE_PENDING", "승인 대기 상태"),
	APPROVE("ROLE_APPROVE", "승인 상태"),
	REJECTED("ROLE_REJECTED", "승인 거부 상태");

	private final String key;
	private final String description;

	public boolean isPending() {
		return this == PENDING;
	}

	public boolean isApproved() {
		return this == APPROVE;
	}

	public boolean isRejected() {
		return this == REJECTED;
	}
}
