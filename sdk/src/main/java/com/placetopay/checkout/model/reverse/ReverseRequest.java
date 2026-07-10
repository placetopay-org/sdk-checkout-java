package com.placetopay.checkout.model.reverse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.auth.Auth;

/** Body of {@code POST /api/reverse}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReverseRequest(
        Auth auth,
        /** The {@code internalReference} returned in a Transaction. */
        String internalReference) {
    public static ReverseRequest of(String internalReference) {
        return new ReverseRequest(null, internalReference);
    }

    public ReverseRequest withAuth(Auth newAuth) {
        return new ReverseRequest(newAuth, internalReference);
    }
}
