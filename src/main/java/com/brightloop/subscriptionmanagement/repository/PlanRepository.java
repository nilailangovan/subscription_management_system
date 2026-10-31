package com.brightloop.subscriptionmanagement.repository;

import com.brightloop.subscriptionmanagement.entity.Plan;
import com.brightloop.subscriptionmanagement.entity.PlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Subscription Plan operations.
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    /**
     * Active plan by ID.
     */
    Optional<Plan> findByIdAndActiveTrue(Long id);

    /**
     * Paginated list of active plans for ADMIN / STAFF.
     */
    Page<Plan> findByActiveTrue(Pageable pageable);

    /**
     * Search active plans by name for ADMIN / STAFF.
     */
    Page<Plan> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    /**
     * Available plans for CUSTOMERS:
     * - active flag = true
     * - status = ACTIVE
     * - startDate <= today AND endDate >= today
     */
    Page<Plan> findByStatusAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            PlanStatus status, LocalDate currentDate1, LocalDate currentDate2, Pageable pageable
    );

    /**
     * Search available plans for CUSTOMERS by name.
     */
    Page<Plan> findByNameContainingIgnoreCaseAndStatusAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String name, PlanStatus status, LocalDate currentDate1, LocalDate currentDate2, Pageable pageable
    );

    /**
     * Auto-close scheduled job query:
     * Find plans with autoClose=true, status not already CLOSED, and endDate before today.
     */
    List<Plan> findByAutoCloseTrueAndStatusNotAndEndDateBefore(PlanStatus status, LocalDate currentDate);
}

