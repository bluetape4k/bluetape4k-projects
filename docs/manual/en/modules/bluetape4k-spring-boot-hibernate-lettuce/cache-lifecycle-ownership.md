---
title: Cache lifecycle and ownership
description: Separate the lifecycle owned by the Spring customizer, Hibernate SessionFactory, RegionFactory, and Redis client.
manualId: bluetape4k-spring-boot-hibernate-lettuce
chapterId: cache-lifecycle-ownership
---

# Cache lifecycle and ownership

## Auto-configuration does not create resources

`LettuceNearCacheHibernateAutoConfiguration` creates one `HibernatePropertiesCustomizer`. It writes a RegionFactory class name and cache settings into Hibernate's mutable property map. It does not directly create a Redis client or Caffeine cache.

Resource creation begins when Hibernate starts `LettuceNearCacheRegionFactory` while building the `SessionFactory`. The top-level owner is therefore the Hibernate `SessionFactory` lifecycle, not an independent Spring singleton.

## Responsibilities by layer

| Layer | Responsibility |
| --- | --- |
| Spring Boot property binding | Read external settings into typed properties |
| `HibernatePropertiesCustomizer` | Translate Spring names into Hibernate properties |
| Hibernate `SessionFactory` | Start and stop RegionFactory |
| `LettuceNearCacheRegionFactory` | Manage regions, caches, and Lettuce/Redis resources |
| Hibernate access strategy | Order entity and collection reads, writes, and invalidation |
| Application | Own transactions, entity annotations, and operational settings |

Do not independently close a client owned by RegionFactory or create a duplicate client for the same cache lifecycle. Conversely, this module does not close a Lettuce client that the application created for another purpose.

## Entities and regions

Second-level cache is not applied to every entity automatically.

```kotlin
@Entity
@Cacheable
@Cache(
    usage = CacheConcurrencyStrategy.READ_WRITE,
    region = "catalog.product",
)
class Product(/* ... */)
```

Choose the concurrency strategy for update rate and acceptable stale reads. Region names connect TTL, observation, and eviction; keep them stable. Renaming a region creates a new region while old Redis entries may remain until TTL.

## L1 and L2 time boundaries

Caffeine L1 is process-local, while Redis L2 is shared across instances. Even with RESP3 client tracking, account for network loss and reconnect windows. Unless a specific contract says otherwise, keep L1 expiry shorter than or equal to Redis TTL so a local entry does not outlive the intended remote lifetime.

The cache must not expose uncommitted transaction changes. Verify the selected concurrency strategy and Hibernate event ordering around commit and rollback with a real database and Redis integration test.

## Shutdown and upgrades

During normal shutdown, Spring closes JPA `EntityManagerFactory`; Hibernate then closes `SessionFactory` and RegionFactory. A forced shutdown may skip local cleanup callbacks, while Redis TTL still bounds remote entry lifetime.

When changing codec or key format, rolling instances may read the same Redis region with different versions. If formats are incompatible, separate the namespace or region and retire the old cache after its TTL.

## Executable evidence

- [`LettuceNearCacheHibernateAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt)
- [`LettuceNearCacheRegionFactory.kt`](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt)
- [`LettuceNearCacheStorageAccess.kt`](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccess.kt)
- [`LettuceNearCacheIntegrationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheIntegrationTest.kt)
