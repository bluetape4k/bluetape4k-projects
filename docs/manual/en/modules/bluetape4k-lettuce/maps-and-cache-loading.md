---
title: Maps and cache loading
description: Define read-through, write-through, write-behind, invalidation, and shutdown behavior.
manualId: bluetape4k-lettuce
chapterId: maps-and-cache-loading
---

# Maps and cache loading

## Simple and loaded maps

`LettuceMap` wraps Redis Hash commands with sync and async APIs; `LettuceSuspendMap` provides suspending calls. `LettuceLoadedMap` stores one Redis value per key with a TTL and invokes `MapLoader` on a miss.

```kotlin
LettuceLoadedMap(
    client = client,
    loader = object : MapLoader<Long, Account> {
        override fun load(key: Long): Account? = repository.find(key)
        override fun loadAllKeys(): Iterable<Long> = repository.findAllIds()
    },
    writer = accountWriter,
    config = LettuceCacheConfig.READ_WRITE_THROUGH,
).use { cache ->
    val account = cache[42L]
}
```

Write-through calls the writer before Redis, so writer failure leaves Redis unchanged. Write-behind enqueues a bounded item and updates Redis immediately; database failure appears later. A full queue fails immediately.

## Invalidation and shutdown

`delete` changes the writer and Redis; `evict` removes only Redis state. Pattern invalidation uses `SCAN` plus `UNLINK`. Closing a write-behind map drains up to its shutdown timeout and warns about remaining entries. The suspended implementation cancels its owned job, not the caller's whole scope.

## The 1.12.1 near-cache boundary

The `nearCache*` fields and presets are validated but not consumed by loaded maps. There is no Caffeine store or RESP3 tracking invalidation in this release. Implement a local layer separately or verify the concrete contract in `bluetape4k-cache-lettuce`.

## Source and tests

- [`LettuceLoadedMap.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/map/LettuceLoadedMap.kt)
- [`LettuceSuspendedLoadedMap.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/map/LettuceSuspendedLoadedMap.kt)
- [`LettuceLoadedMapTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/map/LettuceLoadedMapTest.kt)

Continue with [Filters, scripts, and primitives](./filters-scripts-and-primitives.md).
