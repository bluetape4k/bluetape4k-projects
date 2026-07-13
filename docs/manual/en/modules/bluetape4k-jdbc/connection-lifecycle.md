---
title: Connection and DataSource lifecycle
description: Define connection ownership and the boundary of the optional HikariCP helpers.
manualId: bluetape4k-jdbc
chapterId: connection-lifecycle
---

# Connection and DataSource lifecycle

## Choose the owner first

Separate ownership of the `DataSource`, pool, and `Connection` before choosing a helper. The application manages the `DataSource` and pool lifecycle. `withConnect` closes only the connection borrowed for one operation. Returning that connection or passing it to another thread uses a closed resource after the block ends.

```kotlin
import io.bluetape4k.jdbc.sql.withConnect
import javax.sql.DataSource

fun ping(dataSource: DataSource): Boolean =
    dataSource.withConnect { connection ->
        connection.prepareStatement("SELECT 1").use { statement ->
            statement.executeQuery().use { rs -> rs.next() }
        }
    }
```

`withConnect` throws `IllegalStateException` if `DataSource.connection` is null and closes a successfully acquired connection with `use`. The creating code still closes each statement and `ResultSet`.

## What each helper closes

| API | Resource | Closing boundary |
| --- | --- | --- |
| `DataSource.withConnect` | `Connection` | Closes after the block. |
| `DataSource.withStatement` | `Connection`, `Statement` | Closes in nested block order. |
| `DataSource.runQuery` | `Connection`, `Statement`, `ResultSet` | The mapper must consume the result before returning. |
| `Connection.preparedStatement` | `PreparedStatement` | Closes the statement; the caller still owns the connection. |
| `hikariDataSourceOf` | `HikariDataSource` | Creates only; the application closes it. |

DataSource extensions usually borrow and close a connection for one block. Connection extensions do not transfer ownership of a connection the caller already holds.

## Configuring HikariCP

HikariCP is an optional `compileOnly` dependency. Add it to the application runtime before using these helpers.

```kotlin
import io.bluetape4k.jdbc.hikari.hikariDataSourceOf

val dataSource = hikariDataSourceOf(
    jdbcUrl = "jdbc:postgresql://db.example.com:5432/app",
    username = "app",
    password = databasePassword,
) {
    poolName = "app-pool"
    maximumPoolSize = 16
    connectionTimeout = 3_000
}

try {
    // application work
} finally {
    dataSource.close()
}
```

`hikariConfigOf` and `hikariDataSourceOf` only wrap Hikari configuration in Kotlin lambdas. They do not choose pool sizes, manage credentials, or install shutdown hooks.

## When Spring owns the connection

A Spring transaction manager may bind a connection to the current transaction. Calling a separate `DataSource.withConnect` or `withTransaction` can create another connection or transaction boundary. Prefer the framework-managed boundary and use only the low-level mapping or statement helpers that fit inside it.

## Operational rules

- Create the pool at application startup and close it once at shutdown.
- Do not create a `HikariDataSource` per request.
- Do not return `Connection`, `Statement`, `ResultSet`, or a lazy `Sequence` from `withConnect`.
- Observe acquisition time, active and idle connections, timeout, and leak detection through pool metrics.

## Sources and tests

- [`DataSourceExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceExtensions.kt)
- [`ConnectionExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ConnectionExtensions.kt)
- [`HikariSupport.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/hikari/HikariSupport.kt)
- [`DataSourceSupportTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/DataSourceSupportTest.kt)
- [`HikariSupportTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/hikari/HikariSupportTest.kt)

## Next chapter

After ownership is explicit, continue with [Prepared statements and batches](./statements-batches.md).
