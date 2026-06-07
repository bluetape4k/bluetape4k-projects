# bluetape4k Spring Boot observability demo

[English](./README.md) | 한국어

Spring Boot 4에서 bluetape4k observation helper를 Spring Boot Actuator Prometheus metrics와
애플리케이션 소유 OTLP tracing 설정으로 사용하는 실행 가능한 예제입니다.

## 예제 시나리오

이 예제는 작은 주문 이벤트 workflow를 모델링합니다.

1. 클라이언트가 선택적 `X-Request-Id` correlation header와 함께 주문 이벤트를 전송합니다.
2. Spring MVC controller가 요청을 `OrderEventService`로 전달합니다.
3. `OrderEventService`는 HTTP/service 경계를 `ObservationRegistry.observeSpring`으로 감쌉니다.
4. 같은 논리 이벤트에 대해 `event.publish` observation과 `event.consume` observation을 기록합니다.
5. Spring Boot Actuator가 HTTP 및 event observation metrics를 `/actuator/prometheus`로 노출합니다.
6. 로컬 collector endpoint를 지정하면 Spring Boot Micrometer tracing exporter가 OTLP trace를 보낼 수 있습니다.

Prometheus server 없이 scrape 결과를 확인할 수 있고, 테스트는 OTLP collector 없이 실행되도록 구성했습니다.

## Architecture

![Spring Boot observability demo architecture](../../../docs/images/readme-diagrams/examples-spring-boot-observability-spring-boot-demo-architecture-01.png)

Spring Boot가 metrics endpoint 등록을 소유합니다. 애플리케이션은 관측 대상 작업을 제공하고,
Actuator가 Micrometer observation을 Prometheus scrape output으로 변환합니다.

## Sequence Diagram

![Spring Boot observability demo sequence](../../../docs/images/readme-diagrams/examples-spring-boot-observability-spring-boot-demo-sequence-01.png)

## 의존성

이 예제는 Spring Boot BOM과 기존 bluetape4k helper 모듈을 사용합니다.

- `bluetape4k-spring-boot-core`: `ObservationRegistry.observeSpring`
- `bluetape4k-micrometer`: event publish/consume telemetry helper
- `spring-boot-starter-actuator`와 `micrometer-registry-prometheus`: `/actuator/prometheus`
- `micrometer-tracing-bridge-otel`과 `opentelemetry-exporter-otlp`: 선택적 OTLP trace export

## 설정

Prometheus와 OTLP 설정은 Spring Boot가 소유합니다. 이 애플리케이션은 별도 custom Prometheus endpoint를 만들지 않습니다.

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

`management.defaults.metrics.export.enabled=false`는 예제 classpath에 Datadog 같은 registry 구현이
있더라도 credential 없이 시작되지 않게 막습니다. Prometheus만 명시적으로 활성화합니다.

## 실행

```bash
./gradlew :observability-spring-boot-demo:bootRun
```

애플리케이션은 `localhost:8080`에서 실행됩니다.

## 확인

관측 대상 애플리케이션 이벤트를 만듭니다.

```bash
curl -X POST -H 'X-Request-Id: REQ_123' \
  http://localhost:8080/orders/order-1/events
```

Spring Boot Actuator를 통해 metrics를 scrape합니다.

```bash
curl http://localhost:8080/actuator/prometheus | grep -E 'http_server_requests|event_publish|event_consume'
```

Health도 Actuator endpoint로 확인합니다.

```bash
curl http://localhost:8080/actuator/health
```

Trace를 export하려면 로컬 OTLP collector를 실행하거나 다음 환경 변수를 지정합니다.

```bash
export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://localhost:4318/v1/traces
```

테스트는 외부 trace export를 비활성화하고, 애플리케이션 기동, event telemetry 실행, Prometheus 출력의 HTTP/event observation metrics를 검증합니다.

## 테스트

```bash
./gradlew :observability-spring-boot-demo:compileKotlin :observability-spring-boot-demo:test
```
