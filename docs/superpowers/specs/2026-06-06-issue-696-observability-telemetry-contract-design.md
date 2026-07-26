# Issue #696 Observability Telemetry Contract Design

Date: 2026-06-06 Repo: `bluetape4k-projects`
Issue: #696 Parent epic: #690

## Context

`1.11.0`의 observability lane은 Spring Boot 4, Ktor, application event publish/consume 경로가 같은 metric/tag/span/correlation 정책을 공유하도록 만드는 것이 목표다. 현재 repo에는 이미 기반 모듈이 있다.

- `infra/micrometer`: Micrometer timer, Observation, coroutine Observation, Retrofit2 metric helper를 제공한다.
- `infra/opentelemetry`: OpenTelemetry SDK helper, coroutine/Flow span helper, exporter/provider DSL, legacy WebFlux tracing helper를 제공한다.
- `ktor/observability`: Ktor `CallId`, `CallLogging`, 선택적
  `MicrometerMetrics`, application-owned Prometheus scrape route를 설치한다.
- `docs/lessons/2026-05-28-issue-613-ktor-observability.md`: Ktor observability는 registry/exporter/tracing backend를 application-owned policy로 남기고, raw correlation header를 그대로 echo하지 않는다고 결정했다.

이 문서는 구현 API를 추가하지 않는다. 후속 이슈가 새 helper API를 추가할 때 반드시 참조할 cross-framework telemetry contract를 정의한다.

## External Reference Baseline

- Spring Boot 4 observability uses Micrometer Observation for metrics and traces, and recommends Micrometer Observation/Tracing APIs over direct OpenTelemetry API use in Spring applications:
  <https://docs.spring.io/spring-boot/reference/actuator/observability.html>
- Spring Boot Actuator exposes Prometheus through `/actuator/prometheus` when Prometheus registry support is present and the endpoint is exposed:
  <https://docs.spring.io/spring-boot/reference/actuator/metrics.html>
- Micrometer Observation separates low-cardinality and high-cardinality values; only low-cardinality values are meter tags:
  <https://docs.micrometer.io/micrometer/reference/observation/introduction.html>
  and
  <https://docs.micrometer.io/micrometer/reference/observation/components.html>
- OpenTelemetry HTTP semantic conventions define HTTP span names, status rules,
  `http.route`, `url.template`, and `error.type` behavior:
  <https://opentelemetry.io/docs/specs/semconv/http/http-spans/>
- OpenTelemetry messaging semantic conventions define producer/consumer span kinds, operation names/types, destination attributes, and context propagation:
  <https://opentelemetry.io/docs/specs/semconv/messaging/messaging-spans/>
- Ktor CallId/CallLogging/Micrometer/OpenTelemetry plugins are explicit application installs:
  <https://ktor.io/docs/server-call-id.html>,
  <https://ktor.io/docs/server-call-logging.html>,
  <https://ktor.io/docs/server-metrics-micrometer.html>,
  <https://ktor.io/docs/server-opentelemetry.html>
- Ktor's current official OpenTelemetry server documentation uses the
  `opentelemetry-ktor-3.0` instrumentation artifact with an alpha version in its example. #691 must explicitly decide whether to depend on that artifact, wrap it optionally, or provide a narrower in-repo helper.

## Goals

1. Define one naming and cardinality policy for HTTP server, HTTP client, event publish, and event consume telemetry.
2. Make Spring Boot 4 and Ktor helpers consistent without hiding each framework's native integration model.
3. Keep registry, exporter, backend, and global SDK choices application-owned.
4. Prevent sensitive header, raw correlation ID, payload body, and high-cardinality value leakage by default.
5. Give implementation issues concrete acceptance checks and examples.

## Non-Goals

- Do not implement helper APIs in this issue.
- Do not introduce dependencies or module registration changes.
- Do not mandate a specific tracing exporter, metrics backend, collector, or global OpenTelemetry SDK.
- Do not replace Spring Boot Actuator or Ktor plugin conventions.
- Do not record event payload bodies, Authorization/Cookie headers, raw user identifiers, or unbounded URL/path values by default.

## Design Options

### Option A: Micrometer Observation first, OTel semantic mapping

Use Micrometer Observation as the common application instrumentation contract. Map Observation name/key values to OpenTelemetry semantic conventions where a span is created by Micrometer Tracing, Ktor OpenTelemetry instrumentation, or an explicit `bluetape4k-opentelemetry` helper.

Decision: Adopt.

Rationale:

- Spring Boot 4 already centers custom instrumentation on
  `ObservationRegistry`.
- `bluetape4k-micrometer` already has blocking and coroutine Observation helpers.
- Low-cardinality meter tag policy stays clear.
- OTel semantic conventions still guide span names and attributes without forcing direct OTel API use everywhere.

