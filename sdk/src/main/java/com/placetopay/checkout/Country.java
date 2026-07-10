package com.placetopay.checkout;

/**
 * Country preset. Supplies default currency, locale and (optionally) an override base URL that
 * takes precedence over the environment default when the country has its own gateway host (e.g.
 * Ecuador).
 *
 * <p>Per-request values on the wire (e.g. {@code amount.currency}) always win over these defaults.
 */
public enum Country {

    /** Colombia. Defaults: COP / es_CO. Uses environment base URL. */
    COLOMBIA("COP", "es_CO", null, null),

    /** Ecuador. Defaults: USD / es_EC. Overrides base URL to checkout.placetopay.ec. */
    ECUADOR(
            "USD",
            "es_EC",
            "https://checkout.placetopay.ec",
            "https://checkout-test.placetopay.ec");

    private final String defaultCurrency;
    private final String defaultLocale;
    private final String productionBaseUrlOverride;
    private final String sandboxBaseUrlOverride;

    Country(
            String defaultCurrency,
            String defaultLocale,
            String productionBaseUrlOverride,
            String sandboxBaseUrlOverride) {
        this.defaultCurrency = defaultCurrency;
        this.defaultLocale = defaultLocale;
        this.productionBaseUrlOverride = productionBaseUrlOverride;
        this.sandboxBaseUrlOverride = sandboxBaseUrlOverride;
    }

    public String defaultCurrency() {
        return defaultCurrency;
    }

    public String defaultLocale() {
        return defaultLocale;
    }

    /**
     * Returns the base URL this country prefers for the given environment, or {@code null} if the
     * country does not override the environment default.
     */
    public String baseUrlOverride(Environment env) {
        return switch (env) {
            case PRODUCTION -> productionBaseUrlOverride;
            case SANDBOX -> sandboxBaseUrlOverride;
        };
    }
}
