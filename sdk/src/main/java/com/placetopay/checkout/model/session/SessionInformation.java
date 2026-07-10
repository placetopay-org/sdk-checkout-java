package com.placetopay.checkout.model.session;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.model.common.Status;
import com.placetopay.checkout.model.transaction.SubscriptionInformation;
import com.placetopay.checkout.model.transaction.Transaction;
import java.util.List;
import java.util.Map;

/**
 * Body of query and collect responses — full snapshot of a session.
 *
 * <p>Convenience helpers {@link #lastTransaction(boolean)}, {@link #lastApprovedTransaction()} and
 * {@link #lastAuthorization()} mirror the PHP SDK's {@code RedirectInformation} helpers so code can
 * port across languages with minimal surface-area changes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionInformation(
        Long requestId,
        Status status,
        /** Echo of the original CreateSessionRequest (without auth). Free-form map. */
        Map<String, Object> request,
        List<Transaction> payment,
        SubscriptionInformation subscription,
        List<Map<String, Object>> autopay) {
    /**
     * Most-recent transaction on this session. When {@code approvedOnly=true}, walks the list and
     * returns the first APPROVED transaction (matching PHP's {@code lastTransaction($approved)}
     * behavior). When {@code false}, returns the last element of {@link #payment()}.
     *
     * @return the chosen transaction, or {@code null} if none qualify.
     */
    @JsonIgnore
    public Transaction lastTransaction(boolean approvedOnly) {
        if (payment == null || payment.isEmpty()) {
            return null;
        }
        if (!approvedOnly) {
            return payment.get(payment.size() - 1);
        }
        for (Transaction t : payment) {
            if (t.isApproved()) {
                return t;
            }
        }
        return null;
    }

    /** Shortcut: most-recent transaction regardless of status. */
    @JsonIgnore
    public Transaction lastTransaction() {
        return lastTransaction(false);
    }

    /** Shortcut: first APPROVED transaction in order, or {@code null} if none. */
    @JsonIgnore
    public Transaction lastApprovedTransaction() {
        return lastTransaction(true);
    }

    /**
     * Authorization code of the last approved transaction, or {@code ""} when the session has no
     * approved payments yet. Safe to call at any time.
     */
    @JsonIgnore
    public String lastAuthorization() {
        Transaction t = lastApprovedTransaction();
        if (t == null || t.authorization() == null) {
            return "";
        }
        return t.authorization();
    }
}
