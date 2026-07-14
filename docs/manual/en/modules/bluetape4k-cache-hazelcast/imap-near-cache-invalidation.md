---
title: IMap near-cache invalidation
description: Follow Caffeine L1 and Hazelcast IMap L2 reads, writes, entry-listener invalidation, and statistics.
manualId: bluetape4k-cache-hazelcast
chapterId: imap-near-cache-invalidation
---

# IMap near-cache invalidation

## Only L1 misses reach IMap

Both native near-cache implementations use String keys. They read Caffeine L1 first, then query the `IMap` named by `cacheName` and populate L1 on a hit. `getAll` groups only L1 misses into `IMap.getAll`.

```kotlin
val products = HazelcastCaches.nearCache<Product>(hazelcast) {
    cacheName = "products-v1"
    maxLocalSize = 5_000
    frontExpireAfterWrite = Duration.ofMinutes(5)
    recordStats = true
}
```

`frontExpireAfterWrite` and `frontExpireAfterAccess` affect only Caffeine L1. Configure IMap TTL and eviction separately.

## Entry events invalidate other L1 copies

Construction registers `IMap.addEntryListener(listener, true)`. Update and remove events invalidate the key when the code classifies the member as remote, while expiry always invalidates and add events are ignored.

Events travel asynchronously from the write response. The listener reduces the stale window but does not make reads linearizable. Verify `event.member.localMember()` behavior in the actual client or member topology.

## Writes change L1 first

`put`, `putAll`, `remove`, and `removeAll` modify L1 before calling IMap. A failed IMap operation can therefore leave only the local tier changed.

```text
put(key, value)
  1. Caffeine L1 put
  2. IMap set
```

`replace` updates L1 only after the IMap result. `putIfAbsent` performs a normal read first and then checks the atomic IMap winner. Failure state differs by operation.

## Clear, close, and statistics

`clearLocal` clears only the current JVM L1. `clearAll` also clears the shared IMap. `close` removes the listener and closes L1 while retaining map data and the Hazelcast instance.

The `isClosed` flag makes close idempotent, but operations do not uniformly fail by checking that flag. Remove closed objects from application scope rather than reusing them.

`stats()` combines Caffeine local counters with code-maintained IMap hits and misses. With `recordStats=false`, local hits, misses, and evictions can appear as zero even under traffic.

## Sources and tests

- [`HazelcastNearCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCache.kt)
- [`HazelcastSuspendNearCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastSuspendNearCache.kt)
- [`HazelcastEntryEventListener.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastEntryEventListener.kt)
- [`HazelcastLocalCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastLocalCache.kt)
- [`HazelcastNearCacheTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCacheTest.kt)
