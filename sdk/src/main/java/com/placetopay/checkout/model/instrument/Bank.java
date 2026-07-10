package com.placetopay.checkout.model.instrument;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Bank(String code, String name, Integer einterface) {}
