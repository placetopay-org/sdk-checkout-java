package com.placetopay.checkout.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.placetopay.checkout.Checkout;
import com.placetopay.checkout.CheckoutConfig;
import com.placetopay.checkout.Environment;
import com.placetopay.checkout.model.common.Amount;
import com.placetopay.checkout.model.session.CancelSessionResponse;
import com.placetopay.checkout.model.session.CreateSessionRequest;
import com.placetopay.checkout.model.session.CreateSessionResponse;
import com.placetopay.checkout.model.session.Payment;
import com.placetopay.checkout.model.session.SessionInformation;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end smoke test against the PlaceToPay sandbox. Gated by environment variables — set {@code
 * PLACETOPAY_LOGIN} and {@code PLACETOPAY_SECRET_KEY} to run. Never commit credentials to source
 * control.
 *
 * <p>Exercises the happy path of session lifecycle that the sandbox can validate without a human
 * completing a UI flow: create → query → cancel → query.
 *
 * <p>Tagged {@code integration} so Gradle can exclude it by default. Run locally with:
 *
 * <pre>
 *   PLACETOPAY_LOGIN=... PLACETOPAY_SECRET_KEY=... \
 *     gradle -p checkout-java :sdk:test -PrunIntegration=true
 * </pre>
 */
@Tag("integration")
class SandboxSmokeTest {

    @Test
    @DisplayName("end-to-end: create → query → cancel → query")
    void sessionLifecycleAgainstSandbox() {
        String login = System.getenv("PLACETOPAY_LOGIN");
        String secret = System.getenv("PLACETOPAY_SECRET_KEY");
        assumeTrue(
                login != null && !login.isBlank() && secret != null && !secret.isBlank(),
                "PLACETOPAY_LOGIN / PLACETOPAY_SECRET_KEY not set — skipping live sandbox test.");

        Checkout client =
                Checkout.of(
                        CheckoutConfig.builder()
                                .login(login)
                                .secretKey(secret)
                                .environment(Environment.SANDBOX)
                                .build());

        // 1) create
        CreateSessionRequest req =
                CreateSessionRequest.builder()
                        .ipAddress("127.0.0.1")
                        .userAgent("checkout-java-smoke/1.0")
                        .returnUrl("https://example.com/return")
                        .payment(
                                Payment.of(
                                        "smoke-jv-" + UUID.randomUUID().toString().substring(0, 8),
                                        "Java SDK sandbox smoke test",
                                        Amount.of("COP", new BigDecimal("50000"))))
                        .build();
        CreateSessionResponse created = client.createSession(req);
        assertThat(created.requestId()).isNotNull().isPositive();
        assertThat(created.processUrl()).isNotBlank();
        long requestId = created.requestId();

        // 2) query — PENDING or OK are both fine.
        SessionInformation info = client.querySession(requestId);
        assertThat(info.status()).isNotNull();

        // 3) cancel
        CancelSessionResponse cancel = client.cancelSession(requestId);
        assertThat(cancel.status()).isNotNull();

        // 4) query again — should now be REJECTED.
        SessionInformation afterCancel = client.querySession(requestId);
        assertThat(afterCancel.status()).isNotNull();
        assertThat(afterCancel.status().isRejected())
                .as("expected REJECTED after cancel; got %s", afterCancel.status().status())
                .isTrue();
    }
}
