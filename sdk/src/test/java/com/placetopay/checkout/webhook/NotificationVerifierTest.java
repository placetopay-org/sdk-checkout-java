package com.placetopay.checkout.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.placetopay.checkout.model.common.Status;
import com.placetopay.checkout.model.common.StatusCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class NotificationVerifierTest {

    private static final String SECRET = "test-secret";
    private static final OffsetDateTime DATE =
            OffsetDateTime.of(2026, 4, 22, 15, 30, 45, 0, ZoneOffset.UTC);
    private static final String SEED_FMT = "yyyy-MM-dd'T'HH:mm:ssxxx";

    private static String hashHex(String algorithm, String s) throws Exception {
        byte[] digest =
                MessageDigest.getInstance(algorithm).digest(s.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    private static String baseFor(long requestId, StatusCode code) {
        String date = DATE.format(DateTimeFormatter.ofPattern(SEED_FMT));
        return requestId + code.name() + date + SECRET;
    }

    private static NotificationPayload payload(long requestId, StatusCode code, String signature) {
        Status status = new Status(code, "00", "ok", DATE);
        return new NotificationPayload(requestId, "ORDER-1", signature, status);
    }

    @Test
    void sha256ValidSignatureVerifies() throws Exception {
        NotificationPayload p =
                payload(
                        12345,
                        StatusCode.APPROVED,
                        hashHex("SHA-256", baseFor(12345, StatusCode.APPROVED)));
        assertThat(NotificationVerifier.verifySha256(p, SECRET)).isTrue();
    }

    @Test
    void sha256TamperedStatusFails() throws Exception {
        String goodSig = hashHex("SHA-256", baseFor(12345, StatusCode.APPROVED));
        NotificationPayload tampered = payload(12345, StatusCode.REJECTED, goodSig);
        assertThat(NotificationVerifier.verifySha256(tampered, SECRET)).isFalse();
    }

    @Test
    void sha256WrongSecretFails() throws Exception {
        NotificationPayload p =
                payload(
                        12345,
                        StatusCode.APPROVED,
                        hashHex("SHA-256", baseFor(12345, StatusCode.APPROVED)));
        assertThat(NotificationVerifier.verifySha256(p, "other")).isFalse();
    }

    @Test
    void sha256RejectsSha1Signature() throws Exception {
        NotificationPayload p =
                payload(
                        12345,
                        StatusCode.APPROVED,
                        hashHex("SHA-1", baseFor(12345, StatusCode.APPROVED)));
        assertThat(NotificationVerifier.verifySha256(p, SECRET)).isFalse();
    }

    @Test
    void nullPayloadReturnsFalse() {
        assertThat(NotificationVerifier.verifySha256(null, SECRET)).isFalse();
    }

    @Test
    void nullSignatureReturnsFalse() {
        NotificationPayload p =
                new NotificationPayload(1L, "r", null, new Status(StatusCode.OK, null, null, DATE));
        assertThat(NotificationVerifier.verifySha256(p, SECRET)).isFalse();
    }

    @Test
    void nullStatusReturnsFalse() {
        NotificationPayload p = new NotificationPayload(1L, "r", "sig", null);
        assertThat(NotificationVerifier.verifySha256(p, SECRET)).isFalse();
    }

    @Test
    @SuppressWarnings("deprecation")
    void sha1LegacyValidSignatureVerifies() throws Exception {
        NotificationPayload p =
                payload(
                        12345,
                        StatusCode.APPROVED,
                        hashHex("SHA-1", baseFor(12345, StatusCode.APPROVED)));
        assertThat(NotificationVerifier.verifySha1Legacy(p, SECRET)).isTrue();
    }

    @Test
    @SuppressWarnings("deprecation")
    void sha1LegacyRejectsSha256Signature() throws Exception {
        NotificationPayload p =
                payload(
                        12345,
                        StatusCode.APPROVED,
                        hashHex("SHA-256", baseFor(12345, StatusCode.APPROVED)));
        assertThat(NotificationVerifier.verifySha1Legacy(p, SECRET)).isFalse();
    }
}
