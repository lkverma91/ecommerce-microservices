package com.ecommerce.authservice.service;

import com.ecommerce.authservice.dto.AuthResponse;
import com.ecommerce.authservice.dto.AuthUserResponse;
import com.ecommerce.authservice.dto.LoginRequest;
import com.ecommerce.authservice.dto.RegisterRequest;
import com.ecommerce.authservice.client.UserServiceClient;
import com.ecommerce.authservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserServiceClient userServiceClient;
    private final JwtUtil jwtUtil;

    public Optional<AuthResponse> login(LoginRequest request) {
        return userServiceClient.validateCredentials(request.getEmail(), request.getPassword())
                .map(user -> {
                    var roles = user.getRoles() != null ? user.getRoles() : java.util.List.<String>of();
                    String token = jwtUtil.generateToken(user.getId(), user.getEmail(), roles);
                    return AuthResponse.builder().token(token).user(user).build();
                });
    }

    public AuthResponse register(RegisterRequest request) {
        AuthUserResponse user = userServiceClient.createUser(
                request.getEmail(),
                request.getName(),
                request.getPhone(),
                request.getPassword()
        );
        var roles = user.getRoles() != null ? user.getRoles() : java.util.List.<String>of();
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), roles);
        return AuthResponse.builder().token(token).user(user).build();
    }
}
