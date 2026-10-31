package com.brightloop.subscriptionmanagement.config;

import com.brightloop.subscriptionmanagement.dto.response.ErrorResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Custom entry point for handling unauthenticated requests.
 *
 * By default, Spring Security redirects to a login page when auth fails.
 * Since this is a REST API (no login page), we override this to return
 * a clean JSON 401 Unauthorized response instead.
 *
 * This is triggered when a request reaches a secured endpoint
 * without a valid JWT token.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        // Build a JSON error response
        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .message("Authentication required. Please log in and include your JWT token in the Authorization header.")
                .timestamp(LocalDateTime.now())
                .build();

        // Write the JSON error to the HTTP response
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Use Jackson to convert the DTO to JSON (with Java 8 date/time support)
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}

