package com.placetopay.checkout.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RedactingBodyLoggerTest {

    private final RedactingBodyLogger log = new RedactingBodyLogger();

    @Test
    void masksTranKeyAndNonce() {
        String input =
                "{\"auth\":{\"login\":\"visible\",\"tranKey\":\"secret\",\"nonce\":\"n1\",\"seed\":\"2026-04-22T15:30:45+00:00\"}}";
        String out = log.redact(input);
        assertThat(out).contains("\"login\":\"visible\"");
        assertThat(out).contains("\"tranKey\":\"***\"");
        assertThat(out).contains("\"nonce\":\"***\"");
        assertThat(out).contains("2026-04-22T15:30:45");
    }

    @Test
    void masksCardPanKeepingLast4() {
        String input =
                "{\"instrument\":{\"card\":{\"number\":\"4111111111111111\",\"cvv\":\"123\"}}}";
        String out = log.redact(input);
        assertThat(out).contains("1111\"");
        assertThat(out).doesNotContain("4111111111111111");
        assertThat(out).contains("\"cvv\":\"***\"");
    }

    @Test
    void masksTokenKeepingLast4() {
        String input = "{\"instrument\":{\"token\":{\"token\":\"tk-abcdefghij\"}}}";
        String out = log.redact(input);
        assertThat(out).contains("ghij");
        assertThat(out).contains("*");
    }

    @Test
    void masksPinPasswordSecretKeyAndCreditCode() {
        String input =
                "{\"pin\":\"1234\",\"password\":\"p\",\"secretKey\":\"k\",\"credit\":{\"code\":\"XYZ\"}}";
        String out = log.redact(input);
        assertThat(out).contains("\"pin\":\"***\"");
        assertThat(out).contains("\"password\":\"***\"");
        assertThat(out).contains("\"secretKey\":\"***\"");
        assertThat(out).contains("\"code\":\"***\"");
    }

    @Test
    void recursesIntoArrays() {
        String input = "{\"payments\":[{\"card\":{\"cvv\":\"999\"}},{\"card\":{\"cvv\":\"111\"}}]}";
        String out = log.redact(input);
        assertThat(out).doesNotContain("999");
        assertThat(out).doesNotContain("111");
    }

    @Test
    void leavesNonSensitiveFieldsUntouched() {
        String input =
                "{\"reference\":\"ORDER-1\",\"amount\":{\"currency\":\"COP\",\"total\":50000}}";
        assertThat(log.redact(input)).contains("ORDER-1", "COP");
    }

    @Test
    void returnsPlaceholderForNonJson() {
        String out = log.redact("not valid json {");
        assertThat(out).startsWith("<non-json body");
    }

    @Test
    void passesThroughNullOrEmpty() {
        assertThat(log.redact(null)).isNull();
        assertThat(log.redact("")).isEmpty();
        assertThat(log.redact("   ")).isEqualTo("   ");
    }

    @Test
    void shortNumberIsMaskedFully() {
        String out = log.redact("{\"card\":{\"number\":\"1234\"}}");
        assertThat(out).contains("\"***\"");
    }
}
