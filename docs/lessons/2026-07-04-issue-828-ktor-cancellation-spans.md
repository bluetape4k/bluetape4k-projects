# Issue #828 Ktor Cancellation Spans

## Context

The observability contract says intentional coroutine cancellation must not become an error metric or an `ERROR` span status. `ktor/observability` delegated Ktor server span lifecycle to OpenTelemetry's Ktor instrumentation, but had no regression test for a route that throws `CancellationException`.

## Decision

Add a focused Ktor OpenTelemetry regression test that throws `CancellationException` from a route and asserts exported spans never use `StatusCode.ERROR`. If the instrumentation exports a cancellation span in the future, the test requires the status to remain `UNSET`.

## Outcome

The current OpenTelemetry Ktor behavior does not export a span for the canceled route in the Ktor test host. That already satisfies "no ERROR span" for cancellation, and the new test locks the contract while preserving existing coverage that real 500 responses still record `ERROR`.

## Verification

- Targeted cancellation and real-error tracing tests
- Full `:bluetape4k-ktor-observability` compile/test command
- `git diff --check`

## Future Guard

When wrapping framework instrumentation, test cancellation separately from ordinary handler failures. Do not assume a framework's error response shape is the same as the tracing status contract.
