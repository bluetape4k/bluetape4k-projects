---
title: Concurrency and lifecycle
description: Define ConcurrentReducer capacity and ShutdownQueue close ordering.
manualId: bluetape4k-core
chapterId: concurrency-lifecycle
---

# Concurrency and lifecycle

## Problem to solve

Asynchronous aggregation needs explicit capacity and shutdown behavior for queued and running work.

## Mental model

`ConcurrentReducer` limits running and queued work; `ShutdownQueue` closes registered resources in reverse order.

## Smallest API surface

Use `ConcurrentReducer` for bounded aggregation and `ShutdownQueue` for deterministic LIFO close actions.

## Complete example

Create a reducer with small capacity and observe a failed future when full, queued cancellation on close, and the separate lifecycle of an already-running external stage.

## Selection guide

Use a reducer only when aggregation is required. It is not a generic replacement for a queue, executor, or coroutine policy.

## Failure, cancellation, and lifecycle contract

`add` returns a failed future when full or closed rather than throwing synchronously. Close cancels queued work but does not force-cancel an external stage already running.

## Operations and diagnosis

Observe running, queued, rejected, and close latency separately. Record which LIFO action failed during shutdown.

## Source and representative tests

[`ConcurrentReducer.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/ConcurrentReducer.kt), [`ShutdownQueue.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/utils/ShutdownQueue.kt), and their tests define the contract.

## Next chapter and runnable workshop

See [Bounded collections](./bounded-collections.md) for bounded state and [Recipes](./recipes.md) for composition.
