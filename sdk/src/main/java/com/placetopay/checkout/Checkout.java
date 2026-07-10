package com.placetopay.checkout;

import com.placetopay.checkout.http.HttpTransport;
import com.placetopay.checkout.model.collect.CollectRequest;
import com.placetopay.checkout.model.common.Status;
import com.placetopay.checkout.model.instrument.InvalidateTokenRequest;
import com.placetopay.checkout.model.reverse.ReverseRequest;
import com.placetopay.checkout.model.reverse.ReverseResponse;
import com.placetopay.checkout.model.session.CancelSessionResponse;
import com.placetopay.checkout.model.session.CreateSessionRequest;
import com.placetopay.checkout.model.session.CreateSessionResponse;
import com.placetopay.checkout.model.session.SessionInformation;
import java.util.Objects;

/**
 * Primary client for the PlaceToPay Checkout API.
 *
 * <p>Construct one instance per set of credentials (typically per-process, shared across threads) —
 * the underlying {@link java.net.http.HttpClient} is thread-safe and pools connections internally.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Checkout checkout = Checkout.builder()
 *     .environment(Environment.SANDBOX)
 *     .country(Country.COLOMBIA)
 *     .login("YOUR_MERCHANT_LOGIN")
 *     .secretKey("YOUR_SECRET_KEY")
 *     .build();
 *
 * CreateSessionResponse response = checkout.createSession(
 *     CreateSessionRequest.builder()
 *         .ipAddress("127.0.0.1")
 *         .userAgent("MyApp/1.0")
 *         .returnUrl("https://example.com/return")
 *         .payment(Payment.builder()
 *             .reference("order-123")
 *             .description("Demo purchase")
 *             .amount(new Amount("COP", 50_000d, null, null))
 *             .build())
 *         .build());
 * }</pre>
 *
 * <p>Every operation method issues a single HTTP call. Retries are governed by the {@link
 * CheckoutConfig.RetrySettings} on the config.
 */
public final class Checkout {

    private final CheckoutConfig config;
    private final HttpTransport transport;

    private Checkout(CheckoutConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.transport = new HttpTransport(config);
    }

    /**
     * @return the builder used to construct a {@link Checkout}.
     */
    public static CheckoutConfig.Builder builder() {
        return CheckoutConfig.builder();
    }

    /** Construct directly from a pre-built config. */
    public static Checkout of(CheckoutConfig config) {
        return new Checkout(config);
    }

    /**
     * @return the effective configuration of this client.
     */
    public CheckoutConfig config() {
        return config;
    }

    /** Advanced: the HTTP transport used by this client. */
    public HttpTransport transport() {
        return transport;
    }

    // ─── Session lifecycle ───────────────────────────────────────────────

    /** {@code POST /api/session} — create a new checkout session. */
    public CreateSessionResponse createSession(CreateSessionRequest request) {
        return createSession(request, null);
    }

    /** {@code POST /api/session} with an idempotency key. */
    public CreateSessionResponse createSession(
            CreateSessionRequest request, String idempotencyKey) {
        Objects.requireNonNull(request, "request");
        return transport.postJson(
                "/api/session", withDefaults(request), CreateSessionResponse.class, idempotencyKey);
    }

    /** {@code POST /api/session/{requestId}} — fetch session state. */
    public SessionInformation querySession(long requestId) {
        return transport.postAuthOnly("/api/session/" + requestId, SessionInformation.class);
    }

    /** {@code POST /api/session/{requestId}/cancel} — cancel a pending session. */
    public CancelSessionResponse cancelSession(long requestId) {
        return transport.postAuthOnly(
                "/api/session/" + requestId + "/cancel", CancelSessionResponse.class);
    }

    // ─── Collect / reverse / invalidate ──────────────────────────────────

    /** {@code POST /api/collect} — direct charge on a stored instrument. */
    public SessionInformation collect(CollectRequest request) {
        return collect(request, null);
    }

    /** {@code POST /api/collect} with an idempotency key. */
    public SessionInformation collect(CollectRequest request, String idempotencyKey) {
        Objects.requireNonNull(request, "request");
        return transport.postJson(
                "/api/collect", request, SessionInformation.class, idempotencyKey);
    }

    /** {@code POST /api/reverse} — reverse by internal reference. */
    public ReverseResponse reverse(String internalReference) {
        return reverse(internalReference, null);
    }

    /** {@code POST /api/reverse} with an idempotency key. */
    public ReverseResponse reverse(String internalReference, String idempotencyKey) {
        Objects.requireNonNull(internalReference, "internalReference");
        ReverseRequest req = ReverseRequest.of(internalReference);
        return transport.postJson("/api/reverse", req, ReverseResponse.class, idempotencyKey);
    }

    /** {@code POST /api/instrument/invalidate} — revoke a stored token. */
    public Status invalidateToken(InvalidateTokenRequest request) {
        Objects.requireNonNull(request, "request");
        // The response envelope is {"status": {...}}; the endpoint returns ErrorResponse-shaped.
        com.placetopay.checkout.model.common.ErrorResponse env =
                transport.postJson(
                        "/api/instrument/invalidate",
                        request,
                        com.placetopay.checkout.model.common.ErrorResponse.class,
                        null);
        return env == null ? null : env.status();
    }

    // ─── Country defaults ────────────────────────────────────────────────

    /** Apply the country default locale to the request when the caller left it null. */
    private CreateSessionRequest withDefaults(CreateSessionRequest req) {
        if (config.country() == null || req.locale() != null) {
            return req;
        }
        String defaultLocale = config.defaultLocale();
        if (defaultLocale == null) {
            return req;
        }
        // Records are immutable; spin up a copy with only locale updated.
        return new CreateSessionRequest(
                req.auth(),
                defaultLocale,
                req.buyer(),
                req.payer(),
                req.payment(),
                req.subscription(),
                req.returnUrl(),
                req.cancelUrl(),
                req.ipAddress(),
                req.userAgent(),
                req.expiration(),
                req.paymentMethod(),
                req.skipResult(),
                req.noBuyerFill(),
                req.captureAddress(),
                req.fields(),
                req.type(),
                req.autopay(),
                req.metadata(),
                req.initiatorIndicator());
    }
}
