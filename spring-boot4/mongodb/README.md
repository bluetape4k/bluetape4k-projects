# Module bluetape4k-spring-boot4-mongodb

English | [한국어](./README.ko.md)

An extension library for working with [Spring Data MongoDB Reactive](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/) using Kotlin Coroutines (Spring Boot 4.x).

Provides extension functions that convert `Flux`/`Mono` return types from `ReactiveMongoOperations` to `Flow`/
`suspend`, along with Kotlin infix DSLs for building `Criteria`, `Query`, and `Update` objects.

> Provides the same functionality as the Spring Boot 3 module (
`bluetape4k-spring-mongodb`), adapted to the Spring Boot 4.x API.

## Features

- **ReactiveMongoOperations coroutine extensions**: `Flux` → `Flow`, `Mono` → `suspend` conversions
- **Criteria infix DSL**: `"age".criteria() gt 28`, `"name".criteria() eq "Alice"`, etc.
- **Query builder extensions**: `queryOf()`, `sortAscBy()`, `paginate()`, etc.
- **Update DSL**: `"field" setTo value`, `"field".incBy()`, etc.

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot4-mongodb:${bluetape4kVersion}")
}
```

## Usage Examples

### ReactiveMongoOperations Coroutine Extensions

```kotlin
import io.bluetape4k.spring4.mongodb.coroutines.*

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
import io.bluetape4k.spring4.mongodb.query.*

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
./gradlew :bluetape4k-spring-boot4-mongodb:test
```

## Architecture Diagrams

### Core Class Structure

```mermaid
classDiagram
    class ReactiveMongoOperationsExt {
        <<extension>>
        +findAsFlow(query): Flow~T~
        +findAllAsFlow(): Flow~T~
        +findOneOrNullSuspending(query): T?
        +countSuspending(query?): Long
        +existsSuspending(query): Boolean
        +insertSuspending(entity): T
        +insertAllAsFlow(entities): Flow~T~
        +saveSuspending(entity): T
        +updateMultiSuspending(query, update): UpdateResult
        +removeSuspending(query): DeleteResult
        +aggregateAsFlow(aggregation): Flow~O~
    }
    class CriteriaDsl {
        <<DSL>>
        +criteria() gt value
        +criteria() eq value
        +criteria() inValues list
        +criteria().isNull()
        +andWith(other)
    }
    class QueryBuilderExt {
        <<extension>>
        +queryOf(criteria): Query
        +sortAscBy(field): Query
        +paginate(page, size): Query
    }
    class UpdateDsl {
        <<DSL>>
        +setTo(value): Update
        +andSet(field, value): Update
        +incBy(delta): Update
    }
    class UserRepository {
        -operations: ReactiveMongoOperations
        +findAllUsers(): Flow~User~
        +findByName(name): User?
        +save(user): User
        +delete(query): DeleteResult
    }

    UserRepository --> ReactiveMongoOperationsExt
    UserRepository --> CriteriaDsl
    UserRepository --> QueryBuilderExt
    UserRepository --> UpdateDsl

    style ReactiveMongoOperationsExt fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style QueryBuilderExt fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style CriteriaDsl fill:#F57F17,stroke:#E65100,color:#000000
    style UpdateDsl fill:#F57F17,stroke:#E65100,color:#000000
    style UserRepository fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
```

### ReactiveMongoOperations Coroutine Extension Flow

```mermaid
flowchart TD
    App["Application Code"] --> Ext["Coroutine Extension Functions<br/>(bluetape4k-spring-boot4-mongodb)"]
    Ext --> ROps["ReactiveMongoOperations"]
    ROps --> Reactor["Reactor<br/>Mono / Flux"]
    Reactor --> Driver["MongoDB Reactive Driver"]
    Driver --> MongoDB[("MongoDB")]
    Ext -- "Mono → suspend" --> App
    Ext -- "Flux → Flow" --> App

    classDef appStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    classDef extStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef reactorStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef driverStyle fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    classDef dbStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100

    class App appStyle
    class Ext,ROps extStyle
    class Reactor reactorStyle
    class Driver driverStyle
    class MongoDB dbStyle
```

### Criteria / Query / Update DSL Flow

```mermaid
flowchart LR
    Code["Application Code"] --> CriteriaDSL["Criteria infix DSL<br/>age.criteria() gt 20"]
    Code --> QueryBuilder["Query builder extensions<br/>queryOf() / paginate()"]
    Code --> UpdateDSL["Update DSL<br/>field setTo value"]
    CriteriaDSL --> Query["Query object"]
    QueryBuilder --> Query
    UpdateDSL --> Update["Update object"]
    Query --> ROps["ReactiveMongoOperations<br/>Coroutine Extensions"]
    Update --> ROps
    ROps --> MongoDB[("MongoDB")]

    classDef appStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    classDef dslStyle fill:#F57F17,stroke:#E65100,color:#000000
    classDef queryStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef opsStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef dbStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100

    class Code appStyle
    class CriteriaDSL,QueryBuilder,UpdateDSL dslStyle
    class Query,Update queryStyle
    class ROps opsStyle
    class MongoDB dbStyle
```

### Coroutine Conversion Sequence

```mermaid
sequenceDiagram
        participant App as Application
        participant Ext as Coroutine Extension
        participant Ops as ReactiveMongoOperations
        participant DB as MongoDB

    App->>Ext: findAllAsFlow<User>()
    Ext->>Ops: findAll(User::class) → Flux<User>
    Ops->>DB: find({}) query
    DB-->>Ops: Document stream
    Ops-->>Ext: Flux<User>
    Ext-->>App: Flow<User> (coroutine stream)

    App->>Ext: insertSuspending(user)
    Ext->>Ops: insert(user) → Mono<User>
    Ops->>DB: insertOne request
    DB-->>Ops: Inserted document
    Ops-->>Ext: Mono<User>
    Ext-->>App: User (suspend result)
```

## References

- [Spring Data MongoDB Official Documentation](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [bluetape4k-mongodb](../../data/mongodb/README.md) — Native MongoDB Kotlin driver extensions
