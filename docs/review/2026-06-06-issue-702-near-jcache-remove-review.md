# Issue #702 NearJCache remove(key, oldValue) Review

## Scope

- Files:
  - `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt`
  - `cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/jcache/AbstractNearJCacheTest.kt`
- Work type: Type B Fast Track test/tech-debt cleanup.
- Goal: lock value-aware remove semantics for sync `NearJCache` back-cache consistency.

## Review Result

- P0: 0
- P1: 0
- Verdict: PASS

## Checks

- API compatibility: PASS. Public behavior is unchanged; the TODO is replaced with a contract comment.
- Cache consistency: PASS. Matching value removal now asserts back cache deletion and peer front-cache invalidation.
- Negative cases: PASS. Non-matching value and missing-key cases keep front/back caches unchanged or empty.
- Provider coverage: PASS. The shared conformance fixture runs through `Cache2KNearJJCacheTest` and `EhcacheNearJJCacheTest`.
- Blast radius: PASS. CodeGraph reported risk score `0.00`, no impacted files, and no test gaps for the two changed files.

## Validation Evidence

- `./gradlew :bluetape4k-cache-core:test --tests "*NearJJCacheTest"`: PASS, 140 passing.
- `./gradlew :bluetape4k-cache-core:test`: PASS, 470 passing.
- `git diff --check`: PASS.

## Notes

- The previous `:cache:cache-core:test` command failed before test execution because the repo uses the flat Gradle path `:bluetape4k-cache-core`.
- IntelliJ diagnostics were not available in this session; targeted Gradle compilation/tests were used as fallback.
