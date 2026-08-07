---
title: Local providers and JCache
description: Compare the real defaults of the Caffeine, Cache2k, and Ehcache helpers, JCache managers, expiry, and coroutine APIs.
manualId: bluetape4k-cache-core
chapterId: local-providers-jcache
---

# Local providers and JCache

## Choose the smallest provider

Caffeine fits small heap caches and loading caches. The `caffeine {}` helper only wraps its builder, so the application must set maximum size, expiry, and statistics.

```kotlin
val cache = caffeine {
    maximumSize(5_000)
    expireAfterAccess(Duration.ofMinutes(5))
    recordStats()
}.build<String, Product>()
```

The Cache2k helpers provide typed builders and a default manager. The Ehcache helpers connect heap, off-heap, and disk resource pools. Both provider implementations are `compileOnly` in the 1.12.1 build, so applications using them must supply the runtime dependency.

## JCache managers and names

`JCaching.Caffeine`, `JCaching.Cache2k`, and `JCaching.EhCache` lazily create one provider manager and get or create caches by name. `getOrCreate` uses an internal lock to avoid duplicate creation within one manager.

```kotlin
val cache = JCaching.Caffeine.getOrCreate<String, User>(
    "users-by-id",
    jcacheConfiguration {
        setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES))
        isStatisticsEnabled = true
    },
)
```

A cache name is an identity within the manager. Reusing a name with a different value type can make typed `getCache` fail, so use a stable naming rule in production and tests.

## Eternal does not mean unlimited capacity

`getDefaultJCacheConfiguration()` uses `EternalExpiryPolicy`. Time does not expire an entry, but the provider may still evict it because of capacity. `NearJCacheConfig` uses a different default for its front cache: 30 minutes after access. Verify the configuration path rather than assuming one module-wide expiry.

## Coroutine boundary

`CaffeineSuspendJCache` adapts a Caffeine `AsyncCache` and awaits its futures.

```kotlin
val cache = CaffeineSuspendJCache<String, User> {
    maximumSize(1_000)
    expireAfterWrite(Duration.ofMinutes(10))
}

cache.put("42", user)
check(cache.get("42") == user)
cache.close()
```

`close()` runs once, invalidates entries, and requests cleanup. Listener registration on this local implementation only logs a debug message. Choose a remote provider when listener-backed invalidation is required.

## Sources and tests

- [`CaffeineSupport.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/caffeine/CaffeineSupport.kt)
- [`Cache2kSupport.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupport.kt)
- [`EhcacheSupport.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/ehcache/EhcacheSupport.kt)
- [`JCacheSupport.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheSupport.kt)
- [`CaffeineSuspendJCacheTest.kt`](../../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/CaffeineSuspendJCacheTest.kt)
