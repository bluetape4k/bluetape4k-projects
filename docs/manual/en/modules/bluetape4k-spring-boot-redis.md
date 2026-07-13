---
manualId: bluetape4k-spring-boot-redis
title: "bluetape4k-spring-boot-redis"
description: "A module that replaces Spring Data Redis's serialization layer with high-performance binary serialization and compression combinations (Spring Boot 4.x)."
kind: library
group: spring
---

# bluetape4k-spring-boot-redis

## Problem {#problem}

A module that replaces Spring Data Redis's serialization layer with high-performance binary serialization and compression combinations (Spring Boot 4.x). This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-spring-boot-redis` when the application needs auto-configuration conditions, bean ownership, property binding, and application lifecycle. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-redis")
}
```

Gradle project path: `:bluetape4k-spring-boot-redis`. Source directory: `spring-boot/redis`.

## Concepts {#concepts}

The first source-level concepts to inspect are `RedisBinarySerializer`, `RedisBinarySerializers`, `RedisCompressSerializer`, and `RedisSerializationContextSupport`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`RedisBinarySerializer`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializer.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`RedisBinarySerializer`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RedisBinarySerializers`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializers.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RedisCompressSerializer`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisCompressSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RedisSerializationContextSupport`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Key Features**, **Architecture Diagrams**, **Redis Serializer Class Structure**, **ReactiveRedisTemplate Serialization Flow**, **Installation**, **Usage Examples**, **ReactiveRedisTemplate Configuration (DSL approach)**, **ReactiveRedisTemplate Configuration (convenience function approach)**, **RedisTemplate Configuration**, and **Serializer Reference**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-core"))
api(project(":bluetape4k-io"))
api("org.springframework.boot:spring-boot-starter-data-redis")
runtimeOnly(libs.fory.kotlin)
runtimeOnly(libs.kryo5)
runtimeOnly(libs.lz4.java)
runtimeOnly(libs.zstd.jni)
runtimeOnly(libs.snappy.java)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track condition reports, startup failures, pool/client health, request latency, and graceful shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-spring-boot-redis:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractRedisSerializerTest`](../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/AbstractRedisSerializerTest.kt)
- [`RedisBinarySerializerTest`](../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializerTest.kt)
- [`RedisBinarySerializersTest`](../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializersTest.kt)
- [`RedisCompressSerializerTest`](../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisCompressSerializerTest.kt)
- [`RedisSerializationContextSupportTest`](../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupportTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../spring-boot/redis/README.md)
- [Module build](../../../../spring-boot/redis/build.gradle.kts)
- [`RedisBinarySerializer`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializer.kt)
- [`RedisBinarySerializers`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializers.kt)
- [`RedisCompressSerializer`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisCompressSerializer.kt)
- [`RedisSerializationContextSupport`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupport.kt)
- [`AbstractRedisSerializerTest`](../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/AbstractRedisSerializerTest.kt)
- [`RedisBinarySerializerTest`](../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializerTest.kt)
- [`RedisBinarySerializersTest`](../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializersTest.kt)
- [`RedisCompressSerializerTest`](../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisCompressSerializerTest.kt)
- [`RedisSerializationContextSupportTest`](../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupportTest.kt)
