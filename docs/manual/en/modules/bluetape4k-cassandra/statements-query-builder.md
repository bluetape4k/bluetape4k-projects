---
title: Choose statements and QueryBuilder APIs
description: Select raw CQL, prepared and bound statements, batches, or QueryBuilder by task and safety boundary.
manualId: bluetape4k-cassandra
chapterId: statements-query-builder
---

# Choose statements and QueryBuilder APIs

## Start with the value boundary

A fixed query shape rarely needs more than a CQL string. Use `QueryBuilder` when conditions or assignments must be composed. In either case, keep runtime values out of the CQL text and bind them through markers. String interpolation makes quoting and escaping a caller responsibility and prevents prepared-query reuse.

| Task | Choose | Reason |
| --- | --- | --- |
| Run one fixed CQL statement | `statementOf` | Concise support for positional and named values |
| Repeat the same CQL with different values | Prepared + bound statement | Separates CQL preparation from value binding |
| Compose conditional CRUD | DataStax `QueryBuilder` | Keeps identifiers and value expressions distinct |
| Apply an atomic group of changes in one partition | `BatchStatement`, after checking the semantics | Use only when Cassandra batch behavior is required |

## Use `statementOf` for fixed CQL

`statementOf` has overloads for CQL without values, positional values, and named values. Blank CQL fails immediately with `IllegalArgumentException`. Marker count and value-type errors surface later when the driver prepares or executes the statement.

```kotlin
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import io.bluetape4k.cassandra.cql.statementOf

val allUsers: SimpleStatement =
    statementOf("SELECT id, name FROM users")

val oneUser: SimpleStatement =
    statementOf("SELECT id, name FROM users WHERE id = ?", 7L)

val namedUser: SimpleStatement =
    statementOf(
        "SELECT id, name FROM users WHERE id = :id",
        mapOf("id" to 7L),
    )
```

Use `simpleStatementOf(query) { ... }` when page size, consistency, or another statement option belongs at construction time. Neither `statementOf` nor `simpleStatementOf` is a value-interpolation helper. Keep `?` or `:name` markers for runtime data.

## Separate prepare from bind for repeated work

Prepare an UPDATE once outside the repeated work, then bind values for each item.

```kotlin
import com.datastax.oss.driver.api.core.CqlSession
import io.bluetape4k.cassandra.cql.executeSuspending
import io.bluetape4k.cassandra.cql.prepareSuspending

suspend fun renameUsers(
    session: CqlSession,
    renames: Iterable<Pair<Long, String>>,
) {
    val prepared = session.prepareSuspending(
        "UPDATE users SET name = ? WHERE id = ?",
    )

    for ((userId, newName) in renames) {
        val bound = prepared.bind(newName, userId)
        session.executeSuspending(bound)
    }
}
```

This example executes sequentially inside one function; the key point is that it does not prepare the same statement inside the loop. Changing marker order can write to the wrong columns even when the Kotlin types happen to match. Prefer named markers and `boundStatementBuilder()` when a statement has many or optional values. If a caller already has a `BoundStatement` template, `boundStatementOf(template) { ... }` creates a new statement instead of mutating the template.

## Compose conditional CRUD with QueryBuilder

Tables and columns are identifiers; user names and IDs are values. QueryBuilder can construct the statement shape while values remain named bind markers that are filled after preparation.

```kotlin
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.update
import io.bluetape4k.cassandra.cql.executeSuspending
import io.bluetape4k.cassandra.cql.prepareSuspending

suspend fun renameUserWithBuilder(
    session: CqlSession,
    userId: Long,
    newName: String,
) {
    val updateStatement = update("users")
        .setColumn("name", bindMarker("name"))
        .whereColumn("id").isEqualTo(bindMarker("id"))
        .build()

    val prepared = session.prepareSuspending(updateStatement)
    val bound = prepared.boundStatementBuilder()
        .setString("name", newName)
        .setLong("id", userId)
        .build()

    session.executeSuspending(bound)
}
```

A table or column name cannot be supplied through a value marker. If an identifier comes from configuration, validate it against an allowlist, construct a `CqlIdentifier`, and use the corresponding QueryBuilder identifier overload. Do not turn stored data such as a user name into an identifier or raw snippet.

`String.raw()` passes a CQL fragment directly to `QueryBuilder.raw`. The library does not validate its syntax or safety. Reserve it for static, reviewed fragments such as a function form that QueryBuilder cannot express directly. Never pass request parameters, tenant data, or search text to `raw()`. The `literal()` helpers can render CQL literals, but bind markers remain the default for runtime input.

## Roles of the Simple, Bound, and Batch factories

| Factory | Result and boundary |
| --- | --- |
| `simpleStatementOf(query) { ... }` | A new `SimpleStatement` with builder options applied |
| `boundStatementOf(template) { ... }` | A new `BoundStatement` based on an existing bound template |
| `batchStatementOf(type)` | An empty batch |
| `batchStatementOf(type, *statements)` | A batch retaining vararg statement order |
| `batchStatementOf(type, statements)` | A batch populated from an `Iterable` |
| `batchStatementOf(type) { ... }` | A batch built with the builder DSL |
| `batchStatementOf(template) { ... }` | A new batch extending an existing batch template |

These factories shorten driver object construction. They do not change Cassandra execution semantics.

## A batch is not a throughput tool

`BatchStatement` is not a general-purpose performance optimization for sending many requests at once. First establish that the changes must succeed or fail as one operation, and keep them in the same partition when possible. A multi-partition batch can increase coordinator and batch-log work.

`BatchType.LOGGED` creates a logged batch, but atomicity, isolation, and performance still follow Cassandra's batch rules. The helper does not add cross-partition transactions or relational-database isolation. If the only goal is fewer network round trips, first consider asynchronous statements with a bounded concurrency level.

```kotlin
import com.datastax.oss.driver.api.core.cql.BatchType
import io.bluetape4k.cassandra.cql.batchStatementOf

val batch = batchStatementOf(BatchType.LOGGED, updateName, updateProfile)
```

Every item must be a `BatchableStatement`. Mixing unrelated partition keys or continuously growing a batch can increase operational cost and failure scope even when the driver accepts it.

## Find failures close to the query

- `statementOf` and `simpleStatementOf` reject blank CQL immediately.
- Missing markers, extra markers, and incompatible values fail during prepare, bind, or execute.
- Validate dynamic identifiers with an allowlist. Identifier validation is not the same as value validation.
- Do not use `raw()` unless the caller can establish that the fragment is safe.
- Before retrying statements from a failed batch individually, check idempotence and the possibility of partial effects.

## Sources and representative tests

- [`StatementSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt): simple, bound, and batch factories and the `statementOf` overloads
- [`QueryBuilderSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/querybuilder/QueryBuilderSupport.kt): bind markers, raw fragments, and UDT helpers
- [`RelationBuilderSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/querybuilder/RelationBuilderSupport.kt): comparison and `IN` relation helpers
- [`TermSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/querybuilder/TermSupport.kt): function, operator, and literal term helpers
- [`TermSupportTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/querybuilder/TermSupportTest.kt): operator, function, collection, tuple, and UDT literal rendering and codec failures

## Previous and next

- Previous: [Map rows and Cassandra values into Kotlin types](./rows-data-mapping.md)
- Next: [Operational boundaries and Testcontainers verification](./operations-testing.md)
