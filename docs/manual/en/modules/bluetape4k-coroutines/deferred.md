---
title: Deferred coordination
description: Implement first completion, first success, and loser cancellation as distinct policies.
manualId: bluetape4k-coroutines
chapterId: deferred
---

# Deferred coordination

“The fastest task” is ambiguous. The first task to **finish** may have failed; the first task to **succeed** must produce a value. Whether losers are cancelled is a third decision.

![Comparison of awaitAny, awaitAnyAndCancelOthers, and firstSuccessTaskScope](../../../assets/coroutines/deferred-race-policy.svg)

## Decision table

| Requirement | API | First failure | Losers |
| --- | --- | --- | --- |
| observe the first terminal result | `awaitAny` | propagate | continue |
| accept the first terminal result and reclaim capacity | `awaitAnyAndCancelOthers` | propagate | cancel |
| skip failed replicas and accept first success | `firstSuccessTaskScope` | wait for others | cancel after winner |
| require both results | `zip` / `zipWith` | propagate | parent policy |

Empty input is invalid. A single element has the same result as awaiting that deferred directly.

## First completion with cleanup

Start every request in the same caller scope; the coordinator does not create a long-lived owner.

```kotlin
suspend fun <T> fastestReplica(
    replicas: List<suspend () -> T>,
): T = coroutineScope {
    replicas
        .map { load -> async { load() } }
        .awaitAnyAndCancelOthers()
}
```

The implementation wraps each await in an indexed signal, selects the first signal to finish, and attempts to cancel every other signal and original deferred before returning or rethrowing the winner result.

- first success: return its value and cancel losers;
- first failure: throw it and cancel losers;
- first cancelled task: propagate cancellation and cancel losers;
- caller cancellation: `ensureActive()` prevents it from being mistaken for an ordinary candidate result.

## First successful result

Skipping failed replicas requires the structured first-success policy.

```kotlin
suspend fun loadFromAnyProvider(): Quote = firstSuccessTaskScope {
    fork { primary.loadQuote() }
    fork { secondary.loadQuote() }
    fork { archive.loadQuote() }
    join().result { cause ->
        QuoteUnavailable("all providers failed", cause)
    }
}
```

Remaining tasks are cleaned up after a winner. If every task fails, the `result` mapper creates the domain exception.

## Owning DeferredValue

`DeferredValue` starts immediately in its own `DefaultCoroutineScope`, so its owner must close it.

```kotlin
suspend fun loadAnswer(): Int {
    val value = deferredValueOf { repository.loadAnswer() }
    return try {
        value.await()
    } finally {
        value.close()
    }
}
```

Use `await()` in coroutine code. Blocking `value` access on a constrained dispatcher can starve or deadlock that pool.

## Transform and combine

`Deferred.map`, `mapAll`, `concatMap`, and `zipWith` represent the transformed result as another deferred. Source failure, source cancellation, and transform exceptions propagate unchanged.

```kotlin
val profile = async { loadProfile(id) }
val quota = async { loadQuota(id) }
val summary = profile.zipWith(quota) { p, q -> Summary(p, q) }
return summary.await()
```

The helper does not transfer ownership: the scope that started the source deferreds still owns their lifecycle.

## Operational checks

| Signal | Why |
| --- | --- |
| winner latency and identity | detect replicas that fail fastest |
| loser cancellation duration | find I/O that ignores cancellation |
| in-flight bound per race | prevent retry-times-race amplification |
| all-failed ratio | ensure first-success is not hiding an outage |

## Source and representative tests

- [`DeferredSupport.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/support/DeferredSupport.kt)
- [`DeferredValue.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/DeferredValue.kt)
- [`DeferredSupportTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/support/DeferredSupportTest.kt)
- [`StructuredConcurrencyTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/StructuredConcurrencyTest.kt)

For ordering and concurrency across stream items, continue with [Ordered and parallel Flow](./flow.md).
