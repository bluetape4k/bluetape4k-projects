# Module bluetape4k-exposed-r2dbc

Exposed R2DBC 환경에서 사용할 수 있는 확장 함수와 Repository 패턴을 제공합니다.

## 개요

`bluetape4k-exposed-r2dbc`는 JetBrains Exposed의 R2DBC(Reactive Relational Database Connectivity) 드라이버를 사용하여
비동기/반응형 데이터베이스 작업을 수행할 수 있는 확장 기능을 제공합니다. Kotlin Coroutines와 완벽하게 호환됩니다.

### 주요 기능

- **Repository 패턴**: `R2dbcRepository<ID, T, E>`, `SoftDeletedR2dbcRepository<ID, T, E>` 인터페이스
- **Flow 기반 조회**: `findAll`, `findBy`, `findByField` 등이 `Flow<E>` 반환
- **Batch Insert 지원**: 충돌 무시 배치 삽입(`BatchInsertOnConflictDoNothing`) 패턴
- **Coroutines 친화 API**: 모든 단건 조회/변경 연산이 `suspend` 함수
- **Soft Delete 지원**: `SoftDeletedR2dbcRepository`
- **가상 스레드 트랜잭션**: `virtualThreadTransaction` — Java 21 Virtual Thread 기반 R2DBC 트랜잭션 실행
- **R2DBC Readable 확장**: `Readable.getString`, `Readable.getLong` 등 타입 안전 컬럼 값 조회 확장 함수
- **SELECT \* 지원**: `ImplicitQuery` / `FieldSet.selectImplicitAll()` — `SELECT *` SQL 생성
- **테이블 메타데이터 조회**: `Table.suspendColumnMetadata()`, `suspendIndexes()` 등 비동기 메타데이터 API

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-r2dbc:${version}")

    // R2DBC 드라이버 (예시)
    implementation("org.postgresql:r2dbc-postgresql:1.0.5.RELEASE")
    // 또는
    implementation("io.r2dbc:r2dbc-h2:1.0.0.RELEASE")
}
```

## 기본 사용법

### 1. R2dbcRepository 구현

```kotlin
import io.bluetape4k.exposed.r2dbc.repository.LongR2dbcRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

data class ActorRecord(
    val id: Long = 0L,
    val firstName: String,
    val lastName: String,
)

object ActorTable : LongIdTable("actors") {
    val firstName = varchar("first_name", 50)
    val lastName  = varchar("last_name",  50)
}

class ActorRepository : LongR2dbcRepository<ActorTable, ActorRecord> {
    override val table = ActorTable

    override suspend fun ResultRow.toEntity() = ActorRecord(
        id        = this[ActorTable.id].value,
        firstName = this[ActorTable.firstName],
        lastName  = this[ActorTable.lastName],
    )

    suspend fun save(record: ActorRecord): ActorRecord {
        val id = ActorTable.insertAndGetId {
            it[firstName] = record.firstName
            it[lastName]  = record.lastName
        }
        return record.copy(id = id.value)
    }
}

// 사용 예시
suspendTransaction {
    val repo = ActorRepository()
    val saved = repo.save(ActorRecord(firstName = "Johnny", lastName = "Depp"))

    val found = repo.findById(saved.id)                              // suspend, 없으면 예외
    val foundOrNull = repo.findByIdOrNull(saved.id)                  // suspend, 없으면 null
    val all = repo.findAll(limit = 10).toList()                      // Flow<E>
    val byName = repo.findBy { ActorTable.lastName eq "Depp" }.toList()
    val page = repo.findPage(pageNumber = 0, pageSize = 20)          // suspend
}
```

### 2. SoftDeletedR2dbcRepository 구현

```kotlin
import io.bluetape4k.exposed.core.dao.id.SoftDeletedIdTable
import io.bluetape4k.exposed.r2dbc.repository.LongSoftDeletedR2dbcRepository

object ContactTable : SoftDeletedIdTable<Long>("contacts") {
    override val id = long("id").autoIncrement().entityId()
    val name = varchar("name", 100)
    override val primaryKey = PrimaryKey(id)
}

data class ContactRecord(
    val id: Long = 0L,
    val name: String,
    val isDeleted: Boolean = false,
)

class ContactRepository : LongSoftDeletedR2dbcRepository<ContactTable, ContactRecord> {
    override val table = ContactTable

    override suspend fun ResultRow.toEntity() = ContactRecord(
        id        = this[ContactTable.id].value,
        name      = this[ContactTable.name],
        isDeleted = this[ContactTable.isDeleted],
    )
}

