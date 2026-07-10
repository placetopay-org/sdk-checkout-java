package com.placetopay.checkout.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.placetopay.checkout.internal.LenientOffsetDateTimeDeserializer;
import java.time.OffsetDateTime;

/**
 * Status block returned by most endpoints. Convenience predicates mirror the PHP SDK's semantics
 * ({@code isApproved()}, {@code isRejected()}, {@code isError()}) so code can port across languages
 * without changing behavior.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Status(
        StatusCode status,
        String reason,
        String message,
        @JsonDeserialize(using = LenientOffsetDateTimeDeserializer.class) OffsetDateTime date) {
    /**
     * {@code true} when {@link #status()} is exactly {@code APPROVED}. Matches PHP {@code
     * isApproved()}.
     */
    public boolean isApproved() {
        return status == StatusCode.APPROVED;
    }

    /**
     * {@code true} when {@link #status()} is exactly {@code REJECTED}. Matches PHP {@code
     * isRejected()}.
     */
    public boolean isRejected() {
        return status == StatusCode.REJECTED;
    }

    /**
     * {@code true} when {@link #status()} is exactly {@code ERROR}. Matches PHP {@code isError()}.
     */
    public boolean isError() {
        return status == StatusCode.ERROR;
    }

    /**
     * Permissive success check — {@code OK}, {@code APPROVED}, or {@code APPROVED_PARTIAL}. Use
     * this when you want "the operation worked" rather than "fully approved".
     */
    public boolean isSuccessful() {
        return status == StatusCode.OK
                || status == StatusCode.APPROVED
                || status == StatusCode.APPROVED_PARTIAL;
    }

    /** {@code true} when pending processor/user action. */
    public boolean isPending() {
        return status == StatusCode.PENDING || status == StatusCode.PENDING_VALIDATION;
    }
}
