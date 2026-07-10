package com.placetopay.checkout.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HexFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link AuthSigner}. The primary purpose is the <strong>cross-SDK test
 * vector</strong> (ARCHITECTURE.md §3): given identical inputs, the Python, .NET and Java SDKs must
 * produce byte-exact output.
 */
class AuthSignerTest {

    private static final String LOGIN = "SDK_TEST_LOGIN";
    private static final String SECRET_KEY = "SDK_TEST_SECRET";
    private static final byte[] RAW_NONCE =
            HexFormat.of().parseHex("00112233445566778899aabbccddeeff");
    private static final String SEED = "2026-04-22T15:30:45+00:00";

    // Expected values, computed once and hardcoded. Any byte change here is a contract break.
    private static final String EXPECTED_WIRE_NONCE = "ABEiM0RVZneImaq7zN3u/w==";

    @Test
    @DisplayName("deterministic vector reproduces across languages (wire nonce)")
    void testVectorWireNonce() {
        Auth auth = AuthSigner.sign(LOGIN, RAW_NONCE, SEED, SECRET_KEY);
        assertThat(auth.nonce()).isEqualTo(EXPECTED_WIRE_NONCE);
    }

    @Test
    @DisplayName(
            "deterministic vector: tranKey is base64(SHA-256(rawNonce || utf8(seed) || utf8(secretKey)))")
    void testVectorTranKey() throws Exception {
        Auth auth = AuthSigner.sign(LOGIN, RAW_NONCE, SEED, SECRET_KEY);

        // Compute the expected value from first principles; if this matches the
        // other SDKs, they all agree on the wire format.
        byte[] digestIn =
                new byte
                        [RAW_NONCE.length
                                + SEED.getBytes("UTF-8").length
                                + SECRET_KEY.getBytes("UTF-8").length];
        System.arraycopy(RAW_NONCE, 0, digestIn, 0, RAW_NONCE.length);
        System.arraycopy(
                SEED.getBytes("UTF-8"),
                0,
                digestIn,
                RAW_NONCE.length,
                SEED.getBytes("UTF-8").length);
        System.arraycopy(
                SECRET_KEY.getBytes("UTF-8"),
                0,
                digestIn,
                RAW_NONCE.length + SEED.getBytes("UTF-8").length,
                SECRET_KEY.getBytes("UTF-8").length);

        byte[] expectedDigest = java.security.MessageDigest.getInstance("SHA-256").digest(digestIn);
        String expectedTranKey = java.util.Base64.getEncoder().encodeToString(expectedDigest);

        assertThat(auth.tranKey()).isEqualTo(expectedTranKey);
    }

    @Test
    @DisplayName("login and seed flow through verbatim")
    void loginAndSeedFlowThrough() {
        Auth auth = AuthSigner.sign(LOGIN, RAW_NONCE, SEED, SECRET_KEY);
        assertThat(auth.login()).isEqualTo(LOGIN);
        assertThat(auth.seed()).isEqualTo(SEED);
    }

    @Test
    @DisplayName("generateNonce produces 16 random bytes")
    void generateNonceLength() {
        byte[] a = AuthSigner.generateNonce();
        byte[] b = AuthSigner.generateNonce();
        assertThat(a).hasSize(16);
        assertThat(b).hasSize(16);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("isoSeedNow matches the locked '+00:00' format (not 'Z')")
    void isoSeedNowFormat() {
        String seed = AuthSigner.isoSeedNow();
        assertThat(seed)
                .endsWith("+00:00")
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+00:00");
    }

    @Test
    @DisplayName("sign rejects null arguments")
    void signRejectsNulls() {
        assertThatThrownBy(() -> AuthSigner.sign(null, RAW_NONCE, SEED, SECRET_KEY))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AuthSigner.sign(LOGIN, null, SEED, SECRET_KEY))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AuthSigner.sign(LOGIN, RAW_NONCE, null, SECRET_KEY))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AuthSigner.sign(LOGIN, RAW_NONCE, SEED, null))
                .isInstanceOf(NullPointerException.class);
    }
}
