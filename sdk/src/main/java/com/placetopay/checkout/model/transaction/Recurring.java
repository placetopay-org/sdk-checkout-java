package com.placetopay.checkout.model.transaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Recurring(
        /** {@code D} daily, {@code M} monthly, {@code Y} yearly. */
        String periodicity,
        Integer interval,
        LocalDate nextPayment,
        /** {@code -1} for infinite. */
        Integer maxPeriods,
        LocalDate dueDate,
        String notificationUrl) {}
