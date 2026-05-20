# Issue 495 Single-flight Loading

## Context

Issue #495 introduced reusable same-key in-flight loading for memoizer implementations. The first migration slice targeted cache-core in-memory sync, async, and suspend memoizers.

## Decision

Added an internal `SingleFlight` primitive instead of a new public API. It keeps separate coordination paths for blocking calls, `CompletableFuture`, and suspend calls while sharing one generation-token contract for `clear()`.

The migrated memoizers now write computed values only when the captured token is still current. Callers whose evaluator started before `clear()` still receive their computed result, but the stale result does not repopulate the cache.

## Outcome

- `InMemoryMemoizer` no longer uses `ConcurrentHashMap.getOrPut` for cache misses, avoiding duplicate same-key evaluator execution while a miss is active.
- `InMemoryAsyncMemoizer` delegates in-flight and generation handling to `SingleFlight`, while preserving null future failure behavior.
- `InMemorySuspendMemoizer` delegates same-key coordination to `SingleFlight` and keeps failed/cancelled work retryable.
- Added focused tests for same-key coalescing, clear-during-flight, null Java future completion, and suspend cancellation cleanup.

## Verification

Passed:

```bash
./gradlew :bluetape4k-cache-core:compileKotlin :bluetape4k-cache-core:compileTestKotlin --no-configuration-cache
./gradlew :bluetape4k-cache-core:test --tests '*SingleFlightTest' --tests '*InMemory*MemoizerTest' --no-configuration-cache
./gradlew :bluetape4k-cache-core:test --no-configuration-cache
git diff --check
```

Full cache-core result: 464 passing.

## Future Guidance

- Reuse `SingleFlight` for backend memoizers before adding another local `inFlight` + generation implementation.
- Increment generation before clearing in-flight maps or caches.
- Avoid short latch timeouts in concurrency tests; full-suite load can turn a correct test into a scheduling race.
