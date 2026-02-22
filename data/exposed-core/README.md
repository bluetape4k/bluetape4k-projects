# Module bluetape4k-exposed-core

JetBrains Exposed의 핵심 컬럼 타입, 확장 함수, Repository 공통 인터페이스를 제공하는 기반 모듈입니다. JDBC 의존 없이 사용할 수 있어 R2DBC, 직렬화, 암호화 등 다양한 상위 모듈에서 공유됩니다.

## 개요

`bluetape4k-exposed-core`는 다음을 제공합니다:

- **커스텀 컬럼 타입**: 압축(LZ4/Snappy/Zstd), 암호화, 직렬화(Kryo/Fory) 기반의 Binary/Blob 컬럼
- **컬럼 확장 함수**: 클라이언트 측 ID 생성(`timebasedGenerated`, `snowflakeGenerated`, `ksuidGenerated` 등)
- **ResultRow 확장**: `getOrNull`, `toMap` 등 ResultRow 처리 보조
- **Blob 확장**: `ExposedBlob` 유틸 함수
- **배치 삽입**: `BatchInsertOnConflictDoNothing` (중복 무시 배치 삽입)
- **공통 인터페이스**: `HasIdentifier<ID>`, `ExposedPage<T>`

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-core:${version}")

    // 압축 컬럼 타입 사용 시
    implementation("io.bluetape4k:bluetape4k-io:${version}")

    // 암호화 컬럼 타입 사용 시
    implementation("io.bluetape4k:bluetape4k-crypto:${version}")
}
```

## 기본 사용법

### 1. 클라이언트 측 ID 자동 생성 컬럼

```kotlin
import io.bluetape4k.exposed.core.ksuidGenerated
import io.bluetape4k.exposed.core.snowflakeGenerated
import io.bluetape4k.exposed.core.timebasedGenerated
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object Orders: IntIdTable("orders") {
    // 클라이언트에서 Timebased UUID 자동 생성
    val trackingId = javaUUID("tracking_id").timebasedGenerated()

    // 클라이언트에서 Snowflake ID 자동 생성
    val snowflakeId = long("snowflake_id").snowflakeGenerated()

    // 클라이언트에서 KSUID 자동 생성
    val ksuid = varchar("ksuid", 27).ksuidGenerated()

    val name = varchar("name", 255)
}
```

### 2. 압축 컬럼 타입

```kotlin
import io.bluetape4k.exposed.core.compress.compressedBinary
import io.bluetape4k.exposed.core.compress.compressedBlob
import io.bluetape4k.io.compressor.Compressors
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object Documents: LongIdTable("documents") {
    val title = varchar("title", 255)

    // LZ4 압축으로 Binary 저장
    val contentLz4 = compressedBinary("content_lz4", 65535, Compressors.LZ4)

    // Zstd 압축으로 Blob 저장
    val contentZstd = compressedBlob("content_zstd", Compressors.Zstd).nullable()
}
```

### 3. 암호화 컬럼 타입

```kotlin
import io.bluetape4k.exposed.core.encrypt.encryptedVarChar
import io.bluetape4k.exposed.core.encrypt.encryptedBinary
import io.bluetape4k.crypto.encrypt.Encryptors
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object Users: LongIdTable("users") {
    val name = varchar("name", 100)

    // AES 암호화로 varchar 저장
    val ssn = encryptedVarChar("ssn", 512, Encryptors.AES)

    // 암호화된 Binary 저장
    val secret = encryptedBinary("secret", 1024, Encryptors.AES).nullable()
}
```

### 4. 직렬화 컬럼 타입

```kotlin
import io.bluetape4k.exposed.core.serializable.binarySerializedBinary
import io.bluetape4k.io.serializer.BinarySerializers
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

data class UserProfile(val age: Int, val tags: List<String>)

object Users: LongIdTable("users") {
    val name = varchar("name", 100)

    // Kryo 직렬화로 Binary 저장
    val profile = binarySerializedBinary<UserProfile>(
        "profile", 4096, BinarySerializers.Kryo
    ).nullable()
}
```

### 5. 중복 무시 배치 삽입

```kotlin
import io.bluetape4k.exposed.core.BatchInsertOnConflictDoNothing
import org.jetbrains.exposed.v1.jdbc.statements.BatchInsertBlockingExecutable

val executable = BatchInsertBlockingExecutable(
    statement = BatchInsertOnConflictDoNothing(MyTable)
)
executable.run {
    statement.addBatch()
    statement[MyTable.uniqueKey] = "key1"
    execute(transaction)
}
```

### 6. HasIdentifier 인터페이스

```kotlin
import io.bluetape4k.exposed.core.HasIdentifier

// ID를 가진 엔티티 인터페이스
data class UserRecord(
    override val id: Long,
    val name: String,
    val email: String
): HasIdentifier<Long>
```

### 7. ExposedPage (페이징 결과)

```kotlin
import io.bluetape4k.exposed.repository.ExposedPage

// 페이징 결과 래퍼
val page: ExposedPage<UserRecord> = ExposedPage(
    content = users,
    totalCount = 100L,
    pageNumber = 0,
    pageSize = 20
)

println("총 페이지: ${page.totalPages}")
println("마지막 페이지: ${page.isLast}")
```

## 주요 파일/클래스 목록

| 파일                                                 | 설명                     |
|----------------------------------------------------|------------------------|
| `HasIdentifier.kt`                                 | ID를 가진 엔티티 공통 인터페이스    |
| `ColumnExtensions.kt`                              | 클라이언트 측 ID 자동 생성 확장 함수 |
| `ExposedColumnSupports.kt`                         | 컬럼 타입 관련 지원 함수         |
| `ResultRowExtensions.kt`                           | ResultRow 처리 확장 함수     |
| `BatchInsertOnConflictDoNothing.kt`                | 중복 무시 배치 삽입            |
| `statements/api/ExposedBlobExtensions.kt`          | ExposedBlob 유틸 함수      |
| `compress/CompressedBinaryColumnType.kt`           | 압축 Binary 컬럼 타입        |
| `compress/CompressedBlobColumnType.kt`             | 압축 Blob 컬럼 타입          |
| `encrypt/EncryptedVarCharColumnType.kt`            | 암호화 VarChar 컬럼 타입      |
| `encrypt/EncryptedBinaryColumnType.kt`             | 암호화 Binary 컬럼 타입       |
| `encrypt/EncryptedBlobColumnType.kt`               | 암호화 Blob 컬럼 타입         |
| `serializable/BinarySerializedBinaryColumnType.kt` | 직렬화 Binary 컬럼 타입       |
| `serializable/BinarySerializedBlobColumnType.kt`   | 직렬화 Blob 컬럼 타입         |
| `repository/ExposedPage.kt`                        | 페이징 결과 데이터 클래스         |

## 테스트

```bash
./gradlew :bluetape4k-exposed-core:test
```

## 참고

- [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- [bluetape4k-io (압축/직렬화)](../../../io/io)
- [bluetape4k-crypto (암호화)](../../../io/crypto)
- [bluetape4k-idgenerators (ID 생성)](../../../utils/idgenerators)
