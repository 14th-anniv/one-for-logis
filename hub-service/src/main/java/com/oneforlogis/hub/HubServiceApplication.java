package com.oneforlogis.hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

@Import({
        com.oneforlogis.common.config.SwaggerConfig.class,
        com.oneforlogis.common.config.JpaAuditConfig.class
})
@EnableDiscoveryClient
@SpringBootApplication
public class HubServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HubServiceApplication.class, args);
    }
}