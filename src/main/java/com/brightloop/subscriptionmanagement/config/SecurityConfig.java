package com.brightloop.subscriptionmanagement.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration.
 *
 * This class defines:
 *  1. Which endpoints are public (no login required) and which require authentication.
 *  2. That the app uses JWT (stateless sessions — no server-side session storage).
 *  3. The password encoder (BCrypt).
 *  4. Method-level security (@PreAuthorize support).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // Enables @PreAuthorize on controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    /**
     * Defines the security rules for all HTTP requests.
     *
     * @param http the HttpSecurity builder
     * @return the configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (Cross-Site Request Forgery) protection.
            // CSRF protection is only needed for browser-based sessions (cookies).
            // Since we use JWT tokens (not cookies), CSRF is not needed.
            .csrf(AbstractHttpConfigurer::disable)

            // Define which endpoints are public and which require authentication
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — anyone can access these without logging in
                .requestMatchers(
                    "/api/auth/signup",
                    "/api/auth/login",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    // Swagger / OpenAPI documentation endpoints
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                // All other endpoints require a valid JWT token
                .anyRequest().authenticated()
            )

            // Configure exception handling
            .exceptionHandling(ex -> ex
                // Return JSON 401 when unauthenticated request hits a protected endpoint
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                // Return JSON 403 when authenticated user lacks permission
                .accessDeniedHandler(customAccessDeniedHandler)
            )

            // Use STATELESS session management — the server won't store session state.
            // Every request must carry its own JWT token.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Add our JWT filter BEFORE Spring's default UsernamePasswordAuthenticationFilter.
            // This way, JWT tokens are validated before the default auth process runs.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures BCrypt as the password hashing algorithm.
     *
     * BCrypt is a strong, adaptive hashing algorithm designed for passwords.
     * It automatically salts passwords and is resistant to brute-force attacks.
     *
     * @return a BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

