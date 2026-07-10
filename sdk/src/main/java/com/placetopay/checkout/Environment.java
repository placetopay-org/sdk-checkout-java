package com.placetopay.checkout;

/**
 * Environment preset. Determines the default base URL when no explicit {@link
 * CheckoutConfig.Builder#baseUrl(String) baseUrl} is supplied.
 *
 * <p>Per ARCHITECTURE.md §12, the country preset layers on top of the environment preset (e.g.
 * {@code PRODUCTION + ECUADOR} resolves to a country-specific base URL).
 */
public enum Environment {

    /** Sandbox / test environment. */
    SANDBOX("https://checkout-test.placetopay.com"),

    /** Production environment (Colombia default). */
    PRODUCTION("https://checkout.placetopay.com");

    private final String defaultBaseUrl;

    Environment(String defaultBaseUrl) {
        this.defaultBaseUrl = defaultBaseUrl;
    }

    /**
     * @return the canonical base URL for this environment, without country overrides.
     */
    public String defaultBaseUrl() {
        return defaultBaseUrl;
    }
}
