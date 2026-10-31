package com.brightloop.subscriptionmanagement.config;

import com.brightloop.subscriptionmanagement.entity.BillingPeriod;
import com.brightloop.subscriptionmanagement.entity.Plan;
import com.brightloop.subscriptionmanagement.entity.PlanStatus;
import com.brightloop.subscriptionmanagement.entity.Product;
import com.brightloop.subscriptionmanagement.entity.ProductType;
import com.brightloop.subscriptionmanagement.entity.ProductVariant;
import com.brightloop.subscriptionmanagement.entity.Role;
import com.brightloop.subscriptionmanagement.entity.User;
import com.brightloop.subscriptionmanagement.repository.PlanRepository;
import com.brightloop.subscriptionmanagement.repository.ProductRepository;
import com.brightloop.subscriptionmanagement.repository.ProductVariantRepository;
import com.brightloop.subscriptionmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

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
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PlanRepository planRepository;

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
        seedDemoPlan();
    }

    /**
     * Seeds one demo plan linked to a demo product with variants, only on an empty catalogue,
     * so a fresh install has something to subscribe to in the customer portal.
     */
    private void seedDemoPlan() {
        if (planRepository.count() > 0) {
            return;
        }

        Product product = productRepository.save(Product.builder()
                .name("LoopPay ERP Suite")
                .type(ProductType.SOFTWARE)
                .salesPrice(new BigDecimal("1500.00"))
                .costPrice(new BigDecimal("400.00"))
                .notes("Demo product seeded on first start")
                .active(true)
                .build());

        productVariantRepository.save(ProductVariant.builder()
                .product(product).attribute("Edition").value("Standard")
                .extraPrice(BigDecimal.ZERO).active(true).build());
        productVariantRepository.save(ProductVariant.builder()
                .product(product).attribute("Edition").value("Odoo")
                .extraPrice(new BigDecimal("560.00")).active(true).build());

        LocalDate today = LocalDate.now();
        planRepository.save(Plan.builder()
                .name("ERP Monthly")
                // Mutable set: Hibernate wraps it and must be able to change it later
                .products(new HashSet<>(Set.of(product)))
                .price(new BigDecimal("1500.00"))
                .billingPeriod(BillingPeriod.MONTHLY)
                .minQuantity(1)
                .startDate(today.minusDays(1))
                .endDate(today.plusYears(1))
                .status(PlanStatus.ACTIVE)
                .active(true)
                .build());

        log.info("🌱 Demo plan 'ERP Monthly' seeded with product '{}' and 2 variants.", product.getName());
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

