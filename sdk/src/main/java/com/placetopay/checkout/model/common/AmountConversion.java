package com.placetopay.checkout.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/** Amount reported with a currency conversion applied by the gateway. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AmountConversion(AmountBase from, AmountBase to, BigDecimal factor) {}
