---
title: Redisson JCache and suspend wrappers
description: Provider creation, named-cache reuse, coroutine bridges, and atomicity boundaries.
manualId: bluetape4k-cache-redisson
chapterId: redisson-jcache-suspend
---

# Redisson JCache and suspend wrappers

## Two entry points

`RedissonJCaching` uses Redisson's `JCachingProvider` to create standard `javax.cache.Cache` instances. `RedissonSuspendJCache` wraps the same cache with `SuspendJCache`. `RedissonCaches` exposes both choices through one factory.

```kotlin
val cache = RedissonCaches.suspendJCache<String, User>(
    redisson = redissonClient,
    cacheName = "users",
    configuration = MutableConfiguration<String, User>().apply {
        setTypes(String::class.java, User::class.java)
    },
)
cache.put("u:1", User("u:1", "debop"))
```

Passing a `RedissonClient` keeps ownership in the application. A `Config` overload allows the provider to participate in client creation, so document manager and client shutdown separately.

## Named-cache reuse

`RedissonJCaching.cacheManager` is a lazy singleton. `getOrCreate` reuses an existing named cache. Do not assume a new configuration replaces the type, expiry, loader, or writer of an existing cache. Tests should use unique names and clean up their entries or wrappers.

## Awaiting async operations

Most CRUD methods call Redisson JCache `*Async()` methods and `await()` them. `putAllFlow` starts individual puts and waits for all of them. `entries()` iterates the cache directly, so a large scan is not automatically a cheap non-blocking stream. `removeAll()` explicitly runs the blocking provider call on `Dispatchers.IO`.

## `getAndPut` is not atomic

Redisson JCache has no `getAndPutAsync()` in this path. The 1.12.1 wrapper performs `get` followed by `put`, allowing another writer to interleave. Use an atomic `RMap` operation when strict read-modify-write semantics matter.

## Close is not deletion

`close()` closes the wrapped JCache but does not delete stored entries. Use `clear()` or `removeAll()` for data removal. Ordinary close failures are logged; `CancellationException` is rethrown.

## Source and tests

- [`RedissonJCaching.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/jcache/RedissonJCaching.kt)
- [`RedissonSuspendJCache.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/jcache/RedissonSuspendJCache.kt)
- [`RedissonSuspendJCacheTest.kt`](../../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/jcache/RedissonSuspendJCacheTest.kt)
