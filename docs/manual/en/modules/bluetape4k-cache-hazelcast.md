---
manualId: bluetape4k-cache-hazelcast
title: "Hazelcast Cache Integration"
description: "Use Hazelcast JCache, IMap-backed memoizers, and near caches with explicit cluster ownership, invalidation, serialization, and failure contracts."
kind: library
group: caching
learningOrder: 510
---

# Hazelcast Cache Integration

## Capabilities {#problem}

`bluetape4k-cache-hazelcast` adapts an existing `HazelcastInstance` to JCache, function-result memoizers, and near caches that combine a Caffeine L1 with a Hazelcast `IMap` L2. It provides synchronous, `CompletableFuture`, and coroutine APIs and invalidates per-JVM L1 entries from `IMap` entry events.

The module does not create or own the Hazelcast cluster. Connection, authentication, member discovery, backups, map TTL, and serialization remain application-level Hazelcast configuration. This module adds cache contracts to the supplied instance and maps.

## Decisions before adoption {#when-to-use}

- Choose between standard JCache and the direct `IMap` near-cache and memoizer APIs.
- Prefer the smaller Caffeine helpers in `bluetape4k-cache-core` for a single JVM.
- Use this backend when the application already shares a Hazelcast data-grid keyspace rather than a Redis cache.
- Native near-cache keys are strings; memoizers and JCache accept generic keys.
- Verify that values are readable and writable under the cluster's Hazelcast serialization configuration.
- Decide whether a short L1 stale window is acceptable and how listener registration, removal, and cluster failures will be monitored.
- The application owns startup and shutdown of the `HazelcastInstance`.

## Coordinates {#coordinates}

Applications manage only the central BOM version, not separate Hazelcast or bluetape4k module versions.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cache-hazelcast")
}
```

The Gradle project is `:bluetape4k-cache-hazelcast`, backed by `cache/cache-hazelcast`. The application environment supplies the Hazelcast cluster and client or member configuration.

## First near cache {#quick-start}

```kotlin
val hazelcast: HazelcastInstance = applicationHazelcastClient()

val users = HazelcastCaches.nearCache<User>(hazelcast) {
    cacheName = "users-v1"
    maxLocalSize = 10_000
    frontExpireAfterWrite = Duration.ofMinutes(10)
    recordStats = true
}

