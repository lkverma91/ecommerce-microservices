package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.UserRequest;
import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.entity.AuthProvider;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.exception.DuplicateResourceException;
import com.ecommerce.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with email " + request.getEmail() + " already exists");
        }
        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .authProvider(AuthProvider.LOCAL)
                .active(true)
                .roles(new ArrayList<>(List.of(DEFAULT_ROLE)))
                .build();
        user = userRepository.save(user);
        return mapToResponse(user);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with email " + request.getEmail() + " already exists");
        }
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        user = userRepository.save(user);
        return mapToResponse(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Validates email/password for auth-service. Returns empty if not found or password invalid.
     */
    public Optional<UserResponse> validateCredentials(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getPasswordHash() != null && passwordEncoder.matches(password, u.getPasswordHash()))
                .filter(User::getActive)
                .map(this::mapToResponse);
    }

    /**
     * Find user by OAuth provider id, or create new user. Used by auth-service after OAuth callback.
     */
    @Transactional
    public UserResponse findOrCreateByOAuth(AuthProvider authProvider, String providerId, String email, String name) {
        return userRepository.findByAuthProviderAndProviderId(authProvider, providerId)
                .map(this::mapToResponse)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user != null) {
                        user.setAuthProvider(authProvider);
                        user.setProviderId(providerId);
                        if (user.getRoles() == null || user.getRoles().isEmpty()) {
                            user.setRoles(new ArrayList<>(List.of(DEFAULT_ROLE)));
                        }
                        return mapToResponse(userRepository.save(user));
                    }
                    User newUser = User.builder()
                            .email(email)
                            .name(name != null && !name.isBlank() ? name : email)
                            .passwordHash(null)
                            .authProvider(authProvider)
                            .providerId(providerId)
                            .active(true)
                            .roles(new ArrayList<>(List.of(DEFAULT_ROLE)))
                            .build();
                    return mapToResponse(userRepository.save(newUser));
                });
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .active(user.getActive())
                .roles(user.getRoles() != null ? new ArrayList<>(user.getRoles()) : List.of())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
