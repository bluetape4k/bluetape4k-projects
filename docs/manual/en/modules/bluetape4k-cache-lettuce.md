---
manualId: bluetape4k-cache-lettuce
title: "Lettuce JCache and Near Cache"
description: "Configure Redis JCache, distributed memoizers, and Caffeine L1 plus Redis L2 near caches with explicit invalidation, TTL, and lifecycle contracts."
kind: library
group: caching
learningOrder: 520
---

# Lettuce JCache and Near Cache

## Capabilities {#problem}

`bluetape4k-cache-lettuce` implements the `bluetape4k-cache-core` contracts with Lettuce and Redis. It provides a Redis-hash JCache provider, synchronous and suspend JCache APIs, Redis-backed memoizers, and near caches that combine Caffeine L1 with Redis L2.

The near cache isolates Redis keys as `cacheName:key`, fills L1 after an L2 hit, and uses RESP3 CLIENT TRACKING pushes to invalidate keys changed by another connection. Reads and writes can continue when tracking is unavailable, but cross-process L1 consistency then needs another strategy.

## Decisions before adoption {#when-to-use}

- Choose between the standard JCache API and the statistics and L1/L2 controls of `NearCacheOperations`.
- Prefer the smaller Caffeine helpers in `bluetape4k-cache-core` for a single-JVM cache.
- JCache TTL applies to the Redis hash for the whole cache, not to individual entries.
- Native near-cache values use separate Redis keys, and `redisTtl` applies per key.
- Configure the client for RESP3 and monitor tracking startup when using CLIENT TRACKING.
- Treat the codec as a persisted wire format; migrate with a new cache name or a deliberate clear.
- Decide who closes the `RedisClient`, manager, and per-cache connection.

## Coordinates {#coordinates}

Applications manage only the central BOM version, not separate Lettuce, Caffeine, or bluetape4k module versions.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cache-lettuce")
}
```

The application environment supplies Redis. The Gradle project is `:bluetape4k-cache-lettuce`, backed by `cache/cache-lettuce`.

## First near cache {#quick-start}

Start with an RESP3 client and explicit names, capacity, and expiry values.

```kotlin
val redisClient = RedisClient.create("redis://localhost:6379").also {
    it.options = ClientOptions.builder()
        .protocolVersion(ProtocolVersion.RESP3)
        .build()
}

val users = LettuceCaches.nearCache<User>(redisClient) {
    cacheName = "users"
    maxLocalSize = 10_000
    frontExpireAfterWrite = Duration.ofMinutes(10)
    redisTtl = Duration.ofHours(1)
    useRespProtocol3 = true
    recordStats = true
}

