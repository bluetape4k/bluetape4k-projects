---
manualId: bluetape4k-coroutines
title: "Coroutine and Flow Extensions"
description: Lifecycle-aware coroutine scopes, Deferred helpers, Flow operators, subjects, and structured-concurrency bridges.
kind: library
group: concurrency
learningOrder: 200
---

# Coroutine and Flow Extensions

## Problem {#problem}

Kotlin coroutines provide the primitives, but backend code still repeats scope ownership, `Deferred` coordination, Flow windowing and parallel mapping, subject-like multicast behavior, and bridges to Java structured concurrency. Repeating those pieces makes cancellation and shutdown behavior inconsistent. `bluetape4k-coroutines` packages the repository's shared contracts.

![Decision map for the bluetape4k-coroutines manual](../../assets/coroutines/module-foundation.svg)

## When to use {#when-to-use}

Use this module when the required operator or lifecycle abstraction already exists here and its cancellation contract matches the caller. Keep plain `coroutineScope`, `async`, and standard Flow operators when they solve the problem directly. Choose a caller-owned scope when work must stop with a request; do not create a long-lived helper scope for request-scoped work.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
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

## Choosing the first abstraction {#decision-guide}

| Requirement | Default choice | Choose a bluetape4k helper when |
| --- | --- | --- |
| Combine two or three suspend calls inside one request | The caller's `coroutineScope` and `async` | The same winner or zip rule appears in several places; use `DeferredSupport`. |
| Transform a Flow asynchronously while preserving input order | `map` or `flow.async` | Element computations should overlap but emission order must remain stable. |
| Bound a transform where throughput matters more than result order | `mapParallel` | `parallelism` can be derived from downstream capacity. |
| Let a component own both a dispatcher and a scope | A framework lifecycle scope | If you create the dispatcher too, close the `CloseableCoroutineScope` from that lifecycle. |
| Select the first replica to finish | `awaitAny` | Use `awaitAnyAndCancelOthers` when losers must be cancelled. Both select first completion, not first success. |
| Model a hot stream with terminal completion or error | `StateFlow`/`SharedFlow` | Use a subject when explicit `complete()` or `emitError()` semantics are required. |
| Bridge a JDK structured-task policy | `coroutineScope`/`supervisorScope` | Use structured helpers for direct virtual-thread fail-fast or first-success policies. |

## Practical recipes {#recipes}

### 1. Keep request work in the caller scope

```kotlin
suspend fun loadDashboard(userId: String): Dashboard = coroutineScope {
    val profile = async { profileClient.load(userId) }
    val notices = async { noticeClient.load(userId) }
    Dashboard(profile.await(), notices.await())
}
```

Keeping children out of a separate application scope propagates caller cancellation to both requests. Create a closeable scope only when a component truly owns background work, and connect it to the owner's `close()`.

### 2. Separate ordering from throughput

```kotlin
val ordered = ids.asFlow()
    .async { id -> client.load(id) }
    .toList()                      // preserves input order

val throughput = ids.asFlow()
    .mapParallel(parallelism = 8) { id -> client.load(id) }
    .toList()                      // completion order may differ
```

`mapParallel(parallelism = 1)` takes the plain `map` path and preserves order. Values above one use `flatMapMerge`, so output order must not become an API contract.

### 3. Select the fastest replica and cancel losers

```kotlin
suspend fun <T> fastestReplica(
    requests: List<Deferred<T>>,
): T = requests.awaitAnyAndCancelOthers()
```

The winner is the first task to **complete**. If that task fails or is cancelled, its result is propagated and all losers are cancelled. Use `firstSuccessTaskScope` when the requirement is the first **successful** result.

### 4. Do not lose the first PublishSubject event

```kotlin
val subject = PublishSubject<Event>()
coroutineScope {
    val collector = launch { subject.collect(::handle) }
    subject.awaitCollector()
    subject.emit(Event.Started)
    subject.complete()
    collector.join()
}
```

`PublishSubject` does not replay old values, so an event emitted before registration is lost. Use `awaitCollector()` in tests and adapters where startup order matters. Terminal calls after `complete()` or `emitError()` are ignored.

## Patterns {#patterns}

Use structured scopes at the narrowest lifecycle boundary. Rethrow `CancellationException` before broad exception handling. For parallel Flow work, set concurrency from the downstream service capacity rather than CPU count alone. Prefer `await()` over blocking bridges. If a type owns a scope, make the owner `Closeable` and connect it to application shutdown.

## Integrations {#integrations}

The module integrates with `bluetape4k-core`, virtual-thread dispatchers, Reactor context helpers, Java `CompletableFuture`/streams, and Kotlin Flow. Reactor-specific helpers stay in the `coroutines.reactor` package so applications that do not use Reactor can avoid coupling their code to that model.

