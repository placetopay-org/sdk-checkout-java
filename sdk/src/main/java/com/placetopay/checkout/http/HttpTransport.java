package com.placetopay.checkout.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.placetopay.checkout.CheckoutConfig;
import com.placetopay.checkout.auth.Auth;
import com.placetopay.checkout.auth.AuthSigner;
import com.placetopay.checkout.error.AuthenticationException;
import com.placetopay.checkout.error.NetworkException;
import com.placetopay.checkout.error.NotFoundException;
import com.placetopay.checkout.error.PlaceToPayException;
import com.placetopay.checkout.error.RateLimitException;
import com.placetopay.checkout.error.ServerException;
import com.placetopay.checkout.error.ValidationException;
import com.placetopay.checkout.internal.JsonSupport;
import com.placetopay.checkout.model.common.ErrorResponse;
import com.placetopay.checkout.model.common.Status;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The HTTP engine that drives every request made by {@link com.placetopay.checkout.Checkout}.
 *
 * <p>Responsibilities (ARCHITECTURE.md §4–9):
 *
 * <ul>
 *   <li>One shared {@link HttpClient} per {@link com.placetopay.checkout.Checkout} instance.
 *   <li>Inject a fresh {@link Auth} block into every outbound JSON body.
 *   <li>Retry on network errors, HTTP 5xx and HTTP 429 with exponential backoff + jitter.
 *   <li>Honor the {@code Retry-After} response header when present.
 *   <li>Redact sensitive fields before DEBUG-logging request/response bodies.
 *   <li>Translate HTTP errors into the typed {@link PlaceToPayException} hierarchy.
 * </ul>
 *
 * <p>Thread-safe: the config is immutable and {@link HttpClient} is safe for concurrent use.
 */
public final class HttpTransport {

    private static final Logger LOG = LoggerFactory.getLogger(HttpTransport.class);

    private final CheckoutConfig config;
    private final HttpClient httpClient;
    private final String userAgent;
    private final RedactingBodyLogger redactor = new RedactingBodyLogger();

    public HttpTransport(CheckoutConfig config) {
        this.config = config;
        this.httpClient =
                config.httpClient() != null
                        ? config.httpClient()
                        : HttpClient.newBuilder()
                                .connectTimeout(config.connectTimeout())
                                .followRedirects(HttpClient.Redirect.NEVER)
                                // Honor system proxy settings (HTTP_PROXY / HTTPS_PROXY env vars,
                                // -Dhttps.proxyHost JVM flags, OS-level proxy config). Without
                                // this,
                                // java.net.http.HttpClient ignores the system default — Python's
                                // requests and .NET's HttpClient both honor it, so this restores
                                // cross-SDK parity for callers behind a corporate proxy.
                                .proxy(ProxySelector.getDefault())
                                .build();
        this.userAgent = UserAgent.build(config.userAgentSuffix());
    }

    /** Access the underlying client — useful for advanced scenarios (custom adapters / tests). */
    public HttpClient httpClient() {
        return httpClient;
    }

    /**
     * POST a JSON body (arbitrary object — serialized via Jackson) with a freshly-generated auth
     * block injected at the root. Returns the parsed response as the requested type.
     *
     * @param path request path (e.g. {@code /api/session})
     * @param requestBody the request record; auth (if present) is overwritten. Pass {@code null}
     *     for auth-only requests.
     * @param responseType the expected response type for deserialization
     * @param idempotencyKey optional; sent as {@code X-Idempotency-Key} header when non-null
     * @param <T> response type
     * @return the parsed response
     * @throws PlaceToPayException on any error path (authentication, validation, network,
     *     rate-limit, server, etc.)
     */
    public <T> T postJson(
            String path, Object requestBody, Class<T> responseType, String idempotencyKey) {
        URI uri = URI.create(config.baseUrl() + path);
        Auth auth = AuthSigner.signNow(config.login(), config.secretKey(), config.authAdditional());
        byte[] bodyBytes = buildBody(requestBody, auth);
        logRequest(uri, bodyBytes);

        HttpRequest.Builder reqBuilder =
                HttpRequest.newBuilder(uri)
                        .timeout(config.readTimeout())
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("User-Agent", userAgent)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            reqBuilder.header("X-Idempotency-Key", idempotencyKey);
        }
        for (Map.Entry<String, String> h : config.additionalHeaders().entrySet()) {
            reqBuilder.header(h.getKey(), h.getValue());
        }
        HttpRequest request = reqBuilder.build();

        int maxAttempts = config.retry().maxAttempts();
        Throwable lastCause = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            HttpResponse<byte[]> response;
            try {
                response = httpClient.send(request, BodyHandlers.ofByteArray());
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                lastCause = e;
                LOG.debug(
                        "transport error on attempt {}/{}: {}", attempt, maxAttempts, e.toString());
                if (attempt == maxAttempts) {
                    throw new NetworkException(
                            "Network error after " + attempt + " attempt(s): " + e.getMessage(), e);
                }
                sleep(computeBackoff(attempt, null));
                continue;
            }

            int status = response.statusCode();
            byte[] responseBody = response.body();
            logResponse(status, responseBody);

            if (status >= 200 && status < 300) {
                return deserialize(responseBody, responseType);
            }

            boolean retryable = status >= 500 || status == 429;
            if (!retryable || attempt == maxAttempts) {
                throw buildError(status, responseBody, response);
            }

