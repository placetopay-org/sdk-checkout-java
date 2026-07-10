package com.placetopay.checkout.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Canonical set of status codes observed on the PlaceToPay API. Unknown values deserialize to
 * {@link #UNKNOWN} to preserve forward compatibility.
 */
public enum StatusCode {
    OK,
    APPROVED,
    APPROVED_PARTIAL,
    PARTIAL,
    PENDING,
    PENDING_VALIDATION,
    REJECTED,
    REFUNDED,
    EXPIRED,
    FAILED,
    ERROR,
    UNKNOWN;

    @JsonCreator
    public static StatusCode fromString(String raw) {
        if (raw == null) return null;
        for (StatusCode s : values()) {
            if (s.name().equalsIgnoreCase(raw)) return s;
        }
        return UNKNOWN;
    }
}
