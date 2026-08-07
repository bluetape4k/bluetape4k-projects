---
title: Clients, distributed objects, and streams
description: Understand Redisson client ownership, batches, transactions, lock IDs, and Stream consumer-group helpers.
manualId: bluetape4k-redisson
chapterId: client-distributed-objects-streams
---

# Clients, distributed objects, and streams

## The creator closes the client

`redissonClient {}` and `redissonClientOf(config)` create a new `RedissonClient` on every call and do not shut it down. Create one client per application lifecycle and call `shutdown()` from the same owner. A reactive client is a view of an internally created client, so it has the same ownership requirement.

```kotlin
class RedisResources : AutoCloseable {
    val client = redissonClient {
        useSingleServer().address = "redis://127.0.0.1:6379"
    }

    override fun close() = client.shutdown()
}
```

When Spring Boot owns a client bean, reuse it. A second client also creates a second connection pool and Netty resource set.

## Batch and transaction solve different problems

`withBatch` queues async commands and reduces round trips; it does not promise atomic execution. `withTransaction` commits after a successful action and attempts rollback before rethrowing the original failure.

```kotlin
client.withBatch {
    getBucket<String>("profile:42").setAsync("Debop")
    getAtomicLong("profile:revision").incrementAndGetAsync()
}

client.withTransaction {
    getMap<String, Long>("balances").put("42", 1_000L)
    getMap<String, String>("ledger").put("tx-1", "credited")
}
```

Rollback is remote work and can fail. The 1.12.1 helper preserves the original application failure. Retry a complete transaction only when its work is idempotent.

## Distributed objects and lock ownership

`RMap`, `RBucket`, `RLock`, and `RAtomicLong` are Redis object proxies and cannot outlive their client. Coroutines can move across threads, so `getLockId(lockName)` creates a Snowflake ID instead of using a thread ID.

Use the same ID for acquire and unlock, and release the lock from `finally`. Cancellation does not remove that requirement.

## Stream consumer-group helpers

`RStreamSupport` builds entries and groups ACK or claim operations. `ackAllAsync` validates the group and ID list. `claimAllAsync` returns message bodies; `fastClaimAllAsync` returns IDs only.

```kotlin
val claimed = stream.claimAllAsync(
    "billing", "worker-2", Duration.ofSeconds(30), pendingIds
).await()
processIdempotently(claimed)
stream.ackAllAsync("billing", claimed.keys).await()
```

Claim transfers ownership but does not create exactly-once processing. A crash between the business operation and ACK can redeliver the message, so processing must be idempotent.

## Source and tests

- [`RedissonClientSupport.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt)
- [`RedissonClientExtensions.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientExtensions.kt)
- [`RStreamSupport.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RStreamSupport.kt)
- [`RedissonClientExtensionsTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonClientExtensionsTest.kt)
- [`RStreamSupportTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RStreamSupportTest.kt)
