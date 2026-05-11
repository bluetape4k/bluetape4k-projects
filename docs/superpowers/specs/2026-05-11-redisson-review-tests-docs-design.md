# bluetape4k-redisson Review / Tests / Docs Design

## Scope

- Module: `infra/redisson`
- Goal: close review findings, add missing edge tests, strengthen public API KDoc/examples, and refresh README files so a standalone redisson PR can be opened after the lettuce PR.
- Skills: `bluetape4k-design` 6-Tier gate and `bluetape4k-patterns`.

## Findings

### P1 - README advertises non-existent leader election APIs

`README.md` and `README.ko.md` document `RedissonLeaderElection`, `RedissonSuspendLeaderElection`, and `RedissonLeaderGroupElection`, but `infra/redisson/src/main/kotlin` has no `leader` package. The feature table, examples, diagrams, and Redis version table must not advertise non-exported APIs.

### P1 - README NearCache example uses non-existent API shape

The NearCache example imports `RedisCacheConfig` and calls a constructor shape that does not exist. The actual public entry point is `RedissonNearCache(redisson, LocalCachedMapOptions)` plus `RedissonNearCache.defaultLocalCacheOptions(name)`.

### P2 - RFuture adapters lack direct edge coverage

Existing tests cover success paths and empty collections. Missing edge coverage:

- `sequence()` preserves input ordering even when futures complete out of order.
- `awaitAll()` propagates failed futures.
- coroutine stress around `awaitAll()` uses `SuspendedJobTester`, as required for coroutine-safety test additions.

## Acceptance Criteria

- Public docs mention only APIs exported from `infra/redisson`.
- NearCache examples compile against the current public API shape.
- RFuture edge tests compile and pass.
- `./gradlew :bluetape4k-redisson:test` passes, or any infrastructure blocker is recorded.
- 6-Tier review has no remaining P0/P1 findings for the redisson PR scope.
