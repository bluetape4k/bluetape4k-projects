---
title: Configuration, codecs, and TTL
description: Configure Hibernate properties, duration parsing, region TTLs, and serialization trust boundaries.
manualId: bluetape4k-hibernate-cache-lettuce
chapterId: configuration-codecs-ttl
---

# Configuration, codecs, and TTL

## Validate settings at startup

Provider settings use the `hibernate.cache.lettuce.` prefix.

| Setting | Default | Meaning |
| --- | --- | --- |
| `redis_uri` | `redis://localhost:6379` | Redis endpoint |
| `codec` | `lz4fory` | Value serialization and compression |
| `use_resp3` | `true` | RESP3 for CLIENT TRACKING |
| `local.max_size` | `10000` | L1 entries per region |
| `local.expire_after_write` | `30m` | L1 write expiry |
| `redis_ttl.default` | `120s` | Default L2 TTL |
| `redis_ttl.<region>` | none | Region TTL override |
| `local.record_stats` | `false` | Record Caffeine statistics |

Booleans accept only `true` or `false`. Durations support `500ms`, `90s`, `15m`, and `2h`; no suffix means seconds. Invalid text and non-positive sizes or durations fail before cache creation.

## Region TTLs

```properties
hibernate.cache.lettuce.redis_ttl.default=120s
hibernate.cache.lettuce.redis_ttl.io.example.Product=300s
hibernate.cache.lettuce.redis_ttl.io.example.Order=600s
```

A region override wins over the default. L1 expiry and Redis TTL are independent: an L1 miss may refill from Redis, while an L2 miss continues to the database.

The `default-update-timestamps-region` always has a null Redis TTL. Expiring timestamps before cached query results would undermine stale-result detection.

## Fifteen codecs

Supported names are `jdk`, `kryo`, and `fory`, plus `gzip`, `lz4`, `snappy`, and `zstd` combinations. Names are case-insensitive, but typos never fall back silently.

Compression trades CPU for payload size. Measure representative entity sizes and latency rather than assuming compressed codecs are faster.

## Serialization trust boundary

Kryo and Fory reconstruct objects from Redis bytes. Isolate the Redis network, require authentication and ACLs, and do not share arbitrary write access with untrusted applications.

JDK serialization is not an allowlist-based Jackson serializer. A misleading 1.12.1 KDoc phrase associates `jdk` with Jackson, but the actual implementation calls `LettuceBinaryCodecs.jdk()`; this manual follows the code.

## Sources and tests

- [`LettuceNearCacheProperties.kt`](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheProperties.kt)
- [`LettuceNearCachePropertiesTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCachePropertiesTest.kt)
- [`LettuceBinaryCodecs.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecs.kt)
