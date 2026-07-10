package com.placetopay.checkout.model.transaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentModifier(String type, String code, Map<String, String> additional) {}
