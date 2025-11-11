package com.oneforlogis.user.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.oneforlogis.common.security.SecurityConfigBase;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends SecurityConfigBase {

	@Bean
	public PasswordEncoder passwordEncoder() {

		return new BCryptPasswordEncoder();
	}

	@Override
	protected void configureAuthorization(
		AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {

		// 회원가입 & 로그인 경로는 인증 없이 접근 허용
		auth.requestMatchers(HttpMethod.POST, "/api/v1/users/signup").permitAll();
		auth.requestMatchers(HttpMethod.POST, "/api/v1/users/login").permitAll();
	}
}
