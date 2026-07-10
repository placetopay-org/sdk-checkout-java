package com.placetopay.checkout.examples;

import com.placetopay.checkout.Checkout;
import com.placetopay.checkout.error.PlaceToPayException;
import com.placetopay.checkout.model.collect.CollectRequest;
import com.placetopay.checkout.model.common.Amount;
import com.placetopay.checkout.model.common.DocumentType;
import com.placetopay.checkout.model.common.Person;
import com.placetopay.checkout.model.common.Status;
import com.placetopay.checkout.model.instrument.Instrument;
import com.placetopay.checkout.model.instrument.InvalidateTokenRequest;
import com.placetopay.checkout.model.instrument.Token;
import com.placetopay.checkout.model.reverse.ReverseResponse;
import com.placetopay.checkout.model.session.CancelSessionResponse;
import com.placetopay.checkout.model.session.CreateSessionRequest;
import com.placetopay.checkout.model.session.CreateSessionResponse;
import com.placetopay.checkout.model.session.Payment;
import com.placetopay.checkout.model.session.SessionInformation;
import com.placetopay.checkout.model.session.SubscriptionRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single-page playground controller. Serves the HTML UI at {@code /} and JSON endpoints under
 * {@code /api/*}. Credentials are held only in {@link ClientHolder}, never persisted.
 */
@RestController
public class CheckoutController {

    private final ClientHolder holder;

    public CheckoutController(ClientHolder holder) {
        this.holder = holder;
    }

    // ─── HTML UI ─────────────────────────────────────────────────────

    @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    public org.springframework.http.ResponseEntity<String> index() {
        return org.springframework.http.ResponseEntity.ok()
                .header("Cache-Control", "no-store, must-revalidate")
                .contentType(MediaType.TEXT_HTML)
                .body(INDEX_HTML);
    }

    // ─── Status / connect / disconnect ───────────────────────────────

    @GetMapping(path = "/api/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ClientHolder.Snapshot status() {
        return holder.snapshot();
    }

    public record ConnectBody(
            String login,
            String secret_key,
            String environment,
            String country,
            String custom_base_url) {}

    @PostMapping(
            path = "/api/connect",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> connect(@RequestBody ConnectBody body) {
        try {
            holder.connect(
                    body.login(),
                    body.secret_key(),
                    body.environment(),
                    body.country(),
                    body.custom_base_url());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("detail", e.getMessage()));
        }
        return ResponseEntity.ok(holder.snapshot());
    }

    @PostMapping(path = "/api/disconnect", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Boolean> disconnect() {
        holder.disconnect();
        return Map.of("connected", false);
    }

    // ─── SDK operations ──────────────────────────────────────────────

    @PostMapping(
            path = "/api/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createSession(@RequestBody Map<String, Object> body) {
        Checkout client = holder.require();
        if (client == null) return notConnected();

        String reference =
                stringOr(body, "reference", "demo-" + UUID.randomUUID().toString().substring(0, 8));
        String currency = stringOr(body, "currency", "COP");
        double amount = doubleOr(body, "amount", 50000.0);

        Payment payment =
                Payment.of(
                        reference,
                        stringOr(body, "description", "Demo session"),
                        Amount.of(currency, BigDecimal.valueOf(amount)));

        CreateSessionRequest req =
                CreateSessionRequest.builder()
                        .ipAddress("127.0.0.1")
                        .userAgent("checkout-java-playground/1.0")
                        .returnUrl(stringOr(body, "return_url", "https://example.com/return"))
                        .payment(payment)
                        .build();
        try {
            CreateSessionResponse resp = client.createSession(req);
            return ResponseEntity.ok(resp);
        } catch (PlaceToPayException e) {
            return gatewayError(e);
        }
    }

    @PostMapping(
            path = "/api/create-subscription",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createSubscriptionSession(@RequestBody Map<String, Object> body) {
        Checkout client = holder.require();
        if (client == null) return notConnected();

        String reference =
                stringOr(body, "reference", "sub-" + UUID.randomUUID().toString().substring(0, 8));
        String description = stringOr(body, "description", "Demo subscription session");
        String returnUrl = stringOr(body, "return_url", "https://example.com/return");

        CreateSessionRequest req =
                CreateSessionRequest.builder()
                        .ipAddress("127.0.0.1")
                        .userAgent("checkout-java-playground/1.0")
                        .returnUrl(returnUrl)
                        .subscription(new SubscriptionRequest(reference, description, null))
                        .build();
        try {
            CreateSessionResponse resp = client.createSession(req);
            return ResponseEntity.ok(resp);
        } catch (PlaceToPayException e) {
            return gatewayError(e);
        }
    }

    @PostMapping(
            path = "/api/query",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> querySession(@RequestBody Map<String, Object> body) {
        Checkout client = holder.require();
        if (client == null) return notConnected();
        if (!body.containsKey("request_id")) return missingField("request_id");
        try {
            SessionInformation info = client.querySession(longOr(body, "request_id", 0));
            return ResponseEntity.ok(info);
        } catch (PlaceToPayException e) {
            return gatewayError(e);
        }
    }

    @PostMapping(
            path = "/api/cancel",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancelSession(@RequestBody Map<String, Object> body) {
        Checkout client = holder.require();
        if (client == null) return notConnected();
        if (!body.containsKey("request_id")) return missingField("request_id");
        try {
            CancelSessionResponse resp = client.cancelSession(longOr(body, "request_id", 0));
            return ResponseEntity.ok(resp);
        } catch (PlaceToPayException e) {
            return gatewayError(e);
        }
    }

    @PostMapping(
            path = "/api/collect",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> collect(@RequestBody Map<String, Object> body) {
        Checkout client = holder.require();
        if (client == null) return notConnected();

        String docType = stringOr(body, "document_type", "CC");
        DocumentType dt;
        try {
            dt = DocumentType.valueOf(docType);
        } catch (IllegalArgumentException e) {
            dt = DocumentType.CC;
        }

        String token = stringOr(body, "token", "");
        if (token.isBlank()) return missingField("token");

        CollectRequest req =
                CollectRequest.builder()
                        .payer(
                                new Person(
                                        stringOr(body, "document", "1040035000"),
                                        dt,
                                        stringOr(body, "name", "Test"),
                                        stringOr(body, "surname", "Buyer"),
                                        null,
                                        stringOr(body, "email", "test@example.com"),
                                        null,
                                        null))
                        .payment(
                                Payment.of(
                                        "collect-" + UUID.randomUUID().toString().substring(0, 8),
                                        stringOr(body, "description", "Collect via token"),
                                        Amount.of(
                                                stringOr(body, "currency", "COP"),
                                                BigDecimal.valueOf(
                                                        doubleOr(body, "amount", 10000.0)))))
                        .instrument(
                                Instrument.ofToken(
                                        new Token(
                                                token, null, null, null, null, null, null, null,
                                                null)))
                        .build();
        try {
            SessionInformation info = client.collect(req);
            return ResponseEntity.ok(info);
        } catch (PlaceToPayException e) {
            return gatewayError(e);
        }
    }

    @PostMapping(
            path = "/api/reverse",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reverse(@RequestBody Map<String, Object> body) {
        Checkout client = holder.require();
        if (client == null) return notConnected();
        String internalRef = stringOr(body, "internal_reference", "");
        if (internalRef.isBlank()) return missingField("internal_reference");
        try {
            ReverseResponse resp = client.reverse(internalRef);
            return ResponseEntity.ok(resp);
        } catch (PlaceToPayException e) {
            return gatewayError(e);
        }
    }

    @PostMapping(
            path = "/api/invalidate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> invalidate(@RequestBody Map<String, Object> body) {
        Checkout client = holder.require();
        if (client == null) return notConnected();
        String tokenVal = stringOr(body, "token", "");
        if (tokenVal.isBlank()) return missingField("token");
        try {
            InvalidateTokenRequest req = InvalidateTokenRequest.ofToken(tokenVal);
            Status status = client.invalidateToken(req);
            return ResponseEntity.ok(status);
        } catch (PlaceToPayException e) {
            return gatewayError(e);
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────

    private static ResponseEntity<?> notConnected() {
        return ResponseEntity.badRequest()
                .body(Map.of("detail", "Not connected. Use section 1 first."));
    }

    private static ResponseEntity<?> gatewayError(PlaceToPayException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("detail", e.getMessage());
        body.put("type", e.getClass().getSimpleName());
        if (e.reason() != null) body.put("reason", e.reason());
        if (e.getCause() != null) body.put("cause", e.getCause().getClass().getSimpleName());
        if (e.rawBody() != null && !e.rawBody().isBlank()) {
            String raw = e.rawBody();
            body.put("rawBody", raw.length() > 1500 ? raw.substring(0, 1500) + "..." : raw);
        }
        return ResponseEntity.status(502).body(body);
    }

    private static ResponseEntity<?> missingField(String name) {
        return ResponseEntity.badRequest().body(Map.of("detail", "'" + name + "' is required."));
    }

    private static String stringOr(Map<String, Object> body, String key, String fallback) {
        Object v = body.get(key);
        if (v == null) return fallback;
        String s = v.toString();
        return s.isBlank() ? fallback : s;
    }

    private static double doubleOr(Map<String, Object> body, String key, double fallback) {
        Object v = body.get(key);
        if (v == null) return fallback;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long longOr(Map<String, Object> body, String key, long fallback) {
        Object v = body.get(key);
        if (v == null) return fallback;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ─── Single-page UI (same template as Python / .NET playgrounds) ─

    private static final String INDEX_HTML =
            """
<!doctype html>
<html lang="en"><head><meta charset="utf-8"><title>PlaceToPay — Java playground</title>
<link rel="icon" type="image/png" href="https://placetopay-static-prod-bucket.s3.us-east-2.amazonaws.com/placetopay-com/favicons/apple-icon-57x57.png">
<style>
  :root{
    --p2p-orange: #F36C21;
    --p2p-orange-dark: #C94518;
    --p2p-orange-soft: #FEF2EC;
    --p2p-orange-ring: #FCD9C2;
    --ink: #1F2937;
    --ink-soft: #4B5563;
    --muted: #6B7280;
    --border: #E5E7EB;
    --bg: #FAF7F3;
    --card: #FFFFFF;
    --ok-bg: #DCFCE7; --ok-ink: #166534;
    --err-bg: #FEE2E2; --err-ink: #991B1B;
    --err: #DC2626;
  }
  *{box-sizing:border-box;}
  html,body{margin:0;}
  body{font:14px/1.5 -apple-system,"Segoe UI",Roboto,sans-serif;color:var(--ink);background:var(--bg);min-height:100vh;}
  .layout{display:grid;grid-template-columns:260px 1fr;min-height:100vh;}
  aside{background:var(--card);border-right:1px solid var(--border);padding:1.5rem 1.1rem;position:sticky;top:0;height:100vh;overflow-y:auto;}
  main{padding:2rem 2.5rem;max-width:900px;}
  .brand{display:flex;align-items:center;gap:.6rem;margin-bottom:1.25rem;}
  .brand-mark{width:38px;height:38px;border-radius:999px;box-shadow:0 2px 6px rgba(243,108,33,.35);display:block;object-fit:cover;}
  .brand-text{line-height:1.1;}
  .brand-text .t1{font-weight:700;font-size:14px;color:var(--ink);}
  .brand-text .t2{font-size:11px;color:var(--muted);}
  .status-card{border:1px solid var(--border);border-radius:8px;padding:.7rem .85rem;background:var(--bg);margin-bottom:1.25rem;}
  .status-card.live{background:var(--p2p-orange-soft);border-color:var(--p2p-orange-ring);}
  .status-row{display:flex;align-items:center;gap:.5rem;font-size:12.5px;}
  .dot{width:8px;height:8px;border-radius:999px;background:#9CA3AF;}
  .dot.on{background:#16A34A;box-shadow:0 0 0 3px #DCFCE7;}
  .dot.off{background:#DC2626;box-shadow:0 0 0 3px #FEE2E2;}
  .status-detail{margin-top:.5rem;font-size:11.5px;color:var(--ink-soft);display:grid;gap:.15rem;}
  .status-detail code{background:white;border:1px solid var(--border);padding:1px 5px;border-radius:3px;font-size:11px;}
  .status-card button{margin-top:.5rem;width:100%;}
  .menu-heading{font-size:10.5px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:var(--muted);margin:1rem 0 .35rem;padding:0 .5rem;}
  nav ul{list-style:none;margin:0;padding:0;}
  nav li a{display:flex;align-items:center;gap:.55rem;padding:.5rem .65rem;border-radius:6px;color:var(--ink-soft);text-decoration:none;font-size:13px;cursor:pointer;transition:all .12s;}
  nav li a:hover{background:var(--p2p-orange-soft);color:var(--p2p-orange-dark);}
  nav li a.active{background:var(--p2p-orange);color:white;font-weight:600;}
  nav li a.active .num{background:rgba(255,255,255,.22);color:white;}
  nav li a.disabled{opacity:.45;cursor:not-allowed;}
  nav li a.disabled:hover{background:transparent;color:var(--ink-soft);}
  .num{display:inline-flex;width:22px;height:22px;border-radius:999px;background:var(--border);color:var(--ink-soft);font-size:11px;font-weight:700;align-items:center;justify-content:center;flex-shrink:0;}
  h1{margin:0 0 .25rem;font-size:1.6rem;letter-spacing:-0.01em;}
  .sub{color:var(--muted);font-size:12.5px;margin-bottom:1.5rem;}
  section{background:var(--card);border:1px solid var(--border);border-radius:10px;padding:1.5rem 1.75rem;margin:0;box-shadow:0 1px 2px rgba(0,0,0,0.03);display:none;}
  section.active{display:block;}
  section.connected{background:linear-gradient(180deg,var(--p2p-orange-soft) 0%,var(--card) 60%);border-color:var(--p2p-orange-ring);}
  section h2{margin:0 0 1rem;font-size:1.15rem;color:var(--ink);display:flex;align-items:center;gap:.6rem;}
  section h2 .badge{background:var(--p2p-orange);color:white;font-size:11px;padding:2px 8px;border-radius:999px;font-weight:700;}
  form{display:grid;gap:.75rem;grid-template-columns:1fr 1fr;align-items:end;}
  form .full{grid-column:1/-1;}
  label{display:grid;gap:.25rem;font-size:12px;color:var(--ink-soft);font-weight:500;}
  input,select,textarea{padding:.55rem .7rem;font:inherit;border:1px solid var(--border);border-radius:6px;background:white;color:var(--ink);transition:border-color .12s,box-shadow .12s;}
  input:focus,select:focus,textarea:focus{outline:0;border-color:var(--p2p-orange);box-shadow:0 0 0 3px var(--p2p-orange-ring);}
  textarea{min-height:6rem;font-family:ui-monospace,Menlo,Consolas,monospace;font-size:12px;resize:vertical;}
  button{cursor:pointer;background:var(--p2p-orange);color:white;border:0;font-weight:600;padding:.6rem 1.2rem;border-radius:6px;font:inherit;font-weight:600;transition:background .12s;}
  button:hover{background:var(--p2p-orange-dark);}
  button.ghost{background:transparent;color:var(--p2p-orange-dark);border:1px solid var(--p2p-orange);}
  button.ghost:hover{background:var(--p2p-orange-soft);}
  button.danger{background:var(--err);}
  button.danger:hover{background:#B91C1C;}
  pre{background:#0F172A;color:#E2E8F0;padding:1rem 1.1rem;border-radius:8px;overflow-x:auto;font-size:11.5px;line-height:1.5;white-space:pre-wrap;word-break:break-all;max-height:420px;margin-top:1rem;}
  .hint{font-size:11.5px;color:var(--muted);margin-top:.35rem;}
  .link-cta{display:inline-block;margin-top:.75rem;padding:.55rem .85rem;background:var(--p2p-orange-soft);border:1px solid var(--p2p-orange-ring);border-radius:6px;color:var(--p2p-orange-dark);text-decoration:none;font-weight:600;font-size:13px;}
  .link-cta:hover{background:var(--p2p-orange-ring);}
  .not-connected-banner{padding:.9rem 1.1rem;background:#FEF3C7;color:#78350F;border:1px solid #FCD34D;border-radius:8px;font-size:13px;margin-bottom:1rem;}
</style></head><body>

<div class="layout">

<aside>
  <div class="brand">
    <img class="brand-mark" src="https://placetopay-static-prod-bucket.s3.us-east-2.amazonaws.com/placetopay-com/favicons/apple-icon-57x57.png" alt="PlaceToPay" width="38" height="38">
    <div class="brand-text">
      <div class="t1">PlaceToPay</div>
      <div class="t2">Java SDK playground</div>
    </div>
  </div>

  <div id="status-card" class="status-card">
    <div class="status-row"><span id="status-dot" class="dot off"></span><strong id="status-text">Disconnected</strong></div>
    <div id="status-detail" class="status-detail" style="display:none"></div>
    <button id="btn-disconnect" class="ghost" style="display:none" onclick="disconnect()">Disconnect</button>
  </div>

  <div class="menu-heading">Operations</div>
  <nav><ul id="menu">
    <li><a data-target="sec-connect"    onclick="show(event,'sec-connect')"    class="active"><span class="num">1</span>Connection</a></li>
    <li><a data-target="sec-create"     onclick="show(event,'sec-create')"     class="disabled"><span class="num">2</span>Create session</a></li>
    <li><a data-target="sec-create-subscription" onclick="show(event,'sec-create-subscription')" class="disabled"><span class="num">3</span>Create subscription session</a></li>
    <li><a data-target="sec-query"      onclick="show(event,'sec-query')"      class="disabled"><span class="num">4</span>Query session</a></li>
    <li><a data-target="sec-cancel"     onclick="show(event,'sec-cancel')"     class="disabled"><span class="num">5</span>Cancel session</a></li>
    <li><a data-target="sec-collect"    onclick="show(event,'sec-collect')"    class="disabled"><span class="num">6</span>Collect</a></li>
    <li><a data-target="sec-reverse"    onclick="show(event,'sec-reverse')"    class="disabled"><span class="num">7</span>Reverse</a></li>
    <li><a data-target="sec-invalidate" onclick="show(event,'sec-invalidate')" class="disabled"><span class="num">8</span>Invalidate token</a></li>
  </ul></nav>
</aside>

<main>
  <h1>Checkout SDK Playground</h1>
  <p class="sub">Every PlaceToPay Checkout SDK operation in one place. Credentials live only in this process's memory — a server restart wipes them.</p>

  <div id="connect-banner" class="not-connected-banner">Start by entering your PlaceToPay credentials in section <strong>1 · Connection</strong>. The other operations light up once you're connected.</div>

  <section id="sec-connect" class="active">
    <h2>Connection <span class="badge">1</span></h2>
    <form id="form-connect" onsubmit="return connect(event)">
      <label>Login<input name="login" required autocomplete="off"></label>
      <label>Secret key<input name="secret_key" type="password" required autocomplete="off"></label>
      <label>Environment<select name="environment"><option>SANDBOX</option><option>PRODUCTION</option></select></label>
      <label>Country (optional)<select name="country"><option value="">—</option><option>COLOMBIA</option><option>ECUADOR</option></select></label>
      <div class="full hint">Country applies regional defaults (currency/locale) and can affect production routing.</div>
      <label class="full"><input id="custom-url-enabled" type="checkbox"> Use custom WebCheckout URL</label>
      <label id="custom-url-row" class="full" style="display:none">Custom URL<input id="custom-base-url" name="custom_base_url" type="url" placeholder="https://checkout-test.placetopay.com" autocomplete="off"></label>
      <div class="full"><button>Connect</button><div class="hint">Secret goes to this local server only — never logged, never persisted.</div><div id="connect-target" class="hint"></div></div>
    </form>
    <pre id="out-connect" style="display:none"></pre>
  </section>

  <section id="sec-create">
    <h2>Create session <span class="badge">2</span></h2>
    <form onsubmit="call(event, '/api/create', 'out-create'); return false;">
      <label>Amount<input name="amount" type="number" step="0.01" value="50000" required></label>
      <label>Currency<input name="currency" value="COP" required></label>
      <label class="full">Description<input name="description" value="Demo session"></label>
      <label class="full">Return URL<input name="return_url" value="https://example.com/return"></label>
      <div class="full"><button>Create &amp; get processUrl</button></div>
    </form>
    <pre id="out-create" style="display:none"></pre>
  </section>

  <section id="sec-create-subscription">
    <h2>Create subscription session <span class="badge">3</span></h2>
    <form onsubmit="call(event, '/api/create-subscription', 'out-create-subscription'); return false;">
      <label class="full">Reference <span class="hint">(optional)</span><input name="reference" placeholder="sub-abc123"></label>
      <label class="full">Description<input name="description" value="Demo subscription session"></label>
      <label class="full">Return URL<input name="return_url" value="https://example.com/return"></label>
      <div class="full"><button>Create subscription session &amp; get processUrl</button></div>
    </form>
    <pre id="out-create-subscription" style="display:none"></pre>
  </section>

  <section id="sec-query">
    <h2>Query session <span class="badge">4</span></h2>
    <form onsubmit="call(event, '/api/query', 'out-query'); return false;">
      <label>Request ID<input name="request_id" type="number" required></label>
      <div><button>Query</button></div>
    </form>
    <pre id="out-query" style="display:none"></pre>
  </section>

  <section id="sec-cancel">
    <h2>Cancel session <span class="badge">5</span></h2>
    <form onsubmit="call(event, '/api/cancel', 'out-cancel'); return false;">
      <label>Request ID<input name="request_id" type="number" required></label>
      <div><button class="danger">Cancel</button></div>
    </form>
    <pre id="out-cancel" style="display:none"></pre>
  </section>

  <section id="sec-collect">
    <h2>Collect (direct charge) <span class="badge">6</span></h2>
    <form onsubmit="call(event, '/api/collect', 'out-collect'); return false;">
      <label class="full">Token<input name="token" required placeholder="subscription token"></label>
      <label>Amount<input name="amount" type="number" step="0.01" value="10000"></label>
      <label>Currency<input name="currency" value="COP"></label>
      <label>Payer document<input name="document" value="1040035000"></label>
      <label>Document type<input name="document_type" value="CC"></label>
      <label>Name<input name="name" value="Test"></label>
      <label>Surname<input name="surname" value="Buyer"></label>
      <label>Email<input name="email" type="email" value="test@example.com"></label>
      <div class="full"><button>Collect</button></div>
    </form>
    <pre id="out-collect" style="display:none"></pre>
  </section>

  <section id="sec-reverse">
    <h2>Reverse <span class="badge">7</span></h2>
    <form onsubmit="call(event, '/api/reverse', 'out-reverse'); return false;">
      <label>Internal reference<input name="internal_reference" required></label>
      <div><button class="danger">Reverse</button></div>
    </form>
    <pre id="out-reverse" style="display:none"></pre>
  </section>

  <section id="sec-invalidate">
    <h2>Invalidate token <span class="badge">8</span></h2>
    <form onsubmit="call(event, '/api/invalidate', 'out-invalidate'); return false;">
      <label class="full">Token<input name="token" required></label>
      <div><button class="danger">Invalidate</button></div>
    </form>
    <pre id="out-invalidate" style="display:none"></pre>
  </section>
</main>

</div>

<script>
function endpointForSelection(environment, country){
  const env = (environment || 'SANDBOX').toUpperCase();
  const ctry = (country || '').toUpperCase();
  if (ctry === 'ECUADOR'){
    return env === 'PRODUCTION'
      ? 'https://checkout.placetopay.ec'
      : 'https://checkout-test.placetopay.ec';
  }
  return env === 'PRODUCTION'
    ? 'https://checkout.placetopay.com'
    : 'https://checkout-test.placetopay.com';
}

function syncCustomBaseUrlState(){
  const enabled = document.getElementById('custom-url-enabled');
  const row = document.getElementById('custom-url-row');
  const input = document.getElementById('custom-base-url');
  if(!enabled || !row || !input) return;
  row.style.display = enabled.checked ? 'grid' : 'none';
  input.disabled = !enabled.checked;
  input.required = enabled.checked;
}

function refreshTargetHint(){
  const form = document.getElementById('form-connect');
  const hint = document.getElementById('connect-target');
  if(!form || !hint) return;
  const fd = new FormData(form);
  const env = (fd.get('environment') || 'SANDBOX').toString().toUpperCase();
  const country = (fd.get('country') || '').toString().toUpperCase();
  const customEnabled = document.getElementById('custom-url-enabled')?.checked;
  const customUrl = (document.getElementById('custom-base-url')?.value || '').trim();
  const target = customEnabled && customUrl ? customUrl : endpointForSelection(env, country);
  const connected = document.getElementById('status-text')?.textContent === 'Connected';
  hint.innerHTML = 'WebCheckout target: <code>' + target + '</code>' + (connected ? ' <span class="hint">(applies on reconnect)</span>' : '');
}

function show(ev, id){
  if(ev) ev.preventDefault();
  const link = document.querySelector('a[data-target="'+id+'"]');
  if(link && link.classList.contains('disabled')) return false;
  for(const s of document.querySelectorAll('section')) s.classList.remove('active');
  document.getElementById(id).classList.add('active');
  for(const a of document.querySelectorAll('nav a')) a.classList.remove('active');
  if(link) link.classList.add('active');
  return false;
}

async function refreshStatus(){
  const r = await fetch('/api/status'); const j = await r.json();
  const dot   = document.getElementById('status-dot');
  const text  = document.getElementById('status-text');
  const detail= document.getElementById('status-detail');
  const btn   = document.getElementById('btn-disconnect');
  const card  = document.getElementById('status-card');
  const banner= document.getElementById('connect-banner');
  const secConnect = document.getElementById('sec-connect');
  const menu = document.getElementById('menu');

  if (j.connected){
    dot.className='dot on'; text.textContent='Connected';
    card.classList.add('live');
    detail.style.display='grid';
    detail.innerHTML = '<div>Login: <code>'+j.loginMasked+'</code></div>'
      + '<div>Env: <code>'+j.environment+'</code> &nbsp; Country: <code>'+j.country+'</code></div>'
      + '<div>WebCheckout: <code>'+(j.baseUrl || endpointForSelection((j.environment || 'SANDBOX').toUpperCase(), (j.country || '').toUpperCase()))+'</code></div>';
    // Keep form selections editable while connected; changes are applied on reconnect.
    refreshTargetHint();
    btn.style.display='block';
    banner.style.display='none';
    secConnect.classList.add('connected');
    for(const a of menu.querySelectorAll('a')) a.classList.remove('disabled');
  } else {
    dot.className='dot off'; text.textContent='Disconnected';
    card.classList.remove('live');
    detail.style.display='none'; detail.innerHTML='';
    btn.style.display='none';
    banner.style.display='block';
    secConnect.classList.remove('connected');
    for(const a of menu.querySelectorAll('a'))
      if(a.getAttribute('data-target')!=='sec-connect') a.classList.add('disabled');
    if(!document.getElementById('sec-connect').classList.contains('active')) show(null,'sec-connect');
  }
}
document.querySelector('#form-connect select[name="environment"]')?.addEventListener('change', refreshTargetHint);
document.querySelector('#form-connect select[name="country"]')?.addEventListener('change', refreshTargetHint);
document.getElementById('custom-url-enabled')?.addEventListener('change', () => { syncCustomBaseUrlState(); refreshTargetHint(); });
document.getElementById('custom-base-url')?.addEventListener('input', refreshTargetHint);
syncCustomBaseUrlState();
refreshTargetHint();
refreshStatus();

function fmt(out, r, j){
  out.style.display='block';
  out.textContent = 'HTTP ' + r.status + '\\n\\n' + JSON.stringify(j, null, 2);
}

async function connect(ev){
  ev.preventDefault();
  const f = ev.target; const body = Object.fromEntries(new FormData(f).entries());
  const customEnabled = document.getElementById('custom-url-enabled')?.checked;
  if (!customEnabled) delete body.custom_base_url;
  const out = document.getElementById('out-connect');
  const r = await fetch('/api/connect',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
  const j = await r.json(); fmt(out, r, j);
  if (r.ok){
    const secretInput = f.querySelector('input[name="secret_key"]');
    if (secretInput) secretInput.value = '';
    await refreshStatus();
  }
  return false;
}

async function disconnect(){
  await fetch('/api/disconnect',{method:'POST'}); await refreshStatus();
}

async function call(ev, path, outId){
  ev.preventDefault();
  const form = ev.target;
  const out = document.getElementById(outId);
  const body = Object.fromEntries(new FormData(form).entries());
  const r = await fetch(path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
  let j; try { j = await r.json(); } catch { j = {error:'non-JSON response'}; }
  fmt(out, r, j);
  if ((path==='/api/create' || path==='/api/create-subscription') && j.processUrl){
    const sibling = out.nextElementSibling;
    if (sibling && sibling.classList && sibling.classList.contains('link-cta')) sibling.remove();
    const a = document.createElement('a'); a.href=j.processUrl; a.target='_blank';
    a.className='link-cta';
    a.textContent='→ Open processUrl in a new tab (complete the payment)';
    out.after(a);
  }
  return false;
}
</script>

</body></html>
""";
}
