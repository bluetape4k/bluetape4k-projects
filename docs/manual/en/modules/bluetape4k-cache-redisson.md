---
manualId: bluetape4k-cache-redisson
title: "Redisson JCache and Near Cache"
description: "Use Redisson JCache, Redis-backed memoizers, and RLocalCachedMap near caches with explicit concurrency, invalidation, and lifecycle contracts."
kind: library
group: caching
learningOrder: 530
---

# Redisson JCache and Near Cache

## What this module provides {#problem}

`bluetape4k-cache-redisson` connects Redisson to the common APIs in `bluetape4k-cache-core`. It provides JCache and coroutine `SuspendJCache`, sync/async/suspend memoizers, and `RLocalCachedMap` near caches.

The module does not merely create a Redis connection. It lets an application-owned Redisson client use standard cache APIs, merge same-key work within one JVM, invalidate local entries through Redisson Pub/Sub, and preserve coroutine cancellation.

## Decide before using it {#when-to-use}

- The application already uses Redisson or wants its distributed objects and codecs.
- Completed values must be shared through Redis, while in-flight work only needs coordination inside one JVM.
- You want `RLocalCachedMap` invalidation instead of maintaining a separate local cache and listener.
- You have chosen cache-aside or an actual loader/writer-based read/write-through contract.
- The application owns Redisson client shutdown and treats `close()` separately from entry deletion.

Use [cache-core](./bluetape4k-cache-core.md) for a single-JVM cache. Read the [redisson](./bluetape4k-redisson.md) manual when you also need maps, locks, topics, or broader Redisson support.

## Add the dependency {#coordinates}

Consumers manage only the central BOM version, not individual Redisson or library versions.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cache-redisson")
}
```

The Gradle path is `:bluetape4k-cache-redisson`, and the source directory is `cache/cache-redisson`. The 1.12.1 directory move did not change the Maven artifact or `io.bluetape4k.cache` package.

## First Redisson near cache {#quick-start}

Create and close the Redisson client at the application boundary. Close the near-cache wrapper in its own scope.

```kotlin
val products = RedissonCaches.suspendNearCache<Product>(
    redisson = redissonClient,
    config = redissonNearCacheConfig {
        cacheName = "products"
        maxLocalSize = 5_000
        timeToLive = Duration.ofMinutes(30)
    },
)

