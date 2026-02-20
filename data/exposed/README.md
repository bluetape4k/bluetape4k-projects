# Module bluetape4k-exposed

JetBrains Exposed 사용 시 자주 반복되는 패턴을 줄여주는 Kotlin 확장 라이브러리입니다.

## 개요

`bluetape4k-exposed`는 [JetBrains Exposed](https://github.com/JetBrains/Exposed) ORM을 Kotlin 환경에서 더 편리하게 사용할 수 있도록 다양한 확장 함수와 유틸리티를 제공합니다.

### 주요 기능

- **Table/Column 확장**: 테이블/컬럼 정의 및 조회 편의 함수
- **DAO 확장**: Entity/EntityClass 보조 기능
- **ID Table 지원**: Snowflake, KSUID, UUID 등 ID 전략 테이블
- **Soft Delete 지원**: 소프트 삭제 테이블/리포지토리 패턴
- **암호화 컬럼 타입**: 문자열/바이너리 암복호화 컬럼
- **압축 컬럼 타입**: 바이너리 데이터 압축 저장 컬럼
- **Repository 패턴**: 범용 CRUD Repository 인터페이스
- **Coroutines 지원**: Suspend 함수 기반 비동기 쿼리

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed:${version}")

    // Exposed 모듈 (필요에 따라 추가)
    implementation("org.jetbrains.exposed:exposed-core")
    implementation("org.jetbrains.exposed:exposed-dao")
    implementation("org.jetbrains.exposed:exposed-jdbc")
}
```

## 기본 사용법

### 1. Repository 패턴

`ExposedRepository` 인터페이스를 구현하여 CRUD 작업을 간소화합니다.

```kotlin
import io.bluetape4k.exposed.repository.ExposedRepository
import io.bluetape4k.exposed.core.HasIdentifier
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable

// 엔티티 정의
data class User(
    override val id: Long,
    val name: String,
    val email: String,
): HasIdentifier<Long>

// 테이블 정의
object Users: IdTable<Long>("users") {
    val name = varchar("name", 100)
    val email = varchar("email", 255)
}

// Repository 구현
class UserRepository: ExposedRepository<User, Long> {
    override val table = Users

    override fun ResultRow.toEntity(): User = User(
        id = this[Users.id].value,
        name = this[Users.name],
        email = this[Users.email]
    )
}

// 사용 예시
val repo = UserRepository()

// 조회
val user = repo.findById(1L)
val allUsers = repo.findAll()
val activeUsers = repo.findBy { Users.active eq true }

// 존재 확인
val exists = repo.existsById(1L)

// 개수 조회
val count = repo.count()
val activeCount = repo.countBy { Users.active eq true }

// 삭제
repo.deleteById(1L)
```

### 2. ID Table 확장

다양한 ID 생성 전략을 지원하는 테이블 베이스 클래스를 제공합니다.

#### SnowflakeIdTable

```kotlin
import io.bluetape4k.exposed.dao.id.SnowflakeIdTable
import io.bluetape4k.exposed.dao.id.SnowflakeIdEntity
import io.bluetape4k.exposed.dao.id.SnowflakeIdEntityClass

object Orders: SnowflakeIdTable("orders") {
    val customerId = long("customer_id")
    val amount = decimal("amount", 10, 2)
}

class Order(id: EntityID<Long>): SnowflakeIdEntity(id) {
    var customerId by Orders.customerId
    var amount by Orders.amount

    companion object: SnowflakeIdEntityClass<Order>(Orders)
}
```

#### KSUID Table

```kotlin
import io.bluetape4k.exposed.dao.id.KsuidTable
import io.bluetape4k.exposed.dao.id.KsuidMillisTable

object Sessions: KsuidTable("sessions") {
    val userId = long("user_id")
    val token = varchar("token", 255)
}
```

#### TimebasedUUID Table

```kotlin
import io.bluetape4k.exposed.dao.id.TimebasedUUIDTable
import io.bluetape4k.exposed.dao.id.TimebasedUUIDBase62Table

object Events: TimebasedUUIDTable("events") {
    val type = varchar("type", 50)
    val payload = text("payload")
}
```

### 3. Soft Delete 패턴

논리 삭제(Soft Delete)를 지원하는 테이블과 리포지토리를 제공합니다.

```kotlin
import io.bluetape4k.exposed.dao.id.SoftDeletedIdTable
import io.bluetape4k.exposed.repository.SoftDeletedRepository

object Documents: SoftDeletedIdTable<Long>("documents") {
    val title = varchar("title", 255)
    val content = text("content")
    // isDeleted 컬럼이 자동으로 추가됨
}

class DocumentRepository: SoftDeletedRepository<Document, Long> {
    override val table = Documents
    override fun ResultRow.toEntity(): Document = ...
}

// 사용 시 삭제된 항목은 자동으로 제외됨
val activeDocs = repo.findAll()  // is_deleted = false 만 조회
```

### 4. 암호화 컬럼 타입

민감한 데이터를 자동으로 암호화하여 저장합니다.

```kotlin
import io.bluetape4k.exposed.core.encrypt.encryptedVarChar
import io.bluetape4k.exposed.core.encrypt.encryptedBlob
import io.bluetape4k.exposed.core.encrypt.encryptedBinary

