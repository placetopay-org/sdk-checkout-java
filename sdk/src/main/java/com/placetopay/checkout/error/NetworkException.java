package com.placetopay.checkout.error;

/**
 * Indicates a failure before the server returned an HTTP status: DNS, TCP, TLS handshake, read
 * timeout, etc. Wraps the original {@link java.io.IOException} or equivalent as the {@link
 * Throwable#getCause() cause}.
 */
public class NetworkException extends PlaceToPayException {
    public NetworkException(String message, Throwable cause) {
        super(message, null, null, null, null, cause);
    }
}
