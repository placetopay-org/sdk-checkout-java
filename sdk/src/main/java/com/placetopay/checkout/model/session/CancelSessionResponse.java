package com.placetopay.checkout.model.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.model.common.Status;

/** Body of {@code POST /api/session/{requestId}/cancel}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CancelSessionResponse(Status status, SessionInformation session) {}
