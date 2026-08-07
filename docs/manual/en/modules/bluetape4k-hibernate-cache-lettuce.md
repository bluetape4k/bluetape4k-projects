---
manualId: bluetape4k-hibernate-cache-lettuce
title: "Hibernate Second-Level Cache with Lettuce"
description: "Run Hibernate second-level caching with Caffeine L1 and Redis L2 while preserving region, TTL, invalidation, and failure boundaries."
kind: library
group: caching
learningOrder: 570
---

# Hibernate Second-Level Cache with Lettuce

## Capabilities {#problem}

`bluetape4k-hibernate-cache-lettuce` connects Hibernate ORM 7.2 second-level caching to a Lettuce Near Cache. Each Hibernate region receives a Caffeine L1 and Redis L2, covering entity, collection, natural-id, query-result, and update-timestamps regions.

The module does not turn cache consistency into a database transaction guarantee. Most cache failures are logged and converted into a database fallback, and failed RESP3 CLIENT TRACKING startup does not stop the cache. Validate invalidation and fallback behavior in addition to hit rate.

## Decisions before adoption {#when-to-use}

- Measure whether repeated load cost exceeds Redis and serialization overhead.
- Select cacheable entities and collections by region.
- Decide whether the short stale window of `NONSTRICT_READ_WRITE` is acceptable.
- Align Caffeine expiry, Redis TTL, and Hibernate query timestamps.
- Confirm that Redis 6+ and RESP3 CLIENT TRACKING are available.
- Define the trust boundary for Redis data and serialization codecs.

Use plain Caffeine for a small single-process cache. Use [bluetape4k-cache-lettuce](./bluetape4k-cache-lettuce.md) when the application needs a direct cache API rather than a Hibernate provider.

## Coordinates {#coordinates}

