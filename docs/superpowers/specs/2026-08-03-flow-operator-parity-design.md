# Flow Operator Parity Design

## Problem

Issue #1297 asks for a focused RxJava 3 / Reactor `Flux` parity audit for the
`bluetape4k-coroutines` Flow extensions. The module already has count-based
windowing, debounce batching, eager ordered mapping, and one drop policy, but
it has no count-or-time boundary, idle timeout fallback, or bounded eager
ordered mapping contract. The work must add useful public API without
reimplementing operators already supplied by `kotlinx.coroutines`.

## Current evidence

- `windowed.kt` implements count/step windows and `windowedFlow` materializes
  each window as a cold `Flow`.
- `bufferingDebounce.kt` uses `produceIn` and `whileSelect`, but measures a
  debounce interval with `System.nanoTime`; that is not suitable for
  deterministic virtual-time tests.
- `concatMapEager.kt` launches one inner collector per source item and stores
  values in an unbounded `ConcurrentLinkedQueue`.
- `onBackpressureDrop.kt` is the only custom backpressure-named operator.
- `bluetape4k-coroutines` already has `kotlinx-coroutines-test`, JUnit 5,
  bluetape assertions, and a kotlinx benchmark target.
- Reactor `Flux.bufferTimeout(maxSize, maxTime)` defines the count-or-time
  boundary; RxJava `Observable.buffer(timespan, count)` documents the same
  first-of-count-or-time behavior. Kotlin Flow remains sequential by default,
  and `buffer`/`flatMapMerge` are the standard concurrency primitives.
- Kotlin `select` is biased toward the first ready clause. The implementation
  registers the input receive clause before the timeout clause, so a value that
  becomes ready at the same virtual instant as the deadline wins the tie; the
  next iteration then applies the timeout to the still-open batch.

References:

- https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html#bufferTimeout(int,java.time.Duration)
- https://reactivex.io/RxJava/3.x/javadoc/3.0.10/io/reactivex/rxjava3/core/Observable.html
- https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flat-map-latest.html
- https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/buffer.html

## Constraints

1. Public GitHub artifacts remain English; KDoc and README examples follow the
   repository's Korean-first KDoc and bilingual README convention.
2. `CancellationException` is never converted into a normal operator error or
   aggregated with data-plane failures.
3. New timer behavior must use coroutine suspension (`onTimeout`/`delay`) so
   `runTest` virtual time can prove it.
4. Existing `concatMapEager(transform)` source compatibility and ordering must
   remain intact.
5. No dependency, module, or generated catalog changes are required.
6. Count-or-time timers start when the first element enters a batch/window, not
   at subscription; empty windows are never emitted. Each exposed window is a
   repeatable cold `Flow` backed by a completed snapshot.
7. The idle timeout starts at collection and resets after every emitted item;
   `FlowTimeoutException` extends `java.util.concurrent.TimeoutException`.

## Alternatives

### A. Add every Rx/Reactor family in one large operator layer

This maximizes vocabulary parity but duplicates standard Flow operators,
creates several independent error policies, and makes cancellation semantics
hard to review. Rejected for this release.

### B. Add only documentation aliases and an inventory matrix

This is low risk, but leaves the P1 count/time and idle-time gaps identified by
the issue unresolved. Rejected as insufficient delivery.

### C. Implement a small, contract-first high-value subset (recommended)

Implement count-or-time batching/windows, idle timeout with explicit fallback,
and bounded eager ordered mapping. Map `switchMap` to standard
`flatMapLatest`; map `buffer`/`conflate`/overflow to standard Flow; split
delay-error and custom backpressure policy families into a linked follow-up.
This keeps the public surface small while proving the requested lifecycle and
concurrency contracts.

## Chosen design

### 1. Count-or-time operators

Add:

```kotlin
fun <T> Flow<T>.bufferTimeout(maxSize: Int, timeout: Duration): Flow<List<T>>
fun <T> Flow<T>.windowTimeout(maxSize: Int, timeout: Duration): Flow<Flow<T>>
```

Both validate `maxSize > 0` and `timeout.isPositive()`. A new buffer/window
starts when its first element arrives. It closes on the first of:

- `maxSize` elements;
- `timeout` elapsed since that buffer/window started.

Normal upstream completion emits one non-empty final partial buffer/window. An
upstream failure propagates without emitting the in-flight partial value, which
matches the module's `windowed` behavior and avoids publishing data from a
failed batch. Cancellation cancels the producer and timer together. A shared
internal count-or-time collector owns the timer and list allocation; the
window API exposes each completed list through `asFlow()`. Because the snapshot
is complete before it is exposed, collecting a returned window more than once
replays the same values; this is intentionally different from a live Reactor
window and is called out in migration documentation.

