---
title: Lifecycle, failures, and operations
description: Operate RegionFactory startup and shutdown, Redis fallback, eviction, and cache observability.
manualId: bluetape4k-hibernate-cache-lettuce
chapterId: lifecycle-failures-operations
---

# Lifecycle, failures, and operations

## SessionFactory owns resources

On `prepareForUse`, Hibernate validates settings and the codec, creates a `RedisClient`, applies RESP3 when enabled, assigns the client, and registers it with `ShutdownQueue`.

Shutdown order matters:

1. Close every region Near Cache and tracking connection.
2. Clear the region map.
3. Shut down and null the shared `RedisClient`.

Closing the client first would leave caches trying to clean up through a stopped client. `StorageAccess.release()` is intentionally a no-op because RegionFactory owns shared caches. Close the SessionFactory explicitly to run this lifecycle.

## Convert cache failures into database fallback

| Operation | Result after cache failure |
| --- | --- |
| `getFromCache` | Warn and return `null`; Hibernate reads the database |
| `putIntoCache` | Warn and ignore; transaction continues |
| `contains` | Warn and return `false` |
| Key or region eviction | Warn and ignore |

This prevents a Redis outage from directly failing a business transaction. It can instead create a miss storm against the database or leave stale local entries after failed eviction. Correlate cache errors with database-pool pressure.

## Eviction scope

`evictData(key)` removes L1 state and calls Redis `UNLINK`. Region-wide `evictData()` clears L1, scans `${regionName}:*`, and unlinks batches.

`clearAll()` is not constant-time. Large regions need multiple scan round trips. Avoid routine global eviction; plan namespace or version transitions for schema and serialization changes.

## Operational signals

- Hibernate second-level hits, misses, and puts
- Per-region `CacheRegionStatistics`
- Query-cache and update-timestamps activity
- Caffeine size and hit rate with stats enabled
- Redis latency, errors, reconnects, and connections
- Database latency and pool active or pending counts
- Region eviction duration and deleted-key count

A high hit rate does not compensate for stale reads or a fallback overload. Compare p95/p99, database load, and correctness against a cache-disabled baseline.

## Failure drills

Stop Redis briefly and verify database fallback, pool capacity, and repopulation after recovery. Modify a key through another process to verify RESP3 invalidation. Before changing codecs, prove backward readability or prepare a bounded region-eviction procedure.

## Sources and tests

- [`LettuceNearCacheRegionFactory.kt`](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt)
- [`LettuceNearCacheStorageAccess.kt`](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccess.kt)
- [`HibernateCacheStatisticsTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateCacheStatisticsTest.kt)
- [`HibernateCacheContainmentTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateCacheContainmentTest.kt)
- [`LettuceNearCacheRegionFactoryTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactoryTest.kt)
