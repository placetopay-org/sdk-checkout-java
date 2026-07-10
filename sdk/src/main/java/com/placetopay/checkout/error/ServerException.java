package com.placetopay.checkout.error;

import com.placetopay.checkout.model.common.Status;

/** HTTP 5xx — upstream problem. Exposes the raw HTTP status via {@link #statusCode()}. */
public class ServerException extends PlaceToPayException {

    private final int statusCode;

    public ServerException(
            int statusCode,
            String message,
            Status status,
            String reason,
            Long requestId,
            String rawBody) {
        super(message, status, reason, requestId, rawBody, null);
        this.statusCode = statusCode;
    }

    /**
     * @return the HTTP status code (5xx) returned by the server.
     */
    public int statusCode() {
        return statusCode;
    }
}
