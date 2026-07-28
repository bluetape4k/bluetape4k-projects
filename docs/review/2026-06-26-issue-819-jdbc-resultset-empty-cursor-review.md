# Review — Issue 819 JDBC ResultSet Empty Cursor Contract (2026-06-26)

**Scope**: `:bluetape4k-jdbc`
**Issue**: #819
**Branch**: `fix/jdbc-resultset-empty-cursor-contract`

## Tiers

- Tier 1 Security: PASS
- Tier 4 Code correctness: PASS
- Tier 5 Tests: PASS
- Tier 6 Documentation: PASS
- Tier 7 Evidence: PASS

## 발견 사항

- P0: 0
- P1: 0
- P2/P3: 0

## 증거

- `ResultSet.isEmptyByMovingCursor()` and `ResultSet.isNotEmptyByMovingCursor()` now make the destructive cursor movement part of the API name.
- Existing `ResultSet.isEmpty()` and `ResultSet.isNotEmpty()` remain binary-compatible shims but are deprecated with replacement guidance.
- KDoc now states that the helpers call `ResultSet.next()` exactly once and leave non-empty results positioned on the first row.
- English and Korean JDBC README examples now use the explicit cursor-moving helper names and document the first-row position.
- Regression proof before the fix: targeted `:bluetape4k-jdbc:test` failed in `compileTestKotlin` because the explicit cursor-moving helper names did not exist.
- Validation after the fix: `./gradlew :bluetape4k-jdbc:cleanTest :bluetape4k-jdbc:compileKotlin :bluetape4k-jdbc:compileTestKotlin :bluetape4k-jdbc:test --no-build-cache` passed with 119 tests.
- `git diff --check` passed.

## Notes

Forward-only JDBC result sets cannot provide a general safe peek without
buffering or changing the caller's consumption model. This fix keeps the old
behavior available but makes intentional cursor movement explicit at the API and
documentation boundary.
