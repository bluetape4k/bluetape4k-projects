# 이슈 #691 Ktor OpenTelemetry tracing

## 배경

`ktor/observability`는 이미 CallId, CallLogging, optional Micrometer, Prometheus
route helper를 소유했다. #696의 observability contract는 Ktor tracing을 명시적이고
application-owned이며 correlation ID 주변에서 안전하게 유지하도록 요구했다.

## 결정

application이 생성한 `OpenTelemetry` instance를 받고 OpenTelemetry의 Ktor 3 server
instrumentation을 감싸는 좁은 Ktor OpenTelemetry helper를 추가한다.
`opentelemetry-ktor-3.0`은 `compileOnly`로 두어 main runtime dependency graph에서
제외하고, tracing을 활성화할 때 application dependency로 문서화한다.

## 결과

baseline installer는 `KtorOpenTelemetryTracingConfig`를 통해 opt in할 수 있고, tracing을
사용하지 않는 기존 사용자는 이전 behavior를 유지한다. sanitize된 `correlation.id`는
trace-only이자 opt-in이며, `correlation.present`는 bounded 값이다.

## 검증

- `./gradlew :bluetape4k-ktor-observability:compileKotlin`
- `./gradlew :bluetape4k-ktor-observability:compileTestKotlin`
- `./gradlew :bluetape4k-ktor-observability:test`
- `git diff --check`

## 향후 가드

framework tracing wrapper를 추가할 때 library helper 안에서 SDK나 exporter를 만들지
않는다. alpha instrumentation dependency는 optional로 유지하고, backend-specific
behavior를 assert하는 대신 `InMemorySpanExporter`로 test한다.
