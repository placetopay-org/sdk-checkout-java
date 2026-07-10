package com.placetopay.checkout.model.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.model.common.Status;

/** Response body of {@code POST /api/session}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateSessionResponse(
        Status status,
        Long requestId,
        /** Where to redirect the end user to complete payment. */
        String processUrl) {}
