# Contributing — PlaceToPay Checkout Java SDK

Thanks for taking the time to contribute. This document walks through local
setup, the expected workflow for changes, and the quality bar we enforce.

## Local setup

You need:

- **JDK 21 LTS** (Temurin recommended: https://adoptium.net)
- **Gradle 8.x or 9.x** — or rely on the project's Gradle wrapper once generated

```bash
git clone https://github.com/placetopay/checkout-java.git
cd checkout-java

export JAVA_HOME=$(/usr/libexec/java_home -v 21)    # macOS
./gradlew --version                                  # or: gradle --version
```

## Building and testing

```bash
./gradlew :sdk:build                         # compiles + tests + JaCoCo
./gradlew :sdk:test                          # tests only
./gradlew :sdk:jacocoTestCoverageVerification  # ≥ 85 % gate
./gradlew :examples:bootRun                  # run the Spring Boot demo
```

The coverage gate is enforced via `jacocoTestCoverageVerification` which runs
as part of `./gradlew :sdk:check` (transitively invoked by `build`).

## Running the examples app

```bash
export PLACETOPAY_LOGIN=your-login
export PLACETOPAY_SECRET_KEY=your-secret
./gradlew :examples:bootRun                  # http://localhost:8080
```

## Coding standards

- **Java 21 idioms** — records over classes for pure data, sealed hierarchies,
  pattern matching in `switch`, `java.time` over `Date`.
- **No third-party HTTP client** — we use the JDK's
  `java.net.http.HttpClient`. Don't add OkHttp, Apache HttpClient, RestAssured,
  etc.
- **Jackson 2** for JSON. One `ObjectMapper` instance in
  `internal.JsonSupport.MAPPER`; do not construct new instances ad-hoc.
- **Maven Central only** — no JCenter, no Bintray, no custom repos.
- **Records expose canonical constructors + a `withAuth(Auth)` copy helper**
  for any request record (`CreateSessionRequest`, `CollectRequest`, …). The
  SDK relies on these to inject auth.
- Make internal helpers package-private. The public API is deliberately
  small: `Checkout`, `CheckoutConfig`, `Environment`, `Country`, the
  `model/**` records, the `auth.Auth` record, and the exception hierarchy.

## Logs and PCI/PII

All sensitive fields are redacted by `http.RedactingBodyLogger`. Before
adding a new field that might carry PAN, CVV, token, pin, or password
content, add the property name to the redaction sets. Unit-test in
`sdk/src/test/java/com/placetopay/checkout/http/RedactingBodyLoggerTest.java`.

## The cross-SDK auth test vector

If you touch `AuthSigner.java`, `AuthSignerTest` must continue to produce
identical output to the Python and .NET SDKs for these inputs:

```
login     = "SDK_TEST_LOGIN"
secretKey = "SDK_TEST_SECRET"
rawNonce  = HexFormat.of().parseHex("00112233445566778899aabbccddeeff")
seed      = "2026-04-22T15:30:45+00:00"

→ nonce()    = "ABEiM0RVZneImaq7zN3u/w=="
→ tranKey()  = "ktUB+Efbix9v8H/DxdxtDQu9u33E4BLGr6so+LrvAjs="
```

Any change in that output is a **cross-SDK contract break** and must be
discussed + mirrored in `checkout-python` and `checkout-dotnet` before
merging.

## Pull requests

1. Branch off `main`.
2. Open a PR with a short description of **why** the change is needed.
3. `./gradlew :sdk:build` must pass locally (runs tests + JaCoCo gate).
4. Add tests for new behavior. `ModelRoundtripTest.java` is the right place
   for new public records.
5. Update `CHANGELOG.md` under an `Unreleased` section (Keep a Changelog
   format).
6. Keep the auth test vector green.

## Reporting issues

GitHub issue tracker for bugs and feature requests. For **security issues**,
do NOT open a public issue — email security@placetopay.com per
[SECURITY.md](../SECURITY.md).

## License

By contributing you agree that your changes are licensed under the MIT
license (see [LICENSE](LICENSE)).
