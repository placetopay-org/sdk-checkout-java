package com.placetopay.checkout;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.placetopay.checkout.model.collect.CollectRequest;
import com.placetopay.checkout.model.common.Amount;
import com.placetopay.checkout.model.common.DocumentType;
import com.placetopay.checkout.model.common.Person;
import com.placetopay.checkout.model.common.Status;
import com.placetopay.checkout.model.common.StatusCode;
import com.placetopay.checkout.model.instrument.Instrument;
import com.placetopay.checkout.model.instrument.InvalidateTokenRequest;
import com.placetopay.checkout.model.instrument.Token;
import com.placetopay.checkout.model.session.CreateSessionRequest;
import com.placetopay.checkout.model.session.Payment;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests of every public facade method on {@link Checkout} — routes through the real
 * {@link com.placetopay.checkout.http.HttpTransport}, real {@link
 * com.placetopay.checkout.auth.AuthSigner}, and a WireMock server.
 */
class CheckoutFacadeTest {

    private WireMockServer server;
    private Checkout client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        CheckoutConfig cfg =
                CheckoutConfig.builder()
                        .login("test-login")
                        .secretKey("test-secret")
                        .environment(Environment.SANDBOX)
                        .country(Country.COLOMBIA)
                        .baseUrl(server.baseUrl())
                        .connectTimeout(Duration.ofSeconds(5))
                        .readTimeout(Duration.ofSeconds(5))
                        .retry(
                                new CheckoutConfig.RetrySettings(
                                        1, Duration.ofMillis(1), Duration.ofMillis(5), true))
                        .userAgentSuffix("tests/1.0")
                        .authAdditional(java.util.Map.of("tenant", "acme"))
                        .build();
        client = Checkout.of(cfg);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private static final String OK =
            """
        {"requestId":42,"processUrl":"https://example.com/r","status":{"status":"OK","reason":"00","message":"ok","date":"2026-04-22T15:30:45+00:00"}}
        """;

    private static final String OK_SESSION_INFO =
            """
        {"requestId":42,"status":{"status":"OK","reason":"00","message":"ok","date":"2026-04-22T15:30:45+00:00"},"payment":[{"status":{"status":"APPROVED","date":"2026-04-22T15:30:45+00:00"},"authorization":"AUTH-1","internalReference":1}]}
        """;

