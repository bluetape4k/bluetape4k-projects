# Issue 804 - Cancel Leaf Redis Futures

## Context

Redis bulk await helpers waited on one aggregate `CompletableFuture` created by
`CompletionStage.sequence`. Cancelling the caller coroutine cancelled that
aggregate await, but it did not cancel the original Lettuce `RedisFuture` or
Redisson `RFuture` leaves.

## Decision

Put leaf cancellation in the shared `CompletionStage.sequence` boundary. When
the returned aggregate future is cancelled, each source future receives
`cancel(true)`. Redis-specific helpers keep their existing API and inherit the
shared all-or-nothing cancellation behavior.

## Outcome

Core, Lettuce, and Redisson tests now prove that aggregate/coroutine
cancellation cancels pending source futures while preserving input-order success
results and existing failure propagation.

## Future Guidance

When wrapping many external futures behind one coroutine await, cancellation
must be propagated to the source futures, not only to the aggregate future.
Cover both the shared aggregate helper and one representative adapter boundary
when the behavior is reused across modules.
