---
title: 실패, 취소, 수명주기와 운영
description: Redis 장애와 partial failure, coroutine 취소, close 계약과 운영 지표를 정리합니다.
manualId: bluetape4k-cache-redisson
chapterId: failures-cancellation-operations
---

# 실패, 취소, 수명주기와 운영

## 실패를 계층별로 본다

| 실패 지점 | 1.12.1 동작 | 운영 판단 |
| --- | --- | --- |
| JCache CRUD | Redisson future 예외를 `await()`가 전파 | timeout과 retry owner를 한 계층에 둡니다. |
| sync memoizer evaluator | promise를 실패시키고 in-flight 제거 | 다음 요청이 evaluator를 다시 실행할 수 있습니다. |
| async memoizer Redis put | warning 뒤 evaluator 결과로 future 완료 가능 | cache 저장 실패를 성공 계산과 분리해 관측합니다. |
| suspend memoizer 취소 | deferred 실패, in-flight 제거, 취소 재전파 | 취소를 fallback 값으로 바꾸지 않습니다. |
| sync Near Cache close | destroy 예외를 소비 | close 실패를 데이터 삭제 성공으로 해석하지 않습니다. |
| suspend close | 일반 예외는 warning, 취소는 재전파 | shutdown timeout과 취소 로그를 구분합니다. |

## cache 장애가 원본 장애로 번지는 경로

cache-aside에서 Redis miss나 오류가 늘면 첫 요청은 DB에서 읽고 cache를 다시 채웁니다. 많은 key가 동시에 miss하면 원본 pool과 downstream이 먼저 포화될 수 있습니다. retry 횟수만 늘리면 같은 요청이 Redis와 DB 양쪽을 압박해 cache가 장애 증폭기가 됩니다.

timeout, retry budget, stale fallback 허용 범위, 원본 저장소 bulkhead를 함께 정합니다. 쓰기 실패를 읽기 fallback으로 숨기지 않습니다.

## cancellation은 실패 값이 아니다

suspend memoizer evaluator가 취소되면 Redis에 값을 저장하지 않습니다. in-flight deferred도 제거되므로 다음 호출은 새 계산으로 복구합니다. waiter가 취소된 것과 evaluator owner가 취소된 것은 다르므로 structured concurrency에서 owner scope를 명확히 둡니다.

## close 순서

1. 새 cache 요청 유입을 중단합니다.
2. 필요하면 진행 중인 evaluator를 기다리거나 취소합니다.
3. wrapper를 close합니다.
4. 마지막 owner가 Redisson client와 JCache provider를 닫습니다.

entry를 지워야 한다면 close 전에 `clear()` 또는 `clearAll()`을 별도 정책으로 실행합니다. 일반 shutdown에서 공유 Redis 데이터를 자동 삭제하지 않습니다.

## 관측할 값

- Redis command latency, timeout, connection과 reconnect
- Pub/Sub invalidation 지연과 reconnect 뒤 local clear
- memoizer evaluator 횟수, latency, failure, cancellation
- cache get 결과와 실제 Redis network metrics
- local entry 수, Redis map 크기, eviction과 decode 오류
- cache failure 시 원본 DB pool saturation과 query latency

Near Cache의 `stats()`만으로 local hit rate를 계산하지 않습니다. Redisson과 Redis telemetry를 같은 시간축에 둡니다.

## Testcontainers 검증

module test는 실제 Redis Testcontainer를 사용합니다. 장애·취소 테스트까지 포함하므로 다른 무거운 backend test와 직렬 실행합니다.

```bash
./gradlew :bluetape4k-cache-redisson:test --no-build-cache --no-configuration-cache
```

## Source와 tests

- [`RedissonSuspendMemoizerTest.kt`](../../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/memoizer/RedissonSuspendMemoizerTest.kt)
- [`ResilientRedissonNearCacheTest.kt`](../../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientRedissonNearCacheTest.kt)
- [`ResilientRedissonSuspendNearCacheTest.kt`](../../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientRedissonSuspendNearCacheTest.kt)
- [`RedisServers.kt`](../../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/RedisServers.kt)
