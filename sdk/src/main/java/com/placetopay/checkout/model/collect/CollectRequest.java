package com.placetopay.checkout.model.collect;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.auth.Auth;
import com.placetopay.checkout.model.common.NameValuePair;
import com.placetopay.checkout.model.common.Person;
import com.placetopay.checkout.model.instrument.Instrument;
import com.placetopay.checkout.model.session.Payment;
import java.util.List;

/**
 * Body of {@code POST /api/collect}.
 *
 * <p>The optional {@code provider} field mirrors the PHP SDK's {@code provider} parameter — pins
 * the collect call to a specific payment processor when the gateway has more than one route
 * available.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CollectRequest(
        Auth auth,
        String locale,
        Person payer,
        Person buyer,
        Payment payment,
        Instrument instrument,
        String ipAddress,
        String userAgent,
        List<NameValuePair> fields,
        String provider) {
    /** Copy with the supplied auth block injected by the SDK. */
    public CollectRequest withAuth(Auth newAuth) {
        return new CollectRequest(
                newAuth,
                locale,
                payer,
                buyer,
                payment,
                instrument,
                ipAddress,
                userAgent,
                fields,
                provider);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String locale;
        private Person payer;
        private Person buyer;
        private Payment payment;
        private Instrument instrument;
        private String ipAddress;
        private String userAgent;
        private List<NameValuePair> fields;
        private String provider;

        public Builder locale(String v) {
            this.locale = v;
            return this;
        }

        public Builder payer(Person v) {
            this.payer = v;
            return this;
        }

        public Builder buyer(Person v) {
            this.buyer = v;
            return this;
        }

        public Builder payment(Payment v) {
            this.payment = v;
            return this;
        }

        public Builder instrument(Instrument v) {
            this.instrument = v;
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

        public Builder fields(List<NameValuePair> v) {
            this.fields = v;
            return this;
        }

        public Builder provider(String v) {
            this.provider = v;
            return this;
        }

        public CollectRequest build() {
            return new CollectRequest(
                    null,
                    locale,
                    payer,
                    buyer,
                    payment,
                    instrument,
                    ipAddress,
                    userAgent,
                    fields,
                    provider);
        }
    }
}
