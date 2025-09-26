package com.innowise.ApiGateway;

import com.innowise.ApiGateway.dto.UserCredentialsDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class RegistrationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    private MockWebServer userServiceServer;
    private MockWebServer authServiceServer;

    private Integer userServicePort = 8081;
    private Integer authServicePort = 8082;

    @BeforeEach
    void setUp() throws IOException {

        userServiceServer = new MockWebServer();
        authServiceServer = new MockWebServer();

        userServiceServer.start(userServicePort);
        authServiceServer.start(authServicePort);
    }

    @AfterEach
    void tearDown() throws IOException {
        userServiceServer.shutdown();
        authServiceServer.shutdown();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("userServiceAddress", () -> "http://localhost:8081");
        registry.add("authServiceAddress", () -> "http://localhost:8082");
    }

    @Test
    void testSuccessfulRegistration() {
        userServiceServer.enqueue(new MockResponse()
                .setResponseCode(200));
        authServiceServer.enqueue(new MockResponse()
                .setResponseCode(200));

        UserCredentialsDto dto = new UserCredentialsDto("test@example.com", "password");

        webTestClient.post()
                .uri("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().isEmpty();

        assertThat(userServiceServer.getRequestCount()).isEqualTo(1);
        assertThat(authServiceServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void testRegistrationFailureWithRollback() throws InterruptedException {
        userServiceServer.enqueue(new MockResponse()
                .setResponseCode(200));
        authServiceServer.enqueue(new MockResponse()
                .setResponseCode(500));
        userServiceServer.enqueue(new MockResponse()
                .setResponseCode(200));

        UserCredentialsDto dto = new UserCredentialsDto("test@example.com", "password");

        webTestClient.post()
                .uri("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody().isEmpty();

        assertThat(userServiceServer.getRequestCount()).isEqualTo(2);
        assertThat(authServiceServer.getRequestCount()).isEqualTo(1);

         userServiceServer.takeRequest(); //first post request
        var rollbackRequest = userServiceServer.takeRequest();
        assertThat(rollbackRequest.getMethod()).isEqualTo("DELETE");
        assertThat(rollbackRequest.getPath()).isEqualTo("/api/v1/users/email/test%40example.com");
    }
}