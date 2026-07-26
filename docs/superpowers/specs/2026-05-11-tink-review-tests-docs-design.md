# io/tink Review, Tests, and Docs Design

## Scope

- Module: `io/tink`
- Work type: strict 6-Tier code review, edge/concurrency test hardening, public KDoc and README update.
- Verification mode: local compile/test plus GitHub CI after PR creation.

## Findings

| Priority | Finding                                                                                                        | Evidence                                                                                                                                                                                                    | Required Action                                                                                                        |
|----------|----------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| P0       | None                                                                                                           | No immediate release-blocking data-loss path found in current scope.                                                                                                                                        | N/A                                                                                                                    |
| P1       | `LettuceVersionedKeysetStore` releases its Redis lock with separate `GET` and `DEL` commands.                  | `withLock` checks `commands.get(lockKey) == token` and then calls `commands.del(lockKey)`. If the lock expires between those commands and another owner acquires it, the old owner can delete the new lock. | Release the lock with a single compare-and-delete Lua script and add Redis edge tests.                                 |
| P1       | Redis `rotateIfDue` implementations can rotate more than once under concurrent due checks.                     | Both Lettuce and Redisson implementations read `current()` outside the rotation lock and call `rotate()` when elapsed. Multiple callers that observe the same stale keyset can queue multiple rotations.    | Recheck the active keyset inside the rotation lock before creating a new version; add `MultithreadingTester` coverage. |
| P2       | Keyset JSON helpers intentionally serialize cleartext key material but the risk is easy to miss at call sites. | `TinkKeysetJsonSupport` uses `InsecureSecretKeyAccess`; README only briefly warns during a manual serialization example.                                                                                    | Strengthen KDoc and README warnings around cleartext keyset JSON and Redis store protection requirements.              |
| P2       | Digest string verification throws on malformed Base64 despite returning `Boolean`.                             | `TinkDigester.matches(data: String, expected: String)` calls `Base64.getDecoder().decode(expected)` directly.                                                                                               | Return `false` for malformed Base64 and add an edge test.                                                              |

## Acceptance Criteria

- P0/P1 gate closes with zero remaining P0/P1 findings.
- Lettuce lock release is token-checked atomically.
- Concurrent due rotation produces a single new active keyset version per due window.
- New tests use `MultithreadingTester` for thread-safety coverage.
- Public KDoc and README.md/README.ko.md document cleartext keyset risks and operational guidance.

## Step Checklist Completion Report

| Item                          | Status | Notes                                                                          |
|-------------------------------|--------|--------------------------------------------------------------------------------|
| Worktree created              | Done   | `.worktrees/tink-review-tests-docs` from `origin/develop`                      |
| Current repo evidence checked | Done   | Source, tests, README, build file inspected                                    |
| Official docs checked         | Done   | Google Tink AEAD, Deterministic AEAD, keyset/key management docs checked       |
| User intent clear             | Done   | Apply same strict review/test/docs/PR workflow to `io/tink`, then next modules |
| Review-only boundary          | N/A    | User asked to perform the work                                                 |
