package com.placetopay.checkout.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Identity document type. See {@link DocumentValidator} for per-country regex validation. See
 * {@code openapi/checkout.yaml#/components/schemas/DocumentType} for the full semantic mapping.
 */
public enum DocumentType {
    // ─── Colombia ────────────────────────────────────────────────────
    /** Colombian citizen ID (Cédula de Ciudadanía). */
    CC,
    /** Foreign ID (Cédula de Extranjería). */
    CE,
    /** Minor ID (Tarjeta de Identidad). */
    TI,
    /** Birth certificate (Registro Civil). */
    RC,
    /** Colombian business tax ID. */
    NIT,
    /** Rol Único Tributario (business). */
    RUT,

    // ─── Generic ─────────────────────────────────────────────────────
    /** Passport. */
    PPN,
    /** Tax ID (generic). */
    TAX,
    /** License. */
    LIC,
    /** Generic identifier (CD). */
    CD,

    // ─── Country-specific ────────────────────────────────────────────
    /** US Social Security Number. */
    SSN,
    /** Panama identity document. */
    CIP,
    /** Brazil CPF. */
    CPF,
    /** Ecuador citizen ID (Cédula de Identidad). */
    CI,
    /** Ecuador business tax ID (Registro Único Contribuyente). */
    RUC,
    /** Peru DNI. */
    DNI,
    /** Costa Rica personal physical ID. */
    CRCPF,
    /** Costa Rica personal juridical ID (business). */
    CPJ,
    /** Costa Rica Migration &amp; Extranjería document. */
    DIMEX,
    /** Costa Rica diplomatic identification. */
    DIDI,
    /** Chile Rol Único Tributario (business). */
    CLRUT,

    // ─── Legacy aliases kept for source compatibility ────────────────
    /** Temporary card. Kept from the legacy enum. */
    TE,
    /** Passport (short form). Kept from the legacy enum. */
    PP;

    @JsonCreator
    public static DocumentType fromString(String raw) {
        if (raw == null) return null;
        for (DocumentType d : values()) {
            if (d.name().equalsIgnoreCase(raw)) return d;
        }
        return null;
    }
}
