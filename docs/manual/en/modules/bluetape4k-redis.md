---
manualId: bluetape4k-redis
title: "Redis Command Abstractions"
description: "The dependency contract, selection rules, and migration boundaries of the Redis umbrella module that exports Lettuce and Redisson together."
kind: library
group: caching
learningOrder: 550
---

# Redis Command Abstractions

## What it provides {#problem}

`bluetape4k-redis` is an umbrella module that brings `bluetape4k-lettuce` and `bluetape4k-redisson` into an application through one coordinate. It adds no Kotlin API of its own; its Gradle model exports both submodules as `api` dependencies. This is useful when an existing application uses both client families or is migrating between them.

The artifact name does not imply Spring Data Redis, a Spring Cache provider, or client auto-configuration. Those capabilities belong to `bluetape4k-spring-boot-redis`, `bluetape4k-cache-lettuce`, and `bluetape4k-cache-redisson`.

## Decide before adoption {#when-to-use}

- Confirm that the application actually uses both Lettuce and Redisson APIs.
- Prefer Lettuce alone when Redis commands and coroutine adapters are the main requirement.
- Choose Redisson for distributed locks, objects, Streams, or Redisson Near Cache.
- Do not assume that the two clients share lifecycle or codec wire-format rules.
- Start from a separate module when the requirement is a Spring Data Redis serializer or Spring Cache abstraction.
- Plan to replace the umbrella coordinate with one client after a migration ends.

## Add the dependency {#coordinates}

Consumers manage only the `bluetape4k-dependencies` BOM version, not separate Lettuce, Redisson, or bluetape4k module versions.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-redis")
}
```

This coordinate puts both client artifacts on the compile and runtime classpaths. If the application uses only one, follow [Selective dependency migration](./bluetape4k-redis/selective-dependency-migration.md) to reduce the classpath and operational surface.

## Make the first choice {#quick-start}

Keep the umbrella during a transition, but isolate each client family behind separate packages or adapters.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-redis")
}
```

Once new code uses only Lettuce and all Redisson calls are gone, replace the coordinate with `bluetape4k-lettuce`. The umbrella exposes no factory or initialization function of its own; use the [Lettuce manual](./bluetape4k-lettuce.md) and [Redisson manual](./bluetape4k-redisson.md) for client examples.

## Selection map {#api-by-task}

| Requirement | Start with | Boundary to keep explicit |
| --- | --- | --- |
| Both client families during a migration | `bluetape4k-redis` | The umbrella groups dependencies only. |
| Redis commands, pipelines, `RedisFuture`, coroutines | `bluetape4k-lettuce` | Assign ownership of the client and cached connections. |
| Distributed objects, locks, Streams, Redisson transactions | `bluetape4k-redisson` | Treat `shutdown()` and the Codec as deployment contracts. |
| Function-result or cache-aside abstraction | `bluetape4k-cache-lettuce` or `bluetape4k-cache-redisson` | Distinguish client helpers from cache providers. |
| Spring `RedisTemplate` serializers | `bluetape4k-spring-boot-redis` | The umbrella creates no Spring beans. |

## Learning path {#concepts}

These chapters start from the actual 1.12.1 Gradle contract. They do not invent umbrella APIs or tests; they show how to select, operate, and remove client dependencies using concrete build examples.

1. [Umbrella dependency contract](./bluetape4k-redis/umbrella-dependency-contract.md) — inspect the two exported artifacts and the capabilities the umbrella does not provide.
2. [Choosing Lettuce or Redisson](./bluetape4k-redis/lettuce-vs-redisson.md) — compare a command-oriented client with a distributed-object client by requirement.
3. [Selective dependency migration](./bluetape4k-redis/selective-dependency-migration.md) — inventory usages and reduce the build to one client coordinate.
4. [Lifecycle and codec boundaries](./bluetape4k-redis/lifecycle-codec-boundaries.md) — manage shutdown, coroutine behavior, keyspaces, and wire formats per client.
5. [Separating cache providers and Spring Data Redis](./bluetape4k-redis/cache-spring-separation.md) — distinguish client helpers, cache abstractions, and serializer modules.
6. [Testing, operations, and ecosystem paths](./bluetape4k-redis/testing-operations-ecosystem.md) — connect submodule tests and metrics to workshops and Exposed caching.

