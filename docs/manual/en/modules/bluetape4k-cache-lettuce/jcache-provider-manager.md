---
title: JCache provider, manager, and configuration
description: Understand JCache SPI lookup, manager identity, Redis hash storage, TTL, codecs, and resource ownership.
manualId: bluetape4k-cache-lettuce
chapterId: jcache-provider-manager
---

# JCache provider, manager, and configuration

## Find the provider through SPI

`LettuceCachingProvider` is registered in `META-INF/services/javax.cache.spi.CachingProvider`. JCache can discover it, and the provider reuses one `LettuceCacheManager` for each `(ClassLoader, URI)` pair.

```kotlin
val provider = Caching.getCachingProvider(
    "io.bluetape4k.cache.jcache.LettuceCachingProvider"
)
val manager = provider.getCacheManager(
    URI("redis://redis.example:6379/2"),
    Thread.currentThread().contextClassLoader,
)
```

Without a URI, the provider uses its logical `lettuce-jcache-default` URI and connects to `redis://localhost:6379`. The ClassLoader is part of manager identity, which matters in plugin and application-server environments.

## RedisClient ownership

A provider-created manager also creates and shuts down its `RedisClient`. `LettuceJCaching.cacheManagerOf(redisClient)` instead reuses an application-owned client and does not close it.

```kotlin
val redisClient = RedisClient.create("redis://localhost:6379")
val cache = LettuceJCaching.getOrCreate<String, User>(
    redisClient, "users-v1", ttlSeconds = 600,
)

try {
    cache.put("42", user)
} finally {
    cache.close()
    redisClient.shutdown()
}
```

`LettuceJCaching` keeps one manager per client. Align manager and client lifetime at the application boundary instead of repeatedly closing and reopening the cached manager.

## Redis hash layout

One JCache maps to one Redis hash. A JCache key becomes a string hash field, and the value becomes bytes produced by the selected `LettuceBinaryCodec`.

```text
Redis hash: users-v1
  field "42" -> <lz4+fory bytes>
  field "84" -> <lz4+fory bytes>
```

The default key encoder calls `toString()`. Iteration over non-String keys also needs a decoder.

```kotlin
val config = lettuceCacheConfigOf<Int, User>(
    ttlSeconds = 600,
    keyCodec = Int::toString,
    keyDecoder = String::toInt,
)
```

Direct lookup can work without the decoder, but iteration cannot reconstruct the key and throws `CacheException`.

## TTL covers the whole hash

`LettuceCacheConfig.ttlSeconds` is not per-entry expiry. A successful write refreshes the TTL of the Redis hash, so all entries under the cache name expire together.

- Redis 8+ uses the detected `HSETEX` path.
- Older servers fall back to `HSET` or `HMSET` followed by `EXPIRE`.
- `null` means no expiry.
- Continued writes extend the whole hash lifetime.

Use separate cache names or the native near cache when entries need independent TTLs.

## Manager validation and deletion

`createCache` rejects blank and duplicate names. Typed `getCache` rejects key or value types that differ from the stored configuration. Operations after manager close fail with `IllegalStateException`.

`destroyCache` clears the Redis hash and closes the cache connection. `cache.close()` only removes the wrapper and closes its connection; Redis data remains. Do not couple process shutdown to data deletion.

## Sources and tests

- [`LettuceCachingProvider.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCachingProvider.kt)
- [`LettuceCacheManager.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCacheManager.kt)
- [`LettuceCacheConfig.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCacheConfig.kt)
- [`LettuceCachingProviderTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceCachingProviderTest.kt)
- [`LettuceJCacheManagerTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceJCacheManagerTest.kt)
