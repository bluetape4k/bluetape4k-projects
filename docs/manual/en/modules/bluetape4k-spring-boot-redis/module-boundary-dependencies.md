---
title: Module boundary and dependencies
description: Separate the serializer API in this Spring Boot Redis module from auto-configuration, client, and cache responsibilities.
manualId: bluetape4k-spring-boot-redis
chapterId: module-boundary-dependencies
---

# Module boundary and dependencies

## Read the source before the module name

The production surface has four files: `RedisBinarySerializer`, `RedisCompressSerializer`, the predefined `RedisBinarySerializers`, and `RedisSerializationContext` helpers.

Release 1.12.1 does not contain:

- `@AutoConfiguration` or starter metadata
- module-owned `RedisTemplate` or `ReactiveRedisTemplate` beans
- dedicated `@ConfigurationProperties` or template customizers
- client creation or endpoint, pool, SSL, and timeout settings
- cache-aside, JCache, memoizers, near caches, or Pub/Sub invalidation

It is a small serialization integration on top of Spring Boot's connection factory.

## Dependency scope

The build exposes the Spring Data Redis starter and connects `bluetape4k-core` and `bluetape4k-io`. It publishes the codec and compressor runtime dependencies needed by the documented Fory, Kryo, LZ4, Snappy, and Zstd combinations.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-redis")
}
```

Consumers do not pin those library versions independently. Adding the artifact still does not provision Redis or define a connection.

## Responsibility split with Spring Data Redis

Spring Boot configures `RedisConnectionFactory` and `ReactiveRedisConnectionFactory`. Spring Data Redis provides templates, command execution, and exception translation. This module only controls how template inputs become bytes and how bytes become values.

```text
application object
  -> bluetape4k RedisSerializer
  -> Spring Data Redis template
  -> connection factory / Lettuce driver
  -> Redis server
```

Serializers own no connections and have nothing to close. Spring manages the connection factory and template bean lifecycle.

## Related Redis modules

`bluetape4k-lettuce` provides Lettuce commands, coroutines, and codec helpers. `bluetape4k-redisson` provides Redisson clients, distributed objects, codecs, and coroutine APIs. The cache-lettuce and cache-redisson modules implement JCache, memoizers, and near caches.

The decision here is not “Which Redis client should I use?” It is “Which byte format should a Spring Data Redis template persist?”

## Adoption checklist

- Select the Redis driver and connection factory managed by Spring Boot.
- Decide between synchronous, reactive, or both template styles.
- Fix a stable key and hash-key format.
- Record value serializer and compressor as a schema.
- Verify compatibility with existing values.
- Select a separate cache module when JCache or near-cache behavior is required.

## Sources and tests

- [`build.gradle.kts`](../../../../../spring-boot/redis/build.gradle.kts)
- [`RedisBinarySerializer.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializer.kt)
- [`RedisSerializationContextSupport.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupport.kt)
- [`RedisConsumerRuntimeClasspathTest.kt`](../../../../../spring-boot/redis/src/consumerRuntimeTest/kotlin/io/bluetape4k/spring/redis/serializer/RedisConsumerRuntimeClasspathTest.kt)
