package com.placetopay.checkout.model.transaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Item(
        String sku, String name, String category, String qty, BigDecimal price, BigDecimal tax) {}
