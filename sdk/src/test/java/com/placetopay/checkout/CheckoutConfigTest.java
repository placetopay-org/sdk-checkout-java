package com.placetopay.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CheckoutConfigTest {

    @Test
    void defaultsAreSensible() {
        CheckoutConfig cfg = CheckoutConfig.builder().login("L").secretKey("S").build();
        assertThat(cfg.environment()).isEqualTo(Environment.SANDBOX);
        assertThat(cfg.country()).isNull();
        assertThat(cfg.baseUrl()).isEqualTo("https://checkout-test.placetopay.com");
        assertThat(cfg.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(cfg.readTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(cfg.retry().maxAttempts()).isEqualTo(3);
        assertThat(cfg.additionalHeaders()).isEmpty();
        assertThat(cfg.authAdditional()).isNull();
    }

    @Test
    void productionBaseUrl() {
        CheckoutConfig cfg =
                CheckoutConfig.builder()
                        .login("L")
                        .secretKey("S")
                        .environment(Environment.PRODUCTION)
                        .build();
        assertThat(cfg.baseUrl()).isEqualTo("https://checkout.placetopay.com");
    }

    @Test
    void ecuadorCountryOverridesBaseUrl() {
        CheckoutConfig cfg =
                CheckoutConfig.builder()
                        .login("L")
                        .secretKey("S")
                        .environment(Environment.PRODUCTION)
                        .country(Country.ECUADOR)
                        .build();
        assertThat(cfg.baseUrl()).isEqualTo("https://checkout.placetopay.ec");
        assertThat(cfg.defaultCurrency()).isEqualTo("USD");
        assertThat(cfg.defaultLocale()).isEqualTo("es_EC");
    }

    @Test
    void ecuadorSandboxOverride() {
        CheckoutConfig cfg =
                CheckoutConfig.builder()
                        .login("L")
                        .secretKey("S")
                        .environment(Environment.SANDBOX)
                        .country(Country.ECUADOR)
                        .build();
        assertThat(cfg.baseUrl()).isEqualTo("https://checkout-test.placetopay.ec");
    }

    @Test
    void rejectsHttpOnNonLoopbackHosts() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(
                        () ->
                                CheckoutConfig.builder()
                                        .login("L")
                                        .secretKey("S")
                                        .baseUrl("http://example.com")
                                        .build());
    }

    @Test
    void acceptsHttpOnLocalhost() {
        CheckoutConfig cfg =
                CheckoutConfig.builder()
                        .login("L")
                        .secretKey("S")
                        .baseUrl("http://localhost:8080")
                        .build();
        assertThat(cfg.baseUrl()).isEqualTo("http://localhost:8080");
    }

    @Test
    void acceptsHttpOn127_0_0_1() {
        CheckoutConfig cfg =
                CheckoutConfig.builder()
                        .login("L")
                        .secretKey("S")
                        .baseUrl("http://127.0.0.1:9000")
                        .build();
        assertThat(cfg.baseUrl()).isEqualTo("http://127.0.0.1:9000");
    }

    @Test
    void trailingSlashNormalized() {
        CheckoutConfig cfg =
                CheckoutConfig.builder()
                        .login("L")
                        .secretKey("S")
                        .baseUrl("https://example.com/api/")
                        .build();
        assertThat(cfg.baseUrl()).isEqualTo("https://example.com/api");
    }

    @Test
    void builderSettersAllExposed() {
        HttpClient http = HttpClient.newHttpClient();
        CheckoutConfig cfg =
                CheckoutConfig.builder()
                        .login("L")
                        .secretKey("S")
                        .environment(Environment.SANDBOX)
                        .country(Country.COLOMBIA)
                        .connectTimeout(Duration.ofSeconds(3))
                        .readTimeout(Duration.ofSeconds(7))
                        .retry(CheckoutConfig.RetrySettings.disabled())
                        .userAgentSuffix("my-app/1.0")
                        .additionalHeaders(Map.of("X-Trace", "abc"))
                        .authAdditional(Map.of("tenant", "acme"))
                        .httpClient(http)
                        .build();
        assertThat(cfg.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(cfg.readTimeout()).isEqualTo(Duration.ofSeconds(7));
        assertThat(cfg.retry().maxAttempts()).isEqualTo(1);
        assertThat(cfg.userAgentSuffix()).isEqualTo("my-app/1.0");
        assertThat(cfg.additionalHeaders()).containsEntry("X-Trace", "abc");
        assertThat(cfg.authAdditional()).containsEntry("tenant", "acme");
        assertThat(cfg.httpClient()).isSameAs(http);
    }

    @Test
    void retrySettingsDefaults() {
        CheckoutConfig.RetrySettings r = CheckoutConfig.RetrySettings.defaults();
        assertThat(r.maxAttempts()).isEqualTo(3);
        assertThat(r.honorRetryAfter()).isTrue();
    }

    @Test
    void retrySettingsDisabled() {
        CheckoutConfig.RetrySettings r = CheckoutConfig.RetrySettings.disabled();
        assertThat(r.maxAttempts()).isEqualTo(1);
        assertThat(r.honorRetryAfter()).isFalse();
    }

    @Test
    void retrySettingsRejectsInvalid() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(
                        () ->
                                new CheckoutConfig.RetrySettings(
                                        0, Duration.ZERO, Duration.ZERO, false));
    }

    @Test
    void requiresLoginAndSecretKey() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> CheckoutConfig.builder().secretKey("S").build());
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> CheckoutConfig.builder().login("L").build());
    }
}
