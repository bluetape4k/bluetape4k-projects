# Issue 805 - Distributed Rate Limiter Cancellation

## Context

`DistributedSuspendRateLimiter` used `withTimeout` and then caught
`TimeoutCancellationException` before `CancellationException`. That made the
limiter-owned timeout path clear, but it also allowed timeout-shaped
cancellation from an async bucket boundary to be converted into
`RateLimitResult.ERROR`.

## Decision

Model limiter-owned timeout explicitly with `withTimeoutOrNull`, convert only
that `null` result into `RateLimitResult.ERROR`, and rethrow every
`CancellationException` before general error handling.

## Outcome

The distributed suspend tests now cover three cancellation boundaries: cancelled
await, async timeout cancellation, and caller `withTimeout` with and without
`defaultTimeout`. The existing limiter-owned timeout test still returns
`RateLimitStatus.ERROR`.

## Future Guidance

Do not catch `TimeoutCancellationException` as an ordinary error around suspend
boundaries. Use an explicit timeout result boundary when a component owns the
timeout, then let coroutine cancellation propagate through the normal
`CancellationException` path.
