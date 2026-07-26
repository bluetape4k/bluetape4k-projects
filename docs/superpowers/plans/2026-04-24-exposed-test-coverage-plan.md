# Exposed JDBC/R2DBC Test Coverage Improvement Plan

- **Date**: 2026-04-24
- **Modules**: `data/exposed-jdbc`, `data/exposed-r2dbc`
- **Base branch**: `develop`
- **Worktree**: `.worktrees/exposed-test-coverage`
- **Goal
  **: Close concrete test gaps for uncovered public extension APIs in both modules. Use "public function → test exists" as the coverage criterion (no JaCoCo/Kover).

## Context & Constraints

- **Research already done**: SoftDeleted test coverage is complete. The gaps below are the final verified list.
- **No coverage tooling**: JaCoCo/Kover not configured. Verification is manual source-file-to-test mapping.
- **Logging**: `KLogging()` in blocking tests; `KLoggingChannel()` in suspend tests. `companion object` required.
- **JDBC test pattern**: `withTables(testDB) { ... }` +
  `@ParameterizedTest @MethodSource(ENABLE_DIALECTS_METHOD)`, extend `AbstractExposedTest`.
- **R2DBC test pattern**: `runSuspendIO { withTables(testDB, ...) { ... } }` +
  `@ParameterizedTest @MethodSource(ENABLE_DIALECTS_METHOD)`, extend `AbstractExposedR2dbcTest`.
- **Detekt applies** to production modules (`exposed-jdbc`,
  `exposed-r2dbc`) — keep functions small, no magic numbers, proper nullability.
- **After every `.kt` edit**: run `ide_diagnostics`, fix imports via `ide_optimize_imports`, resolve `@Deprecated` via
  `lsp_code_actions`.
- **README policy**: Coverage tests do NOT require README updates (no public API change).
- **Worktree-first**: all code changes inside `.worktrees/exposed-test-coverage`.

## Task List

### T1 — Worktree setup & baseline discovery

- **complexity: low**
- Create worktree: `git worktree add .worktrees/exposed-test-coverage -b feat/exposed-test-coverage`.
- Confirm baseline: both modules currently build and tests pass on `develop`.
- Record baseline test file list for `data/exposed-jdbc/src/test/kotlin/io/bluetape4k/exposed/jdbc/` and
  `data/exposed-r2dbc/src/test/kotlin/io/bluetape4k/exposed/r2dbc/`.
- Exit: worktree ready, baseline snapshot captured in plan progress log.

### T2 — JDBC: Locate & study `SchemaUtilsExtensions`

- **complexity: low**
- Read `data/exposed-jdbc/src/main/kotlin/io/bluetape4k/exposed/jdbc/SchemaUtilsExtensions.kt`.
- Identify signature: `fun JdbcTransaction.execCreateMissingTablesAndColumns(vararg tables: Table)`.
- Identify reference `AbstractExposedTest` imports and an exemplar sibling test for style alignment.
- Exit: clear picture of function semantics + the nearest-neighbour style reference file path recorded.

### T3 — JDBC: Implement `SchemaUtilsExtensionsTest`

- **complexity: medium**
- Create `data/exposed-jdbc/src/test/kotlin/io/bluetape4k/exposed/jdbc/SchemaUtilsExtensionsTest.kt`.
- Test cases:
    1.
  `execCreateMissingTablesAndColumns creates missing table` — start with empty schema, call, verify table exists (insert + select a row).
    2. `execCreateMissingTablesAndColumns is idempotent` — call twice, no exception, row still insertable.
    3.
  `execCreateMissingTablesAndColumns adds missing column` — create table with subset of columns (v1 table), then call with extended table (v2), verify new column usable.
    4. `execCreateMissingTablesAndColumns with multiple tables` — vararg form covers >1 table in a single call.
- Patterns: `AbstractExposedTest` + `@ParameterizedTest @MethodSource(ENABLE_DIALECTS_METHOD)` +
  `withTables(testDB) { }` + `companion object: KLogging()`.
- Run `ide_diagnostics` after save; fix imports; resolve any Detekt/Deprecated warnings.
- Exit: 4 tests green for all enabled dialects; Detekt clean.

### T4 — R2DBC: Locate & study `TableExtensions` suspend APIs

