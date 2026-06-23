# Issue #847 JwtReader Remaining TTL

Issue #847 found that `JwtReader.expiredTtl` was documented and used as a TTL
but returned the absolute JWT `exp` timestamp in epoch milliseconds.

## Decision

Split the two concepts explicitly. `expiresAtMillis` now exposes the absolute
expiration timestamp, `remainingTtlMillis` exposes the remaining TTL in
milliseconds, and the existing `expiredTtl` property follows the remaining TTL
contract for compatibility with the documented name. `isExpired` compares the
absolute expiration timestamp directly instead of reusing the TTL value.

## Lessons

- Public auth APIs should not overload one value as both a TTL and an absolute
  timestamp. Cache/session callers can retain authorization state for far too
  long when those units are confused.
- A one-hour token is an effective regression fixture: the remaining TTL must be
  near 3,600,000 milliseconds, while an epoch timestamp is orders of magnitude
  larger.
- README examples should show both the remaining TTL and absolute expiration
  accessors so consumers choose the correct value for cache TTLs and audit logs.

## Verification

- RED: `./gradlew :bluetape4k-jwt:test --tests "io.bluetape4k.jwt.reader.JwtReaderExpirationTest.expiredTtl - exp 클레임이 있으면 남은 TTL 밀리초를 반환한다" --no-build-cache` failed with `Expected <1782192683000> to be less than <3600001>`.
- GREEN targeted: `./gradlew :bluetape4k-jwt:test --tests "io.bluetape4k.jwt.reader.JwtReaderExpirationTest" --no-build-cache` passed.
- Module: `./gradlew :bluetape4k-jwt:test --no-build-cache` passed with 150 tests and 10 pending.
- Build: `./gradlew :bluetape4k-jwt:build --no-build-cache` passed.
- Hygiene: `git diff --check` passed.
- Static analysis: `./gradlew detekt` passed with `:detekt NO-SOURCE`.
