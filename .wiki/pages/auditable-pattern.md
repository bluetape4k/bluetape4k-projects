# Auditable 패턴

> 마지막 업데이트: 2026-04-07 | 관련 specs: 1개

## 개요

JPA의 `@CreatedBy`, `@CreatedDate`, `@LastModifiedBy`, `@LastModifiedDate`에 대응하는 Exposed 감사(Audit) 패턴.
`exposed-core` / `exposed-dao` / `exposed-jdbc` 3계층에 걸쳐 구현되며,
`UserContext`의 듀얼 전략(ScopedValue + ThreadLocal)으로 가상 스레드와 코루틴 양쪽을 지원한다.

---

## 핵심 설계 결정 (ADR)

| 결정 | 이유 | 날짜 | 관련 spec |
|------|------|------|----------|
| 3계층 분리: exposed-core / exposed-dao / exposed-jdbc | 기존 `build.gradle.kts` 의존성 흐름(core → dao → jdbc)과 일치하여 추가 의존성 변경 불필요 | 2026-03-29 | auditable-exposed-design |
| `UserContext` 듀얼 전략 (ScopedValue 우선 + ThreadLocal fallback) | ScopedValue는 Preview API이고 일반 Thread/코루틴에서 전파 안 됨. ThreadLocal fallback으로 두 환경 모두 지원 | 2026-03-29 | auditable-exposed-design |
| 타임스탬프는 DB 서버의 UTC `CURRENT_TIMESTAMP` | JVM/DB 클럭 불일치 문제 방지. `createdAt`은 `defaultExpression(CurrentTimestamp)`, `updatedAt`은 Repository UPDATE 시 `CurrentTimestamp` 사용 | 2026-03-29 | auditable-exposed-design |
| `flush()`에서 타임스탬프 직접 설정 안 함 | `Instant.now()` 사용 시 JVM 시간과 DB 시간이 다를 수 있음. createdBy/updatedBy(사용자 정보)만 flush()에서 설정 | 2026-03-29 | auditable-exposed-design |
| `updatedAt`을 Repository DSL UPDATE에서 설정 | DAO `flush()`는 DAO 전용이라 DSL UPDATE에 적용 불가. `auditedUpdateById`에서 `it[table.updatedAt] = CurrentTimestamp`로 명시적 처리 | 2026-03-29 | auditable-exposed-design |
| Kotlin 다중 상속 불가로 Auditable+SoftDeleted 조합은 사용자 코드에서 | 두 기능을 모두 내장 클래스로 제공하면 조합 수 폭발. 사용자가 직접 `AuditableSoftDeletedIdTable` 등을 상속 정의 | 2026-03-29 | auditable-exposed-design |

---

## 패턴 & 사용법

### 3계층 구조

```
exposed-core
  └── Auditable 인터페이스
  └── UserContext (ScopedValue + ThreadLocal 듀얼)
  └── AuditableIdTable<ID>  ← createdBy/createdAt/updatedBy/updatedAt 컬럼 정의
      └── AuditableIntIdTable
      └── AuditableLongIdTable
      └── AuditableUUIDTable
        ↓
exposed-dao
  └── AuditableEntity<ID>   ← flush() 오버라이드로 createdBy/updatedBy 자동 설정
      └── AuditableIntEntity
      └── AuditableLongEntity
      └── AuditableUUIDEntity
  └── AuditableEntityClass  ← EntityClass 팩토리
        ↓
exposed-jdbc
  └── AuditableJdbcRepository  ← auditedUpdateById/auditedUpdateAll 제공
```

### UserContext — ScopedValue + ThreadLocal 듀얼 전략

```kotlin
object UserContext {
    const val DEFAULT_USERNAME = "system"

    // ScopedValue — Virtual Thread 환경 권장
    val SCOPED_USER: ScopedValue<String> = ScopedValue.newInstance()

    // ThreadLocal — 일반 Thread / 코루틴 환경
    private val THREAD_LOCAL_USER: InheritableThreadLocal<String?> = InheritableThreadLocal()

    // 두 전략 동시 설정 (Virtual Thread + 코루틴 혼용 환경 대응)
    fun <T> withUser(username: String, block: () -> T): T {
        THREAD_LOCAL_USER.set(username)
        try {
            return ScopedValue.where(SCOPED_USER, username).call(block)
        } finally {
            THREAD_LOCAL_USER.remove()
        }
    }

    // 우선순위: ScopedValue > ThreadLocal > "system"
    fun getCurrentUser(): String {
        runCatching { SCOPED_USER.get() }.getOrNull()?.let { return it }
        THREAD_LOCAL_USER.get()?.let { return it }
        return DEFAULT_USERNAME
    }
}
```

### AuditableIdTable — 테이블 컬럼 정의

```kotlin
abstract class AuditableIdTable<ID: Any>(name: String = "") : IdTable<ID>(name) {
    // clientDefault → INSERT 시 createdBy 자동 설정
    val createdBy = varchar("created_by", 128)
        .clientDefault { UserContext.getCurrentUser() }

    // DB 서버 UTC 시간 자동 설정
    val createdAt = timestamp("created_at")
        .defaultExpression(CurrentTimestamp)
        .nullable()

    val updatedBy = varchar("updated_by", 128).nullable()
    val updatedAt = timestamp("updated_at").nullable()
}

// 사용 예시
object ArticleTable : AuditableLongIdTable("articles") {
    val title = varchar("title", 255)
}
```