products.put("p:1", Product("p:1", "keyboard"))
val product = products.get("p:1")
products.close()
```

Keys are `String`, and the default codec is `RedissonCodecs.LZ4Fory`. `close()` destroys the wrapper; it is not an alias for `clearAll()`.

## API selection map {#api-by-task}

| Task | Start with | Boundary |
| --- | --- | --- |
| Standard JCache | `RedissonJCaching`, `RedissonCaches.jcache` | A lazy singleton manager reuses a cache with the same name. |
| Coroutine JCache | `RedissonSuspendJCache`, `suspendJCache` | Most calls await async APIs, but `getAndPut` is a get-then-put sequence. |
| Sync result reuse | `RMap.memoizer` | The evaluator runs on the caller thread; in-flight sharing is JVM-local. |
| Future result reuse | `RMap.asyncMemoizer` | A cache-write failure can be logged while the result future still succeeds. |
| Suspend result reuse | `RMap.suspendMemoizer` | Failed or cancelled evaluation is not stored and can be retried. |
| Sync near cache | `RedissonCaches.nearCache` | Calls blocking `RLocalCachedMap` operations. |
| Suspend near cache | `RedissonCaches.suspendNearCache` | Awaits Redisson async operations; `clearLocal()` is local. |
| Legacy front/back cache | `nearJCache`, `suspendNearJCache` | Prefer the native `RLocalCachedMap` path for new code. |

## Learning path {#concepts}

These chapters go beyond the feature list. They follow the 1.12.1 source and runnable tests to explain defaults, races, state after failure, and operating decisions.

1. [Redisson JCache and suspend wrappers](./bluetape4k-cache-redisson/redisson-jcache-suspend.md) — provider creation, cache reuse, async bridges, and non-atomic `getAndPut`.
2. [Memoizers and same-key work sharing](./bluetape4k-cache-redisson/memoizers-concurrency.md) — JVM-local single-flight behavior across sync, future, and suspend APIs.
3. [RLocalCachedMap near-cache invalidation](./bluetape4k-cache-redisson/near-cache-invalidation.md) — local/Redis tiers, Pub/Sub invalidation, clear scope, and metric limits.
4. [Configuration, codecs, and client ownership](./bluetape4k-cache-redisson/config-codec-ownership.md) — TTL, idle, eviction, reconnect, wire format, and lifecycle owners.
5. [Failures, cancellation, lifecycle, and operations](./bluetape4k-cache-redisson/failures-cancellation-operations.md) — Redis failures, partial success, cancellation, close, and telemetry.
6. [Cache strategies and ecosystem paths](./bluetape4k-cache-redisson/ecosystem-paths.md) — cache-aside versus read/write-through/behind, then Hibernate, Spring, Exposed, and workshops.

For a first adoption, read 1, 3, and 4 before adding memoization. For an existing deployment, start with the failure table in chapter 5 and the persistence boundaries in chapter 6.

## Recommended patterns {#patterns}

Give each cache a name that identifies its service and data contract. Set TTL and local capacity explicitly, then verify that the default `INVALIDATE` and reconnect `CLEAR` policies match the stale-read budget.

Limit memoizer evaluators to repeatable reads or calculations. Completed values are shared through Redis, but the in-flight map is process-local, so separate JVMs can evaluate the same first miss.

Do not call a cache `put` database write-through. Persistence write-through requires a loader or writer that actually invokes the source of truth.

## Integrations {#integrations}

This module implements `bluetape4k-cache-core` contracts and uses client/codec support from `bluetape4k-redisson`. It also connects directly to resilience4j for near-cache decorator behavior.

[cache-core](./bluetape4k-cache-core.md) explains provider-independent contracts; [redisson](./bluetape4k-redisson.md) covers broader Redisson APIs. Persistence caching continues through [Hibernate](./bluetape4k-hibernate.md), [Spring Boot Hibernate Lettuce](./bluetape4k-spring-boot-hibernate-lettuce.md), and the Exposed workshop.

## Configuration {#configuration}

`RedissonNearCacheConfig` defaults to name `redisson-near-cache`, 10,000 local entries, no TTL or idle expiry, `INVALIDATE`, reconnect `CLEAR`, and LRU. Names cannot be blank, and capacity, TTL, and idle durations must be positive.

The default wire codec is `LZ4Fory`. A codec change is a data-format migration: every producer and consumer of the map must agree on the key and value format.

JCache provider discovery uses `META-INF/services/javax.cache.spi.CachingProvider`. Since the manager can reuse an existing named cache, tests and tenants need distinct names.

## Failure behavior {#failures}

`RedissonSuspendJCache.getAndPut` is not atomic. Use an atomic Redisson `RMap` operation when strict read-modify-write semantics are required. A suspend memoizer rethrows `CancellationException`, removes the in-flight entry, and does not store a successful value.

Sync near-cache close suppresses destroy failures. Suspend close logs ordinary failures but preserves cancellation. Neither close method is a data-cleanup API.

When Redis is unstable, misses can move load to the source database. Excessive retry can turn the cache into a failure amplifier, so set timeout, fallback, and source-pool budgets together.

## Operations {#operations}

Observe Redis latency/errors/reconnects with local size and wrapper hit/miss counters. In 1.12.1, native near-cache statistics cannot split local and backend hits: local hit/miss fields are zero, and back hit/miss fields count the integrated get result. Do not interpret them as Redis round trips.

Track decode failures, name collisions, capacity/eviction, Pub/Sub reconnect clears, and Redisson client shutdown. The application remains the client owner.

## Testing {#testing}

The module uses a Redis Testcontainer, so do not run it concurrently with other heavy backend suites.

```bash
./gradlew :bluetape4k-cache-redisson:test --no-build-cache --no-configuration-cache
```

Factory and JCache tests cover CRUD and close. Memoizer tests cover thread, virtual-thread, and coroutine contention plus failure/cancellation recovery. Near-cache tests reuse the common cache-core conformance fixtures and add resilience cases.

## Workshops {#workshops}

Start with the module tests. For persistence read-through, write-through, and write-behind, follow the [redisson-demo manual](./bluetape4k-examples-redisson-demo.md) and its cache-strategy tests.

The [exposed-workshop](https://github.com/bluetape4k/exposed-workshop) cache chapter connects `JdbcCacheRepository`, `EntityMapLoader`, and `EntityMapWriter`. Broader Kotlin/Spring examples continue in [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop).

## 1.12.1 scope {#limitations}

This manual targets the `bluetape4k-projects` 1.12.1 release source. The module has no RESP3 hybrid near-cache API. Native Redisson near caches use `RLocalCachedMap`; legacy JCache near caches use a separate front/back model.

Memoizer in-flight sharing is not a distributed lock. Native near-cache statistics do not provide a true local/backend split. The suspend JCache `getAndPut` wrapper is not atomic.

## Source and tests {#sources}

- [`RedissonCaches.kt`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/RedissonCaches.kt)
- [`RedissonSuspendJCache.kt`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/jcache/RedissonSuspendJCache.kt)
- [`RedissonMemoizer.kt`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonMemoizer.kt)
- [`RedissonSuspendMemoizer.kt`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/memoizer/RedissonSuspendMemoizer.kt)
- [`RedissonNearCache.kt`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCache.kt)
- [`RedissonNearCacheConfig.kt`](../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheConfig.kt)
- [`RedissonSuspendMemoizerTest.kt`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/memoizer/RedissonSuspendMemoizerTest.kt)
- [`RedissonNearCacheTest.kt`](../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/nearcache/RedissonNearCacheTest.kt)
