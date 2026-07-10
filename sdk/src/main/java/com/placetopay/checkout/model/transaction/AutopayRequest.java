package com.placetopay.checkout.model.transaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.model.common.Amount;
import java.time.LocalDate;

/** Required payload when {@code CreateSessionRequest.type=autopay}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AutopayRequest(
        String periodicity,
        Integer interval,
        LocalDate nextPayment,
        Integer maxPeriods,
        Amount amount,
        LocalDate dueDate,
        String notificationUrl) {}
