---
title: Transactions and state restoration
description: Define commit, rollback, connection-state restoration, and suppressed-exception behavior.
manualId: bluetape4k-jdbc
chapterId: transactions
---

# Transactions and state restoration

## A transaction borrows connection state

A pooled `Connection` can be reused by the next request. A transaction helper must handle more than commit and rollback: it must also restore the previous `autoCommit`, isolation level, and read-only state. `withTransaction` records those three values, executes the transaction, and restores each value independently.

```kotlin
import io.bluetape4k.jdbc.sql.executeUpdate
import io.bluetape4k.jdbc.sql.withTransaction
import javax.sql.DataSource

fun renameAccount(
    dataSource: DataSource,
    id: Long,
    newName: String,
) {
    dataSource.withTransaction { connection ->
        connection.executeUpdate(
            "UPDATE accounts SET display_name = ? WHERE id = ?",
            newName,
            id,
        )
        connection.executeUpdate(
            "INSERT INTO account_audit(account_id, action) VALUES (?, ?)",
            id,
            "RENAME",
        )
    }
}
```

The result is returned only when both the block and commit succeed. If either throws a `Throwable`, the helper attempts rollback and rethrows the original throwable.

## State transitions

| Point in time | `autoCommit` | Isolation | Read-only |
| --- | --- | --- | --- |
| Before entry | Current connection value | Current connection value | Current connection value |
| Transaction block | `false` | Requested value, `READ_COMMITTED` by default | Existing value, or `true` when using the read-only helper |
| Successful exit | Restored to original value | Restored to original value | Restored to original value |
| Failed exit | Restoration attempted after rollback | Restoration attempted independently | Restoration attempted independently |

If one restoration fails, the helper still attempts the others. This preserves as many opportunities as possible to keep contaminated state out of the pool.

## Exception precedence

The block failure is the primary cause. If rollback or restoration also fails, those failures are attached to the original throwable as `suppressed` exceptions. If the work and commit succeed but only restoration fails, the restoration exception is delivered to the caller. Treating that path as success could return a connection with incorrect state to the pool.

```kotlin
try {
    dataSource.withTransaction { connection ->
        // work
    }
} catch (failure: Throwable) {
    logger.error("JDBC transaction failed", failure)
    failure.suppressed.forEach { suppressed ->
        logger.warn("Rollback or state restoration also failed", suppressed)
    }
    throw failure
}
```

Keep logging responsibility at the top-level operation boundary so that every layer does not record the same failure.

## Read-only and temporary-state helpers

`withReadOnlyTransaction` sets `isReadOnly = true` inside the transaction and restores the value present at entry. A database or driver may interpret read-only as an optimization hint or a write restriction, but it is not an identical security boundary across all databases.

`withIsolationLevel`, `withAutoCommit`, `withReadOnly`, and `withHoldability` change one property for the duration of a block and restore it in `finally`. They do not commit or roll back. Use `withTransaction` when the operation needs a transaction boundary.

In version 1.12.1, if both the block and restoration fail in one of these single-property helpers, the restoration exception may mask the original exception. Use `withTransaction` for transactional work that needs rollback and suppressed-exception preservation, and test restoration failure as a separate failure mode on paths that use a single-property helper.

## Keep one connection inside a DataSource transaction

`DataSource.withTransaction` borrows one connection and passes it to `Connection.withTransaction`. Execute all SQL through the block parameter.

```kotlin
dataSource.withTransaction { connection ->
    connection.executeUpdate(firstSql, firstValue)
    connection.executeUpdate(secondSql, secondValue)
}
```

Calling `dataSource.executeUpdate(...)` again inside the block can borrow another connection, which is not guaranteed to participate in the current transaction.

## Framework transaction boundaries

When a Spring transaction manager or Exposed transaction already owns the boundary, follow that framework's connection binding and propagation rules. Nesting this helper does not combine two transaction models. Prefer one transaction owner for one database operation.

## Sources and tests

- [`TransactionExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/TransactionExtensions.kt)
- [`DataSourceTransactionExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceTransactionExtensions.kt)
- [`TransactionExtensionsTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/TransactionExtensionsTest.kt)
- [`DataSourceTransactionExtensionsTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/DataSourceTransactionExtensionsTest.kt)

## Next chapter

After understanding the low-level JDBC boundary, continue with [Choosing what comes after JDBC](./ecosystem-paths.md) to decide whether to keep it or move to Exposed or Hibernate.