    @Test
    void createSessionInjectsAuthAndLocale() {
        server.stubFor(
                post(urlPathEqualTo("/api/session"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(OK)));
        var req =
                CreateSessionRequest.builder()
                        .ipAddress("127.0.0.1")
                        .userAgent("t/1.0")
                        .returnUrl("https://example.com/return")
                        .payment(Payment.of("R-1", "D", Amount.of("COP", new BigDecimal("50000"))))
                        .build();
        var resp = client.createSession(req);
        assertThat(resp.requestId()).isEqualTo(42L);
        assertThat(resp.processUrl()).isEqualTo("https://example.com/r");
        server.verify(
                postRequestedFor(urlPathEqualTo("/api/session"))
                        .withRequestBody(matchingJsonPath("$.auth.login", equalTo("test-login")))
                        .withRequestBody(matchingJsonPath("$.locale", equalTo("es_CO")))
                        .withRequestBody(
                                matchingJsonPath("$.auth.additional.tenant", equalTo("acme"))));
    }

    @Test
    void createSessionWithIdempotencyKey() {
        server.stubFor(
                post(urlPathEqualTo("/api/session"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(OK)));
        var req =
                CreateSessionRequest.builder()
                        .ipAddress("127.0.0.1")
                        .userAgent("t/1.0")
                        .returnUrl("https://example.com/return")
                        .payment(Payment.of("R", "D", Amount.of("COP", new BigDecimal("100"))))
                        .build();
        client.createSession(req, "IDEM-1");
        server.verify(
                postRequestedFor(urlPathEqualTo("/api/session"))
                        .withHeader("X-Idempotency-Key", equalTo("IDEM-1")));
    }

    @Test
    void querySession() {
        server.stubFor(
                post(urlPathEqualTo("/api/session/42"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(OK_SESSION_INFO)));
        var info = client.querySession(42L);
        assertThat(info.requestId()).isEqualTo(42L);
        assertThat(info.lastApprovedTransaction()).isNotNull();
        assertThat(info.lastAuthorization()).isEqualTo("AUTH-1");
    }

    @Test
    void cancelSession() {
        String body =
                """
            {"status":{"status":"OK","date":"2026-04-22T15:30:45+00:00"},
             "session":{"requestId":42,"status":{"status":"REJECTED","date":"2026-04-22T15:30:45+00:00"}}}
            """;
        server.stubFor(
                post(urlPathEqualTo("/api/session/42/cancel"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(body)));
        var resp = client.cancelSession(42L);
        assertThat(resp.status().status()).isEqualTo(StatusCode.OK);
        assertThat(resp.session().status().status()).isEqualTo(StatusCode.REJECTED);
    }

    @Test
    void collect() {
        server.stubFor(
                post(urlPathEqualTo("/api/collect"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(OK_SESSION_INFO)));
        var req =
                CollectRequest.builder()
                        .payer(
                                new Person(
                                        "1040",
                                        DocumentType.CC,
                                        "T",
                                        "B",
                                        null,
                                        "t@e.co",
                                        null,
                                        null))
                        .payment(Payment.of("C-1", "D", Amount.of("COP", new BigDecimal("10000"))))
                        .instrument(
                                Instrument.ofToken(
                                        new Token(
                                                "tk", null, null, null, null, null, null, null,
                                                null)))
                        .provider("evertec")
                        .build();
        var info = client.collect(req);
        assertThat(info.requestId()).isEqualTo(42L);
        server.verify(
                postRequestedFor(urlPathEqualTo("/api/collect"))
                        .withRequestBody(matchingJsonPath("$.provider", equalTo("evertec"))));
    }

    @Test
    void collectWithIdempotencyKey() {
        server.stubFor(
                post(urlPathEqualTo("/api/collect"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(OK_SESSION_INFO)));
        var req =
                CollectRequest.builder()
                        .payer(
                                new Person(
                                        "1040",
                                        DocumentType.CC,
                                        "T",
                                        "B",
                                        null,
                                        "t@e.co",
                                        null,
                                        null))
                        .payment(Payment.of("C-1", "D", Amount.of("COP", new BigDecimal("10000"))))
                        .instrument(
                                Instrument.ofToken(
                                        new Token(
                                                "tk", null, null, null, null, null, null, null,
                                                null)))
                        .build();
        client.collect(req, "IDEM-C");
        server.verify(
                postRequestedFor(urlPathEqualTo("/api/collect"))
                        .withHeader("X-Idempotency-Key", equalTo("IDEM-C")));
    }

    @Test
    void reverse() {
        String body =
                """
            {"status":{"status":"APPROVED","date":"2026-04-22T15:30:45+00:00"},
             "payment":{"status":{"status":"REFUNDED","date":"2026-04-22T15:30:45+00:00"},"internalReference":1}}
            """;
        server.stubFor(
                post(urlPathEqualTo("/api/reverse"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(body)));
        var resp = client.reverse("42");
        assertThat(resp.status().isApproved()).isTrue();
        server.verify(
                postRequestedFor(urlPathEqualTo("/api/reverse"))
                        .withRequestBody(matchingJsonPath("$.internalReference", equalTo("42"))));
    }

    @Test
    void reverseWithIdempotencyKey() {
        String body =
                "{\"status\":{\"status\":\"APPROVED\",\"date\":\"2026-04-22T15:30:45+00:00\"}}";
        server.stubFor(
                post(urlPathEqualTo("/api/reverse"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(body)));
        client.reverse("42", "IDEM-R");
        server.verify(
                postRequestedFor(urlPathEqualTo("/api/reverse"))
                        .withHeader("X-Idempotency-Key", equalTo("IDEM-R")));
    }

    @Test
    void invalidateToken() {
        String body =
                "{\"status\":{\"status\":\"OK\",\"reason\":\"00\",\"message\":\"ok\",\"date\":\"2026-04-22T15:30:45+00:00\"}}";
        server.stubFor(
                post(urlPathEqualTo("/api/instrument/invalidate"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(body)));
        var req = InvalidateTokenRequest.ofToken("tk");
        Status status = client.invalidateToken(req);
        assertThat(status.status()).isEqualTo(StatusCode.OK);
    }

    @Test
    void countryDefaultLocaleApplied() {
        server.stubFor(
                post(urlPathEqualTo("/api/session"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(OK)));
        var req =
                CreateSessionRequest.builder()
                        .ipAddress("127.0.0.1")
                        .userAgent("t/1.0")
                        .returnUrl("https://example.com/return")
                        .payment(Payment.of("R", "D", Amount.of("COP", new BigDecimal("100"))))
                        .build();
        client.createSession(req);
        server.verify(
                postRequestedFor(urlPathEqualTo("/api/session"))
                        .withRequestBody(matchingJsonPath("$.locale", equalTo("es_CO"))));
    }

    @Test
    void callerLocaleWinsOverCountryDefault() {
        server.stubFor(
                post(urlPathEqualTo("/api/session"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(OK)));
        var req =
                CreateSessionRequest.builder()
                        .ipAddress("127.0.0.1")
                        .userAgent("t/1.0")
                        .returnUrl("https://example.com/return")
                        .locale("en_US")
                        .payment(Payment.of("R", "D", Amount.of("COP", new BigDecimal("100"))))
                        .build();
        client.createSession(req);
        server.verify(
                postRequestedFor(urlPathEqualTo("/api/session"))
                        .withRequestBody(matchingJsonPath("$.locale", equalTo("en_US"))));
    }

    @Test
    void configurationAccessorsExposed() {
        assertThat(client.config().login()).isEqualTo("test-login");
        assertThat(client.config().country()).isEqualTo(Country.COLOMBIA);
        assertThat(client.config().environment()).isEqualTo(Environment.SANDBOX);
        assertThat(client.config().defaultCurrency()).isEqualTo("COP");
        assertThat(client.config().defaultLocale()).isEqualTo("es_CO");
        assertThat(client.transport()).isNotNull();
    }
}
