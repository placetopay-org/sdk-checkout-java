package com.placetopay.checkout.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Parses gateway date strings defensively.
 *
 * <p>Some live responses may ship non-ISO variants in {@code status.date}. Instead of failing the
 * full response parse, this deserializer returns {@code null} when the value cannot be interpreted.
 */
public final class LenientOffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

    @Override
    public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }

        if (token == JsonToken.VALUE_NUMBER_INT) {
            long epoch = p.getLongValue();
            // Heuristic: values with more than 10 digits are treated as epoch millis.
            if (Math.abs(epoch) > 9_999_999_999L) {
                return Instant.ofEpochMilli(epoch).atOffset(ZoneOffset.UTC);
            }
            return Instant.ofEpochSecond(epoch).atOffset(ZoneOffset.UTC);
        }

        String raw = p.getValueAsString();
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim();

        // Canonical ISO first.
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            // try more formats below
        }

        try {
            return Instant.parse(value).atOffset(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // try more formats below
        }

        try {
            return ZonedDateTime.parse(value).toOffsetDateTime();
        } catch (DateTimeParseException ignored) {
            // try more formats below
        }

        try {
            // Accept timestamps without timezone by assuming UTC.
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atOffset(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // fall through
        }

        // Last-resort normalization for offsets like -0500 -> -05:00.
        if (value.matches(".*[+-]\\d{4}$")) {
            String normalized =
                    value.substring(0, value.length() - 5)
                            + value.substring(value.length() - 5, value.length() - 2)
                            + ":"
                            + value.substring(value.length() - 2);
            try {
                return OffsetDateTime.parse(normalized);
            } catch (DateTimeParseException ignored) {
                // fall through
            }
        }

        // Keep response usable even if the date is malformed.
        return null;
    }
}
