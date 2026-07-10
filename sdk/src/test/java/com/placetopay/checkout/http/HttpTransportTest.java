package com.placetopay.checkout.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.placetopay.checkout.CheckoutConfig;
import com.placetopay.checkout.Environment;
import com.placetopay.checkout.error.AuthenticationException;
import com.placetopay.checkout.error.NetworkException;
import com.placetopay.checkout.error.NotFoundException;
import com.placetopay.checkout.error.PlaceToPayException;
import com.placetopay.checkout.error.RateLimitException;
import com.placetopay.checkout.error.ServerException;
import com.placetopay.checkout.error.ValidationException;
import com.placetopay.checkout.model.session.SessionInformation;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the real retry loop, auth injection and typed-error mapping against a WireMock server.
 */
class HttpTransportTest {

    private WireMockServer server;
    private HttpTransport transport;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();

        // The config's HTTPS check allows http:// for localhost, so the WireMock
        // URL (which is http://localhost:PORT) is accepted directly.
        CheckoutConfig cfg =
                CheckoutConfig.builder()
                        .login("test-login")
                        .secretKey("test-secret")
                        .environment(Environment.SANDBOX)
                        .baseUrl(server.baseUrl())
                        .connectTimeout(Duration.ofSeconds(5))
                        .readTimeout(Duration.ofSeconds(5))
                        .retry(
                                new CheckoutConfig.RetrySettings(
                                        3, Duration.ofMillis(1), Duration.ofMillis(5), true))
                        .build();
        transport = new HttpTransport(cfg);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private static final String OK_BODY =
            """
        {
          "requestId": 42,
          "status": {"status":"OK","reason":"00","message":"ok","date":"2026-04-22T15:30:45+00:00"}
        }
        """;

    private static final String NON_ISO_DATE_BODY =
            """
                                {
                                        "requestId": 42,
                                        "status": {"status":"OK","reason":"00","message":"ok","date":"2026-04-22 15:30:45"}
                                }
                                """;

    // ─── Retry ────────────────────────────────────────────────────────

    @Test
    void retriesOn500ThenSucceeds() {
        server.stubFor(
                post(urlPathEqualTo("/api/session/42"))
                        .inScenario("retry")
                        .whenScenarioStateIs("Started")
                        .willReturn(aResponse().withStatus(500))
                        .willSetStateTo("second"));
        server.stubFor(
                post(urlPathEqualTo("/api/session/42"))
                        .inScenario("retry")
                        .whenScenarioStateIs("second")
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(OK_BODY)));

        SessionInformation info =
                transport.postAuthOnly("/api/session/42", SessionInformation.class);
        assertThat(info.requestId()).isEqualTo(42L);
        server.verify(2, postRequestedFor(urlPathEqualTo("/api/session/42")));
    }

    @Test
    void toleratesNonIsoStatusDateWithoutFailingWholeResponse() {
        server.stubFor(
                post(urlPathEqualTo("/api/session/42"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(NON_ISO_DATE_BODY)));

        SessionInformation info =
                transport.postAuthOnly("/api/session/42", SessionInformation.class);

        assertThat(info.requestId()).isEqualTo(42L);
        assertThat(info.status()).isNotNull();
        assertThat(info.status().status())
                .isEqualTo(com.placetopay.checkout.model.common.StatusCode.OK);
        assertThat(info.status().date()).isNull();
    }

    @Test
    void failsAfterMaxAttemptsOn500() {
        server.stubFor(
                post(urlPathEqualTo("/api/session/42")).willReturn(aResponse().withStatus(500)));

        assertThatExceptionOfType(ServerException.class)
                .isThrownBy(
                        () -> transport.postAuthOnly("/api/session/42", SessionInformation.class))
                .satisfies(ex -> assertThat(ex.statusCode()).isEqualTo(500));
        server.verify(3, postRequestedFor(urlPathEqualTo("/api/session/42")));
    }

    @Test
    void doesNotRetryOn4xx() {
        server.stubFor(
                post(urlPathEqualTo("/api/session/42"))
                        .willReturn(
                                aResponse()
                                        .withStatus(400)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"status\":{\"status\":\"FAILED\",\"reason\":\"?X\",\"message\":\"Bad\"}}")));
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(
                        () -> transport.postAuthOnly("/api/session/42", SessionInformation.class));
        server.verify(1, postRequestedFor(urlPathEqualTo("/api/session/42")));
    }

    @Test
    void mapsStatusCodesToTypedExceptions() {
        Map<Integer, Class<? extends PlaceToPayException>> cases =
                Map.of(
                        401, AuthenticationException.class,
                        404, NotFoundException.class);
        for (Map.Entry<Integer, Class<? extends PlaceToPayException>> e : cases.entrySet()) {
            server.resetAll();
            server.stubFor(
                    post(urlPathEqualTo("/api/x"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(e.getKey())
                                            .withHeader("Content-Type", "application/json")
                                            .withBody("{\"status\":{\"status\":\"ERROR\"}}")));
            assertThatExceptionOfType(e.getValue())
                    .isThrownBy(() -> transport.postAuthOnly("/api/x", SessionInformation.class));
        }
    }

    @Test
    void rateLimitExposesRetryAfter() {
        server.stubFor(
                post(urlPathEqualTo("/api/session/42"))
                        .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "7")));
        assertThatExceptionOfType(RateLimitException.class)
                .isThrownBy(
                        () -> transport.postAuthOnly("/api/session/42", SessionInformation.class))
                .satisfies(ex -> assertThat(ex.retryAfter()).isNotNull());
    }

    // ─── Auth injection ──────────────────────────────────────────────

    @Test
    void injectsAuthBlockOnEveryRequest() {
        server.stubFor(
                post(urlPathEqualTo("/api/session/42"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(OK_BODY)));

        transport.postAuthOnly("/api/session/42", SessionInformation.class);
        transport.postAuthOnly("/api/session/42", SessionInformation.class);

        server.verify(
                postRequestedFor(urlPathEqualTo("/api/session/42"))
                        .withRequestBody(matchingJsonPath("$.auth.login", equalTo("test-login"))));
        // We got 2 hits; not asserting uniqueness of nonce (WireMock has each on record).
    }

    @Test
    void userAgentSetOnRequest() {
        server.stubFor(
                post(urlPathEqualTo("/api/session/42"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(OK_BODY)));
        transport.postAuthOnly("/api/session/42", SessionInformation.class);
        server.verify(
                postRequestedFor(urlPathEqualTo("/api/session/42"))
                        .withHeader("User-Agent", containing("placetopay-sdk-java/")));
    }

    // ─── Network failures ───────────────────────────────────────────

    @Test
    void networkFailureWrappedAsNetworkException() {
        // Stop the server to force connection failures.
        server.stop();
        try {
            assertThatExceptionOfType(NetworkException.class)
                    .isThrownBy(
                            () ->
                                    transport.postAuthOnly(
                                            "/api/session/42", SessionInformation.class));
        } finally {
            server.start(); // restart for @AfterEach stop()
        }
    }
}
