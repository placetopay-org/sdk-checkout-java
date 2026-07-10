package com.placetopay.checkout.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/**
 * Currency + total pair. Used stand-alone (e.g. dispersion items) and as a base for {@link Amount}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AmountBase(String currency, BigDecimal total) {}
