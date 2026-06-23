# Issue #847 JwtReader TTL Review

## Scope

- `utils/jwt/src/main/kotlin/io/bluetape4k/jwt/reader/JwtReader.kt`
- `utils/jwt/src/test/kotlin/io/bluetape4k/jwt/reader/JwtReaderExpirationTest.kt`
- `utils/jwt/README.md`
- `utils/jwt/README.ko.md`

## Verdict

Local 7-tier equivalent review: APPROVE.

P0/P1 findings: 0.

## Review Notes

| Lens | Finding | Severity | Resolution |
|---|---|---:|---|
| API correctness | `expiredTtl` returned `exp` epoch millis despite TTL documentation. | P1 | Added `expiresAtMillis` for the epoch value and changed `expiredTtl` to remaining TTL semantics. |
| Expiration logic | `isExpired` reused the mislabeled TTL value as an absolute timestamp. | P1 | `isExpired` now compares `expiresAtMillis` directly with the current clock. |
| Regression coverage | Existing tests locked the timestamp behavior. | P1 | Updated timestamp coverage to `expiresAtMillis` and added one-hour remaining TTL regression coverage. |
| Documentation | README examples encouraged assigning `expiredTtl` to a TTL variable without showing the absolute timestamp accessor. | P2 | README and README.ko now show `remainingTtlMillis` and `expiresAtMillis` separately. |
| Compatibility | Existing callers still have an `expiredTtl` property. | P2 | Kept `expiredTtl` as a remaining TTL alias rather than removing the property. |

## Verification

- RED: remaining-TTL regression failed against the epoch millis implementation.
- GREEN targeted: `JwtReaderExpirationTest` passed.
- Module: `./gradlew :bluetape4k-jwt:test --no-build-cache` passed with 150 tests and 10 pending.
- Build: `./gradlew :bluetape4k-jwt:build --no-build-cache` passed.
- Hygiene: `git diff --check` passed.
- Static analysis: `./gradlew detekt` passed with `:detekt NO-SOURCE`.
