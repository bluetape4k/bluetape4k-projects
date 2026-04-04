# bluetape4k-spring-boot4-hibernate-lettuce

English | [한국어](./README.ko.md)

**Spring Boot 4 Auto-Configuration** for Hibernate 7 **2nd Level Cache** (Lettuce Near Cache).

Simply add `bluetape4k.cache.lettuce-near.*` settings to your `application.yml` and Hibernate Second Level Cache activates automatically — no additional code required. Millisecond-based durations (e.g., `500ms`) are passed through directly to Hibernate configuration.

## UML

```mermaid
classDiagram
    class LettuceNearCacheHibernateAutoConfiguration {
        +hibernatePropertiesCustomizer(): HibernatePropertiesCustomizer
    }
    class LettuceNearCacheMetricsAutoConfiguration {
        +lettuceNearCacheMetricsBinder(): LettuceNearCacheMetricsBinder
    }
    class LettuceNearCacheActuatorAutoConfiguration {
        +lettuceNearCacheEndpoint(): LettuceNearCacheEndpoint
    }
    class HibernatePropertiesCustomizer {
        <<interface>>
        +customize(hibernateProperties): void
    }
    class LettuceNearCacheRegionFactory {
        +buildCache(regionName, config): RegionAccessStrategy
        +getL1Cache(region): CaffeineCache
        +getL2Cache(region): RedisCache
    }
    class LettuceNearCacheMetricsBinder {
        +bindTo(registry): void
    }
    class LettuceNearCacheEndpoint {
        +stats(): Map~String, CacheStats~
    }

    LettuceNearCacheHibernateAutoConfiguration --> HibernatePropertiesCustomizer
    HibernatePropertiesCustomizer --> LettuceNearCacheRegionFactory
    LettuceNearCacheMetricsAutoConfiguration --> LettuceNearCacheMetricsBinder
    LettuceNearCacheActuatorAutoConfiguration --> LettuceNearCacheEndpoint
    LettuceNearCacheMetricsBinder --> LettuceNearCacheRegionFactory
    LettuceNearCacheEndpoint --> LettuceNearCacheRegionFactory
```

### Auto-Configuration Activation Flow

```mermaid
flowchart TD
    Props["application.yml<br/>bluetape4k.cache.lettuce-near.*"]
    AutoConfig["Spring Boot 4<br/>Auto Configuration"]
    Customizer["HibernatePropertiesCustomizer"]
    RegionFactory["Lettuce Near Cache<br/>RegionFactory"]
    L1["L1 Cache<br/>Caffeine"]
    L2["L2 Cache<br/>Redis"]
    DB[("Database")]

    Props --> AutoConfig
    AutoConfig --> Customizer
    Customizer --> RegionFactory
    RegionFactory --> L1
    RegionFactory --> L2
    L2 --> DB
```

## Spring Boot 4-Specific Notes

Package names have changed in Spring Boot 4:

| Spring Boot 3 | Spring Boot 4 |
|---|---|
| `org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer` | `org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer` |

The Spring Boot 4 BOM must also be applied explicitly:

```kotlin
// build.gradle.kts
dependencies {
    // Spring Boot 4 BOM (use platform instead of dependencyManagement)
    implementation(platform(Libs.spring_boot4_dependencies))

    implementation(project(":bluetape4k-spring-boot4-hibernate-lettuce"))

    // Hibernate must be declared explicitly
    compileOnly(Libs.springBoot("hibernate"))

    // Spring Boot Starters
    implementation(Libs.springBootStarter("data-jpa"))
    implementation(Libs.springBootStarter("actuator"))   // Actuator endpoint (optional)
    implementation(Libs.micrometer_core)                 // Micrometer metrics (optional)
}
```

## Features