- **complexity: low**
- Read `data/exposed-r2dbc/src/main/kotlin/io/bluetape4k/exposed/r2dbc/TableExtensions.kt` (or equivalent location).
- Confirm signatures for `suspendColumnMetadata`, `suspendIndexes`, `suspendPrimaryKeyMetadata`, `suspendSequences`.
- Determine dialect support boundary for `suspendSequences` (PostgreSQL only — guard via
  `testDB == TestDB.POSTGRESQL` check or assume/skip).
- Exit: full signature + dialect matrix recorded.

### T5 — R2DBC: Implement `TableExtensionsTest`

- **complexity: medium**
- Create `data/exposed-r2dbc/src/test/kotlin/io/bluetape4k/exposed/r2dbc/TableExtensionsTest.kt`.
- Test cases:
    1.
  `suspendColumnMetadata returns all columns of created table` — columns count + column names match table definition.
    2. `suspendIndexes returns declared indexes` — define unique + non-unique index, assert presence.
    3. `suspendPrimaryKeyMetadata returns PK metadata for PK table` — verify PK column set.
    4. `suspendPrimaryKeyMetadata returns null for table without PK` — table without primary key.
    5. `suspendSequences returns sequences (PostgreSQL only)` —
       `Assumptions.assumeTrue(testDB == TestDB.POSTGRESQL)` guard; declare sequence, verify listed.
- Patterns: `AbstractExposedR2dbcTest` + `runSuspendIO { withTables(testDB, ...) { } }` +
  `@ParameterizedTest @MethodSource(ENABLE_DIALECTS_METHOD)` + `companion object: KLoggingChannel()`.
- Run `ide_diagnostics` after save.
- Exit: 5 tests green (PG-only case skipped on H2/others); Detekt clean.

### T6 — R2DBC: Extend `QueryExtensionsTest` with DB-backed forEach / forEachIndexed

- **complexity: medium**
- Read existing `data/exposed-r2dbc/src/test/kotlin/io/bluetape4k/exposed/r2dbc/QueryExtensionsTest.kt`.
- Add a new `@Nested` class `DatabaseBacked` (or new parameterized test methods) that uses
  `runSuspendIO { withTables(testDB, ...) { } }`.
- Test cases:
    1. `Query_forEach iterates all rows` — insert N rows, collect via
       `forEach`, assert accumulated list equals inserted rows.
    2. `Query_forEach with empty result does not invoke block` — empty table, counter stays 0.
    3.
  `Query_forEachIndexed provides sequential indexes starting at 0` — insert N rows, collect (index, row) pairs, verify indexes [0..N-1].
    4. `Query_forEachIndexed with empty result does not invoke block`.
- Use a minimal local test table (Int id + String value); keep inside the nested class.
- Exit: 4 new tests green on all enabled dialects; existing Flow-only tests still pass.

### T7 — R2DBC: Extend `ReadableExtensionsTest` with UUID / Blob getters

- **complexity: medium**
- Read existing `data/exposed-r2dbc/src/test/kotlin/io/bluetape4k/exposed/r2dbc/ReadableExtensionsTest.kt`.
- Add test cases:
    1. `getUuidOrNull by index returns value when present` & `returns null when column is null`.
    2. `getUuidOrNull by label returns value when present` & `returns null when column is null`.
    3. `getExposedBlob by index returns blob bytes`.
    4. `getExposedBlobOrNull by index returns null when column is null` & `returns blob when present`.
    5. `getExposedBlobOrNull by label returns null when column is null`.
- Use a dedicated test table with nullable UUID + nullable Blob columns.
- Ensure pattern matches existing file (same base class, same logger style).
- Exit: 5+ new tests green; detekt clean.

### T8 — Run full module test suites

- **complexity: low**
- `./gradlew :bluetape4k-exposed-jdbc:test` — all green (existing + new).
- `./gradlew :bluetape4k-exposed-r2dbc:test` — all green (existing + new).
- Use `./bin/repo-test-summary --` wrapper for condensed output.
- Capture PASS / SKIP / FAIL counts per module for the PR description.
- Exit: both modules 100% pass; failure triage done (if any).

### T9 — Detekt verification

- **complexity: low**
- `./gradlew :bluetape4k-exposed-jdbc:detekt :bluetape4k-exposed-r2dbc:detekt`.
- Fix any violations in new files (magic numbers → named constants, function size, TooManyFunctions if applicable).
- Exit: Detekt passes for both modules.

### T10 — Coverage mapping document & progress log

- **complexity: low**
- Update this plan file's "Progress" section with a table mapping each targeted public function → test file → test method names.
- Record final test counts (added / total) per module.
- Add testlog entry to `docs/testlogs/2026-04.md` (top row): bugfix/test execution history with date, scope, result.
- Exit: mapping table completed; testlog updated.

