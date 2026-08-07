---
title: SQL execution and parameter binding
description: Execute SQL with DatabaseClient and R2dbcClient while binding named, indexed, and nullable parameters safely.
manualId: bluetape4k-r2dbc
chapterId: sql-and-binding
---

# SQL execution and parameter binding

## Two execution paths

`DatabaseClient.execute` returns Spring's `GenericExecuteSpec`. `R2dbcClient.execute<T>` maps rows through the stored `MappingR2dbcConverter`. Choose direct `DatabaseClient` mapping for explicit SQL projections and the typed path to reuse Spring Data mapping metadata.

```kotlin
val users = client
    .execute<User>("SELECT * FROM users WHERE active = :active")
    .bind("active", true)
    .fetch()
    .flow()
    .toList()
```

Use `awaitOne()` for exactly one row, `awaitOneOrNull()` for zero or one, and `flow()` for multiple rows. Match the terminal operation to the query's cardinality contract.

## Named parameters

```kotlin
val spec = client.databaseClient
    .sql("SELECT * FROM users WHERE username = :username AND active = :active")
    .bindMap(mapOf("username" to "jsmith", "active" to true))
```

`bindMap` converts entries to R2DBC parameters. A raw `null` has no database type and fails with `IllegalArgumentException`.

## Preserve null type information

```kotlin
client.databaseClient
    .sql("UPDATE users SET description = :description WHERE user_id = :id")
    .bindMap(mapOf("description" to typedNullParameter<String>()))
    .bind("id", 1)
    .fetch()
    .awaitRowsUpdated()
```

For a single value, use `bindNullable<String>("description", value)`. In both cases, the null retains its target type.

## Indexed parameters

Spring R2DBC uses zero-based indexed binding.

```kotlin
client.databaseClient
    .sql("SELECT name FROM users WHERE username = ? AND active = ?")
    .bindIndexedMap(mapOf(0 to "jsmith", 1 to true))
    .map<String> { row, _ -> row.get("name", String::class.java)!! }
    .awaitOne()
```

`bindIndexedMap` rejects negative indexes and raw nulls. Placeholder syntax varies by driver, so verify `?` or `$1` behavior with the selected driver rather than mixing styles.

## Prefer tests over drifted README examples

The 1.12.1 README mentions `awaitList`, `awaitSingleAsMap`, `awaitCount`, and `awaitExists`, but those names are absent from the release source. This chapter uses the verified Spring R2DBC operations from `ExecuteTest`: `awaitOne`, `awaitOneOrNull`, `flow`, and `awaitRowsUpdated`.

Driver and Spring exceptions propagate. Do not retry every backend failure; consider transaction state, idempotency, and the timeout budget. Preserve Flow cancellation instead of converting it to a fallback value.

## Sources and tests

- [`DatabaseClientSupport.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/DatabaseClientSupport.kt)
- [`ParameterSupport.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/ParameterSupport.kt)
- [`Execute.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Execute.kt)
- [`ExecuteTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/ExecuteTest.kt)
- [`DatabaseClientSupportTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/support/DatabaseClientSupportTest.kt)

## Next chapter

Continue to [CRUD and row mapping](./crud-and-mapping.md).
