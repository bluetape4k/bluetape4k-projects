# bluetape4k-spring-boot3-hibernate-lettuce-demo

English | [한국어](./README.ko.md)

A demo application for Spring Boot 3 + Hibernate 7 **2nd Level Cache (2LC)** with **Lettuce Near Cache**.

This example demonstrates how to activate Hibernate 2nd Level Cache with zero code using the auto-configuration from the
`bluetape4k-spring-boot3-hibernate-lettuce` module.

## Architecture

```
Client HTTP Request
       ↓
ProductController (REST API)
       ↓
ProductRepository (Spring Data JPA)
       ↓
Hibernate Session Factory
       ↓
┌─────────────────────────────────────────────┐
│ Lettuce Near Cache Region Factory           │
├─────────────────────────────────────────────┤
│ L1 Cache (Caffeine)                         │
│ ├─ maxSize: 10,000 items                    │
│ ├─ expireAfterWrite: 30m                    │
│ └─ localStats() ← Metrics/Actuator          │
├─────────────────────────────────────────────┤
│ L2 Cache (Redis)                            │
│ ├─ TTL: 120s (default)                      │
│ ├─ RESP3 CLIENT TRACKING                    │
│ ├─ Codec: LZ4+Fory                          │
│ └─ Per-region TTL configurable              │
└─────────────────────────────────────────────┘
       ↓
H2 Database
```

```mermaid
classDiagram
    class ProductController {
        -productRepository: ProductRepository
        +findAll(): List~Product~
        +findById(id): ResponseEntity~Product~
        +create(product): Product
        +update(id, product): Product
        +delete(id): ResponseEntity~Void~
    }
    class CacheController {
        +stats(): Map
        +evictAll(): ResponseEntity~Void~
        +evict(region): ResponseEntity~Void~
    }
    class ProductRepository {
        <<interface>>
        +findAll(): List~Product~
        +findById(id): Optional~Product~
        +save(product): Product
        +delete(product)
    }
    class Product {
        +id: Long?
        +name: String
        +description: String?
        +price: Double
    }
    class LettuceNearCacheRegionFactory {
        +getL1Cache(region): CaffeineCache
        +getL2Cache(region): RedisCache
        +getStats(region): RegionStats
    }

    ProductController --> ProductRepository
    ProductRepository --> Product
    Product --> LettuceNearCacheRegionFactory : cached by
    CacheController --> LettuceNearCacheRegionFactory

    style ProductController fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    style CacheController fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    style ProductRepository fill:#E0F7FA,stroke:#80DEEA,color:#00695C
    style Product fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    style LettuceNearCacheRegionFactory fill:#E0F2F1,stroke:#80CBC4,color:#00695C
```

### Request Flow Diagram

```mermaid
flowchart TD
    Client["Client HTTP Request"]
    Controller["ProductController"]
    Repo["ProductRepository"]
    Hibernate["Hibernate Session Factory"]
    RegionFactory["Lettuce Near Cache<br/>Region Factory"]
    L1["L1 Cache<br/>Caffeine"]
    L2["L2 Cache<br/>Redis"]
    DB[("H2 Database")]

    Client --> Controller --> Repo --> Hibernate --> RegionFactory
    RegionFactory --> L1
    RegionFactory --> L2
    Repo --> DB

    classDef extStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef springStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef dataStyle fill:#F57F17,stroke:#E65100,color:#000000

    class Client extStyle
    class Controller extStyle
    class Repo serviceStyle
    class Hibernate springStyle
    class RegionFactory coreStyle
    class L1 extStyle
    class L2 extStyle
    class DB dataStyle
```

## Domain Model

### Product Entity

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

- `@Cacheable`: Enables JPA 2nd Level Cache
- `@Cache(NONSTRICT_READ_WRITE)`: Optimistic lock-based update strategy
- `region = "product"`: Redis key prefix (`product::*`)

## REST API

### Product API (`/api/products`)

| Method   | Path                 | Description           | Cache Behavior       |
|----------|----------------------|-----------------------|----------------------|
| `GET`    | `/api/products`      | Retrieve all products | Not cached           |
| `GET`    | `/api/products/{id}` | Retrieve by ID        | L1/L2 Hit/Miss       |
| `POST`   | `/api/products`      | Create a product      | Stored in L1 + L2    |
| `PUT`    | `/api/products/{id}` | Update a product      | L1 + L2 updated      |
| `DELETE` | `/api/products/{id}` | Delete a product      | Removed from L1 + L2 |

#### Example: Retrieve a product (cache in action)

```bash
# First request: fetched from DB (L1 Miss, L2 Miss)
curl http://localhost:8080/api/products/1

# Response (200 OK)
{
  "id": 1,
  "name": "Laptop",
  "description": "High-performance laptop",
  "price": 999.99
}

# Second request: served immediately from L1 cache (L1 Hit)
curl http://localhost:8080/api/products/1
```

#### Example: Create a product

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mouse",
    "description": "Wireless mouse",
    "price": 29.99
  }'

# Response (200 OK) - automatically stored in L1 + L2 cache
{
  "id": 2,
  "name": "Mouse",
  "description": "Wireless mouse",
  "price": 29.99
}
```

#### Example: Update a product (cache refresh)

```bash
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Laptop",
    "description": "Ultra-fast gaming laptop",
    "price": 1299.99,
    "id": 1
  }'

