---
title: Failures, cancellation, and operations
description: Connect reactive-session failures and cancellation limits to concrete operational signals.
manualId: bluetape4k-hibernate-reactive
chapterId: failure-cancellation-operations
---

# Failures, cancellation, and operations

## Exception propagation

An exception from session work becomes a `Uni` or `CompletionStage` failure and then reaches the suspending caller. Tests verify preservation of `IllegalStateException` and `RuntimeException`. Query syntax, mapping, named-graph, and lock failures remain provider exceptions; the extensions do not translate them into domain errors.

Transaction failure handling delegates to Hibernate Reactive `withTransaction`. Keep retries outside that transaction as an application policy, and enable them only after checking idempotency and replay cost.

## What cancellation guarantees

Every Mutiny and Stage callback catches and rethrows `CancellationException`. This prevents the coroutine-to-`Uni` or coroutine-to-`CompletableFuture` boundary from turning cancellation into an ordinary failure.

The 1.12.1 tests do not prove immediate cancellation of an in-flight driver query. Cancellation is a coroutine cooperation contract, not a guarantee about when SQL already sent to the database stops. Configure query and database-side timeouts separately.

## Do not block the event loop

Session work runs on `currentVertxDispatcher()`. Do not directly run these operations in the block:

- JDBC or blocking ORM calls;
- synchronous file or network I/O;
- long compression, encryption, or bulk transformations;
- `Thread.sleep` or blocking locks.

When needed, move data beyond the transaction boundary and process it on a separate dispatcher.

## Operational signals

| Signal | Related causes |
| --- | --- |
| event-loop delay | blocking calls, long callbacks, excessive mapping |
| pool acquire delay | pool size, long transactions, database load |
| query latency | execution plans, lazy fetches, lock waits, network |
| rollback increase | validation failure, contention, timeout, cancellation |
| slow shutdown | open factories or sessions, unfinished requests |

The test fixture's `drop-and-create` and pool size `30` are not production recommendations. Derive settings from service instance count, database connection limits, and the latency budget.

## Executable evidence

- [Mutiny coroutine bridge](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionFactorySupport.kt)
- [Stage coroutine bridge](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionFactorySupport.kt)
- [`MutinyExtrasTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/MutinyExtrasTest.kt)
- [`StageExtrasTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/StageExtrasTest.kt)
- [`MySQLLauncher.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/MySQLLauncher.kt)
