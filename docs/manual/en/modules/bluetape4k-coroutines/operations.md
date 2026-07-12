---
title: Operations and observability
description: Observe in-flight work, pressure, latency, cancellation, readiness, and shutdown as one lifecycle.
manualId: bluetape4k-coroutines
chapterId: operations
---

# Operations and observability

Coroutine count alone does not explain behavior. Observe demand, pressure, service time, cancellation reason, and resource ownership at the same boundary.

![Minimum operational signals and deterministic shutdown for a coroutine boundary](../../../assets/coroutines/observability-boundaries.svg)

## Minimum signal set

| Signal | Question | Useful dimensions |
| --- | --- | --- |
| in-flight jobs/workers | how much demand is active | operation, owner |
| queue/buffer depth | is production faster than downstream | component, capacity |
| latency P50/P95/P99 | where is tail latency created | operation, outcome |
| cancellation | caller, timeout, or shutdown | reason, owner |
| shutdown duration | time from intake stop to release | component, phase |

Do not put request IDs or exception messages in metric labels. Keep metrics bounded and use traces/logs for high-cardinality details.

## Do not count cancellation as failure

```kotlin
suspend fun <T> Timer.recordSuspend(block: suspend () -> T): T {
    val sample = Timer.start(registry)
    try {
        return block()
    } catch (e: CancellationException) {
        cancellationCounter.increment()
        throw e
    } catch (e: Exception) {
        failureCounter.increment()
        throw e
    } finally {
        sample.stop(this)
    }
}
```

Separate caller cancellation from timeout to distinguish abandonment from service latency. Do not turn normal cancellation into an error span, but record its reason and owner.

## Readiness versus liveness

- Readiness asks whether new work can be accepted safely.
- Liveness asks whether the process can recover.

A saturated connection pool or permanently full queue may require throttling intake or dropping readiness. CPU utilization alone is insufficient.

## Deterministic shutdown

1. turn readiness off;
2. stop listeners, consumers, and other intake;
3. drain bounded work within an explicit timeout;
4. terminally close channels/subjects, owned scopes, and dispatchers;
5. record remaining jobs, threads, and resources.

```kotlin
override fun close() = runBlocking {
    accepting.set(false)
    withTimeoutOrNull(shutdownTimeout) { pendingJobs.joinAll() }
    subject.complete()
    workerScope.close()
}
```

Review which thread calls `runBlocking` and the framework shutdown deadline. Never wait for a dispatcher from work running on that same constrained dispatcher.

## Troubleshooting order

| Symptom | First signal | Next check |
| --- | --- | --- |
| rising latency | queue wait and downstream latency | parallelism/capacity mismatch |
| rising timeout | in-flight and retry count | retry-times-race amplification |
| slow shutdown | remaining jobs and dispatcher threads | owner/close path |
| first event missing | collector count and registration time | Subject startup contract |
| cancellation pollutes errors | exception classification | rethrow `CancellationException` |

## Source and verification anchors

- lifecycle: [`CloseableCoroutineScope.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/CloseableCoroutineScope.kt)
- Flow pressure: [`AsyncFlow.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/AsyncFlow.kt)
- Subject cancellation: [`SubjectCancellationTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/SubjectCancellationTest.kt)

Finally, assemble the contracts into runnable scenarios in [Recipes and workshops](./recipes.md).
