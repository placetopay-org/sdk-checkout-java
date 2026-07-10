# Changelog

All notable changes to the PlaceToPay Checkout Java SDK are documented here. The
format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- `querySession` / `collect` now accept heterogeneous
  `payment[].processorFields[].value` payloads (string, number, boolean, object)
  to match gateway responses without parse failures.

## [1.0.0] - 2026-04-24

Ground-up rewrite of the legacy `placetopay/java-placetopay` SDK (v1.0.9, 2020).
**Not backwards-compatible** — the package root changes from
`com.placetopay.java_placetopay` to `com.placetopay.checkout`, the auth
algorithm moves from SHA-1 to SHA-256, and the API surface is redesigned.

### Added
- All six PlaceToPay Checkout endpoints:
  `POST /api/session`, `POST /api/session/{id}`, `POST /api/session/{id}/cancel`,
  `POST /api/collect`, `POST /api/reverse`, `POST /api/instrument/invalidate`.
- SHA-256 signing with cryptographic 16-byte nonces sourced from
  the platform's default non-blocking CSPRNG (`new SecureRandom()`,
  i.e. NativePRNG / Windows-PRNG).
- `Environment` preset (sandbox / production) plus `Country` preset
  (Colombia / Ecuador) with first-class locale + currency + baseUrl overrides.
- Typed exception hierarchy (`AuthenticationException`, `ValidationException`,
  `NotFoundException`, `RateLimitException`, `NetworkException`,
  `ServerException`) rooted at `PlaceToPayException`.
- Retry policy with exponential backoff + jitter, honoring the `Retry-After`
  response header on HTTP 429.
- SLF4J-based debug logging with automatic PCI/PII field redaction
  (PAN, CVV, tranKey, nonce, tokens, PIN, password).
- Webhook signature verification in `NotificationVerifier` with both the
  modern SHA-256 path and a deprecated SHA-1 legacy fallback.
- OSGi-friendly `Automatic-Module-Name: com.placetopay.checkout` manifest.
- Reproducible cross-SDK auth test vector (see `openapi/ARCHITECTURE.md` §3).

### Removed
- SOAP transport (present in the legacy SDK) — REST only.
- SHA-1 signing for outgoing requests (only retained in
  `NotificationVerifier.verifySha1Legacy` for inbound webhook compatibility).
- JCenter dependency repository (discontinued in 2021).

### Security
- Cryptographically-secure nonce generation (was: `new BigInteger(130, new SecureRandom())` then double base64 encoding, which the Java reference SDK still does).
- HTTPS-only; plain `http://` base URLs are rejected at configuration time.
- Fields named `tranKey`, `nonce`, `cvv`, `pin`, `password`, `secretKey`,
  `token`, `code`, `number` are automatically redacted in DEBUG-level log output.

[1.0.0]: https://github.com/placetopay/checkout-java/releases/tag/v1.0.0
