package com.brightloop.subscriptionmanagement.config;

import com.brightloop.subscriptionmanagement.entity.Role;
import com.brightloop.subscriptionmanagement.entity.User;
import com.brightloop.subscriptionmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Data Seeder — runs automatically when the application starts.
 *
 * This class seeds the database with a default ADMIN user
 * if one doesn't already exist.
 *
 * Implements CommandLineRunner, which means Spring Boot calls the
 * run() method automatically after the application context is loaded.
 *
 * Admin credentials are loaded from application.properties:
 *   app.admin.email
 *   app.admin.password
 *   app.admin.first-name
 *   app.admin.last-name
 */
@Component
@RequiredArgsConstructor
@Slf4j  // Lombok annotation that creates a 'log' logger automatically
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.first-name}")
    private String adminFirstName;

    @Value("${app.admin.last-name}")
    private String adminLastName;

    /**
     * Called automatically by Spring Boot on startup.
     *
     * Checks if the admin user already exists. If not, creates one.
     * This makes the seeder safe to run multiple times (idempotent).
     */
    @Override
    public void run(String... args) {
        seedAdminUser();
    }

    /**
     * Creates the default admin user if they don't already exist.
     */
    private void seedAdminUser() {
        // Check if admin already exists — avoid creating duplicates
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("✅ Admin user already exists: {}", adminEmail);
            return;
        }

        // Build the admin user entity
        User adminUser = User.builder()
                .firstName(adminFirstName)
                .lastName(adminLastName)
                .email(adminEmail)
                // Hash the password before saving — NEVER store plain text!
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .active(true)
                .build();

        // Save to the database
        userRepository.save(adminUser);

        log.info("🌱 Default admin user seeded successfully.");
        log.info("   Email   : {}", adminEmail);
        log.info("   Password: {} (change this in production!)", adminPassword);
    }
}

