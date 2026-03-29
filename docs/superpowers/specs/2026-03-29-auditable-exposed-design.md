# Auditable 패턴 Exposed 모듈 통합 설계

> **작성일**: 2026-03-29
> **원본**: `exposed-workshop/07-jpa/02-convert-jpa-advanced/ex05_auditable`
> **대상 모듈**: `data/exposed-core`, `data/exposed-jdbc`

---

## 1. 배경 및 목표

JPA `@CreatedBy`, `@CreatedDate`, `@LastModifiedBy`,
`@LastModifiedDate` 어노테이션이 제공하는 감사(Auditing) 패턴을 Exposed DSL/DAO 기반으로 제공한다. 워크숍 코드를 라이브러리 품질로 승격하면서 다음 목표를 달성한다:

1. **DSL 테이블 계층** (`AuditableIdTable` 등) -- `exposed-core`
2. **DAO 엔티티 계층** (`AuditableEntity` 등) -- `exposed-dao` (기존 `KsuidEntity`, `SnowflakeIdEntity` 등과 동일 모듈)
3. **Repository 지원** (`AuditableJdbcRepository`) -- `exposed-jdbc`
4. **사용자 컨텍스트** (`UserContext`) -- `exposed-core` (ScopedValue 우선, ThreadLocal fallback)

> **타임스탬프 기준**: 모든 타임스탬프는 **DB 서버의 UTC `CURRENT_TIMESTAMP`** 를 기본으로 한다.
> - `createdAt`: `defaultExpression(CurrentTimestamp)` — DB가 INSERT 시 자동 설정
> - `updatedAt`: Repository DSL 업데이트 시 `it[table.updatedAt] = CurrentTimestamp` 사용 (DB UTC 보장)
> - `createdBy`/`updatedBy` 기본값: `"system"` (`UserContext.DEFAULT_USERNAME`)

---

## 2. 패키지 구조

### 2.1 exposed-core (`io.bluetape4k.exposed.core.auditable`)

```
io.bluetape4k.exposed.core.auditable/
  Auditable.kt              -- Auditable 인터페이스
  UserContext.kt             -- 사용자 컨텍스트 (ScopedValue + ThreadLocal)
  AuditableIdTable.kt        -- abstract AuditableIdTable<ID>
  AuditableIntIdTable.kt     -- Int PK
  AuditableLongIdTable.kt    -- Long PK
  AuditableUUIDTable.kt      -- java.util.UUID PK
```

### 2.2 exposed-dao (`io.bluetape4k.exposed.dao.auditable`)

```
io.bluetape4k.exposed.dao.auditable/
  AuditableEntity.kt         -- abstract AuditableEntity<ID>
  AuditableIntEntity.kt      -- Int PK
  AuditableLongEntity.kt     -- Long PK
  AuditableUUIDEntity.kt     -- UUID PK
  AuditableEntityClass.kt    -- EntityClass 팩토리 (Int/Long/UUID)
```

> 기존 `KsuidEntity`, `SnowflakeIdEntity` 등과 동일한 `exposed-dao` 모듈에 배치하여 DAO 엔티티 계층의 일관성을 유지한다.

### 2.3 exposed-jdbc (`io.bluetape4k.exposed.jdbc.repository`)

```
io.bluetape4k.exposed.jdbc.repository/
  AuditableJdbcRepository.kt -- Auditable 전용 Repository 인터페이스
```

---

## 3. 기존 코드 통합 분석

### 3.1 exposed-core 기존 구조

| 패키지      | 기존 클래스                                                                       | 충돌 여부                                                         |
|----------|------------------------------------------------------------------------------|---------------------------------------------------------------|
| `dao.id` | `KsuidTable`, `SnowflakeIdTable`, `SoftDeletedIdTable`, `TimebasedUUIDTable` | **없음** -- 모두 `IdTable<T>`를 직접 상속. Auditable은 별도 패키지           |
| `core`   | `HasIdentifier`, `ExposedPage`, `ColumnExtensions`                           | **없음** -- `Auditable`은 `HasIdentifier`와 목적이 다름 (감사 필드 vs 식별자) |

### 3.2 exposed-jdbc 기존 구조

