package com.placetopay.checkout.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Centralized Jackson configuration used across the SDK.
 *
 * <p>Rules (per ARCHITECTURE.md §13):
 *
 * <ul>
 *   <li>Wire naming: camelCase.
 *   <li>{@code null} optional fields omitted from the wire.
 *   <li>Unknown response fields ignored — forward compatibility.
 *   <li>{@code java.time} types (Instant, OffsetDateTime, LocalDate) used instead of {@code
 *       java.util.Date}.
 * </ul>
 *
 * <p>Internal use only; not part of the public API.
 */
public final class JsonSupport {

    public static final ObjectMapper MAPPER = create();

    private JsonSupport() {}

    private static ObjectMapper create() {
        ObjectMapper m = new ObjectMapper();
        m.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
        m.registerModule(new JavaTimeModule());
        // Omit null — explicit `false` booleans still travel as `false`.
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        m.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        m.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return m;
    }
}