suspendTransaction {
    val repo = ContactRepository()

    // 논리 삭제
    repo.softDeleteById(1L)

    // 활성 레코드만 조회 (isDeleted = false)
    val active = repo.findActive().toList()

    // 삭제된 레코드만 조회
    val deleted = repo.findDeleted().toList()

    // 복원
    repo.restoreById(1L)

    // 활성 레코드 페이징
    val page = repo.findActivePage(pageNumber = 0, pageSize = 20)
}
```

### 3. 배치 삽입 / Upsert

```kotlin
suspendTransaction {
    val repo = ActorRepository()

    // 배치 삽입
    val inserted = repo.batchInsert(actorList) { actor ->
        this[ActorTable.firstName] = actor.firstName
        this[ActorTable.lastName]  = actor.lastName
    }

    // 배치 Upsert
    val upserted = repo.batchUpsert(actorList) { actor ->
        this[ActorTable.firstName] = actor.firstName
        this[ActorTable.lastName]  = actor.lastName
    }
}
```

## R2dbcRepository 주요 메서드

| 메서드                                   | suspend 여부 | 반환 타입          | 설명                     |
|---------------------------------------|------------|----------------|------------------------|
| `count()`                             | suspend    | `Long`         | 전체 레코드 수               |
| `countBy(predicate)`                  | suspend    | `Long`         | 조건에 맞는 레코드 수           |
| `existsById(id)`                      | suspend    | `Boolean`      | ID로 존재 여부 확인           |
| `existsBy(predicate)`                 | suspend    | `Boolean`      | 조건으로 존재 여부 확인          |
| `findById(id)`                        | suspend    | `E`            | ID로 단건 조회 (없으면 예외)     |
| `findByIdOrNull(id)`                  | suspend    | `E?`           | ID로 단건 조회 (없으면 null)   |
| `findAll(limit, offset, ...)`         | —          | `Flow<E>`      | 전체 조회 (페이징/정렬 지원)      |
| `findWithFilters(...)`                | —          | `Flow<E>`      | 다중 조건 AND 조합 조회        |
| `findBy(...)`                         | —          | `Flow<E>`      | `findWithFilters`의 alias |
| `findFirstOrNull(...)`                | suspend    | `E?`           | 조건에 맞는 첫 번째 엔티티        |
| `findLastOrNull(...)`                 | suspend    | `E?`           | 조건에 맞는 마지막 엔티티         |
| `findByField(field, value)`           | —          | `Flow<E>`      | 특정 컬럼 값으로 조회           |
| `findByFieldOrNull(field, value)`     | suspend    | `E?`           | 특정 컬럼 값으로 첫 번째 조회      |
| `findAllByIds(ids)`                   | —          | `Flow<E>`      | 여러 ID로 일괄 조회           |
| `findPage(pageNumber, pageSize, ...)` | suspend    | `ExposedPage<E>` | 페이징 조회               |
| `deleteById(id)`                      | suspend    | `Int`          | ID로 삭제                |
| `deleteAll(op)`                       | suspend    | `Int`          | 조건에 맞는 레코드 삭제          |
| `deleteAllByIds(ids)`                 | suspend    | `Int`          | 여러 ID로 일괄 삭제           |
| `updateById(id, ...)`                 | suspend    | `Int`          | ID로 수정                |
| `updateAll(predicate, ...)`           | suspend    | `Int`          | 조건에 맞는 레코드 일괄 수정       |
| `batchInsert(entities, ...)`          | suspend    | `List<E>`      | 배치 삽입                 |
| `batchUpsert(entities, ...)`          | suspend    | `List<E>`      | 배치 Upsert             |

## SoftDeletedR2dbcRepository 추가 메서드

| 메서드                                         | suspend 여부 | 반환 타입          | 설명                       |
|---------------------------------------------|------------|----------------|--------------------------|
| `softDeleteById(id)`                        | suspend    | `Unit`         | ID로 논리 삭제 (`isDeleted=true`) |
| `restoreById(id)`                           | suspend    | `Unit`         | ID로 논리 삭제 복원             |
| `countActive(predicate)`                    | suspend    | `Long`         | 활성 레코드 수                 |
| `countDeleted(predicate)`                   | suspend    | `Long`         | 삭제된 레코드 수                |
| `findActive(limit, offset, ...)`            | —          | `Flow<E>`      | 활성 레코드만 조회               |
| `findDeleted(limit, offset, ...)`           | —          | `Flow<E>`      | 삭제된 레코드만 조회              |
| `softDeleteAll(predicate)`                  | suspend    | `Int`          | 조건에 맞는 레코드 일괄 논리 삭제      |
| `restoreAll(predicate)`                     | suspend    | `Int`          | 조건에 맞는 레코드 일괄 복원         |
| `findActivePage(pageNumber, pageSize, ...)` | suspend    | `ExposedPage<E>` | 활성 레코드 페이징 조회          |

## 편의 타입 별칭

| 인터페이스                              | 기본키 타입           |
|------------------------------------|------------------|
| `IntR2dbcRepository`               | `Int`            |
| `LongR2dbcRepository`              | `Long`           |
| `UuidR2dbcRepository`              | `kotlin.uuid.Uuid` |
| `UUIDR2dbcRepository`              | `java.util.UUID` |
| `StringR2dbcRepository`            | `String`         |
| `IntSoftDeletedR2dbcRepository`    | `Int`            |
| `LongSoftDeletedR2dbcRepository`   | `Long`           |
| `UuidSoftDeletedR2dbcRepository`   | `kotlin.uuid.Uuid` |
| `UUIDSoftDeletedR2dbcRepository`   | `java.util.UUID` |
| `StringSoftDeletedR2dbcRepository` | `String`         |

## 가상 스레드 트랜잭션

Java 21 Virtual Thread 기반으로 R2DBC 트랜잭션을 실행합니다.

```kotlin
import io.bluetape4k.exposed.r2dbc.virtualThreadTransaction

