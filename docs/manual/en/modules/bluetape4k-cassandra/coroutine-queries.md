---
title: Coroutine queries and multi-page reads
description: Execute Java Driver async queries as suspend functions and cancellation-aware Flow pipelines.
manualId: bluetape4k-cassandra
chapterId: coroutine-queries
---

# Coroutine queries and multi-page reads

## Problem and API choice

The Java Driver asynchronous API returns `CompletionStage`. In coroutine code, use `executeSuspending` and `prepareSuspending` to suspend until the driver operation completes instead of chaining futures and callbacks. Choose the overload that matches the input you already have.

| Input | API | When to choose it |
| --- | --- | --- |
| CQL without values | `executeSuspending(cql)` | The query has no bind markers |
| Positional values | `executeSuspending(cql, *values)` | Bind values to `?` markers in order |
| Named values | `executeSuspending(cql, values)` | Bind a `Map<String, Any?>` to `:name` markers |
| An existing statement | `executeSuspending(statement)` | Configure paging, consistency, or other statement options at the call site |
| CQL string | `prepareSuspending(cql)` | Prepare a string as a prepared statement |
| `SimpleStatement` | `prepareSuspending(statement)` | Prepare the statement together with its settings |
| `PrepareRequest` | `prepareSuspending(request)` | Construct the driver's prepare request directly |

## Single query

For a small number of positional values, the CQL overload is the shortest choice. It suspends the current coroutine until the future returned by `executeAsync` completes, then returns its `AsyncResultSet`.

```kotlin
import com.datastax.oss.driver.api.core.CqlSession
import io.bluetape4k.cassandra.cql.executeSuspending

suspend fun markInactive(
    session: CqlSession,
    id: Long,
) {
    session.executeSuspending(
        "UPDATE users SET active = ? WHERE id = ?",
        false,
        id,
    )
}
```

Use `executeSuspending(cql)` when there are no values, or `executeSuspending(cql, mapOf("id" to id))` for named markers. If the caller already built a `SimpleStatement` or another `Statement`, pass it to the statement overload instead of converting it back to a string.

## Prepared query

When the same CQL runs with different values, prepare it before binding those values. This example keeps preparation, execution, and reading one row in one function.

```kotlin
import com.datastax.oss.driver.api.core.CqlSession
import io.bluetape4k.cassandra.cql.executeSuspending
import io.bluetape4k.cassandra.cql.prepareSuspending

data class User(
    val id: Long,
    val name: String,
)

suspend fun findUser(session: CqlSession, id: Long): User? {
    val prepared = session.prepareSuspending("SELECT id, name FROM users WHERE id = ?")
    val result = session.executeSuspending(prepared.bind(id))
    return result.one()?.let { row -> User(row.getLong("id"), row.getString("name") ?: "") }
}
```

Choose the `prepareSuspending` overload for the input you already have: `String`, `SimpleStatement`, or `PrepareRequest`. The deprecated `suspendExecute` and `execute` aliases cover CQL varargs, named-value maps, and `Statement` inputs. The deprecated `suspendPrepare` and `prepare` aliases cover only `String` and `SimpleStatement`; there is no deprecated alias for a `PrepareRequest`. During migration, replace each alias with the corresponding `executeSuspending` or `prepareSuspending` overload. Do not use the deprecated aliases in new code.

## Flow paging model

`asFlow` traverses the pages of an `AsyncResultSet` that has already been obtained. It does not execute the query.

```kotlin
import com.datastax.oss.driver.api.core.CqlSession
import io.bluetape4k.cassandra.cql.asFlow
import io.bluetape4k.cassandra.cql.executeSuspending
import io.bluetape4k.cassandra.cql.statementOf
import kotlinx.coroutines.flow.toList

data class User(
    val id: Long,
    val name: String,
)

suspend fun loadActiveUsers(session: CqlSession): List<User> {
    val result = session.executeSuspending(statementOf("SELECT id, name FROM users WHERE active = ?", true))
    return result.asFlow { row -> User(row.getLong("id"), row.getString("name") ?: "") }.toList()
}
```

`executeSuspending` first runs the asynchronous query and produces the initial `AsyncResultSet`. The initial query has therefore executed before `asFlow` collection begins. The `Flow` is cold for traversing and emitting pages from that result, not for executing the initial query.

Once collection starts, `asFlow` maps and emits every row in the current page in order. Only after exhausting that page does it call `fetchNextPage().await()` when `hasMorePages()` is `true`. The implementation does not promise parallel page fetches, prefetching, or a separate page buffer.

## Cancellation and errors

Failures surface at the stage where they occur.

| Failure point | When it surfaces |
| --- | --- |
| Execute or prepare | At the corresponding suspend call |
| Row mapper | During `Flow` collection when that row is mapped |
| Next-page fetch | When collection exhausts the current page and crosses the page boundary |

A `CancellationException` raised while waiting for the next page is rethrown as cancellation. Do not wrap it as an ordinary failure or retry it blindly in a mapper or downstream operation. Other mapper and fetch failures propagate to the collector without conversion.

If the mapper fails on a row or collection is cancelled while processing it, traversal stops at that row. Rows emitted earlier from the current page remain observable to the collector. No later page is fetched because the current page was not exhausted. The next-page fetch starts only after every row in the current page has been emitted successfully.

Rows from earlier pages may already have been consumed when a later-page fetch fails. A consumer that requires all-or-nothing effects must explicitly buffer the complete result before committing those effects. That choice can require memory proportional to the result size.

## Result size and collection choice

Use `toList()` only when the result is bounded and retaining every row in memory is acceptable. For results that are large or not known to be bounded, use `map` or `transform` to shape each value and then process it progressively with `collect`; keep downstream concurrency, queues, and external calls bounded. `map` and `transform` are also cold intermediate operators, so calling them alone does not execute the pipeline: a terminal operator such as `collect` is still required. `asFlow` does not accumulate the full result, but buffers or concurrent work introduced by the collector still need their own capacity limits.

The paging order remains current-page emission followed by the next-page fetch. Adding an operator such as `buffer` downstream must not be treated as a driver-level guarantee of parallel page prefetch.

## Sources and representative tests

- [`AsyncCqlSessionSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupport.kt): `executeSuspending` and `prepareSuspending` overloads and deprecated-alias forwarding
- [`AsyncResultSetSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupport.kt): current-page emission, `fetchNextPage().await()`, and `CancellationException` rethrowing
- [`StatementSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt): `statementOf` with positional and named values
- [`AsyncCqlSessionSupportTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupportTest.kt): integration coverage for CQL, positional values, named values, and statement execution
- [`AsyncResultSetSupportTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupportTest.kt): integration coverage for collecting rows and mapped values across pages
- [`AsyncResultSetSupportUnitTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupportUnitTest.kt): first-page traversal, multi-page ordering, and next-page fetch failure propagation

## Next reading

If session ownership is not settled yet, start with [Session lifecycle](./session-lifecycle.md).

Continue with [Map rows and Cassandra values into Kotlin types](./rows-data-mapping.md) for the result-mapping boundary.
