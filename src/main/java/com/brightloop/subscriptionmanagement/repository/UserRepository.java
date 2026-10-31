package com.brightloop.subscriptionmanagement.repository;

import com.brightloop.subscriptionmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User database operations.
 *
 * Spring Data JPA auto-generates all basic CRUD methods.
 * We only need to declare custom query methods here.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     * Used during login and forgot-password flows.
     *
     * @param email the email to search for
     * @return an Optional containing the user if found, or empty if not
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given email already exists.
     * Used during signup to enforce unique emails.
     *
     * @param email the email to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
}

