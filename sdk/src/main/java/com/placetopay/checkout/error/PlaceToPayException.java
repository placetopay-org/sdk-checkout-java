package com.placetopay.checkout.error;

import com.placetopay.checkout.model.common.Status;

/**
 * Base unchecked exception for all SDK errors. Callers that want coarse-grained error handling can
 * catch this single type.
 *
 * <p>Carries (when available): the API-level {@link Status} object, the short {@code reason} code,
 * a human {@code message}, the session {@code requestId} and the raw response body (for debugging
 * only — never parse it yourself).
 */
public class PlaceToPayException extends RuntimeException {

    private final Status status;
    private final String reason;
    private final Long requestId;
    private final String rawBody;

    public PlaceToPayException(
            String message,
            Status status,
            String reason,
            Long requestId,
            String rawBody,
            Throwable cause) {
        super(message, cause);
        this.status = status;
        this.reason = reason;
        this.requestId = requestId;
        this.rawBody = rawBody;
    }

    public PlaceToPayException(String message, Throwable cause) {
        this(message, null, null, null, null, cause);
    }

    public PlaceToPayException(String message) {
        this(message, null, null, null, null, null);
    }

    /**
     * @return the API {@code Status} block, if the server returned one. May be {@code null}.
     */
    public Status status() {
        return status;
    }

    /**
     * @return short reason code from the API, if known.
     */
    public String reason() {
        return reason;
    }

    /**
     * @return the {@code requestId} this error relates to, if known.
     */
    public Long requestId() {
        return requestId;
    }

    /**
     * @return the raw response body for debugging. <strong>Do not parse</strong> — structure is not
     *     part of the SDK contract.
     */
    public String rawBody() {
        return rawBody;
    }
}
