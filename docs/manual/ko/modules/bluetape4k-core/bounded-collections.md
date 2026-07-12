---
title: Bounded collections
description: Stack과 ring buffer의 iteration order, capacity, eviction, thread-safety 계약을 비교합니다.
manualId: bluetape4k-core
chapterId: bounded-collections
---

# Bounded collections

`BoundedStack`과 `RingBuffer`는 모두 가장 오래된 값을 제거해 memory를 제한하지만 읽는 순서가 반대입니다.

![Capacity 3에서 BoundedStack과 RingBuffer의 ordering 비교](../../../assets/core/bounded-collection-ordering.svg)

## 선택 기준

| 요구 | 타입 | index 0 | iteration |
| --- | --- | --- | --- |
| 최근 값부터 읽기, undo/history stack | `BoundedStack` | newest/top | newest → oldest |
| 시간 순서로 recent history 순회 | `RingBuffer` | oldest/read head | oldest → newest |

둘 다 `maxSize > 0`을 생성 시 검증하고 `ReentrantLock`으로 public read/write를 보호합니다.

## 동일한 overflow, 다른 order

```kotlin
val stack = BoundedStack<Int>(3).apply { pushAll(1, 2, 3, 4) }
val ring = RingBuffer<Int>(3).apply { addAll(1, 2, 3, 4) }

check(stack.toList() == listOf(4, 3, 2))
check(ring.toList() == listOf(2, 3, 4))
```

두 collection 모두 1을 evict합니다. `BoundedStack.pop()`은 4부터 제거하고, `RingBuffer.drop(1)`은 2부터 제거합니다.

## API 의미

`BoundedStack`은 `push`, `pop`, `peek`, `insert`, `update`, `remove`를 stack top 기준 index로 제공합니다. Empty `pop/peek`은 `NoSuchElementException`, 잘못된 index는 `IndexOutOfBoundsException`입니다.

`RingBuffer`는 `add`, index get/set, `drop`, `removeIf`, `clear`를 chronological index로 제공합니다. `drop(n)`은 음수를 거부하고 size 이상이면 전체를 비웁니다.

## 무엇이 아닌가

Bounded memory는 backpressure가 아닙니다. Overflow가 오래된 값을 조용히 교체하므로 모든 event delivery가 필요하면 channel/queue/persistent log를 사용합니다. 여러 producer/consumer의 blocking coordination도 제공하지 않습니다.

## 운영과 테스트

Eviction count, capacity 도달률, snapshot size를 관찰합니다. 테스트는 wrap-around 뒤 index/iteration, concurrent access, invalid capacity, empty operation, insert/remove boundary를 포함합니다.

## Source와 representative tests

- [`BoundedStack.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/collections/BoundedStack.kt)
- [`RingBuffer.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/collections/RingBuffer.kt)
- [`BoundedStackTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/BoundedStackTest.kt)
- [`RingBufferTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/RingBufferTest.kt)

실행 중/대기 중 비동기 work의 capacity는 collection이 아니라 [Concurrency와 lifecycle](./concurrency-lifecycle.md)에서 다룹니다.
