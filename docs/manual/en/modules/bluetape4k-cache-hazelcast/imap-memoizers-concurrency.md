---
title: IMap memoizers and concurrency
description: Compare sync, CompletableFuture, and coroutine execution with JVM-local same-key coalescing and cluster races.
manualId: bluetape4k-cache-hazelcast
chapterId: imap-memoizers-concurrency
---

# IMap memoizers and concurrency

## Three APIs share the same storage pattern

Each memoizer reads an `IMap`, evaluates on a miss, and stores with `putIfAbsent`. If another JVM has already stored a value, that value wins.

```kotlin
val scores: IMap<String, Score> = hazelcast.getMap("scores-v1")

val sync = scores.memoizer { key -> calculateScore(key) }
val future = scores.asyncMemoizer { key -> calculateScore(key) }
val suspend = scores.suspendMemoizer { key -> calculateScoreSuspend(key) }
```

The synchronous evaluator runs on the calling thread. The async evaluator runs in `CompletableFuture.supplyAsync`'s default executor. The suspend evaluator runs in the calling coroutine. Make CPU and blocking expectations explicit inside the evaluator.

## Single-flight is local to one JVM

Each memoizer instance keeps a `ConcurrentHashMap<K, ...>` named `inFlight`. Release tests verify one evaluator call for concurrent requests to the same key in one JVM.

Another JVM has another map, so this is not cluster-wide single-flight. Two members can evaluate the same miss, after which `IMap.putIfAbsent` selects one result. Evaluators with side effects must tolerate duplicate execution or use a stronger cluster coordination boundary.

## Failures allow the next call to retry

Sync, async, and suspend implementations remove failed in-flight entries. Async and suspend tests verify that the first failed evaluation is followed by a successful re-evaluation.

Failures are not cached. A hot key that keeps failing can repeatedly hit the source, so apply retry and concurrency limits outside the memoizer.

## Clear does not cancel active evaluation

`clear` calls `IMap.clear()`. The async implementation also clears its local in-flight map but does not cancel futures already running. Sync and suspend implementations do not explicitly clear their in-flight maps. An evaluator completing after clear can store a value again.

For a live schema migration or force purge, switching to a new map name avoids this race more reliably.

## Sources and tests

- [`HazelcastMemoizer.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastMemoizer.kt)
- [`HazelcastAsyncMemoizer.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastAsyncMemoizer.kt)
- [`HazelcastSuspendMemoizer.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastSuspendMemoizer.kt)
- [`HazelcastAsyncMemoizerTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/memoizer/HazelcastAsyncMemoizerTest.kt)
- [`HazelcastSuspendMemoizerTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/memoizer/HazelcastSuspendMemoizerTest.kt)
