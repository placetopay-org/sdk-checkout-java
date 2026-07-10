package com.placetopay.checkout.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Serialized auth block present on every request body sent to the PlaceToPay API.
 *
 * <p>Wire shape:
 *
 * <pre>{@code
 * {
 *   "login":      "<merchant login>",
 *   "tranKey":    "<base64 SHA-256 digest>",
 *   "nonce":      "<base64 of 16 random bytes>",
 *   "seed":       "2026-04-22T15:30:45+00:00",
 *   "additional": { "k": "v", ... }      // optional; omitted when null/empty
 * }
 * }</pre>
 *
 * <p>The {@code secretKey} is never transmitted — only the digest derived from it. The optional
 * {@code additional} map mirrors the PHP SDK's {@code authAdditional} configuration —
 * merchant-specific metadata the gateway records alongside each authenticated session. It is NOT
 * part of the signing digest.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Auth(
        @JsonProperty("login") String login,
        @JsonProperty("tranKey") String tranKey,
        @JsonProperty("nonce") String nonce,
        @JsonProperty("seed") String seed,
        @JsonProperty("additional") Map<String, String> additional) {
    /** Constructor for the common case — no {@code additional} metadata. */
    public Auth(String login, String tranKey, String nonce, String seed) {
        this(login, tranKey, nonce, seed, null);
    }
}
