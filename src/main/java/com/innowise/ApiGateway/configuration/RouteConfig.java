package com.innowise.ApiGateway.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Value("${userServiceAddress}")
    private String userServiceAddress;

    @Value("${authServiceAddress}")
    private String authServiceAddress;

    @Value("${orderServiceAddress}")
    private String orderServiceAddress;

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("userServiceRoute", r -> r
                        .path("/api/v1/users/**")
                        .uri(userServiceAddress))
                .route("authServiceRoute", r -> r
                        .path("/api/v1/auth/**")
                        .uri(authServiceAddress))
                .route("orderServiceRoute", r -> r
                        .path("/api/v1/orders/**")
                        .uri(orderServiceAddress))
                .build();
    }
}
