---
manualId: bluetape4k-redisson
title: "Redisson Coroutine Extensions"
description: "Use Redisson clients, distributed objects and streams, coroutines, codecs, local caches, and persistence boundaries from the 1.12.1 source."
kind: library
group: caching
learningOrder: 560
---

# Redisson Coroutine Extensions

## Provided capabilities {#problem}

`bluetape4k-redisson` removes recurring Kotlin integration code around Redisson. It provides DSL and YAML client creation, batch and transaction helpers, coroutine adapters for `RFuture`, Stream consumer-group helpers, codec combinations, and builders for `RMapCache` and `RLocalCachedMap`.

The module does not own client lifecycle or choose a consistency model for the application. The application must shut down clients it creates and deploy compatible cache names, codecs, and local-cache policies across nodes. A plain `get`/`put` flow without a `MapLoader` or `MapWriter` is cache-aside, not read-through or write-through.

## Decisions before adoption {#when-to-use}

- Decide whether Spring Boot owns the client or the application creates and shuts it down.
- Pick one service boundary: synchronous calls, `RFuture`, or coroutines.
- Choose codecs based on whether Redis values stay inside one deployment or cross service and version boundaries.
- Define acceptable local-cache staleness and recovery after Pub/Sub disconnects.
- Separate cache-aside from loader/writer-backed read-through, write-through, and write-behind.
- Define Stream ACK, pending-message claim, retry, and duplicate-processing rules.

## Coordinates {#coordinates}

Consumers manage only the `bluetape4k-dependencies` BOM version. Coroutine and codec integrations have optional runtime dependencies; add only those used by the application.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-redisson")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core") // for coroutine APIs
}
```

## First client and map {#quick-start}

The component that creates the client also owns shutdown.

```kotlin
import io.bluetape4k.redis.redisson.redissonClient
import io.bluetape4k.redis.redisson.cache.mapCache
import java.time.Duration

val redisson = redissonClient {
    useSingleServer().address = "redis://127.0.0.1:6379"
}

val users = mapCache<String, String>("users", redisson) {
    timeToLive(Duration.ofMinutes(10))
}

