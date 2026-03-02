package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.FindOrCreateOAuthRequest;
import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.dto.ValidateCredentialsRequest;
import com.ecommerce.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal endpoints for auth-service. Not exposed via gateway.
 * Callable only from within the cluster (e.g. auth-service via service discovery).
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalAuthController {

    private final UserService userService;

    @PostMapping("/validate")
    public ResponseEntity<UserResponse> validateCredentials(@Valid @RequestBody ValidateCredentialsRequest request) {
        return userService.validateCredentials(request.getEmail(), request.getPassword())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/users/find-or-create")
    public ResponseEntity<UserResponse> findOrCreateByOAuth(@Valid @RequestBody FindOrCreateOAuthRequest request) {
        UserResponse user = userService.findOrCreateByOAuth(
                request.getAuthProvider(),
                request.getProviderId(),
                request.getEmail(),
                request.getName()
        );
        return ResponseEntity.ok(user);
    }
}
