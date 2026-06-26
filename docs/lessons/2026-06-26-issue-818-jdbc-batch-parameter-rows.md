# Lessons Learned - JDBC Batch Parameter Rows (2026-06-26)

**Issue**: #818
**Module**: `:bluetape4k-jdbc`

## L1: Batch row shape must be validated before binding

### Problem

JDBC `PreparedStatement` keeps parameter state across batch additions. When a
later row had fewer values than an earlier row, the shorter row could reuse a
stale value and insert corrupted data instead of failing.

### Lesson

List-based batch helpers must validate parameter row shape before preparing or
executing a statement, then clear statement parameters before binding each row.
Do not rely on the driver to catch short rows after previous values have already
been bound.

### Future Guard

When changing JDBC batch helpers, include a regression with a later row that has
fewer parameters and assert both fail-fast behavior and zero persisted rows for
the attempted batch.