| 패키지          | 기존 클래스                                        | 충돌 여부                                                                                |
|--------------|-----------------------------------------------|--------------------------------------------------------------------------------------|
| `repository` | `JdbcRepository`, `SoftDeletedJdbcRepository` | **없음** -- `AuditableJdbcRepository`는 `SoftDeletedJdbcRepository` 패턴을 참조하여 동일 방식으로 추가 |
| (없음)         | --                                            | `auditable` 패키지를 새로 추가하여 `AuditableEntity` 배치                                        |

> **Note**: `AuditableEntity`는 기존 `KsuidEntity`, `SnowflakeIdEntity` 등과 동일하게 **`exposed-dao`
** 에 배치한다. DAO 엔티티 계층의 일관성을 유지하며, `exposed-jdbc`는 `exposed-dao`에 `api` 의존하므로 Repository에서도 접근 가능하다.

### 3.3 exposed-dao 기존 구조 (참조)

| 패키지      | 기존 클래스                                                                 | 비고                         |
|----------|------------------------------------------------------------------------|----------------------------|
| `dao`    | `EntityExtensions` (`idEquals`, `idHashCode`, `entityToStringBuilder`) | `AuditableEntity`에서 그대로 활용 |
| `dao.id` | `KsuidEntity`, `SnowflakeIdEntity` 등                                   | 별도 ID 전략, Auditable과 직교    |

### 3.4 의존성 흐름

```
exposed-core (Auditable, UserContext, AuditableIdTable)
    ↓
exposed-dao (AuditableEntity, AuditableEntityClass -- exposed-core에 api 의존)
    ↓
exposed-jdbc (AuditableJdbcRepository -- exposed-dao에 api 의존)
```

기존 `build.gradle.kts` 의존성 구조와 일치하므로 **추가 의존성 변경 불필요**.

> **Note**: `AuditableIdTable`이 `org.jetbrains.exposed.v1.javatime.timestamp`/`CurrentTimestamp`를 사용하므로, `exposed-core`의
`build.gradle.kts`에 `exposed-java-time` 의존성이 필요하다 (이미 포함된 경우 확인 필요).

---

## 4. UserContext 설계 개선

### 4.1 문제: 워크숍 코드의 한계

워크숍에서는 `ScopedValue`만 사용:

```kotlin
val CURRENT_USER: ScopedValue<String?> = ScopedValue.newInstance()
```

- Java 21에서 `ScopedValue`는 Preview API -- `--enable-preview` 필요
- 일반 Thread / Coroutines `Dispatchers.Default`에서는 `ScopedValue` 전파 안 됨
- `ScopedValue.get()` 바인딩 없으면 `NoSuchElementException`

### 4.2 개선: 듀얼 전략 (ScopedValue 우선 + ThreadLocal fallback)

```kotlin
object UserContext {

    const val DEFAULT_USERNAME = "system"

    /**
     * ScopedValue 기반 사용자 컨텍스트 (Virtual Thread / ScopedValue.where 사용 시).
     * Java 21 Preview API -- 프로젝트는 이미 --enable-preview 활성화 상태.
     */
    val SCOPED_USER: ScopedValue<String> = ScopedValue.newInstance()

    /**
     * ThreadLocal 기반 사용자 컨텍스트 (일반 Thread / Coroutines 사용 시).
     * InheritableThreadLocal로 자식 스레드 전파 지원.
     */
    private val THREAD_LOCAL_USER: InheritableThreadLocal<String?> = InheritableThreadLocal()

    /**
     * ScopedValue 범위 내에서 사용자를 설정하고 블록을 실행합니다.
     * Virtual Thread 환경에서 권장됩니다.
     */
    fun <T> withUser(username: String, block: () -> T): T {
        THREAD_LOCAL_USER.set(username)
        try {
            return ScopedValue.where(SCOPED_USER, username).call(block)
        } finally {
            THREAD_LOCAL_USER.remove()
        }
    }

    /**
     * ThreadLocal 범위 내에서 사용자를 설정하고 블록을 실행합니다.
     * Coroutines / 일반 스레드 환경에서 사용합니다.
     */
    fun <T> withThreadLocalUser(username: String, block: () -> T): T {
        val prev = THREAD_LOCAL_USER.get()
        THREAD_LOCAL_USER.set(username)
        try {
            return block()
        } finally {
            if (prev != null) THREAD_LOCAL_USER.set(prev) else THREAD_LOCAL_USER.remove()
        }
    }

    /**
     * 현재 사용자를 반환합니다.
     * 우선순위: ScopedValue > ThreadLocal > DEFAULT_USERNAME
     */
    fun getCurrentUser(): String {
        // 1. ScopedValue 확인
        runCatching { SCOPED_USER.get() }.getOrNull()?.let { return it }
        // 2. ThreadLocal 확인
        THREAD_LOCAL_USER.get()?.let { return it }
        // 3. 기본값
        return DEFAULT_USERNAME
    }
}
```

