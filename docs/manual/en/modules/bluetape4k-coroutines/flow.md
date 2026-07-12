---
title: Ordered and parallel Flow
description: Select Flow operators from input order, completion order, parallelism, and buffer capacity.
manualId: bluetape4k-coroutines
chapterId: flow
---

# Ordered and parallel Flow

Concurrent execution and emission order are separate contracts. The important difference between `flow.async` and `mapParallel` is output ordering, not simply speed.

![Ordered emission in flow.async and completion-order emission in mapParallel](../../../assets/coroutines/ordered-parallel-flow.svg)

## Decision table

| Requirement | Choice | Output order | Pressure control |
| --- | --- | --- | --- |
| simple sequential transform | standard `map` | input | one at a time |
| overlap computation but retain response order | `Flow.async` | input | collect buffer |
| process whichever result finishes first | `mapParallel(n)` | completion may win | bounded by `n` |

## Ordered concurrency

`Flow.async` converts each input into a `LazyDeferred` and starts it in the collect scope. Work overlaps, but downstream awaits deferred values in original order.

```kotlin
productIds.asFlow()
    .async(Dispatchers.IO) { id -> catalog.load(id) }
    .collect(capacity = 16) { product -> render(product) }
```

A slow earlier item delays emission of later completed items. This head-of-line cost is appropriate when response order is contractual.

Capacity accepts `Channel.BUFFERED`, `Channel.CONFLATED`, or a non-negative number. It bounds result buffering; it is not permission for unbounded work.

## Throughput-first transforms

`mapParallel` coerces parallelism to at least one. One uses plain `map`; values above one use `flatMapMerge(concurrency)`.

```kotlin
val stored = events.asFlow()
    .mapParallel(parallelism = 8, context = Dispatchers.IO) { event ->
        repository.persist(event)
    }
    .toList()
```

Faster items may emit first, so input order must not be a public contract. This is a good fit for independent persistence, enrichment, and thumbnail work.

## Sizing parallelism

Do not derive it from CPU count alone.

```text
effective parallelism = min(
  application budget,
  connection pool capacity,
  remote concurrency limit,
  memory/buffer budget
)
```

With retries, worst-case in-flight work approaches `parallelism × attempts`. Sum competing pipelines that share the same downstream boundary.

## Failure and cancellation

- transform failure fails collection and structured siblings are cancelled;
- collector cancellation propagates upstream and into active children;
- `flowOn(context)` changes execution context, not lifecycle ownership;
- remote work surviving timeout usually indicates a client cancellation-bridge problem.

## What to test

1. parallelism one, zero, and negative values take the ordered sequential path;
2. values above one are allowed to change output order;
3. active transforms never exceed the configured bound;
4. collector cancellation cleans up child work and remote requests;
5. a slow first item demonstrates head-of-line waiting on the ordered path.

## Operational signals

Track P95/P99 item latency, active transforms, buffer utilization, and downstream waiting together. A permanently full buffer is a capacity mismatch. Locate that boundary before increasing parallelism.

## Source and representative tests

- [`AsyncFlow.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/AsyncFlow.kt)
- [`mapParallel.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/mapParallel.kt)
- [`AsyncFlowTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/AsyncFlowTest.kt)
- [`MapParallelTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/MapParallelTest.kt)

For callback and hot-stream delivery semantics, continue with [Subjects and event contracts](./subjects.md).
