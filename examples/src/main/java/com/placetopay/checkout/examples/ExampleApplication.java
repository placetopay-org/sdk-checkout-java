package com.placetopay.checkout.examples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Java SDK playground. Credentials are NOT injected at startup —
 * the UI at <code>/</code> collects them at runtime and hands them to {@link ClientHolder}.
 *
 * <p>Run:
 *
 * <pre>
 *   ./gradlew :examples:bootRun
 *   # then open http://localhost:8080
 * </pre>
 */
@SpringBootApplication
public class ExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}
