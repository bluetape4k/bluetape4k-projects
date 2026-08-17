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

`NearJCache.clear()` and its compatibility alias `clearAllCache()` both clear this wrapper's front and back caches. A plain back-cache clear may not notify another Near Cache that shares it. Use provider-backed key removal or invalidation when remote local entries must be removed.

The legacy `NearJCache` constructors and provider factories default to
`NearJCacheClearAuthority.DENY`. Therefore `clear()`, `clearAllCache()`, and
no-argument `removeAll()` throw `SecurityException` before changing either tier.
Choose `NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE` only after verifying that
the caller exclusively owns the back namespace. Key-scoped `removeAll(keys)` and
single-key `remove` remain available for tenant-scoped cleanup. The authority is
runtime-only and is not serialized with `NearJCacheConfig`. A direct
`nearCache.backCache.clear()` call is outside the wrapper guard and must remain
caller-owned; do not expose that reference to untrusted code. `ClearBack` on
`ResilientNearJCache` and `ResilientSuspendNearJCache` is outside this PR2
contract.

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

<!-- issue-1369-bulk-policy:start -->
## Bulk `getAll` front residency policy

<!-- contract: default-bypass; bounded-all-or-nothing; single-key-get-unchanged; repeated-back-read; legacy-safe-default -->

```kotlin
val safeDefault = NearJCacheConfig<String, User>()
val bounded = NearJCacheConfig<String, User>(
    bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(128),
)
```

The default `BulkFrontPopulationPolicy.BypassFront` returns front hits and every
back hit without storing the bulk back result in front.
`BulkFrontPopulationPolicy.PopulateIfAtMost(n)` stores the whole batch only when
`backValues.size <= n`; an oversized batch is never partially stored. The entry
count is not resident byte size or a back query size limit, and single-key
`get()` read-through population is unchanged.

The configuration MXBean exposes `BYPASS_FRONT` or `POPULATE_IF_AT_MOST` plus
`bulkFrontPopulationMaximumEntryCount`; `0` means not applicable for bypass.
New configuration and a restored legacy stream both select the safe default.
Correct results do not imply the same residency: repeated `getAll` calls can
repeat back reads and change local hit ratio and back load. The former unlimited
mode is not restored; use an explicit bound only after reviewing front capacity
and local heap budget.
<!-- issue-1369-bulk-policy:end -->

<!-- issue-1351-nearcache-management:start -->
## Explicit NearJCache management

Configure both opt-in flags before construction. The front uses Caffeine through
`NearJCacheConfig.CaffeineCacheManagerFactory`; the example sets exact types and
keeps `setStoreByValue(false)`.

```kotlin
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheClearAuthority
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheConfigurationMXBean
import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheTierStatisticsMXBean
import io.bluetape4k.cache.nearcache.jcache.management.registerMBeans
import java.lang.management.ManagementFactory
import javax.cache.configuration.MutableConfiguration
import javax.management.JMX

val manager = NearJCacheConfig.CaffeineCacheManagerFactory.create()
val configuration = MutableConfiguration<String, String>()
    .setTypes(String::class.java, String::class.java)
    .setStatisticsEnabled(true)
    .setManagementEnabled(true)
    .setStoreByValue(false)
val front = manager.createCache("orders-front", configuration)
val back = manager.createCache("orders-back", configuration)
val nearCache = NearJCache(
    front,
    back,
    NearJCacheConfig(frontCacheConfiguration = configuration),
    NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
)
val server = ManagementFactory.getPlatformMBeanServer()
val registration = nearCache.registerMBeans(server, "orders-service", "orders-v1")
val names = registration.activeObjectNames.associateBy { it.getKeyProperty("type") }
val management = JMX.newMXBeanProxy(server, names.getValue("NearJCacheConfiguration"), NearJCacheConfigurationMXBean::class.java)
val statistics = JMX.newMXBeanProxy(server, names.getValue("NearJCacheStatistics"), NearJCacheTierStatisticsMXBean::class.java)

nearCache.put("42", "Ada")
statistics.clear()
check(nearCache.get("42") != null) // data remains; only counters were reset
nearCache.clear()
check(nearCache.get("42") == null) // front and back data were removed
registration.close()
nearCache.close()
back.close()
```

The factory returns a provider-managed cache manager. Close it only during the
application's provider shutdown, not as part of wrapper cleanup.

Java uses `NearJCacheMBeans.registerMBeans(nearCache, server, managerId, cacheId)`.
The caller owns the `MBeanServer`; the registration does not own the back cache,
cache manager, or provider. IDs must be stable and non-secret because they are
visible in `ObjectName` and recovery errors.

The immutable configuration snapshot and logical/tier counters use
`statisticsScope=NEAR_JCACHE_WRAPPER_V1`. Check `supportedOperations` and the
capabilities `isFrontEvictionObservationSupported`,
`isBulkRemovalCountSupported`, and `isBackWriteCompletionIncluded`; their
current value is `false`, meaning unsupported observation rather than a zero
event count.

For asynchronous writes, API success records caller-visible acceptance. Keep an
inventory of every `BackCacheWriteCompletion`, correlating its diagnostic
`operation` with the stable `operationId` until remote completion. No zero-loss
global drain exists, so stop admission before migration. For synchronous
migration, close the old registration, close the old cache, then register the
replacement. Keep the JMX namespace exclusive, investigate collisions, and
treat `RECOVERY_REQUIRED` as immediate cleanup work. The ownership token only
provides best-effort stale-owner protection.
<!-- issue-1351-nearcache-management:end -->

<!-- nearjcache-clear-authority-contract -->
### #1368 shared-back clear authority

The default wrapper is safe for a shared back namespace. Use key-scoped removal
for a tenant-owned key set; only an explicitly verified exclusive owner may use
namespace-wide clear operations. The enum is runtime-only and does not change
the serializable `NearJCacheConfig`.

```kotlin
val shared = NearJCache(front, back, NearJCacheConfig(), NearJCacheClearAuthority.DENY)
shared.removeAll(setOf("tenant-a:key-1"))
val owner = NearJCache(
    front,
    back,
    NearJCacheConfig(),
    NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
)
owner.clearAllCache()
```
<!-- /nearjcache-clear-authority-contract -->

## Sources and tests

- [`NearCacheOperations.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheOperations.kt)
- [`SuspendNearCacheOperations.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/SuspendNearCacheOperations.kt)
- [`NearJCache.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt)
- `NearJCacheMBeanRegistration.kt` (current SNAPSHOT source: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanRegistration.kt`)
- `NearJCacheTierStatisticsMXBean.kt` (current SNAPSHOT source: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheTierStatisticsMXBean.kt`)
- [`SuspendNearJCache.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/SuspendNearJCache.kt)
- [`AbstractNearCacheOperationsTest.kt`](../../../../../cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/AbstractNearCacheOperationsTest.kt)
