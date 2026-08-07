---
title: Codecs, wire format, and security
description: Explains KafkaCodec type headers, Jackson allowlists, poison-pill behavior, and optional binary/compression backends.
manualId: bluetape4k-kafka
chapterId: codecs-wire-format-security
---

# Codecs, wire format, and security

## One type for serialization and deserialization

`KafkaCodec<T>` implements Kafka `Serializer<T>`, `Deserializer<T>`, and `Closeable`. `StringKafkaCodec`, `ByteArrayKafkaCodec`, `JacksonKafkaCodec`, and binary codecs can apply the same wire-format contract on producers and consumers.

```kotlin
val codec = StringKafkaCodec()
val bytes = codec.serialize("events", "hello")
val restored = codec.deserialize("events", bytes)
```

`StringKafkaCodec` reads key/value-specific serializer and deserializer encodings and falls back to UTF-8 for an invalid charset name. It reads an empty byte array as `null`, so it cannot distinguish empty strings from null in protocols that need both.

## Type-header contract

`AbstractKafkaCodec` writes the Java FQN to the `bluetape4k.kafka.codec.value.type` header by default. The Jackson codec uses that header to select a class during deserialization. The wire format therefore includes both payload bytes and this header.

If an untrusted producer can edit the header, it controls class-loading input. The 1.12.1 default `allowedTypePackages` is therefore empty and denies every header class.

```kotlin
val codec = JacksonKafkaCodec(
    allowedTypePackages = setOf("com.example.events")
)
```

Entries match an exact FQN or a package prefix. `ALLOW_ALL_TYPES_UNSAFE` bypasses all checks and is only appropriate when every producer and broker is controlled.

## Poison pills

On an ordinary deserialization `Exception`, `AbstractKafkaCodec` logs a warning and returns `null`. The consumer loop can continue, but the failed record can be lost unless it is observed. `CancellationException` and JVM `Error` propagate.

Count null failures and configure recovery such as Spring Kafka `ErrorHandlingDeserializer` and `DeadLetterPublishingRecoverer`. If null is a valid payload, use an envelope that distinguishes valid null from decode failure.

## Binary codecs and optional dependencies

`KafkaCodecs` exposes Kryo/Fory combined with LZ4, Snappy, and Zstd. Kryo, Fory, Snappy, and Zstd are optional in the 1.12.1 build. Accessing a singleton without its runtime classes can fail during initialization.

The Fory variants use a default serializer that accepts unregistered classes. Restrict them to trusted topics, or provide a codec backed by an application-configured serializer that enforces registration.

## Compatibility procedure

Changing a codec, compression algorithm, or class name is a topic-schema change:

1. Verify old consumers against new producer records.
2. Verify new consumers against stored historical records.
3. Update package renames and allowlists together.
4. Measure header/payload size and compression CPU.
5. Test DLQ and replay for failed records.

## Sources and tests

- [`KafkaCodec.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt)
- [`JacksonKafkaCodec.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/JacksonKafkaCodec.kt)
- [`BinaryKafkaCodecs.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecs.kt)
- [`StringKafkaCodec.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/StringKafkaCodec.kt)
- [`JacksonKafkaCodecSecurityTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/codec/JacksonKafkaCodecSecurityTest.kt)
- [`AbstractKafkaCodecPoisonPillTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/codec/AbstractKafkaCodecPoisonPillTest.kt)
