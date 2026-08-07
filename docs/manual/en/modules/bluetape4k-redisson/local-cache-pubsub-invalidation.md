---
title: Local cached maps and Pub/Sub invalidation
description: Understand the JVM front cache, Redis back cache, Pub/Sub synchronization, and reconnect policy of RLocalCachedMap.
manualId: bluetape4k-redisson
chapterId: local-cache-pubsub-invalidation
---

# Local cached maps and Pub/Sub invalidation

## Near Cache removes round trips and adds copies

`RLocalCachedMap` can answer hot reads from JVM memory. It also creates one copy per application node, so invalidation delivery and reconnect behavior become part of consistency. A Redis interruption can leave stale local values rather than causing only remote lookup failures.

```kotlin
val users = localCachedMap<String, User>("users", client) {
    cacheSize(10_000)
    evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
    timeToLive(Duration.ofMinutes(5))
    syncStrategy(LocalCachedMapOptions.SyncStrategy.INVALIDATE)
    reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.LOAD)
}
```

All nodes must use the same map name and codec to share remote data and invalidation traffic.

## INVALIDATE or UPDATE

`SyncStrategy.INVALIDATE` removes another node's local entry and makes its next read fetch Redis. `UPDATE` sends the changed value to local caches. Choose from update frequency, message size, and acceptable stale time.

Invalidation Pub/Sub is not a durable business event log. `ReconnectionStrategy` and TTL define recovery for messages missed while disconnected.

## Reconnection policy

In 1.12.1, `RedissonNearCache.defaultLocalCacheOptions` uses LFU, a 60-second local TTL, 120-second max idle, `ReconnectionStrategy.LOAD`, and `SyncStrategy.UPDATE`. These are usable defaults, not workload-independent answers.

Test the miss spike after reconnect, invalidation traffic under write load, and the maximum stale interval after an outage.

## The exact scope of destroy

`RedissonNearCache.destroy()` calls `frontCache.destroy()` only. It leaves the Redis map intact, separating instance shutdown from shared-data deletion. Explicit map deletion belongs to a separate administrative policy, never a normal shutdown hook.

Destroy near-cache instances that are replaced during runtime so local listeners and resources do not accumulate.

## Pattern invalidation cost

`RedisCacheInvalidationStrategy.invalidateByPattern` finds matching keys with `keySet(pattern)` and removes them with `fastRemove`. Broad patterns can scan and delete many keys. Use narrow namespaces and observe target count and latency.

## Source and tests

- [`LocalCacheMapSupport.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/LocalCacheMapSupport.kt)
- [`RedissonNearCache.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/nearcache/RedissonNearCache.kt)
- [`CacheInvalidationStrategy.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/CacheInvalidationStrategy.kt)
- [`LocalCacheMapSupportTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/cache/LocalCacheMapSupportTest.kt)
- [`RedissonNearCacheTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/nearcache/RedissonNearCacheTest.kt)
