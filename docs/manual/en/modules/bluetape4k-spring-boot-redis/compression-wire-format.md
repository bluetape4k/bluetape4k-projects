---
title: Compression and Redis wire formats
description: Separate byte compression from object serialization and manage format, performance, and compatibility boundaries.
manualId: bluetape4k-spring-boot-redis
chapterId: compression-wire-format
---

# Compression and Redis wire formats

## Compression-only serialization

`RedisCompressSerializer` accepts a `ByteArray`, compresses it, and restores it. It does not convert objects into a binary format.

```kotlin
val serializer = RedisCompressSerializer(Compressors.LZ4)
val compressed = serializer.serialize(payload)
val restored = serializer.deserialize(compressed)
```

Use it when another layer already emits bytes, such as Protobuf, images, or documents. Use a `RedisBinarySerializer` combination for application objects.

## Available compressors

Compression-only singletons are `Gzip`, `LZ4`, `Snappy`, and `Zstd`. Binary combinations apply one of the same compressors after object serialization.

The compressor name is not embedded automatically. A value written with `LZ4Fory` cannot be identified and decoded as an older format by `ZstdFory`. Use a key prefix, cache name, or envelope header to identify the format.

## Measure small payloads

Compression can reduce network and Redis memory while adding CPU and allocation. Headers and block overhead may make small values larger.

Measure representative workloads for:

- compressed and uncompressed byte size
- serialize/compress and decompress/deserialize latency
- end-to-end command latency and application CPU
- Redis memory and network throughput
- the production payload-size distribution

Module tests prove round-trip correctness. They make no benchmark claim that one combination is faster.

## Null and corrupted payloads

`serialize(null)` returns empty bytes, while `deserialize(null)` returns `null`. Keep this distinct from compressing a real empty byte array.

Truncated or mismatched payloads fail during decompression. Trying several compressors as fallback can hide corruption and drift. Record an explicit format version and observe the failure instead.

## Manage deployment formats

1. Allocate a new key prefix or schema version.
2. Verify the new writer and reader together.
3. Test whether an old reader can see a new value during rolling deployment.
4. Add dual-read or an explicit migration if required.
5. Define TTL or deletion for the old keyspace.

A compressor change is a wire-format change even when the object serializer stays Fory.

## Sources and tests

- [`RedisCompressSerializer.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisCompressSerializer.kt)
- [`RedisBinarySerializers.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializers.kt)
- [`RedisCompressSerializerTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisCompressSerializerTest.kt)
- [`RedisBinarySerializersTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializersTest.kt)
