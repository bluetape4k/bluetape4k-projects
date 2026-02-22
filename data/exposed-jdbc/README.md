# Module bluetape4k-exposed-jdbc

JetBrains Exposed JDBC 계층을 위한 Repository 패턴, 트랜잭션 확장, 쿼리 유틸리티를 제공하는 모듈입니다. `bluetape4k-exposed-core`와
`bluetape4k-exposed-dao`를 기반으로 JDBC에 특화된 기능을 제공합니다.

## 개요

`bluetape4k-exposed-jdbc`는 다음을 제공합니다:

- **Repository 패턴**: `ExposedRepository<T, ID>`, `SoftDeletedRepository<T, ID>` 인터페이스
- **Coroutines 지원**: `SuspendedQuery` — suspend 함수로 JDBC 쿼리 실행
- **Virtual Thread 트랜잭션**: JDK 21+ Virtual Thread 기반 트랜잭션 실행
- **테이블/스키마 확장**: `ImplicitSelectAll`, `TableExtensions`, `SchemaUtilsExtensions`

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-jdbc:${version}")

    // Coroutines 지원 시 (SuspendedQuery)
    implementation("io.bluetape4k:bluetape4k-coroutines:${version}")
}
```

## 기본 사용법

### 1. ExposedRepository 구현

```kotlin
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.repository.ExposedRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

data class UserRecord(
    override val id: Long,
    val name: String,
    val email: String,
): HasIdentifier<Long>

object UserTable: LongIdTable("users") {
    val name = varchar("name", 100)
    val email = varchar("email", 200)
}

class UserRepository: ExposedRepository<UserRecord, Long> {

    override val table = UserTable

    override fun ResultRow.toEntity() = UserRecord(
        id = this[UserTable.id].value,
        name = this[UserTable.name],
        email = this[UserTable.email],
    )

    fun save(user: UserRecord): UserRecord {
        val id = UserTable.insert {
            it[name] = user.name
            it[email] = user.email
        } get UserTable.id
        return user.copy(id = id.value)
    }
}

// 사용
transaction {
    val repo = UserRepository()
    val user = repo.save(UserRecord(0L, "홍길동", "hong@example.com"))

    val found = repo.findById(user.id)
    val page = repo.findPage(pageNumber = 0, pageSize = 20)
    println("총 레코드: ${page.totalCount}, 총 페이지: ${page.totalPages}")
}
```

### 2. SoftDeletedRepository 구현

```kotlin
import io.bluetape4k.exposed.dao.id.SoftDeletedIdTable
import io.bluetape4k.exposed.repository.SoftDeletedRepository

object PostTable: SoftDeletedIdTable<Long>("posts", LongIdTable("posts")) {
    val title = varchar("title", 255)
    val content = text("content")
}

class PostRepository: SoftDeletedRepository<PostRecord, Long> {
    override val table = PostTable

    override fun ResultRow.toEntity() = PostRecord(
        id = this[PostTable.id].value,
        title = this[PostTable.title],
        content = this[PostTable.content],
        isDeleted = this[PostTable.isDeleted],
    )
}

transaction {
    val repo = PostRepository()

    // Soft delete
    repo.softDeleteById(postId)

    // 활성 레코드만 조회
    val activePosts = repo.findActive()
}
```

### 3. Coroutines 기반 쿼리 (SuspendedQuery)

```kotlin
import io.bluetape4k.exposed.core.suspendedQuery
import kotlinx.coroutines.Dispatchers

// IO Dispatcher에서 JDBC 쿼리를 suspend 함수로 실행
val users = suspendedQuery(Dispatchers.IO) {
    UserTable.selectAll()
        .where { UserTable.name like "%홍%" }
        .map { it.toEntity() }
}
```

### 4. Virtual Thread 트랜잭션

```kotlin
import io.bluetape4k.exposed.core.transactions.virtualThreadTransaction

// JDK 21+ Virtual Thread에서 트랜잭션 실행
virtualThreadTransaction {
    val users = UserTable.selectAll().map { it.toEntity() }
    println("사용자 수: ${users.size}")
}
```

### 5. 테이블 확장

```kotlin
import io.bluetape4k.exposed.core.implicitSelectAll
import io.bluetape4k.exposed.core.dropAndCreate

// 여러 테이블을 한 번에 selectAll
val rows = listOf(UserTable, OrderTable).implicitSelectAll()

// 테이블 DROP 후 재생성 (테스트용)
SchemaUtils.dropAndCreate(UserTable, OrderTable)
```

### 6. ExposedPage — 페이징 결과

```kotlin
// ExposedRepository.findPage() 사용
transaction {
    val repo = UserRepository()
    val page = repo.findPage(
        pageNumber = 0,
        pageSize = 20,
        sortOrder = SortOrder.ASC
    ) { UserTable.name like "홍%" }

    println("전체 수: ${page.totalCount}")
    println("현재 페이지: ${page.pageNumber}")
    println("전체 페이지: ${page.totalPages}")
    println("마지막 페이지: ${page.isLast}")
    page.content.forEach { println(it) }
}
```

## ExposedRepository 주요 메서드

| 메서드                                   | 설명                   |
|---------------------------------------|----------------------|
| `count()`                             | 전체 레코드 수             |
| `countBy(predicate)`                  | 조건에 맞는 레코드 수         |
| `existsById(id)`                      | ID로 존재 여부 확인         |
| `findById(id)`                        | ID로 단건 조회            |
| `findByIdOrNull(id)`                  | ID로 단건 조회 (없으면 null) |
| `findAll(limit, offset, ...)`         | 전체 조회 (페이징/정렬 지원)    |
| `findPage(pageNumber, pageSize, ...)` | 페이징 조회               |
| `deleteById(id)`                      | ID로 삭제               |
| `deleteAll(op)`                       | 조건에 맞는 레코드 삭제        |
| `updateById(id, ...)`                 | ID로 수정               |
| `batchInsert(entities, ...)`          | 배치 삽입                |
| `batchUpsert(entities, ...)`          | 배치 Upsert            |

## 주요 파일/클래스 목록

| 파일                                              | 설명                        |
|-------------------------------------------------|---------------------------|
| `repository/ExposedRepository.kt`               | JDBC Repository 기본 인터페이스  |
| `repository/SoftDeletedRepository.kt`           | Soft Delete 지원 Repository |
| `core/SuspendedQuery.kt`                        | suspend 함수로 JDBC 쿼리 실행    |
| `core/transactions/VirtualThreadTransaction.kt` | Virtual Thread 기반 트랜잭션    |
| `core/ImplecitSelectAll.kt`                     | 여러 테이블 묵시적 전체 조회          |
| `core/TableExtensions.kt`                       | 테이블 확장 함수                 |
| `core/SchemaUtilsExtensions.kt`                 | SchemaUtils 확장 함수         |

## 테스트

```bash
./gradlew :bluetape4k-exposed-jdbc:test
```

## 참고

- [JetBrains Exposed JDBC](https://github.com/JetBrains/Exposed/wiki/DSL)
- [bluetape4k-exposed-core](../exposed-core)
- [bluetape4k-exposed-dao](../exposed-dao)
