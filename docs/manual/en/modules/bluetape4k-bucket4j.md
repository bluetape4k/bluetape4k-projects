---
manualId: bluetape4k-bucket4j
title: "Distributed Rate Limiting"
description: "A wrapper and utility module for building application-level rate limiters using Bucket4j."
kind: library
group: operations
learningOrder: 1010
---

# Distributed Rate Limiting

## Problem {#problem}

A wrapper and utility module for building application-level rate limiters using Bucket4j. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-bucket4j` when the application needs client lifecycle, reconnect policy, backpressure, retries, and observability. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-bucket4j")
}
```

Gradle project path: `:bluetape4k-bucket4j`. Source directory: `infra/bucket4j`.

## Concepts {#concepts}

The first source-level concepts to inspect are `BucketKeyValidation`, `ConfigurationSupport`, `SuspendLocalBucket`, `AsyncBucketProxyProvider`, `BucketProxyProvider`, `LettuceBasedProxyManagerSupport`, `RedissonBasedProxyManagerSupport`, and `Slf4jBucketListener`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`BucketKeyValidation`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/BucketKeyValidation.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`BucketKeyValidation`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/BucketKeyValidation.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ConfigurationSupport`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/ConfigurationSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendLocalBucket`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/coroutines/SuspendLocalBucket.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AsyncBucketProxyProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/AsyncBucketProxyProvider.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BucketProxyProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/BucketProxyProvider.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceBasedProxyManagerSupport`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/redis/LettuceBasedProxyManagerSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RedissonBasedProxyManagerSupport`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/redis/RedissonBasedProxyManagerSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Slf4jBucketListener`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/internal/Slf4jBucketListener.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AbstractLocalBucketProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/local/AbstractLocalBucketProvider.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LocalBucketProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/local/LocalBucketProvider.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Key Features**, **Class Structure**, **Bucket4j Integration Class Diagram**, **Rate Limiting Sequence Diagrams**, **Local Rate Limiter — Token Consumption Flow**, **Distributed Suspend Rate Limiter — Redis-Based Coroutine Flow**, **What This Module Adds Over Raw Bucket4j**, **Dependency**, **Usage Examples**, and **1) Local Rate Limiter**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-core"))
compileOnly(project(":bluetape4k-cache-core"))
api(libs.bucket4j.core)
compileOnly(libs.bucket4j.lettuce)
compileOnly(libs.bucket4j.redisson)
compileOnly(libs.caffeine)
compileOnly(libs.lettuce.core)
compileOnly(libs.redisson)
compileOnly(project(":bluetape4k-coroutines"))
compileOnly(libs.kotlinx.coroutines.core)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track connection state, queue depth, retries, timeouts, remote errors, and graceful shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-bucket4j:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractBucket4jTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/AbstractBucket4jTest.kt)
- [`ConfigurationSupportTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/ConfigurationSupportTest.kt)
- [`TestRedisServer`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/TestRedisServer.kt)
- [`SuspendLocalBucketListenerTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/coroutines/SuspendLocalBucketListenerTest.kt)
- [`SuspendedLocalBucketTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/coroutines/SuspendedLocalBucketTest.kt)
- [`AbstractAsyncBucketProxyProviderTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/distributed/AbstractAsyncBucketProxyProviderTest.kt)
- [`AbstractBucketProxyProviderTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/distributed/AbstractBucketProxyProviderTest.kt)
- [`LettuceAsyncBucketProxyProviderTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/distributed/redis/LettuceAsyncBucketProxyProviderTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Bucket4j Integration Class Diagram

[![Bucket4j Integration Class Diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-bucket4j-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-bucket4j-diagram-01.svg)

_Release README: [`infra/bucket4j/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/bucket4j/README.md)_

### Local Rate Limiter Token Consumption Flow

[![Local Rate Limiter Token Consumption Flow](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-bucket4j-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-bucket4j-sequence-01.svg)

_Release README: [`infra/bucket4j/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/bucket4j/README.md)_

### Distributed Suspend Rate Limiter Redis Coroutine Flow

[![Distributed Suspend Rate Limiter Redis Coroutine Flow](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-bucket4j-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-bucket4j-sequence-02.svg)

_Release README: [`infra/bucket4j/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/bucket4j/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../infra/bucket4j/README.md)
- [Module build](../../../../infra/bucket4j/build.gradle.kts)
- [`BucketKeyValidation`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/BucketKeyValidation.kt)
- [`ConfigurationSupport`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/ConfigurationSupport.kt)
- [`SuspendLocalBucket`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/coroutines/SuspendLocalBucket.kt)
- [`AsyncBucketProxyProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/AsyncBucketProxyProvider.kt)
- [`BucketProxyProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/BucketProxyProvider.kt)
- [`LettuceBasedProxyManagerSupport`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/redis/LettuceBasedProxyManagerSupport.kt)
- [`RedissonBasedProxyManagerSupport`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/distributed/redis/RedissonBasedProxyManagerSupport.kt)
- [`Slf4jBucketListener`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/internal/Slf4jBucketListener.kt)
- [`AbstractLocalBucketProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/local/AbstractLocalBucketProvider.kt)
- [`LocalBucketProvider`](../../../../infra/bucket4j/src/main/kotlin/io/bluetape4k/bucket4j/local/LocalBucketProvider.kt)
- [`AbstractBucket4jTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/AbstractBucket4jTest.kt)
- [`ConfigurationSupportTest`](../../../../infra/bucket4j/src/test/kotlin/io/bluetape4k/bucket4j/ConfigurationSupportTest.kt)
