package com.oneforlogis.notification.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

// 테스트용 JPA Auditing 설정
@TestConfiguration
@EnableJpaAuditing
public class TestJpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("TEST_USER");
    }
}