            Duration retryAfter =
                    config.retry().honorRetryAfter()
                            ? parseRetryAfter(
                                    response.headers().firstValue("Retry-After").orElse(null))
                            : null;
            LOG.debug("retry {}/{} after HTTP {}", attempt, maxAttempts, status);
            sleep(computeBackoff(attempt, retryAfter));
        }

        // Unreachable; keeps the compiler happy.
        throw new NetworkException("Unexpected fallthrough in retry loop", lastCause);
    }

    /** Overload for endpoints whose body is the auth block only (query, cancel). */
    public <T> T postAuthOnly(String path, Class<T> responseType) {
        return postJson(path, null, responseType, null);
    }

    // ─── Body assembly ────────────────────────────────────────────────────

    private byte[] buildBody(Object requestBody, Auth auth) {
        try {
            ObjectNode root;
            if (requestBody == null) {
                root = JsonSupport.MAPPER.createObjectNode();
            } else {
                JsonNode tree = JsonSupport.MAPPER.valueToTree(requestBody);
                if (!(tree instanceof ObjectNode objNode)) {
                    throw new IllegalArgumentException(
                            "Request body must serialize to a JSON object, got: "
                                    + tree.getNodeType());
                }
                root = objNode;
            }
            root.set("auth", JsonSupport.MAPPER.valueToTree(auth));
            return JsonSupport.MAPPER.writeValueAsBytes(root);
        } catch (JsonProcessingException e) {
            throw new PlaceToPayException("Failed to serialize request body", e);
        }
    }

    private <T> T deserialize(byte[] responseBody, Class<T> type) {
        if (responseBody.length == 0) {
            throw new PlaceToPayException("Empty response body");
        }
        try {
            return JsonSupport.MAPPER.readValue(responseBody, type);
        } catch (IOException e) {
            throw new PlaceToPayException("Failed to parse response body", e);
        }
    }

    // ─── Error translation ────────────────────────────────────────────────

    private PlaceToPayException buildError(
            int statusCode, byte[] rawBody, HttpResponse<byte[]> response) {
        String bodyStr =
                rawBody == null ? "" : new String(rawBody, java.nio.charset.StandardCharsets.UTF_8);
        Status status = null;
        String reason = null;
        String message = "HTTP " + statusCode;
        Long requestId = parseRequestIdHeader(response);

        try {
            ErrorResponse err = JsonSupport.MAPPER.readValue(rawBody, ErrorResponse.class);
            if (err != null && err.status() != null) {
                status = err.status();
                reason = status.reason();
                if (status.message() != null && !status.message().isBlank()) {
                    message = status.message();
                }
            }
        } catch (Exception ignored) {
            // Non-JSON / unexpected shape — keep the generic message.
        }

        return switch (statusCode) {
            case 400 -> new ValidationException(message, status, reason, requestId, bodyStr);
            case 401 -> new AuthenticationException(message, status, reason, requestId, bodyStr);
            case 404 -> new NotFoundException(message, status, reason, requestId, bodyStr);
            case 429 ->
                    new RateLimitException(
                            message,
                            status,
                            reason,
                            requestId,
                            bodyStr,
                            parseRetryAfter(
                                    response.headers().firstValue("Retry-After").orElse(null)));
            default ->
                    (statusCode >= 500 && statusCode < 600)
                            ? new ServerException(
                                    statusCode, message, status, reason, requestId, bodyStr)
                            : new PlaceToPayException(message + ": " + bodyStr);
        };
    }

    private static Long parseRequestIdHeader(HttpResponse<byte[]> response) {
        return response.headers()
                .firstValue("X-Request-Id")
                .map(
                        v -> {
                            try {
                                return Long.parseLong(v);
                            } catch (NumberFormatException e) {
                                return null;
                            }
                        })
                .orElse(null);
    }

    // ─── Retry / backoff ──────────────────────────────────────────────────

    /** Exposed package-private for unit tests. */
    Duration computeBackoff(int attempt, Duration retryAfter) {
        if (retryAfter != null) {
            long capMs = config.retry().maxDelay().toMillis();
            long wantedMs = retryAfter.toMillis();
            return Duration.ofMillis(Math.min(Math.max(0, wantedMs), capMs));
        }
        long initialMs = config.retry().initialDelay().toMillis();
        long capMs = config.retry().maxDelay().toMillis();
        long expMs = (long) Math.min(capMs, initialMs * Math.pow(2, attempt - 1));
        double jitter = 0.5 + ThreadLocalRandom.current().nextDouble() / 2.0;
        return Duration.ofMillis((long) (expMs * jitter));
    }

    private static Duration parseRetryAfter(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String v = headerValue.trim();
        try {
            long seconds = Long.parseLong(v);
            return Duration.ofSeconds(Math.max(0, seconds));
        } catch (NumberFormatException ignored) {
            // fall through to HTTP-date
        }
        try {
            ZonedDateTime target = ZonedDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME);
            Duration delta = Duration.between(ZonedDateTime.now(target.getZone()), target);
            return delta.isNegative() ? Duration.ZERO : delta;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void sleep(Duration duration) {
        try {
            long ms = Math.max(0, duration.toMillis());
            if (ms > 0) {
                Thread.sleep(ms);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetworkException("Interrupted during retry backoff", e);
        }
    }

    // ─── Logging ──────────────────────────────────────────────────────────

    private void logRequest(URI uri, byte[] body) {
        if (!LOG.isDebugEnabled()) return;
        LOG.debug(
                "POST {} body={}",
                uri,
                redactor.redact(new String(body, java.nio.charset.StandardCharsets.UTF_8)));
    }

    private void logResponse(int statusCode, byte[] body) {
        if (!LOG.isDebugEnabled()) return;
        LOG.debug(
                "← {} body={}",
                statusCode,
                redactor.redact(new String(body, java.nio.charset.StandardCharsets.UTF_8)));
    }
}