Consumers manage only the central BOM version, not subordinate cache, Lettuce, and Hibernate versions. The application supplies Redis and its database driver.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-hibernate-cache-lettuce")

    runtimeOnly("org.postgresql:postgresql") // replace with the selected driver
}
```

The 1.12.1 artifact includes Fory and LZ4 runtime support. Review the security and runtime characteristics of Snappy, Zstd, Kryo, and JDK serialization when selecting another codec.

## First second-level cache {#quick-start}

Register the region factory and Redis connection, then annotate each cacheable entity.

```properties
hibernate.cache.use_second_level_cache=true
hibernate.cache.region.factory_class=io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory
hibernate.cache.lettuce.redis_uri=redis://localhost:6379
hibernate.cache.lettuce.codec=lz4fory
hibernate.cache.lettuce.use_resp3=true
hibernate.cache.lettuce.local.max_size=10000
hibernate.cache.lettuce.local.expire_after_write=30m
hibernate.cache.lettuce.redis_ttl.default=120s
```

```kotlin
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Product(
    @Id @GeneratedValue
    var id: Long? = null,
    var name: String = "",
)
```

Read the entity from a new Session when testing second-level behavior; repeated access in one Session primarily proves first-level caching.

## API by task {#api-by-task}

| Task | Start with | Boundary to preserve |
| --- | --- | --- |
| Register the Hibernate provider | `LettuceNearCacheRegionFactory` | SessionFactory owns RedisClient and region-cache lifetime. |
| Parse and validate settings | `LettuceNearCacheProperties` | Invalid codecs, booleans, sizes, and durations fail during startup. |
| Cache entities and collections | `@Cache`, `CacheConcurrencyStrategy` | Test first- and second-level caches separately. |
| Cache query results | `hibernate.cache.use_query_cache`, `setCacheable(true)` | The update-timestamps region has no Redis TTL. |
| Evict keys or regions | `SessionFactory.cache.evict*` | Key and region eviction reach both L1 and L2. |
| Inspect statistics | Hibernate statistics, `getCaches()` | Caffeine stats require `local.record_stats=true`. |
| Integrate Spring Boot | `bluetape4k-spring-boot-hibernate-lettuce` | A separate artifact wires properties, Metrics, and Actuator. |

## Learning path {#concepts}

Each chapter follows the actual 1.12.1 source and tests instead of repeating a property list. It explains setup, region isolation, key digests, query invalidation, Redis failures, and shutdown order with code and failure cases.

1. [Near Cache architecture and regions](./bluetape4k-hibernate-cache-lettuce/architecture-regions.md)
2. [Configuration, codecs, and TTL](./bluetape4k-hibernate-cache-lettuce/configuration-codecs-ttl.md)
3. [Entity, collection, and query caching](./bluetape4k-hibernate-cache-lettuce/entity-query-collection-cache.md)
4. [Keys, concurrency, and invalidation](./bluetape4k-hibernate-cache-lettuce/keys-concurrency-invalidation.md)
5. [Lifecycle, failures, and operations](./bluetape4k-hibernate-cache-lettuce/lifecycle-failures-operations.md)
6. [Spring Boot and ecosystem paths](./bluetape4k-hibernate-cache-lettuce/spring-boot-ecosystem.md)

Read chapters 1 through 4 in order for an initial rollout, then build the operational checklist from chapter 5. Even in Spring Boot, understand the provider contract before adopting chapter 6 automation.

## Recommended pattern {#patterns}

Start with read-heavy data whose short stale window is acceptable. Cache only selected entities and collections, and enable query caching only for genuinely repeated query shapes. Let Hibernate drive eviction after writes instead of modifying Redis directly.

Treat database fallback as the base contract. Redis failure can abruptly move traffic to the database, so alert on pool saturation, query latency, cache misses, and cache errors together.

## Integrations {#integrations}

The module combines `bluetape4k-cache-lettuce`, `bluetape4k-lettuce`, `bluetape4k-io`, and Hibernate ORM. [bluetape4k-spring-boot-hibernate-lettuce](./bluetape4k-spring-boot-hibernate-lettuce.md) maps `bluetape4k.cache.lettuce-near.*` settings to Hibernate and adds Metrics and Actuator integration.

Continue with [bluetape4k-hibernate](./bluetape4k-hibernate.md) for ORM lifecycle helpers or [bluetape4k-lettuce](./bluetape4k-lettuce.md) for direct Redis APIs.

## Configuration {#configuration}

Defaults are Redis at `localhost:6379`, `lz4fory`, 10,000 L1 entries, 30-minute write expiry, 120-second Redis TTL, and RESP3 enabled. Durations accept `ms`, `s`, `m`, and `h`; a value without a suffix is seconds.

`hibernate.cache.lettuce.redis_ttl.<regionName>` overrides the default by region. The `default-update-timestamps-region` always disables Redis TTL to preserve query-cache invalidation.

## Failure behavior {#failures}

Invalid codecs, booleans, non-positive sizes or durations, and blank Redis URIs fail startup with `IllegalArgumentException`. At runtime, `LettuceNearCacheStorageAccess` converts Redis failures during get, put, contains, and eviction into `null`, ignored writes, `false`, and ignored eviction after logging a warning. This favors availability but requires monitoring for database fallback and stale local entries.

Failed RESP3 tracking startup is also warning-only. A running cache is not proof that cross-process L1 invalidation works.

## Operations {#operations}

Observe Hibernate second-level hit, miss, and put counts; query-cache and update-timestamps statistics; region entry counts; and Caffeine statistics. Correlate Redis latency, errors, and connections with database latency and pool saturation. Region-wide eviction uses `SCAN` and `UNLINK`, so measure completion time and Redis load for large regions.

## Testing {#testing}

The 1.12.1 suite uses H2 and Testcontainers Redis 7+ to cover entities, collections, queries, natural and composite identifiers, rollback, concurrent reads, and statistics.

```bash
./gradlew :bluetape4k-hibernate-cache-lettuce:test --no-build-cache --no-configuration-cache
```

Close the Session and read again when asserting a second-level hit.

## Workshops {#workshops}

The [Hibernate Lettuce demo](./bluetape4k-spring-boot-hibernate-lettuce-demo.md) provides a runnable Product entity, Spring Data repository, cache endpoints, and `application.yml`. Inside this module, `HibernateEntityCacheTest`, `HibernateQueryCacheTest`, and `HibernateAdvancedKeyCacheTest` are small executable lessons.

Compare broader cache-aside and read/write-through strategies in `bluetape4k-cache-lettuce` and [exposed-workshop](https://github.com/bluetape4k/exposed-workshop). Hibernate `putIntoCache` is not the same contract as an application repository implementing write-through persistence.

## 1.12.1 scope {#limitations}

This manual targets the `bluetape4k-projects` 1.12.1 release source. The factory returns `NONSTRICT_READ_WRITE` by default, although an entity may request `READ_WRITE`. Prefer the default until distributed soft-lock overhead and eviction behavior have been measured.

CLIENT TRACKING startup failure does not stop the factory. StorageAccess failures also do not fail the database transaction, so the cache must remain a rebuildable acceleration layer rather than a source of truth.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Hibernate Lettuce Near Cache 2-Tier Structure diagram

[![Hibernate Lettuce Near Cache 2-Tier Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-hibernate-cache-lettuce-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-hibernate-cache-lettuce-diagram-01.svg)

_Release README: [`cache/hibernate-cache-lettuce/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/hibernate-cache-lettuce/README.md)_

### Hibernate Lettuce Cache Layer Structure diagram

[![Hibernate Lettuce Cache Layer Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-hibernate-cache-lettuce-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-hibernate-cache-lettuce-diagram-02.svg)

_Release README: [`cache/hibernate-cache-lettuce/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/hibernate-cache-lettuce/README.md)_

### getFromCache / putIntoCache Flow diagram

[![getFromCache / putIntoCache Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-hibernate-cache-lettuce-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-hibernate-cache-lettuce-sequence-01.svg)

_Release README: [`cache/hibernate-cache-lettuce/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/hibernate-cache-lettuce/README.md)_

<!-- release-readme-diagrams:end -->

## Sources and tests {#sources}

- [`LettuceNearCacheProperties.kt`](../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheProperties.kt)
- [`LettuceNearCacheRegionFactory.kt`](../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt)
- [`LettuceNearCacheStorageAccess.kt`](../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccess.kt)
- [`LettuceNearCache.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCache.kt)
- [`LettuceNearCachePropertiesTest.kt`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCachePropertiesTest.kt)
- [`HibernateEntityCacheTest.kt`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateEntityCacheTest.kt)
- [`HibernateAdvancedKeyCacheTest.kt`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateAdvancedKeyCacheTest.kt)
- [`HibernateTransactionRollbackTest.kt`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateTransactionRollbackTest.kt)
