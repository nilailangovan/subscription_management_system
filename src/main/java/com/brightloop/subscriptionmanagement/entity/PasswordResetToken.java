package com.brightloop.subscriptionmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a password reset request.
 *
 * When a user requests a password reset, a unique token is generated
 * and stored here along with its expiry time.
 *
 * Maps to the "password_reset_tokens" table.
 */
@Entity
@Table(name = "password_reset_tokens",
    indexes = {
        // Index on token for fast lookup when the user submits the reset link
        @Index(name = "idx_prt_token", columnList = "token"),
        // Index on user_id to find tokens belonging to a user
        @Index(name = "idx_prt_user_id", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    /** Auto-generated primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The unique reset token sent to the user's email.
     * This is a UUID (random, impossible to guess).
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * The user who requested the password reset.
     * Many tokens can belong to one user (e.g., multiple requests).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * When this token expires.
     * Tokens older than this time are invalid and must be rejected.
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Whether this token has already been used.
     * A token can only be used once.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    /** When the token was created */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