### 4.3 Coroutines 확장 (선택적, 추후 `exposed-jdbc`에서)

```kotlin
/**
 * CoroutineContext에 UserContext를 전파하는 Element.
 */
class UserContextElement(val username: String) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<UserContextElement>
}

suspend fun <T> withUserContext(username: String, block: suspend () -> T): T {
    return withContext(UserContextElement(username)) {
        UserContext.withThreadLocalUser(username) { block() }
    }
}
```

> **결정**: Coroutines 확장은 1차 구현에서 제외. `exposed-jdbc`의 `compileOnly(Libs.kotlinx_coroutines_core)` 의존성이 이미 있으므로 추후 추가 용이.

---

## 5. `flush()` 기반 Auditing의 한계 및 개선

### 5.1 워크숍 코드의 문제점

```kotlin
override fun flush(batch: EntityBatchUpdate?): Boolean {
    if (writeValues.isNotEmpty() && createdAt != null) {
        updatedAt = Instant.now()
        updatedBy = UserContext.getCurrentUser()
    }
    if (createdAt == null) {
        createdAt = Instant.now()
        createdBy = UserContext.getCurrentUser()
    }
    return super.flush(batch)
}
```

문제:

1. **`writeValues` 의존** -- Exposed 내부 API에 결합. 버전 업에 취약
2. **생성 판별 로직** -- `createdAt == null`로 신규/수정 구분하는데, DB에서 읽어온 직후 `createdAt != null`이므로 동작은 맞지만 직관적이지 않음
3. **배치 처리** -- `EntityBatchUpdate` 시 `flush()` 호출 타이밍이 다를 수 있음
4. **DSL(non-DAO) 사용 불가** -- `Entity.flush()`는 DAO 전용

### 5.2 개선: 계층별 Auditing 전략

#### A. DSL 레이어 (exposed-core) -- `clientDefault` + `AuditableIdTable`

```kotlin
abstract class AuditableIdTable<ID : Any>(name: String = "") : IdTable<ID>(name) {
    // clientDefault가 항상 값을 제공하므로 non-nullable로 선언
    val createdBy = varchar("created_by", 128)
        .clientDefault { UserContext.getCurrentUser() }

    val createdAt = timestamp("created_at")
        .defaultExpression(CurrentTimestamp)
        .nullable()

    val updatedBy = varchar("updated_by", 128).nullable()

    val updatedAt = timestamp("updated_at").nullable()
}
```

- `clientDefault`로 INSERT 시 `createdBy` 자동 설정
- `createdAt`은 `defaultExpression(CurrentTimestamp)`으로 DB 서버 시간 사용
- `updatedBy`/`updatedAt`은 UPDATE 시 수동 또는 Repository에서 자동 설정

> **서버/클라이언트 시간 일관성**
> - DSL 레이어: `defaultExpression(CurrentTimestamp)` = **DB 서버 시간**
> - DAO 레이어: `flush()`에서 타임스탬프 직접 설정하지 않음 — `createdAt`/`updatedAt` 모두 **DB `CURRENT_TIMESTAMP`** 위임
>
> DAO `flush()`는 `createdBy`/`updatedBy`(사용자 정보)만 설정하고, 타임스탬프(`createdAt`/
`updatedAt`)는 DB 서버 시간에 위임한다. JVM/DB 클럭 불일치 문제가 없으며 전 레이어에서 UTC DB 시간으로 일관성을 유지한다.

#### B. DAO 레이어 (exposed-dao) -- `flush()` 오버라이드 (개선)

타임스탬프(`createdAt`/`updatedAt`)는 DB `CURRENT_TIMESTAMP`로 관리한다. `flush()` 에서는 사용자 정보(`createdBy`/
`updatedBy`)만 설정하고, 타임스탬프는 DB/Repository 레이어에 위임한다.

