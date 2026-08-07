---
title: Choosing binary serializers
description: Understand RedisBinarySerializer null behavior, Kryo and Fory choices, deprecated JDK combinations, and lazy singletons.
manualId: bluetape4k-spring-boot-redis
chapterId: binary-serializers
---

# Choosing binary serializers

## Adapt BinarySerializer to Spring

`RedisBinarySerializer` wraps a bluetape4k `BinarySerializer` as Spring Data Redis `RedisSerializer<Any>`.

```kotlin
val serializer = RedisBinarySerializer(BinarySerializers.LZ4Fory)

val encoded = serializer.serialize(order)
val decoded = serializer.deserialize(encoded) as Order
```

The wrapper adds no type metadata policy. Class registration, schema compatibility, and allowed types remain contracts of the selected binary serializer.

## Predefined lazy combinations

`RedisBinarySerializers` creates every instance lazily.

| Serializer | None | GZip | LZ4 | Snappy | Zstd |
| --- | --- | --- | --- | --- | --- |
| Kryo | `Kryo` | `GzipKryo` | `LZ4Kryo` | `SnappyKryo` | `ZstdKryo` |
| Fory | `Fory` | `GzipFory` | `LZ4Fory` | `SnappyFory` | `ZstdFory` |
| JDK | `Jdk` | `GzipJdk` | `LZ4Jdk` | `SnappyJdk` | `ZstdJdk` |

Every JDK entry is deprecated. Use Kryo or Fory for general object values.

## Null and empty bytes

`serialize(null)` returns `emptyByteArray`; `deserialize(null)` returns `null`. Tests repeat this contract for every binary combination.

An empty byte array can also be valid application data. If null, missing keys, and empty payloads must differ, reject null at the template boundary or use an explicit envelope. Keep key absence as a template-level outcome rather than a serializer domain value.

## Choosing Kryo or Fory

Choose from actual model compatibility, not generic performance claims.

- Round-trip representative DTOs, collections, and nullable fields.
- Test readers and writers from different application versions.
- Verify class-name, field, and polymorphic-type changes.
- Compare a schema-based or readable format when other languages or tools consume the values.

The 1.12.1 tests cover same-version strings, data classes, lists, and compression combinations. They do not guarantee long-term schema evolution.

## Why JDK serialization is deprecated

JDK deserialization can expose gadget chains from classes on the application classpath. Each JDK constant points to a Kryo replacement in its `@Deprecated` metadata.

For existing JDK values, plan a versioned keyspace and migration window instead of suppressing warnings indefinitely. Treat values as untrusted whenever other services, tools, or compromised credentials can write them.

## Sources and tests

- [`RedisBinarySerializer.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializer.kt)
- [`RedisBinarySerializers.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializers.kt)
- [`RedisBinarySerializerTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializerTest.kt)
- [`RedisBinarySerializersTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializersTest.kt)
