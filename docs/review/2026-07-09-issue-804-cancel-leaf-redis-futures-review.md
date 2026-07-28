# Issue 804 검토 - Cancel Leaf Redis Futures

## Scope

- `bluetape4k/core`: `CompletionStage.sequence` aggregate cancellation behavior.
- `infra/lettuce`: `RedisFuture.awaitAll` and `sequence` cancellation contract.
- `infra/redisson`: `RFuture.awaitAll` and `sequence` cancellation contract.

## 발견 사항

- P0/P1: 0 after fix.
- The red test reproduced the issue through Redisson `awaitAll`: cancelling the coroutine left pending `RFuture` leaves uncancelled.
- The fix centralizes leaf cancellation in `CompletionStage.sequence`, so Lettuce and Redisson keep one shared aggregate behavior.
- Existing exception propagation and input-order result construction remain unchanged because result collection still waits for `CompletableFuture.allOf` and maps `join()` in source order.

## Concurrency Test Gate

The new tests are deterministic cancellation-boundary tests, not stress tests. Existing `SuspendedJobTester` coverage remains in both Redis helper test classes for repeated concurrent await stability.

## Verification

- RED: `RFutureSupportTest > awaitAll cancels pending RFuture leaves when coroutine is cancelled` failed with `Expected <false> to be <true>`.
- GREEN: `./gradlew :bluetape4k-core:test --tests "io.bluetape4k.concurrent.CompletionStageSupportTest" :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.RedisFutureSupportTest" :bluetape4k-redisson:test --tests "io.bluetape4k.redis.redisson.coroutines.RFutureSupportTest" --no-build-cache`
- Hygiene: `git diff --check`

## Residual Risk

Changing `CompletionStage.sequence` broadens cancellation propagation for all callers that cancel the returned aggregate future. That is intentional for all-or-nothing bulk waits, but callers that previously expected aggregate cancellation to leave leaves running should avoid cancelling the aggregate or build an explicit detached future boundary.
