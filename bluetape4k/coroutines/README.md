# Module `bluetape4k-coroutines`

English | [한국어](./README.ko.md)

`bluetape4k-coroutines` provides higher-level coroutine utilities used across the Bluetape4k modules.

It focuses on:

- async value wrappers such as `DeferredValue`
- `Deferred` coordination helpers
- `Flow` extension operators
- reusable `CoroutineScope` implementations
- optional Reactor context lookup helpers

## Architecture

### Module Overview

![Module Overview 1](../../docs/images/readme-diagrams/bluetape4k-coroutines-diagram-01.png)

---

### Class Diagram

![Class Diagram 2](../../docs/images/readme-diagrams/bluetape4k-coroutines-diagram-02.png)

---

### DeferredValue Usage Flow

![DeferredValue Usage Flow diagram](../../docs/images/readme-diagrams/bluetape4k-coroutines-sequence-01.png)

---

## Key Features

- **DeferredValue**: Eager async computation wrapper with both suspend (`await()`) and blocking (`value`) access
- **Deferred Helpers**: `zip`, `awaitAny`, `awaitAnyAndCancelOthers` for coordinating multiple `Deferred` values
- **Flow Extensions**: Rich set of operators — batching, windowing, parallel mapping, throttling, gate control, merging
- **AsyncFlow**: Order-preserving async transformation using `Deferred` internally
- **CoroutineScope Implementations**: Ready-to-use scopes for Default, IO, ThreadPool, and Virtual Thread dispatchers
- **Reactor Context Helpers**: Read Reactor `Context` values from within coroutines

## Usage Examples

### DeferredValue

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

### Deferred Helpers

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
- parent coroutine cancellation is rethrown immediately; only a child `Deferred` cancellation is treated as a winner result
- `map`, `mapAll`, and `concatMap` derive new `Deferred` values from an existing one

### Flow Extensions

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

### Subjects

Subject implementations are hot `Flow` bridges for producer/collector coordination.

```kotlin
import io.bluetape4k.coroutines.flow.extensions.subject.BehaviorSubject
import io.bluetape4k.coroutines.flow.extensions.subject.PublishSubject
import io.bluetape4k.coroutines.flow.extensions.subject.awaitCollector
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

suspend fun subjectExample() = coroutineScope {
    val latest = BehaviorSubject(0)
    latest.emit(1)
    latest.complete()

    val events = PublishSubject<Int>()
    val collected = mutableListOf<Int>()
    val job = launch { events.take(2).toList(collected) }
    events.awaitCollector()
    events.emit(10)
    events.emit(20)
    job.join()
}
```

Behavior notes:

- `BehaviorSubject` stores the latest value and replays it to new collectors
- `PublishSubject` does not replay old values; it only sends values to current collectors
- `emit`, `emitError`, and `complete` preserve caller coroutine cancellation while ignoring cancellation from already-removed collectors

### CoroutineScope Implementations

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

### Structured Concurrency — StructuredTaskScope Bridge

`StructuredConcurrency.kt` bridges JDK `StructuredTaskScope` (virtual-thread structured concurrency) with Kotlin Coroutines, providing DSL-style suspend functions that run on `Dispatchers.VT`.

#### Suspend entry points

| Function | Behavior | Underlying |
|---|---|---|
| `taskScope { }` | Fail-fast: first failure cancels remaining tasks | `StructuredTaskScopes.failFast` |
| `failFastTaskScope { }` | Alias for `taskScope` with explicit name | `StructuredTaskScopes.failFast` |
| `firstSuccessTaskScope<T> { }` | First-success: returns earliest success, cancels rest | `StructuredTaskScopes.firstSuccess` |
| `supervisedTaskScope<T, R> { }` | Supervised: all tasks run; partial failures allowed | `StructuredTaskScopes.supervised` |

#### Async (non-blocking) entry points