```kotlin
abstract class AuditableEntity<ID : Any>(id: EntityID<ID>) : Entity<ID>(id), Auditable {

    abstract override var createdBy: String
    abstract override var createdAt: Instant?
    abstract override var updatedBy: String?
    abstract override var updatedAt: Instant?

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        // isNew 플래그를 먼저 캡처하여 생성/수정 분기를 명확히 분리
        val isNew = createdAt == null
        val user = UserContext.getCurrentUser()

        if (isNew) {
            // createdBy만 설정 -- createdAt은 DB defaultExpression(CurrentTimestamp)이 처리
            createdBy = user
        } else if (writeValues.isNotEmpty()) {
            // updatedBy만 설정 -- updatedAt은 AuditableJdbcRepository DSL update에서 CurrentTimestamp로 처리
            updatedBy = user
        }

        return super.flush(batch)
    }

    // TODO: toString() 오버라이드 고려 -- entityToStringBuilder 활용
    //   override fun toString(): String = entityToStringBuilder()

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
}
```

> **타임스탬프 UTC 전략**:
> - `createdAt`: `AuditableIdTable`의 `defaultExpression(CurrentTimestamp)` — DB가 INSERT 시 UTC 시간 자동 설정
> - `updatedAt`: `AuditableJdbcRepository.auditedUpdateById()` 내에서
    `it[table.updatedAt] = CurrentTimestamp` 사용 — DB UTC 보장
> - DAO flush()에서 직접 `Instant.now()`를 사용하지 않으므로 JVM/DB 클럭 불일치 문제 없음

> **`writeValues` 사용 허용 근거**: Exposed DAO 라이브러리의 `Entity` 클래스가 `writeValues`를
`protected`로 노출하며, DAO 확장 패턴에서 표준적으로 사용됨. Exposed 1.0+ 에서 안정화된 API.
>
> **버전 종속성 주의**: `writeValues`는 Exposed `Entity` 클래스의
`protected` 필드로, Exposed 내부 구현에 결합된다. Exposed 메이저 버전 업그레이드 시 API 변경 가능성이 있으므로, 업그레이드 시 이 부분을 우선 검증해야 한다.

#### C. Repository 레이어 (exposed-jdbc) -- UPDATE 시 자동 감사 필드 설정

```kotlin
interface AuditableJdbcRepository<ID : Any, E : Any, T : AuditableIdTable<ID>>
    : JdbcRepository<ID, E> {

    override val table: T

    /**
     * ID로 엔티티를 수정합니다. updatedBy/updatedAt을 자동 설정합니다.
     */
    fun auditedUpdateById(
        id: ID,
        limit: Int? = null,
        updateStatement: T.(UpdateStatement) -> Unit,
    ): Int = table.update(where = { table.id eq id }, limit = limit) {
        it[table.updatedAt] = CurrentTimestamp  // DB UTC CURRENT_TIMESTAMP
        it[table.updatedBy] = UserContext.getCurrentUser()
        updateStatement(it)
    }
}
```

---

## 6. KsuidTable / SnowflakeIdTable 등과의 관계

### 6.1 현재 IdTable 계층

```
IdTable<T>
├── KsuidTable (String PK, ksuidGenerated)
├── KsuidMillisTable (String PK, ksuidMillisGenerated)
├── SnowflakeIdTable (Long PK, snowflakeGenerated)
├── TimebasedUUIDTable (UUID PK, timebasedGenerated)
├── TimebasedUUIDBase62Table (String PK, timebasedGenerated)
├── SoftDeletedIdTable<T> (isDeleted 컬럼 추가)
└── AuditableIdTable<T> (createdBy/createdAt/updatedBy/updatedAt 추가)  ← NEW
```

### 6.2 조합 패턴 -- Auditable + 특수 ID

사용자가 Auditable + Ksuid 조합이 필요한 경우:

**방안 A: 직접 조합 테이블 (권장)**

```kotlin
// 사용자가 직접 조합
open class AuditableKsuidTable(name: String = "") : AuditableIdTable<String>(name) {
    final override val id = varchar("id", 27).ksuidGenerated().entityId()
    override val primaryKey = PrimaryKey(id)
}
```

**방안 B: 라이브러리에서 제공 (1차에서는 제외)**

```
AuditableIdTable<T>
├── AuditableIntIdTable
├── AuditableLongIdTable
├── AuditableUUIDTable
├── AuditableKsuidTable       ← 2차
├── AuditableSnowflakeIdTable ← 2차
```

### 6.3 Auditable + SoftDeleted 조합