### AuditableEntity — flush() 오버라이드로 사용자 정보 자동 설정

```kotlin
abstract class AuditableEntity<ID: Any>(id: EntityID<ID>) : Entity<ID>(id), Auditable {

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        val isNew = createdAt == null
        val user = UserContext.getCurrentUser()

        if (isNew) {
            createdBy = user   // createdAt은 DB defaultExpression이 처리
        } else if (writeValues.isNotEmpty()) {
            updatedBy = user   // updatedAt은 auditedUpdateById에서 CurrentTimestamp로 처리
        }

        return super.flush(batch)
    }
}

// DAO 클래스 정의
class Article(id: EntityID<Long>) : AuditableLongEntity(id) {
    companion object : AuditableLongEntityClass<Article>(ArticleTable)
    var title by ArticleTable.title
    override var createdBy by ArticleTable.createdBy
    override var createdAt by ArticleTable.createdAt
    override var updatedBy by ArticleTable.updatedBy
    override var updatedAt by ArticleTable.updatedAt
}
```

### AuditableJdbcRepository — auditedUpdateById / auditedUpdateAll

```kotlin
interface AuditableJdbcRepository<ID: Any, E: Any, T: AuditableIdTable<ID>>
    : JdbcRepository<ID, E> {

    override val table: T

    // UPDATE 시 updatedBy/updatedAt 자동 설정
    fun auditedUpdateById(
        id: ID,
        limit: Int? = null,
        updateStatement: T.(UpdateStatement) -> Unit,
    ): Int = table.update(where = { table.id eq id }, limit = limit) {
        it[table.updatedAt] = CurrentTimestamp   // DB UTC CURRENT_TIMESTAMP
        it[table.updatedBy] = UserContext.getCurrentUser()
        updateStatement(it)
    }
}
```

### 전체 사용 패턴

```kotlin
// INSERT — createdBy 자동, createdAt은 DB 서버 시간
transaction {
    UserContext.withUser("alice") {
        Article.new { title = "Hello" }
        // createdBy = "alice", createdAt = DB UTC 자동 설정
    }
}

// UPDATE — auditedUpdateById 사용 필수
transaction {
    UserContext.withUser("bob") {
        repo.auditedUpdateById(1L) { it[title] = "Updated" }
        // updatedBy = "bob", updatedAt = DB UTC 자동 설정
    }
}
```

> **중요**: UPDATE 시에는 반드시 `auditedUpdateById()` 또는 `auditedUpdateAll()`을 사용해야 `updatedBy`/`updatedAt`이 자동 설정된다. 일반 `update { }` 사용 시 감사 필드가 설정되지 않는다.

### createdBy/updatedBy 자동 설정 메커니즘 요약

| 시나리오 | 메커니즘 | 타임스탬프 출처 |
|---------|---------|--------------|
| INSERT (DAO) | `AuditableEntity.flush()` → `createdBy = user` | `AuditableIdTable.defaultExpression(CurrentTimestamp)` (DB) |
| INSERT (DSL) | `AuditableIdTable.clientDefault { UserContext.getCurrentUser() }` | `AuditableIdTable.defaultExpression(CurrentTimestamp)` (DB) |
| UPDATE (Repository) | `auditedUpdateById()` → `it[updatedBy] = user` | `it[updatedAt] = CurrentTimestamp` (DB) |
| UPDATE (DAO flush) | `AuditableEntity.flush()` → `updatedBy = user` | `auditedUpdateById` 내부에서 처리 |

---

## 선택하지 않은 방식 / 트레이드오프

| 방식 | 선택하지 않은 이유 |
|------|-----------------|
| `ScopedValue`만 사용 | Java 21 Preview API, 일반 Thread/코루틴에서 전파 안 됨 |
| `Instant.now()`로 타임스탬프 직접 설정 | JVM/DB 클럭 불일치 가능. DB 서버 UTC `CURRENT_TIMESTAMP` 위임으로 일관성 보장 |
| `AuditableKsuidTable`, `AuditableSnowflakeIdTable` 등 조합 클래스 제공 | 조합 수 폭발. 1차에서는 `AuditableIntIdTable`/`AuditableLongIdTable`/`AuditableUUIDTable`만 제공, 사용자 코드에서 조합 |
| Coroutines `UserContextElement` (CoroutineContext 전파) | 1차 구현 제외. `withThreadLocalUser`로 충분하며, 추후 `exposed-jdbc`에 추가 용이 |
| `writeValues` API 사용 회피 | Exposed DAO 라이브러리가 `protected`로 노출하는 표준 확장 포인트. 다만 메이저 업그레이드 시 검증 필요 |

---

## 관련 페이지

- [exposed-patterns.md](exposed-patterns.md) — Repository 제네릭 패턴 및 Cache Repository 인터페이스
- [dependency-decisions.md](dependency-decisions.md) — Exposed 버전 선택 히스토리
