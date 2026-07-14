---
title: "WriteOptions and batches"
description: "Make TTL, timestamp, and LWT options explicit, and understand full Flow collection before batch execution."
---

# WriteOptions and batches

## The option DSL is a thin builder wrapper

`queryOptions`, `insertOptions`, `updateOptions`, `writeOptions`, and `deleteOptions` invoke Spring Data builders through Kotlin receiver lambdas. They do not define new validation rules or defaults.

```kotlin
val options = insertOptions {
    withIfNotExists()
    ttl(Duration.ofMinutes(5))
}
val result = operations.insertSuspending(user, options)
if (!result.wasApplied()) {
    // Handle an existing row.
}
```

TTL, timestamp, timeout, consistency, and LWT have different failure and consistency meanings. Treat them as use-case contracts rather than a single group of performance options.

## Applying WriteOptions to query-builder statements

`addWriteOptions` adds TTL or timestamp to DataStax Query Builder `Insert`, `UpdateStart`, and `DeleteSelection` statements.

- `Insert` receives TTL and timestamp.
- `UpdateStart` receives TTL and timestamp.
- A general `Update` that has already moved past `UpdateStart` may be returned unchanged.
- `DeleteSelection` receives timestamp only.

Apply update options before changing the statement into later shapes such as assignments. Always use the newly returned statement.

## The TTL boundary

`WriteOptions.isPositiveTtl` is implemented as `ttl != null && !ttl.isNegative`. Despite the name, a zero duration is therefore true. Confirm the Spring Data and Cassandra meaning of zero and validate strictly positive application input where that is the intended policy.

A Cassandra write timestamp is not automatically a business version. Adding wall-clock timestamps everywhere can let an older replay win over a newer value. Keep domain versions and driver timestamps separate.

## What Flow batch adapters do

`insertFlow`, `updateFlow`, and `deleteFlow` pass `mono { entities.toList() }` to Spring Data batch operations. They do not stream one row at a time to Cassandra.

```kotlin
val batch = reactiveOperations.batchOps()
    .insertFlow(users.take(batchSize))

val result = batch.execute().awaitSingle()
```

The adapter only configures the batch chain. `execute()` performs it later. If the input Flow fails, list collection fails before batch execution.

## Batch size and atomicity

A Cassandra batch is not a JDBC bulk insert. Use it for a small group of related mutations that needs its semantics. Large cross-partition batches increase coordinator pressure and timeout risk.

Bound both dimensions introduced by the Flow adapter:

1. the collection size the application heap can hold;
2. the statement count and partition distribution Cassandra can handle.

Chunk large streams and execute each chunk sequentially or with bounded concurrency. An infinite Flow never reaches the batch execution stage.

## LWT and failure handling

`withIfNotExists` and version-based optimistic locking use Cassandra LWT. Distinguish transport success from condition success by reading `wasApplied()`. A timeout can leave the original application result uncertain, so define idempotency and read-after-timeout checks before blind retries.

## Sources

- [`OptionsSupport.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupport.kt)
- [`ReactiveCassandraBatchOperationsCoroutines.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveCassandraBatchOperationsCoroutines.kt)
- [`OptionsSupportTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupportTest.kt)
- [`AsyncOptimisticLockingTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/async/AsyncOptimisticLockingTest.kt)