두 기능을 동시에 필요로 하는 경우 Kotlin에서 다중 상속이 불가하므로:

```kotlin
// 사용자가 직접 조합 (composition)
abstract class AuditableSoftDeletedIdTable<T : Any>(name: String = "") : AuditableIdTable<T>(name) {
    val isDeleted: Column<Boolean> = bool("is_deleted").default(false)
}
```

> **1차 스코프에서는 조합 클래스 미제공**. 사용자 코드에서 조합하는 예시를 테스트/문서로 제공.

---

## 7. 상세 클래스 설계

### 7.1 Auditable (exposed-core)

```kotlin
package io.bluetape4k.exposed.core.auditable

import java.time.Instant

/**
 * 감사(Audit) 정보를 포함하는 엔티티 계약입니다.
 *
 * JPA의 `@CreatedBy`, `@CreatedDate`, `@LastModifiedBy`, `@LastModifiedDate`에 대응합니다.
 */
interface Auditable {
    /** 엔티티를 생성한 사용자 (clientDefault로 항상 값이 설정됨) */
    val createdBy: String
    /** 엔티티 생성 시각 */
    val createdAt: Instant?
    /** 엔티티를 마지막으로 수정한 사용자 */
    val updatedBy: String?
    /** 엔티티 마지막 수정 시각 */
    val updatedAt: Instant?
}
```

### 7.2 AuditableIdTable (exposed-core)

```kotlin
package io.bluetape4k.exposed.core.auditable

import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * 감사(Audit) 컬럼(`created_by`, `created_at`, `updated_by`, `updated_at`)을 포함하는 [IdTable] 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - `createdBy`는 [UserContext.getCurrentUser]로 INSERT 시 자동 설정됩니다.
 * - `createdAt`은 DB 서버 시각(`CurrentTimestamp`)으로 설정됩니다.
 * - `updatedBy`/`updatedAt`은 UPDATE 시 수동 설정하거나 [AuditableJdbcRepository]를 통해 자동 설정합니다.
 */
abstract class AuditableIdTable<ID : Any>(name: String = "") : IdTable<ID>(name) {

    // clientDefault가 항상 값을 제공하므로 non-nullable로 선언
    val createdBy = varchar("created_by", 128)
        .clientDefault { UserContext.getCurrentUser() }

    val createdAt = timestamp("created_at")
        .defaultExpression(CurrentTimestamp)
        .nullable()

    val updatedBy = varchar("updated_by", 128).nullable()

    val updatedAt = timestamp("updated_at").nullable()
}
```

### 7.3 Concrete Table 변형 (exposed-core)

```kotlin
// AuditableIntIdTable.kt
abstract class AuditableIntIdTable(name: String = "", columnName: String = "id")
    : AuditableIdTable<Int>(name) {
    final override val id = integer(columnName).autoIncrement().entityId()
    override val primaryKey = PrimaryKey(id)
}

// AuditableLongIdTable.kt
abstract class AuditableLongIdTable(name: String = "", columnName: String = "id")
    : AuditableIdTable<Long>(name) {
    final override val id = long(columnName).autoIncrement().entityId()
    override val primaryKey = PrimaryKey(id)
}

// AuditableUUIDTable.kt
abstract class AuditableUUIDTable(name: String = "", columnName: String = "id")
    : AuditableIdTable<java.util.UUID>(name) {
    final override val id = javaUUID(columnName)
        .clientDefault { java.util.UUID.randomUUID() }
        .entityId()
    override val primaryKey = PrimaryKey(id)
}
```

### 7.4 AuditableEntity (exposed-dao)

