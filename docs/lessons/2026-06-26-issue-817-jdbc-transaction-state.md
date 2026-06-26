# Lessons Learned - JDBC Transaction State (2026-06-26)

**Issue**: #817
**Module**: `:bluetape4k-jdbc`

## L1: Transaction helpers must not hide restoration failures

### Problem

`withTransaction` restored state in one logged-and-swallowed block and caught
only `Exception`. That allowed non-`Exception` failures to skip rollback and
allowed restore failures to be reported as success. `withReadOnlyTransaction`
also reset read-only mode to `false` instead of restoring the caller's original
state.

### Lesson

Transaction helpers must treat rollback and state restoration as part of the
public contract. Capture all caller-owned connection flags, roll back for all
transaction-aborting `Throwable` paths, restore each state field independently,
and attach secondary failures as suppressed exceptions.

### Future Guard

When changing JDBC transaction helpers, include deterministic connection-proxy
tests for `autoCommit=false`, pre-existing read-only state, non-`Exception`
rollback, primary-failure suppression, and success-path restore failure.
