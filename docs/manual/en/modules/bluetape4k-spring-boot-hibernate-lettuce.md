---
manualId: bluetape4k-spring-boot-hibernate-lettuce
title: "Spring Boot Hibernate Cache Integration"
description: "Configure Hibernate second-level Lettuce Near Cache in Spring Boot 4 with explicit conditions, properties, lifecycle, and operations."
kind: library
group: spring
learningOrder: 950
---

# Spring Boot Hibernate Cache Integration

## Provided capabilities {#problem}

`bluetape4k-spring-boot-hibernate-lettuce` connects Hibernate second-level cache to `LettuceNearCacheRegionFactory` in a Spring Boot 4 application. It maps `application.yml` settings to Hibernate properties and, when the required conditions match, registers an Actuator endpoint and Micrometer gauges.

The module does not implement the cache again. [`bluetape4k-hibernate-cache-lettuce`](./bluetape4k-hibernate-cache-lettuce.md) owns the Caffeine L1 and Redis L2 storage and invalidation behavior. This module owns Spring Boot property binding and auto-configuration.

## Decisions before adoption {#when-to-use}

- Confirm that the application uses Spring Boot 4 and Hibernate ORM.
- Choose the Hibernate second-level cache concurrency strategy and the entities and collections to cache.
- Read the underlying provider contract to decide whether a Redis failure should fail the operation or become a cache miss.
- Set operational values for the Redis URI, codec, L1 capacity, L1 expiry, and Redis TTL.
- Decide whether to expose the Actuator endpoint and collect Micrometer statistics.

Use [`bluetape4k-hibernate-cache-lettuce`](./bluetape4k-hibernate-cache-lettuce.md) directly in a non-Spring Hibernate application. For a general Near Cache without ORM, `bluetape4k-cache-lettuce` is the smaller boundary.

## Add the dependency {#coordinates}

Consumers manage only the `bluetape4k-dependencies` version. Do not pin Spring Boot, Hibernate, or lower-level bluetape4k modules independently.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))

    implementation("io.github.bluetape4k:bluetape4k-spring-boot-hibernate-lettuce")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-hibernate")

    runtimeOnly("org.postgresql:postgresql") // replace with the application driver
}
```

Add `spring-boot-starter-actuator` for the Actuator and Micrometer integration. Most Spring Boot, Hibernate, and Micrometer dependencies of this module are `compileOnly`, so the application must provide the starters for features it uses.

## First auto-configuration {#quick-start}

```yaml
bluetape4k:
  cache:
    lettuce-near:
      redis-uri: redis://localhost:6379
      local:
        max-size: 10000
        expire-after-write: 30m
      redis-ttl:
        default: 120s
      metrics:
        enabled: true
        enable-caffeine-stats: true
```

Mark each cached entity with JPA `@Cacheable` and Hibernate `@Cache`.

```kotlin
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "product")
class Product(
    @Id @GeneratedValue
    var id: Long? = null,
    var name: String = "",
)
```

When enabled, a `HibernatePropertiesCustomizer` passes the RegionFactory, second-level cache, Redis, and L1 settings to Hibernate. Adding the module does not cache an entity that has no cache annotation.

## Guide by task {#api-by-task}

| Task | Start with | Boundary to verify |
| --- | --- | --- |
| Connect Hibernate second-level cache | `LettuceNearCacheHibernateAutoConfiguration` | Classpath and `enabled` conditions, property mapping |
| Define Spring settings | `LettuceNearCacheSpringProperties` | Defaults, durations, and per-region TTL |
| Read all or one region | `LettuceNearCacheActuatorEndpoint` | Endpoint exposure and nullable statistics |
| Register aggregate gauges | `LettuceNearCacheMetricsBinder` | `MeterRegistry` availability and registration failures |
| Discover auto-configuration | `AutoConfiguration.imports` | Registration and conditions of the three configurations |
| Store and invalidate entries | `LettuceNearCacheRegionFactory` | Hibernate and Redis resource lifecycle |

## Learning path {#concepts}

Each chapter combines the concept, configuration examples, common mistakes, and links to the 1.12.1 release source and tests.

1. [Auto-configuration conditions and ordering](./bluetape4k-spring-boot-hibernate-lettuce/auto-configuration-conditions.md) — see exactly when the three configurations register or back off.
2. [Properties and Hibernate mapping](./bluetape4k-spring-boot-hibernate-lettuce/properties-and-hibernate-mapping.md) — understand defaults, duration conversion, and per-region TTL.
3. [Cache lifecycle and ownership](./bluetape4k-spring-boot-hibernate-lettuce/cache-lifecycle-ownership.md) — separate Spring customization, Hibernate RegionFactory, and Redis responsibilities.
4. [Actuator and Micrometer observability](./bluetape4k-spring-boot-hibernate-lettuce/observability-actuator-metrics.md) — interpret endpoint results and the two aggregate gauges.
5. [Testing and failure modes](./bluetape4k-spring-boot-hibernate-lettuce/testing-and-failure-modes.md) — use context tests, Redis integration tests, and degraded observation behavior.
6. [Ecosystem paths](./bluetape4k-spring-boot-hibernate-lettuce/ecosystem-paths.md) — continue into lower-level cache modules, the demo, and Hibernate and Redis references.

For a first adoption, read chapters 1→2→3. Read chapters 4 and 5 together before defining dashboards and failure response.

## Recommended patterns {#patterns}

Treat auto-configuration as an adapter that passes application settings into Hibernate. Choose the concurrency strategy and region names for the domain and read pattern. Keep L1 expiry no longer than the Redis TTL so a local entry does not outlive the intended remote lifetime.

Do not begin with oversized values. Observe per-region hits and misses, local size, and eviction before adjusting L1 capacity and TTL. Cache does not replace database consistency rules; verify transaction commit and invalidation behavior with an integration test.

## Integrations {#integrations}

The module exports `bluetape4k-hibernate-cache-lettuce` as an API dependency and optionally integrates Spring Boot 4 auto-configuration, Hibernate integration and JPA starter, Hibernate ORM, Micrometer, and Actuator. Fory and Zstd are included for serialization support.

In Spring Boot 4, `HibernatePropertiesCustomizer` is under `org.springframework.boot.hibernate.autoconfigure`. Do not copy the retired Spring Boot 3 package or module paths from old documentation.

## Configuration {#configuration}

The main defaults are `enabled=true`, `redis-uri=redis://localhost:6379`, `codec=lz4fory`, `use-resp3=true`, `local.max-size=10000`, `local.expire-after-write=30m`, and `redis-ttl.default=120s`. Metrics and Caffeine statistics are enabled by default.