For a new adoption, read chapters 1 and 2 and choose the smaller dependency. For an existing umbrella consumer, use chapters 3 and 4 to lock usage and compatibility boundaries before changing the build.

## Recommended patterns {#patterns}

New applications should depend directly on Lettuce or Redisson when the requirement is clear. If both clients are necessary, keep their key prefixes and wire formats separate so that one client never decodes the other client's values with an incompatible Codec. Create clients once per application lifecycle, not per request, and close them at shutdown.

The umbrella makes a migration easy to start but does not prove that it is complete. Inspect source usage and the runtime classpath before removing a client.

## Integrations {#integrations}

The 1.12.1 build declares exactly two integration edges:

```kotlin
api(project(":bluetape4k-lettuce"))
api(project(":bluetape4k-redisson"))
```

Follow each detailed manual for its transitive and optional runtime boundaries. The umbrella does not add a Spring Boot starter, Spring Cache provider, or database loader/writer.

## Configuration {#configuration}

`bluetape4k-redis` has no configuration class, bean, property, or resource. Configure Lettuce URIs and `ClientResources`, Redisson `Config`, timeouts, retries, pools, and Codecs in the component that owns the actual client. Do not assume equivalent names or defaults across the two implementations.

## Failure behavior {#failures}

The umbrella does not translate exceptions or add retries and fallbacks. Connection failures, timeouts, cancellation, and Codec decode errors follow the selected client contract. Using both clients adds two failure surfaces; do not implement automatic cross-client fallback until data consistency and duplicate-execution rules are defined.

## Operations {#operations}

Observe connection counts, reconnects, command latency, timeouts, queues, and shutdown separately for each active client. Lettuce and Redisson use separate pools even when they connect to the same Redis deployment. While the umbrella remains, include the dependency tree and number of client instances in deployment checks.

## Testing {#testing}

The umbrella has no production source or dedicated tests. A successful `:bluetape4k-redis:test` task must not be presented as a suite that validates umbrella behavior. Verify real contracts in the selected submodule and in application integration tests.

```bash
./gradlew :bluetape4k-lettuce:test --no-build-cache --no-configuration-cache
./gradlew :bluetape4k-redisson:test --no-build-cache --no-configuration-cache
```

Both tasks use Redis Testcontainers, so do not run them in parallel with other heavy database suites. Documentation-only changes need release-file link and structure checks, not those heavy tests.

## Workshops {#workshops}

There is no umbrella-specific workshop. Use `LettuceClientsTest` and `RedisFutureSupportTest` for small Lettuce examples, and `RedissonClientSupportTest` and `RStreamSupportTest` for Redisson. Continue to [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop) and [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop) for cache-and-database scenarios.

## 1.12.1 scope {#limitations}

This manual follows `infra/redis/build.gradle.kts` at the `bluetape4k-projects` 1.12.1 release commit. The umbrella supplies both submodule artifacts. It does not supply a common facade, automatic client selection, Spring configuration, a cache provider, or integration tests.

The README command for `:bluetape4k-redis:test` shows how to run a Gradle task; it does not mean that the umbrella contains test code. Changes made to submodules after 1.12.1 are not described as release behavior here.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Module Dependency Structure diagram

[![Module Dependency Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redis-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redis-diagram-01.svg)

_Release README: [`infra/redis/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/redis/README.md)_

### Exported API Surface diagram

[![Exported API Surface diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redis-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-redis-diagram-02.svg)

_Release README: [`infra/redis/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/redis/README.md)_

<!-- release-readme-diagrams:end -->

## Sources and links {#sources}

- [`infra/redis/build.gradle.kts`](../../../../infra/redis/build.gradle.kts)
- [`infra/redis/README.md`](../../../../infra/redis/README.md)
- [`bluetape4k-lettuce` manual](./bluetape4k-lettuce.md)
- [`bluetape4k-redisson` manual](./bluetape4k-redisson.md)
- [`bluetape4k-cache-lettuce` manual](./bluetape4k-cache-lettuce.md)
- [`bluetape4k-cache-redisson` manual](./bluetape4k-cache-redisson.md)
- [`bluetape4k-spring-boot-redis` manual](./bluetape4k-spring-boot-redis.md)