### Option B: Direct OpenTelemetry API everywhere

Require Spring Boot, Ktor, and event helpers to accept `OpenTelemetry` or
`Tracer` directly and build spans using OTel APIs.

Decision: Reject.

Rationale:

- It fights Spring Boot's Micrometer Observation guidance.
- It would force tracing decisions into helper APIs that should remain optional.
- It risks bypassing Spring Boot Actuator/Micrometer registry behavior.

### Option C: Framework-native only, no shared contract

Leave Spring Boot, Ktor, and event modules to use their native defaults without shared bluetape4k names or cardinality rules.

Decision: Reject.

Rationale:

- The same event or HTTP behavior would be queried differently across frameworks.
- Follow-up implementation issues would repeat naming/cardinality decisions.
- Correlation ID and PII safety rules could drift.

## Shared Naming Contract

Observation and metric names are lowercase dot-separated names. Tags and attributes are lowercase dot-separated names unless an upstream semantic convention already defines the key.

| Path                  | Observation / metric name                                                                   | OTel span name                                                                             | Span kind  |
|-----------------------|---------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|------------|
| HTTP server           | `http.server.requests` unless framework default is already equivalent                       | `{METHOD} {http.route}` when route is available, otherwise `{METHOD}`                      | `SERVER`   |
| HTTP client           | `http.client.requests`; Retrofit legacy helper may keep `retrofit2.requests` until migrated | `{METHOD} {url.template}` when template is available, otherwise `{METHOD}`                 | `CLIENT`   |
| Event publish         | `event.publish`                                                                             | `publish {destination}` when low-cardinality destination is available, otherwise `publish` | `PRODUCER` |
| Event consume/process | `event.consume`                                                                             | `consume {destination}` for push handler processing, otherwise `consume`                   | `CONSUMER` |

Compatibility notes:

- Existing `retrofit2.requests` metrics are not renamed in this design issue. A follow-up can add aliases or a documented migration path if HTTP client metrics are unified later.
- Ktor's native `ktor.http.server.requests` metric remains acceptable when using `MicrometerMetrics`. bluetape4k helpers should document how its tags map to this contract instead of renaming Ktor internals.
- Spring Boot's built-in HTTP observation names should be used as-is unless a helper introduces an explicit custom Observation for non-web behavior.

## Cardinality Contract

### Low-Cardinality Values

These values may be Micrometer low-cardinality key values, meter tags, and OTel attributes when available.

| Key                                    | Applies to         | Examples                            | Notes                                                       |
|----------------------------------------|--------------------|-------------------------------------|-------------------------------------------------------------|
| `http.request.method` / `method`       | HTTP server/client | `GET`, `POST`                       | Use the framework's existing key when it already emits one. |
| `http.route` / `route`                 | HTTP server        | `/orders/{id}`                      | Never use raw path as a meter tag.                          |
| `url.template` / `uri`                 | HTTP client        | `/users/{id}`                       | Use only templated values.                                  |
| `http.response.status_code` / `status` | HTTP               | `200`, `503`                        | Numeric status is bounded enough for metrics.               |
| `outcome`                              | HTTP               | `SUCCESS`, `CLIENT_ERROR`           | Keep aligned with existing Retrofit `Outcome`.              |
| `exception` / `error.type`             | HTTP/events        | simple class name or `_OTHER`       | No exception message by default.                            |
| `event.operation`                      | events             | `publish`, `consume`                | bluetape4k-level alias for implementation docs.             |
| `messaging.operation.name`             | events             | `publish`, `consume`                | OTel semantic key when a messaging system is known.         |
| `messaging.operation.type`             | events             | `send`, `process`                   | Use OTel values when broker semantics are present.          |
| `messaging.system`                     | events             | `kafka`, `nats`, `pulsar`, `spring` | Required only when known without guessing.                  |
| `messaging.destination.name`           | events             | `orders`, `invoice-events`          | Only when bounded and not temporary/anonymous.              |
| `correlation.present`                  | HTTP/events        | `true`, `false`                     | Prefer presence over the correlation value in metrics.      |

### High-Cardinality Values

These values may be high-cardinality Observation values or OTel attributes only when the caller opts in and the value is sanitized.

- Sanitized correlation ID.
- Event message ID.
- Event conversation ID.
- Retry attempt ID or operation ID.
- Remote peer address when it is operationally required.
- Raw URL path only as a high-cardinality trace attribute when a route/template is unavailable and the caller accepts the risk.

### Forbidden Defaults

These values must not be recorded by default as tags, attributes, span events, logs, or Prometheus labels.