// 기본 VirtualThreadExecutor 사용
val count = virtualThreadTransaction(db = database) {
    UserTable.selectAll().count()
}

// 커스텀 Executor 사용
val executor = Executors.newSingleThreadExecutor()
val result = virtualThreadTransaction(executor = executor, db = database) {
    UserTable.insert { it[name] = "Alice" }
    UserTable.selectAll().count()
}
```

## R2DBC Readable 컬럼 값 조회

`io.r2dbc.spi.Readable`에 대해 타입 안전 컬럼 값 조회 확장 함수를 제공합니다.

```kotlin
import io.bluetape4k.exposed.r2dbc.getString
import io.bluetape4k.exposed.r2dbc.getLong
import io.bluetape4k.exposed.r2dbc.getLocalDate

// 인덱스 기반 조회
val name: String = readable.getString(0)
val id: Long = readable.getLong(1)

// 컬럼명 기반 조회 (null 가능)
val nickname: String? = readable.getStringOrNull("nickname")
val birthday: LocalDate? = readable.getLocalDateOrNull("birthday")

// ExposedBlob 조회 (suspend 함수)
val blob: ExposedBlob = readable.getExposedBlob("data")
val blobOrNull: ExposedBlob? = readable.getExposedBlobOrNull("data")
```

지원 타입: `String`, `Boolean`, `Char`, `Byte`, `Short`, `Int`, `Long`, `Float`, `Double`,
`BigDecimal`, `ByteArray`, `Date`, `Timestamp`, `Instant`, `LocalDate`, `LocalTime`,
`LocalDateTime`, `OffsetDateTime`, `UUID`, `ExposedBlob`

## SELECT * 지원

명시적 컬럼 목록 대신 `SELECT *`을 생성하는 `ImplicitQuery`를 제공합니다.

```kotlin
import io.bluetape4k.exposed.r2dbc.selectImplicitAll

// SELECT * FROM actors WHERE ...
val rows = ActorTable.selectImplicitAll()
    .where { ActorTable.lastName eq "Depp" }
    .toList()
```

## 주요 파일/클래스 목록

| 파일                                                    | 설명                                   |
|-------------------------------------------------------|--------------------------------------|
| `repository/R2dbcRepository.kt`                       | R2DBC Repository 기본 인터페이스             |
| `repository/SoftDeletedR2dbcRepository.kt`            | Soft Delete R2DBC Repository         |
| `repository/ExposedR2dbcRepository.kt`                | (Deprecated) 구 Repository 인터페이스       |
| `TableExtensions.kt`                                  | 테이블 메타데이터 비동기 확장 함수                  |
| `QueryExtensions.kt`                                  | Flow/Query 확장 함수 (`forEach`, `any` 등) |
| `ReadableExtensions.kt`                               | R2DBC Readable 타입 안전 컬럼 값 조회 확장      |
| `ImplicitSelectAll.kt`                                | `SELECT *` Query 구현 (`ImplicitQuery`) |
| `virtualThreadTransaction.kt`                         | Java 21 Virtual Thread 기반 트랜잭션 실행    |
| `statements/BatchInsertOnConflictDoNothing.kt`        | ON CONFLICT DO NOTHING 배치 삽입        |

## 테스트

```bash
./gradlew :bluetape4k-exposed-r2dbc:test
```

## 참고

- [JetBrains Exposed R2DBC](https://github.com/JetBrains/Exposed)
- [R2DBC Specification](https://r2dbc.io/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
