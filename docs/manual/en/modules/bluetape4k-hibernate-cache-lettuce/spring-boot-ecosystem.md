---
title: Spring Boot and ecosystem paths
description: Adopt Spring Boot auto-configuration and demos, then choose Hibernate, Lettuce, or Exposed cache learning paths.
manualId: bluetape4k-hibernate-cache-lettuce
chapterId: spring-boot-ecosystem
---

# Spring Boot and ecosystem paths

## Separate the provider from auto-configuration

This module contains the Hibernate RegionFactory. Add the separate Spring Boot artifact for property binding, `HibernatePropertiesCustomizer`, Micrometer, and Actuator.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-hibernate-lettuce")
}
```

The Boot module exposes the core provider as an API dependency; consumers still manage only the central BOM.

```yaml
bluetape4k:
  cache:
    lettuce-near:
      redis-uri: redis://localhost:6379
      codec: lz4fory
      use-resp3: true
      local:
        max-size: 10000
        expire-after-write: 30m
      redis-ttl:
        default: 120s
        regions:
          io.example.Product: 300s
      metrics:
        enabled: true
        enable-caffeine-stats: true
```

Auto-configuration maps these values to `hibernate.cache.lettuce.*` and enables second-level caching. `enabled=false` backs it off. Metrics also enables Hibernate statistics, so measure its production cost.

## Runnable demo

[bluetape4k-spring-boot-hibernate-lettuce-demo](../bluetape4k-spring-boot-hibernate-lettuce-demo.md) contains a Product entity, Spring Data repository, cache controller, and real configuration. Verify the provider contract with core tests, then use the demo to connect HTTP requests to cache statistics.

```bash
./gradlew :bluetape4k-spring-boot-hibernate-lettuce-demo:bootRun
```

Do not copy demo settings directly into production. Redis authentication and TLS, database migration, TTL, pools, and observability remain deployment decisions.

## Choose the next path

| Learning goal | Continue with | Why |
| --- | --- | --- |
| Entity and transaction basics | [bluetape4k-hibernate](../bluetape4k-hibernate.md) | Understand ORM lifecycle before caching it. |
| Direct Near Cache use | [bluetape4k-cache-lettuce](../bluetape4k-cache-lettuce.md) | Use cache APIs outside Hibernate regions. |
| Redis commands and codecs | [bluetape4k-lettuce](../bluetape4k-lettuce.md) | Inspect the Lettuce client and serialization layer. |
| Boot Metrics and Actuator | [Spring Boot Hibernate Lettuce](../bluetape4k-spring-boot-hibernate-lettuce.md) | Add auto-configuration and operational endpoints. |
| Repository cache strategies | [exposed-workshop](https://github.com/bluetape4k/exposed-workshop) | Compare cache-aside and read/write-through around repositories. |

Hibernate second-level caching owns entity state and query results inside the Hibernate lifecycle. The Exposed workshop's `JdbcCacheRepository`, `EntityMapLoader`, and `EntityMapWriter` instead put read-through, write-through, or write-behind ownership in an application repository. These are different ownership models, not interchangeable labels.

## Sources and links

- [`LettuceNearCacheHibernateAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt)
- [`LettuceNearCacheSpringProperties.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheSpringProperties.kt)
- [Demo `application.yml`](../../../../../spring-boot/hibernate-lettuce-demo/src/main/resources/application.yml)
- [bluetape4k-exposed repository](https://github.com/bluetape4k/bluetape4k-exposed)
- [exposed-workshop](https://github.com/bluetape4k/exposed-workshop)
