---
manualId: bluetape4k-coroutines
title: Coroutine and Flow extensions
description: Lifecycle-aware coroutine scopes, Deferred helpers, Flow operators, subjects, and structured-concurrency bridges.
kind: library
group: foundation
---

# Coroutine and Flow extensions

## Problem {#problem}

Kotlin coroutines provide the primitives, but backend code still repeats scope ownership, `Deferred` coordination, Flow windowing and parallel mapping, subject-like multicast behavior, and bridges to Java structured concurrency. Repeating those pieces makes cancellation and shutdown behavior inconsistent. `bluetape4k-coroutines` packages the repository's shared contracts.

## When to use {#when-to-use}

Use this module when the required operator or lifecycle abstraction already exists here and its cancellation contract matches the caller. Keep plain `coroutineScope`, `async`, and standard Flow operators when they solve the problem directly. Choose a caller-owned scope when work must stop with a request; do not create a long-lived helper scope for request-scoped work.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-coroutines")
}
```

The module exposes core and virtual-thread API integration. Java structured-concurrency bridges require a compatible JDK implementation at runtime.

## Concepts {#concepts}

`DefaultCoroutineScope`, `IoCoroutineScope`, `ThreadPoolCoroutineScope`, and `VirtualThreadCoroutineScope` own dispatchers/jobs and must be closed. `DeferredValue` starts one eager async computation and offers suspending access through `await()`; its blocking `value` property is deprecated for coroutine code. Flow extensions cover batching, ranges, races, backpressure policies, parallel transforms, and multicast subjects.

Structured task scopes provide fail-fast, first-success, and supervised policies. The policy determines which failure cancels siblings and what result can be returned.

## Quick start {#quick-start}

```kotlin
import io.bluetape4k.coroutines.deferredValueOf

suspend fun loadAnswer(): Int {
    val value = deferredValueOf { 21 * 2 }
    return try {
        value.await()
    } finally {
        value.close()
    }
}
```

`DeferredValue` owns a `DefaultCoroutineScope`. Closing it is important when the caller may abandon the computation.

## API by task {#api-by-task}

| Task | Start with |
| --- | --- |
| Transform one eager async value | `DeferredValue`, `map`, `flatMap` |
| Wait for or coordinate plain deferred values | `support.awaitAny`, `awaitAnyAndCancelOthers`, `zip` |
| Batch or window a Flow | `chunked`, `windowed`, `bufferUntilChanged` |
| Preserve order while processing asynchronously | `flow.async` |
| Run bounded parallel transforms | `mapParallel` |
| Multicast state or events | `BehaviorSubject`, `PublishSubject` |
| Own a closeable scope | `DefaultCoroutineScope`, `IoCoroutineScope`, `VirtualThreadCoroutineScope` |
| Select structured failure policy | `taskScope`, `firstSuccessTaskScope`, `supervisedTaskScope` |

## Patterns {#patterns}

Use structured scopes at the narrowest lifecycle boundary. Rethrow `CancellationException` before broad exception handling. For parallel Flow work, set concurrency from the downstream service capacity rather than CPU count alone. Prefer `await()` over blocking bridges. If a type owns a scope, make the owner `Closeable` and connect it to application shutdown.

## Integrations {#integrations}

The module integrates with `bluetape4k-core`, virtual-thread dispatchers, Reactor context helpers, Java `CompletableFuture`/streams, and Kotlin Flow. Reactor-specific helpers stay in the `coroutines.reactor` package so applications that do not use Reactor can avoid coupling their code to that model.

## Configuration {#configuration}

There is no central property file. Configure dispatcher, parallelism, buffer size, timeout/deadline, and subject capacity at the call or owning component. The benchmark configuration in the module build is for repository measurements and is not an application runtime default.

## Failures {#failures}

`await()` propagates the original computation failure and cancellation. Fail-fast task scopes cancel remaining work after the first failure; first-success scopes fail when every branch fails; supervised scopes retain partial results. Deadline-based joins throw `TimeoutException`. Blocking access from a constrained coroutine dispatcher can deadlock or starve the pool, which is why `DeferredValue.value` is deprecated.

## Operations {#operations}

Observe active jobs, queue/buffer growth, downstream latency, cancellation rate, and timeout count. Close owned scopes and channels during shutdown. A high parallelism value can move the bottleneck into a database or remote service; capacity limits belong at that boundary.

## Testing {#testing}

Use `runTest` for suspending contracts and `supervisorScope` when a test intentionally observes child failure without cancelling the test body. Representative suites include `DeferredSupportTest`, `StructuredConcurrencyTest`, `AsyncFlowTest`, subject tests, and each Flow operator test.

```bash
./gradlew :bluetape4k-coroutines:test --no-configuration-cache
```

## Workshops {#workshops}

Repository examples use these APIs through higher-level Ktor and Spring modules. For focused experiments, start from `StructuredConcurrencyTest` or a single Flow operator test and vary failure order, cancellation, timeout, and parallelism.

## Limitations {#limitations}

Custom Flow operators do not remove the need to understand cold versus hot streams, buffering, and cancellation. Subject-like APIs can hide ownership if exposed globally. Virtual-thread bridges depend on the selected JDK implementation and should not be assumed to improve every CPU-bound workload.

## Sources {#sources}

- [Module README and examples](../../../../bluetape4k/coroutines/README.md)
- [`DeferredValue` lifecycle contract](../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/DeferredValue.kt)
- [Flow extension source](../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow)
- [Coroutine tests](../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines)
- [Module build and benchmark configuration](../../../../bluetape4k/coroutines/build.gradle.kts)