try {
    users.put("42", user)
    check(users.get("42") == user)
} finally {
    users.close()
    redisClient.shutdown()
}
```

`put` writes Redis first and updates L1 only after success. This is write-through between cache tiers, not a repository write-through to a database.

## API by task {#api-by-task}

| Task | Start with | Boundary |
| --- | --- | --- |
| JCache SPI | `LettuceCachingProvider`, `LettuceCacheManager` | A provider-created manager also owns its RedisClient. |
| Reuse an external client | `LettuceJCaching` | The manager does not close the external client. |
| Synchronous JCache | `LettuceJCache`, `LettuceCaches.jcache` | Values share one Redis hash and its whole-cache TTL. |
| Coroutine JCache | `LettuceSuspendJCache`, `LettuceSuspendCacheManager` | Blocking JCache calls run on `Dispatchers.IO`. |
| Shared function results | three Lettuce memoizers | Same-key coalescing is local to one JVM `inFlight` map. |
| Synchronous near cache | `LettuceNearCache` | Keys are strings; Redis succeeds before L1 changes. |
| Coroutine near cache | `LettuceSuspendNearCache` | Uses Lettuce coroutine commands and async batches. |
| JCache-based L1/L2 | `nearJCache`, `suspendNearJCache` | The composition contract comes from `cache-core`. |
| Retry and fallback | `withResilience` | This adds the common decorator, not another Lettuce implementation. |

## Learning path {#concepts}

These chapters follow the 1.12.1 release source and executable tests. They explain provider identity, Redis storage, concurrent computation, L1/L2 ordering, and invalidation failures with defaults, ownership, and post-failure state.

1. [JCache provider, manager, and configuration](./bluetape4k-cache-lettuce/jcache-provider-manager.md) — SPI lookup, `(ClassLoader, URI)` identity, Redis hashes, TTL, codecs, and ownership.
2. [Synchronous and suspend JCache](./bluetape4k-cache-lettuce/sync-suspend-jcache.md) — CRUD, listeners, EntryProcessor, IO dispatching, close, and destroy.
3. [Redis memoizers and concurrency](./bluetape4k-cache-lettuce/memoizers-concurrency.md) — sync, future, and suspend paths, JVM-local coalescing, failure, and cancellation recovery.
4. [Near-cache L1 and L2](./bluetape4k-cache-lettuce/near-cache-l1-l2.md) — read fill, write order, key isolation, TTL, statistics, and bulk operations.
5. [RESP3 invalidation and Lua CAS](./bluetape4k-cache-lettuce/resp3-invalidation-lua.md) — tracking registration, push payloads, `EVALSHA`, and `NOSCRIPT` fallback.
6. [Lifecycle, tests, and ecosystem](./bluetape4k-cache-lettuce/operations-ecosystem.md) — failures and operations, then Hibernate, Spring, Exposed, and workshop paths.

For JCache, start with chapters 1 and 2. For direct near-cache use, chapters 4, 5, and 6 are the shorter path.

## Patterns {#patterns}

Version the cache name, key encoding, value codec, and TTL as one data contract. Use a new name when the wire format changes. Populate the cache only after a source-of-truth read succeeds, and update or invalidate related keys after the data transaction completes.

RESP3 invalidation arrives asynchronously. Keep values that cannot tolerate even a short stale window out of L1. When adding fallback, limit retries, concurrent loads, and database pressure so a Redis incident does not amplify into a source-store incident.

## Integrations {#integrations}

[`bluetape4k-cache-core`](./bluetape4k-cache-core.md) defines the common JCache, near-cache, and resilience contracts. [`bluetape4k-lettuce`](./bluetape4k-lettuce.md) covers clients, codecs, maps, and Redis commands. Continue to [`bluetape4k-hibernate-cache-lettuce`](./bluetape4k-hibernate-cache-lettuce.md) for Hibernate L2 cache and [`bluetape4k-spring-boot-hibernate-lettuce`](./bluetape4k-spring-boot-hibernate-lettuce.md) for Boot configuration and observability.

For database repository strategies, use [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed), [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop), and [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop). Verify a real loader/writer boundary before calling a cache operation database write-through.

## Configuration {#configuration}

`LettuceNearCacheConfig` defaults to name `lettuce-near-cache`, 10,000 L1 entries, 30-minute expire-after-write, no Redis TTL, RESP3 tracking enabled, and statistics disabled. Names cannot be blank, and sizes and durations must be positive.

`LettuceCacheConfig.ttlSeconds` applies to the whole Redis hash. The default key encoder is `toString()`, and the default value codec is LZ4 plus Fory. Supply a `keyDecoder` to iterate a cache with non-String keys.

## Failures {#failures}

The JCache manager rejects duplicate names, operations after close, and typed lookups that do not match configured key and value types. Codec and Redis command failures propagate.

Near-cache writes update L1 only after Redis succeeds. A CLIENT TRACKING startup failure only logs a warning and leaves the cache running, so remote changes may leave stale L1 entries. Monitor tracking as a separate availability contract.

## Operations {#operations}

Observe L1 hits, misses, evictions, size, Redis hits and misses, command latency, errors, and reconnects. Caffeine counters remain zero when `recordStats=false`. `backCacheSize` and `clearAll` scan `cacheName:*` and use `UNLINK`; measure their cost for large keyspaces.

Track memoizer evaluator latency and hot keys, tracking startup and invalidation delay, and database pressure after fallback. Test the load spike produced when Redis is temporarily unavailable.

## Testing {#testing}

The module suite uses Redis Testcontainers, so run it sequentially with other heavy database suites.

```bash
./gradlew :bluetape4k-cache-lettuce:test --no-build-cache --no-configuration-cache
```

`LettuceJCacheTest` covers TTL, typed keys, and EntryProcessor. Memoizer tests cover races, failures, and cancellation. Near-cache tracking and isolation tests cover external writers, cross-instance invalidation, and scoped `clearAll`.

## Workshops {#workshops}

Use `LettuceJCachesTest` for the smallest factory examples, `LettuceNearCacheTrackingTest` for two-instance invalidation, and `LettuceNearCacheIsolationTest` for key namespaces. Continue to [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop) and [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop) for cache-aside and database loader/writer exercises.

## 1.12.1 scope {#limitations}

This manual targets release commit `7cf0b73646af05c0f8872cc4f6a16983949c4e3e`. JCache listeners receive operations performed by that cache instance; they are not a Redis-backed global JCache event bus.

Tracking startup is fail-open. With `NOLOOP`, the writing connection receives no push and the code updates its own L1 directly. `withResilience` uses a decorator from `cache-core`; there is no standalone `ResilientLettuceNearCache` implementation in this release.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### LettuceNearCache Class Hierarchy diagram

[![LettuceNearCache Class Hierarchy diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-01.svg)

_Release README: [`cache/cache-lettuce/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-lettuce/README.md)_

### Lettuce JCache NearCache Structure diagram

[![Lettuce JCache NearCache Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-02.svg)

_Release README: [`cache/cache-lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-lettuce/README.ko.md)_

### Lettuce Cache Stability Contracts diagram

[![Lettuce Cache Stability Contracts diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-03.svg)

_Release README: [`cache/cache-lettuce/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-lettuce/README.md)_

### Native Lettuce NearCache Structure diagram

[![Native Lettuce NearCache Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-diagram-04.svg)

_Release README: [`cache/cache-lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-lettuce/README.ko.md)_

### RESP3 CLIENT TRACKING Flow diagram

[![RESP3 CLIENT TRACKING Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-lettuce-sequence-01.svg)

_Release README: [`cache/cache-lettuce/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-lettuce/README.md)_

<!-- release-readme-diagrams:end -->

## Sources and tests {#sources}

- [`LettuceCaches.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/LettuceCaches.kt)
- [`LettuceCachingProvider.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCachingProvider.kt)
- [`LettuceJCache.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCache.kt)
- [`LettuceSuspendJCache.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendJCache.kt)
- [`LettuceSuspendMemoizer.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceSuspendMemoizer.kt)
- [`LettuceNearCache.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCache.kt)
- [`TrackingInvalidationListener.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/TrackingInvalidationListener.kt)
- [`LettuceJCacheTest.kt`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceJCacheTest.kt)
- [`LettuceNearCacheTrackingTest.kt`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheTrackingTest.kt)
