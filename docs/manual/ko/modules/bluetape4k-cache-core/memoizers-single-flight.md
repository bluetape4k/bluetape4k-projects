---
title: Memoizer와 같은 key 동시 계산
description: Sync, CompletableFuture, suspend memoizer가 결과와 진행 중 계산을 어떻게 재사용하고 실패와 clear를 처리하는지 설명합니다.
manualId: bluetape4k-cache-core
chapterId: memoizers-single-flight
---

# Memoizer와 같은 key 동시 계산

## 함수를 그대로 호출하는 cache

Memoizer는 cache key를 따로 조립하는 대신 함수 입력을 key로 사용합니다. 같은 입력이 순수하거나 허용된 stale window 안에서 같은 결과를 낼 때 적합합니다.

```kotlin
val summary = (suspend { userId: Long ->
    userRepository.loadSummary(userId)
}).suspendMemoizer()

val first = summary(42L)
val second = summary(42L)
```

`Memoizer`, `AsyncMemoizer`, `SuspendMemoizer`는 각각 blocking 값, `CompletableFuture`, suspend 결과를 다룹니다. in-memory 외에도 Caffeine, Cache2k, Ehcache, JCache adapter가 있습니다.

## Cached result와 in-flight result

완료된 결과를 재사용하는 것과 진행 중 계산을 공유하는 것은 다른 문제입니다. `SingleFlight`는 same-key in-flight 계산을 하나로 합칩니다. blocking caller는 latch를 기다리고, async caller는 같은 future를 받으며, suspend caller는 같은 `CompletableDeferred`를 기다립니다.

```text
caller A ─┐
caller B ─┼─ key=42 ─ one evaluator ─ cached result
caller C ─┘
```

key가 다르면 계산도 독립적으로 진행됩니다. 따라서 전역 lock으로 모든 cache miss를 직렬화하지 않습니다.

## 실패와 취소는 cache 값이 아니다

evaluator가 예외를 던지거나 suspend 계산이 취소되면 in-flight entry를 제거합니다. 다음 호출은 같은 실패를 재생하지 않고 다시 계산할 수 있습니다.

```kotlin
var attempts = 0
val memo = (suspend { key: String ->
    attempts += 1
    if (attempts == 1) error("temporary failure")
    key.length
}).suspendMemoizer()

runCatching { memo("recover") }
check(memo("recover") == 7)
```

async evaluator가 `null`로 완료되면 `SingleFlight`는 `NullPointerException`으로 실패시킵니다. memoizer의 key와 result type은 non-null 계약입니다.

## `clear()`와 진행 중 계산

`clear()`는 저장된 값만 비우지 않습니다. `SingleFlight`의 generation을 올려 이미 진행 중인 계산의 write token도 무효화합니다. clear 전에 시작한 caller는 자신의 계산 결과를 받을 수 있지만, 그 오래된 결과는 clear 뒤 cache에 다시 들어가지 않습니다.

설정 변경, tenant 제거, 권한 변경처럼 “지금 이후에는 이전 결과를 저장하면 안 된다”는 작업은 이 세대 검사가 없으면 clear 직후 오래된 값이 다시 들어올 수 있습니다.

## 구현 선택

- 작은 process-local 함수에는 in-memory memoizer를 사용합니다.
- Caffeine의 capacity·expiry·statistics가 필요하면 Caffeine memoizer를 사용합니다.
- 기존 JCache를 재사용할 때는 JCache memoizer를 선택하되 sync `getOrPut`의 중복 evaluator 가능성을 확인합니다.
- remote cache 공유는 provider 모듈의 memoizer 계약과 serialization 비용을 함께 검토합니다.

## Source와 tests

- [`SingleFlight.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/memoizer/SingleFlight.kt)
- [`InMemorySuspendMemoizer.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/memoizer/inmemory/InMemorySuspendMemoizer.kt)
- [`CaffeineSuspendMemoizer.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/memoizer/caffeine/CaffeineSuspendMemoizer.kt)
- [`SingleFlightTest.kt`](../../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/memoizer/SingleFlightTest.kt)
- [`AbstractSuspendMemoizerTest.kt`](../../../../../cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/memoizer/AbstractSuspendMemoizerTest.kt)