| Function | Returns | Usage |
|---|---|---|
| `CoroutineScope.asyncTaskScope { }` | `Deferred<T>` | Parallel fail-fast scopes via `awaitAll` |
| `CoroutineScope.asyncSupervisedTaskScope<T, R> { }` | `Deferred<R>` | Parallel supervised scopes via `awaitAll` |

Inside each block, `this` is the scope — call `fork { }`, `join()`, `throwIfFailed()`, `results()`, etc. directly.

```kotlin
import io.bluetape4k.coroutines.taskScope
import io.bluetape4k.coroutines.firstSuccessTaskScope
import io.bluetape4k.coroutines.supervisedTaskScope
import io.bluetape4k.coroutines.asyncTaskScope

// Fail-fast: all tasks must succeed
val sum = taskScope {
    val a = fork { fetchA() }
    val b = fork { fetchB() }
    join().throwIfFailed()
    a.get() + b.get()
}

// First-success: fastest source wins
val result = firstSuccessTaskScope<String> {
    fork { fetchFromPrimary() }
    fork { fetchFromFallback() }
    join().result { IllegalStateException("all sources failed") }
}

// Supervised: partial failures tolerated
val allResults = supervisedTaskScope<Int, List<Result<Int>>> {
    fork { 1 }
    fork { throw RuntimeException("subtask failed") }
    fork { 3 }
    join()
    results()
}
// allResults.filter { it.isSuccess }.map { it.getOrThrow() } == [1, 3]

// Async: parallel fail-fast scopes
val d1 = asyncTaskScope { val t = fork { fetchA() }; join().throwIfFailed(); t.get() }
val d2 = asyncTaskScope { val t = fork { fetchB() }; join().throwIfFailed(); t.get() }
val (r1, r2) = awaitAll(d1, d2)
```

Behavior notes:

- All suspend variants run inside `withContext(Dispatchers.VT)` — blocking `join()` is offloaded to virtual threads
- `async` variants start immediately and return `Deferred<T>`; use `supervisorScope { }` in tests when expecting failures
- `joinUntil(deadline)` triggers `TimeoutException` on deadline breach

### Reactor Context Helpers

Reactor-specific helpers live in `io.bluetape4k.coroutines.reactor`.

```kotlin
import io.bluetape4k.coroutines.reactor.currentReactiveContext
import io.bluetape4k.coroutines.reactor.getOrNull

suspend fun traceId(): String? =
    currentReactiveContext()?.getOrNull("traceId")
```

These APIs read Reactor `Context`. They do not create Reactor publishers or bridge `Flow`/`Mono`/`Flux`.

## Flow Operator Diagrams

### 1. Flow Extension Categories

![1. Flow Extension Categories 3](../../docs/images/readme-diagrams/bluetape4k-coroutines-diagram-03.png)

---

### 2. `chunked(n)` — Fixed-Size Chunks

Groups input elements into `List`s of size `n`. Emits the final partial chunk as well (`partialWindow=true` by default).

![2. `chunked(n)` — Fixed-Size Chunks diagram](../../docs/images/readme-diagrams/bluetape4k-coroutines-sequence-02.png)

---

### 3. `windowed(size, step)` — Sliding Window

Emits windows of size `size`, advancing by `step` each time.

![3. `windowed(size, step)` — Sliding Window diagram](../../docs/images/readme-diagrams/bluetape4k-coroutines-sequence-03.png)

---

### 4. `sliding(n)` / `bufferedSliding(n)` — One-Step Sliding Window

`sliding` is equivalent to `windowed(size, step=1)`.
`bufferedSliding` maintains a buffer and emits a snapshot on every element.

![4. `sliding(n)` / `bufferedSliding(n)` — One-Step Sliding Window diagram](../../docs/images/readme-diagrams/bluetape4k-coroutines-sequence-04.png)

---

### 5. `mapParallel(parallelism)` — Parallel Transformation

