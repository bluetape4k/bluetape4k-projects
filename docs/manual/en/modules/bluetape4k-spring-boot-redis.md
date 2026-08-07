---
manualId: bluetape4k-spring-boot-redis
title: "Spring Data Redis Coroutine Support"
description: "Design a Spring Data Redis wire format with Kryo or Fory and compression, then apply it safely to RedisTemplate and ReactiveRedisTemplate."
kind: library
group: spring
learningOrder: 940
---

# Spring Data Redis Coroutine Support

## Capabilities {#problem}

`bluetape4k-spring-boot-redis` adapts bluetape4k binary serializers and compressors to Spring Data Redis `RedisSerializer` and `RedisSerializationContext`. It provides Kryo and Fory object formats, optional GZip, LZ4, Snappy, or Zstd compression, and helpers for applying one policy to the key, value, hash-key, and hash-value slots of `RedisTemplate` and `ReactiveRedisTemplate`.

This module is not a Redis client or cache implementation. Release 1.12.1 contains no `@AutoConfiguration`, `@ConfigurationProperties`, or `RedisTemplateCustomizer`. The application must combine the connection factory supplied by Spring Boot with explicit template beans and serializers.

## Decisions before adoption {#when-to-use}

- Decide whether values need a compact object format or a human-readable format such as JSON.
- Use Kryo or Fory for new data. The JDK serializer combinations are deprecated because of deserialization risk.
- Treat the serializer and compressor combination as a persisted Redis wire format.
- Keep keys in a stable format such as UTF-8 strings and reserve object serializers for values and hash values.
- Align all four serialization slots when synchronous and reactive templates share a keyspace.
- Separate serialization responsibility from connection, timeout, retry, and cache-consistency responsibility.

## Coordinates {#coordinates}

Applications manage only the central BOM version, not separate Spring Data Redis or codec versions.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-redis")
}
```

The artifact publishes the runtime dependencies required by its documented Fory, Kryo, LZ4, Snappy, and Zstd combinations. Its Gradle project is `:bluetape4k-spring-boot-redis`, backed by `spring-boot/redis`.

## First ReactiveRedisTemplate {#quick-start}

Receive Spring Boot's `ReactiveRedisConnectionFactory`, build a serialization context, and register the template explicitly.

```kotlin
@Configuration
class RedisTemplateConfig {

