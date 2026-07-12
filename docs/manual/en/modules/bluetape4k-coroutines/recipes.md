---
title: Recipes and workshops
description: Assemble ownership, failure policy, ordering, and capacity into complete executable scenarios.
manualId: bluetape4k-coroutines
chapterId: recipes
---

# Recipes and workshops

These recipes close ownership, child policy, ordering, capacity, and cleanup inside one scenario rather than listing APIs in isolation.

## Recipe 1: parallel request composition

```kotlin
suspend fun dashboard(userId: String): Dashboard = coroutineScope {
    val profile = async { profileClient.load(userId) }
    val alerts = async { alertClient.load(userId) }
    Dashboard(profile.await(), alerts.await())
}
```

- owner: request caller;
- failure: atomic result, both dependencies required;
- cleanup: caller cancellation reaches both children;
- signals: dependency latency and total request latency.

Do not move these children into a separate `DefaultCoroutineScope`.

## Recipe 2: fastest completion versus first success

```kotlin
suspend fun fastestCache(key: String): Value = coroutineScope {
    listOf(
        async { localCache.get(key) },
        async { regionalCache.get(key) },
    ).awaitAnyAndCancelOthers()
}

suspend fun firstAvailable(key: String): Value = firstSuccessTaskScope {
    fork { primary.get(key) }
    fork { fallback.get(key) }
    join().result { cause -> ValueUnavailable(key, cause) }
}
```

The first function accepts a fast failure as winner. The second skips failures until success. Idempotency is a prerequisite for racing side effects.

## Recipe 3: ordered response versus throughput work

```kotlin
val response = ids.asFlow()
    .async(Dispatchers.IO) { catalog.load(it) }
    .toList() // input order

events.asFlow()
    .mapParallel(parallelism = databasePoolSize / 2) { repository.store(it) }
    .collect() // completion order may differ
```

Do not force public response ordering and background-work capacity through the same operator.

## Recipe 4: callback bridge

```kotlin
coroutineScope {
    val subject = PublishSubject<Event>()
    val collector = launch { subject.collect(handler) }
    subject.awaitCollector()

    val registration = source.register(
        onEvent = { event -> launch { subject.emit(event) } },
        onError = { error -> launch { subject.emitError(error) } },
    )
    try {
        collector.join()
    } finally {
        registration.close()
        collector.cancelAndJoin()
    }
}
```

Adapt callback-thread scheduling and ownership to the real integration. The invariants are registration before first emission and source cleanup in `finally`.

## Workshop map

| Learning goal | Workshop | Manual chapter |
| --- | --- | --- |
| parallel Flow enrichment | [`kotlin/flow-extensions-parallel-enrichment`](https://github.com/bluetape4k/bluetape4k-workshop/tree/develop/kotlin/flow-extensions-parallel-enrichment) | [Flow](./flow.md) |
| race versus fallback | [`kotlin/flow-extensions-race-fallback`](https://github.com/bluetape4k/bluetape4k-workshop/tree/develop/kotlin/flow-extensions-race-fallback) | [Deferred](./deferred.md) |
| Subject delivery comparison | [`kotlin/flow-extensions-subject-bridge`](https://github.com/bluetape4k/bluetape4k-workshop/tree/develop/kotlin/flow-extensions-subject-bridge) | [Subjects](./subjects.md) |
| web request lifecycle | [`spring-boot/webflux-coroutines`](https://github.com/bluetape4k/bluetape4k-workshop/tree/develop/spring-boot/webflux-coroutines) | [Lifecycle](./lifecycle.md) |
| trace and metric propagation | [`observability/micrometer-tracing-coroutines`](https://github.com/bluetape4k/bluetape4k-workshop/tree/develop/observability/micrometer-tracing-coroutines) | [Operations](./operations.md) |

Workshops are executable companions, not the source of truth. This manual and current library source define API and lifecycle contracts; workshops are where you vary inputs and observe those contracts.

## Suggested exercise order

1. run the happy path and record output order;
2. make the fastest branch fail;
3. cancel the collector/caller mid-flight;
4. exceed downstream capacity with parallelism and buffering;
5. verify jobs and threads converge to zero after shutdown.

Return to the [Coroutine and Flow extensions](../bluetape4k-coroutines.md) decision map.
