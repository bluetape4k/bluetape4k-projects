# Issue #694 Spring Observability Helpers 검토

Date: 2026-06-06
Scope: `spring-boot/core`

## Verdict

PASS

- P0: 0
- P1: 0

## 발견 사항

No blocking findings.

## 증거

- Added `ObservationRegistry.observeSpring` and
  `ObservationRegistry.observeSpringSuspending` helpers that use the
  application-owned `ObservationRegistry`.
- Key values are grouped with `SpringObservationKeyValues` so bounded
  low-cardinality labels stay separate from trace-oriented high-cardinality
  values.
- The helpers do not register Prometheus endpoints, create exporters, or mutate
  global OpenTelemetry SDK state.
- Spring Boot core README files now document Prometheus through Actuator and
  OTLP tracing through application-owned Spring Boot properties.
- The stale Spring Boot 4 / Jackson 2 README statements were corrected to
  Jackson 3 by default.

## 검증

- `./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.observability.SpringObservationSupportTest'`
- `./gradlew :bluetape4k-spring-boot-core:test`
- `git diff --check`

## Residual Risk

`micrometer-observation` and context propagation remain `compileOnly` in the
library module. Applications must bring the usual Spring Boot Actuator and
Micrometer runtime dependencies when using these helpers.