```kotlin
package io.bluetape4k.exposed.dao.auditable

import io.bluetape4k.exposed.core.auditable.Auditable
import io.bluetape4k.exposed.core.auditable.UserContext
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityBatchUpdate
import java.time.Instant

/**
 * 감사(Audit) 필드를 자동 관리하는 DAO 엔티티 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - `flush()` 시점에 생성/수정 사용자 정보(`createdBy`/`updatedBy`)를 자동 설정합니다.
 * - 타임스탬프(`createdAt`/`updatedAt`)는 DB `CURRENT_TIMESTAMP`에 위임합니다.
 * - `createdAt == null`이면 신규 생성으로 판단하여 `createdBy`를 설정합니다.
 * - 기존 엔티티에서 `writeValues`가 존재하면 수정으로 판단하여 `updatedBy`를 설정합니다.
 *
 * **주의**: `writeValues`는 Exposed `Entity`의 `protected` 필드입니다. Exposed 메이저 버전 업그레이드 시
 * API 변경 가능성이 있으므로 우선 검증 대상입니다.
 *
 * **DAO updatedAt 갭 주의**: `flush()` 단독 호출 시 `updatedAt`은 설정되지 않습니다.
 * `updatedAt` 자동 설정은 `AuditableJdbcRepository.auditedUpdateById()` 사용 시에만 보장됩니다.
 */
abstract class AuditableEntity<ID : Any>(id: EntityID<ID>) : Entity<ID>(id), Auditable {

    abstract override var createdBy: String
    abstract override var createdAt: Instant?
    abstract override var updatedBy: String?
    abstract override var updatedAt: Instant?

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        // isNew 플래그를 먼저 캡처하여 생성/수정 분기를 명확히 분리
        val isNew = createdAt == null
        val user = UserContext.getCurrentUser()

        if (isNew) {
            // createdBy만 설정 -- createdAt은 DB defaultExpression(CurrentTimestamp)이 처리
            createdBy = user
        } else if (writeValues.isNotEmpty()) {
            // updatedBy만 설정 -- updatedAt은 AuditableJdbcRepository DSL update에서 CurrentTimestamp로 처리
            updatedBy = user
        }

        return super.flush(batch)
    }

    // TODO: toString() 오버라이드 고려 -- entityToStringBuilder 활용
    //   override fun toString(): String = entityToStringBuilder()

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
}

abstract class AuditableIntEntity(id: EntityID<Int>) : AuditableEntity<Int>(id)
abstract class AuditableLongEntity(id: EntityID<Long>) : AuditableEntity<Long>(id)
abstract class AuditableUUIDEntity(id: EntityID<java.util.UUID>) : AuditableEntity<java.util.UUID>(id)
```

### 7.5 AuditableEntityClass (exposed-dao)

```kotlin
package io.bluetape4k.exposed.dao.auditable

import io.bluetape4k.exposed.core.auditable.AuditableIntIdTable
import io.bluetape4k.exposed.core.auditable.AuditableLongIdTable
import io.bluetape4k.exposed.core.auditable.AuditableUUIDTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.UUIDEntityClass

open class AuditableIntEntityClass<out E : AuditableIntEntity>(
    table: AuditableIntIdTable,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<Int>) -> E)? = null,
) : IntEntityClass<E>(table, entityType, entityCtor)

open class AuditableLongEntityClass<out E : AuditableLongEntity>(
    table: AuditableLongIdTable,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<Long>) -> E)? = null,
) : LongEntityClass<E>(table, entityType, entityCtor)

open class AuditableUUIDEntityClass<out E : AuditableUUIDEntity>(
    table: AuditableUUIDTable,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<java.util.UUID>) -> E)? = null,
) : UUIDEntityClass<E>(table, entityType, entityCtor)
```

### 7.6 AuditableJdbcRepository (exposed-jdbc)

```kotlin
package io.bluetape4k.exposed.jdbc.repository

import io.bluetape4k.exposed.core.auditable.AuditableIdTable
import io.bluetape4k.exposed.core.auditable.UserContext
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

/**
 * 감사(Audit) 필드를 자동 관리하는 [JdbcRepository] 확장 인터페이스입니다.
 *
 * [AuditableIdTable]의 `updatedBy`/`updatedAt` 컬럼을 UPDATE 시 자동 설정합니다.
 */
interface AuditableJdbcRepository<ID : Any, E : Any, T : AuditableIdTable<ID>>
    : JdbcRepository<ID, E> {

    override val table: T

    /**
     * ID로 엔티티를 수정합니다. `updatedBy`/`updatedAt`을 자동 설정합니다.
     */
    fun auditedUpdateById(
        id: ID,
        limit: Int? = null,
        updateStatement: T.(UpdateStatement) -> Unit,
    ): Int = table.update(where = { table.id eq id }, limit = limit) {
        it[table.updatedAt] = CurrentTimestamp  // DB UTC CURRENT_TIMESTAMP
        it[table.updatedBy] = UserContext.getCurrentUser()
        updateStatement(it)
    }

    /**
     * 조건에 맞는 모든 엔티티를 수정합니다. `updatedBy`/`updatedAt`을 자동 설정합니다.
     */
    fun auditedUpdateAll(
        predicate: () -> Op<Boolean> = { Op.TRUE },
        limit: Int? = null,
        updateStatement: T.(UpdateStatement) -> Unit,
    ): Int = table.update(where = predicate, limit = limit) {
        it[table.updatedAt] = CurrentTimestamp  // DB UTC CURRENT_TIMESTAMP
        it[table.updatedBy] = UserContext.getCurrentUser()
        updateStatement(it)
    }
}

interface IntAuditableJdbcRepository<E : Any, T : AuditableIdTable<Int>>
    : AuditableJdbcRepository<Int, E, T>

interface LongAuditableJdbcRepository<E : Any, T : AuditableIdTable<Long>>
    : AuditableJdbcRepository<Long, E, T>

interface UUIDAuditableJdbcRepository<E : Any, T : AuditableIdTable<java.util.UUID>>
    : AuditableJdbcRepository<java.util.UUID, E, T>
```

