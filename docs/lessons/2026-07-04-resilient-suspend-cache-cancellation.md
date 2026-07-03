# Resilient Suspend Cache Cancellation

## Context

Issue #942 found `runCatching` around suspend back-cache reads in
`ResilientSuspendNearJCache`. That converted coroutine cancellation into
configured fallback behavior.

## Decision

Replace suspend `runCatching` fallback paths with explicit `try/catch` blocks
that rethrow `CancellationException` before applying non-cancellation fallback
behavior.

## Outcome

`get`, `getAll`, `replace`, and `containsKey` now preserve structured
concurrency cancellation while still returning null/false for ordinary back
cache failures where the existing contract expects graceful degradation.

## Verification

- `./gradlew :bluetape4k-cache-core:test --tests 'io.bluetape4k.cache.nearcache.jcache.ResilientSuspendNearJCacheTest'`

## Future Guidance

Do not use `runCatching` around suspend cache calls unless cancellation is
handled and rethrown before fallback behavior is applied.
