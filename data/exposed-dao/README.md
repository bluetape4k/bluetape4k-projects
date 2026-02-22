# Module bluetape4k-exposed-dao

JetBrains Exposed DAO 계층을 위한 엔티티 확장, String 기반 엔티티, 그리고 다양한 클라이언트 ID 전략을 사용하는 IdTable 구현을 제공합니다.

## 개요

`bluetape4k-exposed-dao`는 다음을 제공합니다:

- **DAO 확장 함수**: `idEquals`, `idHashCode`, `entityToStringBuilder` 등 Entity 공통 구현 보조
- **StringEntity**: `String` 타입 기본 키를 가진 DAO Entity
- **커스텀 IdTable**: KSUID, Snowflake, Timebased UUID, Soft Delete 등 다양한 ID 전략
- `bluetape4k-exposed-core`를 기반으로 하며, DAO 레이어에서만 필요한 기능을 분리

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-dao:${version}")
}
```

## 기본 사용법

### 1. DAO Entity 공통 구현

```kotlin
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.entityToStringBuilder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object UserTable: LongIdTable("users") {
    val name = varchar("name", 100)
    val email = varchar("email", 200)
}

class UserEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<UserEntity>(UserTable)

    var name by UserTable.name
    var email by UserTable.email

    // idEquals/idHashCode 로 ID 기반 equals/hashCode 자동 구현
    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()

    // entityToStringBuilder 로 편리한 toString
    override fun toString(): String = entityToStringBuilder()
        .add("name", name)
        .add("email", email)
        .toString()
}
```

### 2. StringEntity (String PK)

```kotlin
import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object TagTable: StringIdTable("tags") {
    val description = text("description").nullable()
}

class TagEntity(id: EntityID<String>): StringEntity(id) {
    companion object: StringEntityClass<TagEntity>(TagTable)

    var description by TagTable.description
}

// 사용
val tag = TagEntity.new("kotlin") {
    description = "Kotlin 관련 태그"
}
```

### 3. KSUID 기반 IdTable

```kotlin
import io.bluetape4k.exposed.dao.id.KsuidTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

// KSUID를 PK로 사용하는 테이블 (시간 순서 보장)
object OrderTable: KsuidTable("orders") {
    val amount = decimal("amount", 10, 2)
    val status = varchar("status", 20)
}

class OrderEntity(id: EntityID<String>): Entity<String>(id) {
    companion object: EntityClass<String, OrderEntity>(OrderTable)

    var amount by OrderTable.amount
    var status by OrderTable.status
}

// insert 시 KSUID 자동 생성
val order = OrderEntity.new {
    amount = 15000.toBigDecimal()
    status = "PENDING"
}
println(order.id.value) // "2Dgh3kZ..." (KSUID)
```

### 4. Snowflake ID 기반 IdTable

```kotlin
import io.bluetape4k.exposed.dao.id.SnowflakeIdTable

// Snowflake ID(Long)를 PK로 사용
object EventTable: SnowflakeIdTable("events") {
    val type = varchar("type", 50)
    val payload = text("payload")
}
```

### 5. Timebased UUID 기반 IdTable

```kotlin
import io.bluetape4k.exposed.dao.id.TimebasedUUIDTable
import io.bluetape4k.exposed.dao.id.TimebasedUUIDBase62Table

// UUID v1 (시간 기반) PK
object SessionTable: TimebasedUUIDTable("sessions") {
    val userId = long("user_id")
    val expiresAt = long("expires_at")
}

// Base62 인코딩된 UUID PK (URL-safe, 22자)
object TokenTable: TimebasedUUIDBase62Table("tokens") {
    val userId = long("user_id")
    val scope = varchar("scope", 100)
}
```

### 6. Soft Delete 지원 IdTable

```kotlin
import io.bluetape4k.exposed.dao.id.SoftDeletedIdTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

// isDeleted 컬럼이 자동으로 추가되는 테이블
object PostTable: SoftDeletedIdTable<Long>("posts", LongIdTable("posts")) {
    val title = varchar("title", 255)
    val content = text("content")
}

// soft delete
transaction {
    PostTable.update({ PostTable.id eq postId }) {
        it[isDeleted] = true
    }
}

// 활성 레코드만 조회
transaction {
    PostTable.selectAll()
        .where { PostTable.isDeleted eq false }
        .map { it[PostTable.title] }
}
```

## 주요 파일/클래스 목록

| 파일                                   | 설명                                                            |
|--------------------------------------|---------------------------------------------------------------|
| `EntityExtensions.kt`                | `idEquals`, `idHashCode`, `entityToStringBuilder` 등 Entity 보조 |
| `StringEntity.kt`                    | String PK 기반 Entity/EntityClass                               |
| `dao/id/KsuidTable.kt`               | KSUID PK IdTable                                              |
| `dao/id/KsuidMillisTable.kt`         | KSUID Millis PK IdTable                                       |
| `dao/id/SnowflakeIdTable.kt`         | Snowflake Long PK IdTable                                     |
| `dao/id/TimebasedUUIDTable.kt`       | Timebased UUID PK IdTable                                     |
| `dao/id/TimebasedUUIDBase62Table.kt` | Timebased UUID Base62 인코딩 PK IdTable                          |
| `dao/id/SoftDeletedIdTable.kt`       | `isDeleted` 컬럼 포함 Soft Delete IdTable                         |

## ID 전략 비교

| IdTable                    | PK 타입    | 길이  | 특징                |
|----------------------------|----------|-----|-------------------|
| `KsuidTable`               | `String` | 27자 | 시간 정렬, URL-safe   |
| `KsuidMillisTable`         | `String` | 27자 | 밀리초 정밀도 KSUID     |
| `SnowflakeIdTable`         | `Long`   | -   | 분산 환경, 고성능        |
| `TimebasedUUIDTable`       | `UUID`   | 36자 | UUID v1 표준 호환     |
| `TimebasedUUIDBase62Table` | `String` | 22자 | UUID를 Base62로 인코딩 |
| `SoftDeletedIdTable`       | 제네릭      | -   | `isDeleted` 컬럼 포함 |

## 테스트

```bash
./gradlew :bluetape4k-exposed-dao:test
```

## 참고

- [JetBrains Exposed DAO](https://github.com/JetBrains/Exposed/wiki/DAO)
- [bluetape4k-exposed-core](../exposed-core)
- [bluetape4k-idgenerators](../../../utils/idgenerators)
