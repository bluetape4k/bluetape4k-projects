# bluetape4k Ktor observability demo

English | [한국어](./README.ko.md)

Runnable Ktor 3 application that shows application-owned Prometheus metrics routing, opt-in
OpenTelemetry tracing, and bluetape4k event telemetry helpers.

## Example Scenario

The demo models a Ktor application that owns its observability plumbing explicitly:

1. The application creates a `PrometheusMeterRegistry`.
2. `installBluetape4kKtorObservability` installs correlation IDs, call logging, Micrometer metrics, and optional tracing.
3. A client posts an order event with an optional `X-Request-Id` header.
4. The route records `event.publish` and `event.consume` observations through bluetape4k event telemetry helpers.
5. The application exposes its own Prometheus scrape endpoint at `/metrics`.
6. Passing an `OpenTelemetry` SDK instance enables Ktor server spans; passing `null` keeps tracing disabled.

This intentionally differs from the Spring Boot demo: Ktor has no Actuator endpoint, so the application
owns the registry and route.

## Architecture

```mermaid
flowchart LR
    Client[HTTP Client]
    Routes[Ktor Routes]
    Core[bluetape4k Ktor Core]
    Obs[bluetape4k Ktor Observability]
    Service[OrderEventTelemetryService]
    EventObs[bluetape4k event telemetry]
    Registry[PrometheusMeterRegistry]
    Metrics["/metrics"]
    OTel[OpenTelemetry SDK optional]

    Client --> Routes
    Routes --> Core
    Routes --> Obs
    Routes --> Service
    Service --> EventObs
    Obs --> Registry
    EventObs --> Registry
    Routes --> Metrics
    Metrics --> Registry
    Obs -. server spans when configured .-> OTel
```

The scrape route is application-owned. `prometheusScrapeRoute(registry)` only exposes the registry
content; it does not create global exporters or backend connections.

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Client
    participant Ktor as Ktor Routes
    participant Obs as bluetape4k Ktor Observability
    participant EventObs as event telemetry
    participant Prom as PrometheusMeterRegistry
    participant OTel as OpenTelemetry SDK

    Client->>Ktor: POST /orders/{orderId}/events
    Ktor->>Obs: correlation ID, call logging, metrics, tracing
    Ktor->>EventObs: observe event.publish
    Ktor->>EventObs: observe event.consume
    EventObs->>Prom: Micrometer timers
    Obs->>OTel: server span when configured
    Ktor-->>Client: 200 JSON
    Client->>Ktor: GET /metrics
    Ktor->>Prom: scrape()
    Ktor-->>Client: Prometheus text
```

## Dependencies

- `bluetape4k-ktor-core`: JSON, error handling, health/readiness baseline
- `bluetape4k-ktor-observability`: correlation ID, call logging, Micrometer, Prometheus route, OTel tracing helper
- `bluetape4k-micrometer`: event publish/consume telemetry helpers
- `micrometer-registry-prometheus`: application-owned scrape registry
- `opentelemetry-ktor-3.0`: optional Ktor server spans

## Configuration

Ktor does not expose Prometheus through Actuator. The application owns the registry and explicitly
mounts the scrape route:

```kotlin
val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

installBluetape4kKtorObservability(
    Bluetape4kKtorObservabilityConfig(
        meterRegistry = meterRegistry,
        tracing = KtorOpenTelemetryTracingConfig(openTelemetry),
    )
)

routing {
    prometheusScrapeRoute(meterRegistry, path = "/metrics")
}
```

Passing `null` for the `OpenTelemetry` instance keeps tracing disabled. Tests use an in-memory SDK
exporter, so no external collector is required.

## Run

```bash
./gradlew :observability-ktor-demo:run
```

The application listens on `0.0.0.0:8080`.

## Verify

Create an observed application event:

```bash
curl -X POST -H 'X-Request-Id: REQ_123' \
  http://localhost:8080/orders/order-1/events
```

Scrape application-owned Prometheus metrics:

```bash
curl http://localhost:8080/metrics | grep -E 'ktor_http_server_requests|event_publish|event_consume'
```

Check health routes:

```bash
curl http://localhost:8080/health
curl http://localhost:8080/healthz
curl http://localhost:8080/readyz
```

## Test

```bash
./gradlew :observability-ktor-demo:compileKotlin :observability-ktor-demo:test
```
