package com.placetopay.checkout.model.transaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.model.common.AmountBase;

/** A portion of the total amount routed to a sub-merchant account. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DispersionItem(String agreement, String agreementType, AmountBase amount) {}
