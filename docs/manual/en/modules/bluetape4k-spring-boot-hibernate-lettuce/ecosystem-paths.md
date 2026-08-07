---
title: Ecosystem paths
description: Continue from Spring Boot auto-configuration to the lower cache provider, demo, and Hibernate and Redis operations references.
manualId: bluetape4k-spring-boot-hibernate-lettuce
chapterId: ecosystem-paths
---

# Ecosystem paths

## Position of this module

This module is an adapter between Spring Boot and the Hibernate cache provider.

```text
Spring Boot properties / conditions
        ↓
bluetape4k-spring-boot-hibernate-lettuce
        ↓ Hibernate properties
bluetape4k-hibernate-cache-lettuce
        ↓
bluetape4k-cache-lettuce + Redis/Lettuce
```

The right source and tests depend on which layer owns the problem.

## Move down to cache implementation

Read the [`bluetape4k-hibernate-cache-lettuce`](../bluetape4k-hibernate-cache-lettuce.md) manual for region creation, keys, serialization, Caffeine L1, Redis L2, and invalidation. That module is also the entry point for configuring Hibernate properties without Spring.

For a general coroutine Near Cache and Lettuce client API, inspect `bluetape4k-cache-lettuce` and `bluetape4k-lettuce` without the Hibernate adapter. Do not add ORM to a cache with no entity regions or transaction lifecycle.

## Runnable Spring Boot example

[`bluetape4k-spring-boot-hibernate-lettuce-demo`](../bluetape4k-spring-boot-hibernate-lettuce-demo.md) connects a Product entity, Spring Data repository, CRUD controller, cache statistics endpoints, and a real `application.yml`.

A practical adoption path is:

1. Read the demo settings and entity annotations.
2. Validate application properties with this module's context tests.
3. Verify miss→put→hit with a real Redis and database.
4. Expose Actuator and Micrometer dashboards only through the intended operations boundary.

## Return to the persistence choice

Second-level cache reduces selected ORM read costs; it is not a reason by itself to choose ORM. Review entity lifecycle, lazy loading, and transactions in the [`bluetape4k-hibernate`](../bluetape4k-hibernate.md) manual first.

Compare [`bluetape4k-jdbc`](../bluetape4k-jdbc.md) for direct SQL control and [`bluetape4k-r2dbc`](../bluetape4k-r2dbc.md) for non-blocking drivers. For a Kotlin SQL DSL and repository ecosystem, continue to the [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed) repository. Hibernate second-level cache does not transfer unchanged to these choices.

## Official references

- [Spring Boot reference documentation](https://docs.spring.io/spring-boot/reference/)
- [Spring Boot Actuator endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)
- [Hibernate ORM caching](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#caching)
- [Redis client-side caching](https://redis.io/docs/latest/develop/reference/client-side-caching/)
- [Lettuce reference guide](https://redis.github.io/lettuce/)

External documentation follows its current release and can change. Use the release source and tests below for 1.12.1 behavior.

## 1.12.1 source map

- [Spring Boot adapter source](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt)
- [Hibernate cache provider source](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt)
- [Spring Boot demo manual](../bluetape4k-spring-boot-hibernate-lettuce-demo.md)
- [`LettuceNearCacheIntegrationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheIntegrationTest.kt)
