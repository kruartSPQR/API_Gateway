package com.innowise.ApiGateway.controller;

import com.innowise.ApiGateway.dto.UserCredentialsDto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("api/v1/auth")
public class RegistrationController {

    private final WebClient authServiceWebClient;
    private final WebClient userServiceWebClient;

    @PostMapping("/signup")
    @ResponseBody
    public Mono<ResponseEntity<Void>> signUp(@Valid @RequestBody UserCredentialsDto dto) {
        return saveToUserService(dto)
                .then(saveUserCredentials(dto))
                .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).<Void>build()))
                .onErrorResume(e -> {
                    log.error("Registration failed. Trying to rollback.", e);
                    return rollbackUserService(dto)
                            .then(Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build()))
                            .onErrorResume(rollbackError -> {
                                log.error("Rollback failed", rollbackError);
                                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build());
                            });
                });
    }

    public Mono<Void> saveUserCredentials(UserCredentialsDto userCredentialsDto) {
        return authServiceWebClient.post()
                .uri("/api/v1/auth/signup")
                .body(Mono.just(userCredentialsDto), UserCredentialsDto.class)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Void> saveToUserService(UserCredentialsDto userCredentialsDto) {
        return userServiceWebClient.post()
                .uri("/api/v1/users/add")
                .bodyValue(Map.of("email", userCredentialsDto.getEmail()))
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Void> rollbackUserService(UserCredentialsDto userCredentialsDto) {
        return userServiceWebClient.delete()
                .uri("/api/v1/users/email/{email}", userCredentialsDto.getEmail())
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("Rollback failed: {}", e.getMessage()));
    }
}