- 2nd Level Cache enabled with just a dependency + `application.yml` configuration
- Safe auto-configuration using `@ConditionalOnClass` / `@ConditionalOnProperty`
- **Actuator** endpoint (`GET /actuator/nearcache`) — per-region cache statistics
- **Micrometer** metrics (`lettuce.nearcache.*`) — region count, local size
- **Two-tier** caching architecture: L1 (Caffeine) + L2 (Redis)

## Dependencies (Spring Boot 4)

```kotlin
// build.gradle.kts
dependencies {
    // Spring Boot 4 BOM (required)
    implementation(platform(Libs.spring_boot4_dependencies))

    implementation(project(":bluetape4k-spring-boot4-hibernate-lettuce"))

    // Spring Boot Starters
    implementation(Libs.springBootStarter("data-jpa"))
    implementation(Libs.springBootStarter("actuator"))   // Actuator endpoint (optional)
    implementation(Libs.micrometer_core)                 // Micrometer metrics (optional)

    // Hibernate (explicit declaration)
    compileOnly(Libs.springBoot("hibernate"))
}
```

## Quick Start

### 1. Add the dependency and configure application.yml

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

spring:
    jpa:
        hibernate:
            ddl-auto: update
    datasource:
        url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1

management:
    endpoints:
        web:
            exposure:
                include: health, info, metrics, nearcache
```

### 2. Annotate your entities for caching

```kotlin
@Entity
@Table(name = "products")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "product")
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @Column
    val description: String? = null,

    @Column(nullable = false)
    val price: Double = 0.0,
)
```

### 3. Run — auto-configuration takes care of the rest

Hibernate properties are injected automatically and 2nd Level Cache is activated. No additional code required.

## Full Configuration Options

```yaml
bluetape4k:
    cache:
        lettuce-near:
            # Enable/disable (default: true)
            enabled: true

            # Redis connection URI
            redis-uri: redis://localhost:6379

            # Serialization codec (default: lz4fory)
            # Options: lz4fory | fory | kryo | lz4kryo | lz4jdk | gzipfory | zstdfory | jdk
            codec: lz4fory

            # Enable RESP3 CLIENT TRACKING (requires Redis 6+, default: true)
            use-resp3: true

            # L1 (Caffeine) settings
            local:
                max-size: 10000                    # Maximum number of entries
                expire-after-write: 30m            # Expire after write

            # Redis TTL
            redis-ttl:
                default: 120s                      # Default TTL
                regions:
                    # Per-region TTL override (use brackets for keys with dots)
                    "[io.bluetape4k.examples.cache.lettuce.domain.Product]": 300s
                    "[io.bluetape4k.examples.cache.lettuce.domain.Order]": 600s

            # Metrics / statistics
            metrics:
                enabled: true                           # Enable metrics collection
                enable-caffeine-stats: true             # Collect Caffeine CacheStats
