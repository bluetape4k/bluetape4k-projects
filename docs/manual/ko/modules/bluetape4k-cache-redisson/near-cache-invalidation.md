---
title: RLocalCachedMap Near Cache와 무효화
description: Local tier, Redis tier, Pub/Sub invalidation, clear 범위와 통계 해석을 설명합니다.
manualId: bluetape4k-cache-redisson
chapterId: near-cache-invalidation
---

# RLocalCachedMap Near Cache와 무효화

## Redisson이 두 계층을 관리한다

`RedissonNearCache`와 `RedissonSuspendNearCache`는 `RLocalCachedMap<String, V>`를 감쌉니다. 별도 Caffeine front와 Redis back을 애플리케이션이 조합하는 대신 Redisson이 local entry, Redis map과 Pub/Sub invalidation을 함께 관리합니다.

```text
application -> RLocalCachedMap -> local entry
                           \----> Redis map
other client update -> Pub/Sub invalidation -> local entry 제거
```

기본 `SyncStrategy.INVALIDATE`는 변경 내용을 local cache에 복제하기보다 해당 entry를 무효화합니다. 다음 읽기가 Redis에서 최신 값을 가져와 local entry를 다시 채웁니다.

## local clear와 전체 clear

`clearLocal()`은 현재 wrapper의 local entry만 지웁니다. Redis map은 남으므로 다음 get이 다시 값을 가져올 수 있습니다. `clearAll()`은 local과 Redis entry를 모두 지웁니다.

`close()`는 wrapper lifecycle 종료입니다. 동기·suspend 구현 모두 local cached map을 `destroy()`하지만, 데이터를 지우려는 운영 명령은 `clearAll()`로 따로 표현해야 합니다.

## reconnect에서 stale entry 줄이기

기본 `ReconnectionStrategy.CLEAR`는 연결이 끊긴 동안 invalidation event를 놓쳤을 가능성을 고려해 reconnect 시 local cache를 비웁니다. `LOAD` 같은 다른 전략을 선택하려면 event log와 reconnect 비용, stale 허용 시간을 Redisson 동작과 함께 검증합니다.

Redis가 잠깐 불안정되었을 때 local cache가 계속 응답할 수 있는지는 설정과 Redisson 상태에 달려 있습니다. 모든 읽기가 안전한 stale fallback이라고 가정하지 않습니다.

## bulk 연산과 무효화

native Near Cache는 Redisson `fastRemove`/`fastRemoveAsync`로 여러 key를 한 번에 지웁니다. JCache 기반 legacy Near Cache는 Redisson이 bulk entry event를 내지 않는 경로를 고려해 listener 전파와 key별 제거를 검증합니다. 두 구현의 event 모델을 같은 것으로 취급하지 않습니다.

## 통계의 한계

1.12.1 구현은 `RLocalCachedMap`에서 local hit와 Redis hit를 따로 얻지 못합니다. 그래서 `localHits`, `localMisses`, `localEvictions`은 0이고, `backHits`와 `backMisses`는 wrapper의 통합 get 결과를 셉니다.

이 값은 API 사용량을 보는 보조 지표입니다. 실제 network round trip, Redis hit rate와 local cache 효과는 Redisson·Redis metrics와 함께 측정합니다.

## Source와 tests

- [`RedissonNearCache.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCache.kt)
- [`RedissonSuspendNearCache.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonSuspendNearCache.kt)
- [`RedissonNearCacheTest.kt`](../../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheTest.kt)
- [`RedissonSuspendNearCacheTest.kt`](../../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/nearcache/RedissonSuspendNearCacheTest.kt)
