package com.brightloop.subscriptionmanagement.scheduler;

import com.brightloop.subscriptionmanagement.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled jobs for subscription plans.
 * Automatically checks and closes expired plans that have autoClose=true.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanScheduler {

    private final PlanService planService;

    /**
     * Runs every hour at minute 0 (0 * * * * *) to auto-close plans whose endDate has passed.
     * Also runs on application startup after initial delay.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runAutoCloseExpiredPlans() {
        log.info("Starting scheduled job: Auto-closing expired subscription plans");
        try {
            int closedCount = planService.autoCloseExpiredPlans();
            log.info("Scheduled job finished: Successfully auto-closed {} expired plan(s)", closedCount);
        } catch (Exception e) {
            log.error("Error executing autoCloseExpiredPlans scheduler: {}", e.getMessage(), e);
        }
    }
}

