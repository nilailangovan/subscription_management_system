package com.brightloop.subscriptionmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a user account in the system.
 *
 * Maps to the "users" table in the database.
 * A user can be an ADMIN, STAFF, or CUSTOMER.
 *
 * Key rules:
 *  - Email must be unique (enforced at both DB and service level).
 *  - Password is stored as a BCrypt hash (never plain text).
 *  - Role determines what the user is allowed to do.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        // Index on email for fast login/lookup queries
        @Index(name = "idx_users_email", columnList = "email"),
        // Index on role for filtering users by role
        @Index(name = "idx_users_role", columnList = "role")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /** Auto-generated primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User's first name */
    @Column(nullable = false)
    private String firstName;

    /** User's last name */
    @Column(nullable = false)
    private String lastName;

    /**
     * User's email address — must be unique across all accounts.
     * Used as the login username.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Hashed password (BCrypt).
     * NEVER store plain text passwords.
     */
    @Column(nullable = false)
    private String password;

    /**
     * The role of the user: ADMIN, STAFF, or CUSTOMER.
     * Stored as a string in the DB for readability.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * Whether the user account is active.
     * Inactive users cannot log in.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Timestamp when the user was created */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the user was last updated */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Automatically sets createdAt and updatedAt before the entity is first saved.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Automatically updates updatedAt every time the entity is modified.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

