package com.placetopay.checkout.model.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.auth.Auth;
import com.placetopay.checkout.model.common.NameValuePair;
import com.placetopay.checkout.model.common.Person;
import com.placetopay.checkout.model.transaction.AutopayRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /api/session}.
 *
 * <p>At least one of {@link #payment()}, {@link #subscription()} or {@link #autopay()} must be
 * provided (server-enforced; see {@code openapi/checkout.yaml}).
 *
 * <p>Use the {@link Builder} for readable construction in application code — the canonical record
 * constructor is intended for deserialization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateSessionRequest(
        Auth auth,
        String locale,
        Person buyer,
        Person payer,
        Payment payment,
        SubscriptionRequest subscription,
        String returnUrl,
        String cancelUrl,
        String ipAddress,
        String userAgent,
        OffsetDateTime expiration,
        /** Comma-separated method codes (e.g. {@code "visa,master,pse"}). */
        String paymentMethod,
        Boolean skipResult,
        Boolean noBuyerFill,
        Boolean captureAddress,
        List<NameValuePair> fields,
        /** {@code checkin} or {@code autopay}. */
        String type,
        AutopayRequest autopay,
        /**
         * Free-form merchant metadata. Canonical keys the PlaceToPay API recognizes (v2.26.8+):
         * {@code openingDate} (ISO-8601 timestamp for when the customer opened the session) and
         * {@code beneficiaryId} (identifier of the final beneficiary for regulated verticals).
         * Extra keys are accepted and forwarded as-is.
         */
        Map<String, String> metadata,
        /**
         * API v2.27.1 — optional hint about who initiated the payment. Common values are {@code
         * CIT} (cardholder-initiated) and {@code MIT} (merchant-initiated). The gateway validates
         * the accepted vocabulary.
         */
        String initiatorIndicator) {
    /** Copy with the supplied auth block injected by the SDK. */
    public CreateSessionRequest withAuth(Auth newAuth) {
        return new CreateSessionRequest(
                newAuth,
                locale,
                buyer,
                payer,
                payment,
                subscription,
                returnUrl,
                cancelUrl,
                ipAddress,
                userAgent,
                expiration,
                paymentMethod,
                skipResult,
                noBuyerFill,
                captureAddress,
                fields,
                type,
                autopay,
                metadata,
                initiatorIndicator);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String locale;
        private Person buyer;
        private Person payer;
        private Payment payment;
        private SubscriptionRequest subscription;
        private String returnUrl;
        private String cancelUrl;
        private String ipAddress;
        private String userAgent;
        private OffsetDateTime expiration;
        private String paymentMethod;
        private Boolean skipResult;
        private Boolean noBuyerFill;
        private Boolean captureAddress;
        private List<NameValuePair> fields;
        private String type;
        private AutopayRequest autopay;
        private Map<String, String> metadata;
        private String initiatorIndicator;

        public Builder locale(String v) {
            this.locale = v;
            return this;
        }

        public Builder buyer(Person v) {
            this.buyer = v;
            return this;
        }

        public Builder payer(Person v) {
            this.payer = v;
            return this;
        }

        public Builder payment(Payment v) {
            this.payment = v;
            return this;
        }

        public Builder subscription(SubscriptionRequest v) {
            this.subscription = v;
            return this;
        }

        public Builder returnUrl(String v) {
            this.returnUrl = v;
            return this;
        }

        public Builder cancelUrl(String v) {
            this.cancelUrl = v;
            return this;
        }

        public Builder ipAddress(String v) {
            this.ipAddress = v;
            return this;
        }

        public Builder userAgent(String v) {
            this.userAgent = v;
            return this;
        }

        public Builder expiration(OffsetDateTime v) {
            this.expiration = v;
            return this;
        }

        public Builder paymentMethod(String v) {
            this.paymentMethod = v;
            return this;
        }

        public Builder skipResult(Boolean v) {
            this.skipResult = v;
            return this;
        }

        public Builder noBuyerFill(Boolean v) {
            this.noBuyerFill = v;
            return this;
        }

        public Builder captureAddress(Boolean v) {
            this.captureAddress = v;
            return this;
        }

        public Builder fields(List<NameValuePair> v) {
            this.fields = v;
            return this;
        }

        public Builder type(String v) {
            this.type = v;
            return this;
        }

        public Builder autopay(AutopayRequest v) {
            this.autopay = v;
            return this;
        }

        public Builder metadata(Map<String, String> v) {
            this.metadata = v;
            return this;
        }

        public Builder initiatorIndicator(String v) {
            this.initiatorIndicator = v;
            return this;
        }

        public CreateSessionRequest build() {
            return new CreateSessionRequest(
                    null,
                    locale,
                    buyer,
                    payer,
                    payment,
                    subscription,
                    returnUrl,
                    cancelUrl,
                    ipAddress,
                    userAgent,
                    expiration,
                    paymentMethod,
                    skipResult,
                    noBuyerFill,
                    captureAddress,
                    fields,
                    type,
                    autopay,
                    metadata,
                    initiatorIndicator);
        }
    }
}
