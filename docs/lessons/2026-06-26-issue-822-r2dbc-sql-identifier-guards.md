# Lessons Learned — R2DBC SQL Identifier Guards (2026-06-26)

**Issue**: #822
**Module**: `:bluetape4k-r2dbc`

## L1: Helper coverage is not enough for public DSL safety

### Problem

`requireValidIdentifier(...)` already existed and had unit tests, but the public insert/update builders stored field names before calling that helper. `QueryBuilder.whereGroup(...)` also accepted arbitrary non-blank operators and interpolated them between conditions.

### Lesson

When a SQL DSL has a validation helper, test the public builder paths that interpolate identifiers or operators, not only the helper itself. Helper-level tests prove the predicate, but public-path tests prove the guard is actually wired into the DSL.

### Future Guard

For R2DBC SQL DSL changes, add direct regression tests against the fluent API entrypoint whenever a string argument later becomes SQL syntax instead of a bound value.
