---
title: Concurrency and lifecycle
description: Bound ConcurrentReducer capacity and make ShutdownQueue ordering explicit.
manualId: bluetape4k-core
chapterId: concurrency-lifecycle
---

# Concurrency and lifecycle

## Problem

Limiting only concurrent calls to an asynchronous dependency still allows an unbounded waiting queue when producers are faster. Shutdown must reject new work, distinguish queued work from already-running external work, and close dependent resources in reverse order.

`ConcurrentReducer` bounds both **active capacity** and **queue capacity**. `ShutdownQueue` is a final JVM-exit safety net that closes process-wide resources in reverse registration order.

![ConcurrentReducer capacity and failure paths](../../../assets/core/concurrent-reducer-capacity.svg)

## ConcurrentReducer state model

| State/signal | Meaning |
| --- | --- |
| `activeCount` | Tasks currently holding permits |
| `queuedCount` | Accepted tasks not yet started |
| `remainingActiveCapacity` | Permits available to start immediately |
| `remainingQueueCapacity` | Additional tasks that may wait |
| closed | New submissions rejected and queued promises cancelled |

Both `maxConcurrency` and `maxQueueSize` must be positive. The implementation combines an `ArrayBlockingQueue`, a `Semaphore`, and a single-thread pump executor that advances work after completions.

## Complete usage example

```kotlin
import io.bluetape4k.concurrent.concurrentReducerOf
import java.util.concurrent.CompletionStage

fun <T> fetchAll(
    ids: List<String>,
    fetchAsync: (String) -> CompletionStage<T>,
): List<T> = concurrentReducerOf<T>(
    maxConcurrency = 8,
    maxQueueSize = 64,
).use { reducer ->
    val promises = ids.map { id -> reducer.add { fetchAsync(id) } }
    promises.map { it.join() }
}
```

Observe every promise returned by `add`. A full queue or closed reducer does not synchronously throw at the call site; it returns an already-failed `CompletableFuture`.

## Submission and completion contract

| Situation | Returned promise |
| --- | --- |
| Accepted into queue | Completes with task result/error |
| Queue full | Fails with `CapacityReachedException` |
| Reducer closed | Fails with `RejectedExecutionException` |
| Task lambda throws | Fails with the same error and releases permit |
| Task returns a `null` stage | Fails with `NullPointerException` |
| Caller cancels queued promise | Skipped before start and permit released |

`join()` can wrap the cause in `CompletionException`. Apply separate policy for overload rejection, task failure, and caller cancellation by inspecting the cause.

## What close guarantees

`close()` is idempotent. The first call marks the reducer closed, cancels queued promises with `cancel(false)`, and shuts down its pump executor. It cannot forcibly cancel an external `CompletionStage` that has already started because the reducer does not own that stage.

Design shutdown in this order:

1. Stop producers from creating new work.
2. Drain running work as required, or use the external client's cancellation API.
3. Close the reducer to cancel queued work.
4. Close the client/executor invoked by the reducer.

## ShutdownQueue's role

![Shutdown resources in reverse registration order](../../../assets/core/shutdown-order.svg)

`ShutdownQueue.register(closeable)` ignores duplicate registration of the same object. Its JVM shutdown hook uses `pollLast()` for LIFO closing. `closeSafe` prevents one close failure from blocking remaining cleanup.

```kotlin
val client = ExternalClient()
val service = Service(client)

ShutdownQueue.register(client)   // dependency first
ShutdownQueue.register(service)  // dependent wrapper later; closes first
```

Do not replace normal lifecycle ownership with this queue. Close directly in a Spring bean, request, or test-fixture lifecycle when an earlier deterministic boundary exists. Use `ShutdownQueue` only to protect against process-exit omissions.

## Choosing the mechanism

| Requirement | Choice |
| --- | --- |
| Bound active and waiting `CompletionStage` work | `ConcurrentReducer` |
| Bound only suspend-function concurrency | Coroutine semaphore or `mapParallel` |
| Durable delivery and retry | Broker or durable queue |
| Normal shutdown of component-owned resource | Direct close in component lifecycle |
| JVM-exit safety net for process-wide resource | `ShutdownQueue` |

## Operations and troubleshooting

- Track `activeCount`, `queuedCount`, queue-full rejection, and task failure separately.
- Queue capacity is a hidden latency budget. Estimate worst wait from `maxQueueSize / throughput`.
- Record producer-stop, queued-cancellation, running-drain, and dependency-close times separately during shutdown.
- Unbounded retry on a full queue amplifies overload. Define a retry budget, backoff, and caller-visible 429/503 policy.

## Source and representative tests

- [`ConcurrentReducer.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/ConcurrentReducer.kt)
- [`ConcurrentReducerTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/concurrent/ConcurrentReducerTest.kt)
- [`ShutdownQueue.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/utils/ShutdownQueue.kt)
- [`ShutdownQueueTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/utils/ShutdownQueueTest.kt)

Continue with [Bounded collections](./bounded-collections.md) for process-local state and [Core recipes](./recipes.md) for the assembled flow.
