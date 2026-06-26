# Lessons Learned - Issue 806 OpenTelemetry Redacted Exception Telemetry (2026-06-27)

Issue: #806
Module: `infra/opentelemetry`

## Context

OpenTelemetry span helpers recorded exception messages into exported span status and exception events by default. Exception messages often contain SQL fragments, URLs, tokens, or user input, so helper-managed failure paths must treat raw messages as sensitive by default.

## Decision

Helper-managed failures now emit a redacted `exception` event and set `StatusCode.ERROR` with `"unspecified error"` as the exported message. The original exception is still rethrown to callers. Full OpenTelemetry `recordException` remains available only as an explicit opt-in at a boundary where exporting the raw exception message is allowed.

## Test Helper Judgment

The change affects coroutine and Flow failure paths, so `SuspendedJobTester` is the fitting bluetape4k helper. It was used for coroutine and Flow redaction stress tests.

`MultithreadingTester` and `StructuredTaskScopeTester` were not used because this fix does not add shared mutable production state, thread contention, virtual-thread behavior, or `StructuredTaskScope` semantics.

## Future Guard

For telemetry helpers, test both status descriptions and event attributes. `recordException(error)` can export `exception.message`, so a status-only assertion is not enough for secret-leak regressions.
