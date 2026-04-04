# Module bluetape4k-spring-boot4-r2dbc

English | [한국어](./README.ko.md)

An extension library that makes Spring Data R2DBC easier to use with Kotlin Coroutines (Spring Boot 4.x).

> Provides the same functionality as the Spring Boot 3 module (`bluetape4k-spring-r2dbc`), adapted to the Spring Boot 4.x API.

## Key Features

- **R2dbcEntityOperations extensions**: Coroutines-based CRUD operations
- **ReactiveInsert/Update/Delete/Select extensions**: Type-safe coroutine operations
- **Naming convention**: Consistent `XyzSuspending` function naming

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot4-r2dbc:${bluetape4kVersion}")
}
```

## Usage Examples

### R2dbcEntityOperations Extensions

```kotlin
import io.bluetape4k.spring4.r2dbc.coroutines.*

class PostService(private val operations: R2dbcEntityOperations) {

    suspend fun findById(id: Long): Post? =
        operations.findOneByIdOrNullSuspending<Post>(id)

    fun findAll(): Flow<Post> =
        operations.selectAllSuspending<Post>()

    suspend fun save(post: Post): Post =
        operations.insertSuspending(post)

    suspend fun update(id: Long, title: String): Int {
        val query = Query.query(Criteria.where("id").isEqual(id))
        val update = Update.update("title", title)
        return operations.updateSuspending<Post>(query, update)
    }

    suspend fun delete(id: Long): Int {
        val query = Query.query(Criteria.where("id").isEqual(id))
        return operations.deleteSuspending<Post>(query)
    }

    suspend fun count(): Long =
        operations.countAllSuspending<Post>()
}
```

### Repository Example

```kotlin
@Table("posts")
data class Post(
    @Id val id: Long? = null,
    val title: String,
    val content: String,
    val authorId: Long,
    val createdAt: Instant = Instant.now(),
)

@Repository
class PostRepository(private val operations: R2dbcEntityOperations) {

    suspend fun findById(id: Long): Post? =
        operations.findOneByIdOrNullSuspending<Post>(id)

    fun findAll(): Flow<Post> =
        operations.selectAllSuspending<Post>()

    suspend fun save(post: Post): Post =
        operations.insertSuspending(post)

    suspend fun delete(id: Long): Int {
        val query = Query.query(Criteria.where("id").isEqual(id))
        return operations.deleteSuspending<Post>(query)
    }
}
```

### Naming Convention

Coroutine functions follow the `XyzSuspending` naming pattern.

| Function                                | Return Type | Description                         |
|----------------------------------------|-------------|-------------------------------------|
| `findOneByIdSuspending<T>(id)`         | `T`         | Find by ID                          |
| `findOneByIdOrNullSuspending<T>(id)`   | `T?`        | Find by ID (null if not found)      |
| `selectAllSuspending<T>()`             | `Flow<T>`   | Select all records                  |
| `selectSuspending<T>(query)`           | `Flow<T>`   | Select by query                     |
| `selectOneSuspending<T>(query)`        | `T`         | Select single record                |
| `selectOneOrNullSuspending<T>(query)`  | `T?`        | Select single record (null if none) |
| `insertSuspending(entity)`             | `T`         | Insert                              |
| `updateSuspending<T>(query, update)`   | `Int`       | Update                              |
| `deleteSuspending<T>(query)`           | `Int`       | Delete                              |
| `deleteAllSuspending<T>()`             | `Int`       | Delete all                          |
| `countAllSuspending<T>()`              | `Long`      | Total count                         |
| `countSuspending<T>(query)`            | `Long`      | Conditional count                   |
| `existsSuspending<T>(query)`           | `Boolean`   | Check existence                     |

## Build and Test

```bash
./gradlew :bluetape4k-spring-boot4-r2dbc:test
```

## Architecture Diagrams

### Core Class Structure

```mermaid
classDiagram
    class PostRepository {
        -operations: R2dbcEntityOperations
        +findById(id): Post?
        +findAll(): Flow~Post~
        +save(post): Post
        +update(id, title): Int
        +delete(id): Int
        +count(): Long
    }
    class R2dbcEntityOperationsExt {
        <<extension>>
        +findOneByIdOrNullSuspending(id): T?
        +selectAllSuspending(): Flow~T~
        +selectSuspending(query): Flow~T~
        +selectOneSuspending(query): T
        +insertSuspending(entity): T
        +updateSuspending(query, update): Int
        +deleteSuspending(query): Int
        +deleteAllSuspending(): Int
        +countAllSuspending(): Long
        +existsSuspending(query): Boolean
    }
    class Post {
        +id: Long?
        +title: String
        +content: String
        +authorId: Long
        +createdAt: Instant
    }
    class R2dbcConfig {
        +connectionFactory(): ConnectionFactory
        +r2dbcEntityOperations(): R2dbcEntityOperations
    }

    PostRepository --> R2dbcEntityOperationsExt
    PostRepository --> Post
    R2dbcConfig --> PostRepository : inject
```

### R2DBC + Coroutines Data Flow

```mermaid
flowchart TD
    App["Application Code"] --> Ext["Coroutine Extension Functions<br/>(XyzSuspending / Flow)"]
    Ext --> ROps["R2dbcEntityOperations"]
    ROps --> R2DBC["Spring Data R2DBC"]
    R2DBC --> Driver["R2DBC Driver<br/>(H2 / PostgreSQL / MySQL)"]
    Driver --> DB[("Relational Database")]
    Ext -- "Mono → suspend" --> App
    Ext -- "Flux → Flow" --> App
```

### CRUD Operation Hierarchy

```mermaid
flowchart LR
    Service["Service / Repository"] --> Select["selectAllSuspending()<br/>selectSuspending(query)<br/>findOneByIdOrNullSuspending(id)"]
    Service --> Insert["insertSuspending(entity)<br/>insertOrNullSuspending(entity)"]
    Service --> Update["updateSuspending(query, update)"]
    Service --> Delete["deleteSuspending(query)<br/>deleteAllSuspending()"]
    Service --> Count["countAllSuspending()<br/>countSuspending(query)<br/>existsSuspending(query)"]
    Select --> ROps["R2dbcEntityOperations"]
    Insert --> ROps
    Update --> ROps
    Delete --> ROps
    Count --> ROps
    ROps --> DB[("Database")]
```

### Coroutine Conversion Sequence

```mermaid
sequenceDiagram
    participant App as Application
    participant Ext as XyzSuspending Extension
    participant Ops as R2dbcEntityOperations
    participant DB as Database

    App->>Ext: findOneByIdOrNullSuspending<Post>(id)
    Ext->>Ops: selectOne(query, Post::class) → Mono<Post>
    Ops->>DB: SELECT * FROM posts WHERE id=?
    DB-->>Ops: Row data
    Ops-->>Ext: Mono<Post>
    Ext-->>App: Post? (suspend return)

    App->>Ext: selectAllSuspending<Post>()
    Ext->>Ops: select(Post::class) → Flux<Post>
    Ops->>DB: SELECT * FROM posts
    DB-->>Ops: Row stream
    Ops-->>Ext: Flux<Post>
    Ext-->>App: Flow<Post> (coroutine stream)
```

## References

- [Spring Data R2DBC Official Documentation](https://docs.spring.io/spring-data/r2dbc/reference/)
- [Kotlin Coroutines Support](https://docs.spring.io/spring-framework/reference/languages/kotlin/coroutines.html)
