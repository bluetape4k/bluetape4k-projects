---
title: "Async and low-level CQL operations"
description: "Use CompletableFuture-based Spring Data APIs, row mappers, extractors, and prepared statements from coroutines."
---

# Async and low-level CQL operations

## Awaiting async operations

`AsyncCassandraOperationsCoroutines.kt` applies `await()` to Spring Data `CompletableFuture` results. It exposes suspend operations for select, slice, count, exists, insert, update, delete, and truncate.

```kotlin
suspend fun findPage(
    operations: AsyncCassandraOperations,
    statement: Statement<*>,
): Slice<User> = operations.sliceSuspending(statement)
```

Unlike the reactive path, this API is not Flow streaming. A list query returns a completed collection and retains Spring Data async paging and materialization behavior.

## Reading nullable results

When the Spring Data async API returns a nullable future result, the adapter retains `T?`, `Boolean?`, or `Long?`. `selectSuspending` changes a null list to `emptyList()`, and `sliceSuspending` changes null to an empty `SliceImpl`.

That normalization represents no rows; it does not hide a backend failure. A service calling `selectOneOrNullSuspending` or nullable count and exists operations must define what null means in its domain.

## AsyncCqlOperations and mappers

Low-level CQL adapters accept a CQL string or `Statement` plus a result-set extractor or row mapper.

```kotlin
val names: List<String> = asyncCql.querySuspending(
    "SELECT name FROM users WHERE team = ?",
    teamId,
) { row, _ -> row.getString("name")!! }
```

A row mapper must know exact column names and nullability. Prefer `AsyncCassandraOperations` when full entity mapping is required. Use a result-set extractor only when paging or execution metadata requires direct control.

## ReactiveCqlOperations choices

`ReactiveCqlOperationsSupport` has overloads for objects, maps, rows, and result sets as suspend values or Flow. It can also separate prepared-statement creation from binding.

```kotlin
val rows: Flow<Row> = reactiveCql.queryForRowsFlow(statement)
val user: User? = reactiveCql.queryForObjectSuspending(cql, rowMapper, id)
```

These operations do not add another SQL-client abstraction. They preserve Spring Data prepared-statement creators, binders, row mappers, and exception translation.

## Prepared statements and binding

Prepare repeated CQL with `ReactiveSession.prepareSuspending` and retain a reusable query shape through `BoundStatement`. Bind values instead of interpolating strings. Besides escaping safety, this supports prepare-cache reuse and clearer observation.

Put consistency, page size, timeout, tracing, and idempotence on the statement or execution profile. Coroutine adapters do not override them.

## Failures and cancellation

`CompletableFuture.await()` participates in coroutine cancellation, but it does not mirror the server state of a submitted Cassandra query. Review idempotence and LWT semantics before enabling write retries.

Null assertions in mappers, wrong column types, and bind-marker mismatches are data-contract failures outside the adapter. Translate them only at a repository or service boundary that can define a stable domain error.

## Sources

- [`AsyncCassandraOperationsCoroutines.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/AsyncCassandraOperationsCoroutines.kt)
- [`AsyncCqlOperationsCoroutines.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/AsyncCqlOperationsCoroutines.kt)
- [`ReactiveCqlOperationsSupport.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/ReactiveCqlOperationsSupport.kt)
- [`AsyncCqlOperationsCoroutinesUnitTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/cql/AsyncCqlOperationsCoroutinesUnitTest.kt)
