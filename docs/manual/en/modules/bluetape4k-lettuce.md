---
manualId: bluetape4k-lettuce
title: "Lettuce Coroutine Client"
description: "Operate the Lettuce Redis client from Kotlin with connection, coroutine, codec, map, and script utilities."
kind: library
group: caching
learningOrder: 540
---

# Lettuce Coroutine Client

## Capabilities {#problem}

`bluetape4k-lettuce` adds Kotlin-oriented client and connection factories, sync/async/coroutine command entry points, `RedisFuture` adapters, object codecs, and distributed data structures to Lettuce. It also includes loaded maps, Lua script execution, and probabilistic filters.

It is not a framework that owns the Redis lifecycle. The application must decide who closes clients, connections, and write-behind workers.

## Decisions before adoption {#when-to-use}

- Decide whether raw Lettuce is enough or wrappers and coroutine adapters are needed.
- Assign ownership of `RedisClient`, cached connections, and shared `ClientResources`.
- Choose one sync, async-future, or coroutine path per call chain.
- Freeze the Redis wire format and plan codec migrations.
- Define cache-miss, write-through, and write-behind failure policy.
- Use Lua only when a single Redis command cannot provide the required atomicity.

## Dependencies {#coordinates}

Consumers manage only the central `bluetape4k-dependencies` BOM version.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-lettuce")
}
```

Coroutine, cache-core, Kryo/Fory, and compression capabilities are optional and require the corresponding runtime dependencies when used.

## First connection {#quick-start}

```kotlin
val client = LettuceClients.clientOf("redis://localhost:6379")
try {
    val commands = LettuceClients.commands(client)
    commands.set("greeting", "hello")
    check(commands.get("greeting") == "hello")
} finally {
    LettuceClients.shutdown(client)
}
```

`commands(client)` reuses a client-scoped cached connection. Direct connections and loaded-map connections remain owned by their creators.

## API map {#api-by-task}

| Task | Start with | Boundary |
| --- | --- | --- |
| Client and cached connection | `LettuceClients` | Distinguish `shutdown(client)` from process-wide `shutdown()`. |
| Sync/async/coroutine commands | `commands`, `asyncCommands`, `coroutinesCommands` | Lettuce marks its coroutine API experimental. |
| Await futures | `awaitSuspending`, `awaitAll` | Preserve failures and cancellation. |
| Store objects | `LettuceBinaryCodecs`, `LettuceJsonCodecs` | A codec defines the persisted wire format. |
| Maps with loaders and writers | `LettuceMap`, `LettuceLoadedMap` | Write-through and write-behind fail at different times. |
| Atomic scripts | `RedisScriptRunner` | It falls back from `EVALSHA` to `EVAL` only on `NOSCRIPT`. |
| Probabilistic structures | `LettuceBloomFilter`, `LettuceCuckooFilter` | Include false positives and initialization parameters in the contract. |

## Learning path {#concepts}

These chapters go beyond a feature list. They follow the 1.12.1 release source and representative tests, with runnable examples for ownership, cancellation, wire compatibility, and write-behind failures.

1. [Clients and connections](./bluetape4k-lettuce/clients-and-connections.md)
2. [Commands and coroutines](./bluetape4k-lettuce/commands-and-coroutines.md)
3. [Codecs and serialization](./bluetape4k-lettuce/codecs-and-serialization.md)
4. [Maps and cache loading](./bluetape4k-lettuce/maps-and-cache-loading.md)
5. [Filters, scripts, and primitives](./bluetape4k-lettuce/filters-scripts-and-primitives.md)
6. [Operations and ecosystem](./bluetape4k-lettuce/operations-and-ecosystem.md)

## Recommended patterns {#patterns}

Reuse a client per application and close it from the shutdown lifecycle. Issue all pipelined commands inside `withPipeline`, then await the futures outside the block. Migrate codecs with a new key prefix or a full cache reset. Treat write-behind capacity, flush failures, dead letters, and shutdown draining as operational state.

## Integrations {#integrations}

Lettuce core is an API dependency; coroutine, cache-core, serializers, and compression libraries are optional. Use [`bluetape4k-cache-lettuce`](./bluetape4k-cache-lettuce.md) for memoization and [`bluetape4k-hibernate-cache-lettuce`](./bluetape4k-hibernate-cache-lettuce.md) for Hibernate second-level caching.

## Configuration {#configuration}

Default clients share NCPU-sized `ClientResources` and enable keep-alive and TCP_NODELAY. Override the connection timeout with `-Dbluetape4k.lettuce.connectTimeoutMs`. `LettuceCacheConfig` validates TTL, key prefix, write mode, queue capacity, and shutdown timeout inputs.

## Failure behavior {#failures}

`awaitAll()` propagates a failed future instead of returning a partial list. Write-through does not update Redis when the writer fails. A full write-behind queue raises `IllegalStateException`; repeated flush failures are sent to dead-letter keys on a best-effort basis. Coroutine maps rethrow `CancellationException`.

## Operations {#operations}

Observe connection and reconnect state, command latency, pipeline batch size, write-behind queues and dead letters, and cache hit/miss rates. `shutdown(client)` closes that client's cached connections; parameterless `shutdown()` closes shared resources and belongs only at process termination.

## Testing {#testing}

```bash
./gradlew :bluetape4k-lettuce:test --no-build-cache --no-configuration-cache
```

The release tests cover cached connection reuse, future ordering and failure propagation, codec incompatibility, loader/writer modes, cancellation, and script fallback. Redis tests use Testcontainers and should not run in parallel with other heavy database suites.

## Workshops {#workshops}

Start with `LettuceClientsTest`, `RedisFutureSupportTest`, `LettuceLoadedMapTest`, and `RedisScriptTest`. Continue to [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop) and [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop) for cache and database boundaries.

## 1.12.1 scope {#limitations}

`LettuceCacheConfig` exposes near-cache fields, but the 1.12.1 loaded maps do not consume them and provide neither a Caffeine store nor RESP3 client-tracking invalidation. Do not treat the `*_WITH_NEAR_CACHE` presets as a working local-cache guarantee. RESP3 appears only in a rejected benchmark configuration.

## Source and tests {#sources}

- [`LettuceClients.kt`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceClients.kt)
- [`RedisFutureSupport.kt`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupport.kt)
- [`LettuceBinaryCodecs.kt`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecs.kt)
- [`LettuceLoadedMap.kt`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/map/LettuceLoadedMap.kt)
- [`RedisScript.kt`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/script/RedisScript.kt)
- [`LettuceClientsTest.kt`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/LettuceClientsTest.kt)
- [`LettuceSuspendedLoadedMapTest.kt`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/map/LettuceSuspendedLoadedMapTest.kt)

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Distributed Primitive API Families diagram

[![Distributed Primitive API Families diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-lettuce-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-lettuce-diagram-01.svg)

_Release README: [`infra/lettuce/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/lettuce/README.md)_

### Lettuce Codec API Structure diagram

[![Lettuce Codec API Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-lettuce-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-lettuce-diagram-02.svg)

_Release README: [`infra/lettuce/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/lettuce/README.md)_

### LettuceLoadedMap Read-Through / Write-Through Flow diagram

[![LettuceLoadedMap Read-Through / Write-Through Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-lettuce-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-lettuce-sequence-01.svg)

_Release README: [`infra/lettuce/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/lettuce/README.md)_

<!-- release-readme-diagrams:end -->
