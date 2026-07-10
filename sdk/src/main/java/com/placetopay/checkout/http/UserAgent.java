package com.placetopay.checkout.http;

/**
 * Builds the {@code User-Agent} header for outbound requests. Format: {@code
 * placetopay-sdk-java/<version> (java/<java.version>; <os>; [suffix])}.
 */
public final class UserAgent {

    /** SDK version — kept in sync with {@code gradle.properties}. */
    public static final String SDK_VERSION = "1.0.0";

    private UserAgent() {}

    public static String build(String suffix) {
        String javaVersion = System.getProperty("java.version", "unknown");
        String osName = System.getProperty("os.name", "unknown");
        String osVersion = System.getProperty("os.version", "");
        StringBuilder ua =
                new StringBuilder("placetopay-sdk-java/")
                        .append(SDK_VERSION)
                        .append(" (java/")
                        .append(javaVersion)
                        .append("; ")
                        .append(osName);
        if (!osVersion.isEmpty()) {
            ua.append(' ').append(osVersion);
        }
        if (suffix != null && !suffix.isBlank()) {
            ua.append("; ").append(suffix);
        }
        ua.append(')');
        return ua.toString();
    }
}
