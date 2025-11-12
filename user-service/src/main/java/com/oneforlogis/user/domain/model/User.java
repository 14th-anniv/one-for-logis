package com.oneforlogis.user.domain.model;

import java.util.UUID;

import com.oneforlogis.common.model.BaseEntity;
import com.oneforlogis.common.model.Role;
import com.oneforlogis.user.presentation.request.UserSignupRequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String slack_id;

	@Column(nullable = false)
	private String password;

	@Column
	@Enumerated(EnumType.STRING)
	private Role role;

	@Column(nullable = false)
	private String nickname;

	@Column(nullable = false)
	private String company_name;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private Status status;

	@Column(nullable = false)
	private String email;

	@Builder
	public User(String name, String slack_id, String password, Role role, String nickname, String company_name,
		Status status, String email) {
		this.name = name;
		this.slack_id = slack_id;
		this.password = password;
		this.role = role;
		this.nickname = nickname;
		this.company_name = company_name;
		this.status = status;
		this.email = email;
	}

	public static User createAdmin(UserSignupRequest request, String encodedPassword) {
		return User.builder()
			.name(request.name())
			.slack_id(request.slack_id())
			.password(encodedPassword)
			.role(Role.MASTER)
			.nickname(request.nickname())
			.company_name(request.company_name())
			.status(Status.APPROVE)
			.email(request.email())
			.build();
	}

	public static User createUser(UserSignupRequest request, String encodedPassword) {
		return User.builder()
			.name(request.name())
			.slack_id(request.slack_id())
			.password(encodedPassword)
			.role(null)
			.nickname(request.nickname())
			.company_name(request.company_name())
			.status(Status.PENDING)
			.email(request.email())
			.build();
	}

	public void updateStatus(Status status) {
		this.status = status;
	}

	public void updateRole(Role role) {
		this.role =  role;
	}
}
