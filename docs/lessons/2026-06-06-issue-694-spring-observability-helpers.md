# Issue #694 Spring Observability Helpers

## Context

The Spring Boot 4 observability work needed helper APIs for service, HTTP
handler, and event handler code while preserving Spring Boot Actuator and
Micrometer ownership of metrics, tracing, exporters, and endpoints.

## Decision

Add narrow `ObservationRegistry` helpers in `spring-boot/core` instead of
auto-configuring exporters or custom endpoints. Treat Prometheus and OTLP as
application configuration concerns documented in README examples.

## Outcome

`observeSpring` and `observeSpringSuspending` now manage observation lifecycle,
exception recording, cancellation propagation, and coroutine scope cleanup.
README files also now reflect Spring Boot 4's Jackson 3 default.

## Verification

- `./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.observability.SpringObservationSupportTest'`
- `./gradlew :bluetape4k-spring-boot-core:test`
- `git diff --check`

## Future Guard

For Spring Boot observability helpers, accept framework-owned objects such as
`ObservationRegistry` and document backend properties. Do not instantiate
OpenTelemetry SDKs, tracing exporters, or Prometheus endpoints in the library.
