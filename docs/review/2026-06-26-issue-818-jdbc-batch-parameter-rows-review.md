# Review - Issue 818 JDBC Batch Parameter Rows (2026-06-26)

**Scope**: `:bluetape4k-jdbc`
**Issue**: #818
**Branch**: `fix/jdbc-batch-parameter-row-validation`

## Tiers

- Tier 1 Security: PASS
- Tier 4 Code correctness: PASS
- Tier 5 Tests: PASS
- Tier 7 Evidence: PASS

## 발견 사항

- P0: 0
- P1: 0
- P2/P3: 0

## 증거

- `Connection.executeBatch` now validates that every parameter row has the same width before preparing a statement.
- `Connection.executeLargeBatch` applies the same validation path as `executeBatch`.
- Both batch helpers call `clearParameters()` before binding each row, so driver state from a previous row cannot be reused.
- Regression proof before the fix: `DataSource executeBatch - inconsistent parameter rows fail fast` and `DataSource executeLargeBatch - inconsistent parameter rows fail fast` failed because no `IllegalArgumentException` was thrown.
- Stale-binding proof before the fix: the failed `executeBatch` regression inserted corrupted `BatchMismatch%` rows and made the existing batch-count test fail with `Expected <7> to equal to <3>, but was not.`
- Validation after the fix: `./gradlew :bluetape4k-jdbc:test --tests 'io.bluetape4k.jdbc.sql.DataSourceTransactionExtensionsTest' --no-build-cache` passed with 13 tests.
- Module validation after the fix: `./gradlew :bluetape4k-jdbc:cleanTest :bluetape4k-jdbc:compileKotlin :bluetape4k-jdbc:compileTestKotlin :bluetape4k-jdbc:test --no-build-cache` passed with 121 tests.
- `git diff --check` passed.

## Notes

The validation intentionally checks row-to-row consistency instead of parsing SQL
placeholders. JDBC drivers still own SQL placeholder validation, while these
helpers prevent stale bound values from being carried into shorter rows.
