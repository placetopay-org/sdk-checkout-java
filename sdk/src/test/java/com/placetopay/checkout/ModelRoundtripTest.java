package com.placetopay.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.placetopay.checkout.internal.JsonSupport;
import com.placetopay.checkout.model.collect.CollectRequest;
import com.placetopay.checkout.model.common.Address;
import com.placetopay.checkout.model.common.Amount;
import com.placetopay.checkout.model.common.AmountBase;
import com.placetopay.checkout.model.common.AmountConversion;
import com.placetopay.checkout.model.common.AmountDetail;
import com.placetopay.checkout.model.common.Discount;
import com.placetopay.checkout.model.common.DocumentType;
import com.placetopay.checkout.model.common.ErrorResponse;
import com.placetopay.checkout.model.common.NameValuePair;
import com.placetopay.checkout.model.common.Person;
import com.placetopay.checkout.model.common.Status;
import com.placetopay.checkout.model.common.StatusCode;
import com.placetopay.checkout.model.common.TaxDetail;
import com.placetopay.checkout.model.instrument.Bank;
import com.placetopay.checkout.model.instrument.Card;
import com.placetopay.checkout.model.instrument.Credit;
import com.placetopay.checkout.model.instrument.Instrument;
import com.placetopay.checkout.model.instrument.InvalidateTokenRequest;
import com.placetopay.checkout.model.instrument.Token;
import com.placetopay.checkout.model.reverse.ReverseRequest;
import com.placetopay.checkout.model.reverse.ReverseResponse;
import com.placetopay.checkout.model.session.AuthOnlyRequest;
import com.placetopay.checkout.model.session.CancelSessionResponse;
import com.placetopay.checkout.model.session.CreateSessionRequest;
import com.placetopay.checkout.model.session.CreateSessionResponse;
import com.placetopay.checkout.model.session.Payment;
import com.placetopay.checkout.model.session.SessionInformation;
import com.placetopay.checkout.model.session.SubscriptionRequest;
import com.placetopay.checkout.model.transaction.AutopayRequest;
import com.placetopay.checkout.model.transaction.DispersionItem;
import com.placetopay.checkout.model.transaction.Item;
import com.placetopay.checkout.model.transaction.PaymentModifier;
import com.placetopay.checkout.model.transaction.Recurring;
import com.placetopay.checkout.model.transaction.SubscriptionInformation;
import com.placetopay.checkout.model.transaction.Transaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Smoke coverage: every public record/entity survives a JSON roundtrip (serialize → deserialize →
 * re-serialize produces the same bytes). Exercises generated accessors / equals / hashCode that
 * behavioral tests don't reach.
 */
class ModelRoundtripTest {

    private <T> void roundtrip(T value, Class<T> type) {
        try {
            String first = JsonSupport.MAPPER.writeValueAsString(value);
            T back = JsonSupport.MAPPER.readValue(first, type);
            String second = JsonSupport.MAPPER.writeValueAsString(back);
            assertThat(second).isEqualTo(first);
        } catch (Exception e) {
            throw new AssertionError("roundtrip failed for " + type.getSimpleName(), e);
        }
    }

    @Test
    void address() {
        roundtrip(new Address("St 1", "Bogotá", "DC", "110111", "CO", "+57 1"), Address.class);
    }

    @Test
    void nameValuePair() {
        roundtrip(new NameValuePair("k", "v", "both"), NameValuePair.class);
    }

    @Test
    void taxDetail() {
        roundtrip(
                new TaxDetail("valueAddedTax", new BigDecimal("190"), new BigDecimal("1000")),
                TaxDetail.class);
    }

    @Test
    void amountDetail() {
        roundtrip(new AmountDetail("discount", new BigDecimal("10")), AmountDetail.class);
    }

    @Test
    void amountBase() {
        roundtrip(new AmountBase("COP", new BigDecimal("50000")), AmountBase.class);
    }

    @Test
    void amountConversion() {
        roundtrip(
                new AmountConversion(
                        new AmountBase("COP", new BigDecimal("100")),
                        new AmountBase("USD", new BigDecimal("0.03")),
                        new BigDecimal("0.0003")),
                AmountConversion.class);
    }

    @Test
    void discount() {
        roundtrip(new Discount("XMAS25", "percentage", 25.0, 100.0, 25.0), Discount.class);
    }

    @Test
    void person() {
        roundtrip(
                new Person("1040", DocumentType.CC, "T", "B", "Co", "t@e.co", "+57", null),
                Person.class);
    }

    @Test
    void item() {
        roundtrip(
                new Item("SKU-1", "Thing", "cat", "1", new BigDecimal("100"), new BigDecimal("19")),
                Item.class);
    }

    @Test
    void card() {
        roundtrip(new Card("T Buyer", "4111111111111111", "123", "12", "30", 1, "C"), Card.class);
    }

    @Test
    void bank() {
        roundtrip(new Bank("1007", "Bancolombia", 1), Bank.class);
    }

    @Test
    void credit() {
        roundtrip(new Credit("C1", "installment", "G", 3), Credit.class);
    }

    @Test
    void tokenEntity() {
        roundtrip(
                new Token("tok", "sub", "visa", "Visa", "Bank", "1111", null, 1, null),
                Token.class);
    }

    @Test
    void instrument() {
        roundtrip(
                Instrument.ofToken(
                        new Token("tok", null, null, null, null, null, null, null, null)),
                Instrument.class);
    }

    @Test
    void recurring() {
        roundtrip(
                new Recurring(
                        "M",
                        1,
                        java.time.LocalDate.of(2026, 5, 1),
                        12,
                        null,
                        "https://example.com/hook"),
                Recurring.class);
    }

