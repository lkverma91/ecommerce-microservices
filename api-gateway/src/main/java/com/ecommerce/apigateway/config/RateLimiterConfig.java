package com.ecommerce.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    /**
     * Resolves rate limit key by client IP (X-Forwarded-For or remote address).
     * Falls back to "anonymous" if IP cannot be determined.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"))
                .switchIfEmpty(Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                        .map(addr -> addr.getAddress().getHostAddress()))
                .switchIfEmpty(Mono.just("anonymous"));
    }
}
