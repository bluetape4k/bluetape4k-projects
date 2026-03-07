# Module `bluetape4k-coroutines`

`bluetape4k-coroutines` provides higher-level coroutine utilities used across the Bluetape4k modules.

It focuses on:

- async value wrappers such as `DeferredValue`
- `Deferred` coordination helpers
- `Flow` extension operators
- reusable `CoroutineScope` implementations
- optional Reactor context lookup helpers

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-coroutines:${version}")
}
```

Optional integrations:

- Reactor helpers require `reactor-core` and `kotlinx-coroutines-reactor`
- Virtual-thread scope usage requires a runtime that supports virtual threads

## DeferredValue

Use `DeferredValue` when you want one eager async computation with both blocking and suspending access paths.

```kotlin
import io.bluetape4k.coroutines.deferredValueOf
import io.bluetape4k.coroutines.flatMap
import io.bluetape4k.coroutines.map
import kotlinx.coroutines.delay

val source = deferredValueOf {
    delay(100)
    21
}

val doubled = source.map { it * 2 }
val tripled = source.flatMap { deferredValueOf { it * 3 } }
```

Behavior notes:

- `await()` is the preferred API inside coroutines
- `value` blocks the calling thread until completion
- `map` / `flatMap` create new `DeferredValue` instances and do not mutate the source

## Deferred Helpers

Helpers for plain `Deferred` live in `io.bluetape4k.coroutines.support`.

```kotlin
import io.bluetape4k.coroutines.support.awaitAny
import io.bluetape4k.coroutines.support.awaitAnyAndCancelOthers
import io.bluetape4k.coroutines.support.zip
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

suspend fun deferredExample() = coroutineScope {
    val user = async { delay(20); "john" }
    val age = async { delay(10); 30 }

    val combined = zip(user, age) { name, years -> "$name:$years" }.await()
    val firstFinished = awaitAny(user, age)
    val winner = listOf(user, age).awaitAnyAndCancelOthers()

    Triple(combined, firstFinished, winner)
}
```

Behavior notes:

- `awaitAny(...)` returns or rethrows the earliest completed result
- `awaitAnyAndCancelOthers()` also cancels losers when the winner finishes with failure or cancellation
- `map`, `mapAll`, and `concatMap` derive new `Deferred` values from an existing one

## Flow Extensions

Flow operators live in `io.bluetape4k.coroutines.flow.extensions`.

If you need asynchronous processing while preserving upstream order, use `io.bluetape4k.coroutines.flow.async`.

```kotlin
import io.bluetape4k.coroutines.flow.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList

suspend fun orderedAsync() =
    (1..4).asFlow()
        .async(Dispatchers.IO) { it * 100 }
        .toList()
```

```kotlin
import io.bluetape4k.coroutines.flow.extensions.chunked
import io.bluetape4k.coroutines.flow.extensions.mapParallel
import io.bluetape4k.coroutines.flow.extensions.windowed
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList

suspend fun flowExample() {
    val chunks = (1..9).asFlow().chunked(3).toList()
    val windows = (1..5).asFlow().windowed(size = 3, step = 1).toList()
    val mapped = (1..4).asFlow().mapParallel(parallelism = 2) { it * 10 }.toList()
}
```

Useful entry points:

- `chunked`, `windowed`, `sliding`
- `mapParallel`
- `bufferUntilChanged`
- `takeUntil`, `skipUntil`
- `amb`, `race`, `withLatestFrom`
- `groupBy`, `publish`, `replay`

## CoroutineScope Implementations

```kotlin
import io.bluetape4k.coroutines.DefaultCoroutineScope
import io.bluetape4k.coroutines.IoCoroutineScope
import io.bluetape4k.coroutines.ThreadPoolCoroutineScope
import io.bluetape4k.coroutines.VirtualThreadCoroutineScope

val defaultScope = DefaultCoroutineScope()
val ioScope = IoCoroutineScope()
val poolScope = ThreadPoolCoroutineScope(poolSize = 4, name = "worker")
val vtScope = VirtualThreadCoroutineScope()
```

- `DefaultCoroutineScope`: `Dispatchers.Default + SupervisorJob`
- `IoCoroutineScope`: `Dispatchers.IO + SupervisorJob`
- `ThreadPoolCoroutineScope`: fixed-size pool with explicit `close()`
- `VirtualThreadCoroutineScope`: virtual-thread dispatcher backed scope

## Reactor Context Helpers

Reactor-specific helpers live in `io.bluetape4k.coroutines.reactor`.

```kotlin
import io.bluetape4k.coroutines.reactor.currentReactiveContext
import io.bluetape4k.coroutines.reactor.getOrNull

suspend fun traceId(): String? =
    currentReactiveContext()?.getOrNull("traceId")
```

These APIs read Reactor `Context`. They do not create Reactor publishers or bridge `Flow`/`Mono`/`Flux`.

## Representative Tests

- `src/test/kotlin/io/bluetape4k/coroutines/DeferredValueTest.kt`
- `src/test/kotlin/io/bluetape4k/coroutines/support/DeferredSupportTest.kt`
- `src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/MapParallelTest.kt`

Run the module tests with:

```bash
./gradlew :bluetape4k-coroutines:test
```
