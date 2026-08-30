---
title: Actuator and Micrometer observability
description: Interpret the nearcache endpoint, aggregate gauges, nullable values, and operational dashboard signals.
manualId: bluetape4k-spring-boot-hibernate-lettuce
chapterId: observability-actuator-metrics
---

# Actuator and Micrometer observability

> Contract scope: **2.0.0 current contract** on `develop`. The stable rollback
> reference remains [1.12.1](https://github.com/bluetape4k/bluetape4k-projects/releases/tag/1.12.1).

## Enable the endpoint

The `nearcache` endpoint bean is registered when both
`bluetape4k.cache.lettuce-near.enabled` and
`bluetape4k.cache.lettuce-near.metrics.enabled` are enabled, Actuator is on the
classpath, and an `EntityManagerFactory` bean exists. The endpoint does not
require a `MeterRegistry` bean. Add it to
`management.endpoints.web.exposure.include` for HTTP access.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,nearcache
```

```text
GET /actuator/nearcache
GET /actuator/nearcache/{regionName}
```

The first returns `Map<String, RegionStats>`. A missing individual region returns `null`.

## Read RegionStats

| Field | Source | Nullable behavior |
| --- | --- | --- |
| `regionName` | RegionFactory | Never null |
| `localSize` | Caffeine local cache | `0` if no cache is available |
| Local hit rate, hit/miss/eviction | Caffeine stats | `null` when local stats are disabled |
| L2 hit/miss/put | Hibernate statistics | `null` when statistics are off or lookup fails |

Distinguish `0` from `null`: zero may be a measured value, while null means that statistic was not collected.

The endpoint wraps factory unwrapping and statistics lookup in `runCatching`. A different RegionFactory or an unwrap failure yields an empty map or `null`. Endpoint output alone does not prove backend health.

## Micrometer gauges

The binder uses the same root and metrics property gates, but additionally
requires actual `EntityManagerFactory` and `MeterRegistry` beans. Without a
registry the endpoint may still exist while the gauges remain absent. After
singleton initialization, the binder registers:

- `lettuce.nearcache.active.regions`: current RegionFactory cache-map size
- `lettuce.nearcache.total.local.size`: total local entries across all regions

The gauges read current RegionFactory state when sampled, so new regions affect the value. They do not provide per-region hit rate or Redis latency.

```text
GET /actuator/metrics/lettuce.nearcache.active.regions
GET /actuator/metrics/lettuce.nearcache.total.local.size
```

## Dashboard signals

Aggregate gauges can hide a hot region's eviction spike. Combine them with:

- active regions and local size per application instance
- Hibernate L2 hits, misses, and puts per region
- Redis command latency, errors, and reconnects
- database query count and latency
- cache configuration and deployment events

Local eviction cleanup may be asynchronous. The current integration test
therefore checks a non-negative local size rather than an exact instantaneous
count. Use trends and correlation with database load.

## Registration failures

The binder logs at debug and registers nothing when a different RegionFactory is active. Unwrap or gauge-registration failures produce a warning but do not fail startup. Alert separately on missing observation and cache-backend failures so operators can distinguish the cause.

## Executable evidence

- [`LettuceNearCacheActuatorEndpoint.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorEndpoint.kt)
- [`LettuceNearCacheActuatorAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorAutoConfiguration.kt)
- [`LettuceNearCacheMetricsBinder.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsBinder.kt)
- [`LettuceNearCacheMetricsAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsAutoConfiguration.kt)
- [`LettuceNearCacheIntegrationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheIntegrationTest.kt)
