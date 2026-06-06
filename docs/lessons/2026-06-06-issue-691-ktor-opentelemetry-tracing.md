# Issue #691 Ktor OpenTelemetry Tracing

## Context

`ktor/observability` already owned CallId, CallLogging, optional Micrometer, and
Prometheus route helpers. The observability contract from #696 required Ktor
tracing to stay explicit, application-owned, and safe around correlation IDs.

## Decision

Add a narrow Ktor OpenTelemetry helper that accepts an application-created
`OpenTelemetry` instance and wraps OpenTelemetry's Ktor 3 server instrumentation.
Keep `opentelemetry-ktor-3.0` out of the main runtime dependency graph by using
`compileOnly`; document it as an application dependency when tracing is enabled.

## Outcome

The baseline installer can opt in through `KtorOpenTelemetryTracingConfig`, while
existing users without tracing keep the previous behavior. Sanitized
`correlation.id` is trace-only and opt-in; `correlation.present` is bounded.

## Verification

- `./gradlew :bluetape4k-ktor-observability:compileKotlin`
- `./gradlew :bluetape4k-ktor-observability:compileTestKotlin`
- `./gradlew :bluetape4k-ktor-observability:test`
- `git diff --check`

## Future Guard

When adding framework tracing wrappers, do not create SDKs or exporters inside
library helpers. Keep alpha instrumentation dependencies optional and test them
with `InMemorySpanExporter` instead of asserting backend-specific behavior.
