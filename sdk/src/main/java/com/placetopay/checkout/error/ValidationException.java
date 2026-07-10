package com.placetopay.checkout.error;

import com.placetopay.checkout.model.common.Status;

/** HTTP 400 — request validation failed. May carry field-level details in {@link #rawBody()}. */
public class ValidationException extends PlaceToPayException {
    public ValidationException(
            String message, Status status, String reason, Long requestId, String rawBody) {
        super(message, status, reason, requestId, rawBody, null);
    }
}
