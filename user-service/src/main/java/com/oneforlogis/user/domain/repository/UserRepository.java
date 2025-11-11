package com.oneforlogis.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

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
}
