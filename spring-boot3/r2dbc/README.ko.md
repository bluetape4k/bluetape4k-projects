# Module bluetape4k-spring-r2dbc

[English](./README.md) | 한국어

Spring Data R2DBC를 Kotlin 코루틴에서 사용하기 편하게 확장한 라이브러리입니다.

## 주요 기능

- **R2dbcEntityOperations 확장**: 코루틴 기반 CRUD 연산
- **ReactiveInsert/Update/Delete/Select 확장**: 타입 안전한 코루틴 연산
- **네이밍 규칙**: `XyzSuspending` 형식의 일관된 함수명

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-r2dbc:${version}")
}
```

## 주요 기능 상세

### 1. R2dbcEntityOperations 확장

#### ID로 단건 조회

```kotlin
import io.bluetape4k.spring.r2dbc.coroutines.*
import org.springframework.data.r2dbc.core.R2dbcEntityOperations

class PostService(private val operations: R2dbcEntityOperations) {

    suspend fun findById(id: Long): Post {
        // ID로 조회 (없으면 예외)
        return operations.findOneByIdSuspending<Post>(id)
    }

    suspend fun findByIdOrNull(id: Long): Post? {
        // ID로 조회 (없으면 null)
        return operations.findOneByIdOrNullSuspending<Post>(id)
    }

    suspend fun findFirstById(id: Long): Post {
        // ID로 첫 번째 결과 조회
        return operations.findFirstByIdSuspending<Post>(id)
    }

    suspend fun findFirstByIdOrNull(id: Long): Post? {
        // ID로 첫 번째 결과 조회 (없으면 null)
        return operations.findFirstByIdOrNullSuspending<Post>(id)
    }

    // 커스텀 ID 컬럼명 지정
    suspend fun findByPostId(postId: String): Post {
        return operations.findOneByIdSuspending<Post>(postId, "post_id")
    }
}
```

#### Query를 이용한 조회

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

#### 전체 조회 및 건수

```kotlin
class PostService(private val operations: R2dbcEntityOperations) {

    // 전체 조회
    fun findAll(): Flow<Post> {
        return operations.selectAllSuspending<Post>()
    }

    // 전체 건수
    suspend fun countAll(): Long {
        return operations.countAllSuspending<Post>()
    }

    // 조건부 건수
    suspend fun countByStatus(status: PostStatus): Long {
        val query = Query.query(Criteria.where("status").isEqual(status.name))
        return operations.countSuspending<Post>(query)
    }

    // 존재 여부 확인
    suspend fun existsByAuthorId(authorId: Long): Boolean {
        val query = Query.query(Criteria.where("author_id").isEqual(authorId))
        return operations.existsSuspending<Post>(query)
    }
}
```

---

### 2. Insert 확장

```kotlin
import io.bluetape4k.spring.r2dbc.coroutines.*

class PostService(private val operations: R2dbcEntityOperations) {

    suspend fun createPost(title: String, content: String): Post {
        val post = Post(
            title = title,
            content = content,
            createdAt = Instant.now()
        )

        // 저장된 엔티티 반환
        return operations.insertSuspending(post)
    }

    suspend fun createPostOrNull(title: String, content: String): Post? {
        val post = Post(title = title, content = content)
        return operations.insertOrNullSuspending(post)
    }
}
```

---

### 3. Update 확장

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

### 4. Delete 확장

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

### 5. Flow 기반 스트리밍

대량 데이터를 Flow로 스트리밍하여 메모리 효율적으로 처리합니다.

```kotlin
class PostService(private val operations: R2dbcEntityOperations) {

    // Flow로 스트리밍
    fun streamAllPosts(): Flow<Post> {
        return operations.selectAllSuspending<Post>()
    }

    // Flow 처리
    suspend fun exportAllPosts(): Int {
        var count = 0
        operations.selectAllSuspending<Post>()
            .collect { post ->
                exportToCsv(post)
                count++
            }
        return count
    }

    // 배치 처리
    fun processInBatches(batchSize: Int = 100): Flow<List<Post>> {
        return operations.selectAllSuspending<Post>()
            .chunked(batchSize)
    }
}
```

---

### 6. 네이밍 규칙

코루틴 함수는 `XyzSuspending` 형식으로 제공됩니다.

---

### 7. 전체 예시

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

## 테스트

```bash
./gradlew :spring:r2dbc:test
```

## 아키텍처 다이어그램

### 핵심 클래스 다이어그램

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

    style R2dbcEntityOperationsExt fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style PostRepository fill:#E0F7FA,stroke:#80DEEA,color:#00695C
    style Post fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    style PostStatus fill:#FFFDE7,stroke:#FFF176,color:#F57F17
```

### R2DBC + Coroutines 데이터 흐름

```mermaid
flowchart TD
    App["애플리케이션 코드"] --> Ext["코루틴 확장 함수<br/>(XyzSuspending / Flow)"]
    Ext --> ROps["R2dbcEntityOperations"]
    ROps --> R2DBC["Spring Data R2DBC"]
    R2DBC --> Driver["R2DBC Driver<br/>(H2 / PostgreSQL / MySQL)"]
    Driver --> DB[("관계형 데이터베이스")]
    Ext -- "Mono → suspend" --> App
    Ext -- "Flux → Flow" --> App

    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef asyncStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef springStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef extStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    classDef dataStyle fill:#F57F17,stroke:#E65100,color:#000000

    class App serviceStyle
    class Ext asyncStyle
    class ROps asyncStyle
    class R2DBC springStyle
    class Driver extStyle
    class DB dataStyle
```

### CRUD 연산 계층 구조

```mermaid
flowchart LR
    Service["서비스 / Repository"] --> Select["selectAllSuspending()<br/>selectSuspending(query)<br/>findOneByIdOrNullSuspending(id)"]
    Service --> Insert["insertSuspending(entity)<br/>insertOrNullSuspending(entity)"]
    Service --> Update["updateSuspending(query, update)"]
    Service --> Delete["deleteSuspending(query)<br/>deleteAllSuspending()"]
    Service --> Count["countAllSuspending()<br/>countSuspending(query)<br/>existsSuspending(query)"]
    Select --> ROps["R2dbcEntityOperations"]
    Insert --> ROps
    Update --> ROps
    Delete --> ROps
    Count --> ROps
    ROps --> DB[("데이터베이스")]

    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef asyncStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
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

### 코루틴 변환 시퀀스

```mermaid
sequenceDiagram
        participant App as 애플리케이션
        participant Ext as XyzSuspending 확장
        participant Ops as R2dbcEntityOperations
        participant DB as 데이터베이스

    App->>Ext: findOneByIdOrNullSuspending<Post>(id)
    Ext->>Ops: selectOne(query, Post::class) → Mono<Post>
    Ops->>DB: SELECT * FROM posts WHERE id=?
    DB-->>Ops: 행 데이터
    Ops-->>Ext: Mono<Post>
    Ext-->>App: Post? (suspend 반환)

    App->>Ext: selectAllSuspending<Post>()
    Ext->>Ops: select(Post::class) → Flux<Post>
    Ops->>DB: SELECT * FROM posts
    DB-->>Ops: 행 스트림
    Ops-->>Ext: Flux<Post>
    Ext-->>App: Flow<Post> (코루틴 스트림)
```

## 참고

- [Spring Data R2DBC Reference](https://docs.spring.io/spring-data/r2dbc/reference/)
- [Kotlin Coroutines Support](https://docs.spring.io/spring-framework/reference/languages/kotlin/coroutines.html)
