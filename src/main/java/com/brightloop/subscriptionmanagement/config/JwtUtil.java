package com.brightloop.subscriptionmanagement.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for creating and validating JWT tokens.
 *
 * A JWT (JSON Web Token) is a compact, self-contained token that stores
 * user information (like user ID, email, role) encoded and signed.
 *
 * Structure of a JWT:
 *   header.payload.signature
 *
 * The server signs tokens with a secret key. Clients send tokens back
 * in the Authorization header: "Bearer <token>"
 */
@Component
public class JwtUtil {

    /**
     * The secret key used to sign tokens.
     * Loaded from application.properties (app.jwt.secret).
     * Must be kept private — if someone gets this, they can forge tokens!
     */
    @Value("${app.jwt.secret}")
    private String secretKey;

    /**
     * How long a token stays valid (in milliseconds).
     * Loaded from application.properties (app.jwt.expiration-ms).
     */
    @Value("${app.jwt.expiration-ms}")
    private long tokenExpiryMs;

    /**
     * Generates a JWT token for a logged-in user.
     *
     * @param userDetails the authenticated user
     * @param userId      the user's database ID (added as a custom claim)
     * @param role        the user's role (ADMIN, STAFF, CUSTOMER)
     * @return the signed JWT token string
     */
    public String generateToken(UserDetails userDetails, Long userId, String role) {
        // Extra claims to embed in the token payload
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId);
        extraClaims.put("role", role);

        return Jwts.builder()
                .claims(extraClaims)
                // The "subject" is the username (email in our case)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tokenExpiryMs))
                // Sign the token with our secret key
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the username (email) from a JWT token.
     *
     * @param token the JWT token
     * @return the email stored in the token's subject claim
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from a JWT token.
     *
     * @param token the JWT token
     * @return when the token expires
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Validates a JWT token:
     *  1. Checks that the username in the token matches the user.
     *  2. Checks that the token is not expired.
     *
     * @param token       the JWT token to validate
     * @param userDetails the user to validate against
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Checks if the token has expired.
     *
     * @param token the JWT token
     * @return true if expired, false if still valid
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts a specific claim from the token using a function.
     * This is a generic helper used by other extract methods.
     *
     * @param token          the JWT token
     * @param claimsResolver a function that picks which claim to extract
     * @return the extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and returns all claims from the token.
     * Throws an exception if the token is tampered with or expired.
     *
     * @param token the JWT token
     * @return all claims in the token's payload
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Converts the Base64-encoded secret string into a cryptographic key
     * used to sign and verify tokens.
     *
     * @return the SecretKey for HMAC-SHA signing
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

