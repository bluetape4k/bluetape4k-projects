---
title: Prepared statements and batches
description: Bind parameters, read generated keys, and enforce batch-row boundaries.
manualId: bluetape4k-jdbc
chapterId: statements-batches
---

# Prepared statements and batches

## Separate values from SQL structure

Do not interpolate external values into SQL. Parameter overloads of `Connection.executeQuery` and `executeUpdate` create a prepared statement and bind each value with `setObject` at a one-based JDBC parameter index.

```kotlin
fun findDisplayName(dataSource: DataSource, accountId: Long): String? =
    dataSource.executeQuery(
        "SELECT display_name FROM accounts WHERE id = ?",
        accountId,
    ) { rs -> rs.mapFirst { it.getString("display_name") } }
```

Identifiers and `ORDER BY` direction cannot be bound as values. Choose those fragments from an allowlist instead of inserting user input into identifier positions.

## Generated keys

Use `executeUpdateWithGeneratedKeys` when an insert must return a generated key. Extract the value while the generated-keys `ResultSet` is open.

```kotlin
val accountId: Long? = dataSource.executeUpdateWithGeneratedKeys(
    "INSERT INTO accounts(display_name) VALUES (?)",
    "Ada",
) { keys ->
    if (keys.next()) keys.getLong(1) else null
}
```

Generated-key support and returned columns vary by driver. Verify the production driver, and turn a missing required key into an explicit service failure.

## Direct binding

Use `preparedStatement` when a vendor-specific setter, stream, or LOB needs more control than `setObject`.

```kotlin
dataSource.withConnect { connection ->
    connection.preparedStatement(
        "SELECT id FROM accounts WHERE status = ? AND created_at >= ?",
    ) { statement ->
        statement.setString(1, "ACTIVE")
        statement.setTimestamp(2, cutoff)
        statement.executeQuery().use { rs ->
            rs.toList { it.getLong("id") }
        }
    }
}
```

The block closes the prepared statement; the outer `withConnect` closes the connection.

## Batch-row contract

`executeBatch(sql, paramsList, batchSize)` divides parameter rows into JDBC batches. Version 1.12.1 checks that every row has the same parameter count before preparing the statement, preventing a shorter row from reusing bindings from the previous row.

```kotlin
val rows = listOf(
    listOf("Ada", "ACTIVE"),
    listOf("Grace", "ACTIVE"),
    listOf("Linus", "INACTIVE"),
)

val results: List<IntArray> = dataSource.executeBatch(
    "INSERT INTO accounts(display_name, status) VALUES (?, ?)",
    rows,
    batchSize = 500,
)
```

The check does not prove that row width matches the SQL placeholder count; the driver validates that during execution. Version 1.12.1 also does not validate that `batchSize` is positive. Reject zero and negative values when loading application configuration.

`executeLargeBatch` combines results into a `LongArray`. `executeBatch` returns one `IntArray` for each executed chunk, which is useful when observing or diagnosing chunk boundaries.

## Use one transaction connection

When multiple batches form one business operation, execute them on the connection passed to `withTransaction`. Repeated `DataSource.executeBatch` calls may borrow separate connections.

```kotlin
dataSource.withTransaction { connection ->
    connection.executeBatch(accountSql, accountRows, batchSize = 500)
    connection.executeBatch(auditSql, auditRows, batchSize = 500)
}
```

Choose batch size using memory, driver buffers, network packet limits, and lock duration. Measure with production-like payloads instead of assuming that larger batches are always faster.

## Sources and tests

- [`PreparedStatementExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementExtensions.kt)
- [`PrepareStatementSupport.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PrepareStatementSupport.kt)
- [`PreparedStatementArgumentSetter.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementArgumentSetter.kt)
- [`DataSourceTransactionExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceTransactionExtensions.kt)
- [`PreparedStatementExtensionsTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementExtensionsTest.kt)
- [`DataSourceTransactionExtensionsTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/DataSourceTransactionExtensionsTest.kt)

## Next chapter

Continue with [Reading and mapping ResultSet](./resultset-mapping.md) to choose cursor-consumption and return-value contracts.
