# Module bluetape4k-spring-r2dbc

English | [한국어](./README.ko.md)

An extension library that makes Spring Data R2DBC more ergonomic to use with Kotlin coroutines.

## Key Features

- **R2dbcEntityOperations extensions**: Coroutine-based CRUD operations
- **ReactiveInsert/Update/Delete/Select extensions**: Type-safe coroutine operations
- **Naming convention**: Consistent `XyzSuspending` function names

## Adding the Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-r2dbc:${version}")
}
```

## Feature Details

### 1. R2dbcEntityOperations Extensions

#### Find by ID

```kotlin
import io.bluetape4k.spring.r2dbc.coroutines.*
import org.springframework.data.r2dbc.core.R2dbcEntityOperations

class PostService(private val operations: R2dbcEntityOperations) {

    suspend fun findById(id: Long): Post {
        // Find by ID (throws if not found)
        return operations.findOneByIdSuspending<Post>(id)
    }

    suspend fun findByIdOrNull(id: Long): Post? {
        // Find by ID (returns null if not found)
        return operations.findOneByIdOrNullSuspending<Post>(id)
    }

    suspend fun findFirstById(id: Long): Post {
        // Find the first result by ID
        return operations.findFirstByIdSuspending<Post>(id)
    }

    suspend fun findFirstByIdOrNull(id: Long): Post? {
        // Find the first result by ID (returns null if not found)
        return operations.findFirstByIdOrNullSuspending<Post>(id)
    }

    // Specify a custom ID column name
    suspend fun findByPostId(postId: String): Post {
        return operations.findOneByIdSuspending<Post>(postId, "post_id")
    }
}
```

#### Query-Based Lookup

```kotlin
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.isEqual

class PostService(private val operations: R2dbcEntityOperations) {

    suspend fun findByTitle(title: String): Post? {
        val query = Query.query(Criteria.where("title").isEqual(title))
        return operations.selectOneOrNullSuspending<Post>(query)
    }

    suspend fun findByStatus(status: PostStatus): List<Post> {
        val query = Query.query(Criteria.where("status").isEqual(status.name))
        return operations.selectSuspending<Post>(query).toList()
    }

    suspend fun findByAuthorId(authorId: Long): Post {
        val query = Query.query(Criteria.where("author_id").isEqual(authorId))
        return operations.selectFirstSuspending<Post>(query)
    }
}
```

#### Find All and Count

```kotlin
class PostService(private val operations: R2dbcEntityOperations) {

    // Find all
    fun findAll(): Flow<Post> {
        return operations.selectAllSuspending<Post>()
    }

    // Count all
    suspend fun countAll(): Long {
        return operations.countAllSuspending<Post>()
    }

    // Conditional count
    suspend fun countByStatus(status: PostStatus): Long {
        val query = Query.query(Criteria.where("status").isEqual(status.name))
        return operations.countSuspending<Post>(query)
    }

    // Existence check
    suspend fun existsByAuthorId(authorId: Long): Boolean {
        val query = Query.query(Criteria.where("author_id").isEqual(authorId))
        return operations.existsSuspending<Post>(query)
    }
}
```

---

### 2. Insert Extensions

```kotlin
import io.bluetape4k.spring.r2dbc.coroutines.*

class PostService(private val operations: R2dbcEntityOperations) {

    suspend fun createPost(title: String, content: String): Post {
        val post = Post(
            title = title,
            content = content,
            createdAt = Instant.now()
        )

        // Returns the saved entity
        return operations.insertSuspending(post)
    }

    suspend fun createPostOrNull(title: String, content: String): Post? {
        val post = Post(title = title, content = content)
        return operations.insertOrNullSuspending(post)
    }
}
```

---

### 3. Update Extensions

```kotlin
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Update

class PostService(private val operations: R2dbcEntityOperations) {

    suspend fun updatePostTitle(id: Long, newTitle: String): Int {
        val query = Query.query(Criteria.where("id").isEqual(id))
        val update = Update.update("title", newTitle)

        return operations.updateSuspending<Post>(query, update)
    }

    suspend fun updatePostStatus(id: Long, status: PostStatus): Int {
        val query = Query.query(Criteria.where("id").isEqual(id))
        val update = Update.update("status", status.name)
            .set("updated_at", Instant.now())

        return operations.updateSuspending<Post>(query, update)
    }
}
```

---

### 4. Delete Extensions

```kotlin
class PostService(private val operations: R2dbcEntityOperations) {

    suspend fun deleteById(id: Long): Int {
        val query = Query.query(Criteria.where("id").isEqual(id))
        return operations.deleteSuspending<Post>(query)
    }

    suspend fun deleteByAuthorId(authorId: Long): Int {
        val query = Query.query(Criteria.where("author_id").isEqual(authorId))
        return operations.deleteSuspending<Post>(query)
    }

    suspend fun deleteAll(): Int {
        return operations.deleteAllSuspending<Post>()
    }
}
```

---

### 5. Flow-Based Streaming

Stream large datasets as a `Flow` for memory-efficient processing.

```kotlin
class PostService(private val operations: R2dbcEntityOperations) {

    // Stream as Flow
    fun streamAllPosts(): Flow<Post> {
        return operations.selectAllSuspending<Post>()
    }

