package com.ecommerce.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Config Server Application
 *
 * @EnableConfigServer - Enables the config server endpoints.
 * Clients fetch config via: GET /{application}/{profile} or /{application}/{profile}/{label}
 *
 * Config is served from native filesystem (classpath:/config-repo) or Git.
 * Registers with Eureka for discovery.
 */
@EnableConfigServer
@EnableDiscoveryClient
@SpringBootApplication
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
