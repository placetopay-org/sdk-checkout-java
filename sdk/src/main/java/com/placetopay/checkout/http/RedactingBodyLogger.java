package com.placetopay.checkout.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.placetopay.checkout.internal.JsonSupport;
import java.util.Set;

/**
 * Produces a debug-safe representation of a JSON request/response body by masking known-sensitive
 * fields.
 *
 * <p>Rules from ARCHITECTURE.md §9:
 *
 * <ul>
 *   <li>{@code auth.tranKey}, {@code auth.nonce} → {@code "***"}
 *   <li>{@code card.number}, {@code token.token} → mask all but last 4
 *   <li>{@code cvv}, {@code pin}, {@code password}, {@code credit.code}, {@code secretKey} → {@code
 *       "***"}
 *   <li>By-name redaction applied recursively regardless of nesting location.
 * </ul>
 */
public final class RedactingBodyLogger {

    private static final String MASK = "***";

    /** Field names redacted entirely anywhere they appear. */
    private static final Set<String> REDACT_FULLY =
            Set.of("tranKey", "nonce", "cvv", "pin", "password", "secretKey", "code");

    /** Field names whose value is partially masked (keep last 4 chars). */
    private static final Set<String> REDACT_PARTIAL = Set.of("number", "token");

    private final ObjectMapper mapper;

    public RedactingBodyLogger() {
        this(JsonSupport.MAPPER);
    }

    public RedactingBodyLogger(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Redacts sensitive fields in the given JSON body. If the input isn't valid JSON, returns a
     * fixed marker string so we never log raw (possibly-sensitive) bytes as a fallback.
     */
    public String redact(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        try {
            JsonNode root = mapper.readTree(body);
            redactInPlace(root);
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            return "<non-json body: " + body.length() + " bytes>";
        }
    }

    private void redactInPlace(JsonNode node) {
        if (node == null) return;
        if (node instanceof ObjectNode obj) {
            obj.fieldNames()
                    .forEachRemaining(
                            name -> {
                                JsonNode child = obj.get(name);
                                if (REDACT_FULLY.contains(name)) {
                                    obj.put(name, MASK);
                                } else if (REDACT_PARTIAL.contains(name) && child.isTextual()) {
                                    obj.put(name, maskKeepLast4(child.asText()));
                                } else {
                                    redactInPlace(child);
                                }
                            });
        } else if (node instanceof ArrayNode arr) {
            for (int i = 0; i < arr.size(); i++) {
                redactInPlace(arr.get(i));
            }
        }
    }

    private static String maskKeepLast4(String v) {
        if (v == null) return null;
        if (v.length() <= 4) return MASK;
        return "*".repeat(Math.min(v.length() - 4, 12)) + v.substring(v.length() - 4);
    }
}
