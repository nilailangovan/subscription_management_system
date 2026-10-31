package com.brightloop.subscriptionmanagement.repository;

import com.brightloop.subscriptionmanagement.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for PasswordResetToken database operations.
 *
 * Handles storing and retrieving password reset tokens.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Finds a reset token by its token string.
     * Called when the user clicks the reset link and submits the new password.
     *
     * @param token the token string from the reset link
     * @return an Optional containing the token record if found
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Deletes all tokens belonging to a specific user.
     * Called before creating a new token to avoid stale tokens piling up.
     *
     * @param userId the ID of the user whose tokens to delete
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.user.id = :userId")
    void deleteAllByUserId(Long userId);

    /**
     * Deletes all expired tokens (cleanup job friendly).
     *
     * @param now the current time; tokens with expiresAt before this are deleted
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiresAt < :now")
    void deleteAllExpiredTokens(LocalDateTime now);
}

