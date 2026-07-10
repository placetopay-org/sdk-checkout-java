package com.placetopay.checkout.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Verifies incoming webhook notifications from PlaceToPay.
 *
 * <h2>Algorithm</h2>
 *
 * The server builds the signature as:
 *
 * <pre>
 *     base = requestId || status.status || status.date || secretKey
 *     signature = hex( HASH( base ) )
 * </pre>
 *
 * <h2>Hash function</h2>
 *
 * <ul>
 *   <li><strong>{@link #verifySha256}</strong> — use for servers on the modern SHA-256 standard
 *       (matches the outgoing-request signing algorithm).
 *   <li><strong>{@link #verifySha1Legacy}</strong> — backward-compat for servers that still emit
 *       SHA-1 hex (the algorithm used by the reference Java SDK {@code
 *       placetopay/java-placetopay}).
 * </ul>
 *
 * <p>The current docs page for notifications returns HTTP 403 publicly, so the exact server-side
 * algorithm can vary by environment. Prefer {@code verifySha256} and fall back to the legacy path
 * only if verification fails. Once the server behavior is confirmed the deprecated path should be
 * removed.
 */
public final class NotificationVerifier {

    private NotificationVerifier() {}

    /**
     * Verify the signature using SHA-256 hex.
     *
     * @return {@code true} if the payload's {@code signature} matches the expected SHA-256 digest,
     *     {@code false} otherwise.
     */
    public static boolean verifySha256(NotificationPayload payload, String secretKey) {
        return verify(payload, secretKey, "SHA-256");
    }

    /**
     * Verify the signature using SHA-1 hex (legacy; scheduled for removal in v2).
     *
     * <p><strong>Prefer {@link #verifySha256}</strong>. SHA-1 is cryptographically deprecated and
     * this path is retained only for interop with deployments still on the PHP SDK's default
     * algorithm ({@code $algorithm = 'sha1'}).
     *
     * @deprecated SHA-1 is deprecated. Use {@link #verifySha256}; this method will be removed in
     *     SDK v2.0.0. Open a ticket with PlaceToPay to enable SHA-256 webhook signing on your
     *     merchant account.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public static boolean verifySha1Legacy(NotificationPayload payload, String secretKey) {
        return verify(payload, secretKey, "SHA-1");
    }

    private static boolean verify(NotificationPayload payload, String secretKey, String algorithm) {
        if (payload == null || payload.signature() == null || payload.status() == null) {
            return false;
        }
        String expected =
                digestHex(
                        String.valueOf(payload.requestId()),
                        payload.status().status() == null ? "" : payload.status().status().name(),
                        formatDate(payload.status().date()),
                        secretKey,
                        algorithm);
        // Constant-time comparison.
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                payload.signature().getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * ISO-8601 with offset (e.g. {@code 2026-04-22T15:30:45+00:00}). Returns an empty string for a
     * null input. Matches the seed format used for signing.
     */
    private static String formatDate(OffsetDateTime date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx"));
    }

    private static String digestHex(
            String requestId, String statusCode, String date, String secretKey, String algorithm) {
        String base = requestId + statusCode + date + secretKey;
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(base.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithm not available: " + algorithm, e);
        }
    }
}