- `Authorization`, `Cookie`, `Set-Cookie`, API keys, session tokens, bearer tokens, secrets, passwords.
- Raw request body, response body, event payload body.
- Raw query string.
- Raw caller-supplied correlation header before sanitization.
- Email, phone number, full username, tenant/user/customer ID as a meter tag.
- Exception messages when they may contain user input or secret values.
- Temporary or anonymous messaging destination names unless explicitly redacted or bucketed.

## Correlation ID Contract

1. The canonical inbound and outbound HTTP header remains `X-Request-Id` unless the application configures another header.
2. Ktor helpers must continue to sanitize inbound IDs through
   `KtorCorrelationId.sanitize()` before MDC or response propagation.
3. Spring Boot helpers must use equivalent rules: trim, allow only
   `[A-Za-z0-9_.-]`, cap the value, reject blank results, generate a safe ID when absent.
4. Metrics record only `correlation.present`.
5. Traces may record sanitized `correlation.id` only as high-cardinality data and only when the helper or application explicitly enables it.
6. Logs may include the sanitized ID in MDC. They must not include raw query strings or raw header values.
7. Event metadata may carry the sanitized correlation ID when the event transport supports metadata/headers. If not supported, do not place the ID inside payload bodies.
8. OTel trace context (`traceparent`/baggage or messaging-specific trace context) and business correlation ID are separate concepts. Propagate both only when supported, and do not treat the business correlation ID as the trace ID.

## Status and Error Contract

HTTP:

- Server spans leave status unset for 1xx, 2xx, 3xx, and ordinary 4xx outcomes unless framework/application context marks the request as failed.
- Client spans set error for 4xx when the client treats it as failed, and for 5xx/unknown response codes or transport failures.
- Transport failures set `error.type` to a bounded value such as exception simple class name or `_OTHER`.
- Intentional coroutine cancellation keeps span status unset and rethrows
  `CancellationException`.

Events:

- Publish/consume success records a normal completion.
- Handler exception records error and rethrows/propagates according to the caller's event framework contract.
- Cancellation is not converted into an error metric or ERROR span status.
- Batch publish/consume records `messaging.batch.message_count` when known and bounded. Per-message high-cardinality IDs require opt-in.

## Framework Integration Contract

### Spring Boot 4

- Prefer `ObservationRegistry` and Micrometer Observation helpers for custom instrumentation.
- Do not create a custom Prometheus endpoint by default. Document Actuator
  `/actuator/prometheus` and endpoint exposure/security requirements.
- Do not install or mutate a global OpenTelemetry SDK by default.
- If Micrometer Tracing/OTLP is used, rely on Spring Boot configuration and beans instead of hidden helper-owned exporters.
- Spring helper APIs should accept `ObservationRegistry` or use a bean already present in the application context.

### Ktor

- Keep installation explicit through Ktor plugin-style helpers.
- `installBluetape4kKtorObservability()` remains the baseline for CallId, CallLogging, optional MicrometerMetrics, and application-owned registry.
- Prometheus scrape route remains application-owned and route-based.
- OTel tracing support in #691 should accept application-owned
  `OpenTelemetry`/`Tracer` configuration or wrap Ktor's `KtorServerTelemetry`
  without owning exporters.
- If #691 uses `KtorServerTelemetry`, it must document the current alpha instrumentation dependency risk and keep the dependency opt-in.
- Install tracing before logging/telemetry-related plugins when using KtorServerTelemetry, matching Ktor guidance.

### Application Events and Messaging

- Generic Spring application events use `event.publish` and `event.consume`
  with `event.type` as low-cardinality only when the type is a bounded simple name or explicit logical name.
- Broker-backed modules should map to OTel messaging semantic keys when the broker system, destination, operation, and context propagation are known.
- Producer helpers should inject trace context into event metadata/headers when available; consumer helpers should extract it and prefer span links for batch or fan-out scenarios.
- Do not rewrite Kafka/NATS/Pulsar modules wholesale in #695. Add narrow wrappers and examples first.

## Metric and Span Examples

### HTTP Server

Observation:

```text
name = http.server.requests
low = method=GET, route=/orders/{id}, status=200, outcome=SUCCESS, correlation.present=true
high = correlation.id=<sanitized opt-in value>
```

Span:

```text
name = GET /orders/{id}
kind = SERVER
attributes = http.request.method=GET, http.route=/orders/{id}, http.response.status_code=200
```

### HTTP Client

Observation:

```text
name = http.client.requests
low = method=POST, uri=/payments/{id}, status=503, outcome=SERVER_ERROR
high = retry.operation.id=<opt-in value>
```

Span:

```text
name = POST /payments/{id}
kind = CLIENT
status = ERROR for 5xx or transport failure
attributes = http.request.method=POST, url.template=/payments/{id}, error.type=HttpServerError
```