    // Process a Flow
    suspend fun exportAllPosts(): Int {
        var count = 0
        operations.selectAllSuspending<Post>()
            .collect { post ->
                exportToCsv(post)
                count++
            }
        return count
    }

    // Batch processing
    fun processInBatches(batchSize: Int = 100): Flow<List<Post>> {
        return operations.selectAllSuspending<Post>()
            .chunked(batchSize)
    }
}
```

---

### 6. Naming Convention

Coroutine functions follow the `XyzSuspending` naming pattern.

---

### 7. Complete Example

```kotlin
@Table("posts")
data class Post(
    @Id val id: Long? = null,
    val title: String,
    val content: String,
    val status: PostStatus,
    val authorId: Long,
    val createdAt: Instant,
    val updatedAt: Instant? = null
)

enum class PostStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}

@Repository
class PostRepository(
    private val operations: R2dbcEntityOperations
) {

    suspend fun findById(id: Long): Post? {
        return operations.findOneByIdOrNullSuspending<Post>(id)
    }

    suspend fun findAll(): List<Post> {
        return operations.selectAllSuspending<Post>().toList()
    }

    suspend fun findByStatus(status: PostStatus): List<Post> {
        val query = Query.query(Criteria.where("status").isEqual(status.name))
        return operations.selectSuspending<Post>(query).toList()
    }

    suspend fun save(post: Post): Post {
        return operations.insertSuspending(post)
    }

    suspend fun update(id: Long, title: String, content: String): Int {
        val query = Query.query(Criteria.where("id").isEqual(id))
        val update = Update.update("title", title)
            .set("content", content)
            .set("updated_at", Instant.now())
        return operations.updateSuspending<Post>(query, update)
    }

    suspend fun delete(id: Long): Int {
        val query = Query.query(Criteria.where("id").isEqual(id))
        return operations.deleteSuspending<Post>(query)
    }

    suspend fun count(): Long {
        return operations.countAllSuspending<Post>()
    }
}
```

---

## Testing

```bash
./gradlew :spring:r2dbc:test
```

## Architecture Diagrams

### Core Class Diagram

```mermaid
classDiagram
    class R2dbcEntityOperationsExt {
        <<extension>>
        +findOneByIdSuspending(id): T
        +findOneByIdOrNullSuspending(id): T?
        +selectAllSuspending(): Flow~T~
        +selectSuspending(query): Flow~T~
        +selectOneSuspending(query): T
        +selectOneOrNullSuspending(query): T?
        +insertSuspending(entity): T
        +insertOrNullSuspending(entity): T?
        +updateSuspending(query, update): Int
        +deleteSuspending(query): Int
        +deleteAllSuspending(): Int
        +countAllSuspending(): Long
        +countSuspending(query): Long
        +existsSuspending(query): Boolean
    }
    class PostRepository {
        -operations: R2dbcEntityOperations
        +findById(id): Post?
        +findAll(): Flow~Post~
        +findByStatus(status): List~Post~
        +save(post): Post
        +update(id, title, content): Int
        +delete(id): Int
        +count(): Long
    }
    class Post {
        +id: Long?
        +title: String
        +content: String
        +status: PostStatus
        +authorId: Long
        +createdAt: Instant
        +updatedAt: Instant?
    }
    class PostStatus {
        <<enum>>
        DRAFT
        PUBLISHED
        ARCHIVED
    }

    PostRepository --> R2dbcEntityOperationsExt : uses
    PostRepository --> Post
    Post --> PostStatus

    style R2dbcEntityOperationsExt fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    style PostRepository fill:#00838F,stroke:#006064,color:#FFFFFF
    style Post fill:#F57F17,stroke:#E65100,color:#FFFFFF
    style PostStatus fill:#F57F17,stroke:#E65100,color:#FFFFFF
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

    classDef serviceStyle fill:#1565C0,stroke:#1565C0,color:#FFFFFF
    classDef asyncStyle fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    classDef springStyle fill:#2E7D32,stroke:#1B5E20,color:#FFFFFF
    classDef extStyle fill:#37474F,stroke:#263238,color:#FFFFFF
    classDef dataStyle fill:#F57F17,stroke:#E65100,color:#000000

    class App serviceStyle
    class Ext asyncStyle
    class ROps asyncStyle
    class R2DBC springStyle
    class Driver extStyle
    class DB dataStyle
```

### CRUD Operation Layer

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

    classDef serviceStyle fill:#1565C0,stroke:#1565C0,color:#FFFFFF
    classDef asyncStyle fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    classDef dataStyle fill:#F57F17,stroke:#E65100,color:#000000

    class Service serviceStyle
    class Select asyncStyle
    class Insert asyncStyle
    class Update asyncStyle
    class Delete asyncStyle
    class Count asyncStyle
    class ROps asyncStyle
    class DB dataStyle
```

### Coroutine Conversion Sequence

```mermaid
sequenceDiagram
    box rgb(187,222,251) Application Layer
        participant App as Application
    end
    box rgb(225,190,231) Coroutine Extensions
        participant Ext as XyzSuspending Extension
        participant Ops as R2dbcEntityOperations
    end
    box rgb(224,224,224) Data Layer
        participant DB as Database
    end

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

- [Spring Data R2DBC Reference](https://docs.spring.io/spring-data/r2dbc/reference/)
- [Kotlin Coroutines Support](https://docs.spring.io/spring-framework/reference/languages/kotlin/coroutines.html)
