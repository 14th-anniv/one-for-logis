package com.oneforlogis.product.global;

import com.oneforlogis.common.security.SecurityConfigBase;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@Configuration
@EnableMethodSecurity
public class SecurityConfig extends SecurityConfigBase {

    @Override
    protected void configureAuthorization(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers("/api/v1/internal/**").permitAll()
                .requestMatchers("/api/v1/**").authenticated();
    }
}
