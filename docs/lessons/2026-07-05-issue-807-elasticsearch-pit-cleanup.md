# Issue 807 - Elasticsearch PIT Cleanup

## Context

`searchAsFlow` opened a Point-in-Time context and closed it from the collector coroutine's `finally` block. During cancellation, a suspend close call can be cancelled before it reaches Elasticsearch.

## Decision

Move PIT close into a small `NonCancellable` best-effort cleanup helper. Log cleanup failures without replacing the original collector cancellation or upstream failure.

## Outcome

A new unit test proves suspend cleanup completes after the parent coroutine is cancelled, and the existing Elasticsearch integration test continues to pass.

## Future Guidance

When a coroutine Flow opens a remote resource and closes it with a suspend call, put the close path behind a `NonCancellable` cleanup boundary and test the helper independently from heavy infrastructure tests.

