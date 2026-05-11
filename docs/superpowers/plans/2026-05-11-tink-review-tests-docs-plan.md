# io/tink Review, Tests, and Docs Plan

## Plan

1. Fix P1 Redis lock and rotation concurrency behavior.
   - Replace Lettuce `GET` + `DEL` lock release with compare-and-delete Lua.
   - Recheck due rotation inside Redis locks for Lettuce and Redisson.
2. Add edge/concurrency tests.
   - Test Lettuce Lua release behavior for matching and stale tokens.
   - Add `MultithreadingTester` coverage for concurrent `rotateIfDue`.
   - Add malformed Base64 digest match edge test.
3. Update public docs.
   - Korean KDoc for keyset JSON cleartext risk and Redis keyset store lifecycle.
   - README.md and README.ko.md with Tink advantages, recommended scenarios, anti-patterns, and key-management warnings.
4. Run strict gate verification.
   - `git diff --check`
   - `./gradlew :bluetape4k-tink:compileTestKotlin --no-build-cache`
   - targeted and full module tests where feasible.
   - GitHub CI after PR creation.

## 6-Tier Gate

| Tier | Gate | Result Before Patch | Exit Requirement |
|---|---|---|---|
| 1 | Public API contract | No required signature removals; keyset risk docs too light. | Public contract documented without source-breaking removal. |
| 2 | Correctness / edge cases | P2 malformed digest Base64 edge found. | Malformed expected digest returns `false`. |
| 3 | Thread / concurrency safety | P1 Redis lock release and `rotateIfDue` race found. | Atomic release and lock-internal due recheck verified. |
| 4 | Security / key lifecycle | Cleartext keyset JSON is intentional but risky. | Strong docs and warnings added; no accidental plaintext claims. |
| 5 | Documentation / examples | README lacks practical Tink usage/anti-pattern guidance. | README.md and README.ko.md synchronized. |
| 6 | Verification / maintainability | Existing tests cover happy paths, but not lock race gates. | Targeted tests and module tests recorded. |

## P0/P1 Gate

| Priority | Finding | Status | Validation |
|---|---|---|---|
| P0 | None | Closed | No P0 found in current scope |
| P1 | Lettuce lock release uses non-atomic `GET` + `DEL` | Closed | Lua compare-and-delete helper; Redis token-match/stale-token tests passed |
| P1 | Redis `rotateIfDue` can rotate multiple times under concurrent due checks | Closed | Lock-internal active recheck; Lettuce/Redisson `MultithreadingTester` tests passed |

Latest integrated findings after local and advisor review: P0 = 0, P1 = 0.

## Advisor Review

Claude Code Opus advisor is required by the bluetape4k design workflow when available.

Artifact: `.omx/artifacts/claude-tink-review-20260511.md`

| Priority | Finding | Decision | Follow-up |
|---|---|---|---|
| P0/P1 | No remaining P0/P1 findings | Gate closed | Advisor reported `P0=0 P1=0` |
| P2 | `require(acquired)` is semantically weaker for operational lock acquisition failure | Accepted | Changed to `check(acquired)` |
| P2 | `MutableClock.current` should document cross-thread visibility | Accepted | Added `@Volatile` |
| P2 | Redis `persist()` write order deserves intent comment | Accepted | Added comment that active version is updated last |

## External Evidence

- Google Tink docs: AEAD is the recommended primitive for most data encryption; associated data is authenticated but not encrypted.
- Google Tink Deterministic AEAD docs: deterministic encryption reveals repeated plaintext equality and should be used only when determinism is required.
- Google Tink keyset/key management docs: keysets enable rotation and key material must be protected when persisted.

## Verification Evidence

| Command | Result |
|---|---|
| `./gradlew :bluetape4k-tink:compileTestKotlin --no-build-cache` | Passed |
| `./gradlew :bluetape4k-tink:test --tests "io.bluetape4k.tink.digest.TinkDigesterTest" --tests "io.bluetape4k.tink.keyset.redis.LettuceVersionedKeysetStoreTest" --tests "io.bluetape4k.tink.keyset.redis.RedissonVersionedKeysetStoreTest" --no-build-cache` | Passed, 25 tests |
| `./gradlew :bluetape4k-tink:test --no-build-cache` | Passed, 139 tests |
| `git diff --check` | Passed |
