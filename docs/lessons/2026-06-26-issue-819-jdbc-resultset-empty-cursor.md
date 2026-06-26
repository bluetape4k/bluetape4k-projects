# Lessons Learned — JDBC ResultSet Empty Cursor Contract (2026-06-26)

**Issue**: #819
**Module**: `:bluetape4k-jdbc`

## L1: JDBC cursor predicates are consumption APIs

### Problem

`ResultSet.isEmpty()` and `ResultSet.isNotEmpty()` looked like simple
predicates, but both advanced the JDBC cursor by calling `next()`. A caller that
checked `isNotEmpty()` and then used `toList` on the same `ResultSet` would miss
the first row.

### Lesson

Any helper that calls `ResultSet.next()` must make cursor movement explicit in
its name, KDoc, tests, and README examples. Forward-only cursors cannot be
treated like reusable collections.

### Future Guard

When reviewing ResultSet helpers, add tests that inspect the cursor position
after the helper call, not only the returned boolean or mapped value.
