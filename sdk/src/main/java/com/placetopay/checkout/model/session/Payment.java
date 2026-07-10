package com.placetopay.checkout.model.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.model.common.Amount;
import com.placetopay.checkout.model.common.AmountDetail;
import com.placetopay.checkout.model.common.NameValuePair;
import com.placetopay.checkout.model.common.Person;
import com.placetopay.checkout.model.instrument.Instrument;
import com.placetopay.checkout.model.transaction.DispersionItem;
import com.placetopay.checkout.model.transaction.Item;
import com.placetopay.checkout.model.transaction.PaymentModifier;
import com.placetopay.checkout.model.transaction.Recurring;
import java.util.List;

/** Payment block present on {@code CreateSessionRequest} and {@code CollectRequest}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Payment(
        /** Merchant-unique reference (≤32 chars). */
        String reference,
        /** Human description (≤250 chars). */
        String description,
        Amount amount,
        Boolean allowPartial,
        Person shipping,
        List<Item> items,
        List<NameValuePair> fields,
        Recurring recurring,
        /** Tokenize the instrument after successful payment. */
        Boolean subscribe,
        List<DispersionItem> dispersion,
        List<PaymentModifier> modifiers,
        String agreement,
        String agreementType,
        AmountDetail discount,
        Instrument instrument) {
    /** Shortcut: minimal payment with only reference, description and amount. */
    public static Payment of(String reference, String description, Amount amount) {
        return new Payment(
                reference,
                description,
                amount,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
