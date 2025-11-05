package com.oneforlogis.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "One For Logis API", version = "v1"),
        security = {
                @SecurityRequirement(name = "X-User-Id"),
                @SecurityRequirement(name = "X-User-Name"),
                @SecurityRequirement(name = "X-User-Role")
        }
)
@SecuritySchemes({
        @SecurityScheme(
                name = "X-User-Id",
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER,
                paramName = "X-User-Id"
        ),
        @SecurityScheme(
                name = "X-User-Name",
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER,
                paramName = "X-User-Name"
        ),
        @SecurityScheme(
                name = "X-User-Role",
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER,
                paramName = "X-User-Role"
        )
})
public class SwaggerConfig {
}