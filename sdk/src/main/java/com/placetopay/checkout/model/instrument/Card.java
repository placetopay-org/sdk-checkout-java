package com.placetopay.checkout.model.instrument;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Raw card details. The {@code number} and {@code cvv} fields are redacted from SDK logs — see
 * ARCHITECTURE.md §9.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Card(
        String name,
        /** PAN — masked in responses; redacted in debug logs. */
        String number,
        /** CVV — never logged. */
        String cvv,
        /** Two-digit month {@code 01}–{@code 12}. */
        String expirationMonth,
        /** Two-digit year. */
        String expirationYear,
        Integer installments,
        /** {@code C}=credit, {@code A}=debit savings, {@code R}=debit checking. */
        String kind) {}
