---
title: Future and coroutine boundaries
description: Preserve failure and cancellation across RFuture, CompletableFuture, suspend batches, and suspend transactions.
manualId: bluetape4k-redisson
chapterId: future-coroutine-boundaries
---

# Future and coroutine boundaries

## Keep one execution model per call path

Redisson exposes synchronous and `RFuture` APIs. A coroutine service should call async methods and `await()` rather than mixing blocking `get()` calls. Coroutine syntax does not turn a synchronous Redis call into non-blocking I/O.

```kotlin
suspend fun loadProfiles(client: RedissonClient, ids: List<String>): List<Profile?> {
    val map = client.getMap<String, Profile>("profiles")
    return ids.map { map.getAsync(it) }.awaitAll()
}
```

`awaitAll()` returns an empty list for an empty collection, preserves input order, and propagates a failure instead of returning a partial list.

## Sequence and dispatcher choice

`Iterable<RFuture<V>>.sequence()` creates a `CompletableFuture<List<V>>` and defaults to the virtual-thread executor. `awaitAll()` uses the current coroutine dispatcher as its executor, falling back to `Dispatchers.Default`. These functions combine futures that already exist; they do not provide concurrency limits.

For large ID lists, chunk requests or use a Redisson bulk operation rather than creating an unbounded number of commands first.

## Suspend batch and transaction

`withSuspendedBatch` registers commands and awaits `executeAsync()`. Use async methods inside the DSL. It is a round-trip optimization, not a transaction.

`withSuspendedTransaction` awaits commit and, after failure, attempts async rollback before rethrowing the original failure. Cancellation raised while awaiting rollback remains cancellation.

```kotlin
client.withSuspendedTransaction {
    getMap<String, Long>("balances").putAsync("42", 1_000L)
}
```

Cancellation does not guarantee that Redis cancels a command already sent to the server. Retried operations need request IDs, compare-and-set, or another deduplication rule.

## Do not turn failures into misses

Converting every timeout, decode error, or cancellation to `null` makes an outage look like a normal cache miss. Even optional cache reads should distinguish miss from backend failure and emit a metric.

## Source and tests

- [`RFutureSupport.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/coroutines/RFutureSupport.kt)
- [`RedissonClientCoroutine.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/coroutines/RedissonClientCoroutine.kt)
- [`RFutureSupportTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/coroutines/RFutureSupportTest.kt)
- [`RedissonClientCoroutineTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/coroutines/RedissonClientCoroutineTest.kt)
- [`GetLockIdTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/coroutines/GetLockIdTest.kt)
