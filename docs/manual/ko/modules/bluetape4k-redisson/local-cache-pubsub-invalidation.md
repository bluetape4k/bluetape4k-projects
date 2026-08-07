---
title: Local Cached Map과 Pub/Sub 무효화
description: RLocalCachedMap의 local front cache, Redis back cache, Pub/Sub 동기화와 reconnect 정책을 설명합니다.
manualId: bluetape4k-redisson
chapterId: local-cache-pubsub-invalidation
---

# Local Cached Map과 Pub/Sub 무효화

## Near Cache가 줄이는 것과 늘리는 것

`RLocalCachedMap`은 자주 읽는 값을 JVM memory에서 반환해 Redis 왕복을 줄입니다. 그 대신 각 application node에 값의 사본이 생기고 invalidation message 전달과 reconnect가 일관성 계약에 들어옵니다. Redis가 잠깐 불안정되었을 때 remote lookup만 실패하는 것이 아니라 stale local value가 남을 수 있습니다.

`localCachedMap`은 이름과 `LocalCachedMapOptions`를 받아 Redisson map을 만듭니다.

```kotlin
val users = localCachedMap<String, User>("users", client) {
    cacheSize(10_000)
    evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
    timeToLive(Duration.ofMinutes(5))
    syncStrategy(LocalCachedMapOptions.SyncStrategy.INVALIDATE)
    reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.LOAD)
}
```

모든 node가 같은 map name과 Codec을 사용해야 같은 remote data와 invalidation channel을 공유합니다.

## INVALIDATE와 UPDATE

`SyncStrategy.INVALIDATE`는 다른 node의 local entry를 제거하고 다음 요청이 Redis에서 다시 읽게 합니다. `UPDATE`는 변경된 값을 local cache에 전파합니다. update message 크기와 빈도, stale 허용 시간, write workload를 보고 선택합니다.

무효화 message는 business event log가 아닙니다. Pub/Sub을 durable queue처럼 사용하면 안 됩니다. 연결이 끊긴 동안 놓친 message를 어떻게 복구할지는 `ReconnectionStrategy`와 TTL이 담당합니다.

## Reconnection policy

1.12.1 `RedissonNearCache.defaultLocalCacheOptions`는 LFU, local TTL 60초, max idle 120초, `ReconnectionStrategy.LOAD`, `SyncStrategy.UPDATE`를 설정합니다. 이 값은 편리한 기본값이지 모든 workload의 정답이 아닙니다.

- stale data를 거의 허용하지 못하면 짧은 TTL과 reconnect clear/load 정책을 검토합니다.
- update 빈도가 높으면 local cache invalidation traffic이 Redis 절감분보다 커질 수 있습니다.
- cache stampede를 피하려면 reconnect 직후 miss 폭증을 부하 테스트합니다.

## destroy의 정확한 범위

`RedissonNearCache.destroy()`는 `frontCache.destroy()`만 호출합니다. Redis의 remote map data는 삭제하지 않습니다. 이는 application instance 종료와 shared data 삭제를 분리하기 위한 계약입니다.

전체 Redis data를 지우려면 명시적으로 map delete/clear 정책을 적용해야 하며, 일반 shutdown hook에서 실행하면 안 됩니다. 반대로 near-cache instance를 오래 교체하면서 `destroy()`하지 않으면 local listener와 resource가 남을 수 있습니다.

## Pattern invalidation 비용

`RedisCacheInvalidationStrategy.invalidateByPattern`은 `keySet(pattern)`으로 key를 찾은 뒤 `fastRemove`합니다. 넓은 pattern은 큰 key scan과 삭제를 만들 수 있습니다. cache key namespace를 좁게 설계하고 운영 경로에서는 대상 수와 latency를 측정합니다.

## Source와 tests

- [`LocalCacheMapSupport.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/LocalCacheMapSupport.kt)
- [`RedissonNearCache.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/nearcache/RedissonNearCache.kt)
- [`CacheInvalidationStrategy.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/CacheInvalidationStrategy.kt)
- [`LocalCacheMapSupportTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/cache/LocalCacheMapSupportTest.kt)
- [`RedissonNearCacheTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/nearcache/RedissonNearCacheTest.kt)
