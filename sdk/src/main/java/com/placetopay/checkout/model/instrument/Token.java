package com.placetopay.checkout.model.instrument;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.placetopay.checkout.internal.LenientOffsetDateTimeDeserializer;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Token(
        /** The tokenized instrument identifier. Redacted in debug logs except last 4. */
        String token,
        String subtoken,
        String franchise,
        String franchiseName,
        String issuerName,
        String lastDigits,
        @JsonDeserialize(using = LenientOffsetDateTimeDeserializer.class) OffsetDateTime validUntil,
        Integer installments,
        /** CVV — never logged. */
        String cvv) {}
