# Migration — Java SDK: `placetopay/java-placetopay` → `com.placetopay:checkout-sdk`

The legacy `java-placetopay` SDK (v1.0.9, 2020) targeted Java 7/8, depended
on the discontinued JCenter repository, and used SHA-1 signatures. The new
`checkout-sdk` package targets Java 21 LTS, ships through Maven Central, and
has a redesigned API surface.

---

## TL;DR

Update your dependency block:

**Maven**

```diff
- <dependency>
-   <groupId>com.placetopay</groupId>
-   <artifactId>java-placetopay</artifactId>
-   <version>1.0.9</version>
- </dependency>
+ <dependency>
+   <groupId>com.placetopay</groupId>
+   <artifactId>checkout-sdk</artifactId>
+   <version>1.0.0</version>
+ </dependency>
```

**Gradle (Kotlin DSL)**

```diff
- implementation("com.placetopay:java-placetopay:1.0.9")
+ implementation("com.placetopay:checkout-sdk:1.0.0")
```

Remove any leftover `repositories { jcenter() }` — the old SDK depended on
it; the new one does not. Use `mavenCentral()` only.

---

## 1. Package rename

```diff
- import com.placetopay.java_placetopay.PlaceToPay;
- import com.placetopay.java_placetopay.Entities.Models.RedirectRequest;
- import com.placetopay.java_placetopay.Entities.Amount;
+ import com.placetopay.checkout.Checkout;
+ import com.placetopay.checkout.CheckoutConfig;
+ import com.placetopay.checkout.Environment;
+ import com.placetopay.checkout.Country;
+ import com.placetopay.checkout.model.session.CreateSessionRequest;
+ import com.placetopay.checkout.model.common.Amount;
```

## 2. Construction

**Before**

```java
URL url = new URL("https://checkout.placetopay.com/");
PlaceToPay gateway = new PlaceToPay("login", "trankey", url, PlaceToPay.TP_REST);
```

**After**

```java
Checkout client = Checkout.builder()
    .login("login")
    .secretKey("trankey")                // raw credential, used to compute tranKey on the wire
    .environment(Environment.PRODUCTION)
    .country(Country.COLOMBIA)           // default locale + currency
    .build()
    .toCheckout();                       // see note below
```

> **Note:** `Checkout.builder()` currently returns a `CheckoutConfig.Builder`
> → `CheckoutConfig`. Use `Checkout.of(config)` to wrap the config. Or use
> the fluent helper shown in the README: `Checkout.of(CheckoutConfig
> .builder()….build())`.

The legacy SOAP toggle (`PlaceToPay.TP_SOAP`) is gone. SOAP is not supported
in the new SDK; REST is the only transport.

## 3. Method renames

| Legacy                                         | New                                         |
| ---------------------------------------------- | ------------------------------------------- |
| `gateway.request(RedirectRequest)`             | `checkout.createSession(CreateSessionRequest)` |
| `gateway.query(String)`                        | `checkout.querySession(long)`               |
| `gateway.collect(CollectRequest)`              | `checkout.collect(CollectRequest)`          |
| `gateway.reverse(String)`                      | `checkout.reverse(String)`                  |
| *(not available)*                              | `checkout.cancelSession(long)` **new**      |
| *(not available)*                              | `checkout.invalidateToken(InvalidateTokenRequest)` **new** |

## 4. Immutable records everywhere

Every request/response DTO is now a `record` (Java 16+). Constructors are
canonical; use the `.builder()` static factory for optional fields:

```java
CreateSessionRequest req = CreateSessionRequest.builder()
    .ipAddress("127.0.0.1")
    .userAgent("MyApp/1.0")
    .returnUrl("https://example.com/return")
    .payment(Payment.of(
        "ORDER-1", "Demo",
        Amount.of("COP", new BigDecimal("50000"))))
    .build();
```

The SDK injects `auth` automatically — never set it yourself.

## 5. Signing algorithm

The legacy SDK used **SHA-1** for the `tranKey` digest. The new SDK uses
**SHA-256** everywhere. This is a wire-visible change: your gateway account
must be configured to accept SHA-256 signatures (all production accounts
since 2023 are).

The canonical algorithm (ARCHITECTURE.md §3) is:

```
rawNonce = SecureRandom.getInstanceStrong().nextBytes(new byte[16])
seed     = ISO 8601 UTC with +00:00 offset
digestIn = rawNonce ++ utf8(seed) ++ utf8(secretKey)
tranKey  = base64(SHA-256(digestIn))
wireNonce= base64(rawNonce)
```

## 6. Error handling

The legacy `PlaceToPayException` / `BadPlaceToPayException` pair is replaced
by a typed hierarchy:

```diff
  try {
      checkout.createSession(req);
- } catch (PlaceToPayException e) {
-     log.error(e.getMessage());
- }
+ } catch (ValidationException e) {
+     // HTTP 400 — server-side validation failed
+ } catch (AuthenticationException e) {
+     // HTTP 401 — digest mismatch / bad creds
+ } catch (RateLimitException e) {
+     Duration wait = e.retryAfter();
+     // back off and retry
+ } catch (NetworkException e) {
+     // wraps the underlying IOException as getCause()
+ }
```

All inherit `PlaceToPayException extends RuntimeException`.

## 7. Nonce & notification signature

- Legacy nonce generation used a 130-bit `BigInteger` converted to hex and
  then base64-encoded as ASCII bytes (double-encoding that only worked by
  accident). The new SDK uses 16 raw bytes of cryptographic randomness
  directly.
- Notification verification:
  ```diff
  - new Notification(payload, tranKey).isValidNotification()     // SHA-1 hex
  + com.placetopay.checkout.webhook.NotificationVerifier.verifySha256(
  +     payload, secretKey)                                       // SHA-256 hex
  ```
  Legacy servers still emitting SHA-1 can be handled with
  `verifySha1Legacy(payload, secretKey)` during migration.

## 8. Dependency changes

| Legacy                    | New                                                       |
| ------------------------- | --------------------------------------------------------- |
| `org.json:json:20160810`  | `jackson-databind` + `jackson-datatype-jsr310` (2.18)     |
| Gradle 2.4 / JCenter      | Gradle 8+ / Maven Central                                 |
| SOAP transport            | *(removed)* — REST only                                   |
| `HttpURLConnection`       | `java.net.http.HttpClient` (JDK-native, 11+)              |
| No SLF4J                  | SLF4J 2.0 (the consumer picks the backend)                |

## 9. Security improvements you get for free

- **SHA-256 signing** (legacy SDK used SHA-1).
- **`SecureRandom.getInstanceStrong()`** for nonce (legacy used
  `SecureRandom` without the "strong" specifier, which is still okay but
  less explicit).
- **HTTPS enforcement** — plain `http://` rejected unless it targets
  localhost.
- **PCI/PII log redaction** — `RedactingBodyLogger` scrubs PAN, CVV,
  `tranKey`, `nonce`, tokens, pin, password at SLF4J DEBUG level.

## 10. New features

- `cancelSession(long)` — stop a pending session.
- `invalidateToken(InvalidateTokenRequest)` — revoke a stored token.
- `CheckoutConfig.Builder.authAdditional(Map)` — merchant-specific metadata
  on the auth block.
- `CollectRequest.provider()` — optional processor override.
- `Discount` record modeled explicitly.
- `Status.isApproved() / isRejected() / isError()` convenience methods.
- `SessionInformation.lastTransaction(boolean) /
  lastApprovedTransaction() / lastAuthorization()` helpers.
- `Country.ECUADOR` preset with the right base URL + currency.
- Typed `AutopayRequest` for recurring-with-token flows.
