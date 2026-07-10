package com.placetopay.checkout.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Display hint key/value pair. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NameValuePair(
        String keyword,
        String value,
        /**
         * Where this value should be surfaced: {@code payment | receipt | both | none | approval}.
         */
        String displayOn) {
    public NameValuePair(String keyword, String value) {
        this(keyword, value, null);
    }
}