---

## 8. 테스트 계획

### 8.1 exposed-jdbc 테스트 (`data/exposed-jdbc/src/test/kotlin/`)

패키지: `io.bluetape4k.exposed.jdbc.repository`

#### A. DSL + Repository 테스트

```kotlin
class AuditableJdbcRepositoryTest : AbstractExposedTest() {

    // 테스트 테이블
    object AuditableActorTable : AuditableLongIdTable("auditable_actors") {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
    }

    // 테스트 레코드
    data class AuditableActorRecord(
        val id: Long = 0L,
        val firstName: String,
        val lastName: String,
        override val createdBy: String = UserContext.DEFAULT_USERNAME,
        override val createdAt: Instant? = null,
        override val updatedBy: String? = null,
        override val updatedAt: Instant? = null,
    ) : Auditable

    // Repository 구현
    val repo = object : LongAuditableJdbcRepository<AuditableActorRecord, AuditableActorTable> {
        override val table = AuditableActorTable
        override fun extractId(entity: AuditableActorRecord) = entity.id
        override fun ResultRow.toEntity() = AuditableActorRecord(...)
    }

    // 테스트 케이스:
    // 1. INSERT 시 createdBy/createdAt 자동 설정
    // 2. INSERT 직후 updatedAt == null 검증 (생성 시 수정 필드 미설정)
    // 3. UserContext.withUser 내 INSERT 시 지정 사용자 반영
    // 4. auditedUpdateById 시 updatedBy/updatedAt 자동 설정
    // 5. 일반 updateById 시 감사 필드 미변경 확인
    // 6. UserContext.withThreadLocalUser 동작 확인
}
```

#### B. DAO Entity 테스트

```kotlin
class AuditableEntityTest : AbstractExposedTest() {

    object Articles : AuditableLongIdTable("articles") {
        val title = varchar("title", 255)
    }

    class Article(id: EntityID<Long>) : AuditableLongEntity(id) {
        companion object : AuditableLongEntityClass<Article>(Articles)
        var title by Articles.title
        override var createdBy by Articles.createdBy
        override var createdAt by Articles.createdAt
        override var updatedBy by Articles.updatedBy
        override var updatedAt by Articles.updatedAt
    }

    // 테스트 케이스:
    // 1. new {} 시 createdBy/createdAt 자동 설정 (flush 시점)
    // 2. new {} 직후 updatedAt == null, updatedBy == null 검증
    // 3. 프로퍼티 수정 후 flush 시 updatedBy/updatedAt 설정
    // 4. UserContext.withUser 내 생성/수정 시 사용자 반영
    // 5. equals/hashCode가 idEquals/idHashCode 기반 동작
}
```

---

## 9. 구현 태스크 목록

### Phase 1: Core (exposed-core)

| #   | 태스크                                                                 | 파일                                               |
|-----|---------------------------------------------------------------------|--------------------------------------------------|
| 1.1 | `Auditable` 인터페이스 생성                                                | `exposed-core/.../auditable/Auditable.kt`        |
| 1.2 | `UserContext` 싱글톤 생성 (ScopedValue + ThreadLocal)                    | `exposed-core/.../auditable/UserContext.kt`      |
| 1.3 | `AuditableIdTable<ID>` 추상 클래스 생성                                    | `exposed-core/.../auditable/AuditableIdTable.kt` |
| 1.4 | `AuditableIntIdTable`, `AuditableLongIdTable`, `AuditableUUIDTable` | `exposed-core/.../auditable/`                    |

