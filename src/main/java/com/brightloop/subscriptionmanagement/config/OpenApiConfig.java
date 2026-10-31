package com.brightloop.subscriptionmanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI Configuration for LoopPay.
 *
 * Configures the interactive Swagger UI documentation at:
 *   http://localhost:8080/swagger-ui/index.html
 *
 * Sets up JWT "Bearer" security scheme so you can click the "Authorize"
 * padlock button in Swagger and test protected endpoints directly.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LoopPay — Subscription Management API")
                        .description("REST API documentation for LoopPay Subscription Management System. " +
                                     "Authenticate via /api/auth/login, copy the token, and click 'Authorize' to test secured endpoints.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("LoopPay Team")
                                .email("support@looppay.com")))
                // Add global security requirement so Swagger sends Bearer token
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token obtained from POST /api/auth/login")));
    }
}

