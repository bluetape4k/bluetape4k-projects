---
manualId: bluetape4k-lettuce
title: "bluetape4k-lettuce"
description: "A Kotlin extension module for the Lettuce Redis client, providing high-performance binary codecs and RedisFuture → Coroutines adapters."
kind: library
group: infrastructure
---

# bluetape4k-lettuce

## Problem {#problem}

A Kotlin extension module for the Lettuce Redis client, providing high-performance binary codecs and RedisFuture → Coroutines adapters. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-lettuce` when the application needs client lifecycle, reconnect policy, backpressure, retries, and observability. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-lettuce")
}
```

Gradle project path: `:bluetape4k-lettuce`. Source directory: `infra/lettuce`.

## Concepts {#concepts}

The first source-level concepts to inspect are `LettuceClients`, `LettuceConst`, `RedisCommandSupports`, `RedisFutureSupport`, `LettuceAtomicLong`, `LettuceSuspendAtomicLong`, `LettuceBinaryCodec`, and `LettuceBinaryCodecs`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`LettuceClients`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceClients.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`LettuceClients`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceClients.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceConst`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceConst.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RedisCommandSupports`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisCommandSupports.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RedisFutureSupport`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceAtomicLong`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/atomic/LettuceAtomicLong.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceSuspendAtomicLong`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/atomic/LettuceSuspendAtomicLong.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceBinaryCodec`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceBinaryCodecs`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecs.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceIntCodec`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceIntCodec.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceJsonCodec`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodec.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Features**, **Performance Optimizations**, **Codec Benchmark Results**, **Connection Benchmark Results**, **Key Techniques**, **1. Shared DEFAULTCLIENTRESOURCES (NCPU Thread Pool)**, **2. Tuned SocketOptions**, **3. withPipeline{} — Batch Flush Extension**, **4. Collection.awaitAll() — Bulk Await**, and **Lessons from Benchmarking**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
api(project(":bluetape4k-io"))
api(project(":bluetape4k-netty"))
api(libs.lettuce.core)
compileOnly(project(":bluetape4k-coroutines"))
compileOnly(libs.kotlinx.coroutines.core)
compileOnly(libs.kotlinx.coroutines.reactor)
compileOnly(project(":bluetape4k-cache-core"))
compileOnly(libs.fory.kotlin)
compileOnly(libs.kryo5)
compileOnly(libs.lz4.java)
compileOnly(libs.snappy.java)
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
./gradlew :bluetape4k-lettuce:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractLettuceTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/AbstractLettuceTest.kt)
- [`AsyncCommandsTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/AsyncCommandsTest.kt)
- [`CoroutinesCommandTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/CoroutinesCommandTest.kt)
- [`LettuceClientsTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/LettuceClientsTest.kt)
- [`LettuceTestUtils`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/LettuceTestUtils.kt)
- [`RedisCommandSupportsTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/RedisCommandSupportsTest.kt)
- [`RedisFutureSupportTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupportTest.kt)
- [`LettuceAtomicLongTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/atomic/LettuceAtomicLongTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../infra/lettuce/README.md)
- [Module build](../../../../infra/lettuce/build.gradle.kts)
- [`LettuceClients`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceClients.kt)
- [`LettuceConst`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceConst.kt)
- [`RedisCommandSupports`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisCommandSupports.kt)
- [`RedisFutureSupport`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupport.kt)
- [`LettuceAtomicLong`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/atomic/LettuceAtomicLong.kt)
- [`LettuceSuspendAtomicLong`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/atomic/LettuceSuspendAtomicLong.kt)
- [`LettuceBinaryCodec`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt)
- [`LettuceBinaryCodecs`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecs.kt)
- [`LettuceIntCodec`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceIntCodec.kt)
- [`LettuceJsonCodec`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodec.kt)
- [`AbstractLettuceTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/AbstractLettuceTest.kt)
- [`AsyncCommandsTest`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/AsyncCommandsTest.kt)
