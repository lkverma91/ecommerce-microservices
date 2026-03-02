package com.ecommerce.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** User info in auth response; matches frontend User and user-service UserResponse. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {

    private Long id;
    private String email;
    private String name;
    private String phone;
    private Boolean active;
    private List<String> roles;
    private LocalDateTime createdAt;
}
