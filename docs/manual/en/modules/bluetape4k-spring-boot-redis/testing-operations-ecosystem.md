---
title: Testing, operations, and ecosystem
description: Extend serializer, context, and consumer-runtime checks into application operations and related Lettuce, Redisson, cache, and workshop paths.
manualId: bluetape4k-spring-boot-redis
chapterId: testing-operations-ecosystem
---

# Testing, operations, and ecosystem

## What module tests prove

Unit tests run without Redis and cover:

- binary round trips for strings, data classes, and lists
- Kryo, Fory, JDK, and four compressor combinations
- null serialization and deserialization
- compression-only round trips
- key and value pairs in serialization contexts
- JDK deprecation messages and replacements

```bash
./gradlew :bluetape4k-spring-boot-redis:test
```

These checks prove serializer symmetry, not Redis connection, TTL, transaction, cluster, or rolling-version behavior.

## Consumer runtime classpath

Runtime-only codecs need a consumer-shaped check. `consumerRuntimeTest` executes `LZ4Fory` and `LZ4Kryo` round trips with the runtime classpath that published consumers receive.

```bash
./gradlew :bluetape4k-spring-boot-redis:consumerRuntimeTest
```

The module's `check` task depends on this test. When documenting another standard combination, verify both its runtime dependency and consumer test coverage.

## Application integration tests

Add these boundaries in the consuming service:

1. A reactive template reads values from a synchronous template.
2. Both value and hash-value operations round-trip.
3. Old and new application versions cross-read.
4. Corrupted, truncated, and mismatched-compressor payloads follow the intended failure policy.
5. Null, missing keys, and empty bytes remain distinct.
6. Redis command timeouts remain distinguishable from serializer failures.

Run Redis Testcontainers sequentially with other heavy integration suites.

## Operational signals

Observe encode/decode failures, payload bytes, Redis command latency, and application CPU together. A good compression ratio may still cost too much CPU, while network-heavy deployments may benefit from it.

Avoid unbounded metric labels for class or field names. Use a limited serializer schema ID for metrics and sampled logs for detailed exceptions.

## Incident response

After a deployment, rising decode failures first point to writer and reader versions and key prefixes. Separate them from connection failures. If only the new format is affected, stop its writer, isolate v2 keys, and keep the old-key fallback until recovery.

Do not clear the entire Redis database automatically. Scope cleanup by prefix, TTL, and the cost of rebuilding from the source of truth.

## Ecosystem learning path

- Spring Data Redis serializers and templates: this manual
- Lettuce commands, coroutines, and codecs: [`bluetape4k-lettuce`](../bluetape4k-lettuce.md)
- Redisson clients, distributed objects, and codecs: [`bluetape4k-redisson`](../bluetape4k-redisson.md)
- JCache, memoizers, and near caches: [`bluetape4k-cache-lettuce`](../bluetape4k-cache-lettuce.md), [`bluetape4k-cache-redisson`](../bluetape4k-cache-redisson.md)
- Runnable Spring Boot Redis examples: [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)

Serialization is one part of a cache strategy. Design cache-aside, read-through, write-through, and invalidation at the cache and repository boundary.

## Sources and tests

- [`build.gradle.kts`](../../../../../spring-boot/redis/build.gradle.kts)
- [`RedisBinarySerializersTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializersTest.kt)
- [`RedisSerializationContextSupportTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupportTest.kt)
- [`RedisConsumerRuntimeClasspathTest.kt`](../../../../../spring-boot/redis/src/consumerRuntimeTest/kotlin/io/bluetape4k/spring/redis/serializer/RedisConsumerRuntimeClasspathTest.kt)
