package com.placetopay.checkout.error;

import com.placetopay.checkout.model.common.Status;

/** HTTP 404 — session, reference or token not found. */
public class NotFoundException extends PlaceToPayException {
    public NotFoundException(
            String message, Status status, String reason, Long requestId, String rawBody) {
        super(message, status, reason, requestId, rawBody, null);
    }
}
