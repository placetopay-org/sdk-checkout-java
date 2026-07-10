package com.placetopay.checkout;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable configuration for a {@link Checkout} client.
 *
 * <p>Construct via {@link #builder()}. A {@link CheckoutConfig} is safe to share between threads
 * and across client instances.
 *
 * <p>Resolution order for {@link #baseUrl()}:
 *
 * <ol>
 *   <li>explicit {@link Builder#baseUrl(String) baseUrl(...)} wins
 *   <li>else {@link Country#baseUrlOverride(Environment)} if set
 *   <li>else {@link Environment#defaultBaseUrl()}
 * </ol>
 */
public final class CheckoutConfig {

    private final String login;
    private final String secretKey;
    private final Environment environment;
    private final Country country;
    private final String baseUrl;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final RetrySettings retry;
    private final String userAgentSuffix;
    private final Map<String, String> additionalHeaders;
    private final Map<String, String> authAdditional;
    private final HttpClient httpClient;

    private CheckoutConfig(Builder b) {
        this.login = Objects.requireNonNull(b.login, "login is required");
        this.secretKey = Objects.requireNonNull(b.secretKey, "secretKey is required");
        this.environment = b.environment == null ? Environment.SANDBOX : b.environment;
        this.country = b.country;
        this.baseUrl = resolveBaseUrl(b.baseUrl, this.environment, this.country);
        this.connectTimeout = b.connectTimeout == null ? Duration.ofSeconds(10) : b.connectTimeout;
        this.readTimeout = b.readTimeout == null ? Duration.ofSeconds(30) : b.readTimeout;
        this.retry = b.retry == null ? RetrySettings.defaults() : b.retry;
        this.userAgentSuffix = b.userAgentSuffix;
        this.additionalHeaders =
                b.additionalHeaders == null
                        ? Map.of()
                        : Collections.unmodifiableMap(new LinkedHashMap<>(b.additionalHeaders));
        this.authAdditional =
                b.authAdditional == null
                        ? null
                        : Collections.unmodifiableMap(new LinkedHashMap<>(b.authAdditional));
        this.httpClient = b.httpClient;
    }

    private static String resolveBaseUrl(String explicit, Environment env, Country country) {
        String raw = explicit;
        if (raw == null && country != null) {
            raw = country.baseUrlOverride(env);
        }
        if (raw == null) {
            raw = env.defaultBaseUrl();
        }
        URI uri = URI.create(raw);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        boolean https = "https".equalsIgnoreCase(scheme);
        // Allow plain http:// for loopback targets only (local dev / tests).
        // Production / sandbox URLs always resolve to https.
        boolean loopback =
                "http".equalsIgnoreCase(scheme)
                        && host != null
                        && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1"));
        if (!https && !loopback) {
            throw new IllegalArgumentException(
                    "baseUrl must use https:// (http is allowed only for localhost/127.0.0.1); got: "
                            + raw);
        }
        // normalize — strip trailing slashes so path concatenation is predictable
        return raw.replaceAll("/+$", "");
    }

    public String login() {
        return login;
    }

    public String secretKey() {
        return secretKey;
    }

    public Environment environment() {
        return environment;
    }

    public Country country() {
        return country;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration readTimeout() {
        return readTimeout;
    }

    public RetrySettings retry() {
        return retry;
    }

    public String userAgentSuffix() {
        return userAgentSuffix;
    }

    public Map<String, String> additionalHeaders() {
        return additionalHeaders;
    }

    /**
     * Optional auth-block extra metadata — mirrors the PHP SDK's {@code authAdditional}.
     * Transmitted as {@code additional} inside the auth block; NOT part of the signing digest.
     * Returns {@code null} when unset.
     */
    public Map<String, String> authAdditional() {
        return authAdditional;
    }

    public HttpClient httpClient() {
        return httpClient;
    }

    public String defaultCurrency() {
        return country == null ? null : country.defaultCurrency();
    }

    public String defaultLocale() {
        return country == null ? null : country.defaultLocale();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Retry policy settings. Applies to network errors, timeouts, HTTP 5xx and HTTP 429. */
    public record RetrySettings(
            int maxAttempts, Duration initialDelay, Duration maxDelay, boolean honorRetryAfter) {
        public RetrySettings {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be >= 1");
            }
            Objects.requireNonNull(initialDelay, "initialDelay");
            Objects.requireNonNull(maxDelay, "maxDelay");
        }

        public static RetrySettings defaults() {
            return new RetrySettings(3, Duration.ofMillis(200), Duration.ofSeconds(5), true);
        }

        /** Retries disabled — single attempt only. */
        public static RetrySettings disabled() {
            return new RetrySettings(1, Duration.ZERO, Duration.ZERO, false);
        }
    }

    /** Mutable builder for {@link CheckoutConfig}. Not thread-safe; single-use. */
    public static final class Builder {
        private String login;
        private String secretKey;
        private Environment environment;
        private Country country;
        private String baseUrl;
        private Duration connectTimeout;
        private Duration readTimeout;
        private RetrySettings retry;
        private String userAgentSuffix;
        private Map<String, String> additionalHeaders;
        private Map<String, String> authAdditional;
        private HttpClient httpClient;

        public Builder login(String login) {
            this.login = login;
            return this;
        }

        public Builder secretKey(String secretKey) {
            this.secretKey = secretKey;
            return this;
        }

        public Builder environment(Environment env) {
            this.environment = env;
            return this;
        }

        public Builder country(Country country) {
            this.country = country;
            return this;
        }

        /**
         * Escape hatch — overrides {@code environment} + {@code country} resolution. HTTPS only.
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder connectTimeout(Duration d) {
            this.connectTimeout = d;
            return this;
        }

        public Builder readTimeout(Duration d) {
            this.readTimeout = d;
            return this;
        }

        public Builder retry(RetrySettings retry) {
            this.retry = retry;
            return this;
        }

        public Builder userAgentSuffix(String suffix) {
            this.userAgentSuffix = suffix;
            return this;
        }

        public Builder additionalHeaders(Map<String, String> headers) {
            this.additionalHeaders = headers;
            return this;
        }

        /**
         * Optional {@code additional} metadata sent inside the auth block. Mirrors the PHP SDK's
         * {@code authAdditional}. NOT part of the signing digest.
         */
        public Builder authAdditional(Map<String, String> extra) {
            this.authAdditional = extra;
            return this;
        }

        /** Inject a custom {@link HttpClient}. Useful for proxy/tracing scenarios. */
        public Builder httpClient(HttpClient client) {
            this.httpClient = client;
            return this;
        }

        public CheckoutConfig build() {
            return new CheckoutConfig(this);
        }
    }
}
