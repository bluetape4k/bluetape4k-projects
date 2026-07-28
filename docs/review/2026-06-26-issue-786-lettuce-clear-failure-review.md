# Review - Lettuce Near Cache Clear Failure (2026-06-26)

Issue: #786
Branch: `fix/lettuce-near-cache-clear-failure`
Module: `:bluetape4k-cache-lettuce`

## Scope

- Changed blocking `LettuceNearCache.clearAll()` to propagate Redis backend cleanup failures.
- Added a regression test that makes Redis key decoding fail during `SCAN`, proves `clearAll()` throws, and verifies the
  backend key remains.

## Review Findings

P0: 0
P1: 0

P2/P3: none requiring code changes before PR.

## 7-Tier 검토

| Tier | Result | Evidence |
|---|---:|---|
| Correctness | PASS | Blocking `clearAll()` now matches suspend semantics by letting `clearBack()` failures propagate. |
| Regression coverage | PASS | `clearAll - Redis clear 실패를 호출자에게 전파한다` covers the hidden failure path. |
| Data safety | PASS | The regression proves the backend key remains when clear fails, so callers can retry or handle partial clear. |
| Compatibility | PASS | Successful clear behavior and cache-name isolation tests remain in the same test class. |
| Simplicity | PASS | Production change removes ignored `runCatching`; no new abstraction or dependency. |
| Concurrency | PASS | No concurrent behavior changed; existing `MultithreadingTester` and `StructuredTaskScopeTester` tests remain untouched. |
| Evidence | PASS | Full affected module test passed locally. CodeGraph impact analysis reported low risk / no additional impacted files. |

## 검증 Evidence

- `./gradlew :bluetape4k-cache-lettuce:test --tests 'io.bluetape4k.cache.nearcache.LettuceNearCacheTest.clearAll - Redis clear 실패를 호출자에게 전파한다' --no-configuration-cache --rerun-tasks`
  - Result: PASS, 1 passing, `BUILD SUCCESSFUL in 27s`.
- `./gradlew :bluetape4k-cache-lettuce:test --max-workers=1 --no-configuration-cache`
  - Result: PASS, 436 passing, `BUILD SUCCESSFUL in 47s`.
- CodeGraph impact radius
  - Result: low risk, no additional impacted files.

## Remaining Risk

CI must rerun the affected module on GitHub Actions before merge.
