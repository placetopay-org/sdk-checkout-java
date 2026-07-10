package com.placetopay.checkout.model.transaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.model.common.NameValuePair;
import com.placetopay.checkout.model.common.Status;
import java.util.List;

/** Information about a subscription/token created as part of a session. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubscriptionInformation(
        /** {@code token} or {@code recurring}. */
        String type, Status status, List<NameValuePair> instrument) {}
