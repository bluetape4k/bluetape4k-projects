---
title: Configuration, failures, tests, and ecosystem
description: Separate L1 and IMap configuration, inspect post-failure state and lifecycle, and select the appropriate cache backend.
manualId: bluetape4k-cache-hazelcast
chapterId: operations-testing-ecosystem
---

# Configuration, failures, tests, and ecosystem

## Separate L1 from cluster configuration

`HazelcastNearCacheConfig` configures only Caffeine L1: name, maximum size, expire-after-write, optional expire-after-access, and statistics. Names must not be blank, and sizes and durations must be positive.

```kotlin
val config = hazelcastNearCacheConfig {
    cacheName = "catalog-v1"
    maxLocalSize = 20_000
    frontExpireAfterWrite = Duration.ofMinutes(5)
    frontExpireAfterAccess = Duration.ofMinutes(1)
    recordStats = true
}
```

Configure IMap backups, cluster TTL and max-idle, eviction, in-memory format, split-brain protection, and serializers in Hazelcast. L1 expiry is not IMap expiry; a long-lived cluster value can populate L1 again on the next miss.

## Inspect which tier survives a failure

- Front-first `put` and `remove` can leave only L1 changed after backend failure.
- `replace` changes L1 after backend success.
- Listener delay or removal can leave a stale L1 after a remote change.
- Multiple JVMs can evaluate one memoizer miss independently.
- `clearAll` affects the whole shared IMap for that cache name.

A resilience decorator does not turn these operations into a database transaction. Define idempotency, tolerated staleness, and source-store load before retry counts.

## Make shutdown order explicit

Stop new requests, close near caches to remove listeners, close cache managers and proxies, and finally shut down the application-owned Hazelcast client or member. Module `close` operations do not shut down the instance.

## Verification path

The module suite uses a Testcontainers Hazelcast server and client. Run this heavyweight suite sequentially with other database or broker containers.

```bash
./gradlew :bluetape4k-cache-hazelcast:test --no-build-cache --no-configuration-cache
```

Before adoption, add tests with application value types for two-client invalidation, front-first writes during disconnect, serializer schema migration, evaluator failure and duplication, and listener removal during shutdown.

## Backend selection

- Choose this module when the application already operates Hazelcast `IMap` and cluster events.
- Choose `cache-lettuce` for Redis operations with explicit codecs and RESP3 tracking.
- Choose `cache-redisson` for Redisson distributed objects and local cached maps.
- Start with `cache-core` Caffeine helpers for a single JVM.

Cache-aside means the application loads the source on a miss and then populates the cache. L1-to-IMap write-through synchronizes cache tiers; it does not persist to a database.

## Exclude stale README claims in 1.12.1

The release source does not contain the standalone `ResilientHazelcastNearCache`, write-behind queue, or tombstone implementation described by the README. Factory tests show that `withResilience` returns the common `cache-core` decorator. Keep operations and documentation within that contract.

## Sources and tests

- [`HazelcastNearCacheConfig.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCacheConfig.kt)
- [`HazelcastCachesTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/HazelcastCachesTest.kt)
- [`HazelcastNearCacheConfigTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCacheConfigTest.kt)
- [`HazelcastServers.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/HazelcastServers.kt)
- [`ResilientHazelcastNearCacheOpsTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientHazelcastNearCacheOpsTest.kt)
- [`build.gradle.kts`](../../../../../cache/cache-hazelcast/build.gradle.kts)
