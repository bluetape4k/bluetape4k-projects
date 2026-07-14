---
title: Redis memoizer와 동시 계산
description: 동기, CompletionStage, suspend memoizer의 Redis 공유와 JVM 내 same-key 병합, 실패·취소 복구를 설명합니다.
manualId: bluetape4k-cache-lettuce
chapterId: memoizers-concurrency
---

# Redis memoizer와 동시 계산

## 어떤 결과를 공유하는가

세 memoizer는 `LettuceMap` 또는 `LettuceSuspendMap`에 함수 결과를 저장합니다. 입력 key는 `toString()`으로 Redis field가 되므로 다른 타입이 같은 문자열을 만들지 않도록 map 이름이나 key encoder 경계를 분리합니다.

```kotlin
val connection = redisClient.connect(LettuceLongCodec)
val resultMap = LettuceMap<Long>(connection, "pricing:factorial:v1")
val factorial = resultMap.memoizer { n: Long -> computeFactorial(n) }

val first = factorial(10L) // miss: 계산 후 Redis 저장
val next = factorial(10L)  // hit: Redis 값 반환
```

memoizer는 함수가 순수하거나 같은 key에 대해 재사용해도 되는 결과에만 적용합니다. 사용자 권한, 현재 시간이나 transaction 상태가 결과에 영향을 준다면 그 값을 key에 포함하거나 memoization을 사용하지 않습니다.

## 한 JVM의 same-key 병합

동기·async·suspend 구현 모두 `ConcurrentHashMap<K, ...>`에 진행 중인 계산을 둡니다. 같은 JVM에서 같은 key 요청이 겹치면 첫 요청의 future/deferred를 기다려 evaluator 중복 실행을 줄입니다.

이 `inFlight` map은 Redis에 저장되지 않습니다. 여러 애플리케이션 instance가 동시에 miss를 만나면 각 process가 evaluator를 실행할 수 있습니다. 최종 저장은 `putIfAbsent`로 경쟁하고, 패자는 Redis에 먼저 저장된 값을 다시 읽어 반환합니다.

```text
JVM A miss -> evaluate ----+-> Redis putIfAbsent wins
JVM B miss -> evaluate ----+-> loses, reads winner
```

중복 계산 자체가 위험한 작업이라면 distributed lock이나 source system의 idempotency가 필요합니다.

## 동기와 async 경로

`LettuceMemoizer`는 첫 caller가 Redis 조회와 evaluator를 수행하고 같은 process의 나머지 caller는 `CompletableFuture.get()`으로 기다립니다. blocking evaluator와 함께 사용합니다.

`LettuceAsyncMemoizer`는 Redis async command와 `CompletionStage` evaluator를 연결합니다. 완료 시 `inFlight.remove(key, promise)`로 자신이 등록한 promise만 지워 재진입으로 교체된 새 promise를 잘못 제거하지 않습니다.

```kotlin
val squares = LettuceMap<Int>(intConnection, "squares:v1")
    .asyncMemoizer { n: Int ->
        CompletableFuture.supplyAsync { n * n }
    }

check(squares(7).join() == 49)
```

executor와 timeout은 memoizer가 대신 정하지 않습니다. evaluator가 쓰는 executor, Redis command timeout과 caller의 deadline을 애플리케이션 수명주기에 맞춰 구성합니다.

## suspend 실패와 취소

`LettuceSuspendMemoizer`는 `CompletableDeferred`를 공유합니다. evaluator가 실패하거나 취소되면 기다리는 caller에도 같은 실패를 전달하고 `finally`에서 해당 key의 in-flight entry를 제거합니다. 실패 값은 Redis에 저장하지 않으므로 다음 호출은 새 계산을 시도합니다.

```kotlin
val profiles = suspendMap.suspendMemoizer { id: Long ->
    profileRepository.load(id) // 실패나 CancellationException은 그대로 전파
}
```

첫 계산을 수행하던 coroutine이 취소되면 공유 deferred도 예외로 완료됩니다. 취소를 fallback 값으로 바꾸지 않기 때문에 structured concurrency의 취소 신호가 보존됩니다.

## clear와 세대 경계

동기·async `clear()`는 `inFlight`를 비우고 Redis map도 삭제합니다. 그 순간 이미 실행 중인 evaluator는 나중에 다시 값을 쓸 수 있습니다. suspend 구현의 `clear()`는 Redis map만 비우며 in-flight 계산을 취소하지 않습니다.

`clear()`를 강한 세대 전환이나 모든 계산 취소로 해석하지 않습니다. 배포로 계산 규칙이 바뀌면 map 이름에 버전을 넣어 새 key space를 사용합니다.

## 테스트할 항목

- 첫 miss와 다음 hit에서 evaluator 호출 수
- 같은 JVM의 동시 same-key 요청
- 여러 JVM이 동시에 miss를 만나는 허용 범위
- evaluator 실패 뒤 같은 key 재호출
- suspend evaluator 취소와 job 취소 뒤 복구
- codec 변경, key 문자열 충돌과 오래된 값 처리

## Source와 tests

- [`LettuceMemoizer.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceMemoizer.kt)
- [`LettuceAsyncMemoizer.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceAsyncMemoizer.kt)
- [`LettuceSuspendMemoizer.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceSuspendMemoizer.kt)
- [`LettuceAsyncMemoizerTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/memoizer/LettuceAsyncMemoizerTest.kt)
- [`LettuceSuspendMemoizerTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/memoizer/LettuceSuspendMemoizerTest.kt)
