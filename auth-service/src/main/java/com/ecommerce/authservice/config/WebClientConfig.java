package com.ecommerce.authservice.config;

import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("userServiceWebClient")
    public WebClient userServiceWebClient(WebClient.Builder builder,
                                          LoadBalancedExchangeFilterFunction loadBalanced) {
        return builder
                .baseUrl("http://user-service")
                .filter(loadBalanced)
                .build();
    }
}
