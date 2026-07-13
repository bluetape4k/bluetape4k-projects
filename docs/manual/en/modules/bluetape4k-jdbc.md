---
manualId: bluetape4k-jdbc
title: "Module bluetape4k-jdbc"
description: "Use Kotlin helpers without losing the connection, statement, ResultSet, and transaction lifecycle defined by JDBC."
kind: library
group: data
---

# Module bluetape4k-jdbc

## Capabilities {#problem}

`bluetape4k-jdbc` adds Kotlin extensions to standard JDBC. It shortens the code for borrowing connections, executing prepared statements, converting `ResultSet` rows, and restoring transaction state. It is not an ORM and does not hide SQL, the connection pool, or database-driver behavior.

Use it when you want direct SQL control without rewriting JDBC resource handling. If you instead need a typed table/query DSL or managed entity lifecycle, continue to [Choosing the next persistence layer](./bluetape4k-jdbc/ecosystem-paths.md) and evaluate Exposed or Hibernate.

## Decisions before adoption {#when-to-use}

- Decide who creates and closes the `DataSource` and connection pool.
- Decide whether the service owns transactions or an existing Spring transaction manager does.
- Decide whether each query fully materializes its result or truly needs a lazy sequence backed by an open `ResultSet`.
- Bind values as prepared-statement parameters instead of interpolating them into SQL.
- Bound input rows and JDBC batch size for bulk writes.

## Coordinates {#coordinates}

Consumers manage the central BOM version, not each library version. The application separately chooses its database driver and pool implementation.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-jdbc")

    runtimeOnly("org.postgresql:postgresql") // replace with the selected driver
}
```

## First query {#quick-start}

`withConnect` closes the connection borrowed from the `DataSource` when its block ends. `executeQuery` also closes its prepared statement and `ResultSet` within the same scope.

```kotlin
import io.bluetape4k.jdbc.sql.executeQuery
import io.bluetape4k.jdbc.sql.mapSingle
import javax.sql.DataSource

data class AccountSummary(
    val id: Long,
    val name: String,
)

fun findAccount(dataSource: DataSource, id: Long): AccountSummary =
    dataSource.executeQuery(
        "SELECT id, name FROM accounts WHERE id = ?",
        id,
    ) { rs ->
        rs.mapSingle { row ->
            AccountSummary(
                id = row.getLong("id"),
                name = row.getString("name"),
            )
        }
    }