```

### Configuration → Hibernate properties mapping

| Spring Configuration                   | Hibernate property                                 |
|--------------------------------------|----------------------------------------------------|
| `redis-uri`                          | `hibernate.cache.lettuce.redis_uri`                |
| `codec`                              | `hibernate.cache.lettuce.codec`                    |
| `use-resp3`                          | `hibernate.cache.lettuce.use_resp3`                |
| `local.max-size`                     | `hibernate.cache.lettuce.local.max_size`           |
| `local.expire-after-write`           | `hibernate.cache.lettuce.local.expire_after_write` |
| `redis-ttl.default`                  | `hibernate.cache.lettuce.redis_ttl.default`        |
| `redis-ttl.regions[name]`            | `hibernate.cache.lettuce.redis_ttl.{name}`         |
| `metrics.enabled=true`               | `hibernate.generate_statistics=true`               |
| `metrics.enable-caffeine-stats=true` | `hibernate.cache.lettuce.local.record_stats=true`  |

## Auto-Configuration Classes

| Class                                          | Condition                                                              | Role                                  |
|----------------------------------------------|------------------------------------------------------------------------|---------------------------------------|
| `LettuceNearCacheHibernateAutoConfiguration` | `LettuceNearCacheRegionFactory`, `EntityManagerFactory` on classpath   | Registers `HibernatePropertiesCustomizer` |
| `LettuceNearCacheMetricsAutoConfiguration`   | `MeterRegistry` on classpath + Bean                                    | Registers `LettuceNearCacheMetricsBinder` |
| `LettuceNearCacheActuatorAutoConfiguration`  | `Endpoint` (actuate) on classpath + `EntityManagerFactory` Bean        | Registers `/actuator/nearcache` endpoint  |

## Actuator Endpoint

### Retrieve Statistics for All Regions

```bash
GET /actuator/nearcache
```

Example response:

```json
{
  "product": {
    "regionName": "product",
    "localSize": 850,
    "localHitRate": 0.984,
    "localHitCount": 12453,
    "localMissCount": 203,
    "localEvictionCount": 10,
    "l2HitCount": 12050,
    "l2MissCount": 403,
    "l2PutCount": 1200
  }
}
```

### Retrieve Details for a Specific Region

```bash
GET /actuator/nearcache/{regionName}
```

Example:

```bash
GET /actuator/nearcache/product
```

Response:

```json
{
  "regionName": "product",
  "localSize": 850,
  "localHitRate": 0.984,
  "localHitCount": 12453,
  "localMissCount": 203,
  "localEvictionCount": 10,
  "l2HitCount": 12050,
  "l2MissCount": 403,
  "l2PutCount": 1200
}
```

## Micrometer Metrics

When `metrics.enabled=true`, the following Gauges are registered:

| Metric                             | Description                           |
|----------------------------------|---------------------------------------|
| `lettuce.nearcache.region.count` | Number of active regions              |
| `lettuce.nearcache.local.size`   | Estimated total L1 cache entry count  |

```bash
# Retrieve Micrometer metrics (JSON)
GET /actuator/metrics/lettuce.nearcache.region.count
GET /actuator/metrics/lettuce.nearcache.local.size
```

Example response:

```json
{
  "name": "lettuce.nearcache.region.count",
  "baseUnit": "items",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 2.0
    }
  ]
}
```

## Disabling

To completely disable auto-configuration:

```yaml
bluetape4k:
    cache:
        lettuce-near:
            enabled: false   # Disables HibernatePropertiesCustomizer, MetricsBinder, and Endpoint
```

## Running Tests

### Unit Tests (no Redis/DB required)

```bash
./gradlew :bluetape4k-spring-boot4-hibernate-lettuce:test
```

Uses `ApplicationContextRunner` to test configuration without a real Redis or database instance.

### Integration Tests (Testcontainers)

Integration tests automatically manage Redis + H2 via Testcontainers.

```bash
./gradlew :bluetape4k-spring-boot4-hibernate-lettuce:test -i
```

## Related Modules

- [`bluetape4k-cache-lettuce`](../../infra/cache-lettuce/README.md) — Near Cache core implementation
- [`bluetape4k-hibernate-cache-lettuce`](../../infra/hibernate-cache-lettuce/README.md) — Hibernate Region Factory
- [`bluetape4k-spring-boot4-hibernate-lettuce-demo`](../hibernate-lettuce-demo/README.md) — Usage example

## Differences from Spring Boot 3

| Aspect | Spring Boot 3 | Spring Boot 4 |
|---|---|---|
| `HibernatePropertiesCustomizer` package | `org.springframework.boot.autoconfigure.orm.jpa` | `org.springframework.boot.hibernate.autoconfigure` |
| BOM configuration | `dependencyManagement { imports }` | `implementation(platform(Libs.spring_boot4_dependencies))` |
| Explicit Hibernate dependency | Not required | `compileOnly(Libs.springBoot("hibernate"))` |

## Package Information

- **Group**: `io.github.bluetape4k`
- **Artifact**: `bluetape4k-spring-boot4-hibernate-lettuce`
- **Package**: `io.bluetape4k.spring.boot.autoconfigure.cache.lettuce`

## License

Apache License 2.0
