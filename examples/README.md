# PlaceToPay Checkout - Ejemplo Java

Aplicacion Spring Boot 3.3+ sobre Java 21 que recorre todos los endpoints del SDK Java de PlaceToPay Checkout contra sandbox.

Harness local de validación. No se pública en Maven Central.

## Ejecutar

```bash
# desde checkout-java/
export PLACETOPAY_LOGIN=your-login
export PLACETOPAY_SECRET_KEY=your-secret

./gradlew :examples:bootRun
# abrir http://localhost:8080
```

También puedes configurar credenciales via `application.yml` o perfiles de Spring.

## Flujos

| Flujo | Endpoint | Metodo SDK |
| --- | --- | --- |
| Crear session (form) | `POST /create` | `checkout.createSession(...)` |
| Retorno despues de pago | `GET /return` | `checkout.querySession(...)` |
| Consultar session | `GET /session/{id}` | `checkout.querySession(...)` |
| Cancelar session | `POST /session/{id}/cancel` | `checkout.cancelSession(...)` |
| Collect | `POST /collect` (JSON) | `checkout.collect(...)` |
| Reverse | `POST /reverse` (JSON) | `checkout.reverse(...)` |
| Invalidate token | `POST /invalidate` (JSON) | `checkout.invalidateToken(...)` |

## Transacciones cubiertas

Este portal cubre los 2 bloques funcionales de Checkout:

- Sesion: `createSession`, `querySession`, `cancelSession`.
- Token/post-pago: `collect`, `reverse`, `invalidateToken`.

## Funcionalidades destacadas del ejemplo

- Firma SHA-256 y nonce criptografico manejados por el SDK.
- Verificacion de estado de sesion al retorno del pagador.
- Prueba local de los 6 endpoints soportados por el contrato OpenAPI.
