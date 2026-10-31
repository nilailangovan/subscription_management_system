package com.brightloop.subscriptionmanagement.config;

import com.brightloop.subscriptionmanagement.entity.User;
import com.brightloop.subscriptionmanagement.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication Filter.
 *
 * This filter runs ONCE for every HTTP request that comes into the server.
 * It checks whether the request has a valid JWT token in the Authorization header.
 *
 * How it works:
 *  1. Read the "Authorization" header from the request.
 *  2. If it starts with "Bearer ", extract the token.
 *  3. Validate the token using JwtUtil.
 *  4. If valid, set the authenticated user in Spring Security's context.
 *  5. Pass the request to the next filter in the chain.
 *
 * If no token is found or the token is invalid, the request continues as
 * "unauthenticated" — protected endpoints will then return 401 Unauthorized.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Step 1: Get the Authorization header from the request
        final String authorizationHeader = request.getHeader("Authorization");

        // Step 2: If no Authorization header or it doesn't start with "Bearer ", skip JWT processing
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Continue to the next filter
            return;
        }

        // Step 3: Extract the token (remove the "Bearer " prefix)
        final String jwtToken = authorizationHeader.substring(7);

        try {
            // Step 4: Extract the username (email) from the token
            final String userEmail = jwtUtil.extractUsername(jwtToken);

            // Step 5: Only proceed if we got an email AND the user isn't already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Step 6: Load the user from the database
                User user = userRepository.findByEmail(userEmail).orElse(null);

                if (user != null && user.isActive()) {
                    // Step 7: Create a UserDetails-like object for Spring Security
                    // We use the user's email as the username
                    org.springframework.security.core.userdetails.User userDetails =
                            new org.springframework.security.core.userdetails.User(
                                    user.getEmail(),
                                    user.getPassword(),
                                    // Spring Security needs roles prefixed with "ROLE_"
                                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                            );

                    // Step 8: Validate the token against the loaded user
                    if (jwtUtil.isTokenValid(jwtToken, userDetails)) {
                        // Step 9: Create an authentication object and store it in Spring Security's context
                        // This tells Spring Security: "this request is authenticated"
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,               // Credentials are null after authentication
                                        userDetails.getAuthorities()
                                );

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception ex) {
            // If token parsing fails (expired, tampered, invalid), log and continue as unauthenticated
            // The endpoint's security will then return 401 if auth is required
            logger.warn("JWT validation failed: " + ex.getMessage());
        }

        // Step 10: Always continue to the next filter in the chain
        filterChain.doFilter(request, response);
    }
}

