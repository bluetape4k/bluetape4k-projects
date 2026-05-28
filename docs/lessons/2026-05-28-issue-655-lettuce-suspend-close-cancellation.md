# Issue 655: Lettuce suspended loaded-map shutdown cancellation

## Context

`LettuceSuspendedLoadedMap.suspendClose()` waited for the write-behind job under a shutdown timeout, but the old broad `Exception` catch could hide coroutine cancellation while draining pending writes.

## Decision

Keep caller cancellation observable. Use `withTimeoutOrNull` for the map-owned shutdown timeout so that only the internal drain timeout is downgraded to a warning. Rethrow `CancellationException` before generic failure logging, and keep connection/job cleanup in `NonCancellable`.

## Outcome

`suspendClose()` now distinguishes external cancellation from internal drain timeout. A regression test verifies that caller cancellation returns before `writeBehindShutdownTimeout` when the writer is still blocked.

## Verification

- `./gradlew :bluetape4k-lettuce:test --tests 'io.bluetape4k.redis.lettuce.map.LettuceSuspendedLoadedMapTest.suspendClose - caller cancellation is propagated before internal shutdown timeout'`
- `./gradlew :bluetape4k-lettuce:test`

## Future Guidance

For suspending cleanup paths, do not catch `Exception` around suspend calls unless `CancellationException` is rethrown first. Prefer `withTimeoutOrNull` when an operation-owned timeout should be handled locally without confusing caller cancellation.