try {
    users.put("42", "Debop")
} finally {
    redisson.shutdown()
}
```

This `put` writes only to Redis, so the example is cache-aside. Chapter 5 adds the loader/writer boundary required for database persistence.

## API by task {#api-by-task}

| Task | Start with | Boundary to keep |
| --- | --- | --- |
| Create clients | `redissonClient`, `redissonClientOf`, `configFromYamlOf` | Each call creates a client and does not shut it down. |
| Apply concurrency defaults | `applyHighConcurrencyDefaults` | CPU-derived defaults do not replace capacity tests. |
| Batch or transaction | `withBatch`, `withTransaction` | Rollback failure does not replace the original failure; use suspend variants in coroutines. |
| Bridge futures | `sequence`, `awaitAll`, `withSuspendedBatch`, `withSuspendedTransaction` | Propagate failures and cancellation to the caller scope. |
| Distributed objects and streams | Redisson `RMap`, `RLock`, `RStream`; `ackAllAsync`, `claimAllAsync` | The caller owns duplicate-processing and retry policy. |
| TTL map | `mapCache` | Entry expiration does not provide database persistence. |
| Local cached map | `localCachedMap`, `RedissonNearCache` | Choose sync/reconnection policies and call `destroy()` at the correct time. |
| Cache persistence | `RedissonCacheConfig.toMapOptions`, `toLocalCachedMapOptions` | Real read/write-through requires a loader and writer. |
| Codec | `RedissonCodecs`, `Jackson3Codec`, `Fastjson2Codec`, `GzipCodec` | Treat wire compatibility, allow-lists, and expansion limits as deployment contracts. |

## Learning path {#concepts}

The chapters provide detailed explanations, focused examples, and links to the exact 1.12.1 source and tests. They emphasize ownership, failure, and consistency rather than repeating an API inventory.

1. [Clients, distributed objects, and streams](./bluetape4k-redisson/client-distributed-objects-streams.md) — client ownership, batch and transactions, lock IDs, and consumer-group helpers.
2. [Future and coroutine boundaries](./bluetape4k-redisson/future-coroutine-boundaries.md) — failure and cancellation across `RFuture`, `CompletableFuture`, and suspend APIs.
3. [Codecs, security, and wire format](./bluetape4k-redisson/codecs-security-wire-format.md) — Fory, JSON, compression, allow-lists, and migration risks.
4. [Local cached maps and invalidation](./bluetape4k-redisson/local-cache-pubsub-invalidation.md) — JVM front cache, Redis back cache, Pub/Sub sync, and reconnect policy.
5. [Cache modes and persistence](./bluetape4k-redisson/cache-modes-persistence.md) — cache-aside versus loader/writer-backed read/write-through and write-behind.
6. [Lifecycle, testing, and ecosystem](./bluetape4k-redisson/lifecycle-testing-ecosystem.md) — shutdown, operations, Testcontainers, cache-redisson, Exposed, and workshops.

Read 1 through 4 for a first adoption. Read chapter 5 whenever cache and database writes must be coordinated, and chapter 6 when choosing the next ecosystem layer.

## Recommended patterns {#patterns}

Create one client per process or Spring application context and shut it down from the same lifecycle owner. Use batch to reduce round trips and transaction only when commands need atomicity. In coroutine code, use async commands and `await()` rather than mixing blocking `get()` calls.

Treat Near Cache as a consistency choice. Align cache name, codec, `SyncStrategy`, and `ReconnectionStrategy` across nodes. Name cache-aside, write-through, and write-behind explicitly instead of calling every cache plus database flow a write-through cache.

## Integrations {#integrations}

The Redisson client is an API dependency. Spring Boot starter, coroutines, cache-core, idgenerators, Fory, Jackson, Fastjson2, and compression libraries are optional. Missing runtime support can fail during client construction or the first encode.

Use [`bluetape4k-cache-redisson`](./bluetape4k-cache-redisson.md) for Spring Cache integration. Follow chapters 5 and 6 for Exposed repository caching and workshop examples.

## Configuration {#configuration}

`configFromYamlOf` reads Redisson YAML from an InputStream, String, File, or URL and applies the selected codec. `applyHighConcurrencyDefaults` derives thread, pool, timeout, and retry-delay values from CPU count; tune them from Redis topology and measured command latency.

`RedissonCacheConfig` rejects negative TTL, size, and retry values. `toMapOptions` and `toLocalCachedMapOptions` fail with `IllegalArgumentException` instead of silently ignoring unsupported `ttl`, `maxSize`, or `deleteFromDBOnInvalidate` values.

## Failure behavior {#failures}

Synchronous and suspend transactions attempt rollback after an action failure and rethrow the original failure. Rollback failure does not replace it, while cancellation raised during suspend rollback is preserved. Stream helpers reject blank group or consumer names and empty ID collections.

Unrestricted codec fallback widens the trust boundary. Package allow-lists on `Jackson3Codec` and `Fastjson2Codec` disable binary fallback decode by default. Enable migration fallback only for a bounded period and trusted Redis data.

## Operations {#operations}

Observe Redis connections, command latency and timeouts, retries, reconnects, batch size, transaction rollback, Stream pending entries, Near Cache hit ratio, and stale-data incidents. Include Pub/Sub disconnect and local-cache recovery policy in failure drills.

Write-behind adds delayed persistence and possible loss. Measure writer backlog, failures, retries, and drain time, and verify pending writes during shutdown.

## Testing {#testing}

The 1.12.1 tests start Redis through Testcontainers and cover clients, streams, caches, coroutines, and codecs.

```bash
./gradlew :bluetape4k-redisson:test --no-build-cache --no-configuration-cache
```

This is a Docker-backed heavy test and should not run in parallel with other Testcontainers suites. Documentation-only edits use source-link and document-structure checks; run the task when behavior changes.

## Workshops {#workshops}

`RedissonClientSupportTest`, `RStreamSupportTest`, `RedissonClientCoroutineTest`, `LocalCacheMapSupportTest`, and `RedissonNearCacheTest` are the smallest executable examples. Continue with the cache chapter in [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop) and Redis examples in [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop).

## 1.12.1 scope {#limitations}

This manual targets the `bluetape4k-projects` 1.12.1 release commit. `RedissonNearCache` delegates to `RLocalCachedMap`; it is not an abstraction that manually reconciles two independently written maps. `destroy()` closes only the local near-cache instance and leaves Redis data intact.

Preset names in `RedissonCacheConfig` do not create database read/write-through. The application must attach real `MapLoader` and `MapWriter` implementations. `deleteFromDBOnInvalidate` is not supported by option conversion in 1.12.1.

## Source and tests {#sources}

- [`RedissonClientSupport.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt)
- [`RedissonClientExtensions.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientExtensions.kt)
- [`RStreamSupport.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RStreamSupport.kt)
- [`RedissonClientCoroutine.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/coroutines/RedissonClientCoroutine.kt)
- [`RedissonCacheConfig.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/cache/RedissonCacheConfig.kt)
- [`RedissonNearCache.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/nearcache/RedissonNearCache.kt)
- [`Jackson3Codec.kt`](../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Jackson3Codec.kt)
- [`RedissonClientCoroutineTest.kt`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/coroutines/RedissonClientCoroutineTest.kt)
- [`RedissonNearCacheTest.kt`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/nearcache/RedissonNearCacheTest.kt)
- [`RedissonCacheConfigTest.kt`](../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/cache/RedissonCacheConfigTest.kt)

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Codec Selection Map diagram

[![Codec Selection Map diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redisson-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redisson-diagram-01.svg)

_Release README: [`infra/redisson/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/redisson/README.md)_

### Batch / Transaction Processing Flow diagram

[![Batch / Transaction Processing Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redisson-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redisson-diagram-02.svg)

_Release README: [`infra/redisson/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/redisson/README.md)_

### NearCache 2-Tier Cache Flow diagram

[![NearCache 2-Tier Cache Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redisson-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redisson-sequence-01.svg)

_Release README: [`infra/redisson/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/redisson/README.md)_

<!-- release-readme-diagrams:end -->
