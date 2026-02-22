package com.ecommerce.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Server Application
 *
 * @EnableEurekaServer - Marks this application as a Eureka Server.
 * It starts the embedded Eureka server that other services will register with.
 *
 * Eureka Server does NOT register itself with Eureka by default (it's the registry).
 * In a production cluster, you'd run multiple Eureka nodes that peer with each other.
 */
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
