package com.placetopay.checkout.model.instrument;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Credit(String code, String type, String groupCode, Integer installment) {}
