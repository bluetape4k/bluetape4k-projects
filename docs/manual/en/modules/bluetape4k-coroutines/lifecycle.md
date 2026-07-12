---
title: Lifecycle and cancellation
description: Design scope ownership, cancellation propagation, and dispatcher shutdown explicitly.
manualId: bluetape4k-coroutines
chapterId: lifecycle
---

# Lifecycle and cancellation

## Problem to solve

Decide who stops a coroutine before deciding where to start it. Work tied to one request must not share a scope with workers that live until a component closes.

## Mental model

A caller-owned scope follows the caller lifecycle. `CloseableCoroutineScope`, `DefaultCoroutineScope`, and `ThreadPoolCoroutineScope` belong to a component and use `close()` as their boundary.

## Smallest API surface

Use the supplied `CoroutineScope` and `coroutineScope` for ordinary request work. Create a closeable scope only for a component that owns an independent dispatcher.

## Complete example

A component keeps its scope as state and closes it exactly once. The caller guarantees cleanup with `try/finally` or an application lifecycle hook.

## Selection guide

Use caller ownership for work required by the call result and component ownership for workers shared across calls. Do not create a new scope for a simple parallel composition.

## Failure, cancellation, and lifecycle contract

`CancellationException` is a normal termination signal and must not become an ordinary failure. A timeout can stop only the wait, so verify whether the underlying I/O is cancellable.

## Operations and diagnosis

Observe active jobs, dispatcher threads, and queue length together. If a thread survives shutdown, inspect the owner and its close path first.

## Source and representative tests

The contract comes from the [`CloseableCoroutineScope`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/CloseableCoroutineScope.kt) family and [coroutines tests](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines).

## Next chapter and runnable workshop

Continue with [Deferred coordination](./deferred.md). Use the `spring-boot/webflux-coroutines` workshop for request lifecycle examples.
