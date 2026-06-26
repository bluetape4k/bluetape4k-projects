# Review - Issue 817 JDBC Transaction State (2026-06-26)

**Scope**: `:bluetape4k-jdbc`
**Issue**: #817
**Branch**: `fix/jdbc-transaction-state-rollback`

## Tiers

- Tier 1 Security: PASS
- Tier 4 Code correctness: PASS
- Tier 5 Tests: PASS
- Tier 7 Evidence: PASS

## Findings

- P0: 0
- P1: 0
- P2/P3: 0

## Evidence

- `Connection.withTransaction` now captures and restores `autoCommit`, isolation, and read-only state.
- Transaction state restoration runs each property independently, so one restore failure does not skip the remaining properties.
- Non-`Exception` `Throwable` failures now trigger rollback and are rethrown unchanged.
- Rollback failures and restore failures after a primary failure are attached as suppressed exceptions.
- Restore failure after a successful block and commit is surfaced instead of returning success.
- `withReadOnlyTransaction` no longer resets read-only state to `false`; it relies on `withTransaction` to restore the caller's original state.
- Regression proof before the fix: targeted `TransactionExtensionsTest` failed for read-only restoration, non-`Exception` rollback, suppressed restore failure, and success-path restore failure.
- Validation after the fix: `./gradlew :bluetape4k-jdbc:cleanTest :bluetape4k-jdbc:compileKotlin :bluetape4k-jdbc:compileTestKotlin :bluetape4k-jdbc:test --no-build-cache` passed with 126 tests and no Kotlin warnings.
- `git diff --check` passed.

## Notes

The tests use a small `Connection` proxy instead of a driver-specific fake so
restore failures and `Error` paths are deterministic without introducing a new
test dependency.
