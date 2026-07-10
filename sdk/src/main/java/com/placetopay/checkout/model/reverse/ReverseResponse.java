package com.placetopay.checkout.model.reverse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.model.common.Status;
import com.placetopay.checkout.model.transaction.Transaction;

/** Response body of {@code POST /api/reverse}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReverseResponse(Status status, Transaction payment) {}
