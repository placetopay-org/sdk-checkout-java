# PlaceToPay Checkout - SDK Java

[![CI](https://github.com/placetopay/checkout-java/actions/workflows/ci.yml/badge.svg)](https://github.com/placetopay/checkout-java/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.placetopay/checkout-sdk.svg)](https://central.sonatype.com/artifact/com.placetopay/checkout-sdk)
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

SDK oficial de Java 21 para **PlaceToPay Web Checkout**.

Esta libreria te ayuda a integrar pagos con una experiencia directa: crear la
sesion de pago, redirigir al comprador y consultar el estado final de la
transaccion desde tu backend.

Terminología estándar del proyecto: ver `../docs/GLOSARIO.md`.

> Si vienes del SDK legacy de Java, revisa `MIGRATION.md` para mapear metodos y modelos.

---

## Instalación

### Maven

```xml
<dependency>
  <groupId>com.placetopay</groupId>
  <artifactId>checkout-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.placetopay:checkout-sdk:1.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.placetopay:checkout-sdk:1.0.0'
}
```

Requiere **Java 21 LTS** o superior.

## Versiones soportadas

| Versión SDK | Java | Estado | Parches de seguridad hasta |
|-------------|------|--------|----------------------------|
| `1.x` | 21, 25 (LTS) | **Actual** | Alineado al EOL de Java 21 (septiembre 2028) |
| `0.x` | - | Pre-release | End-of-life |

**Política**

- Soportamos el LTS actual y el LTS anterior.
- Cambios breaking solo en major.
- Java 17 o anterior no está soportado.

## Quickstart (5 minutos)

Con este ejemplo puedes iniciar una sesion y obtener el `processUrl` para
enviar al comprador al checkout de PlaceToPay.

```java
import com.placetopay.checkout.Checkout;
import com.placetopay.checkout.Country;
import com.placetopay.checkout.Environment;
import com.placetopay.checkout.model.common.Amount;
import com.placetopay.checkout.model.session.CreateSessionRequest;
import com.placetopay.checkout.model.session.CreateSessionResponse;
import com.placetopay.checkout.model.session.Payment;
import com.placetopay.checkout.model.session.SessionInformation;

Checkout checkout = Checkout.builder()
    .login("YOUR_LOGIN")
    .secretKey("YOUR_SECRET_KEY")
    .environment(Environment.SANDBOX)
    .country(Country.COLOMBIA)
    .build();

CreateSessionResponse response = checkout.createSession(
    CreateSessionRequest.builder()
        .ipAddress("127.0.0.1")
        .userAgent("MyApp/1.0")
        .returnUrl("https://example.com/return")
        .payment(Payment.builder()
            .reference("ORDER-0001")
            .description("Demo purchase")
            .amount(new Amount("COP", 50_000d, null, null))
            .build())
        .build());

System.out.println(response.processUrl());
System.out.println(response.requestId());
```

Después del pago:

```java
SessionInformation info = checkout.querySession(response.requestId());
if (info.status().status() == StatusCode.APPROVED) {
    // completar orden
}
```

## Que puedes hacer con este SDK

| Endpoint | Método | Notas |
| --- | --- | --- |
| `POST /api/session` | `createSession` | Soporta `autopay`, `metadata`, `dispersion` |
| `POST /api/session/{id}` | `querySession` | |
| `POST /api/session/{id}/cancel` | `cancelSession` | Nuevo vs SDK legacy |
| `POST /api/collect` | `collect` | Cobro con instrumento tokenizado |
| `POST /api/reverse` | `reverse` | Por `internalReference` |
| `POST /api/instrument/invalidate` | `invalidateToken` | Revoca token |
| Verificación webhook SHA-256 | `NotificationVerifier.verifySha256` | Recomendado |
| Verificación webhook SHA-1 | `NotificationVerifier.verifySha1Legacy` | Solo compatibilidad legacy |

## Flujo de integracion recomendado

Flujo de sesion (checkout web):

1. `createSession` para iniciar el checkout y obtener `requestId` + `processUrl`.
2. `querySession` para confirmar estado despues del retorno del pagador.
3. `cancelSession` para invalidar una sesion pendiente cuando aplica.

Flujo de tokenizacion y post-pago:

1. `collect` para cobrar con instrumento tokenizado.
2. `reverse` para reversar por `internalReference`.
3. `invalidateToken` para revocar el token.

## Funcionalidades incluidas en v1

- Soporte de `auth.additional` via `CheckoutConfig.Builder.authAdditional(Map)`.
- Soporte de `CollectRequest.provider` para override opcional de procesador.
- Helpers de estado en `Status` y `Transaction` (`isApproved`, `isRejected`, `isError`, `isSuccessful`).
- Helpers de sesion en `SessionInformation` (`lastTransaction`, `lastApprovedTransaction`, `lastAuthorization`).
- Compatibilidad con valores heterogeneos en `processorFields[].value`.

## Casos de uso comunes

- Checkout estandar en web con redireccion (`createSession` + `querySession`).
- Cobro con token guardado (`collect`) para compras recurrentes.
- Reversion de pagos (`reverse`) y revocacion de token (`invalidateToken`).

### Limitaciones por pais

- Ecuador: la funcionalidad `autopay` no esta habilitada en WebCheckout para este pais. Si se envia en `createSession`, puede ser rechazada por el gateway.
- Ecuador: la recurrencia en pago unico no esta disponible actualmente.

### Compatibilidad de respuestas del gateway

- En respuestas de `querySession` / `collect`, `payment[].processorFields[].value` puede llegar como `string`, `number`, `boolean` u objeto JSON.
- El SDK modela ese campo como valor heterogeneo para evitar fallos de parseo cuando el procesador envia metadatos no-string.

## Configuración

`CheckoutConfig.Builder`:

| Método | Default | Uso |
| --- | --- | --- |
| `login(String)` | requerido | Login de comercio |
| `secretKey(String)` | requerido | Secret de comercio |
| `environment(Environment)` | `SANDBOX` | `SANDBOX` o `PRODUCTION` |
| `country(Country)` | `null` | `COLOMBIA`, `ECUADOR`, etc |
| `baseUrl(String)` | - | Override explícito (solo HTTPS) |
| `connectTimeout(Duration)` | 10s | Timeout de conexión |
| `readTimeout(Duration)` | 30s | Timeout de lectura |
| `retry(RetrySettings)` | exponencial | Retry con jitter |
| `userAgentSuffix(String)` | - | Sufijo user-agent |
| `additionalHeaders(Map)` | `{}` | Headers extra |
| `httpClient(HttpClient)` | SDK-owned | Cliente inyectado |

## Manejo de errores

Todas las excepciones heredan de `PlaceToPayException`:

- `AuthenticationException`
- `ValidationException`
- `NotFoundException`
- `RateLimitException`
- `NetworkException`
- `ServerException`

## Verificación de webhook

```java
import com.placetopay.checkout.webhook.NotificationPayload;
import com.placetopay.checkout.webhook.NotificationVerifier;

@PostMapping("/placetopay/webhook")
ResponseEntity<Void> webhook(@RequestBody NotificationPayload payload) {
    if (!NotificationVerifier.verifySha256(payload, secretKey)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    return ResponseEntity.ok().build();
}
```

> Las notificaciones webhook de PlaceToPay están confirmadas sobre SHA-256.

## Pruebas locales con app de ejemplo

El módulo `examples/` es una app Spring Boot para probar los flujos de punta a punta. Ver [`examples/README.md`](examples/README.md).

```bash
export PLACETOPAY_LOGIN=your-login
export PLACETOPAY_SECRET_KEY=your-secret
./gradlew :examples:bootRun
# http://localhost:8080
```

## Seguridad

- Firma: SHA-256.
- Nonce: 16 bytes criptográficos via `SecureRandom`.
- TLS: HTTPS obligatorio.
- Logging: redacción automática de PAN, CVV, `tranKey`, `nonce`, token, PIN y password.
- Disclosure: ver [SECURITY.md](SECURITY.md).

## Instalar desde código fuente

```bash
git clone https://github.com/placetopay/checkout-java.git
cd checkout-java
./gradlew :sdk:build
./gradlew :sdk:publishToMavenLocal
```

## Contribuir

Ver [CONTRIBUTING.md](CONTRIBUTING.md).

## Licencia

MIT, ver [LICENSE](LICENSE).