### 2. Idle timeout and fallback

Add:

```kotlin
class FlowTimeoutException(val timeout: Duration) : java.util.concurrent.TimeoutException

fun <T> Flow<T>.timeout(timeout: Duration): Flow<T>
fun <T> Flow<T>.timeoutOrFallback(timeout: Duration, fallback: Flow<T>): Flow<T>
```

The timer starts when collection starts and resets after every upstream item.
Normal upstream completion wins over a pending timer. On timeout, the upstream
is cancelled first; the no-fallback overload throws `FlowTimeoutException`, and
the fallback overload collects the fallback exactly once. Upstream failures
and fallback failures propagate unchanged. `CancellationException` always
propagates as cancellation.

### 3. Bounded eager ordered mapping

Keep the current overload and add:

```kotlin
fun <T : Any, R : Any> Flow<T>.concatMapEager(
    maxConcurrency: Int,
    bufferCapacity: Int = maxConcurrency,
    transform: suspend (T) -> Flow<R>,
): Flow<R>
```

The existing overload delegates to an unbounded compatibility path. The new
path acquires a `Semaphore(maxConcurrency)` before creating an inner collector
and uses a per-inner `Channel<R>(bufferCapacity)`; an inner producer suspends
when its ordered output queue is full. The downstream drain still consumes
inner queues in source order. Permit release and channel close happen in the
inner `finally` block. Child failure cancels the structured `channelFlow`; no
child is detached or leaked.

### 4. Inventory and exclusions

The inventory matrix records `switchMap`/`switchOnNext` as standard
`flatMapLatest` mappings and records `buffer`, `conflate`, `retryWhen`,
`combine`, and `zip` as standard Flow/non-goals. Delay-error composition,
custom buffer/latest overflow policies, and event-boundary windows are not
implemented in this issue. A single linked follow-up issue will capture those
families before code implementation begins.

## Failure modes and mitigations

1. **Timer and count race:** a value may arrive at the same virtual instant as
   the timeout. The receive clause is registered first, so Kotlin's biased
   `select` gives the value precedence; a timeout-only iteration closes the
   current non-empty batch. Tests cover both ordering permutations with
   `runTest`.
2. **Partial data on upstream failure:** the in-flight list is intentionally
   discarded when the channel closes with a cause; tests assert no partial
   emission before the exception.
3. **Timeout fallback leak:** the upstream producer is cancelled before the
   fallback is collected; tests use a `finally` marker to prove cleanup.
4. **Eager queue growth:** bounded `Channel` capacity suspends inner producers
   while a preceding inner is slow; tests assert active concurrency and a
   bounded queue configuration.
5. **Cancellation misclassification:** all operator catches rethrow
   `CancellationException`; cancellation tests assert the original cancellation
   reaches the collector and no fallback/error aggregation occurs.

## Compatibility and migration

- No existing function is removed or renamed.
- Existing `concatMapEager(transform)` keeps its signature and source order;
  callers wanting a memory/concurrency bound opt into the new overload.
- New types/functions are additive and live in
  `io.bluetape4k.coroutines.flow.extensions`.
- `bufferTimeout`/`windowTimeout` start their timer on the first element and
  expose no empty windows; callers migrating from Reactor must not assume a
  timer-created empty window at subscription.
- A `windowTimeout` result is a repeatable cold snapshot (`asFlow()`), not a
  live single-consumer window. `timeoutOrFallback` subscribes to its fallback
  only after upstream cleanup completes.
- README examples show the new contracts and explicitly point callers to
  standard Flow operators for excluded families.

## Acceptance criteria

- Inventory matrix covers current API, selected API, Rx/Reactor analogue, and
  standard Flow/non-goal mapping.
- KDoc and both README locales document completion, failure, cancellation,
  ordering, buffering, and concurrency semantics for every selected operator.
- `kotlinx-coroutines-test` covers count boundary, timeout boundary, partial
  completion, upstream failure, fallback, cancellation, virtual time, and
  bounded eager concurrency/ordering.
- Benchmark evidence covers count-or-time timer registration/list allocation
  and bounded eager queue behavior; virtual-time tests, not the benchmark,
  prove timer firing and deadline semantics.
- Duplicate search and the linked follow-up issue are recorded before code.
- `git diff --check`, targeted tests, module check, and the repository's
  applicable Kotlin/static checks pass.

## DoD

The issue is merge-ready only after the stacked PR train is green, each PR
body ends with `## DoD Status`, the final inventory and follow-up link are
published, exact-head CI/review/mergeability evidence is fresh, and the final
report lists any unchecked release or merge gate as `PENDING` rather than
silently assuming it.
