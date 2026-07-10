package com.placetopay.checkout.model.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.model.common.NameValuePair;
import java.util.List;

/** Subscription creation block on {@code CreateSessionRequest}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubscriptionRequest(
        String reference, String description, List<NameValuePair> fields) {}
