package com.placetopay.checkout.model.instrument;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.auth.Auth;

/** Request body for {@code POST /api/instrument/invalidate}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InvalidateTokenRequest(Auth auth, String locale, Instrument instrument) {
    /** Build a request without {@code auth} — the SDK injects it automatically. */
    public static InvalidateTokenRequest of(String locale, Instrument instrument) {
        return new InvalidateTokenRequest(null, locale, instrument);
    }

    /** Shortcut: invalidate by raw token string. */
    public static InvalidateTokenRequest ofToken(String token) {
        return of(
                null,
                Instrument.ofToken(
                        new Token(token, null, null, null, null, null, null, null, null)));
    }

    /** Copy with the supplied auth block injected. */
    public InvalidateTokenRequest withAuth(Auth newAuth) {
        return new InvalidateTokenRequest(newAuth, locale, instrument);
    }
}
