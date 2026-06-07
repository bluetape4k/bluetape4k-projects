# bluetape4k Ktor observability demo

[English](./README.md) | 한국어

Ktor 3에서 애플리케이션이 소유한 Prometheus metrics route, opt-in OpenTelemetry tracing,
bluetape4k event telemetry helper를 함께 사용하는 실행 가능한 예제입니다.

## 예제 시나리오

이 예제는 observability 구성을 명시적으로 소유하는 Ktor 애플리케이션을 모델링합니다.

1. 애플리케이션이 `PrometheusMeterRegistry`를 생성합니다.
2. `installBluetape4kKtorObservability`가 correlation ID, call logging, Micrometer metrics, optional tracing을 설치합니다.
3. 클라이언트가 선택적 `X-Request-Id` header와 함께 주문 이벤트를 전송합니다.
4. route는 bluetape4k event telemetry helper로 `event.publish`와 `event.consume` observation을 기록합니다.
5. 애플리케이션이 자체 Prometheus scrape endpoint `/metrics`를 노출합니다.
6. `OpenTelemetry` SDK 인스턴스를 전달하면 Ktor server span이 활성화되고, `null`을 전달하면 tracing은 비활성화됩니다.

Spring Boot 예제와 달리 Ktor에는 Actuator endpoint가 없습니다. 따라서 애플리케이션이 registry와 route를 직접 소유합니다.

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

Scrape route는 애플리케이션이 소유합니다. `prometheusScrapeRoute(registry)`는 registry 내용을
노출할 뿐, global exporter나 backend 연결을 만들지 않습니다.

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
    Ktor->>EventObs: event.publish 관측
    Ktor->>EventObs: event.consume 관측
    EventObs->>Prom: Micrometer timers
    Obs->>OTel: 설정된 경우 server span
    Ktor-->>Client: 200 JSON
    Client->>Ktor: GET /metrics
    Ktor->>Prom: scrape()
    Ktor-->>Client: Prometheus text
```

## 의존성

- `bluetape4k-ktor-core`: JSON, 오류 처리, health/readiness 기본 기능
- `bluetape4k-ktor-observability`: correlation ID, call logging, Micrometer, Prometheus route, OTel tracing helper
- `bluetape4k-micrometer`: event publish/consume telemetry helper
- `micrometer-registry-prometheus`: 애플리케이션 소유 scrape registry
- `opentelemetry-ktor-3.0`: 선택적 Ktor server span

## 설정

Ktor는 Actuator로 Prometheus를 노출하지 않습니다. 애플리케이션이 registry를 소유하고 scrape route를 명시적으로 mount합니다.

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

`OpenTelemetry` 인스턴스에 `null`을 전달하면 tracing은 비활성화됩니다. 테스트는 in-memory SDK exporter를 사용하므로 외부 collector가 필요하지 않습니다.

## 실행

```bash
./gradlew :observability-ktor-demo:run
```

애플리케이션은 `0.0.0.0:8080`에서 실행됩니다.

## 확인

관측 대상 애플리케이션 이벤트를 만듭니다.

```bash
curl -X POST -H 'X-Request-Id: REQ_123' \
  http://localhost:8080/orders/order-1/events
```

애플리케이션 소유 Prometheus metrics를 scrape합니다.

```bash
curl http://localhost:8080/metrics | grep -E 'ktor_http_server_requests|event_publish|event_consume'
```

Health route를 확인합니다.

```bash
curl http://localhost:8080/health
curl http://localhost:8080/healthz
curl http://localhost:8080/readyz
```

## 테스트

```bash
./gradlew :observability-ktor-demo:compileKotlin :observability-ktor-demo:test
```
