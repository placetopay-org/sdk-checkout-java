package com.placetopay.checkout.model.transaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.model.common.AmountConversion;
import com.placetopay.checkout.model.common.Discount;
import com.placetopay.checkout.model.common.Status;
import java.util.List;
import java.util.Map;

/**
 * A single transaction inside a session.
 *
 * <p>Convenience accessors {@link #isApproved()}, {@link #isRejected()} and {@link #isSuccessful()}
 * mirror the PHP SDK's per-transaction predicates so callers don't have to reach through {@link
 * #status()} every time.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Transaction(
        Status status,
        String reference,
        Long internalReference,
        String paymentMethod,
        String paymentMethodName,
        String issuerName,
        AmountConversion amount,
        String authorization,
        String receipt,
        String franchise,
        Boolean refunded,
        List<ProcessorField> processorFields,
        String agreement,
        String agreementType,
        Map<String, Object> dispersion,
        Discount discount,
        SubscriptionInformation subscription) {
    /** {@code true} when the wrapped {@link Status} is exactly APPROVED. */
    @JsonIgnore
    public boolean isApproved() {
        return status != null && status.isApproved();
    }

    /** {@code true} when the wrapped {@link Status} is exactly REJECTED. */
    @JsonIgnore
    public boolean isRejected() {
        return status != null && status.isRejected();
    }

    /** {@code true} for any of OK / APPROVED / APPROVED_PARTIAL. */
    @JsonIgnore
    public boolean isSuccessful() {
        return status != null && status.isSuccessful();
    }
}
