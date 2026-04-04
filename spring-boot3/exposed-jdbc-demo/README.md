# bluetape4k-spring-boot3-exposed-jdbc-demo

English | [한국어](./README.ko.md)

Exposed DAO + Spring Data JDBC Repository + Spring MVC Integration Demo (Spring Boot 3.x)

## Overview

This module demonstrates the basic pattern of wrapping **Exposed DAO entities** in a Spring Data JDBC Repository and exposing them through a Spring MVC REST API.

## UML

```mermaid
classDiagram
    class ProductController {
        -productJdbcRepository: ProductJdbcRepository
        +findAll(): List~ProductDto~
        +findById(id): ResponseEntity~ProductDto~
        +create(dto): ResponseEntity~ProductDto~
        +update(id, dto): ResponseEntity~ProductDto~
        +delete(id): ResponseEntity~Void~
        +search(name): List~ProductDto~
    }
    class ProductJdbcRepository {
        <<interface>>
        +findByName(name): List~ProductEntity~
        +findByPriceLessThan(price): List~ProductEntity~
    }
    class ExposedJdbcRepository {
        <<interface>>
        +save(entity): E
        +findById(id): Optional~E~
        +findAll(): List~E~
        +delete(entity)
    }
    class ProductEntity {
        +id: EntityID~Long~
        +name: String
        +price: BigDecimal
        +stock: Int
        +toDto(): ProductDto
    }
    class Products {
        <<object>>
        +name: Column~String~
        +price: Column~BigDecimal~
        +stock: Column~Int~
    }
    class DataInitializer {
        +initialize()
    }

    ProductController --> ProductJdbcRepository
    ProductJdbcRepository --|> ExposedJdbcRepository
    ProductJdbcRepository --> ProductEntity
    ProductEntity --> Products : mapped by
    DataInitializer --> ProductJdbcRepository
```

### Application Flow Diagram

```mermaid
flowchart TD
    App["DemoApplication"]
    Init["DataInitializer"]
    Controller["ProductController"]
    Service["transaction { ... }"]
    Repo["ProductJdbcRepository"]
    Entity["ProductEntity / Products"]
    DB[("H2 / JDBC DB")]

    App --> Init
    App --> Controller
    Controller --> Service
    Service --> Repo
    Repo --> Entity
    Entity --> DB
```

### Key Characteristics

- **Exposed DAO entity-based**: `ProductEntity` and `Products` table definitions
- **Spring Data JDBC Repository**: Implements `ExposedJdbcRepository<E, ID>`
- **Query methods**: `findByName` and `findByPriceLessThan` auto-generated
- **Spring MVC REST API**: Standard CRUD endpoints
- **Transaction boundary**: A single `transaction {}` block per request handles both DAO and DTO conversion
- **Automatic schema creation**: Tables are created automatically on application startup

## Project Structure

```
src/main/kotlin/io/bluetape4k/examples/exposed/mvc/
├── DemoApplication.kt              # Spring Boot application
├── domain/
│   └── ProductEntity.kt            # Exposed DAO entity + DTO
├── repository/
│   └── ProductJdbcRepository.kt     # Spring Data JDBC Repository
├── controller/
│   └── ProductController.kt         # REST API controller
└── config/
    └── DataInitializer.kt           # Initial data loader
```

## Domain Model

### ProductEntity (Exposed DAO)

```kotlin
object Products : LongIdTable("products") {
    val name = varchar("name", 255)
    val price = decimal("price", 10, 2)
    val stock = integer("stock").default(0)
}

@ExposedEntity
class ProductEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ProductEntity>(Products)
    var name: String by Products.name
    var price: java.math.BigDecimal by Products.price
    var stock: Int by Products.stock
}
```

### ProductDto (Transfer Object)

```kotlin
data class ProductDto(
    val id: Long? = null,
    val name: String,
    val price: java.math.BigDecimal,
    val stock: Int = 0,
)

fun ProductEntity.toDto() = ProductDto(id.value, name, price, stock)
```

## Repository

### ExposedJdbcRepository Implementation

```kotlin
interface ProductJdbcRepository: ExposedJdbcRepository<ProductEntity, Long> {
    fun findByName(name: String): List<ProductEntity>
    fun findByPriceLessThan(price: java.math.BigDecimal): List<ProductEntity>
}
```

`ExposedJdbcRepository` automatically generates PartTree queries. No additional implementation is needed as long as the method naming conventions are followed.

## REST API

### Basic CRUD

| Method | Path | Description |
|--------|------|-------------|
| GET | `/products` | Retrieve all products |
| GET | `/products/{id}` | Retrieve a specific product |
| POST | `/products` | Create a product |
| PUT | `/products/{id}` | Update a product |
| DELETE | `/products/{id}` | Delete a product |
| GET | `/products/search` | Search by name (query parameter `name`) |

