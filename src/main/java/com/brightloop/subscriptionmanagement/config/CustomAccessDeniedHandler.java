package com.brightloop.subscriptionmanagement.config;

import com.brightloop.subscriptionmanagement.dto.response.ErrorResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Custom handler for 403 Forbidden responses.
 *
 * When a logged-in user tries to access a resource they don't have
 * permission for (e.g., a CUSTOMER trying to hit an ADMIN endpoint),
 * Spring Security calls this handler instead of returning a default error page.
 *
 * We return a clean JSON 403 response.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .message("Access denied. You do not have permission to access this resource.")
                .timestamp(LocalDateTime.now())
                .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}

