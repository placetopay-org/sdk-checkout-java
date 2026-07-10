package com.placetopay.checkout.model.common;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Client-side document-number validation for LatAm identity / tax IDs.
 *
 * <p>Ported from the PHP SDK's {@code Dnetix\Redirection\Helpers\DocumentHelper} with byte-exact
 * regex patterns so decisions are identical across languages.
 *
 * <p>Use this to reject obviously-malformed {@code Person.document()} values before hitting the
 * gateway — the server will still validate authoritatively, but catching bad input locally saves a
 * round-trip and improves UX.
 *
 * <p>Unknown types fail validation. Known types with no registered pattern pass through.
 */
public final class DocumentValidator {

    private DocumentValidator() {}

    /** Document types the validator recognizes. */
    public static final Set<DocumentType> SUPPORTED_TYPES =
            EnumSet.of(
                    DocumentType.CC,
                    DocumentType.CE,
                    DocumentType.TI,
                    DocumentType.NIT,
                    DocumentType.RUT,
                    DocumentType.PPN,
                    DocumentType.TAX,
                    DocumentType.LIC,
                    DocumentType.SSN,
                    DocumentType.CIP,
                    DocumentType.CPF,
                    DocumentType.CI,
                    DocumentType.RUC,
                    DocumentType.DNI,
                    DocumentType.CRCPF,
                    DocumentType.CPJ,
                    DocumentType.DIMEX,
                    DocumentType.DIDI,
                    DocumentType.CLRUT);

    /** Types that identify a business rather than a natural person. */
    public static final Set<DocumentType> BUSINESS_DOCUMENT_TYPES =
            EnumSet.of(DocumentType.NIT, DocumentType.RUT, DocumentType.RUC, DocumentType.CLRUT);

    private static final Map<DocumentType, Pattern> PATTERNS;

    static {
        Map<DocumentType, Pattern> p = new EnumMap<>(DocumentType.class);
        p.put(DocumentType.CC, Pattern.compile("^[1-9][0-9]{3,9}$"));
        p.put(DocumentType.CE, Pattern.compile("^([a-zA-Z]{1,5})?[1-9][0-9]{3,7}$"));
        p.put(DocumentType.TI, Pattern.compile("^[1-9][0-9]{4,11}$"));
        p.put(DocumentType.NIT, Pattern.compile("^[1-9]\\d{6,9}$"));
        p.put(DocumentType.RUT, Pattern.compile("^[1-9]\\d{6,9}$"));
        p.put(DocumentType.PPN, Pattern.compile("^[a-zA-Z0-9_]{4,16}$"));
        p.put(DocumentType.TAX, Pattern.compile("^[a-zA-Z0-9_]{4,16}$"));
        p.put(DocumentType.LIC, Pattern.compile("^[a-zA-Z0-9_]{4,16}$"));
        p.put(DocumentType.SSN, Pattern.compile("^\\d{3}\\d{2,3}\\d{4}$"));
        p.put(
                DocumentType.CIP,
                Pattern.compile(
                        "^(PE|E|N|[23456789](?:AV|PI)?|1[0123]?(?:AV|PI)?)-?(\\d{1,4})-?(\\d{1,6})$"));
        p.put(DocumentType.CPF, Pattern.compile("^\\d{10,11}$"));
        p.put(DocumentType.CI, Pattern.compile("^\\d{10}$"));
        p.put(DocumentType.RUC, Pattern.compile("^\\d{13}$"));
        p.put(DocumentType.DNI, Pattern.compile("^\\d{8}$"));
        p.put(DocumentType.CRCPF, Pattern.compile("^[1-9][0-9]{8}$"));
        p.put(DocumentType.CPJ, Pattern.compile("^[1-9][0-9]{9}$"));
        p.put(DocumentType.DIMEX, Pattern.compile("^[1-9][0-9]{10,11}$"));
        p.put(DocumentType.DIDI, Pattern.compile("^[1-9][0-9]{10,11}$"));
        p.put(DocumentType.CLRUT, Pattern.compile("^(\\d{1,2}(?:\\.?\\d{1,3}){2}-[\\dkK])$"));
        PATTERNS = p;
    }

    /**
     * @return {@code true} when {@code type} is recognized by this validator.
     */
    public static boolean isValidType(DocumentType type) {
        return type != null && SUPPORTED_TYPES.contains(type);
    }

    /**
     * Validate {@code document} against the pattern registered for {@code type}. Returns {@code
     * false} for unknown types; returns {@code true} for known types that have no registered
     * pattern.
     */
    public static boolean isValid(DocumentType type, String document) {
        if (!isValidType(type)) {
            return false;
        }
        if (document == null) {
            return false;
        }
        Pattern pattern = PATTERNS.get(type);
        if (pattern == null) {
            return true;
        }
        return pattern.matcher(document).matches();
    }

    /** Convenience string overload — useful when the caller has the raw wire code. */
    public static boolean isValid(String typeCode, String document) {
        if (typeCode == null || typeCode.isEmpty()) {
            return false;
        }
        DocumentType t;
        try {
            t = DocumentType.valueOf(typeCode);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return isValid(t, document);
    }

    /**
     * @return {@code true} when {@code type} identifies a business (NIT / RUT / RUC / CLRUT).
     */
    public static boolean isBusinessDocument(DocumentType type) {
        return type != null && BUSINESS_DOCUMENT_TYPES.contains(type);
    }
}
