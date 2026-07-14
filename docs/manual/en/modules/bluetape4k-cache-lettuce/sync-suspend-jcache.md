---
title: Synchronous and suspend JCache
description: Use Lettuce JCache CRUD, listeners, EntryProcessor, coroutine adapters, and the close versus destroy lifecycle.
manualId: bluetape4k-cache-lettuce
chapterId: sync-suspend-jcache
---

# Synchronous and suspend JCache

## Start with the factory

`LettuceCaches` exposes the common creation paths. The synchronous path returns `JCache<K, V>`, while the coroutine path returns `LettuceSuspendJCache<V>` with String keys.

```kotlin
val sessions = LettuceCaches.jcache<String, Session>(
    redisClient, "sessions-v1", ttlSeconds = 1_800,
)
val suspended = LettuceCaches.suspendJCache<Session>(
    redisClient, "sessions-suspend-v1", ttlSeconds = 1_800,
)
```

Both wrap the Redis-hash `LettuceJCache`. The suspend type is an adapter that runs blocking JCache calls on `Dispatchers.IO`, not a separate fully non-blocking implementation.

## CRUD and batches

`getAll` reads keys in chunks of 100. `putAll` performs one existing-field read only when listeners are present, then distinguishes CREATED from UPDATED events.

```kotlin
sessions.putAll(mapOf("a" to sessionA, "b" to sessionB))
val found = sessions.getAll(setOf("a", "b", "missing"))
check(found.keys == setOf("a", "b"))
```

`replace(key, old, new)` and `remove(key, old)` read and compare before issuing another Redis command. They are not distributed compare-and-set operations. Use the near-cache Lua CAS or a dedicated Redis script when the comparison must be atomic.

## Listeners and EntryProcessor

Entry listeners observe CREATED, UPDATED, and REMOVED operations executed by this cache instance. Writes from another Redis client do not automatically become JCache listener events.

The 1.11.0 implementation supports `invoke` and `invokeAll` with a `MutableEntry`. A requested change is committed through the normal JCache write path after processor execution.

```kotlin
val count = sessions.invoke("a", EntryProcessor<String, Session, Int> { entry, _ ->
    val next = entry.value!!.copy(refreshCount = entry.value!!.refreshCount + 1)
    entry.setValue(next)
    next.refreshCount
})
```

This is not wrapped in one Redis transaction or Lua script, so concurrent external changes can race with it.

## Coroutine execution boundary

The suspend wrapper dispatches CRUD and close calls to `Dispatchers.IO`. `entries()` and `getAll(keys)` return `Flow`, but their source is still synchronous JCache work.

`putAllFlow` collects and writes each pair separately. Collect into a map and call `putAll` when a Redis batch is required.

## Manager lifecycle

`LettuceSuspendCacheManager` reuses wrappers by name and applies default TTL and codec values. `closeCache` removes a wrapper but preserves Redis data, allowing another wrapper to read it. Use `destroyCache` or `clear` when data must be deleted.

Its suspend `close()` tries to close all registered caches. If one close throws `CancellationException`, it finishes the remaining cleanup and then rethrows cancellation.

## Sources and tests

- [`LettuceJCache.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCache.kt)
- [`LettuceSuspendJCache.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendJCache.kt)
- [`LettuceSuspendCacheManager.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendCacheManager.kt)
- [`LettuceJCacheTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceJCacheTest.kt)
- [`LettuceSuspendJCacheManagerTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendJCacheManagerTest.kt)
