package com.placetopay.checkout.error;

import com.placetopay.checkout.model.common.Status;
import java.time.Duration;

/**
 * HTTP 429 — client should back off. Exposes {@link #retryAfter()} when the server hints a delay.
 */
public class RateLimitException extends PlaceToPayException {

    private final Duration retryAfter;

    public RateLimitException(
            String message,
            Status status,
            String reason,
            Long requestId,
            String rawBody,
            Duration retryAfter) {
        super(message, status, reason, requestId, rawBody, null);
        this.retryAfter = retryAfter;
    }

    /**
     * @return the {@code Retry-After} duration hinted by the server, or {@code null} if none.
     */
    public Duration retryAfter() {
        return retryAfter;
    }
}
