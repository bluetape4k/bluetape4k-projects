---
manualId: bluetape4k-core
title: "Core Kotlin Library"
description: Validation, codecs, collections, ranges, time DSLs, reflection helpers, and small concurrency primitives used across bluetape4k.
kind: library
group: foundation
learningOrder: 110
---

# Core Kotlin Library

## Problem {#problem}

Backend modules repeatedly need the same low-level contracts: parameter validation with consistent exception types, byte-safe encoders, bounded collections, date/time helpers, and Kotlin-friendly adapters around Java or Apache Commons APIs. `bluetape4k-core` centralizes those primitives so higher modules do not invent slightly different versions.

## When to use {#when-to-use}

Add core when application code or another library needs several of its foundational types. Prefer a focused JDK/Kotlin expression when it is already clear and complete; core is most valuable when it establishes a shared contract used across modules. Do not pull in core merely for one trivial alias without checking the dependency cost.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-core")
}
```

The repository compiles core with the default Java/JVM 25 toolchain and Kotlin 2.4. Core is an API dependency of several higher bluetape4k modules.

## Concepts {#concepts}

The module is a toolbox rather than one runtime subsystem. Its main families are `support` validation/extensions, `codec` encoders, `collections` bounded and paginated containers, `range` value types, `concurrent` helpers, `functional` adapters, `time` DSLs, and reflection/Apache Commons bridges.

Validation names encode failure semantics: new `require*` helpers reject caller input with `IllegalArgumentException`. Collection capacity is part of the type contract; for example, `BoundedStack` and `RingBuffer` retain only a bounded working set.

## Manual map {#manual-map}

Read Core in the order boundaries move through an execution flow rather than alphabetically by API name.

![Core boundary validation map](../../assets/core/validation-boundary.svg)

| Design question | Chapter | Contract to decide |
| --- | --- | --- |
| Where should invalid caller input stop? | [Validation and invariants](./bluetape4k-core/validation.md) | Exception type, parameter name, non-null internal model |
| How should bytes cross a text boundary? | [Encoding and data boundaries](./bluetape4k-core/encoding-data.md) | Charset, URL-safe Base64/Hex, malformed input |
| In what order should the latest N values be retained? | [Bounded collections](./bluetape4k-core/bounded-collections.md) | Capacity, eviction, stack/ring read order |
| Does a time query include its end? | [Time and ranges](./bluetape4k-core/time-ranges.md) | Endpoint inclusion, overlap, timezone |
| How much active and waiting work is allowed? | [Concurrency and lifecycle](./bluetape4k-core/concurrency-lifecycle.md) | Rejection, cancellation, close order |
| How do these contracts form one component? | [Practical Core recipes](./bluetape4k-core/recipes.md) | End-to-end tests and operational signals |

## Quick start {#quick-start}

```kotlin
import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.support.requireNotBlank

fun tokenFor(userId: String?): String {
    val id = userId.requireNotBlank("userId")
    return id.encodeBase64String()
}
```

`requireNotBlank` returns the validated value, so validation can stay inside an expression without a second null assertion.

## API by task {#api-by-task}

| Task | Start with |
| --- | --- |
| Validate nullable strings and caller arguments | `io.bluetape4k.support.RequireSupport` extensions |
| Encode Base64, Base58, Base62, Hex, or URL62 | `io.bluetape4k.codec` |
| Keep a bounded LIFO or circular window | `BoundedStack`, `RingBuffer` |
| Represent pages or lazy permutations | `PaginatedList`, permutation extensions |
| Express open/closed range endpoints | `io.bluetape4k.range` types |
| Compose duration, period, temporal, or quarter operations | `io.bluetape4k.time` |
| Reduce work concurrently with explicit close semantics | `ConcurrentReducer` |

## Choosing the first abstraction {#decision-guide}

| Requirement | Prefer | Do not choose it when |
| --- | --- | --- |
| Validate nullable input and keep using the same value | `requireNotNull`, `requireNotBlank`, and `requirePositiveNumber` families | You are checking an internal invariant. These functions represent caller errors with `IllegalArgumentException`. |
| Retain recent values in LIFO order | `BoundedStack` | Use `RingBuffer` when values must replay chronologically. |
| Retain the latest N values in insertion order | `RingBuffer` | Use `BoundedStack` when top/pop semantics matter. |
| Bound both concurrent asynchronous requests and their waiting queue | `ConcurrentReducer` | A coroutine semaphore or `mapParallel` is more natural when only suspend-function concurrency needs a bound. |
| Close global resources in reverse order during JVM shutdown | `ShutdownQueue` | Close directly when a request, bean, or component lifecycle ends earlier. |

## Practical recipes {#recipes}

### 1. Validate at the boundary and keep internal types simple

```kotlin
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber

data class PageRequest(val cursor: String, val size: Int)

fun pageRequest(cursor: String?, size: Int): PageRequest =
    PageRequest(
        cursor = cursor.requireNotBlank("cursor"),
        size = size.requirePositiveNumber("size"),
    )
```

Each `require*` function returns its validated receiver, so downstream code needs neither another nullable branch nor `!!`. Match the parameter name to the public API so an operational error identifies the invalid input immediately.

### 2. Choose bounded-collection order deliberately

```kotlin
val undo = BoundedStack<String>(maxSize = 3)
undo.pushAll("v1", "v2", "v3", "v4")
undo.toList() // [v4, v3, v2] — top to bottom

