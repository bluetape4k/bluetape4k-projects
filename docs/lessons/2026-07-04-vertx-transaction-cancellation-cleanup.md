# Vert.x Transaction Cancellation Cleanup

## Context

Issue #940 found that Vert.x SQL transaction helpers performed rollback and
connection close in the caller coroutine context, so caller cancellation could
abort transaction cleanup.

## Decision

Run rollback and close operations inside `NonCancellable` cleanup boundaries.
Preserve the primary cancellation or failure and attach rollback or close
failures as suppressed exceptions.

## Outcome

`withSuspendTransaction` and `withSuspendRollback` now complete cleanup even
when the action cancels its coroutine context, while still rethrowing the
original `CancellationException`.

## Verification

- `./gradlew :bluetape4k-vertx:test --tests 'io.bluetape4k.vertx.sqlclient.PoolSupportTest'`

## Future Guidance

Coroutine-based database lifecycle cleanup should not use `runCatching` around
suspending rollback or close calls. Use explicit `try/catch` in a
`NonCancellable` boundary and preserve secondary cleanup failures as suppressed
evidence.
