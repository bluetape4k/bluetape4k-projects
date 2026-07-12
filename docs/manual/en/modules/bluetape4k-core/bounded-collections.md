---
title: Bounded collections
description: Compare stack and ring-buffer ordering, capacity, and eviction.
manualId: bluetape4k-core
chapterId: bounded-collections
---

# Bounded collections

## Problem to solve

Limit recent values while making traversal order and overflow eviction explicit.

## Mental model

`BoundedStack` reads newest-first and `RingBuffer` reads oldest-first. Both evict the oldest value when capacity is exceeded.

## Smallest API surface

Use `BoundedStack` for reverse-order recent work and `RingBuffer` for chronological history.

## Complete example

Insert 1, 2, 3, and 4 into capacity 3 and verify both traversal orders and eviction of 1.

## Selection guide

Traversal order is the primary choice. Use a concurrency primitive instead when producers, consumers, or backpressure are involved.

## Failure, cancellation, and lifecycle contract

Capacity bounds memory, not throughput. Reject an invalid capacity at construction.

## Operations and diagnosis

Observe capacity hits and eviction count to confirm that data loss matches the intended policy.

## Source and representative tests

The contract is defined by [`BoundedStack.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/collections/BoundedStack.kt), [`RingBuffer.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/collections/RingBuffer.kt), and their tests.

## Next chapter and runnable workshop

Continue with [Concurrency and lifecycle](./concurrency-lifecycle.md) for concurrent aggregation.
