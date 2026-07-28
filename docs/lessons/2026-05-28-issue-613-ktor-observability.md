# 이슈 613 - Ktor observability baseline helper

## 배경

issue #613은 `bluetape4k-ktor-core`가 merge된 뒤 reusable observability helper를
추가한다. 목표는 metric registry, exporter, tracing backend 선택 같은
application-owned policy를 숨기지 않으면서 production diagnostics를 제공하는 것이다.

## 결정

명시적인 Ktor-native helper를 제공한다.

- `installBluetape4kKtorObservability()`는 `CallId`, `CallLogging`, optional
  `MicrometerMetrics`를 설치한다.
- `KtorCorrelationId`는 MDC 또는 response propagation 전에 inbound correlation ID를
  sanitize한다.
- `prometheusScrapeRoute()`는 application이 `PrometheusMeterRegistry`를 제공할 때만
  route를 노출한다.

installer configuration이 `MeterRegistry`를 받으므로 Micrometer core는 API dependency다.
Prometheus registry는 optional scrape helper에만 필요하므로 main code에서는
compile-only로 유지하고 application-owned라고 문서화한다.

## 결과

module은 global mutable registry를 피하고 tracing을 default로 설치하지 않는다.
OpenTelemetry policy는 application-owned로 남기거나, 나중에 더 좁은 module decision으로
다룬다.

## 검증

- `./gradlew :bluetape4k-ktor-observability:compileKotlin :bluetape4k-ktor-observability:compileTestKotlin`
- `./gradlew :bluetape4k-ktor-observability:test :bluetape4k-ktor-observability:koverXmlReport`
- Kover XML: line coverage 88/92 (95.7%).

## 향후 가드

caller가 제공한 correlation header를 그대로 echo하지 않는다. 새 observability helper는
inbound value를 먼저 sanitize하거나 propagation을 application에 맡겨야 한다.
