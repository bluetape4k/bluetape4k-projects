---
title: Configuration, codecs, and client ownership
description: Set near-cache defaults, validate limits, migrate wire formats, and assign Redisson client lifecycle ownership.
manualId: bluetape4k-cache-redisson
chapterId: config-codec-ownership
---

# Configuration, codecs, and client ownership

## Make defaults explicit

| Setting | 1.12.1 default | Meaning |
| --- | --- | --- |
| `cacheName` | `redisson-near-cache` | Redis map name |
| `maxLocalSize` | `10_000` | Maximum local entries per JVM |
| `timeToLive` | `null` | No Redis TTL |
| `maxIdle` | `null` | No idle expiry |
| `syncStrategy` | `INVALIDATE` | Invalidate after another client writes |
| `reconnectionStrategy` | `CLEAR` | Clear local entries after reconnect |
| `evictionPolicy` | `LRU` | Local eviction policy |

Use a service/data/environment-specific name instead of the default. With both durations unset, Redis entries do not expire because of time.

## Validation and scope

Names cannot be blank, local size must be positive, and configured TTL/idle durations must be greater than zero.

```kotlin
val config = redissonNearCacheConfig {
    cacheName = "catalog:products:v1"
    maxLocalSize = 2_000
    timeToLive = Duration.ofMinutes(15)
    maxIdle = Duration.ofMinutes(5)
}
```

TTL applies to Redis entries. Local size and eviction apply to each JVM. They are not two spellings of one capacity limit.

## Codec as a data contract

The default near-cache codec is `RedissonCodecs.LZ4Fory`. A codec is a wire format, not just a compression toggle. If old and new deployments share one map with incompatible codecs, reads can fail. Use a compatible rollout or versioned map names.

JCache `Configuration<K, V>` also carries key/value types, expiry, loader, and writer. Reopening an existing name does not guarantee that a new configuration replaces the old one.

## One client owner

Usually the DI container creates and closes one `RedissonClient`. Near-cache `close()` and memoizer `clear()` do not shut it down. `Config`-based JCache creation can involve provider-owned clients, so verify the global manager/provider lifecycle separately and isolate test cache names.

## Source and tests

- [`RedissonNearCacheConfig.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheConfig.kt)
- [`RedissonCaches.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/RedissonCaches.kt)
- [`RedissonNearCacheConfigTest.kt`](../../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheConfigTest.kt)
