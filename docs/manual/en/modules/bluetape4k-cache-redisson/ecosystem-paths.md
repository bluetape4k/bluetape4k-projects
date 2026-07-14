---
title: Cache strategies and ecosystem paths
description: Distinguish cache-aside from read/write-through/behind and continue into persistence modules and workshops.
manualId: bluetape4k-cache-redisson
chapterId: ecosystem-paths
---

# Cache strategies and ecosystem paths

## Follow the data path

| Strategy | Miss/write owner | Source-store call |
| --- | --- | --- |
| Cache-aside | Caller | Caller reads and writes the source directly |
| Read-through | `CacheLoader`/`MapLoader` | A cache miss invokes the loader |
| Write-through | `CacheWriter`/`MapWriter` | A cache write invokes the writer before success |
| Write-behind | Writer queue | The source is updated asynchronously after the cache |

`RedissonNearCache.put()` and memoizer `RMap.putIfAbsent()` update Redis cache state. Without a writer that calls the database, neither is persistence write-through.

## Cache-aside baseline

```kotlin
suspend fun findProduct(id: String): Product {
    products.get(id)?.let { return it }
    return repository.findById(id).also { loaded ->
        products.put(id, loaded)
    }
}
```

The first request reads the database and fills the cache. Updates still need a documented commit-versus-invalidation order and a stale-window policy.

## Real loader and writer examples

`examples/redisson-demo` uses `MapLoader`, `MapWriter`, and async variants for read-through, write-through, and write-behind. These examples call an actual repository and therefore differ from cache-aside.

Write-behind can reduce response latency but introduces crash, queue overflow, ordering, and retry risks. Verify durability before making it the only path to the source of truth.

## Continue through the ecosystem

- [cache-core](../bluetape4k-cache-core.md): local providers, JCache, memoizers, and common near-cache contracts.
- [redisson](../bluetape4k-redisson.md): codecs, distributed maps, locks, topics, and coroutine bridges.
- [Hibernate](../bluetape4k-hibernate.md): entity lifecycle and second-level-cache boundaries.
- [Spring Boot Hibernate Lettuce](../bluetape4k-spring-boot-hibernate-lettuce.md): Spring/Hibernate cache wiring.
- [redisson-demo](../bluetape4k-examples-redisson-demo.md): runnable loader/writer strategies.

The [exposed-workshop](https://github.com/bluetape4k/exposed-workshop) cache chapter connects `JdbcCacheRepository`, `EntityMapLoader`, and `EntityMapWriter`. [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop) continues with service-level examples. Judge strategy names by the actual loader/writer path when examples differ.

## Source and tests

- [`CacheReadThroughExample.kt`](../../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheReadThroughExample.kt)
- [`CacheWriteThroughExample.kt`](../../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheWriteThroughExample.kt)
- [`CacheWriteBehindExample.kt`](../../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheWriteBehindExample.kt)
- [`MapReadWriteThroughTest.kt`](../../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/readwritethrough/MapReadWriteThroughTest.kt)
