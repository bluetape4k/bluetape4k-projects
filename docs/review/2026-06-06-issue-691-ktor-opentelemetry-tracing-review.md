# Issue #691 Ktor OpenTelemetry Tracing 검토

Date: 2026-06-06
Repo: `bluetape4k-projects`
Scope: `ktor/observability` OpenTelemetry tracing helper

## Gate

- P0: 0
- P1: 0
- Verdict: PASS

## Review Notes

- Public API keeps telemetry backend ownership application-side by accepting an
  `OpenTelemetry` instance and never constructing or registering a global SDK.
- `opentelemetry-ktor-3.0` remains `compileOnly` for main code and is documented
  as a tracing-only application dependency, limiting the alpha instrumentation
  blast radius.
- Correlation handling reuses `KtorCorrelationId.sanitize()` and records
  `correlation.id` only when explicitly enabled. Raw request header values,
  authorization headers, bodies, and query strings are not captured by the
  bluetape4k helper.
- Existing CallId, CallLogging, Micrometer, and Prometheus paths remain covered
  by the expanded module test.

## Verification Evidence

- `./gradlew :bluetape4k-ktor-observability:compileKotlin`
- `./gradlew :bluetape4k-ktor-observability:compileTestKotlin`
- `./gradlew :bluetape4k-ktor-observability:test` - 9 tests passing
- `git diff --check`

## Residual Risk

- The wrapped OpenTelemetry Ktor instrumentation is alpha-versioned. Future
  updates may require source-compatible adjustments around `KtorServerTelemetry`
  builder APIs.
