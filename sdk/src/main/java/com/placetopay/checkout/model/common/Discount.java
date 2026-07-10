package com.placetopay.checkout.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Explicit discount entity — mirrors the PHP SDK's {@code Discount} class.
 *
 * <p>The gateway may return this object inside an approved transaction's {@code discount} slot. The
 * SDK doesn't require callers to construct one for outgoing requests — it is primarily a response
 * shape.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Discount(String code, String type, Double amount, Double base, Double percent) {}
