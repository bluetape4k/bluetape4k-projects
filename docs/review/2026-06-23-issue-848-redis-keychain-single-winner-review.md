# Issue #848 Redis Key-Chain Single-Winner Review

## Scope

- `utils/jwt/src/main/kotlin/io/bluetape4k/jwt/keychain/repository/redis/RedisKeyChainRepository.kt`
- `utils/jwt/src/test/kotlin/io/bluetape4k/jwt/keychain/redis/RedisKeyChainRepositoryTest.kt`

## Verdict

Local 7-tier equivalent review: APPROVE.

P0/P1 findings: 0.

## Review Notes

| Lens | Finding | Severity | Resolution |
|---|---|---:|---|
| Distributed correctness | `rotate()` made the expired-current decision from a process-local cache. | P1 | Rotation now acquires a Redis lock and reloads current from Redis before deciding. |
| Consistency | Losing nodes needed to converge immediately to the winning keychain. | P1 | The lock-protected reload updates `cachedCurrent` before returning false for a non-expired winner. |
| Capacity maintenance | Competing writes could interleave with deque trimming. | P1 | Regular and forced rotation share `changeCurrentAndTrim` under the same lock. |
| Regression coverage | Existing tests covered sequential single-repository behavior only. | P1 | Added a Redis-backed multi-repository stale-cache regression. |
| Compatibility | Existing repository contracts should remain unchanged. | P2 | Existing JWT module tests still pass. |

## Verification

- RED: single-winner Redis regression failed because both stale repositories rotated successfully.
- GREEN targeted: single-winner Redis regression passed.
- Module: `./gradlew :bluetape4k-jwt:test` passed with 149 tests and 10 pending.
- Build: `./gradlew :bluetape4k-jwt:build` passed.
- Hygiene: `git diff --check` passed.
- Static analysis: `./gradlew detekt` passed with `:detekt NO-SOURCE`.