Runs the transform function on up to `parallelism` elements concurrently. Result order is not guaranteed.

> **Performance (2026-04-21)**: `FlowParallel` and `FlowSequential` were redesigned with per-rail `Channel` buffers and a `select`-based fan-in. Benchmark results show a **+32.7% geomean** throughput gain across all parallel operators, with `mapParallel` showing up to **+506%** improvement. `AsyncFlow` also benefits from removing the `LazyDeferred` atomic wrapper.

![5. `mapParallel(parallelism)` — Parallel Transformation diagram](../../docs/images/readme-diagrams/bluetape4k-coroutines-sequence-05.png)

---

### 6. `concatMapEager { }` — Order-Preserving Eager Parallel Collection

Starts inner Flows eagerly and concurrently, but **emits results in source order**.

![6. `concatMapEager { }` — Order-Preserving Eager Parallel Collection diagram](../../docs/images/readme-diagrams/bluetape4k-coroutines-sequence-06.png)

---

### 7. `bufferingDebounce(timeout)` — Debounced Batching

Buffers values arriving within `timeout` and emits them together as a `List`. Resets the timeout on each new arrival.

![7. `bufferingDebounce(timeout)` — Debounced Batching diagram](../../docs/images/readme-diagrams/bluetape4k-coroutines-sequence-07.png)

---

### 8. `throttleLeading` / `throttleTrailing` / `throttleBoth` — Throttle

Within a fixed window, emits the first element (leading), last element (trailing), or both.

![8. `throttleLeading` / `throttleTrailing` / `throttleBoth` — Throttle diagram](../../docs/images/readme-diagrams/bluetape4k-coroutines-sequence-08.png)

---

### 9. `takeUntil(notifier)` / `skipUntil(notifier)` — Gate Control

![9. `takeUntil(notifier)` / `skipUntil(notifier)` — Gate Control diagram](../../docs/images/readme-diagrams/bluetape4k-coroutines-sequence-09.png)

---

### 10. `merge(flows)` — Unordered Merge

Collects multiple Flows concurrently and emits values in arrival order.

![10. `merge(flows)` — Unordered Merge diagram](../../docs/images/readme-diagrams/bluetape4k-coroutines-sequence-10.png)

---

### 11. `pairwise()` / `zipWithNext()` — Adjacent Pairs

Pairs adjacent elements as `Pair`, optionally applying a transform. `zipWithNext` is an alias for `pairwise`.

![11. `pairwise` / `zipWithNext` — Adjacent Pairs diagram](../../docs/images/readme-diagrams/bluetape4k-coroutines-sequence-11.png)

---

### 12. `scanWith(initial) { }` — Lazy Initial Value Accumulation

Calls `initialSupplier` at collect time to produce the seed, then emits each accumulated result.

![12. `scanWith(initial) { }` — Lazy Initial Value Accumulation diagram](../../docs/images/readme-diagrams/bluetape4k-coroutines-sequence-12.png)

---

### 13. `AsyncFlow` — Order-Preserving Async Transformation

Starts each element as a `Deferred` asynchronously, but **emits results in input order**. Unlike
`mapParallel`, output order is guaranteed.

![13. `AsyncFlow` — Order-Preserving Async Transformation diagram](../../docs/images/readme-diagrams/bluetape4k-coroutines-sequence-13.png)

---

## Representative Tests

- `src/test/kotlin/io/bluetape4k/coroutines/DeferredValueTest.kt`
- `src/test/kotlin/io/bluetape4k/coroutines/support/DeferredSupportTest.kt`
- `src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/MapParallelTest.kt`

Run the module tests with:

```bash
./gradlew :bluetape4k-coroutines:test
```

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-coroutines:${version}")
}
```

Optional integrations:

- Reactor helpers require `reactor-core` and `kotlinx-coroutines-reactor`
- Virtual-thread scope usage requires a runtime that supports virtual threads
