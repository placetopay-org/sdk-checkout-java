package com.placetopay.checkout.model.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class DocumentValidatorTest {

    @ParameterizedTest
    @EnumSource(
            value = DocumentType.class,
            names = {"CC", "NIT", "RUC", "CLRUT"})
    void knownTypesAreRecognized(DocumentType type) {
        assertThat(DocumentValidator.isValidType(type)).isTrue();
    }

    @Test
    void legacyEnumAliasesNotInSupportedSet() {
        assertThat(DocumentValidator.SUPPORTED_TYPES)
                .doesNotContain(DocumentType.TE, DocumentType.PP);
    }

    @ParameterizedTest
    @EnumSource(
            value = DocumentType.class,
            names = {"NIT", "RUT", "RUC", "CLRUT"})
    void businessTypes(DocumentType type) {
        assertThat(DocumentValidator.isBusinessDocument(type)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = DocumentType.class,
            names = {"CC", "CE", "TI", "PPN", "SSN", "CPF", "DNI"})
    void nonBusinessTypes(DocumentType type) {
        assertThat(DocumentValidator.isBusinessDocument(type)).isFalse();
    }

    @Test
    void nullIsBusinessReturnsFalse() {
        assertThat(DocumentValidator.isBusinessDocument(null)).isFalse();
    }

    @Test
    void supportedTypesHasNineteenEntries() {
        assertThat(DocumentValidator.SUPPORTED_TYPES).hasSize(19);
    }

    @ParameterizedTest(name = "[{index}] {0} · \"{1}\" → {2}")
    @CsvSource({
        "CC,    1040035000,     true",
        "CC,    987654321,      true",
        "CC,    0123456789,     false",
        "CC,    abc123,         false",
        "CE,    12345,          true",
        "CE,    abcde12345,     true",
        "CE,    012345,         false",
        "TI,    12345,          true",
        "TI,    1234,           false",
        "NIT,   900123456,      true",
        "NIT,   12345,          false",
        "RUT,   900123456,      true",
        "PPN,   AB123,          true",
        "PPN,   passport_001,   true",
        "PPN,   abc,            false",
        "SSN,   123456789,      true",
        "SSN,   12345,          false",
        "CIP,   8-123-456,      true",
        "CIP,   PE-1234-567890, true",
        "CIP,   invalid,        false",
        "CPF,   12345678901,    true",
        "CPF,   123.456.789-01, false",
        "CI,    1234567890,     true",
        "CI,    12345,          false",
        "RUC,   1234567890123,  true",
        "RUC,   123456789,      false",
        "DNI,   12345678,       true",
        "DNI,   1234567,        false",
        "CRCPF, 123456789,      true",
        "CRCPF, 012345678,      false",
        "CPJ,   1234567890,     true",
        "CPJ,   123456789,      false",
        "DIMEX, 12345678901,    true",
        "DIDI,  123456789012,   true",
        "DIMEX, 1234567890,     false",
        "CLRUT, 12.345.678-9,   true",
        "CLRUT, 12.345.678-K,   true",
        "CLRUT, 12345678,       false",
    })
    void patternValidation(DocumentType type, String document, boolean expected) {
        assertThat(DocumentValidator.isValid(type, document)).isEqualTo(expected);
    }

    @Test
    void unknownTypeFailsSafely() {
        assertThat(DocumentValidator.isValid(DocumentType.TE, "anything")).isFalse();
        assertThat(DocumentValidator.isValid(DocumentType.PP, "anything")).isFalse();
    }

    @Test
    void stringOverloadRoundtrips() {
        assertThat(DocumentValidator.isValid("CC", "1040035000")).isTrue();
        assertThat(DocumentValidator.isValid("CC", "abc")).isFalse();
        assertThat(DocumentValidator.isValid("UNKNOWN", "anything")).isFalse();
        assertThat(DocumentValidator.isValid((String) null, "anything")).isFalse();
        assertThat(DocumentValidator.isValid("", "anything")).isFalse();
    }

    @Test
    void nullDocumentFails() {
        assertThat(DocumentValidator.isValid(DocumentType.CC, null)).isFalse();
        assertThat(DocumentValidator.isValid((DocumentType) null, "1040035000")).isFalse();
    }
}
