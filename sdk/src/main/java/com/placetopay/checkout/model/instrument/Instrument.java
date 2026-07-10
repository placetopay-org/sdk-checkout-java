package com.placetopay.checkout.model.instrument;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A payment instrument. At least one of {@code token}, {@code card}, {@code bank} or {@code credit}
 * must be provided (server-enforced).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Instrument(
        Token token,
        Card card,
        Bank bank,
        Credit credit,
        /** PIN — never logged. */
        String pin,
        /** Password — never logged. */
        String password) {
    public static Instrument ofToken(Token t) {
        return new Instrument(t, null, null, null, null, null);
    }

    public static Instrument ofCard(Card c) {
        return new Instrument(null, c, null, null, null, null);
    }
}
