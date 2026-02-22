package com.ecommerce.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Config Server Application
 *
 * @EnableConfigServer - Enables the config server endpoints.
 * Clients fetch config via: GET /{application}/{profile} or /{application}/{profile}/{label}
 *
 * Config is served from native filesystem (classpath:/config-repo) or Git.
 * Should start before other microservices so they can bootstrap their config.
 */
@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