### Request/Response Examples

**Retrieve all products**

```bash
curl http://localhost:8080/products
```

Response:
```json
[
  {
    "id": 1,
    "name": "Kotlin Programming Book",
    "price": 39.99,
    "stock": 100
  },
  {
    "id": 2,
    "name": "Spring Boot Guide",
    "price": 49.99,
    "stock": 50
  }
]
```

**Create a product**

```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Exposed ORM Tutorial",
    "price": 29.99,
    "stock": 200
  }'
```

Response (201 Created):
```json
{
  "id": 3,
  "name": "Exposed ORM Tutorial",
  "price": 29.99,
  "stock": 200
}
```

**Update a product**

```bash
curl -X PUT http://localhost:8080/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Advanced Kotlin",
    "price": 49.99,
    "stock": 150
  }'
```

**Delete a product**

```bash
curl -X DELETE http://localhost:8080/products/1
```

**Search by name**

```bash
curl "http://localhost:8080/products/search?name=Kotlin"
```

## How to Run

### Prerequisites

- Java 21+
- Gradle 8.x+
- Spring Boot 3.4+

### Build

```bash
./gradlew :spring-boot3:exposed-jdbc-demo:build
```

### Run the Application

```bash
./gradlew :spring-boot3:exposed-jdbc-demo:bootRun
```

Or run as a JAR:

```bash
./gradlew :spring-boot3:exposed-jdbc-demo:assemble
java -jar spring-boot3/exposed-jdbc-demo/build/libs/exposed-spring-data-mvc-demo-*.jar
```

### Default Port

The application starts on port `8080` by default.

### Seed Data

When the application starts, three sample products are created automatically:

```
1. Kotlin Programming Book - $39.99 (100 in stock)
2. Spring Boot Guide - $49.99 (50 in stock)
3. Exposed ORM Tutorial - $29.99 (200 in stock)
```

## Database

By default, an **H2 in-memory database** is used. This can be changed in `application.yml`.

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:mvcdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  exposed:
    generate-ddl: true
```

### Switching to PostgreSQL

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/exposed_demo
    driver-class-name: org.postgresql.Driver
    username: postgres
    password: password
```

And in `build.gradle.kts`:

```kotlin
runtimeOnly("org.postgresql:postgresql")
```

## Testing

### Run Unit Tests

```bash
./gradlew :spring-boot3:exposed-jdbc-demo:test
```

### Integration Tests

See `ProductJdbcRepositoryTest` and `ProductControllerTest`.

```bash
./gradlew :spring-boot3:exposed-jdbc-demo:test --tests "ProductControllerTest"
```

## Key Patterns

### Transaction Boundary

All controller methods run inside a `transaction {}` block to keep DAO entities alive.

```kotlin
@GetMapping("/{id}")
fun findById(@PathVariable id: Long): ResponseEntity<ProductDto> {
    val entity = transaction {
        productJdbcRepository.findById(id).orElse(null)?.toDto()
    }
    return entity?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
}
```

### DAO-to-DTO Conversion

Entities are converted to DTOs inside the transaction so they can be safely serialized in the HTTP response.

```kotlin
fun ProductEntity.toDto() = ProductDto(id.value, name, price, stock)
```

### Creating a New Entity

```kotlin
@PostMapping
fun create(@RequestBody dto: ProductDto): ResponseEntity<ProductDto> {
    val created = transaction {
        ProductEntity.new {
            name = dto.name
            price = dto.price
            stock = dto.stock
        }.toDto()
    }
    return ResponseEntity.created(URI.create("/products/${created.id}")).body(created)
}
```

## Caveats

1. **Exposed DAO entities must not escape the transaction boundary**: Convert to DTO inside the transaction to avoid proxy initialization errors during HTTP response serialization.

2. **Spring Data JDBC Repository extension**: Methods added to interfaces that extend `ExposedJdbcRepository` will have PartTree queries generated automatically.

3. **Logging**: By default, DEBUG logging is enabled for the `io.bluetape4k` and `org.jetbrains.exposed` packages.

## Dependencies

```kotlin
implementation(project(":bluetape4k-spring-boot3-exposed-jdbc"))
implementation(Libs.springBootStarter("web"))
implementation(Libs.exposed_spring_boot_starter)
implementation(Libs.exposed_jdbc)
implementation(Libs.exposed_dao)
implementation(Libs.exposed_migration_jdbc)
runtimeOnly(Libs.h2_v2)
```

## References

- [Exposed ORM Official Documentation](https://github.com/JetBrains/Exposed)
- [Spring Boot Official Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JDBC Guide](https://spring.io/projects/spring-data-jdbc)
