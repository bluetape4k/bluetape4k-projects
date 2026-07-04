# Lessons Learned - Issue #827 Ktor Correlation Tracing

## Context

`bluetape4k-ktor-observability` recorded `correlation.present` and optional
`correlation.id` in the OpenTelemetry Ktor extractor `onStart` callback. The
OpenTelemetry plugin starts before Ktor `ApplicationCallPipeline.Setup`, while
Ktor `CallId` sanitizes or generates the call id during `Setup`.

## Lesson

When tracing attributes depend on framework plugins that run later in the call
pipeline, capture them at span end unless the value must participate in sampler
decisions. For Ktor server spans, generated `CallId` values are available by
the OpenTelemetry extractor `onEnd` callback.

## Outcome

The tracing helper now records correlation attributes from `onEnd`, so
headerless requests produce the same generated `X-Request-Id` in both the HTTP
response and the server span.

## Future Guard

Keep generated correlation ID regression coverage whenever changing Ktor
OpenTelemetry extractor timing, CallId installation order, or correlation
header policy.

## Verification

- Regression test failed before the fix with `correlation.present=false`.
- Targeted regression test passed after the fix.
- Full `:bluetape4k-ktor-observability` compile/test/Kover validation passed.
- `git diff --check` passed.
