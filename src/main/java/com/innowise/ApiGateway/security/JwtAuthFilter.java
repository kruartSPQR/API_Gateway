package com.innowise.ApiGateway.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter implements WebFilter {

    private final JwtValidator jwtValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        final String token = header.substring(7);

        try {
            Claims claims = jwtValidator.validateAndExtractClaims(token);

            if (jwtValidator.isTokenExpired(claims)) {
                log.warn("JWT expired for subject: {}", claims.getSubject());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String username = claims.getSubject();
            String role = jwtValidator.extractRole(claims);
            List<SimpleGrantedAuthority> authorities =
                    Collections.singletonList(new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role));

            var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);

            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                            Mono.just(new SecurityContextImpl(auth))
                    ));

        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
