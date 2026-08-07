---
title: Codecs, security, and wire format
description: Choose Redisson codecs with explicit compatibility, package allow-list, fallback decode, and decompression limits.
manualId: bluetape4k-redisson
chapterId: codecs-security-wire-format
---

# Codecs, security, and wire format

## A codec is a deployment contract

Changing a codec changes bytes stored under existing Redis keys. It is a schema migration that affects rolling deploys and rollback, not merely a performance toggle. Every producer and consumer of a cache name must agree on key and value codecs.

| Purpose | 1.12.1 option | Boundary |
| --- | --- | --- |
| General internal objects | `RedissonCodecs.Fory` | Unsupported values may use Kryo5 fallback. |
| Ephemeral high-throughput cache | `FastForyCodec`, `LZ4FastFory` | Old Fory readers cannot read FastFory bytes. |
| Inspectable JSON | `Jackson3Codec` | Manage the type envelope and package allow-list. |
| JSONB | `Fastjson2Codec` | Validate class names before class loading. |
| Compressed values | LZ4, Zstd, Snappy, or GZip wrapper | Measure CPU and bound decompressed size. |

## Match allow-lists to trust boundaries

Without `allowedPackagePrefixes`, `Jackson3Codec` and `Fastjson2Codec` accept all class names and allow fallback decode. Narrow the prefixes when tenants or services share Redis.

```kotlin
val codec = Jackson3Codec(
    allowedPackagePrefixes = setOf("com.acme.billing."),
)
```

With an allow-list, non-JSON binary fallback is disabled by default and fails with `SecurityException`. Enable `allowFallbackDecode` only for a bounded trusted migration.

## FastFory compatibility is asymmetric

`FastForyCodec` can fall back to the old Fory codec when reading old data. Old `ForyCodec` readers cannot decode new FastFory bytes. Deploy readers that understand the old format first, then switch writers, and remove old readers last. Delay the write-format switch when rollback must remain safe.

## Bound decompression

`GzipCodec` accepts `maxDecompressedSize` and rejects excessive expansion or corrupt gzip data.

```kotlin
val codec = GzipCodec(
    innerCodec = RedissonCodecs.Fory,
    maxDecompressedSize = 16 * 1024 * 1024,
)
```

Set the limit from measured p99 payload size. Do not replace decode failures with empty values.

## Benchmark in context

Codec benchmark results depend on payloads, JVM, CPU, and library versions. Rerun them with domain objects instead of copying a fixed ranking. Wire compatibility and trust boundaries come before throughput.

## Source and tests

- [`RedissonCodecs.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/RedissonCodecs.kt)
- [`FastForyCodec.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCodec.kt)
- [`Jackson3Codec.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Jackson3Codec.kt)
- [`Fastjson2Codec.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Fastjson2Codec.kt)
- [`GzipCodecTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/GzipCodecTest.kt)
- [`FastForyCompatibilityTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCompatibilityTest.kt)
