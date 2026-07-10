package com.placetopay.checkout.webhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.model.common.Status;

/**
 * Async notification payload that PlaceToPay POSTs to the merchant's configured URL.
 *
 * <p>Use with {@link NotificationVerifier} to authenticate the payload before acting on it.
 *
 * <p>The {@code signature} field is the server's HMAC-style digest over {@code requestId +
 * status.status + status.date + secretKey}. See {@link NotificationVerifier} for the exact
 * algorithm and a note about the SHA-256 vs legacy SHA-1 variants.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationPayload(
        Long requestId, String reference, String signature, Status status) {}
