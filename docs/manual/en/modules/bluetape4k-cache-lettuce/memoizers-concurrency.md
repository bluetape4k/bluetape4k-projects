---
title: Redis memoizers and concurrency
description: Understand Redis sharing, JVM-local same-key coalescing, failure, and cancellation in sync, future, and suspend memoizers.
manualId: bluetape4k-cache-lettuce
chapterId: memoizers-concurrency
---

# Redis memoizers and concurrency

## What is shared

The three memoizers store function results in a `LettuceMap` or `LettuceSuspendMap`. The input key becomes a Redis field through `toString()`, so separate map names or key types that can produce the same string.

```kotlin
val connection = redisClient.connect(LettuceLongCodec)
val map = LettuceMap<Long>(connection, "pricing:factorial:v1")
val factorial = map.memoizer { n: Long -> computeFactorial(n) }

val first = factorial(10L)
val cached = factorial(10L)
```

Memoize only results that may be reused for the same key. Include tenant, permission, time window, or other result inputs in the key.

## Same-key work within one JVM

Each implementation keeps in-progress work in a `ConcurrentHashMap<K, ...>`. Concurrent callers for the same key in one JVM share the first future or deferred.

The `inFlight` map is not distributed. Two application instances can both evaluate a miss. `putIfAbsent` selects the Redis winner, and the loser reads the winning value.

```text
JVM A miss -> evaluate ----+-> putIfAbsent wins
JVM B miss -> evaluate ----+-> loses and reads winner
```

Use a distributed lock or source-system idempotency when duplicate evaluation itself is unsafe.

## Sync and asynchronous paths

`LettuceMemoizer` performs Redis lookup and evaluation on the first caller; local peers block on its `CompletableFuture`. `LettuceAsyncMemoizer` chains async Redis commands with a `CompletionStage` evaluator.

The async completion removes `inFlight.remove(key, promise)`, so an older completion cannot remove a newer promise installed by re-entry.

```kotlin
val squares = LettuceMap<Int>(intConnection, "squares:v1")
    .asyncMemoizer { n -> CompletableFuture.supplyAsync { n * n } }
check(squares(7).join() == 49)
```

The application still owns evaluator executors, timeouts, and the map connection.

## Suspend failure and cancellation

`LettuceSuspendMemoizer` shares a `CompletableDeferred`. Failure or cancellation completes waiting callers exceptionally and removes the key in `finally`. No failed result is stored, so the next call can evaluate again.

```kotlin
val profiles = suspendMap.suspendMemoizer { id: Long ->
    profileRepository.load(id)
}
```

Cancellation is rethrown rather than converted to a fallback value, preserving structured concurrency.

## Clear is not a generation barrier

Sync and async `clear()` remove in-flight entries and the Redis map, but an evaluator already running can later write again. The suspend implementation clears Redis but does not cancel in-flight work.

Do not use `clear()` as a strict generation switch. Version the Redis map name when the computation contract changes.

## Test checklist

- evaluator count for first miss and later hit
- concurrent same-key calls in one JVM
- acceptable duplicate work across JVMs
- retry after evaluator failure
- retry after suspend cancellation
- codec changes and string-key collisions

## Sources and tests

- [`LettuceMemoizer.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceMemoizer.kt)
- [`LettuceAsyncMemoizer.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceAsyncMemoizer.kt)
- [`LettuceSuspendMemoizer.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceSuspendMemoizer.kt)
- [`LettuceAsyncMemoizerTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/memoizer/LettuceAsyncMemoizerTest.kt)
- [`LettuceSuspendMemoizerTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/memoizer/LettuceSuspendMemoizerTest.kt)
