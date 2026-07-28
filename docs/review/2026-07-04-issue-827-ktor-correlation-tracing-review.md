# Issue #827 Ktor Correlation Tracing 검토

Date: 2026-07-04
Repo: `bluetape4k-projects`
Scope: `ktor/observability` OpenTelemetry correlation attributes

## Gate

- P0: 0
- P1: 0
- Verdict: PASS

## 7-Tier 검토

### Tier 1 - Correctness

- PASS. `KtorOpenTelemetryTracingSupport` now records correlation attributes in
  the OpenTelemetry extractor `onEnd` callback, after Ktor `CallId` has had a
  chance to sanitize or generate `call.callId`.
- PASS. The fallback still sanitizes request headers for applications that call
  `installBluetape4kKtorOpenTelemetryTracing` directly without installing
  bluetape4k `CallId`.

### Tier 2 - API and Compatibility

- PASS. No new public API or dependency is introduced.
- PASS. Existing `KtorOpenTelemetryTracingConfig.captureSanitizedCorrelationId`
  opt-in semantics are preserved.

### Tier 3 - Security and Privacy

- PASS. Raw request headers are not copied to span attributes. Values continue
  through `KtorCorrelationId.sanitize()` before `correlation.id` is recorded.
- PASS. `correlation.id` remains opt-in high-cardinality data.

### Tier 4 - Kotlin and bluetape4k Patterns

- PASS. The change reuses existing `KtorCorrelationId`, `CorrelationIdSettings`,
  and `CallId` integration instead of adding a parallel ID generator.
- PASS. Touched boolean assertions use `shouldBeTrue()` and infix comparison
  assertions from `bluetape4k-assertions`.

### Tier 5 - Tests

- PASS. Added a regression test for headerless requests with generated
  `X-Request-Id` and tracing capture enabled.
- PASS. The test verifies that the response header and span `correlation.id`
  contain the same generated value.

### Tier 6 - Operations

- PASS. No workflow, module registration, exporter, global OpenTelemetry SDK,
  or runtime configuration changes were made.

### Tier 7 - Documentation and Evidence

- PASS. KDoc contract now states that correlation attributes are recorded before
  span end and after CallId sanitization/generation.
- PASS. This review and the issue lesson are committed with the implementation.

## Verification Evidence

- Regression baseline before fix:
  `:bluetape4k-ktor-observability:test --tests "...observability installer records generated correlation id on server span"` failed with `correlation.present=false`.
- Targeted regression after fix:
  `:bluetape4k-ktor-observability:test --tests "...observability installer records generated correlation id on server span"` passed.
- Full module validation:
  `./gradlew :bluetape4k-ktor-observability:compileKotlin :bluetape4k-ktor-observability:compileTestKotlin :bluetape4k-ktor-observability:test :bluetape4k-ktor-observability:koverXmlReport --no-build-cache --no-configuration-cache` passed.
- Test result XML: 11 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check` passed.

## Residual Risk

- OpenTelemetry Ktor instrumentation is still alpha-versioned. Future upstream
  changes to extractor timing should be checked with this regression test.
