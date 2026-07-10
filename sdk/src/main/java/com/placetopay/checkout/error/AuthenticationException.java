package com.placetopay.checkout.error;

import com.placetopay.checkout.model.common.Status;

/** HTTP 401 — digest/credentials mismatch or expired seed. */
public class AuthenticationException extends PlaceToPayException {
    public AuthenticationException(
            String message, Status status, String reason, Long requestId, String rawBody) {
        super(message, status, reason, requestId, rawBody, null);
    }
}
