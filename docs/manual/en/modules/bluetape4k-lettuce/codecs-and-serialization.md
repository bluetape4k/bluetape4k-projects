---
title: Codecs and serialization
description: Treat Redis codecs as wire formats with explicit trust and migration boundaries.
manualId: bluetape4k-lettuce
chapterId: codecs-and-serialization
---

# Codecs and serialization

## A codec is persisted format

`LettuceBinaryCodec<V>` combines String keys with values produced by a `BinarySerializer`. `LettuceJsonCodec<V>` restores JSON to an explicit `valueType`. The integer and long codecs use fixed-width big-endian values compatible with Redisson primitive codecs.

```kotlin
val codec = LettuceBinaryCodecs.lz4Fory<User>()
val connection = LettuceClients.connect(client, codec)
connection.sync().set("user:1", User(1, "Alice"))
```

## Migrate before changing codecs

The default binary codec is LZ4+Fory. FastFory is not wire-compatible with Fory, has no fallback, and does not support cyclic object graphs. A codec switch therefore needs a new prefix, dual writes, or a deliberate cache reset.

## Untrusted payloads

Use object deserializers only for trusted values written by the application. If external actors can modify Redis bytes, narrow Redis permissions and prefer primitives or JSON with an explicit type contract. Compression may cost more than it saves for small values, so benchmark representative payloads.

## Source and tests

- [`LettuceBinaryCodecs.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecs.kt)
- [`LettuceJsonCodec.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodec.kt)
- [`FastForyCompatibilityTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/FastForyCompatibilityTest.kt)

Continue with [Maps and cache loading](./maps-and-cache-loading.md).
