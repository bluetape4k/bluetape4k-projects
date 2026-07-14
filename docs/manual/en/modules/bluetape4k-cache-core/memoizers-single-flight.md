---
title: Memoizers and same-key computation
description: Explain how sync, CompletableFuture, and suspend memoizers reuse completed and in-flight work across failures and clear operations.
manualId: bluetape4k-cache-core
chapterId: memoizers-single-flight
---

# Memoizers and same-key computation

## A cache that still looks like a function

A memoizer uses function input as its cache key. It fits functions that are pure or return results that remain valid for the accepted stale window.

```kotlin
val summary = (suspend { userId: Long ->
    userRepository.loadSummary(userId)
}).suspendMemoizer()

val first = summary(42L)
val second = summary(42L)
```

`Memoizer`, `AsyncMemoizer`, and `SuspendMemoizer` cover blocking values, `CompletableFuture`, and suspend results. Implementations exist for in-memory storage, Caffeine, Cache2k, Ehcache, and JCache.

## Cached results and in-flight results

Reusing a completed result differs from sharing work that is still running. `SingleFlight` merges same-key in-flight evaluation. Blocking callers wait on a latch, async callers receive the same future, and suspend callers await the same `CompletableDeferred`.

```text
caller A ─┐
caller B ─┼─ key=42 ─ one evaluator ─ cached result
caller C ─┘
```

Different keys run independently; there is no global lock that serializes every miss.

## Failure and cancellation are not cache values

Failed or cancelled evaluators are removed from the in-flight map, allowing the next call to recompute.

```kotlin
var attempts = 0
val memo = (suspend { key: String ->
    attempts += 1
    if (attempts == 1) error("temporary failure")
    key.length
}).suspendMemoizer()

runCatching { memo("recover") }
check(memo("recover") == 7)
```

An async evaluator completing with `null` fails with `NullPointerException`; memoizer keys and results are non-null.

## `clear()` and running work

`clear()` removes cached values and increments the `SingleFlight` generation. A caller whose evaluation began before clear may still receive its result, but that old result is not written back after clear. This matters for tenant removal, permission changes, and configuration updates.

## Selecting an implementation

- Use an in-memory memoizer for small process-local functions.
- Use a Caffeine memoizer for capacity, expiry, and statistics.
- Reuse JCache when it is already an application boundary, while noting sync `getOrPut` may duplicate evaluation.
- For remote sharing, review serialization and the selected provider's memoizer contract.

## Sources and tests

- [`SingleFlight.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/memoizer/SingleFlight.kt)
- [`InMemorySuspendMemoizer.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/memoizer/inmemory/InMemorySuspendMemoizer.kt)
- [`CaffeineSuspendMemoizer.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/memoizer/caffeine/CaffeineSuspendMemoizer.kt)
- [`SingleFlightTest.kt`](../../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/memoizer/SingleFlightTest.kt)
- [`AbstractSuspendMemoizerTest.kt`](../../../../../cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/memoizer/AbstractSuspendMemoizerTest.kt)
