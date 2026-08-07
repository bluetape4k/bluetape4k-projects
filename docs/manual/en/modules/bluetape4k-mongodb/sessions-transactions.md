---
title: Sessions and transactions
description: Follow session passing, commit-abort-close order, topology requirements, and cancellation boundaries in withClientSession and inTransaction.
manualId: bluetape4k-mongodb
chapterId: sessions-transactions
---

# Sessions and transactions

## Session-only work

`withClientSession` creates a session with `startSession()`, passes it to a suspend block, and closes it in `finally`. Starting a session does not start a transaction.

```kotlin
val result = client.withClientSession { session ->
    orders.find(session, Filters.eq("orderId", orderId)).firstOrNull()
}
```

Collection helpers such as `findFirst`, `exists`, `upsert`, and `findAsFlow` have no session parameter. Use native driver session overloads for causal consistency or transactional work.

## Successful `inTransaction` path

`inTransaction` opens a session, starts a transaction, and invokes the block. When the block returns, it awaits `commitTransaction()` and returns the block result. `withClientSession` then closes the session in `finally`.

```kotlin
val receipt = client.inTransaction { session ->
    orders.insertOne(session, order)
    inventory.updateOne(
        session,
        Filters.eq("sku", order.sku),
        Updates.inc("stock", -1),
    )
    Receipt(order.id)
}
```

Every operation must receive the same `ClientSession` to participate in the transaction. A session-less overload runs outside it.

## `TransactionOptions`

Without options, the helper calls `session.startTransaction()`. With a value, it calls `startTransaction(transactionOptions)`. Choose read concern, write concern, and read preference according to consistency requirements and cluster configuration.

The helper does not classify transient transaction errors and retry the block. Retrying must account for external side effects and idempotency, so design it explicitly at the application boundary.

## Exceptions and abort

If the block or commit throws an `Exception`, the helper attempts `abortTransaction()` and rethrows the original error. An abort failure is attached to the original error as suppressed and logged. The session closes afterward.

Cancellation has a separate `CancellationException` catch, so the cancellation signal is rethrown. However, 1.12.1 calls `abortTransaction()` directly from the already cancelled coroutine and does not wrap cleanup in a `NonCancellable` context. Do not assume abort completes under every cancellation path.

## Topology requirements

MongoDB transactions require a replica set or sharded cluster. A standalone Testcontainer may fail for topology reasons. Services using transactions should test commit, abort, and retry policy against a fixture with production-like topology behavior.

## External side effects

HTTP calls, message publication, and file writes inside the block are not rolled back by a MongoDB transaction. Use an explicit pattern such as an outbox when database commit and external publication must be coordinated.

## What to observe

Separate transaction duration, commit and abort failures, retry labels, write-concern timeouts, and pool wait. Include suppressed abort failures in exception reporting so cleanup failures remain visible.

## Sources and tests

- [`MongoClientExtensions.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientExtensions.kt)
- [`MongoClientSupportTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoClientSupportTest.kt)

The 1.12.1 test verifies `withClientSession` block execution and exception propagation but does not directly test `inTransaction` commit, abort, or cancellation. Application integration tests must cover that gap.
