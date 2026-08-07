---
title: Properties and Hibernate mapping
description: Understand Spring Boot defaults and the duration and per-region TTL conversion into Hibernate properties.
manualId: bluetape4k-spring-boot-hibernate-lettuce
chapterId: properties-and-hibernate-mapping
---

# Properties and Hibernate mapping

## Defaults

`LettuceNearCacheSpringProperties` uses the prefix `bluetape4k.cache.lettuce-near`.

| Spring property | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Enable all auto-configuration |
| `redis-uri` | `redis://localhost:6379` | Redis connection URI |
| `codec` | `lz4fory` | Cache value codec |
| `use-resp3` | `true` | Use RESP3 client tracking |
| `local.max-size` | `10000` | Maximum Caffeine L1 entry count |
| `local.expire-after-write` | `30m` | L1 expiry after write |
| `redis-ttl.default` | `120s` | Default Redis L2 TTL |
| `metrics.enabled` | `true` | Enable Hibernate statistics and binder |
| `metrics.enable-caffeine-stats` | `true` | Record local Caffeine statistics |

The default Redis URI is for development convenience. Set it explicitly in production configuration and review TLS, authentication, and timeout policy with the lower-level Lettuce setup.

## Hibernate property conversion

The customizer always selects `LettuceNearCacheRegionFactory` and enables second-level cache.

| Spring property | Hibernate property |
| --- | --- |
| `redis-uri` | `hibernate.cache.lettuce.redis_uri` |
| `codec` | `hibernate.cache.lettuce.codec` |
| `use-resp3` | `hibernate.cache.lettuce.use_resp3` |
| `local.max-size` | `hibernate.cache.lettuce.local.max_size` |
| `local.expire-after-write` | `hibernate.cache.lettuce.local.expire_after_write` |
| `redis-ttl.default` | `hibernate.cache.lettuce.redis_ttl.default` |
| `redis-ttl.regions[name]` | `hibernate.cache.lettuce.redis_ttl.{name}` |
| `metrics.enabled=true` | `hibernate.generate_statistics=true` |
| `metrics.enable-caffeine-stats=true` | `hibernate.cache.lettuce.local.record_stats=true` |

Disabling metrics omits the related keys rather than writing `false`. Check the final merge order if the application defines the same Hibernate properties elsewhere.

## Preserving durations

A duration divisible by a second becomes `60s`; all others use milliseconds, such as `500ms`.

```yaml
local:
  expire-after-write: 500ms
redis-ttl:
  default: 1500ms
```

The 1.12.1 tests verify that both values remain `500ms` and `1500ms`; sub-second values are not rounded to seconds.

## Per-region TTL

The `regions` map overrides the default TTL.

```yaml
redis-ttl:
  default: 120s
  regions:
    product: 300s
    "[com.example.Order]": 600s
```

Bracket dotted region names so Spring binding does not split them. The generated Hibernate key preserves the dots. The map key must exactly match the entity annotation's region name.

## Review criteria

- L1 capacity counts entries, not bytes.
- L1 expiry and Redis TTL apply to different tiers.
- `use-resp3=true` assumes Redis 6+ and client tracking.
- A codec change may make existing Redis values unreadable; plan cache transition or flush.
- Statistics have a cost; confirm the needed level under load.

## Executable evidence

- [`LettuceNearCacheSpringProperties.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheSpringProperties.kt)
- [`LettuceNearCacheHibernateAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt)
- [`LettuceNearCachePropertiesCustomizerTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCachePropertiesCustomizerTest.kt)
- [`LettuceNearCacheAutoConfigurationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt)
