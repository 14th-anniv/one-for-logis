package com.oneforlogis.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.oneforlogis.user.domain.model.User;

import io.lettuce.core.dynamic.annotation.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

	boolean existsByName(String name);

	boolean existsByEmail(String email);
	
	// @Query를 사용하여 슬렉 아이디 존재하는지 확인
	@Query("SELECT COUNT(u) FROM User u WHERE u.slack_id = :slackId")
	Long countBySlackId(@Param("slackId") String slackId);

	Optional<User>findByName(String name);

	Optional<User> findByIdAndDeletedAtIsNull(UUID id);


	// 특정 키워드(이름, 이메일, 업체명, 별명, 사용자 상태)로 조회 및 전체 조회
	@Query("SELECT u FROM User u " +
		"WHERE (:keyword IS NULL OR u.name LIKE %:keyword% " +
		"OR u.email LIKE %:keyword% " +
		"OR u.company_name LIKE %:keyword% " +
		"OR u.nickname LIKE %:keyword% " +
		"OR str(u.status) LIKE %:keyword%)")
	Page<User> searchAllIncludingDeleted(@Param("keyword") String keyword, Pageable pageable);

}
