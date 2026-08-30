---
title: Auto-configuration conditions and ordering
description: Verify the 2.0 root, metrics, registry, Actuator, and exposure conditions for every Hibernate-Lettuce auto-configuration phase.
manualId: bluetape4k-spring-boot-hibernate-lettuce
chapterId: auto-configuration-conditions
---

# Auto-configuration conditions and ordering

> Contract scope: **2.0.0 current contract** on `develop`. The stable rollback
> reference remains [1.12.1](https://github.com/bluetape4k/bluetape4k-projects/releases/tag/1.12.1).

## Three configuration classes

`AutoConfiguration.imports` registers three classes:

1. `LettuceNearCacheHibernateAutoConfiguration`
2. `LettuceNearCacheMetricsAutoConfiguration`
3. `LettuceNearCacheActuatorAutoConfiguration`

The first creates a `HibernatePropertiesCustomizer`. The other two add observation after Hibernate setup. Metrics and Actuator also declare ordering after Spring Boot JPA auto-configuration.

## Hibernate configuration conditions

The Hibernate configuration requires `LettuceNearCacheRegionFactory` and `EntityManagerFactory` on the classpath. `bluetape4k.cache.lettuce-near.enabled` must be `true` or absent.

It does not wait for an `EntityManagerFactory` bean because the customizer must contribute properties before that factory is created. `ApplicationContextRunner` proves that `enabled=false` removes the customizer.

```yaml
bluetape4k:
  cache:
    lettuce-near:
      enabled: false
```

## Metrics configuration conditions

The metrics binder requires the RegionFactory, `EntityManagerFactory`, and
`MeterRegistry` classes plus actual `EntityManagerFactory` and `MeterRegistry`
beans. Both `bluetape4k.cache.lettuce-near.enabled` and
`bluetape4k.cache.lettuce-near.metrics.enabled` must be `true` or absent. Root
disable cannot be overridden by enabling metrics.

If the application creates no `MeterRegistry`, the cache may still work while
the binder backs off. That is an optional-feature condition, not a cache
failure. The Actuator dependency separately controls endpoint registration.

## Actuator configuration conditions

The endpoint requires the `Endpoint`, RegionFactory, and `EntityManagerFactory`
classes plus an actual `EntityManagerFactory` bean. It uses the same root and
metrics property gates as the binder, but it does not require a `MeterRegistry`
bean. Disabling either property removes the endpoint bean.

HTTP exposure remains separate from bean registration. A registered endpoint is
reachable over HTTP only when `management.endpoints.web.exposure.include`
contains `nearcache` or an equivalent exposure rule applies.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,nearcache
```

## Activation matrix

The matrix assumes that the classpath conditions described above are satisfied.

| Root `enabled` | `metrics.enabled` | `EntityManagerFactory` | `MeterRegistry` | Actuator | Result |
| --- | --- | --- | --- | --- | --- |
| `false` | any | any | any | any | No customizer, binder, or endpoint bean |
| `true` | `false` | present | present or absent | present or absent | Hibernate customizer only |
| `true` | `true` | absent | any | any | No binder or endpoint bean |
| `true` | `true` | present | absent | present | Endpoint bean only; no Micrometer binder |
| `true` | `true` | present | present | absent | Metrics binder only; no endpoint bean |
| `true` | `true` | present | present | present | Binder and endpoint bean; HTTP still needs exposure |

## Diagnostic order

1. Find all three names in the Spring Boot condition evaluation report.
2. Check `bluetape4k.cache.lettuce-near.enabled` before
   `bluetape4k.cache.lettuce-near.metrics.enabled`.
3. Verify runtime presence of `spring-boot-hibernate`, the JPA starter, and Actuator.
4. Check the `EntityManagerFactory` and `MeterRegistry` beans.
5. If the endpoint bean exists but HTTP does not, check
   `management.endpoints.web.exposure.include`.

## Executable evidence

- [`AutoConfiguration.imports`](../../../../../spring-boot/hibernate-lettuce/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
- [`LettuceNearCacheHibernateAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt)
- [`LettuceNearCacheMetricsAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsAutoConfiguration.kt)
- [`LettuceNearCacheActuatorAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorAutoConfiguration.kt)
- [`LettuceNearCacheAutoConfigurationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt)
