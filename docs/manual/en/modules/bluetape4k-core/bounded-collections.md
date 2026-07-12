---
title: Bounded collections
description: Compare stack and ring-buffer iteration, capacity, eviction, and thread-safety contracts.
manualId: bluetape4k-core
chapterId: bounded-collections
---

# Bounded collections

`BoundedStack` and `RingBuffer` both evict the oldest value to bound memory, but expose opposite read order.

![Ordering comparison for capacity-three BoundedStack and RingBuffer](../../../assets/core/bounded-collection-ordering.svg)

## Choose by read semantics

| Requirement | Type | Index zero | Iteration |
| --- | --- | --- | --- |
| newest-first undo/history | `BoundedStack` | newest/top | newest to oldest |
| chronological recent history | `RingBuffer` | oldest/read head | oldest to newest |

Both validate a positive capacity and protect public reads/writes with `ReentrantLock`.

```kotlin
val stack = BoundedStack<Int>(3).apply { pushAll(1, 2, 3, 4) }
val ring = RingBuffer<Int>(3).apply { addAll(1, 2, 3, 4) }

check(stack.toList() == listOf(4, 3, 2))
check(ring.toList() == listOf(2, 3, 4))
```

Both evict 1. `BoundedStack.pop()` removes 4 first; `RingBuffer.drop(1)` removes 2 first.

## API semantics

`BoundedStack` offers `push`, `pop`, `peek`, `insert`, `update`, and `remove` with top-relative indexes. Empty pop/peek throws `NoSuchElementException`; invalid indexes throw `IndexOutOfBoundsException`.

`RingBuffer` offers add, indexed get/set, `drop`, `removeIf`, and clear with chronological indexes. Negative drop is invalid; dropping at least the current size clears the buffer.

## What bounded collections are not

Bounded memory is not backpressure. Overflow silently replaces old data, so use a channel, queue, or persistent log when every item must be delivered. These types also do not provide blocking producer/consumer coordination.

## Operations and testing

Observe eviction count, capacity saturation, and snapshot size. Test wrap-around indexing/iteration, concurrent access, invalid capacity, empty operations, and insert/remove boundaries.

## Source and representative tests

- [`BoundedStack.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/collections/BoundedStack.kt)
- [`RingBuffer.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/collections/RingBuffer.kt)
- [`BoundedStackTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/BoundedStackTest.kt)
- [`RingBufferTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/RingBufferTest.kt)

Capacity for running and waiting asynchronous work belongs in [Concurrency and lifecycle](./concurrency-lifecycle.md).
