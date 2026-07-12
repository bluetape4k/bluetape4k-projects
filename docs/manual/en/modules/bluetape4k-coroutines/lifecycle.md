---
title: Lifecycle and cancellation
description: Design scope ownership, cancellation propagation, and dispatcher shutdown as one contract.
manualId: bluetape4k-coroutines
chapterId: lifecycle
---

# Lifecycle and cancellation

Where a coroutine ends matters more than where it starts. Moving request work into a component scope loses caller cancellation; forgetting to close a component-owned dispatcher leaks threads.

![Creation, child cancellation, and dispatcher shutdown order for CloseableCoroutineScope](../../../assets/coroutines/scope-lifecycle.svg)

## The two ownership models

| Owner | Default choice | Termination boundary |
| --- | --- | --- |
| HTTP request, message handler, CLI command | caller scope and `coroutineScope` | return or caller cancellation |
| component that survives across calls | a `CloseableCoroutineScope` implementation | component `close()` or framework shutdown |

A request that only combines a few calls does not need a new scope.

```kotlin
suspend fun loadDashboard(id: String): Dashboard = coroutineScope {
    val profile = async { profileClient.load(id) }
    val notices = async { noticeClient.load(id) }
    Dashboard(profile.await(), notices.await())
}
```

The children are cancelled with the caller and none can outlive the function.

## The closeable-scope contract

`CloseableCoroutineScope` guards `_closed` and `_cancelled` with atomic compare-and-set operations.

1. The first `close()` changes `scopeClosed` to true.
2. `clearJobs()` calls `cancelChildren(cause)` first.
3. It then cancels the scope context itself.
4. Later close or clear calls cannot reopen or repeat that state transition.

`DefaultCoroutineScope` owns `Dispatchers.Default + SupervisorJob()`. Supervision isolates sibling failures during normal operation, while owner close still cancels every child.

```kotlin
class ThumbnailWorker : AutoCloseable {
    private val scope = DefaultCoroutineScope()

    fun submit(imageId: String): Job = scope.launch {
        thumbnailService.generate(imageId)
    }

    override fun close() = scope.close()
}
```

## Owning a dispatcher

`ThreadPoolCoroutineScope(poolSize, name)` validates a positive pool size and combines a fixed executor dispatcher with a `SupervisorJob`. Cancelling coroutines is not enough; the executor must also be closed.

```kotlin
class BlockingAdapter : AutoCloseable {
    private val scope = ThreadPoolCoroutineScope(poolSize = 4, name = "legacy-io")

    suspend fun <T> call(block: () -> T): T =
        withContext(scope.coroutineContext) { block() }

    override fun close() = scope.close()
}
```

Its override calls the parent close and then closes the dispatcher behind a second CAS guard. A non-positive pool size fails at construction.

## Preserve cancellation

Cancellation is a structured control signal, not an ordinary application failure.

```kotlin
try {
    remoteClient.load()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    fallback(e)
}
```

A cause passed to `clearJobs(CancellationException("shutdown"))` reaches children at their next suspension point. The representative test also verifies that the parent `Job` becomes inactive.

## Testing owned resources

Fixtures that create dispatchers must close them on every path.

```kotlin
private val scopeLazy = lazy { ThreadPoolCoroutineScope(poolSize = 2) }

@AfterEach
fun closeScope() {
    if (scopeLazy.isInitialized()) scopeLazy.value.close()
}
```

`use {}` is equally valid. The invariant is that success, failure, and assertion failure all cross the same close boundary.

## Troubleshooting

| Symptom | Boundary to inspect | Action |
| --- | --- | --- |
| work continues after a request | child escaped into application/component scope | launch inside caller scope |
| threads remain after shutdown | owner of fixed or virtual dispatcher | connect owner close to shutdown |
| one child failure cancels all work | `Job` versus `SupervisorJob` policy | supervise only independent results |
| remote I/O survives a timeout | client cancellation bridge | add client deadline and idempotency rules |

## Source and verification

- [`CloseableCoroutineScope.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/CloseableCoroutineScope.kt)
- [`DefaultCoroutineScope.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/DefaultCoroutineScope.kt)
- [`ThreadPoolCoroutineScope.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/ThreadPoolCoroutineScope.kt)
- [`AbstractCoroutineScopeTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/AbstractCoroutineScopeTest.kt)

Next: define winner and loser-cleanup semantics in [Deferred coordination](./deferred.md).
