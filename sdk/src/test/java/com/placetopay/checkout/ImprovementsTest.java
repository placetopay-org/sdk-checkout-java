package com.placetopay.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.placetopay.checkout.auth.Auth;
import com.placetopay.checkout.auth.AuthSigner;
import com.placetopay.checkout.internal.JsonSupport;
import com.placetopay.checkout.model.collect.CollectRequest;
import com.placetopay.checkout.model.common.Amount;
import com.placetopay.checkout.model.common.Discount;
import com.placetopay.checkout.model.common.DocumentType;
import com.placetopay.checkout.model.common.Person;
import com.placetopay.checkout.model.common.Status;
import com.placetopay.checkout.model.common.StatusCode;
import com.placetopay.checkout.model.instrument.Instrument;
import com.placetopay.checkout.model.instrument.Token;
import com.placetopay.checkout.model.session.Payment;
import com.placetopay.checkout.model.session.SessionInformation;
import com.placetopay.checkout.model.transaction.Transaction;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the PHP-parity improvements:
 *
 * <ul>
 *   <li>{@code auth.additional} field + config plumbing
 *   <li>{@link CollectRequest#provider()} field
 *   <li>{@link Discount} entity
 *   <li>{@link Status} convenience predicates
 *   <li>{@link SessionInformation} transaction helpers
 *   <li>{@link Transaction} convenience predicates
 * </ul>
 */
class ImprovementsTest {

    private static final byte[] RAW_NONCE =
            HexFormat.of().parseHex("00112233445566778899aabbccddeeff");
    private static final String SEED = "2026-04-22T15:30:45+00:00";

    @Nested
    @DisplayName("auth.additional")
    class AuthAdditional {

        @Test
        void omittedWhenNull() throws Exception {
            Auth auth = AuthSigner.sign("L", RAW_NONCE, SEED, "S");
            String json = JsonSupport.MAPPER.writeValueAsString(auth);
            assertThat(json).doesNotContain("\"additional\"");
        }

        @Test
        void flowsToWire() throws Exception {
            Map<String, String> extra = Map.of("tenant", "acme", "channel", "web");
            Auth auth = AuthSigner.sign("L", RAW_NONCE, SEED, "S", extra);
            String json = JsonSupport.MAPPER.writeValueAsString(auth);
            assertThat(json).contains("\"additional\"", "\"tenant\"", "\"acme\"");
        }

        @Test
        void doesNotChangeDigest() {
            Auth without = AuthSigner.sign("L", RAW_NONCE, SEED, "S");
            Auth with = AuthSigner.sign("L", RAW_NONCE, SEED, "S", Map.of("x", "y"));
            assertThat(with.tranKey()).isEqualTo(without.tranKey());
            assertThat(with.nonce()).isEqualTo(without.nonce());
        }

        @Test
        void configExposesBuilderPath() {
            CheckoutConfig cfg =
                    CheckoutConfig.builder()
                            .login("L")
                            .secretKey("S")
                            .environment(Environment.SANDBOX)
                            .authAdditional(Map.of("mid", "42"))
                            .build();
            assertThat(cfg.authAdditional()).containsEntry("mid", "42");
        }

        @Test
        void nullAdditionalNotPresent() {
            CheckoutConfig cfg =
                    CheckoutConfig.builder()
                            .login("L")
                            .secretKey("S")
                            .environment(Environment.SANDBOX)
                            .build();
            assertThat(cfg.authAdditional()).isNull();
        }
    }

    @Nested
    @DisplayName("CollectRequest.provider")
    class ProviderField {

        @Test
        void serializesWhenSet() throws Exception {
            CollectRequest req =
                    CollectRequest.builder()
                            .payer(
                                    new Person(
                                            "1040035000",
                                            DocumentType.CC,
                                            "Test",
                                            "Buyer",
                                            null,
                                            "t@example.com",
                                            null,
                                            null))
                            .payment(
                                    Payment.of(
                                            "r1",
                                            "d",
                                            Amount.of("COP", new java.math.BigDecimal("10000"))))
                            .instrument(
                                    Instrument.ofToken(
                                            new Token(
                                                    "tok-abc", null, null, null, null, null, null,
                                                    null, null)))
                            .provider("evertec")
                            .build();
            String json = JsonSupport.MAPPER.writeValueAsString(req);
            assertThat(json).contains("\"provider\":\"evertec\"");
        }

        @Test
        void omittedWhenNull() throws Exception {
            CollectRequest req =
                    CollectRequest.builder()
                            .payer(
                                    new Person(
                                            "1040035000",
                                            DocumentType.CC,
                                            "Test",
                                            "Buyer",
                                            null,
                                            "t@example.com",
                                            null,
                                            null))
                            .payment(
                                    Payment.of(
                                            "r2",
                                            "d",
                                            Amount.of("COP", new java.math.BigDecimal("10000"))))
                            .instrument(
                                    Instrument.ofToken(
                                            new Token(
                                                    "tok-abc", null, null, null, null, null, null,
                                                    null, null)))
                            .build();
            String json = JsonSupport.MAPPER.writeValueAsString(req);
            assertThat(json).doesNotContain("\"provider\"");
        }
    }

    @Nested
    @DisplayName("Discount entity")
    class DiscountEntity {

        @Test
        void roundtripsAllFields() throws Exception {
            Discount d = new Discount("XMAS25", "percentage", 25.0, 100.0, 25.0);
            String json = JsonSupport.MAPPER.writeValueAsString(d);
            Discount restored = JsonSupport.MAPPER.readValue(json, Discount.class);
            assertThat(restored).isEqualTo(d);
        }

        @Test
        void transactionParsesNestedDiscount() throws Exception {
            String json =
                    """
                {
                  "status": {"status":"APPROVED","date":"2026-04-22T15:30:45+00:00"},
                  "discount": {"code":"PROMO","amount":5000.0,"base":50000.0}
                }
                """;
            Transaction tx = JsonSupport.MAPPER.readValue(json, Transaction.class);
            assertThat(tx.discount()).isNotNull();
            assertThat(tx.discount().code()).isEqualTo("PROMO");
            assertThat(tx.discount().amount()).isEqualTo(5_000.0);
        }

        @Test
        void transactionAcceptsMixedProcessorFieldValueTypes() throws Exception {
            String json =
                    """
                                {
                                    "status": {"status":"OK","date":"2026-05-22T12:00:00+00:00"},
                                    "processorFields": [
                                        {"keyword":"is3ds","value":true},
                                        {"keyword":"installments","value":1},
                                        {"keyword":"score","value":55002.35},
                                        {"keyword":"installmentInfo","value":{"code":"1","installments":1}}
                                    ]
                                }
                                """;
            Transaction tx = JsonSupport.MAPPER.readValue(json, Transaction.class);
            assertThat(tx.processorFields()).isNotNull();
            assertThat(tx.processorFields()).hasSize(4);
            assertThat(tx.processorFields().get(0).value()).isInstanceOf(Boolean.class);
            assertThat(tx.processorFields().get(1).value()).isInstanceOf(Integer.class);
            assertThat(tx.processorFields().get(2).value()).isInstanceOf(Double.class);
            assertThat(tx.processorFields().get(3).value()).isInstanceOf(Map.class);
        }
    }

    @Nested
    @DisplayName("Status predicates")
    class StatusPredicates {

        @Test
        void approvedOnly() {
            Status s = new Status(StatusCode.APPROVED, null, null, null);
            assertThat(s.isApproved()).isTrue();
            assertThat(s.isRejected()).isFalse();
            assertThat(s.isError()).isFalse();
            assertThat(s.isSuccessful()).isTrue();
        }

        @Test
        void rejectedOnly() {
            Status s = new Status(StatusCode.REJECTED, null, null, null);
            assertThat(s.isRejected()).isTrue();
            assertThat(s.isApproved()).isFalse();
            assertThat(s.isSuccessful()).isFalse();
        }

        @Test
        void errorOnly() {
            Status s = new Status(StatusCode.ERROR, null, null, null);
            assertThat(s.isError()).isTrue();
            assertThat(s.isApproved()).isFalse();
        }

        @Test
        void successfulCoversOkApprovedAndPartial() {
            assertThat(new Status(StatusCode.OK, null, null, null).isSuccessful()).isTrue();
            assertThat(new Status(StatusCode.APPROVED_PARTIAL, null, null, null).isSuccessful())
                    .isTrue();
            // But isApproved is strict:
            assertThat(new Status(StatusCode.APPROVED_PARTIAL, null, null, null).isApproved())
                    .isFalse();
        }

        @Test
        void nullStatusIsNoneOfThem() {
            Status s = new Status(null, null, null, null);
            assertThat(s.isApproved()).isFalse();
            assertThat(s.isRejected()).isFalse();
            assertThat(s.isError()).isFalse();
        }
    }

    @Nested
    @DisplayName("SessionInformation helpers")
    class SessionInformationHelpers {

        private Transaction txn(StatusCode code, String authorization) {
            Status s = new Status(code, null, null, OffsetDateTime.now());
            return new Transaction(
                    s,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    authorization,
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

        private SessionInformation info(List<Transaction> txns) {
            return new SessionInformation(
                    1L, new Status(StatusCode.OK, null, null, null), null, txns, null, null);
        }

        @Test
        void noTransactionsReturnsNullOrEmpty() {
            SessionInformation info = info(null);
            assertThat(info.lastTransaction()).isNull();
            assertThat(info.lastApprovedTransaction()).isNull();
            assertThat(info.lastAuthorization()).isEmpty();
        }

        @Test
        void lastTransactionDefaultReturnsTail() {
            Transaction t1 = txn(StatusCode.REJECTED, null);
            Transaction t2 = txn(StatusCode.APPROVED, "AUTH-123");
            SessionInformation info = info(List.of(t1, t2));
            assertThat(info.lastTransaction()).isSameAs(t2);
        }

        @Test
        void lastApprovedWalksList() {
            Transaction approved = txn(StatusCode.APPROVED, "A");
            Transaction rejected = txn(StatusCode.REJECTED, null);
            SessionInformation info = info(List.of(approved, rejected));
            assertThat(info.lastApprovedTransaction()).isSameAs(approved);
            assertThat(info.lastAuthorization()).isEqualTo("A");
        }

        @Test
        void lastAuthorizationEmptyWhenNoneApproved() {
            SessionInformation info =
                    info(List.of(txn(StatusCode.REJECTED, null), txn(StatusCode.FAILED, null)));
            assertThat(info.lastApprovedTransaction()).isNull();
            assertThat(info.lastAuthorization()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Transaction predicates")
    class TransactionPredicates {

        private Transaction with(StatusCode code) {
            return new Transaction(
                    new Status(code, null, null, null),
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
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        @Test
        void isApproved() {
            assertThat(with(StatusCode.APPROVED).isApproved()).isTrue();
            assertThat(with(StatusCode.REJECTED).isApproved()).isFalse();
            assertThat(with(StatusCode.APPROVED_PARTIAL).isApproved()).isFalse();
        }

        @Test
        void isRejected() {
            assertThat(with(StatusCode.REJECTED).isRejected()).isTrue();
            assertThat(with(StatusCode.APPROVED).isRejected()).isFalse();
        }

        @Test
        void isSuccessfulCoversApprovedAndPartial() {
            assertThat(with(StatusCode.APPROVED).isSuccessful()).isTrue();
            assertThat(with(StatusCode.APPROVED_PARTIAL).isSuccessful()).isTrue();
            assertThat(with(StatusCode.OK).isSuccessful()).isTrue();
            assertThat(with(StatusCode.REJECTED).isSuccessful()).isFalse();
        }

        @Test
        void nullStatusIsFalse() {
            Transaction t =
                    new Transaction(
                            null, null, null, null, null, null, null, null, null, null, null, null,
                            null, null, null, null, null);
            assertThat(t.isApproved()).isFalse();
            assertThat(t.isRejected()).isFalse();
            assertThat(t.isSuccessful()).isFalse();
        }
    }
}
