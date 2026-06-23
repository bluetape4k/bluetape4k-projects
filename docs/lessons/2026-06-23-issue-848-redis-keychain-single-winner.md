# Issue #848 Redis Key-Chain Single-Winner Rotation

Issue #848 found that `RedisKeyChainRepository.rotate()` trusted each
process-local cached current keychain. Two application nodes could cache the
same expired current keychain and then both insert different replacement keys
into Redis.

## Decision

Serialize Redis-backed rotation with a Redisson lock derived from the keychain
queue name. While holding the lock, reload the current keychain from Redis and
update the local cache before deciding whether to rotate. A node that loses the
race sees the winner's non-expired keychain and immediately converges its cache
to that key.

## Lessons

- Distributed repositories cannot make rotation decisions from a per-process
  cache. The lock-protected section must re-read the shared state.
- A stale-cache race can be reproduced deterministically without thread timing:
  two repository instances cache the same expired key, then rotate sequentially.
- Forced rotation should use the same Redis lock and capacity trimming path so
  regular and forced writes cannot interleave around deque maintenance.

## Verification

- RED: `./gradlew :bluetape4k-jwt:test --tests "io.bluetape4k.jwt.keychain.redis.RedisKeyChainRepositoryTest.expired cached keychain rotates with single Redis winner"` failed with `Expected <2> to equal to <1>`.
- GREEN targeted: the same Redis-backed test passed with 1 test.
- Module: `./gradlew :bluetape4k-jwt:test` passed with 149 tests and 10 pending.
- Build: `./gradlew :bluetape4k-jwt:build` passed.
- Hygiene: `git diff --check` passed.
- Static analysis: `./gradlew detekt` passed with `:detekt NO-SOURCE`.