```

`mapSingle` throws `NoSuchElementException` for no rows and `IllegalStateException` for multiple rows. Use `mapFirst` for a zero-or-one result contract.

## API by task {#api-by-task}

| Task | Start with | Boundary to preserve |
| --- | --- | --- |
| Borrow a connection for one operation | `DataSource.withConnect` | The connection closes when the block ends. |
| Execute direct SQL | `runQuery`, `executeUpdate`, `executeInsert` | The caller owns SQL and the resource scope. |
| Execute parameterized SQL | `Connection.executeQuery`, `executeUpdate` | The helper creates and closes a prepared statement. |
| Write parameter rows in batches | `executeBatch`, `executeLargeBatch` | Every row must have the same parameter count. |
| Read SQL NULL as Kotlin nullable | `getIntOrNull`, `getLongOrNull`, and peers | Each helper checks `wasNull()` immediately after the JDBC getter. |
| Map rows | `mapFirst`, `mapSingle`, `toList`, `extract` | Check how far each function advances the cursor. |
| Commit, rollback, and restore state | `withTransaction`, `withReadOnlyTransaction` | Original auto-commit, isolation, and read-only state are restored. |
| Configure HikariCP | `hikariConfigOf`, `hikariDataSourceOf` | HikariCP is `compileOnly`; the application owns the dependency and shutdown. |

## Learning path {#concepts}

Each chapter focuses on a boundary that is easy to get wrong in production. Examples link directly to 1.11.0 source and representative tests, so readers can move from the explanation to the implementation evidence.

1. [Connection and DataSource lifecycle](./bluetape4k-jdbc/connection-lifecycle.md) — choose connection ownership and define the Hikari helper boundary.
2. [Prepared statements and batches](./bluetape4k-jdbc/statements-batches.md) — parameter binding, generated keys, and batch-row contracts.
3. [Reading and mapping ResultSet](./bluetape4k-jdbc/resultset-mapping.md) — SQL NULL, cardinality, collections, cursor movement, and lazy sequences.
4. [Transactions and state restoration](./bluetape4k-jdbc/transactions.md) — commit, rollback, and restoration of reusable connections.
5. [Choosing the next persistence layer](./bluetape4k-jdbc/ecosystem-paths.md) — decide whether to stay with JDBC or move to Exposed or Hibernate.

New users should normally read chapters 1 through 4 in order. Start with chapter 5 when selecting the persistence architecture for a project.

## Recommended pattern {#patterns}

The layer that creates a resource closes it. Convert rows to value objects while the `ResultSet` is open, bind SQL values as parameters, and place transactions around the smallest service operation whose statements must succeed or fail together. Keep pool size, timeouts, and shutdown in application configuration.

## Integrations {#integrations}

The module exposes `bluetape4k-core` as an API dependency. HikariCP, Tomcat JDBC, Agroal, and Spring JDBC are optional `compileOnly` integrations; their APIs do not imply that an implementation is automatically present at runtime.

When Spring owns a transaction, confirm its connection binding before nesting `withTransaction`. Mixing framework-managed and direct JDBC transaction boundaries in one call path obscures commit ownership.

## Configuration {#configuration}

The application owns the JDBC URL, driver, username, credentials, pool size, connection and statement timeouts, and default isolation. This module installs no configuration file or process-wide defaults. Configure HikariCP through `hikariDataSourceOf` or the host framework's datasource settings.

## Failure behavior {#failures}

Driver `SQLException`s propagate by default. Exact-one-row mappers throw when cardinality does not match. If a transaction block or commit fails, rollback is attempted; rollback and transaction-state restoration failures are attached to the original failure as suppressed exceptions.

## Operations {#operations}

Observe pool saturation, connection acquisition time, query latency, rollback count, batch size, and database timeouts together. Do not return lazy sequences or JDBC resources beyond their owning block, and correlate slow queries with pool timeouts in the same operation context.

## Testing {#testing}

Representative 1.11.0 tests include H2-based API coverage and a MySQL Testcontainers path. Serialize this suite with other heavy integration tests when Docker is involved.

```bash
./gradlew :bluetape4k-jdbc:test --no-build-cache --no-configuration-cache
```

## Workshops {#workshops}

No dedicated workshop repository is registered yet. The linked `JdbcTemplateTest`, `TransactionExtensionsTest`, and `ResultSetMappingExtensionsTest` serve as executable examples. A small H2 schema is enough to practice query, mapping, rollback, and batch behavior in sequence.

## 1.11.0 scope {#limitations}

This manual targets the source published by the `bluetape4k-projects` 1.11.0 tag. APIs added to `develop` after the release are excluded. The module does not provide schema migration, a query DSL, entity dirty checking, or a coroutine-friendly non-blocking database driver.

## Sources and tests {#sources}

- [`DataSourceExtensions.kt`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceExtensions.kt)
- [`PreparedStatementExtensions.kt`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementExtensions.kt)
- [`ResultSetExtensions.kt`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetExtensions.kt)
- [`ResultSetMappingExtensions.kt`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetMappingExtensions.kt)
- [`TransactionExtensions.kt`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/TransactionExtensions.kt)
- [`JdbcTemplateTest.kt`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/JdbcTemplateTest.kt)
- [`TransactionExtensionsTest.kt`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/TransactionExtensionsTest.kt)
- [`ResultSetMappingExtensionsTest.kt`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/ResultSetMappingExtensionsTest.kt)
