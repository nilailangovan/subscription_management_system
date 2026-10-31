package com.brightloop.subscriptionmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Brightloop Subscription Management System.
 *
 * @SpringBootApplication is a shortcut annotation that combines:
 *   - @Configuration: marks this as a source of bean definitions
 *   - @EnableAutoConfiguration: lets Spring Boot auto-configure the app
 *   - @ComponentScan: scans all classes in this package and sub-packages
 */
@SpringBootApplication
public class SubscriptionManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriptionManagementSystemApplication.class, args);
    }
}