try {
    users.put("42", user)
    check(users.get("42") == user)
} finally {
    users.close()       // removes the listener and closes Caffeine L1
    // The application owner shuts down hazelcast.
}
```

`put` changes L1 before calling `IMap.set`. A backend failure can therefore leave the new value in L1. Do not treat this order as a database write-through or an atomic dual write.

## API by task {#api-by-task}

| Task | Start with | Boundary |
| --- | --- | --- |
| Hazelcast JCache | `HazelcastJCaching`, `HazelcastCaches.jcache` | Resolves a manager for the supplied instance without SPI auto-discovery. |
| Coroutine JCache | `HazelcastSuspendJCache` | Prefers `ICache` async operations and sends other calls to `Dispatchers.IO`. |
| Shared function results | three Hazelcast memoizers | Same-key coalescing is local to one JVM `inFlight` map. |
| Synchronous near cache | `HazelcastNearCache` | Combines Caffeine L1 with a String-key `IMap` L2. |
| Coroutine near cache | `HazelcastSuspendNearCache` | Uses `await` for async calls and `Dispatchers.IO` for selected bulk and conditional calls. |
| JCache-based L1/L2 | `nearJCache`, `suspendNearJCache` factories | Factories omit listeners to avoid listener serialization failure. |
| Retry and fallback | `withResilience` | Adds the common `cache-core` decorator, not a Hazelcast-specific write-behind implementation. |

## Learning path {#concepts}

These chapters follow the 1.12.1 release source and executable tests. They cover instance ownership, async boundaries, concurrent evaluation, L1/L2 ordering, and listener serialization, including the state left behind after failures.

1. [JCache and HazelcastInstance ownership](./bluetape4k-cache-hazelcast/jcache-instance-ownership.md) — provider choice, manager creation, cache names, and instance lifecycle.
2. [Suspend JCache and async boundaries](./bluetape4k-cache-hazelcast/suspend-jcache-async-boundaries.md) — `ICache` unwrap, `await`, IO fallback, and non-atomic `getAndPut`.
3. [IMap memoizers and concurrency](./bluetape4k-cache-hazelcast/imap-memoizers-concurrency.md) — sync, future, and suspend execution, JVM-local coalescing, and cluster races.
4. [IMap near-cache invalidation](./bluetape4k-cache-hazelcast/imap-near-cache-invalidation.md) — Caffeine L1, `IMap` L2, entry listeners, write order, and statistics.
5. [JCache near-cache serialization limits](./bluetape4k-cache-hazelcast/jcache-near-cache-serialization.md) — listener-factory serialization failures and the consistency limits of listener-free factories.
6. [Configuration, failures, tests, and ecosystem](./bluetape4k-cache-hazelcast/operations-testing-ecosystem.md) — validation, shutdown, operational checks, and backend selection.

For JCache, read 1, 2, then 5. For the native near cache, read 4 and 6. For shared function results, read 3 and 6.

## Patterns {#patterns}

Version the cache name and value serialization schema as one deployment contract. When the value format changes, verify serializer compatibility and migrate to a new map or cache name when needed. Populate the cache only after a source-of-truth read succeeds, and update or invalidate keys after the database transaction commits.

Remote change events are asynchronous, so a near cache can briefly return an old value. Keep values that cannot tolerate that window out of L1. When adding fallback, bound concurrent evaluators and source-store access so a Hazelcast incident does not amplify into a database incident.

## Integrations {#integrations}

[`bluetape4k-cache-core`](./bluetape4k-cache-core.md) defines the shared JCache, near-cache, and resilience contracts. If Redis is the operational standard, compare RESP3 invalidation in [`bluetape4k-cache-lettuce`](./bluetape4k-cache-lettuce.md) with Redisson distributed objects in [`bluetape4k-cache-redisson`](./bluetape4k-cache-redisson.md).

Hazelcast is a natural fit for applications that already use `IMap` and entry events. Design a separate repository loader and writer before calling any cache operation database write-through. Continue with [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed) and [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop) for database cache strategies.

## Configuration {#configuration}

`HazelcastNearCacheConfig` defaults to map name `hazelcast-near-cache`, 10,000 L1 entries, 30-minute expire-after-write, no expire-after-access, and disabled statistics. Names cannot be blank, and capacities and durations must be positive.

These options configure only Caffeine L1. Configure `IMap` backups, TTL, max-idle, in-memory format, and serializers in Hazelcast `Config` or `ClientConfig`. The module adds no separate value codec or wire format.

## Failures {#failures}

Native near-cache `put`, `putAll`, and `remove` calls change L1 first. If the following `IMap` call fails, local and cluster state can diverge. `replace` and a successful `putIfAbsent` update L1 after the backend result. `getAndRemove` and `getAndReplace` compose multiple calls and are not single atomic operations.

Direct listener-backed JCache near-cache construction serializes a listener factory that captures the Caffeine front cache and fails with `HazelcastSerializationException`. The `HazelcastCaches` factories omit the listener, so reads and writes work but peer L1 invalidation is not guaranteed.

## Operations {#operations}

Observe L1 hits, misses, evictions, and size together with `IMap` hits and misses, entry-event delay, client reconnects, and operation latency. Caffeine counters remain zero when `recordStats=false`. For memoizers, monitor evaluator latency, failure, hot keys, and duplicate same-key computation across cluster members.

`close` removes the near-cache listener and closes L1; it does not delete `IMap` data or shut down the `HazelcastInstance`. `clearAll` clears the shared map, so do not reuse one cache name across unrelated features.

## Testing {#testing}

The module suite uses Hazelcast Testcontainers. Run it sequentially with other heavyweight database suites.

```bash
./gradlew :bluetape4k-cache-hazelcast:test --no-build-cache --no-configuration-cache
```

Near-cache tests cover CRUD, bulk operations, statistics, and idempotent close. Memoizer tests cover same-key races and re-evaluation after failure. JCache near-cache tests lock in both the listener-backed serialization failure and the working listener-free factory behavior.

## Workshops {#workshops}

Use `HazelcastCachesTest` for minimal factory construction and the synchronous and suspend near-cache tests for L1 misses, bulk operations, and clear boundaries. `HazelcastNearJCacheTest` also serves as a regression exercise for an unsupported listener combination rather than only a success example.

Continue to [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop) and [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop) for database cache-aside and loader/writer exercises.

## 1.12.1 scope {#limitations}

This manual targets release commit `7cf0b73646af05c0f8872cc4f6a16983949c4e3e`. `META-INF/services/javax.cache.spi.CachingProvider` contains no provider class; `HazelcastJCaching` explicitly creates `HazelcastCachingProvider`.

The standalone `ResilientHazelcastNearCache` and write-behind queue described by the README do not exist in this release source. Use only the `cache-core` `withResilience` decorator. Factory-created JCache near caches run in a listener-free degraded mode and do not provide peer front-cache propagation.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### HazelcastNearCache Class Hierarchy diagram

[![HazelcastNearCache Class Hierarchy diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-hazelcast-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-hazelcast-diagram-01.svg)

_Release README: [`cache/cache-hazelcast/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-hazelcast/README.md)_

### HazelcastNearCache Runtime Flow diagram

[![HazelcastNearCache Runtime Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-hazelcast-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-hazelcast-diagram-02.svg)

_Release README: [`cache/cache-hazelcast/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-hazelcast/README.md)_

### 2-Tier NearCache Flow diagram

[![2-Tier NearCache Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-hazelcast-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-cache-hazelcast-sequence-01.svg)

_Release README: [`cache/cache-hazelcast/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/cache-hazelcast/README.md)_

<!-- release-readme-diagrams:end -->

## Sources and tests {#sources}

- [`HazelcastCaches.kt`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt)
- [`HazelcastJCaching.kt`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/jcache/HazelcastJCaching.kt)
- [`HazelcastSuspendJCache.kt`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/jcache/HazelcastSuspendJCache.kt)
- [`HazelcastMemoizer.kt`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/memoizer/HazelcastMemoizer.kt)
- [`HazelcastNearCache.kt`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCache.kt)
- [`HazelcastEntryEventListener.kt`](../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastEntryEventListener.kt)
- [`HazelcastNearJCacheTest.kt`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCacheTest.kt)
- [`HazelcastSuspendNearCacheTest.kt`](../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/HazelcastSuspendNearCacheTest.kt)
