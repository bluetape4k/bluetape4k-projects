---
manualId: bluetape4k-r2dbc
title: "R2DBC Coroutine Extensions"
description: "Run Spring R2DBC SQL with Kotlin Coroutines and Flow, including mapping, transactions, and connection-pool utilities."
kind: library
group: data
learningOrder: 610
---

# R2DBC Coroutine Extensions

## Capabilities {#problem}

`bluetape4k-r2dbc` adds Kotlin-oriented execution, mapping, CRUD, and transaction helpers to Spring R2DBC's `DatabaseClient` and `R2dbcEntityTemplate`. It also provides DSLs for connection factories and pools, typed-null binding, a dynamic query builder, and PostgreSQL JSON converters.

The module does not replace an R2DBC driver or provide a complete ORM. Callers still own SQL and transaction boundaries, and `QueryBuilder` is not a type-safe DSL that validates tables and columns at compile time. Continue to the [R2DBC ecosystem path](./bluetape4k-r2dbc/ecosystem-paths.md) when a table DSL and repository layer are required.

## Decisions before adoption {#when-to-use}

- Confirm that the call path must avoid blocking JDBC. Calling JDBC from a coroutine does not make the I/O non-blocking.
- Decide who closes the `ConnectionPool` and directly acquired connections.
- Decide whether Spring owns transactions or this module creates the boundary with `withTransactionSuspend`.
- Choose domain-object mapping or raw map results.
- Choose between waiting during overload and failing fast with bounded pending acquire and an acquire timeout.
- Define where SQL fragments and identifiers are validated.

## Dependencies {#coordinates}

Consumers manage only the central `bluetape4k-dependencies` BOM version. The application selects Spring R2DBC and the database driver.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-r2dbc")

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("org.postgresql:r2dbc-postgresql") // replace with the selected driver
}
```

## First query {#quick-start}

When Spring Boot supplies `DatabaseClient`, `R2dbcEntityTemplate`, and `MappingR2dbcConverter`, the 1.12.1 auto-configuration creates `R2dbcClient`. The typed `execute<T>` path maps rows through `MappingR2dbcConverter`.

```kotlin
import io.bluetape4k.r2dbc.R2dbcClient
import io.bluetape4k.r2dbc.core.execute
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.flow

data class AccountSummary(
    val accountId: Long,
    val name: String,
)

fun findActiveAccounts(client: R2dbcClient): Flow<AccountSummary> =
    client
        .execute<AccountSummary>(
            "SELECT account_id, name FROM accounts WHERE active = :active"
        )
        .bind("active", true)
        .fetch()
        .flow()
