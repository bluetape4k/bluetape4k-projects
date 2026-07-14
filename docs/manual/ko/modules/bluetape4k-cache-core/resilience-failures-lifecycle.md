---
title: Retry, 실패와 수명주기
description: Near Cache decorator의 retry, read fallback, write exception, coroutine cancellation과 close 동작을 검증합니다.
manualId: bluetape4k-cache-core
chapterId: resilience-failures-lifecycle
---

# Retry, 실패와 수명주기

## Retry는 remote cache 호출을 반복한다

`withResilience`는 공통 Near Cache interface를 감싸고 Resilience4j retry를 적용합니다.

```kotlin
val resilient = cache.withResilience {
    retryMaxAttempts = 3
    retryWaitDuration = Duration.ofMillis(200)
    retryExponentialBackoff = true
    getFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL
}
```

기본 최대 시도는 3회이고 첫 대기는 500 ms입니다. 횟수와 대기 시간은 모두 0보다 커야 합니다. retry 대상이 side effect를 갖는다면 provider operation이 idempotent한지 먼저 확인합니다.

## Read와 write의 실패 정책

읽기는 두 전략 중 하나를 고릅니다.

| 연산 | `RETURN_FRONT_OR_NULL` | `PROPAGATE_EXCEPTION` |
| --- | --- | --- |
| `get` | 경고 후 `null` | 마지막 예외 전파 |
| `getAll` | 경고 후 빈 map | 마지막 예외 전파 |
| `containsKey` | 경고 후 `false` | 마지막 예외 전파 |

이 fallback 값은 “entry가 없다”와 “backend가 실패했다”를 같은 결과로 보이게 합니다. availability를 우선할 때만 선택하고 error counter와 log를 반드시 관찰합니다.

`put`, `replace`, `remove`, `clearAll` 같은 변경 연산은 retry를 소진하면 예외를 전파합니다. 성공하지 않은 쓰기를 조용히 무시하면 오래된 local 값과 back 값이 갈라질 수 있기 때문입니다.

## Coroutine 취소

`ResilientSuspendNearCacheDecorator`는 `CancellationException`을 retry 대상에서 제외합니다. read fallback 전략이 `RETURN_FRONT_OR_NULL`이어도 취소를 `null`로 바꾸지 않습니다. `put`과 `close` 도중 취소도 그대로 전파합니다.

```kotlin
val job = launch {
    resilient.get("slow-key")
}
job.cancelAndJoin() // retry loop로 되살아나지 않음
```

이 경계가 깨지면 상위 scope가 취소됐는데도 cache 작업이 계속되어 구조적 동시성이 무너집니다.

## Close와 소유권

blocking decorator는 delegate `close()`의 일반 예외를 경고로 남기고 무시합니다. suspend decorator도 일반 예외는 무시하지만 cancellation은 다시 던집니다. 종료 실패를 무시하더라도 connection·thread가 남지 않았다는 뜻은 아니므로 provider metric과 process shutdown log를 확인합니다.

local provider manager와 remote client를 누가 닫는지 factory 문서에서 확인합니다. cache wrapper만 닫고 공유 client까지 닫아 다른 cache를 깨뜨리거나, 반대로 아무도 client를 닫지 않는 두 경우를 모두 피해야 합니다.

## Cache stampede는 retry로 해결되지 않는다

retry는 실패한 remote 호출을 다시 실행합니다. hot key의 동시 miss를 하나로 합치지는 않습니다. 오히려 backend가 느릴 때 여러 caller가 각각 retry하면 장애를 증폭할 수 있습니다. same-key memoizer, provider loader 병합, 작은 jitter·timeout과 원본 저장소 보호 장치를 함께 설계합니다.

## Source와 tests

- [`NearCacheResilienceConfig.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheResilienceConfig.kt)
- [`ResilientNearCacheDecorator.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCacheDecorator.kt)
- [`ResilientSuspendNearCacheDecorator.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecorator.kt)
- [`ResilientNearCacheDecoratorTest.kt`](../../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientNearCacheDecoratorTest.kt)
- [`ResilientSuspendNearCacheDecoratorTest.kt`](../../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCacheDecoratorTest.kt)