### Event Publish

Observation:

```text
name = event.publish
low = event.operation=publish, messaging.system=kafka, messaging.destination.name=orders
high = messaging.message.id=<opt-in value>, correlation.id=<sanitized opt-in value>
```

Span:

```text
name = publish orders
kind = PRODUCER
attributes = messaging.operation.name=publish, messaging.operation.type=send, messaging.system=kafka
```

### Event Consume

Observation:

```text
name = event.consume
low = event.operation=consume, messaging.system=kafka, messaging.destination.name=orders, outcome=SUCCESS
high = messaging.message.id=<opt-in value>
```

Span:

```text
name = consume orders
kind = CONSUMER
attributes = messaging.operation.name=consume, messaging.operation.type=process, messaging.system=kafka
```

## Risks and Mitigations

| Risk                          | Impact                                               | Mitigation                                                                                                                                |
|-------------------------------|------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| Metric cardinality explosion  | Prometheus/metrics backend instability               | Default to low-cardinality route/template/status/outcome; high-cardinality values opt-in only.                                            |
| Raw correlation ID leakage    | Header injection, PII or secret disclosure           | Sanitize before MDC/response/event metadata; never record raw header.                                                                     |
| Framework default drift       | Spring/Ktor emit different names/tags                | Preserve framework defaults but document mapping and helper-specific additions.                                                           |
| Hidden exporter ownership     | Applications cannot control telemetry backend        | Helpers accept registries/tracers/ObservationRegistry; no global SDK/exporter mutation by default.                                        |
| Duplicate Spring observations | Double metrics/traces                                | Document that helpers should avoid annotating already-instrumented controllers/repositories unless automatic instrumentation is disabled. |
| Messaging trace over-modeling | Incorrect parent-child traces in fan-out/batch cases | Prefer OTel messaging links for batch/fan-out; direct parent only in documented single-message scenarios.                                 |

## Acceptance Criteria for Follow-Up Issues

### #691 Ktor OTel tracing

- Accept application-owned OpenTelemetry/Tracer configuration.
- Decide explicitly whether the alpha-versioned Ktor OpenTelemetry instrumentation artifact is adopted, wrapped optionally, or avoided.
- Preserve existing CallId/CallLogging/Micrometer behavior.
- Use sanitized correlation ID only as opt-in high-cardinality trace data.
- Cover normal, error, cancellation, and disabled tracing cases.

### #694 Spring Boot 4 observability helpers

- Use `ObservationRegistry` as the primary helper boundary.
- Document Actuator `/actuator/prometheus` instead of adding a custom endpoint.
- Avoid hidden exporter/global SDK mutation.
- Cover observation lifecycle, exception handling, and coroutine cleanup.

### #695 Event telemetry helpers

- Provide publish and consume wrappers with success/failure/cancellation behavior.
- Map broker-backed paths to OTel messaging semantic keys when known.
- Propagate sanitized correlation ID through metadata only, not payload body.
- Support batch count and span links where applicable.

### #692 Examples

- Show Spring Boot Actuator Prometheus separately from Ktor route-based Prometheus.
- Show OTLP/tracing setup as application-owned configuration.
- Include verification steps for metrics scrape and trace/log output.
- Keep `README.md` and `README.ko.md` synchronized where example docs exist.

## Verification Plan for This Design Issue

- `git diff --check`
- Targeted search for the spec path and issue references.
- Local Step 2-R review using developer, security, Ops/SRE, user/caller, and 7-tier perspectives.
- Gate passes only when integrated review reports `P0 = 0` and `P1 = 0`.

## Step 2 Checklist Completion Report

| Item                                                           | Status | Notes                                                                                       |
|----------------------------------------------------------------|--------|---------------------------------------------------------------------------------------------|
| Architecture pre-design ran or skip reason recorded            | Done   | Compared Micrometer-first, direct OTel, and framework-native-only approaches.               |
| Step 1-R research incorporated                                 | Done   | Current repo docs/code, lesson, and official Spring/Micrometer/OTel/Ktor docs incorporated. |
| Current-behavior claims cite current source/test/doc evidence  | Done   | Evidence paths are listed in Context and External Reference Baseline.                       |
| Spec path confirmed inside feature worktree                    | Done   | This file is under `.worktrees/docs-issue-696-observability-contract`.                      |
| Risks/failure modes included                                   | Done   | See Risks and Mitigations.                                                                  |
| Approach comparison and rejection rationale are research-based | Done   | See Design Options.                                                                         |
| User approval obtained                                         | Done   | User approved the #696 plan on 2026-06-06 KST.                                              |
| Open questions resolved or escalated                           | Done   | No blocker question remains; exporter/backend ownership remains a non-goal.                 |
