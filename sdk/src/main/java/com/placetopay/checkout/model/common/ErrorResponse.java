package com.placetopay.checkout.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Generic error body returned by the API. Consumed internally by the HTTP layer to populate typed
 * exceptions; exposed only so Jackson can deserialize it.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(Status status) {}
