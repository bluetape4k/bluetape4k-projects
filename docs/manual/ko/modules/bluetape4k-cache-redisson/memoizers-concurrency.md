---
title: Memoizer와 같은 key 계산 병합
description: sync, future, suspend memoizer의 cache miss, 경쟁, 실패와 취소 계약을 비교합니다.
manualId: bluetape4k-cache-redisson
chapterId: memoizers-concurrency
---

# Memoizer와 같은 key 계산 병합

## 완료 값과 진행 중인 계산

Redisson memoizer는 완료된 값을 `RMap`에 저장합니다. 같은 key의 진행 중인 계산은 각 JVM의 `ConcurrentHashMap`에서 합칩니다. 따라서 한 JVM에서 16개 coroutine이 동시에 요청하면 evaluator를 한 번 실행할 수 있지만, 서로 다른 JVM의 첫 miss는 각각 계산할 수 있습니다.

```kotlin
val squares = redissonClient
    .getMap<Int, Int>("memoizer:squares", IntegerCodec())
    .suspendMemoizer { key -> expensiveSquare(key) }

val value = squares(7)
```

이 계약은 분산 lock이 아닙니다. cluster 전체에서 evaluator를 한 번만 실행해야 한다면 별도 lock·fencing·idempotency 설계가 필요합니다.

## sync, async, suspend 선택

| API | evaluator | miss 경로 | 적합한 호출자 |
| --- | --- | --- | --- |
| `memoizer` | `(T) -> R` | 호출 thread에서 실행 | CPU 계산 또는 이미 blocking인 경로 |
| `asyncMemoizer` | `CompletionStage<R>` | future chain | Java future 기반 코드 |
| `suspendMemoizer` | `suspend (T) -> R` | Redisson async + `await()` | coroutine 서비스 |

세 구현 모두 `putIfAbsent`로 Redis 경쟁의 winner를 정합니다. sync와 suspend는 다른 process가 먼저 저장한 값이 있으면 그 값을 반환합니다. 1.12.1 async 구현은 `putIfAbsentAsync` 결과와 관계없이 evaluator가 만든 값을 반환하므로 process 간 경쟁 결과가 호출마다 다를 수 있음을 고려합니다.

## 실패와 취소 뒤 복구

evaluator가 실패하면 promise/deferred를 예외로 완료하고 in-flight entry를 제거합니다. 다음 호출은 이전 실패에 고착되지 않고 새 계산을 시작합니다. suspend 구현은 `CancellationException`도 성공 값으로 저장하지 않고 그대로 전파합니다.

대기 중인 coroutine 하나가 취소되어도 공유 deferred 자체는 자동 취소되지 않습니다. evaluator를 실행하는 owner coroutine이 계속 살아 있다면 다른 waiter는 결과를 받을 수 있습니다. 반대로 owner가 취소되면 다음 호출이 새 계산을 시작합니다.

## `clear()`의 차이

sync와 suspend `clear()`는 Redis map을 비웁니다. async `clear()`는 local in-flight map도 먼저 비웁니다. 진행 중인 future와 동시에 clear하면 완료 시점에 값이 다시 저장되거나 새 요청이 evaluator를 중복 실행할 수 있으므로, 운영 clear는 쓰기 quiescence 또는 generation key와 함께 설계합니다.

## evaluator 선택 규칙

memoizer는 조회나 순수 계산에 적합합니다. 결제, 알림 발송처럼 재실행하면 안 되는 side effect를 넣지 않습니다. failure와 cancellation 뒤 evaluator가 다시 실행될 수 있기 때문입니다.

## Source와 tests

- [`RedissonMemoizer.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonMemoizer.kt)
- [`RedissonAsyncMemoizer.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonAsyncMemoizer.kt)
- [`RedissonSuspendMemoizer.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonSuspendMemoizer.kt)
- [`RedissonSuspendMemoizerTest.kt`](../../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/memoizer/RedissonSuspendMemoizerTest.kt)
