# Review - Lettuce Typed Cache Lookup (2026-06-26)

Issue: #787
Branch: `fix/lettuce-cache-typed-getcache`
Module: `:bluetape4k-cache-lettuce`

## Scope

- Added explicit key/value type checks to `LettuceCacheManager.getCache(cacheName, keyType, valueType)`.
- Exposed `LettuceJCache.configuration` internally so the manager can validate the existing cache's configured types.
- Added regression coverage for exact typed lookup, key mismatch, value mismatch, null type arguments, and closed-manager
  behavior.

## 7-Tier Review

| Tier | Result | Evidence |
|---|---:|---|
| Correctness | PASS | Typed lookup now compares requested classes with the cache configuration before returning the cache. |
| Compatibility | PASS | Public API signatures are unchanged; `configuration` visibility is widened only to module-internal. |
| Boundary behavior | PASS | Missing caches still return `null`; closed managers still fail before argument validation. |
| Test coverage | PASS | New mismatch tests failed before the fix and pass after the type checks. |
| Simplicity | PASS | The fix stays in the manager lookup path; no new abstraction or dependency. |
| Documentation | PASS | Lesson artifact records the JCache typed lookup boundary rule. |
| Regression risk | PASS | Cache Lettuce module tests pass; CodeGraph reported low impact for the changed files. |

## Findings

P0: 0
P1: 0

P2/P3: none requiring code changes before PR.

## Validation Evidence

- Reproduced before fix:
  - `./gradlew :bluetape4k-cache-lettuce:test --tests 'io.bluetape4k.cache.jcache.LettuceJCacheManagerTest.typed getCache returns cache when key and value types match' --tests 'io.bluetape4k.cache.jcache.LettuceJCacheManagerTest.typed getCache throws when key type does not match' --tests 'io.bluetape4k.cache.jcache.LettuceJCacheManagerTest.typed getCache throws when value type does not match' --no-build-cache`
  - Result: FAIL with `Expected ClassCastException but no exception was thrown` for key and value mismatch cases.
- After fix:
  - `./gradlew :bluetape4k-cache-lettuce:compileKotlin :bluetape4k-cache-lettuce:compileTestKotlin :bluetape4k-cache-lettuce:test --tests 'io.bluetape4k.cache.jcache.LettuceJCacheManagerTest' --no-build-cache`
  - Result: PASS.
  - `./gradlew :bluetape4k-cache-lettuce:compileKotlin :bluetape4k-cache-lettuce:compileTestKotlin :bluetape4k-cache-lettuce:test --no-build-cache`
  - Result: PASS.
  - `git diff --check`
  - Result: PASS.
