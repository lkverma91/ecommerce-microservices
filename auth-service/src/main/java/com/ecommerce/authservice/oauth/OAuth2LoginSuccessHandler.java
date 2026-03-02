package com.ecommerce.authservice.oauth;

import com.ecommerce.authservice.client.UserServiceClient;
import com.ecommerce.authservice.dto.AuthUserResponse;
import com.ecommerce.authservice.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * After OAuth2 login success: find-or-create user, issue JWT, redirect to frontend callback with token.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserServiceClient userServiceClient;
    private final JwtUtil jwtUtil;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${frontend.auth-callback-path:/auth/callback}")
    private String authCallbackPath;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        String registrationId = token.getAuthorizedClientRegistrationId();
        OAuth2User oauth2User = token.getPrincipal();
        Map<String, Object> attrs = oauth2User.getAttributes();

        String providerId = getProviderId(registrationId, attrs);
        String email = getEmail(registrationId, attrs);
        String name = getName(registrationId, attrs);
        String authProvider = mapRegistrationIdToProvider(registrationId);

        if (providerId == null || email == null || email.isBlank()) {
            log.warn("OAuth2 missing providerId or email for registrationId={}", registrationId);
            redirectToCallback(request, response, null, "missing_attributes");
            return;
        }

        AuthUserResponse user = userServiceClient.findOrCreateByOAuth(authProvider, providerId, email, name);
        List<String> roles = user.getRoles() != null ? user.getRoles() : List.of();
        String jwt = jwtUtil.generateToken(user.getId(), user.getEmail(), roles);
        redirectToCallback(request, response, jwt, null);
    }

    private void redirectToCallback(HttpServletRequest request, HttpServletResponse response, String token, String error) throws IOException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path(authCallbackPath.startsWith("/") ? authCallbackPath : "/" + authCallbackPath);
        if (token != null) {
            builder.queryParam("token", token);
        }
        if (error != null) {
            builder.queryParam("error", error);
        }
        String redirectUrl = builder.build().toUriString();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private static String getProviderId(String registrationId, Map<String, Object> attrs) {
        return switch (registrationId.toLowerCase()) {
            case "google", "github" -> (String) attrs.get("sub");
            case "facebook" -> (String) attrs.get("id");
            case "twitter" -> (String) attrs.get("sub");
            default -> (String) attrs.get("sub");
        };
    }

    private static String getEmail(String registrationId, Map<String, Object> attrs) {
        String email = (String) attrs.get("email");
        if (email != null && !email.isBlank()) return email;
        return (String) attrs.get("mail");
    }

    private static String getName(String registrationId, Map<String, Object> attrs) {
        String name = (String) attrs.get("name");
        if (name != null && !name.isBlank()) return name;
        String given = (String) attrs.get("given_name");
        String family = (String) attrs.get("family_name");
        if (given != null || family != null) {
            return (given != null ? given : "") + " " + (family != null ? family : "").trim();
        }
        return (String) attrs.get("login"); // GitHub
    }

    private static String mapRegistrationIdToProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> "GOOGLE";
            case "facebook" -> "FACEBOOK";
            case "github" -> "GITHUB";
            case "twitter" -> "TWITTER";
            default -> registrationId.toUpperCase();
        };
    }
}