### T11 — Commit & PR

- **complexity: low**
- Commit in worktree with Korean prefix: `test: exposed-jdbc/r2dbc 테스트 커버리지 개선`.
- Push branch; open PR via `gh pr create` with:
    - Summary of gaps closed.
    - Per-module test counts (before/after).
    - Coverage mapping table.
    - Verification commands.
- Run `/oh-my-claudecode:code-reviewer` before merge.
- After merge: `git worktree remove .worktrees/exposed-test-coverage` + `./bin/clean-branches`.
- Exit: PR merged, worktree removed.

## Complexity Summary

| Task                                                                      | Complexity |
|---------------------------------------------------------------------------|------------|
| T1 — Worktree setup & baseline discovery                                  | low        |
| T2 — JDBC: Locate & study SchemaUtilsExtensions                           | low        |
| T3 — JDBC: Implement SchemaUtilsExtensionsTest                            | medium     |
| T4 — R2DBC: Locate & study TableExtensions suspend APIs                   | low        |
| T5 — R2DBC: Implement TableExtensionsTest                                 | medium     |
| T6 — R2DBC: Extend QueryExtensionsTest (DB-backed forEach/forEachIndexed) | medium     |
| T7 — R2DBC: Extend ReadableExtensionsTest (UUID/Blob getters)             | medium     |
| T8 — Run full module test suites                                          | low        |
| T9 — Detekt verification                                                  | low        |
| T10 — Coverage mapping document & progress log                            | low        |
| T11 — Commit & PR                                                         | low        |

## Risks & Mitigations

- **R2DBC PostgreSQL-only APIs** → Guard with `Assumptions.assumeTrue(...)`; do not force PG on H2.
- **Schema state leakage between tests** → Rely on `withTables` cleanup; avoid global schema mutations.
- **Blob comparison semantics** → Compare via `bytes.contentEquals(...)`, not `==`.
- **`JdbcTransaction` receiver API drift
  ** → If the extension signature changed, adjust test call-site; do not modify production signature.
- **Detekt on test files for production module** → Keep helpers/tables small; hoist magic numbers.

## Progress (to be filled during execution)

- [ ] T1 — Worktree setup
- [ ] T2 — JDBC study
- [ ] T3 — JDBC SchemaUtilsExtensionsTest
- [ ] T4 — R2DBC study
- [ ] T5 — R2DBC TableExtensionsTest
- [ ] T6 — R2DBC QueryExtensionsTest extension
- [ ] T7 — R2DBC ReadableExtensionsTest extension
- [ ] T8 — Full suites green
- [ ] T9 — Detekt clean
- [ ] T10 — Coverage mapping + testlog
- [ ] T11 — PR merged + worktree removed

### Coverage Mapping (fill at T10)

| Module | Public function                                              | Test file                                 | Test method(s) |
|--------|--------------------------------------------------------------|-------------------------------------------|----------------|
| jdbc   | `JdbcTransaction.execCreateMissingTablesAndColumns`          | `SchemaUtilsExtensionsTest.kt`            | _tbd_          |
| r2dbc  | `Table.suspendColumnMetadata`                                | `TableExtensionsTest.kt`                  | _tbd_          |
| r2dbc  | `Table.suspendIndexes`                                       | `TableExtensionsTest.kt`                  | _tbd_          |
| r2dbc  | `Table.suspendPrimaryKeyMetadata`                            | `TableExtensionsTest.kt`                  | _tbd_          |
| r2dbc  | `Table.suspendSequences` (PG only)                           | `TableExtensionsTest.kt`                  | _tbd_          |
| r2dbc  | `Query.forEach`                                              | `QueryExtensionsTest.kt` (DatabaseBacked) | _tbd_          |
| r2dbc  | `Query.forEachIndexed`                                       | `QueryExtensionsTest.kt` (DatabaseBacked) | _tbd_          |
| r2dbc  | `getUuidOrNull(Int)` / `getUuidOrNull(String)`               | `ReadableExtensionsTest.kt`               | _tbd_          |
| r2dbc  | `getExposedBlob(Int)`                                        | `ReadableExtensionsTest.kt`               | _tbd_          |
| r2dbc  | `getExposedBlobOrNull(Int)` / `getExposedBlobOrNull(String)` | `ReadableExtensionsTest.kt`               | _tbd_          |
