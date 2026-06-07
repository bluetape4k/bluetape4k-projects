# bluetape4k Spring Boot observability demo

English | [한국어](./README.ko.md)

Runnable Spring Boot 4 application that shows how to use bluetape4k observation helpers with
Spring Boot Actuator Prometheus metrics and application-owned OTLP tracing configuration.

## Example Scenario

The demo models a small order-event workflow:

1. A client posts an order event with an optional `X-Request-Id` correlation header.
2. The Spring MVC controller delegates the request to `OrderEventService`.
3. `OrderEventService` wraps the HTTP/service boundary with `ObservationRegistry.observeSpring`.
4. The service records an `event.publish` observation and an `event.consume` observation for the same logical event.
5. Spring Boot Actuator exposes HTTP and event observation metrics at `/actuator/prometheus`.
6. OTLP tracing can be enabled by pointing Spring Boot's Micrometer tracing exporter at a local collector.

This keeps the example local-testable: Prometheus scraping works without a Prometheus server, and tests do
not require an OTLP collector.

## Architecture

![Spring Boot observability demo architecture](../../../docs/images/readme-diagrams/examples-spring-boot-observability-spring-boot-demo-architecture-01.png)

Spring Boot owns metrics endpoint registration. The application contributes observed work; Actuator
turns Micrometer observations into Prometheus scrape output.

## Sequence Diagram

![Spring Boot observability demo sequence](../../../docs/images/readme-diagrams/examples-spring-boot-observability-spring-boot-demo-sequence-01.png)

## Dependencies

The demo uses the Spring Boot BOM and the existing bluetape4k helper modules:

- `bluetape4k-spring-boot-core` for `ObservationRegistry.observeSpring`
- `bluetape4k-micrometer` for event publish/consume telemetry helpers
- `spring-boot-starter-actuator` plus `micrometer-registry-prometheus` for `/actuator/prometheus`
- `micrometer-tracing-bridge-otel` and `opentelemetry-exporter-otlp` for optional OTLP trace export

## Configuration

Spring Boot owns Prometheus and OTLP configuration. The application does not register a custom
Prometheus endpoint.

```yaml
management:
  defaults:
    metrics:
      export:
        enabled: false
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    prometheus:
      access: read_only
  prometheus:
    metrics:
      export:
        enabled: true
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT:http://localhost:4318/v1/traces}
```

`management.defaults.metrics.export.enabled=false` keeps registry implementations that may be present
on the demo classpath, such as Datadog, from starting without credentials. Prometheus is enabled
explicitly.

## Run

```bash
./gradlew :observability-spring-boot-demo:bootRun
```

The application listens on `localhost:8080`.

## Verify

Create an observed application event:

```bash
curl -X POST -H 'X-Request-Id: REQ_123' \
  http://localhost:8080/orders/order-1/events
```

Scrape metrics through Spring Boot Actuator:

```bash
curl http://localhost:8080/actuator/prometheus | grep -E 'http_server_requests|event_publish|event_consume'
```

Health remains an Actuator endpoint:

```bash
curl http://localhost:8080/actuator/health
```

To export traces, run an OTLP collector locally or set:

```bash
export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://localhost:4318/v1/traces
```

The tests disable external trace export and prove the application starts, event telemetry runs, and
Prometheus output includes HTTP and event observation metrics.

## Test

```bash
./gradlew :observability-spring-boot-demo:compileKotlin :observability-spring-boot-demo:test
```