object SecureData: IdTable<Long>("secure_data") {
    // 암호화된 VARCHAR 컬럼
    val creditCard = encryptedVarChar("credit_card", colLength = 512)

    // 암호화된 BLOB 컬럼
    val privateKey = encryptedBlob("private_key")

    // 암호화된 BINARY 컬럼
    val apiKey = encryptedBinary("api_key", 256)
}
```

### 5. 압축 컬럼 타입

대용량 바이너리 데이터를 압축하여 저장합니다.

```kotlin
import io.bluetape4k.exposed.core.compress.compressedBinary
import io.bluetape4k.exposed.core.compress.compressedBlob

object Files: IdTable<Long>("files") {
    val name = varchar("name", 255)

    // 압축된 BINARY 컬럼
    val content = compressedBinary("content", Int.MAX_VALUE)

    // 압축된 BLOB 컬럼
    val largeData = compressedBlob("large_data")
}
```

### 6. Table/Column 확장 함수

```kotlin
import io.bluetape4k.exposed.core.*

// 테이블 메타데이터 조회
val columns = Users.getColumnMetadata()
val indices = Users.getIndices()
val pk = Users.getPrimaryKeyMetadata()
val sequences = Users.getSequences()

// ResultRow 확장
val user = resultRow.toUser()
```

### 7. Coroutines 지원

suspend 함수를 통한 비동기 쿼리를 지원합니다.

```kotlin
import io.bluetape4k.exposed.core.SuspendedQuery

val users = suspendedQuery {
    Users.selectAll().map { it.toUser() }
}
```

### 8. Virtual Thread 지원

Java 21+ Virtual Thread에서 실행되는 트랜잭션을 지원합니다.

```kotlin
import io.bluetape4k.exposed.core.transactions.virtualThreadTransaction

virtualThreadTransaction {
    // Virtual Thread에서 실행되는 트랜잭션
    Users.selectAll().toList()
}
```

## 주요 파일/클래스 목록

### Core (core/)

| 파일                                  | 설명                  |
|-------------------------------------|---------------------|
| `TableExtensions.kt`                | 테이블 메타데이터 조회 확장 함수  |
| `ColumnExtensions.kt`               | 컬럼 정의 확장 함수         |
| `ResultRowExtensions.kt`            | ResultRow 변환 확장 함수  |
| `SchemaUtilsExtensions.kt`          | 스키마 유틸리티 확장         |
| `SuspendedQuery.kt`                 | suspend 함수 기반 쿼리    |
| `VirtualThreadTransaction.kt`       | Virtual Thread 트랜잭션 |
| `BatchInsertOnConflictDoNothing.kt` | 배치 삽입 확장            |

### Core - 암호화 (core/encrypt/)

| 파일                              | 설명             |
|---------------------------------|----------------|
| `EncryptedVarCharColumnType.kt` | VARCHAR 암호화 컬럼 |
| `EncryptedBinaryColumnType.kt`  | BINARY 암호화 컬럼  |
| `EncryptedBlobColumnType.kt`    | BLOB 암호화 컬럼    |

### Core - 압축 (core/compress/)

| 파일                              | 설명           |
|---------------------------------|--------------|
| `CompressedBinaryColumnType.kt` | BINARY 압축 컬럼 |
| `CompressedBlobColumnType.kt`   | BLOB 압축 컬럼   |

### Core - 직렬화 (core/serializable/)

| 파일                                    | 설명            |
|---------------------------------------|---------------|
| `BinarySerializedBinaryColumnType.kt` | 직렬화 BINARY 컬럼 |
| `BinarySerializedBlobColumnType.kt`   | 직렬화 BLOB 컬럼   |

### DAO (dao/)

| 파일                    | 설명                   |
|-----------------------|----------------------|
| `EntityExtensions.kt` | Entity 확장 함수         |
| `StringEntity.kt`     | String ID Entity 베이스 |

### DAO - ID (dao/id/)

| 파일                            | 설명                     |
|-------------------------------|------------------------|
| `SnowflakeIdTable.kt`         | Snowflake ID 테이블/엔티티   |
| `KsuidTable.kt`               | KSUID 테이블              |
| `KsuidMillisTable.kt`         | KSUID Milliseconds 테이블 |
| `TimebasedUUIDTable.kt`       | Time-based UUID 테이블    |
| `TimebasedUUIDBase62Table.kt` | Base62 인코딩 UUID 테이블    |
| `SoftDeletedIdTable.kt`       | Soft Delete 테이블 베이스    |

### Repository (repository/)

| 파일                         | 설명                     |
|----------------------------|------------------------|
| `ExposedRepository.kt`     | 범용 Repository 인터페이스    |
| `SoftDeletedRepository.kt` | Soft Delete Repository |

## 테스트

```bash
# 모든 테스트 실행
./gradlew :bluetape4k-exposed:test

# 특정 테스트 실행
./gradlew :bluetape4k-exposed:test --tests "io.bluetape4k.exposed.*"
```

## 참고

- [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- [Exposed Wiki](https://github.com/JetBrains/Exposed/wiki)
- [Exposed DAO](https://github.com/JetBrains/Exposed/wiki/DAO)
- [Exposed DSL](https://github.com/JetBrains/Exposed/wiki/DSL)
