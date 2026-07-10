package com.placetopay.checkout.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/** Sub-line on the amount, e.g. discount, shipping, tip, insurance, subtotal. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AmountDetail(String kind, BigDecimal amount) {}
