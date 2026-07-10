package com.placetopay.checkout.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic signing helpers that produce the {@link Auth} block required on every PlaceToPay
 * request.
 *
 * <p>Per ARCHITECTURE.md §3 the algorithm is:
 *
 * <pre>
 *   rawNonce  = cryptoRandomBytes(16)
 *   seed      = now(UTC).toIsoString()      // yyyy-MM-dd'T'HH:mm:ss+00:00
 *   digestIn  = rawNonce || utf8(seed) || utf8(secretKey)
 *   tranKey   = base64( SHA256( digestIn ) )
 *   wireNonce = base64( rawNonce )
 * </pre>
 *
 * All three SDKs (Java, Python, .NET) must produce identical output for the same inputs; see {@code
 * AuthSignerTest} for the regression vector.
 */
public final class AuthSigner {

    /**
     * Matches the specification in {@code analysis.md} — ISO-8601 with numeric offset ({@code
     * +00:00}), never the compact {@code Z} form. The Python and .NET SDKs format their seed
     * identically.
     */
    public static final DateTimeFormatter SEED_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    // Single thread-safe RNG reused across calls. Default SecureRandom selects
    // the platform's non-blocking CSPRNG (NativePRNG / Windows-PRNG / etc.) and
    // is cryptographically secure. We deliberately do NOT use
    // SecureRandom.getInstanceStrong(): on Linux it reads from /dev/random,
    // which can block on entropy-starved hosts (containers cold-start, FaaS,
    // CI runners). NIST SP 800-90A and OWASP both endorse the default
    // SecureRandom for nonce generation; the strength is identical.
    private static final SecureRandom RNG = new SecureRandom();

    private AuthSigner() {}

    /**
     * Generates 16 cryptographically-secure random bytes from a non-blocking source. Suitable for
     * cold-start environments (containers, FaaS) where blocking on /dev/random could stall the
     * first request.
     */
    public static byte[] generateNonce() {
        byte[] bytes = new byte[16];
        RNG.nextBytes(bytes);
        return bytes;
    }

    /** Produces the current ISO-8601 seed for the given clock, truncated to seconds in UTC. */
    public static String isoSeedNow(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        return OffsetDateTime.ofInstant(now, ZoneOffset.UTC).format(SEED_FORMAT);
    }

    /** Convenience: seed for the system UTC clock. */
    public static String isoSeedNow() {
        return isoSeedNow(Clock.systemUTC());
    }

    /**
     * Builds an {@link Auth} object from the given inputs. Pure function; given the same arguments
     * it always produces the same output. Used for both live signing (random nonce + current clock)
     * and deterministic test vectors.
     *
     * @param login merchant login
     * @param rawNonce the 16 raw bytes (not base64) that will become the wire nonce
     * @param seed pre-formatted ISO-8601 seed string
     * @param secretKey merchant secret — never transmitted, only mixed into the digest
     * @return the wire-ready auth record
     */
    public static Auth sign(String login, byte[] rawNonce, String seed, String secretKey) {
        return sign(login, rawNonce, seed, secretKey, null);
    }

    /**
     * Same as {@link #sign(String, byte[], String, String)} with an optional {@code additional}
     * metadata map. The map is transmitted inside the auth block as the {@code additional} field
     * but is NOT part of the signing digest — changing it does not change the resulting {@code
     * tranKey}.
     */
    public static Auth sign(
            String login,
            byte[] rawNonce,
            String seed,
            String secretKey,
            Map<String, String> additional) {
        Objects.requireNonNull(login, "login");
        Objects.requireNonNull(rawNonce, "rawNonce");
        Objects.requireNonNull(seed, "seed");
        Objects.requireNonNull(secretKey, "secretKey");

        byte[] seedBytes = seed.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

        byte[] digestIn = new byte[rawNonce.length + seedBytes.length + keyBytes.length];
        System.arraycopy(rawNonce, 0, digestIn, 0, rawNonce.length);
        System.arraycopy(seedBytes, 0, digestIn, rawNonce.length, seedBytes.length);
        System.arraycopy(
                keyBytes, 0, digestIn, rawNonce.length + seedBytes.length, keyBytes.length);

        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(digestIn);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available in this JVM", e);
        }

        String tranKey = Base64.getEncoder().encodeToString(digest);
        String wireNonce = Base64.getEncoder().encodeToString(rawNonce);
        Map<String, String> extra =
                (additional == null || additional.isEmpty()) ? null : additional;
        return new Auth(login, tranKey, wireNonce, seed, extra);
    }

    /**
     * Convenience: generate a fresh nonce + seed and sign. Prefer this in production; use {@link
     * #sign(String, byte[], String, String)} in tests for determinism.
     */
    public static Auth signNow(String login, String secretKey) {
        return signNow(login, secretKey, null);
    }

    /** Same as {@link #signNow(String, String)} with optional auth metadata. */
    public static Auth signNow(String login, String secretKey, Map<String, String> additional) {
        return sign(login, generateNonce(), isoSeedNow(), secretKey, additional);
    }
}
