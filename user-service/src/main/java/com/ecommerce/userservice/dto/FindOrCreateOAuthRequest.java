package com.ecommerce.userservice.dto;

import com.ecommerce.userservice.entity.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindOrCreateOAuthRequest {

    @NotNull
    private AuthProvider authProvider;

    @NotBlank
    private String providerId;

    @NotBlank
    private String email;

    private String name;
}
