package com.placetopay.checkout.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaxDetail(
        /** e.g. {@code valueAddedTax}. */
        String kind, BigDecimal amount, BigDecimal base) {}
