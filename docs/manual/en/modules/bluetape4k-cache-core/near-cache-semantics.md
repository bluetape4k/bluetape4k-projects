---
title: Near Cache front/back semantics
description: Trace reads, fills, writes, invalidation, and statistics across a local front cache and remote back cache.
manualId: bluetape4k-cache-core
chapterId: near-cache-semantics
---

# Near Cache front/back semantics

## Roles of the two tiers

A Near Cache keeps hot values in a JVM-local front cache and shared values in a back cache. `NearCacheOperations<V>` and its suspend counterpart use `String` keys and standardize reads, writes, deletes, management, and statistics.

```text
get(key)
  ├─ front hit  ───────────────> return
  └─ front miss ─> back lookup ─> front fill ─> return
```

This path moves data between cache tiers; it does not load source data. The caller still owns a database miss when the back cache returns `null`.

## `clearLocal` and `clearAll`

`clearLocal()` empties only the current process's front tier. A later get can refill it from the back cache. `clearAll()` empties both tiers.

Legacy `NearJCache.clear()` also clears only the front, while `clearAllCache()` clears both. A plain back-cache clear may not notify another Near Cache that shares it. Use provider-backed key removal or invalidation when remote local entries must be removed.

## A cache-tier write is not persistence write-through

The common `put` contract requires a provider to update local and back caches along one operation path. It does not mention a database, JDBC repository, or Exposed table.

Partial failure depends on provider ordering. Front-first implementations can leave an uncommitted local value after a failed back write; back-first implementations can fail to fill local state after the remote write succeeds. Verify the selected provider's source and failure tests.

## Listener-backed invalidation

`NearJCache` registers a back-cache entry listener that applies events to the front. `SuspendNearJCache.withoutListener` is a degraded path for environments that cannot serialize the listener into a cluster. It does not promise cross-process invalidation.

Provider event guarantees differ. The 1.12.1 source uses per-key removal where Redisson bulk operations may not emit entry events.

## Reading statistics

`NearCacheStatistics` separates local hits, misses, size, and evictions from back hits and misses. A combined hit rate cannot show whether local caching actually removed network round trips.

- Low local hits and high back hits point to capacity, expiry, or invalidation churn.
- Rising local and back misses point to the cache-aside loader and source-store load.
- Eviction rising with load latency can mean the hot set exceeds local capacity.

## Sources and tests

- [`NearCacheOperations.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheOperations.kt)
- [`SuspendNearCacheOperations.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/SuspendNearCacheOperations.kt)
- [`NearJCache.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt)
- [`SuspendNearJCache.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/SuspendNearJCache.kt)
- [`AbstractNearCacheOperationsTest.kt`](../../../../../cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/AbstractNearCacheOperationsTest.kt)
