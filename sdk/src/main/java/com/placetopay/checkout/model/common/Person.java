package com.placetopay.checkout.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Person(
        String document,
        DocumentType documentType,
        String name,
        String surname,
        String company,
        String email,
        String mobile,
        Address address) {}