Durations divisible by a second are passed to Hibernate as `120s`; all others use milliseconds such as `500ms`. Use Spring's bracketed map-key syntax for region names that contain dots.

```yaml
redis-ttl:
  regions:
    "[com.example.Product]": 300s
```

The [properties chapter](./bluetape4k-spring-boot-hibernate-lettuce/properties-and-hibernate-mapping.md) contains the full mapping table.

## Failure behavior {#failures}

With `enabled=false`, the Hibernate customizer and Actuator endpoint are not registered. Disabling only metrics removes the binder and Hibernate statistics settings, while the main cache configuration and endpoint conditions are still evaluated.

The Actuator endpoint does not propagate failures while unwrapping `EntityManagerFactory`, finding the RegionFactory, or reading statistics. It returns an empty map, `null`, or nullable fields. The metrics binder also logs a warning and allows application startup to continue when registration fails. Observation failure therefore does not prove that the cache is healthy; monitor Redis and real entity access separately.

## Operations {#operations}

Observe Redis connectivity and reconnects, L1 hits, misses, and evictions, Hibernate L2 hits, misses, and puts, region count, local entry count, and database query latency together. Add `/actuator/nearcache` to Spring Boot endpoint exposure before using it over HTTP. It returns region statistics rather than cache keys or values, but still requires normal management-network access control.

Do not close the Redis client independently from application code. Hibernate RegionFactory creates and closes the actual clients and caches, so let `SessionFactory` shutdown own that order.

## Testing {#testing}

`ApplicationContextRunner` tests verify conditions and property mapping without Redis or a database. The integration test uses a Redis Testcontainer and H2 to verify a miss→put→hit cycle, endpoint output, gauges, and concurrent reads.

```bash
./gradlew :bluetape4k-spring-boot-hibernate-lettuce:test --no-build-cache --no-configuration-cache
```

Run the Testcontainers-backed suite sequentially with other database and container tests.

## Examples and practice {#workshops}

There is no dedicated workshop, but [`bluetape4k-spring-boot-hibernate-lettuce-demo`](./bluetape4k-spring-boot-hibernate-lettuce-demo.md) is a runnable application example. It connects Product CRUD, cache statistics endpoints, and `application.yml` settings.

A useful progression is to change one property at a time in `LettuceNearCacheAutoConfigurationTest`, then verify the resulting Hibernate statistics in the integration test.

## 1.12.1 scope {#limitations}

This manual describes the `bluetape4k-projects` 1.12.1 release commit and tests. The module is Spring Boot 4 only and does not support Spring Boot 3 package or auto-configuration paths.

Only two gauges are provided: active region count and total local entry count. Read per-region L1/L2 statistics through the Actuator endpoint or Hibernate statistics. Because the endpoint and binder degrade observation failures, an empty value or missing gauge does not identify the backend cause by itself.

## Source and tests {#sources}

- [`LettuceNearCacheHibernateAutoConfiguration.kt`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt)
- [`LettuceNearCacheSpringProperties.kt`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheSpringProperties.kt)
- [`LettuceNearCacheActuatorAutoConfiguration.kt`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorAutoConfiguration.kt)
- [`LettuceNearCacheActuatorEndpoint.kt`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorEndpoint.kt)
- [`LettuceNearCacheMetricsAutoConfiguration.kt`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsAutoConfiguration.kt)
- [`LettuceNearCacheMetricsBinder.kt`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsBinder.kt)
- [`LettuceNearCacheAutoConfigurationTest.kt`](../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt)
- [`LettuceNearCacheIntegrationTest.kt`](../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheIntegrationTest.kt)

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Spring Boot Hibernate Lettuce class structure diagram

[![Spring Boot Hibernate Lettuce class structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-hibernate-lettuce-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-hibernate-lettuce-diagram-01.svg)

_Release README: [`spring-boot/hibernate-lettuce/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/hibernate-lettuce/README.md)_

### Spring Boot Hibernate Lettuce auto-configuration flow diagram

[![Spring Boot Hibernate Lettuce auto-configuration flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-hibernate-lettuce-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-hibernate-lettuce-diagram-02.svg)

_Release README: [`spring-boot/hibernate-lettuce/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/hibernate-lettuce/README.md)_

<!-- release-readme-diagrams:end -->
