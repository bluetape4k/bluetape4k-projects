# JDBC state restore failures must not replace primary failures

## Context

Issue #946 found JDBC helper functions that restored connection state in
`finally` without preserving a primary block failure when restore also failed.

## Decision

Record the primary failure from the caller block or state-change path, then add
restore failures as suppressed exceptions. If the caller block succeeds and only
restore fails, surface the restore failure.

## Verification

- `./gradlew :bluetape4k-jdbc:test --tests 'io.bluetape4k.jdbc.sql.TransactionExtensionsTest'`
- `git diff --check`

## Future guidance

Any lifecycle helper that temporarily mutates JDBC connection state should use
the same primary-failure preservation pattern as transaction rollback/restore
logic.
