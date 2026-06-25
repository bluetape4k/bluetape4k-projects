# Review — Issue 822 R2DBC SQL Identifier Guards (2026-06-26)

**Scope**: `:bluetape4k-r2dbc`
**Issue**: #822
**Branch**: `fix/r2dbc-sql-identifier-guards`

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

- `InsertValuesSpecImpl` and `InsertValuesKeySpecImpl` now validate every field key with `requireValidIdentifier(...)` before storing it in the values map.
- `UpdateValuesSpecImpl.set(...)` validates every field key before storing it; `update(...)`, nullable setters, and `set(parameters)` flow through those setter paths.
- `QueryBuilder.whereGroup(...)` now normalizes and allow-lists group operators to `and` or `or`.
- Regression proof before the fix: 6 new tests failed with `Expected IllegalArgumentException but no exception was thrown`.
- Validation after the fix: `./gradlew :bluetape4k-r2dbc:cleanTest :bluetape4k-r2dbc:compileKotlin :bluetape4k-r2dbc:compileTestKotlin :bluetape4k-r2dbc:test --no-build-cache` passed with 171 tests.
- `git diff --check` passed.

## Notes

The change keeps raw `where(...)`, `select(...)`, `groupBy(...)`, `having(...)`, and `orderBy(...)` fragments unchanged because those APIs intentionally accept SQL fragments. The hardened paths are only the fluent DSL values that become SQL identifiers or boolean operators before parameter binding can protect values.
