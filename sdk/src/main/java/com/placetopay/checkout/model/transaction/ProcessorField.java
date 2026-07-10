package com.placetopay.checkout.model.transaction;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Processor metadata field returned in transaction responses.
 *
 * <p>The gateway can emit heterogeneous values in {@code value} (booleans, numbers, strings, or
 * objects), so this record models it as {@link Object}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProcessorField(String keyword, Object value, String displayOn) {}
