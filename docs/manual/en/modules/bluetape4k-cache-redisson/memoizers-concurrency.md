---
title: Memoizers and same-key work sharing
description: Compare cache misses, contention, failure, and cancellation across sync, future, and suspend memoizers.
manualId: bluetape4k-cache-redisson
chapterId: memoizers-concurrency
---

# Memoizers and same-key work sharing

## Completed values versus in-flight work

Redisson memoizers store completed values in `RMap`. A `ConcurrentHashMap` coordinates in-flight evaluation inside each JVM. Sixteen coroutines in one process can share one evaluation, but two JVMs can still evaluate the same first miss independently.

```kotlin
val squares = redissonClient
    .getMap<Int, Int>("memoizer:squares", IntegerCodec())
    .suspendMemoizer { key -> expensiveSquare(key) }

val value = squares(7)
```

This is not a distributed lock. Use locking, fencing, and idempotency when evaluation must happen only once across the cluster.

## Choose the evaluator shape

| API | Evaluator | Use it for |
| --- | --- | --- |
| `memoizer` | `(T) -> R` | CPU work or an already-blocking call path |
| `asyncMemoizer` | `CompletionStage<R>` | Java future-based services |
| `suspendMemoizer` | `suspend (T) -> R` | Coroutine services |

All three use `putIfAbsent` to handle Redis races. Sync and suspend variants return the stored winner. The 1.12.1 async path completes with its evaluated value even if another process won the Redis `putIfAbsent`, so cross-process callers can temporarily observe different return values.

## Recovery after failure or cancellation

Evaluator failure completes the promise/deferred exceptionally and removes the in-flight entry. A later call starts a fresh evaluation. The suspend variant also preserves `CancellationException` and does not store it as a successful value.

Cancelling one waiting coroutine does not automatically cancel the shared deferred. Cancelling the owner evaluation removes the entry and lets the next call retry.

## Clear races

Sync and suspend `clear()` remove Redis values. Async `clear()` also clears local in-flight entries. Concurrent clear and evaluation can allow a completing operation to write again or a new caller to start duplicate work. Coordinate operational clears or use generation-based names.

Memoizers belong around repeatable reads and calculations, not payments or message delivery that cannot safely run twice.

## Source and tests

- [`RedissonMemoizer.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonMemoizer.kt)
- [`RedissonAsyncMemoizer.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonAsyncMemoizer.kt)
- [`RedissonSuspendMemoizer.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonSuspendMemoizer.kt)
- [`RedissonSuspendMemoizerTest.kt`](../../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/memoizer/RedissonSuspendMemoizerTest.kt)
