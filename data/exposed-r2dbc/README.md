# Module bluetape4k-exposed-r2dbc

Exposed R2DBC 환경에서 사용할 수 있는 확장 함수와 Repository 보조 기능을 제공합니다.

## 개요

`bluetape4k-exposed-r2dbc`는 JetBrains Exposed의 R2DBC(Reactive Relational Database Connectivity) 드라이버를 사용하여 비동기/반응형 데이터베이스 작업을 수행할 수 있는 확장 기능을 제공합니다. Kotlin Coroutines와 완벽하게 호환됩니다.

### 주요 기능

- **Table/Query 확장**: R2DBC 쿼리 작성 보조
- **Repository 지원**: 공통/SoftDelete 리포지토리 기반 클래스
- **Batch Insert 지원**: 충돌 무시 배치 삽입 패턴
- **코루틴 친화 API**: 비동기 흐름과의 결합
- **Virtual Thread 지원**: Java 21+ Virtual Thread 트랜잭션

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-r2dbc:${version}")

    // R2DBC 드라이버
    implementation("org.postgresql:r2dbc-postgresql:1.2.0")
    // 또는
    implementation("io.r2dbc:r2dbc-h2:1.0.0.RELEASE")
}
```

## 기본 사용법

### 1. Repository 패턴

```kotlin
import io.bluetape4k.exposed.r2dbc.repository.ExposedR2dbcRepository
import org.jetbrains.exposed.v1.r2dbc.*

// 엔티티 정의
data class User(
    val id: Long,
    val name: String,
    val email: String
)

// Repository 구현
class UserRepository: ExposedR2dbcRepository<User, Long> {
    override val table = Users

    override fun ResultRow.toEntity(): User = User(
        id = this[Users.id].value,
        name = this[Users.name],
        email = this[Users.email]
    )
}

// 사용 예시
val repo = UserRepository()

// 조회 (suspend 함수)
val user = repo.findById(1L)
val allUsers = repo.findAll()

// 조건 검색
val activeUsers = repo.findBy { Users.active eq true }

// 저장
val saved = repo.insert(user)

// 삭제
repo.deleteById(1L)
```

### 2. Soft Delete Repository

```kotlin
import io.bluetape4k.exposed.r2dbc.repository.SoftDeletedR2dbcRepository

class DocumentRepository: SoftDeletedR2dbcRepository<Document, Long> {
    override val table = Documents
    override fun ResultRow.toEntity(): Document = ...
}

// 사용 시 삭제된 항목은 자동으로 제외됨
val activeDocs = repo.findAll()  // is_deleted = false 만 조회
```

### 3. 쿼리 확장

```kotlin
import io.bluetape4k.exposed.r2dbc.*

// 테이블 확장
val exists = Users.suspendExists { Users.id eq 1L }
val count = Users.suspendCount()

// 쿼리 확장
val users = Users
    .selectAll()
    .where { Users.active eq true }
    .suspendMap { it.toUser() }
```

### 4. 배치 삽입

```kotlin
import io.bluetape4k.exposed.r2dbc.statements.batchInsertOnConflictDoNothing

// 충돌 무시 배치 삽입
Users.batchInsertOnConflictDoIgnore(usersList) {
    set(Users.name, it.name)
    set(Users.email, it.email)
}
```

### 5. Virtual Thread 트랜잭션

```kotlin
import io.bluetape4k.exposed.r2dbc.virtualThreadTransaction

virtualThreadTransaction {
    // Virtual Thread에서 실행되는 R2DBC 트랜잭션
    Users.selectAll().toList()
}
```

## 주요 파일/클래스 목록

### Core

| 파일                            | 설명                  |
|-------------------------------|---------------------|
| `TableExtensions.kt`          | 테이블 확장 함수           |
| `QueryExtensions.kt`          | 쿼리 확장 함수            |
| `ImplecitSelectAll.kt`        | 암시적 전체 조회           |
| `ReadableExtensions.kt`       | Readable 확장 함수      |
| `virtualThreadTransaction.kt` | Virtual Thread 트랜잭션 |

### Repository (repository/)

| 파일                              | 설명                           |
|---------------------------------|------------------------------|
| `ExposedR2dbcRepository.kt`     | 범용 R2DBC Repository 인터페이스    |
| `SoftDeletedR2dbcRepository.kt` | Soft Delete R2DBC Repository |

### Statements (statements/)

| 파일                                  | 설명          |
|-------------------------------------|-------------|
| `BatchInsertOnConflictDoNothing.kt` | 충돌 무시 배치 삽입 |

## 테스트

```bash
./gradlew :bluetape4k-exposed-r2dbc:test
```

## 참고

- [JetBrains Exposed R2DBC](https://github.com/JetBrains/Exposed)
- [R2DBC Specification](https://r2dbc.io/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
