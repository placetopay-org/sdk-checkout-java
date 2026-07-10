package com.placetopay.checkout.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Address(
        String street,
        String city,
        String state,
        String postalCode,
        /** ISO 3166-1 alpha-2. */
        String country,
        String phone) {}