```

The query starts when the Flow is collected. Keep the path non-blocking and do not mix it with arbitrary blocking or separately acquired connections.

## API map {#api-by-task}

| Task | Start with | Boundary to remember |
| --- | --- | --- |
| Hold the client components | `R2dbcClient` | It stores existing Spring objects; it does not create or close a pool. |
| Configure connection options and a pool | `connectionFactoryOptionsOf`, `r2dbcConnectionPool` | The application supplies the driver and closes the pool. |
| Execute raw SQL | `DatabaseClient.execute`, `R2dbcClient.execute` | The caller owns SQL and result cardinality. |
| Bind named or indexed parameters | `bindMap`, `bindIndexedMap`, `bindNullable` | Raw nulls in maps are rejected; use typed nulls. |
| Map rows to a type | `execute<T>`, `MappingR2dbcConverter.read<T>` | Converter errors propagate when columns and properties do not match. |
| Run table-oriented CRUD | `insert`, `update`, `delete` | Table and column identifiers are checked, but the caller owns raw where SQL. |
| Build dynamic SQL | `query`, `queryWithCount`, `QueryBuilder` | It is not type-safe, and `queryWithCount` executes its block twice. |
| Run a suspend transaction | `withTransactionSuspend` | It connects the same `DatabaseClient` factory to a transaction manager. |
| Initialize schema and data | `connectionFactoryInitializer`, `resourceDatabasePopulatorOf` | Attach initialization and failure policy to the application lifecycle. |

## Learning path {#concepts}

Each chapter explains ownership and failure boundaries that are easy to get wrong, not just a feature list. Examples link to the 1.12.1 release source and representative tests, so readers can continue directly from the explanation to implementation and verification evidence.

1. [Connections and pools](./bluetape4k-r2dbc/connections-and-pools.md) — configure connections, own the pool lifecycle, and choose overload behavior.
2. [SQL execution and parameter binding](./bluetape4k-r2dbc/sql-and-binding.md) — use `DatabaseClient`, typed mapping, named/indexed parameters, and typed nulls.
3. [CRUD and row mapping](./bluetape4k-r2dbc/crud-and-mapping.md) — combine raw-table and entity CRUD with identifier checks, `Readable`, and PostgreSQL JSON conversion.
4. [Dynamic queries](./bluetape4k-r2dbc/dynamic-queries.md) — build nested predicates and count queries within the 1.12.1 validation contract.
5. [Transactions and lifecycle](./bluetape4k-r2dbc/transactions-and-lifecycle.md) — connect commit, rollback, transaction-aware connections, and schema initialization.
6. [R2DBC ecosystem path](./bluetape4k-r2dbc/ecosystem-paths.md) — progress from direct Spring R2DBC to this module, Spring coroutine extensions, Exposed R2DBC, and the workshop.

For first adoption, read chapters 1 through 5 in order. When selecting a persistence technology, start with chapter 6 and return to the required level.

## Recommended patterns {#patterns}

Create the pool once during application startup and close it once during shutdown. Separate parameters from SQL and bind nullable values with typed `Parameter` objects. Place `withTransactionSuspend` around the smallest service boundary whose writes must succeed together. Keep Flow execution non-blocking and isolate any blocking bridge behind an explicit adapter boundary.

## Integrations {#integrations}

`bluetape4k-coroutines` and `r2dbc-pool` are API dependencies. Spring Data R2DBC, Spring Boot auto-configuration, Reactor, Jackson3, and database drivers are optional or `compileOnly`; add the runtime capabilities required by the APIs in use.

Use [`bluetape4k-spring-boot-r2dbc`](./bluetape4k-spring-boot-r2dbc.md) for broader Spring Data R2DBC coroutine CRUD extensions. Move to the R2DBC modules in `bluetape4k-exposed` when a table/column DSL and repository abstraction are needed.

## Configuration {#configuration}

With Spring Boot, `spring.r2dbc.*` owns driver connection settings. For direct configuration, use `R2dbcConnectionConfig` and `R2dbcPoolConfig` to set the driver, SSL, timeouts, pool size, warmup, pending queue, and validation strategy.

In 1.12.1, `R2dbcClientAutoConfiguration` activates when `DatabaseClient` is present and does not back off when the application defines its own `R2dbcClient` bean. Applications that configure the type directly must avoid the auto-configuration conflict themselves.

## Failure behavior {#failures}

`bindMap`, `bindIndexedMap`, and map-based updates reject raw `null` with `IllegalArgumentException`. Use `typedNullParameter<T>()`, `bindNullable<T>()`, or `nullValue`. Mapping and PostgreSQL JSON conversion propagate Spring conversion failures instead of silently substituting values.

Invalid pool-size or JMX settings fail during configuration or pool conversion. An exception in a transaction block triggers rollback, and cancellation must not be swallowed as an ordinary fallback condition.

## Operations {#operations}

Observe active and idle connections, pending acquire, acquire timeout, connection hold time, query p95/p99, and rollbacks together. Bounded-queue failures do not automatically mean the pool should grow; they may be the intended signal that the database cannot accept more work. `validationQuery` adds a database round trip to acquisition, so prefer driver-local validation when it is sufficient.

## Testing {#testing}

The 1.12.1 release tests cover H2 SQL, CRUD, transactions, and pool saturation.

```bash
./gradlew :bluetape4k-r2dbc:test --no-build-cache --no-configuration-cache
```

Pool benchmarks are not ordinary regression tests. PostgreSQL and MySQL benchmarks use Testcontainers and should not run in parallel with other heavy database suites.

## Workshops {#workshops}

Within the module, `ExecuteTest`, `InsertTest`, and `TransactionSupportTest` are the smallest executable examples. For a higher-level Kotlin SQL DSL and repository path, [Exposed R2DBC Workshop](https://github.com/bluetape4k/exposed-r2dbc-workshop) continues through SQL DSL, DDL/DML, coroutines, Spring WebFlux, and production patterns.

## 1.12.1 scope {#limitations}

This manual targets the `bluetape4k-projects` 1.12.1 tag. It does not describe the post-release auto-configuration back-off or `QueryBuilder.limit` and `offset` precondition checks as 1.11 features. In 1.11, callers must enforce `limit > 0` and `offset >= 0`.

The 1.12.1 README mentions APIs that are absent from the release source: `sqlInsert`, `sqlUpdate`, `sqlDelete`, `awaitGeneratedKey`, `awaitSingleAsMap`, `awaitCount`, `awaitExists`, and `awaitList`. They are intentionally excluded; this manual uses only APIs present in release source and tests.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Extension Function API Overview diagram

[![Extension Function API Overview diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-diagram-01.svg)

_Release README: [`data/r2dbc/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/r2dbc/README.md)_

### Core API Class Structure diagram

[![Core API Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-diagram-02.svg)

_Release README: [`data/r2dbc/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/r2dbc/README.md)_

### JDBC vs R2DBC Comparison diagram

[![JDBC vs R2DBC Comparison diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-diagram-03.svg)

_Release README: [`data/r2dbc/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/r2dbc/README.md)_

### R2DBC Query Execution Flow diagram

[![R2DBC Query Execution Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-sequence-01.svg)

_Release README: [`data/r2dbc/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/r2dbc/README.md)_

<!-- release-readme-diagrams:end -->

## Sources and tests {#sources}

- [`R2dbcClient.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/R2dbcClient.kt)
- [`ConnectionPoolSupport.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/ConnectionPoolSupport.kt)
- [`R2dbcPoolConfig.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/R2dbcPoolConfig.kt)
- [`DatabaseClientSupport.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/DatabaseClientSupport.kt)
- [`Execute.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Execute.kt)
- [`QueryBuilder.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/QueryBuilder.kt)
- [`TransactionSupport.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/TransactionSupport.kt)
- [`ExecuteTest.kt`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/ExecuteTest.kt)
- [`ConnectionPoolSupportTest.kt`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/pool/ConnectionPoolSupportTest.kt)
- [`TransactionSupportTest.kt`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/support/TransactionSupportTest.kt)
