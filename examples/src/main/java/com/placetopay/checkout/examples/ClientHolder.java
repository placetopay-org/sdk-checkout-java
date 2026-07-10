package com.placetopay.checkout.examples;

import com.placetopay.checkout.Checkout;
import com.placetopay.checkout.CheckoutConfig;
import com.placetopay.checkout.Country;
import com.placetopay.checkout.Environment;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Process-scoped holder for the live {@link Checkout} client and the credentials the user entered
 * through the UI.
 *
 * <p>Credentials never touch disk. They are cleared when the server restarts or when the user hits
 * the <em>Disconnect</em> button.
 */
@Component
public class ClientHolder {

    private final AtomicReference<State> state = new AtomicReference<>(null);

    public record Snapshot(
            boolean connected,
            String loginMasked,
            String environment,
            String country,
            String baseUrl) {
        public static Snapshot disconnected() {
            return new Snapshot(false, null, null, null, null);
        }
    }

    private record State(
            Checkout checkout,
            String secret,
            String loginMasked,
            String environment,
            String country,
            String baseUrl) {}

    public Snapshot snapshot() {
        State s = state.get();
        if (s == null) return Snapshot.disconnected();
        return new Snapshot(true, s.loginMasked(), s.environment(), s.country(), s.baseUrl());
    }

    public void connect(
            String login,
            String secretKey,
            String environment,
            String country,
            String customBaseUrl) {
        if (login == null || login.isBlank()) throw new IllegalArgumentException("login required");
        if (secretKey == null || secretKey.isBlank())
            throw new IllegalArgumentException("secret_key required");

        Environment env =
                Environment.valueOf(
                        environment == null || environment.isBlank()
                                ? "SANDBOX"
                                : environment.toUpperCase(Locale.ROOT));
        Country ctry =
                (country == null || country.isBlank())
                        ? null
                        : Country.valueOf(country.toUpperCase(Locale.ROOT));

        CheckoutConfig.Builder builder =
                CheckoutConfig.builder()
                        .login(login.trim())
                        .secretKey(secretKey.trim())
                        .environment(env)
                        .country(ctry);

        if (customBaseUrl != null && !customBaseUrl.isBlank()) {
            builder.baseUrl(customBaseUrl.trim());
        }

        CheckoutConfig config = builder.build();
        Checkout client = Checkout.of(config);

        String masked =
                login.length() > 12
                        ? login.substring(0, 6) + "…" + login.substring(login.length() - 4)
                        : "…";

        state.set(
                new State(
                        client,
                        secretKey.trim(),
                        masked,
                        env.name(),
                        ctry != null ? ctry.name() : "—",
                        config.baseUrl()));
    }

    public void disconnect() {
        state.set(null);
    }

    /**
     * @return the live Checkout, or {@code null} if disconnected.
     */
    public Checkout require() {
        State s = state.get();
        return s == null ? null : s.checkout();
    }

    /**
     * @return the live secret, or {@code null} if disconnected.
     */
    public String secret() {
        State s = state.get();
        return s == null ? null : s.secret();
    }

    /** Backwards compat for any legacy helper that wants a simple map shape. */
    public Map<String, String> displayMap() {
        State s = state.get();
        if (s == null) return Map.of();
        return Map.of(
                "login_masked",
                s.loginMasked(),
                "environment",
                s.environment(),
                "country",
                s.country(),
                "base_url",
                s.baseUrl());
    }
}
