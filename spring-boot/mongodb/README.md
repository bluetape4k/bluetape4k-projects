# Module bluetape4k-spring-boot-mongodb

English | [한국어](./README.ko.md)

An extension library for working with [Spring Data MongoDB Reactive](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/) using Kotlin Coroutines (Spring Boot 4.1+).

Provides extension functions that convert `Flux`/`Mono` return types from `ReactiveMongoOperations` to `Flow`/
`suspend`, along with Kotlin infix DSLs for building `Criteria`, `Query`, and `Update` objects.

> This is the versionless Spring Boot 4.1+ implementation.

## Spring Boot 4.1 Configuration Boundary

Spring Boot 4.1 binds Mongo connection settings under `spring.mongodb.*`.
Configure the URI with the current namespace:

```yaml
spring:
    mongodb:
        uri: mongodb://127.0.0.1:27018/synthetic
```

`ReactiveMongoAutoConfiguration` runs after Spring Boot's
`DataMongoReactiveAutoConfiguration`. An existing `ReactiveMongoOperations`
bean always wins, whether it was provided by the application or Spring Boot.
The library creates a fallback `ReactiveMongoTemplate` only when no operations
bean exists and both `ReactiveMongoDatabaseFactory` and `MongoConverter` are
available. The whole library auto-configuration, including its legacy-property
guard, backs off when an operations bean already exists.

### Migration from `spring.data.mongodb.uri`

| Before | After |
|--------|-------|
| `spring.data.mongodb.uri` | `spring.mongodb.uri` |

When the library fallback participates, the legacy-only key fails fast instead
of silently connecting to the default localhost database:

```text
IllegalStateException: Unsupported legacy MongoDB property 'spring.data.mongodb.uri'; use 'spring.mongodb.uri' on Spring Boot 4.1+
```

During a staged migration, if both keys are present, `spring.mongodb.uri` takes
precedence. Use a synthetic URI in tests and keep credentials out of logs and
diagnostic artifacts. An application- or Spring Boot-provided
`ReactiveMongoOperations` bean owns the active connection path, so this library
does not inspect the legacy key on that backoff path.

If migration cannot be completed immediately, pin the last stable artifact and
BOM that support the legacy namespace, then resume the migration before
returning to this Boot 4.1+ artifact:

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:1.12.1"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-mongodb:1.12.1")
}
```

## Features

- **ReactiveMongoOperations coroutine extensions**: `Flux` → `Flow`, `Mono` → `suspend` conversions
- **Criteria infix DSL**: `"age".criteria() gt 28`, `"name".criteria() eq "Alice"`, etc.
- **Query builder extensions**: `queryOf()`, `sortAscBy()`, `paginate()`, etc.
- **Update DSL**: `"field" setTo value`, `"field".incBy()`, etc.

## Diagrams

### Core Class Structure

![Core Class Structure diagram](../../docs/images/readme-diagrams/spring-boot-mongodb-diagram-01.png)

### ReactiveMongoOperations Coroutine Extension Flow

![ReactiveMongoOperations Coroutine Extension Flow diagram](../../docs/images/readme-diagrams/spring-boot-mongodb-diagram-02.png)

### Criteria / Query / Update DSL Flow

![Criteria / Query / Update DSL Flow diagram](../../docs/images/readme-diagrams/spring-boot-mongodb-diagram-03.png)

### Coroutine Conversion Sequence

![Coroutine Conversion Sequence diagram](../../docs/images/readme-diagrams/spring-boot-mongodb-sequence-01.png)

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-mongodb:${bluetape4kVersion}")
}
```

## Usage Examples

### ReactiveMongoOperations Coroutine Extensions

```kotlin
import io.bluetape4k.spring.mongodb.coroutines.*

// Find one
val user: User? = mongoOperations.findOneOrNullSuspending(
    Query(Criteria.where("name").`is`("Alice"))
)

// Find all as Flow
val users: List<User> = mongoOperations.findAllAsFlow<User>().toList()

// Insert
val saved: User = mongoOperations.insertSuspending(User(name = "Bob", age = 25))

// Count
val count: Long = mongoOperations.countSuspending<User>()

// Update
mongoOperations.updateMultiSuspending<User>(
    Query(Criteria.where("city").`is`("Seoul")),
    Update().set("city", "Suwon")
)
```

### Criteria infix DSL

```kotlin
import io.bluetape4k.spring.mongodb.query.*

val c1 = "age".criteria() gt 20
val c2 = "name".criteria() eq "Alice"
val c3 = "city".criteria() inValues listOf("Seoul", "Busan")
val c4 = "deletedAt".criteria().isNull()
val c5 = "age".criteria().gt(20) andWith "city".criteria().`is`("Seoul")
```

### Query Builder Extensions

```kotlin
val query = queryOf("age".criteria() gt 20, "city".criteria() eq "Seoul")
    .sortAscBy("name")
    .paginate(page = 0, size = 10)
```

### Update DSL

```kotlin
val update = ("name" setTo "Alice")
    .andSet("age", 30)
    .andSet("city", "Seoul")
```

## Available Extension Functions

| Function                                  | Return Type    | Description                        |
|-------------------------------------------|----------------|------------------------------------|
| `findAsFlow<T>(query)`                    | `Flow<T>`      | Stream documents matching a query  |
| `findAllAsFlow<T>()`                      | `Flow<T>`      | Stream all documents               |
| `findOneOrNullSuspending<T>(query)`       | `T?`           | Find one document (null if absent) |
| `countSuspending<T>(query?)`              | `Long`         | Count documents                    |
| `existsSuspending<T>(query)`              | `Boolean`      | Check existence                    |
| `insertSuspending(entity)`                | `T`            | Insert a single document           |
| `insertAllAsFlow(entities)`               | `Flow<T>`      | Insert multiple documents          |
| `saveSuspending(entity)`                  | `T`            | Save (insert or update)            |
| `updateMultiSuspending<T>(query, update)` | `UpdateResult` | Update multiple documents          |
| `removeSuspending<T>(query)`              | `DeleteResult` | Delete documents by query          |
| `aggregateAsFlow<I, O>(aggregation)`      | `Flow<O>`      | Execute an aggregation pipeline    |
| `dropCollectionSuspending<T>()`           | `Unit`         | Drop a collection                  |

## Build and Test

```bash
./gradlew :bluetape4k-spring-boot-mongodb:test
```

The `ReactiveMongoAutoConfigurationTest` context suite validates namespace
binding, legacy fail-fast behavior, dual-key precedence, fallback conditions,
Boot ordering, single-instance creation, and context close without MongoDB
network I/O. The coroutine integration suite uses the shared Testcontainers
MongoDB server and should be run separately when validating a real database.

## References

- [Spring Data MongoDB Official Documentation](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [bluetape4k-mongodb](../../data/mongodb/README.md) — Native MongoDB Kotlin driver extensions