val recent = RingBuffer<String>(maxSize = 3)
recent.addAll("v1", "v2", "v3", "v4")
recent.toList() // [v2, v3, v4] — oldest to newest
```

Both types discard the oldest value after reaching capacity, but their read directions differ. `BoundedStack.pop()` removes the newest value; `RingBuffer.drop(n)` removes the oldest values. Both are process-local structures, not durable queues or distributed backpressure mechanisms.

### 3. Put an explicit capacity boundary in front of an asynchronous API

```kotlin
import io.bluetape4k.concurrent.concurrentReducerOf
import java.util.concurrent.CompletionStage

fun <T> fetchBounded(
    ids: List<String>,
    fetchAsync: (String) -> CompletionStage<T>,
): List<T> = concurrentReducerOf<T>(
    maxConcurrency = 8,
    maxQueueSize = 64,
).use { reducer ->
    ids.map { id -> reducer.add { fetchAsync(id) } }
        .map { promise -> promise.join() }
}
```

When the queue is full or the reducer is closed, `add` does not throw directly at the call site. It returns a `CompletableFuture` completed with `CapacityReachedException` or `RejectedExecutionException`, respectively. Always observe the returned promise. `close()` cancels queued jobs, but it does not forcibly interrupt an external `CompletionStage` that is already running.

## Patterns {#patterns}

Validate at the public boundary and pass non-null values inward. Keep codecs at transport/storage boundaries instead of scattering encoding through domain logic. Choose bounded collections when unbounded growth would turn backpressure into an out-of-memory failure. Close lifecycle-owning concurrency helpers in `use`/`try-finally` blocks.

## Integrations {#integrations}

Core wraps or complements Kotlin stdlib, Java time/reflection/concurrency, Apache Commons, Eclipse Collections, and hashing utilities selected by the module build. Higher bluetape4k modules expose core types in their public APIs, so applications may receive core transitively; declare it directly when source code imports its API.

## Configuration {#configuration}

There is no global configuration file. Behavior is selected by constructor arguments and function parameters such as collection capacity, charset, range boundary, or timeout. Keep those values near the owning component rather than hiding them in unrelated global state.

## Failures {#failures}

Validation helpers throw `IllegalArgumentException` for invalid caller input. Codec decoders propagate malformed-input errors according to the underlying codec. Bounded collections reject invalid capacities during construction. `ConcurrentReducer.close()` cancels queued work and rejects submissions after closure; callers must decide whether cancellation is an expected shutdown path or an error.

### Troubleshooting table

| Symptom | Check first | Response |
| --- | --- | --- |
| A validation error reports an unexpected value | Whether an earlier chain step transformed the receiver and whether the parameter name is correct | Validate the original boundary value once, then transform it. |
| Recent items appear in reverse order | `BoundedStack.toList()` is newest-first while `RingBuffer.toList()` is oldest-first | Distinguish undo from history and select the matching type. |
| A `ConcurrentReducer` promise fails through `CompletionException` | Whether the cause is `CapacityReachedException`, a task error, or a null stage | Map queue saturation to an overload policy such as retry/429 separately from task failure. |
| Producers keep submitting after shutdown | Whether `RejectedExecutionException` in returned promises is ignored | Observe every future, stop producers, and only then close the reducer. |
| Resource shutdown order is wrong | `ShutdownQueue` closes in reverse registration order (LIFO) | Register dependencies first and wrappers that use them afterward. |

## Operations {#operations}

Most helpers are allocation-only and own no background service. Pay attention to utilities that wrap executors, queues, or large buffers. Bound capacities from workload evidence, expose close/shutdown in the owning service lifecycle, and avoid using reflection helpers on hot paths without measurement.

## Testing {#testing}

Tests are organized by package and contract. Useful anchors include `BoundedStackTest`, `PaginatedListTest`, codec tests, range tests, time tests, and `ConcurrentReducer` tests. Run the module suite with:

```bash
./gradlew :bluetape4k-core:test --no-configuration-cache
```

When adopting one helper, copy the smallest matching test pattern rather than treating the whole module as one integration surface.

## Workshops {#workshops}

No single workshop covers the entire toolbox. Higher-level repository examples exercise core transitively. For focused learning, start from the matching unit test and turn one assertion into a small runnable experiment.

## Limitations {#limitations}

The breadth of core means its APIs do not share one lifecycle or performance profile. Read the source and tests for the selected family. Encoding is not encryption, reflection helpers do not make inaccessible APIs stable, and bounded containers do not provide distributed backpressure.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Module Overview diagram

[![Module Overview diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-core-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-core-diagram-01.svg)

_Release README: [`bluetape4k/core/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/core/README.md)_

### Core Class Structure diagram

[![Core Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-core-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-core-diagram-02.svg)

_Release README: [`bluetape4k/core/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/core/README.md)_

### Validation Chaining Flow diagram

[![Validation Chaining Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-core-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-core-sequence-01.svg)

_Release README: [`bluetape4k/core/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/core/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README and API catalog](../../../../bluetape4k/core/README.md)
- [Main source packages](../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k)
- [Module tests](../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k)
- [Module build and dependencies](../../../../bluetape4k/core/build.gradle.kts)
