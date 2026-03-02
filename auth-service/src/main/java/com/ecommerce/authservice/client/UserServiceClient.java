package com.ecommerce.authservice.client;

import com.ecommerce.authservice.dto.AuthUserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * Calls user-service internal and public endpoints.
 * Uses load-balanced WebClient (service id: user-service).
 */
@Component
@Slf4j
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient(@Qualifier("userServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /** Validate credentials. Returns user if valid, empty if 401 or error. */
    public java.util.Optional<AuthUserResponse> validateCredentials(String email, String password) {
        try {
            Map<String, String> body = Map.of("email", email, "password", password);
            AuthUserResponse user = webClient.post()
                    .uri("/internal/validate")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(AuthUserResponse.class)
                    .block();
            return java.util.Optional.ofNullable(user);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return java.util.Optional.empty();
            }
            log.warn("user-service validate failed: {}", e.getMessage());
            throw e;
        }
    }

    /** Create user (register). User-service hashes password. */
    public AuthUserResponse createUser(String email, String name, String phone, String password) {
        Map<String, Object> body = Map.of(
                "email", email,
                "name", name != null ? name : email,
                "password", password,
                "phone", phone != null ? phone : ""
        );
        return webClient.post()
                .uri("/users")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(AuthUserResponse.class)
                .block();
    }

    /** Find or create user by OAuth provider. */
    public AuthUserResponse findOrCreateByOAuth(String authProvider, String providerId, String email, String name) {
        Map<String, Object> body = Map.of(
                "authProvider", authProvider,
                "providerId", providerId,
                "email", email,
                "name", name != null ? name : email
        );
        return webClient.post()
                .uri("/internal/users/find-or-create")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(AuthUserResponse.class)
                .block();
    }

    /** Get user by id (for /auth/me). */
    public AuthUserResponse getUserById(Long id) {
        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .bodyToMono(AuthUserResponse.class)
                .block();
    }
}
