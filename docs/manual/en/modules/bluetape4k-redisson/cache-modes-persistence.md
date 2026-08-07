---
title: Cache modes and database persistence
description: Separate cache-aside from MapLoader and MapWriter backed read-through, write-through, and write-behind.
manualId: bluetape4k-redisson
chapterId: cache-modes-persistence
---

# Cache modes and database persistence

## Follow data flow, not preset names

| Strategy | Read miss | Write | Consistency owner |
| --- | --- | --- | --- |
| Cache-aside | Application reads DB and fills cache | Application updates DB and cache | Application service |
| Read-through | `MapLoader` reads DB | Separate policy | Loader and cache configuration |
| Write-through | Cache write waits for `MapWriter` DB write | Synchronous DB persistence | Writer failure reaches caller |
| Write-behind | Queue persists after cache write | Asynchronous batched persistence | Backlog, retry, and shutdown drain |

Calling repository code after `RMap.put()` is cache-aside. Calling it write-through hides its actual transaction and failure boundary.

## What RedissonCacheConfig does

`RedissonCacheConfig` groups cache mode, Redisson `WriteMode`, retry, and local-cache options. Its conversion functions transfer write options but do not create a loader or writer.

```kotlin
val config = RedissonCacheConfig.WRITE_BEHIND.copy(
    writeBehindBatchSize = 100,
    writeBehindDelay = 500,
)

val options = config.toMapOptions<String, User>("users")
    .loader(userLoader)
    .writer(userWriter)

val users = client.getMap(options)
```

The database repository supplies the actual `MapLoader` and `MapWriter`. The loader handles misses; the writer implements database writes and deletes with an explicit transaction boundary.

## Unsupported settings fail fast

The 1.12.1 conversion cannot directly apply `ttl`, `maxSize`, or `deleteFromDBOnInvalidate`. Non-default values fail with `IllegalArgumentException` instead of being ignored.

- Use `RMapCache` entry expiration for TTL.
- Use `nearCacheMaxSize`, `nearCacheTtl`, and `nearCacheMaxIdleTime` for local bounds.
- Implement delete-on-invalidate in the application or repository layer.

## Write-behind operations

Write-behind reduces request-side persistence time by giving up immediate database freshness. A process crash, Redis outage, or writer failure can leave delayed or lost writes.

Before choosing it, define key ordering and coalescing, idempotent retries, backlog and oldest-age metrics, shutdown drain timeout, and a repair path for records present in cache but absent from the database.

## Continue into Exposed and workshops

[bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed) provides the common `JdbcCacheRepository` contract plus the Redisson-specific `AbstractJdbcRedissonRepository`, `ExposedEntityMapLoader`, and `ExposedEntityMapWriter`. Chapter 11 cache-strategy examples in [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop) connect that boundary to `RMap` or `RLocalCachedMap` and Exposed tables. Use this implementation to learn read/write-through; do not treat a plain `put` example as canonical persistence.

## Source and tests

- [`RedissonCacheConfig.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/RedissonCacheConfig.kt)
- [`MapCacheSupport.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/MapCacheSupport.kt)
- [`RedissonCacheConfigTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/cache/RedissonCacheConfigTest.kt)
- [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)
- [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)
