---
title: JCache near-cache serialization limits
description: Explain listener-factory serialization failures and the capability of listener-free factories.
manualId: bluetape4k-cache-hazelcast
chapterId: jcache-near-cache-serialization
---

# JCache near-cache serialization limits

## Why direct listener registration fails

Direct `NearJCache(config, backCache)` construction registers a `MutableCacheEntryListenerConfiguration` so back-cache events can update the front cache. When the listener factory captures a Caffeine cache proxy, Hazelcast cannot serialize that configuration for the cluster. The public `HazelcastNearJCache(...)` factory no longer takes this listener-backed path.

Release tests explicitly expect `HazelcastSerializationException` with an underlying `NotSerializableException` from the direct listener-backed construction. That constructor remains unsupported for a Hazelcast client JCache back cache.

## Hazelcast factory methods create a listener-free composition

`HazelcastCaches.nearJCache` creates a Caffeine front JCache and Hazelcast back JCache without listener registration. The public `HazelcastNearJCache(...)` factory combines the caller-supplied front JCache with a Hazelcast back JCache through the same listener-free path. `suspendNearJCache` also creates a fixed Caffeine front and calls `SuspendNearJCache.withoutListener`.

Serialization-safe `NearJCacheConfig` and the destructive-clear authority are
separate contracts. Existing Hazelcast factory calls default to
`NearJCacheClearAuthority.DENY`; use a key-scoped `removeAll(keys)` for shared
namespaces. Pass `NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE` only when the
caller owns the entire back namespace before calling `clear()` or
`clearAllCache()`. The authority is runtime-only and is not sent through
Hazelcast configuration serialization. The wrapper closes only its supplied
front cache; it does not close the Hazelcast back cache or provider.

```kotlin
import io.bluetape4k.cache.nearcache.jcache.NearJCacheClearAuthority

val cache = HazelcastCaches.nearJCache<String, User>(
    hazelcast,
    NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
) {
    cacheName = "users-v1"
}

cache.put("42", user)
check(cache.getDeeply("42") == user)
cache.clear()             // front and back
check(cache.getDeeply("42") == null)
```

Read-through and two-tier writes work, but another process can change the back cache without evicting this front cache. Factory success does not imply peer invalidation support.

## Distinguish the native IMap near cache

When peer invalidation is required, use the `IMap.addEntryListener` path in `HazelcastNearCache`. That listener runs in the client JVM, so capturing Caffeine L1 does not create the JCache listener-factory serialization problem.

| Choice | Benefit | Limit |
| --- | --- | --- |
| factory JCache near cache | Reuses JCache front/back contracts | No listener or peer L1 propagation |
| direct listener-backed JCache construction | Intended event propagation | Serialization failure in 1.12.1 |
| native IMap near cache | Client-side listener invalidation | String keys and no JCache API |

## Fixed suspend-front settings

The 1.12.1 `suspendNearJCache` factory builds Caffeine with a 10,000-entry maximum and 30-minute expire-after-access. It uses the supplied cache name for the back cache but does not apply custom front capacity and expiry from `NearJCacheConfig` to this fixed front.

<!-- nearjcache-clear-authority-contract -->
### #1368 shared-back clear authority

The listener-free factory still defaults to `DENY`; a key-scoped operation is the
safe path for a shared namespace. An exclusive owner may opt in to a namespace
clear, while the runtime-only authority remains outside serialized configuration.

```kotlin
val shared = HazelcastCaches.nearJCache<String, User>(hazelcast)
shared.removeAll(setOf("tenant-a:key-1"))
val owner = HazelcastCaches.nearJCache<String, User>(
    hazelcast,
    NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
) { cacheName = "users-owner" }
owner.clear()
```
<!-- /nearjcache-clear-authority-contract -->

## Sources and tests

- [`HazelcastCaches.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt)
- [`HazelcastNearJCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCache.kt)
- [`HazelcastSuspendNearJCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastSuspendNearJCache.kt)
- [`HazelcastNearJCacheTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCacheTest.kt)
- [`HazelcastSuspendNearJCacheTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastSuspendNearJCacheTest.kt)
