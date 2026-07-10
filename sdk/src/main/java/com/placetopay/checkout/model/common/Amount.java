package com.placetopay.checkout.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

/** Full amount block with optional tax and detail breakdowns. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Amount(
        String currency, BigDecimal total, List<TaxDetail> taxes, List<AmountDetail> details) {
    /** Shortcut for a simple currency + total amount with no breakdown. */
    public static Amount of(String currency, BigDecimal total) {
        return new Amount(currency, total, null, null);
    }
}