### Phase 2: DAO Entity (exposed-dao) + Repository (exposed-jdbc)

| #   | 태스크                                                                               | 파일                                                                                                            |
|-----|-----------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| 2.1 | `AuditableEntity<ID>` 추상 클래스 생성                                                   | `exposed-dao/.../auditable/AuditableEntity.kt`                                                                |
| 2.2 | `AuditableIntEntity`, `AuditableLongEntity`, `AuditableUUIDEntity`                | `exposed-dao/.../auditable/AuditableIntEntity.kt`, `AuditableLongEntity.kt`, `AuditableUUIDEntity.kt` (별도 파일) |
| 2.3 | `AuditableIntEntityClass`, `AuditableLongEntityClass`, `AuditableUUIDEntityClass` | `exposed-dao/.../auditable/AuditableEntityClass.kt`                                                           |
| 2.4 | `AuditableJdbcRepository` 인터페이스 + 편의 타입 별칭                                        | `exposed-jdbc/.../repository/AuditableJdbcRepository.kt`                                                      |

### Phase 3: 테스트

| #   | 태스크                  | 파일                                                         |
|-----|----------------------|------------------------------------------------------------|
| 3.1 | DSL + Repository 테스트 | `exposed-jdbc/src/test/.../AuditableJdbcRepositoryTest.kt` |
| 3.2 | DAO Entity 테스트       | `exposed-jdbc/src/test/.../AuditableEntityTest.kt`         |
| 3.3 | UserContext 단위 테스트   | `exposed-core/src/test/.../UserContextTest.kt`             |

### Phase 4: 문서화

| #   | 태스크                                                                                    |
|-----|----------------------------------------------------------------------------------------|
| 4.1 | exposed-core README 업데이트 (Auditable 섹션 추가)                                             |
| 4.2 | exposed-jdbc README 업데이트 (AuditableEntity, AuditableJdbcRepository 섹션 추가)              |
| 4.3 | 루트 CLAUDE.md Architecture 섹션 업데이트                                                      |
| 4.4 | `exposed-java-time` 의존성 요구사항 명시 (README에 `exposed-jdbc`가 `exposed-java-time`에 의존함을 기재) |

---

## 10. 결정 사항 요약

| 항목                      | 결정                                      | 근거                                                                                                 |
|-------------------------|-----------------------------------------|----------------------------------------------------------------------------------------------------|
| UserContext 전략          | ScopedValue 우선 + ThreadLocal fallback   | Java 21 프로젝트이지만 Coroutines 호환 필요                                                                   |
| `flush()` 유지 여부         | 유지 (DAO 레이어)                            | Exposed DAO 표준 확장 패턴, `writeValues`는 protected API                                                 |
| `flush()` 생성/수정 분리      | `isNew` 플래그 선캡처 후 `if/else if` 분기       | 생성 시 `updatedAt`/`updatedBy` 미설정 보장                                                                |
| `createdBy` nullable 여부 | non-nullable (`clientDefault`가 항상 값 제공) | `.clientDefault { }.nullable()` 체인 순서 문제 회피                                                        |
| 서버/클라이언트 시간             | DB `CURRENT_TIMESTAMP` 통일               | `flush()`에서 `Instant.now()` 사용하지 않음 — JVM/DB 클럭 불일치 방지                                             |
| 특수 IdTable 조합           | 1차 미제공, 예시 제공                           | 조합 폭발 방지, 사용자 직접 조합 권장                                                                             |
| Auditable + SoftDeleted | 1차 미제공, 예시 제공                           | 다중 상속 불가, composition 패턴 문서화                                                                       |
| `createdBy` 컬럼 길이       | 128 (워크숍 50 → 확장)                       | 이메일/OIDC subject 등 긴 식별자 대응                                                                        |
| Coroutines 확장           | 2차                                      | 1차 스코프 집중, 추후 exposed-jdbc에 추가                                                                     |
| `AuditableEntity` 배치 위치 | **exposed-dao** (사용자 최종 확인)             | `KsuidEntity`, `SnowflakeIdEntity` 등 DAO 엔티티 계층 선례 준수. exposed-jdbc는 `AuditableJdbcRepository`만 담당 |
| `writeValues` 안정성       | 사용 허용 + 버전 종속성 주석                       | Exposed 1.0+ protected API, 메이저 업그레이드 시 검증 필요                                                      |