## Configuration {#configuration}

There is no central property file. Configure dispatcher, parallelism, buffer size, timeout/deadline, and subject capacity at the call or owning component. The benchmark configuration in the module build is for repository measurements and is not an application runtime default.

## Failures {#failures}

`await()` propagates the original computation failure and cancellation. Fail-fast task scopes cancel remaining work after the first failure; first-success scopes fail when every branch fails; supervised scopes retain partial results. Deadline-based joins throw `TimeoutException`. Blocking access from a constrained coroutine dispatcher can deadlock or starve the pool, which is why `DeferredValue.value` is deprecated.

### Cancellation checklist

1. Rethrow `CancellationException` before a broad `catch (e: Exception)` branch.
2. Check that request children do not escape into an application scope and lose caller cancellation.
3. Document the owner and close point of every dispatcher, scope, and channel created directly.
4. Distinguish a timeout that cancels remote I/O from one that only stops the local wait.
5. Use `supervisorScope` when a test intentionally observes child failure without cancelling its body.

### Troubleshooting table

| Symptom | Check first | Response |
| --- | --- | --- |
| Computation continues after the caller finishes | An unclosed `DeferredValue` or manually created scope | Prefer `await()` and call `close()` from the owning lifecycle. |
| `mapParallel` changes result order | Whether `parallelism` is two or greater | Use `flow.async` or plain `map` when ordering is contractual. |
| A remote call remains after timeout | Whether the client maps coroutine cancellation to actual I/O cancellation | Verify the client contract and add explicit deadline/idempotency policy. |
| The first subject event disappears | Whether `emit` ran before collector registration | Use `awaitCollector()` or select a replaying subject/`StateFlow`. |
| One child failure cancels every sibling | Whether fail-fast is the intended policy | Use `supervisorScope` or `supervisedTaskScope` when partial results are valid. |
| Threads remain after shutdown | A scope such as `ThreadPoolCoroutineScope` owns its dispatcher | Connect the component's `close()` to application shutdown. |

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

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Module Overview diagram

[![Module Overview diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-diagram-01.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### Coroutines Class Structure diagram

[![Coroutines Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-diagram-02.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### 1. Flow Extension Categories diagram

[![1. Flow Extension Categories diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-diagram-03.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### DeferredValue Usage Flow diagram

[![DeferredValue Usage Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-01.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### 2. chunked(n) — Fixed-Size Chunks diagram

[![2. chunked(n) — Fixed-Size Chunks diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-02.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### 3. windowed(size, step) — Sliding Window diagram

[![3. windowed(size, step) — Sliding Window diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-03.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### 4. sliding(n) / bufferedSliding(n) — One-Step Sliding Window diagram

[![4. sliding(n) / bufferedSliding(n) — One-Step Sliding Window diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-04.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### 5. mapParallel(parallelism) — Parallel Transformation diagram

[![5. mapParallel(parallelism) — Parallel Transformation diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-05.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-05.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### 6. concatMapEager { } — Order-Preserving Eager Parallel Collection diagram

[![6. concatMapEager { } — Order-Preserving Eager Parallel Collection diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-06.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-06.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### 7. bufferingDebounce(timeout) — Debounced Batching diagram

[![7. bufferingDebounce(timeout) — Debounced Batching diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-07.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-07.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### 8. throttleLeading / throttleTrailing / throttleBoth — Throttle Windows diagram

[![8. throttleLeading / throttleTrailing / throttleBoth — Throttle Windows diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-08.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-08.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### 9. takeUntil(notifier) / skipUntil(notifier) — Gate Control diagram

[![9. takeUntil(notifier) / skipUntil(notifier) — Gate Control diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-09.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-09.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### 10. merge(flows) — Unordered Merge diagram

[![10. merge(flows) — Unordered Merge diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-10.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-10.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### 11. pairwise() / zipWithNext() — Adjacent Pairs diagram

[![11. pairwise() / zipWithNext() — Adjacent Pairs diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-11.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-11.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### 12. scanWith(initial) { } — Lazy Scan Seed diagram

[![12. scanWith(initial) { } — Lazy Scan Seed diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-12.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-12.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

### 13. AsyncFlow — Ordered Async Results diagram

[![13. AsyncFlow — Ordered Async Results diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-13.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-coroutines-sequence-13.svg)

_Release README: [`bluetape4k/coroutines/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/coroutines/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README and examples](../../../../bluetape4k/coroutines/README.md)
- [`DeferredValue` lifecycle contract](../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/DeferredValue.kt)
- [Flow extension source](../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow)
- [Coroutine tests](../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines)
- [Module build and benchmark configuration](../../../../bluetape4k/coroutines/build.gradle.kts)
