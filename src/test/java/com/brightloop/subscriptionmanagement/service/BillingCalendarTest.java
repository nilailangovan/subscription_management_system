package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.entity.BillingPeriod;
import com.brightloop.subscriptionmanagement.entity.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BillingCalendarTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    @Test
    void addPeriod_clampsMonthEnd() {
        assertThat(BillingCalendar.addPeriod(LocalDate.of(2026, 1, 31), BillingPeriod.MONTHLY)).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(BillingCalendar.addPeriod(LocalDate.of(2026, 10, 31), BillingPeriod.WEEKLY)).isEqualTo(LocalDate.of(2026, 11, 7));
    }

    @Test
    void nextBillingDate_activeSubscription_isFirstBoundaryAfterToday() {
        LocalDate next = BillingCalendar.nextBillingDate(LocalDate.of(2026, 8, 15), LocalDate.of(2027, 8, 15),
                BillingPeriod.MONTHLY, SubscriptionStatus.ACTIVE, TODAY);
        assertThat(next).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    void nextBillingDate_startingToday_isNextPeriod() {
        LocalDate next = BillingCalendar.nextBillingDate(TODAY, TODAY.plusYears(1), BillingPeriod.MONTHLY, SubscriptionStatus.ACTIVE, TODAY);
        assertThat(next).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void nextBillingDate_notActiveYet_isStartDate() {
        LocalDate start = TODAY.plusDays(5);
        assertThat(BillingCalendar.nextBillingDate(start, start.plusYears(1), BillingPeriod.YEARLY, SubscriptionStatus.CONFIRMED, TODAY))
                .isEqualTo(start);
    }

    @Test
    void nextBillingDate_closedOrPastExpiry_isNull() {
        assertThat(BillingCalendar.nextBillingDate(TODAY, TODAY.plusYears(1), BillingPeriod.MONTHLY, SubscriptionStatus.CLOSED, TODAY)).isNull();
        assertThat(BillingCalendar.nextBillingDate(TODAY.minusDays(20), TODAY.plusDays(5), BillingPeriod.MONTHLY, SubscriptionStatus.ACTIVE, TODAY)).isNull();
    }

    @Test
    void dueDate_readsDaysFromPaymentTerms() {
        assertThat(BillingCalendar.dueDate(TODAY, "Net 30")).isEqualTo(TODAY.plusDays(30));
        assertThat(BillingCalendar.dueDate(TODAY, "Immediate")).isEqualTo(TODAY);
        assertThat(BillingCalendar.dueDate(TODAY, null)).isEqualTo(TODAY);
    }
}