# Response (200 OK) - both L1 + L2 cache updated
{
  "id": 1,
  "name": "Gaming Laptop",
  "description": "Ultra-fast gaming laptop",
  "price": 1299.99
}
```

#### Example: Delete a product (cache eviction)

```bash
curl -X DELETE http://localhost:8080/api/products/1

# Response (204 No Content) - removed from both L1 + L2 cache
```

### Cache Management API (`/api/cache`)

| Method   | Path                        | Description                  | Behavior                               |
|----------|-----------------------------|------------------------------|----------------------------------------|
| `GET`    | `/api/cache/stats`          | Per-region cache statistics  | Returns L1 size, hit/miss counts       |
| `DELETE` | `/api/cache/evict`          | Evict all region L1 caches   | L1 only removed (L2 retained)          |
| `DELETE` | `/api/cache/evict/{region}` | Evict a specific region's L1 | That region's L1 removed (L2 retained) |

#### Example: Get cache statistics

```bash
curl http://localhost:8080/api/cache/stats

# Response (200 OK)
{
  "product": {
    "regionName": "product",
    "localSize": 15,
    "localHitCount": 245,
    "localMissCount": 18,
    "localHitRate": 0.931
  }
}
```

#### Example: Evict a specific region's L1 cache

```bash
# Evict only L1 (Caffeine) cache; Redis L2 is retained
curl -X DELETE http://localhost:8080/api/cache/evict/product

# Response (204 No Content)
```

#### Example: Evict all L1 caches

```bash
curl -X DELETE http://localhost:8080/api/cache/evict

# Response (204 No Content)
```

> **Note**: These endpoints only evict L1 (Caffeine). Redis L2 is not affected.

### Actuator Endpoints

The `/actuator/nearcache` endpoint provided by Spring Boot Actuator.

#### All Region Statistics

```bash
curl http://localhost:8080/actuator/nearcache

# Response (200 OK)
{
  "product": {
    "regionName": "product",
    "localSize": 42,
    "localHitRate": 0.977,
    "localHitCount": 1830,
    "localMissCount": 42,
    "localEvictionCount": 5,
    "l2HitCount": 1750,
    "l2MissCount": 80,
    "l2PutCount": 120
  }
}
```

#### Specific Region Details

```bash
curl http://localhost:8080/actuator/nearcache/product

# Response (200 OK)
{
  "regionName": "product",
  "localSize": 42,
  "localHitRate": 0.977,
  "localHitCount": 1830,
  "localMissCount": 42,
  "localEvictionCount": 5,
  "l2HitCount": 1750,
  "l2MissCount": 80,
  "l2PutCount": 120
}
```

## Application Configuration

### application.yml

```yaml
spring:
  application:
    name: hibernat-lettuce-demo

  datasource:
    url: jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1;MODE=MySQL
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        cache:
          use_second_level_cache: true

bluetape4k:
  cache:
    lettuce-near:
      redis-uri: redis://localhost:6379
      codec: lz4fory                            # LZ4 compression + Fory serialization
      use-resp3: true                           # RESP3 + CLIENT TRACKING
      local:
        max-size: 10000
        expire-after-write: 30m
      redis-ttl:
        default: 120s
        regions:
          product: 300s                         # product region TTL: 5 minutes
      metrics:
        enabled: true
        enable-caffeine-stats: true

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, actuator, nearcache
  endpoint:
    health:
      show-details: always
```

## Running Tests

### Unit Tests

```bash
./gradlew :bluetape4k-spring-boot3-hibernate-lettuce-demo:test
```

Tests use Testcontainers to manage Redis automatically.

### Test Example

```kotlin
@SpringBootTest
class DemoApplicationTest {

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @BeforeEach
    fun setUp() {
        val sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor::class.java)
        sessionFactory.statistics.clear()
    }

    @Test
    fun `product lookup uses 2LC cache`() {
        // Given
        val product = Product(name = "Laptop", price = 999.99)
        val saved = productRepository.save(product)

        // When (first lookup)
        val result1 = productRepository.findById(saved.id!!).get()

        // Then (fetched from DB)
        assertThat(result1.name).isEqualTo("Laptop")

        // When (second lookup)
        val result2 = productRepository.findById(saved.id).get()

        // Then (served from L1 cache — no DB query)
        assertThat(result2.name).isEqualTo("Laptop")
    }
}
```

## Test Coverage

- `ProductControllerTest`: REST API endpoint tests
- `CacheControllerTest`: Cache management API tests
- `CachingIntegrationTest`: 2LC + Lettuce Near Cache integration tests
- `LettuceNearCacheStatsTest`: Actuator endpoint tests

## Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":bluetape4k-spring-boot3-hibernate-lettuce"))
    implementation(Libs.springBootStarter("web"))
    implementation(Libs.springBootStarter("data-jpa"))
    implementation(Libs.springBootStarter("actuator"))
    runtimeOnly(Libs.h2_database)

    testImplementation(Libs.springBootStarter("test"))
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_junit5)
}
```

## Package Information

- **Group**: `io.github.bluetape4k`
- **Artifact**: `bluetape4k-spring-boot3-hibernate-lettuce-demo`
- **Package**: `io.bluetape4k.examples.cache.lettuce`

## Related Modules

- [`bluetape4k-spring-boot3-hibernate-lettuce`](../hibernate-lettuce/README.md) — Auto-Configuration module
- [`bluetape4k-hibernate-cache-lettuce`](../../infra/hibernate-cache-lettuce/README.md) — Hibernate Region Factory
- [`bluetape4k-cache-lettuce`](../../infra/cache-lettuce/README.md) — Near Cache core

## License

Apache License 2.0