    @Bean
    fun objectRedisTemplate(
        factory: ReactiveRedisConnectionFactory,
    ): ReactiveRedisTemplate<String, Any> {
        val context = redisSerializationContextOf<Any>(
            valueSerializer = RedisBinarySerializers.LZ4Fory,
        )
        return ReactiveRedisTemplate(factory, context)
    }
}
```

This overload uses UTF-8 strings for keys and hash keys and the supplied serializer for values and hash values. Spring owns the bean and connection lifecycle; the application owns compatibility of the persisted bytes.

## API by task {#api-by-task}

| Task | Start with | Boundary |
| --- | --- | --- |
| Serialize objects | `RedisBinarySerializer` | Inherits the format and type contract of its `BinarySerializer`. |
| Select a standard combination | `RedisBinarySerializers` | Lazy singletons reduce setup, but each name denotes a wire format. |
| Compress existing bytes | `RedisCompressSerializer` | Compresses `ByteArray`; it does not serialize objects. |
| Configure a reactive context | `redisSerializationContext {}` | Set key, value, hash, and string slots directly. |
| Supply key and value serializers | `redisSerializationContextOf(K, V)` | Applies the same pair to hash keys and values by default. |
| Use String keys | `redisSerializationContextOf<V>(valueSerializer)` | Fixes keys and hash keys to UTF-8 strings. |
| Configure a synchronous template | `RedisTemplate` serializer properties | This module does not register the bean. |

## Learning path {#concepts}

These chapters follow the 1.12.1 release source and tests from module boundary through migration and operations. They cover template setup, null contracts, runtime dependencies, security, and compatibility rather than only listing serializer names.

1. [Module boundary and dependencies](./bluetape4k-spring-boot-redis/module-boundary-dependencies.md) — Separate the serializer integration from auto-configuration, connections, and cache behavior.
2. [Choosing binary serializers](./bluetape4k-spring-boot-redis/binary-serializers.md) — Compare Kryo, Fory, and deprecated JDK combinations, lazy singletons, and null behavior.
3. [Compression and Redis wire formats](./bluetape4k-spring-boot-redis/compression-wire-format.md) — Distinguish byte compression from object serialization and evaluate size, CPU, and compatibility.
4. [SerializationContext and templates](./bluetape4k-spring-boot-redis/serialization-context-templates.md) — Configure synchronous and reactive templates with the DSL and convenience overloads.
5. [Security, compatibility, and migration](./bluetape4k-spring-boot-redis/migration-security-compatibility.md) — Handle JDK deserialization risk, schema changes, and rolling deployments.
6. [Testing, operations, and ecosystem](./bluetape4k-spring-boot-redis/testing-operations-ecosystem.md) — Extend unit and consumer-runtime checks into application tests and related modules.

For a new integration, follow chapters 1, 2, and 4, then use chapter 5 to verify deployment compatibility. For an existing keyspace migration, start with chapter 5 and choose a versioned keyspace or dual-read plan before changing writers.

## Patterns {#patterns}

Manage the key prefix and serializer combination as one schema version. If `user:v2:*` uses `LZ4Fory`, keep `user:v1:*` until every required reader can handle v2. Changing a serializer is a data migration, not a local refactoring.

The predefined constants reduce construction code but do not choose the right format. Compression can cost more than it saves for small values, so measure representative payload size and latency. Limit writable keyspaces and deserializable types when Redis accepts input from other services or tools.

## Integrations {#integrations}

Spring Boot supplies the connection factory, and Spring Data Redis owns templates, command execution, and repositories. [`bluetape4k-lettuce`](./bluetape4k-lettuce.md) covers low-level Lettuce helpers; [`bluetape4k-redisson`](./bluetape4k-redisson.md) covers Redisson clients, distributed objects, and codecs.

For JCache, memoizers, or near caches, continue to [`bluetape4k-cache-lettuce`](./bluetape4k-cache-lettuce.md) or [`bluetape4k-cache-redisson`](./bluetape4k-cache-redisson.md). [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop) contains application-level Spring Boot Redis examples. Adding this serializer module does not create cache-aside or write-through behavior.

## Configuration {#configuration}

Release 1.12.1 has no module-specific properties. Configure endpoints, pools, SSL, and timeouts with Spring Boot's Spring Data Redis settings. At this module boundary, the application chooses serializers and contexts per template.

```kotlin
val context = redisSerializationContext<String, Any> {
    key(RedisSerializer.string())
    value(RedisBinarySerializers.ZstdFory)
    hashKey(RedisSerializer.string())
    hashValue(RedisBinarySerializers.ZstdFory)
}
```

`defaultSerializer` initializes the context builder. Explicit key, value, and hash settings then override their slots.

## Failures {#failures}

`serialize(null)` returns an empty byte array for both serializer types. `deserialize(null)` returns `null`. If an empty byte array is also valid domain data, define the application's null policy explicitly.

Existing values written by an incompatible codec, missing runtime codecs, compression mismatches, and model changes surface as deserialization errors. The module does not guess an old format or try another serializer. Connection errors and timeouts remain Spring Data Redis concerns; the serializers provide no fallback.

## Operations {#operations}

Count encode and decode failures separately from Redis command failures. Log a bounded serializer schema ID, key prefix, payload size, and exception type instead of the raw value. Evaluate compression with both byte reduction and CPU or end-to-end command latency.

Before a rolling deployment, verify whether the old reader can consume values from the new writer. If not, use a new key prefix and retire old values by TTL or an explicit migration.

## Testing {#testing}

Serializer and context unit tests run without Redis. The module's `check` task also runs `consumerRuntimeTest` to prove that the published runtime classpath contains the documented Fory, Kryo, and compressor implementations.

```bash
./gradlew :bluetape4k-spring-boot-redis:test \
    :bluetape4k-spring-boot-redis:consumerRuntimeTest
```

Applications should add old/new version compatibility, actual Redis round trips, all serialization slots, corrupted payloads, and null-policy tests.

## Workshops {#workshops}

Start with `RedisSerializationContextSupportTest` for small DSL examples and `RedisConsumerRuntimeClasspathTest` for a consumer-style dependency check. Then use the Spring Boot Redis examples in [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop) to connect serialization to connection configuration and cache abstractions.

For near-cache or distributed memoizer exercises, continue to the cache-lettuce or cache-redisson manuals. Verify each provider's codec boundary instead of assuming that template serializers and cache codecs are interchangeable.

## 1.12.1 scope {#limitations}

This manual targets release commit `7cf0b73646af05c0f8872cc4f6a16983949c4e3e`. The production API consists of four Kotlin source files containing serializer and context helpers.

Auto-configuration, customizers, properties, health indicators, metrics, client wrappers, and cache implementations are outside the 1.12.1 module. “Spring Boot 4” identifies its dependency line; it does not imply an auto-configuration module.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Redis Serializer Class Structure diagram

[![Redis Serializer Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-redis-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-redis-diagram-01.svg)

_Release README: [`spring-boot/redis/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/redis/README.md)_

### ReactiveRedisTemplate Serialization Flow diagram

[![ReactiveRedisTemplate Serialization Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-redis-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-redis-diagram-02.svg)

_Release README: [`spring-boot/redis/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/redis/README.md)_

<!-- release-readme-diagrams:end -->

## Sources and tests {#sources}

- [`build.gradle.kts`](../../../../spring-boot/redis/build.gradle.kts)
- [`RedisBinarySerializer.kt`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializer.kt)
- [`RedisBinarySerializers.kt`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializers.kt)
- [`RedisCompressSerializer.kt`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisCompressSerializer.kt)
- [`RedisSerializationContextSupport.kt`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupport.kt)
- [`RedisBinarySerializerTest.kt`](../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializerTest.kt)
- [`RedisConsumerRuntimeClasspathTest.kt`](../../../../spring-boot/redis/src/consumerRuntimeTest/kotlin/io/bluetape4k/spring/redis/serializer/RedisConsumerRuntimeClasspathTest.kt)