    @Test
    void paymentModifier() {
        roundtrip(
                new PaymentModifier("FEDERAL_GOVERNMENT_ENTITY", "X", Map.of("a", "b")),
                PaymentModifier.class);
    }

    @Test
    void dispersionItem() {
        roundtrip(
                new DispersionItem("A", "T", new AmountBase("COP", new BigDecimal("100"))),
                DispersionItem.class);
    }

    @Test
    void subscriptionInformation() {
        roundtrip(
                new SubscriptionInformation(
                        "token", new Status(StatusCode.APPROVED, null, null, null), null),
                SubscriptionInformation.class);
    }

    @Test
    void subscriptionRequest() {
        roundtrip(new SubscriptionRequest("R", "D", null), SubscriptionRequest.class);
    }

    @Test
    void autopayRequest() {
        roundtrip(
                new AutopayRequest(
                        "M",
                        1,
                        java.time.LocalDate.of(2026, 5, 1),
                        12,
                        null,
                        null,
                        "https://example.com/hook"),
                AutopayRequest.class);
    }

    @Test
    void transactionModel() {
        roundtrip(
                new Transaction(
                        new Status(
                                StatusCode.APPROVED,
                                "00",
                                "ok",
                                OffsetDateTime.parse("2026-04-22T15:30:45+00:00")),
                        "R",
                        1L,
                        "visa",
                        "Visa",
                        "Bank",
                        null,
                        "AUTH",
                        "001",
                        "VISA",
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                Transaction.class);
    }

    @Test
    void payment() {
        roundtrip(
                Payment.of("REF", "desc", Amount.of("COP", new BigDecimal("100"))), Payment.class);
    }

    @Test
    void authOnlyRequest() {
        roundtrip(new AuthOnlyRequest(null), AuthOnlyRequest.class);
    }

    @Test
    void createSessionRequest() {
        CreateSessionRequest req =
                CreateSessionRequest.builder()
                        .ipAddress("127.0.0.1")
                        .userAgent("ua/1.0")
                        .returnUrl("https://example.com/return")
                        .payment(Payment.of("R1", "D", Amount.of("COP", new BigDecimal("100"))))
                        .build();
        roundtrip(req, CreateSessionRequest.class);
    }

    @Test
    void createSessionResponse() {
        roundtrip(
                new CreateSessionResponse(
                        new Status(StatusCode.OK, null, null, null), 99L, "https://example.com/r"),
                CreateSessionResponse.class);
    }

    @Test
    void sessionInformation() {
        roundtrip(
                new SessionInformation(
                        99L,
                        new Status(StatusCode.OK, null, null, null),
                        null,
                        List.of(),
                        null,
                        null),
                SessionInformation.class);
    }

    @Test
    void cancelSessionResponse() {
        roundtrip(
                new CancelSessionResponse(new Status(StatusCode.OK, null, null, null), null),
                CancelSessionResponse.class);
    }

    @Test
    void reverseRequest() {
        roundtrip(ReverseRequest.of("42"), ReverseRequest.class);
    }

    @Test
    void reverseResponse() {
        roundtrip(
                new ReverseResponse(new Status(StatusCode.APPROVED, null, null, null), null),
                ReverseResponse.class);
    }

    @Test
    void invalidateTokenRequest() {
        roundtrip(InvalidateTokenRequest.ofToken("tok"), InvalidateTokenRequest.class);
    }

    @Test
    void collectRequest() {
        CollectRequest req =
                CollectRequest.builder()
                        .payer(
                                new Person(
                                        "1040",
                                        DocumentType.CC,
                                        "T",
                                        "B",
                                        null,
                                        "t@e.co",
                                        null,
                                        null))
                        .payment(Payment.of("R", "D", Amount.of("COP", new BigDecimal("100"))))
                        .instrument(
                                Instrument.ofToken(
                                        new Token(
                                                "tok", null, null, null, null, null, null, null,
                                                null)))
                        .provider("evertec")
                        .build();
        roundtrip(req, CollectRequest.class);
    }

    @Test
    void errorResponse() {
        roundtrip(
                new ErrorResponse(new Status(StatusCode.ERROR, "X", "err", null)),
                ErrorResponse.class);
    }

    @Test
    void withAuthCopies() {
        CreateSessionRequest r1 =
                CreateSessionRequest.builder()
                        .ipAddress("127.0.0.1")
                        .userAgent("u")
                        .returnUrl("https://example.com/return")
                        .payment(Payment.of("R", "D", Amount.of("COP", new BigDecimal("100"))))
                        .build();
        com.placetopay.checkout.auth.Auth auth =
                com.placetopay.checkout.auth.AuthSigner.signNow("L", "S");
        CreateSessionRequest withAuth = r1.withAuth(auth);
        assertThat(withAuth.auth()).isEqualTo(auth);

        ReverseRequest rev = ReverseRequest.of("42").withAuth(auth);
        assertThat(rev.auth()).isEqualTo(auth);

        CollectRequest.Builder cb =
                CollectRequest.builder()
                        .payer(
                                new Person(
                                        "1040",
                                        DocumentType.CC,
                                        "T",
                                        "B",
                                        null,
                                        "t@e.co",
                                        null,
                                        null))
                        .payment(Payment.of("R", "D", Amount.of("COP", new BigDecimal("100"))))
                        .instrument(
                                Instrument.ofToken(
                                        new Token(
                                                "tok", null, null, null, null, null, null, null,
                                                null)));
        assertThat(cb.build().withAuth(auth).auth()).isEqualTo(auth);

        InvalidateTokenRequest inv = InvalidateTokenRequest.ofToken("t").withAuth(auth);
        assertThat(inv.auth()).isEqualTo(auth);
    }
}
