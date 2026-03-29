# Auditable 패턴 Exposed 모듈 통합 — 구현 계획

> **스펙**: `docs/superpowers/specs/2026-03-29-auditable-exposed-design.md`
> **작성일**: 2026-03-29
> **대상 모듈**: `data/exposed-core`, `data/exposed-dao`, `data/exposed-jdbc`

---

## 탐색 결과 요약

### 기존 패턴 분석

| 참조 대상                                 | 핵심 패턴                                                                                               | 위치                           |
|---------------------------------------|-----------------------------------------------------------------------------------------------------|------------------------------|
| `SoftDeletedIdTable<T>`               | `IdTable<T>` 상속, 단일 컬럼(`isDeleted`) 추가                                                              | `exposed-core/…/dao/id/`     |
| `KsuidEntity` / `KsuidEntityClass`    | `StringEntity` 상속, 1:1 EntityClass 매핑                                                               | `exposed-dao/…/id/`          |
| `JdbcRepository<ID, E>`               | interface, `table` + `extractId` + `toEntity` 구현 필수                                                 | `exposed-jdbc/…/repository/` |
| `SoftDeletedJdbcRepository<ID, E, T>` | `JdbcRepository` 확장, 테이블 타입 제네릭 `T` 추가                                                              | `exposed-jdbc/…/repository/` |
| 테스트 패턴                                | `AbstractExposedTest`, `@ParameterizedTest` + `@MethodSource(ENABLE_DIALECTS_METHOD)`, `withTables` | `exposed-jdbc/src/test/…`    |

### 의존성 확인

- `exposed-core/build.gradle.kts`: `compileOnly(Libs.exposed_java_time)` **이미 포함** — 추가 변경 불필요
- `exposed-dao/build.gradle.kts`: `api(project(":bluetape4k-exposed-core"))` + `api(Libs.exposed_dao)` —
  `AuditableIdTable` 접근 가능
- `exposed-jdbc/build.gradle.kts`: `api(project(":bluetape4k-exposed-dao"))` + `compileOnly(Libs.exposed_java_time)` *
  *이미 포함** — `CurrentTimestamp` 사용 가능

### 배치 결정 (스펙 준수)

- `Auditable`, `UserContext`, `AuditableIdTable` 계열 → **exposed-core** (`io.bluetape4k.exposed.core.auditable`)
- `AuditableEntity`, `AuditableEntityClass` 계열 → **exposed-dao** (`io.bluetape4k.exposed.dao.auditable`) — 기존
  `KsuidEntity` 등과 동일 모듈
- `AuditableJdbcRepository` 계열 → **exposed-jdbc** (`io.bluetape4k.exposed.jdbc.repository`)
- 테스트 → **exposed-jdbc** (`src/test`)

---

## Phase 1 — exposed-core: Auditable 인터페이스 + UserContext + AuditableIdTable

### Task 1.1: `Auditable` 인터페이스 생성

- **complexity: low**
- **파일**: `data/exposed-core/src/main/kotlin/io/bluetape4k/exposed/core/auditable/Auditable.kt`
- **내용**:
    - `interface Auditable` — `createdBy: String`, `createdAt: Instant?`, `updatedBy: String?`, `updatedAt: Instant?`
    - KDoc (한국어) — JPA `@CreatedBy`/`@CreatedDate`/`@LastModifiedBy`/`@LastModifiedDate` 대응 명시
- **참조**: 스펙 7.1절

### Task 1.2: `UserContext` 싱글톤 생성

- **complexity: high**
- **파일**: `data/exposed-core/src/main/kotlin/io/bluetape4k/exposed/core/auditable/UserContext.kt`
- **내용**:
    - `object UserContext` — `DEFAULT_USERNAME = "system"`
    - `SCOPED_USER: ScopedValue<String>` (Java 21 Preview)
    - `THREAD_LOCAL_USER: InheritableThreadLocal<String?>` (private)
    - `withUser(username, block)` — ScopedValue + ThreadLocal 동시 설정
    - `withThreadLocalUser(username, block)` — ThreadLocal 전용 (Coroutines/일반 Thread)
    - `getCurrentUser()` — 우선순위: ScopedValue > ThreadLocal > DEFAULT_USERNAME
    - KDoc (한국어) — ScopedValue/ThreadLocal 듀얼 전략 설명
- **핵심 결정**: `ScopedValue.get()` 실패 시 `runCatching` 으로 안전 처리, `NoSuchElementException` 방지
- **참조**: 스펙 4.2절

### Task 1.3: `AuditableIdTable<ID>` 추상 클래스 생성

- **complexity: medium**
- **파일**: `data/exposed-core/src/main/kotlin/io/bluetape4k/exposed/core/auditable/AuditableIdTable.kt`
- **내용**:
    - `abstract class AuditableIdTable<ID: Any>(name: String = "") : IdTable<ID>(name)`
    - `createdBy`: `varchar("created_by", 128).clientDefault { UserContext.getCurrentUser() }` — non-nullable
    - `createdAt`: `timestamp("created_at").defaultExpression(CurrentTimestamp).nullable()`
    - `updatedBy`: `varchar("updated_by", 128).nullable()`
    - `updatedAt`: `timestamp("updated_at").nullable()`
    - KDoc (한국어) — 각 컬럼의 INSERT/UPDATE 시 동작 설명
- **참조**: 스펙 7.2절, `SoftDeletedIdTable` 패턴 (단일 컬럼 추가 방식)

### Task 1.4: Concrete Table 변형 3종 생성

- **complexity: low**
- **파일들**:
    - `data/exposed-core/src/main/kotlin/io/bluetape4k/exposed/core/auditable/AuditableIntIdTable.kt`
    - `data/exposed-core/src/main/kotlin/io/bluetape4k/exposed/core/auditable/AuditableLongIdTable.kt`
    - `data/exposed-core/src/main/kotlin/io/bluetape4k/exposed/core/auditable/AuditableUUIDTable.kt`
- **내용**:
    - `AuditableIntIdTable(name, columnName)` — `integer(columnName).autoIncrement().entityId()`
    - `AuditableLongIdTable(name, columnName)` — `long(columnName).autoIncrement().entityId()`
    - `AuditableUUIDTable(name, columnName)` — `javaUUID(columnName).clientDefault { UUID.randomUUID() }.entityId()`
    - 각각 `primaryKey = PrimaryKey(id)`, KDoc (한국어)
- **참조**: 스펙 7.3절

---

## Phase 2 — exposed-dao: AuditableEntity + AuditableEntityClass

### Task 2.1: `AuditableEntity<ID>` 추상 클래스 생성

- **complexity: high**
- **파일**: `data/exposed-dao/src/main/kotlin/io/bluetape4k/exposed/dao/auditable/AuditableEntity.kt`
- **내용**:
    - `abstract class AuditableEntity<ID: Any>(id: EntityID<ID>) : Entity<ID>(id), Auditable`
    - `abstract override var createdBy/createdAt/updatedBy/updatedAt`
    - `flush()` 오버라이드:
        - `val isNew = createdAt == null` 선캡처
        - 신규: `createdBy = user` (createdAt은 DB defaultExpression 위임)
        - 수정: `writeValues.isNotEmpty()` 확인 후 `updatedBy = user` (updatedAt은 Repository/DB 위임)
    - `equals()` = `idEquals(other)`, `hashCode()` = `idHashCode()`
    - KDoc (한국어) — `writeValues` 버전 종속성 경고 포함
- **핵심 결정**: `flush()`에서 타임스탬프 직접 설정하지 않음 — DB `CURRENT_TIMESTAMP` 위임으로 JVM/DB 클럭 불일치 방지
- **⚠️ DAO updatedAt 갭 주의**: `flush()` 단독 호출 시 `updatedAt`은 DB `CURRENT_TIMESTAMP`가 자동 갱신되지 않음. DAO Entity 직접 수정 후
  `flush()` 호출만으로는 `updatedAt`이 설정되지 않으며, `AuditableJdbcRepository.auditedUpdateById()` 사용 시에만
  `updatedAt` 자동 설정이 보장됨. KDoc에 이 제약 명시 필수.
- **참조**: 스펙 5.2절 B, 7.4절

### Task 2.2: Concrete Entity 변형 3종 생성

- **complexity: low**
- **파일**: 스펙 2.2절과 일치하도록 **별도 파일로 분리**:
    - `data/exposed-dao/src/main/kotlin/io/bluetape4k/exposed/dao/auditable/AuditableIntEntity.kt`
    - `data/exposed-dao/src/main/kotlin/io/bluetape4k/exposed/dao/auditable/AuditableLongEntity.kt`
    - `data/exposed-dao/src/main/kotlin/io/bluetape4k/exposed/dao/auditable/AuditableUUIDEntity.kt`
- **내용**:
    - `abstract class AuditableIntEntity(id: EntityID<Int>) : AuditableEntity<Int>(id)`
    - `abstract class AuditableLongEntity(id: EntityID<Long>) : AuditableEntity<Long>(id)`
    - `abstract class AuditableUUIDEntity(id: EntityID<java.util.UUID>) : AuditableEntity<java.util.UUID>(id)`
- **참조**: 스펙 2.2절, 7.4절 하단

### Task 2.3: `AuditableEntityClass` 3종 생성

- **complexity: low**
- **파일**: `data/exposed-dao/src/main/kotlin/io/bluetape4k/exposed/dao/auditable/AuditableEntityClass.kt`
- **내용**:
    - `AuditableIntEntityClass<E: AuditableIntEntity>(table: AuditableIntIdTable, …) : IntEntityClass<E>(…)`
    - `AuditableLongEntityClass<E: AuditableLongEntity>(table: AuditableLongIdTable, …) : LongEntityClass<E>(…)`
    - `AuditableUUIDEntityClass<E: AuditableUUIDEntity>(table: AuditableUUIDTable, …) : UUIDEntityClass<E>(…)`
    - KDoc (한국어) — 사용 예시 포함 (`KsuidEntityClass` 패턴 참조)
- **참조**: 스펙 7.5절, `KsuidEntityClass` 패턴

---

## Phase 3 — exposed-jdbc: AuditableJdbcRepository

### Task 3.1: `AuditableJdbcRepository` 인터페이스 + 편의 타입 별칭 생성

- **complexity: medium**
- **파일**: `data/exposed-jdbc/src/main/kotlin/io/bluetape4k/exposed/jdbc/repository/AuditableJdbcRepository.kt`
- **내용**:
    - `interface AuditableJdbcRepository<ID: Any, E: Any, T: AuditableIdTable<ID>> : JdbcRepository<ID, E>`
    - `override val table: T`
    - `auditedUpdateById(id, limit?, updateStatement)` — `it[table.updatedAt] = CurrentTimestamp` +
      `it[table.updatedBy] = UserContext.getCurrentUser()`
    - `auditedUpdateAll(predicate, limit?, updateStatement)` — 동일 패턴
    - 편의 별칭: `IntAuditableJdbcRepository`, `LongAuditableJdbcRepository`, `UUIDAuditableJdbcRepository`
    - **고려 사항**: `SoftDeletedJdbcRepository` 패턴의 `UuidSoftDeletedJdbcRepository`(Kotlin uuid 전용)처럼, Kotlin
      `kotlin.uuid.Uuid` 타입을 사용하는 경우를 위한 `KotlinUuidAuditableJdbcRepository` 타입 별칭 추가 여부를 검토한다. 1차에서는
      `java.util.UUID` 기반으로 제공하고 필요 시 2차에서 추가.
    - KDoc (한국어) — `SoftDeletedJdbcRepository` 와 동일 스타일
- **핵심 결정**: `CurrentTimestamp` 사용으로 DB UTC 보장 — `Instant.now()` 사용하지 않음
- **참조**: 스펙 7.6절, `SoftDeletedJdbcRepository` 패턴 (테이블 타입 제네릭 `T` 패턴)

---

## Phase 4 — 테스트 (exposed-jdbc, exposed-core)

### Task 4.1: `UserContextTest` 단위 테스트

- **complexity: medium**
- **파일**: `data/exposed-core/src/test/kotlin/io/bluetape4k/exposed/core/auditable/UserContextTest.kt`
- **테스트 케이스**:
    1. `getCurrentUser()` 기본값 = `"system"` 검증
    2. `withUser(username)` 내부에서 `getCurrentUser()` = username 검증
    3. `withUser` 종료 후 `getCurrentUser()` = `"system"` 복원 검증
    4. `withThreadLocalUser(username)` 동작 검증
    5. `withThreadLocalUser` 중첩 시 이전 값 복원 검증
    6. `withUser` 내부에서 `SCOPED_USER.get()` 직접 접근 검증
- **참조**: 스펙 4.2절

### Task 4.2: DSL + Repository 테스트 (`AuditableJdbcRepositoryTest`)

- **complexity: high**
- **파일**: `data/exposed-jdbc/src/test/kotlin/io/bluetape4k/exposed/jdbc/repository/AuditableJdbcRepositoryTest.kt`
- **테스트 구조**:
    - `AuditableActorTable : AuditableLongIdTable("auditable_actors")` — `firstName`, `lastName`
    - `AuditableActorRecord` data class — `Auditable` 구현
    - `LongAuditableJdbcRepository` 구현체
- **테스트 케이스** (`@ParameterizedTest` + `@MethodSource(ENABLE_DIALECTS_METHOD)`, `withTables`):
    1. INSERT 시 `createdBy` = `"system"` (기본값), `createdAt` != null 검증
    2. INSERT 직후 `updatedAt == null`, `updatedBy == null` 검증
    3. `UserContext.withUser("admin")` 내 INSERT 시 `createdBy` = `"admin"` 검증
    4. `auditedUpdateById` 후 `updatedBy` != null, `updatedAt` != null 검증
    5. `auditedUpdateAll` 동작 검증
    6. 일반 `updateById` 시 감사 필드 미변경 확인
- **참조**: 스펙 8.1절 A, `SoftDeletedJdbcRepositoryTest` 테스트 구조

### Task 4.3: DAO Entity 테스트 (`AuditableEntityTest`)

- **complexity: high**
- **파일**: `data/exposed-jdbc/src/test/kotlin/io/bluetape4k/exposed/jdbc/repository/AuditableEntityTest.kt`
- **테스트 구조**:
    - `Articles : AuditableLongIdTable("articles")` — `title`
    - `Article : AuditableLongEntity` + `companion object : AuditableLongEntityClass<Article>(Articles)`
- **테스트 케이스** (`@ParameterizedTest` + `@MethodSource(ENABLE_DIALECTS_METHOD)`, `withTables`):
    1. `new {}` + `flush()` 시 `createdBy` 자동 설정 검증
    2. `new {}` 직후 `updatedAt == null`, `updatedBy == null` 검증
    3. 프로퍼티 수정 후 `flush()` 시 `updatedBy` 설정 검증
    4. `UserContext.withUser("admin")` 내 생성 시 `createdBy` = `"admin"` 검증
    5. `equals()` / `hashCode()` — `idEquals` / `idHashCode` 기반 검증
- **참조**: 스펙 8.1절 B

---

## Phase 5 — 문서화

### Task 5.1: exposed-core README 업데이트

- **complexity: low**
- **파일**: `data/exposed-core/README.md`
- **내용**: Auditable 섹션 추가 — `Auditable` 인터페이스, `UserContext`, `AuditableIdTable` 계열 설명

### Task 5.2: exposed-dao README 업데이트

- **complexity: low**
- **파일**: `data/exposed-dao/README.md`
- **내용**: `AuditableEntity`, `AuditableEntityClass` 설명 추가

### Task 5.3: exposed-jdbc README 업데이트

- **complexity: low**
- **파일**: `data/exposed-jdbc/README.md` (없으면 생성)
- **내용**: `AuditableJdbcRepository` 섹션 추가 — 사용 예시 포함

### Task 5.4: 루트 CLAUDE.md Architecture 섹션 업데이트

- **complexity: low**
- **파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/CLAUDE.md`
- **내용**: Exposed 모듈 설명에 Auditable 관련 클래스 추가

### Task 5.5: exposed-java-time 의존성 문서화

- **complexity: low**
- **내용**: `AuditableIdTable`이 `org.jetbrains.exposed.v1.javatime.timestamp` 및 `CurrentTimestamp`를 사용하므로,
  `exposed-java-time` 의존성이 필요함을 각 모듈 README에 명시한다.
    - `exposed-core`: `compileOnly(Libs.exposed_java_time)` — 이미 포함됨, 문서에 명시
    - `exposed-dao`: `exposed-core`에 `api` 의존하므로 전이 포함됨 — 명시
    - `exposed-jdbc`: `compileOnly(Libs.exposed_java_time)` 이미 포함 — 명시
- **참조**: 플랜 의존성 확인 섹션

### Task 5.6: bluetape4k-patterns 체크리스트 적용 확인

- **complexity: low**
- **내용**: 구현 완료 후 `bluetape4k-patterns` 스킬 체크리스트 항목을 검증한다:
    - KDoc이 모든 공개 클래스/인터페이스/확장 메서드에 한국어로 작성되었는지 확인
    - `equals()` / `hashCode()` 구현 일관성 확인 (`idEquals`/`idHashCode` 사용)
    - 불필요한 `runBlocking` 사용 없음 확인
    - `atomicfu` 사용 시 클래스 레벨 프로퍼티만 사용됨을 확인 (해당 없으면 생략)
    - 테스트가 `@ParameterizedTest` + `@MethodSource(ENABLE_DIALECTS_METHOD)` 패턴을 따르는지 확인

---

## 복잡도별 태스크 요약

### High (핵심 로직, 아키텍처 결정)

| Task | 설명                                                |
|------|---------------------------------------------------|
| 1.2  | `UserContext` — ScopedValue + ThreadLocal 듀얼 전략   |
| 2.1  | `AuditableEntity<ID>` — `flush()` 오버라이드, 생성/수정 분기 |
| 4.2  | DSL + Repository 통합 테스트 (6개 케이스)                  |
| 4.3  | DAO Entity 통합 테스트 (5개 케이스)                        |

### Medium (표준 구현)

| Task | 설명                                                                 |
|------|--------------------------------------------------------------------|
| 1.3  | `AuditableIdTable<ID>` — 4개 감사 컬럼 정의                               |
| 3.1  | `AuditableJdbcRepository` — `auditedUpdateById`/`auditedUpdateAll` |
| 4.1  | `UserContextTest` — 6개 단위 테스트                                      |

### Low (보일러플레이트, KDoc, 설정)

| Task | 설명                                                                                                      |
|------|---------------------------------------------------------------------------------------------------------|
| 1.1  | `Auditable` 인터페이스                                                                                       |
| 1.4  | Concrete Table 3종 (`Int`/`Long`/`UUID`)                                                                 |
| 2.2  | Concrete Entity 3종 (별도 파일: `AuditableIntEntity.kt`, `AuditableLongEntity.kt`, `AuditableUUIDEntity.kt`) |
| 2.3  | `AuditableEntityClass` 3종                                                                               |
| 5.1  | exposed-core README                                                                                     |
| 5.2  | exposed-dao README                                                                                      |
| 5.3  | exposed-jdbc README                                                                                     |
| 5.4  | CLAUDE.md 업데이트                                                                                          |
| 5.5  | exposed-java-time 의존성 문서화                                                                               |
| 5.6  | bluetape4k-patterns 체크리스트 적용 확인                                                                         |

---

## 의존성 그래프 (실행 순서)

```
Phase 1 (exposed-core)
  Task 1.1 ──┐
  Task 1.2 ──┤ (병렬 가능)
  Task 1.3 ──┘── Task 1.3은 1.1, 1.2에 의존
  Task 1.4 ────── Task 1.3에 의존
        │
        ▼
Phase 2 (exposed-dao) ── Phase 1 완료 후
  Task 2.1 ────── Task 1.1, 1.2에 의존
  Task 2.2 ────── Task 2.1에 의존
  Task 2.3 ────── Task 1.4, 2.2에 의존
        │
        ▼
Phase 3 (exposed-jdbc) ── Phase 1 완료 후 (Phase 2와 병렬 가능)
  Task 3.1 ────── Task 1.3에 의존
        │
        ▼
Phase 4 (테스트) ── Phase 1~3 완료 후
  Task 4.1 ────── Task 1.2에 의존 (Phase 1 완료 후 바로 가능)
  Task 4.2 ────── Task 3.1에 의존
  Task 4.3 ────── Task 2.3에 의존
        │
        ▼
Phase 5 (문서화) ── Phase 1~4 완료 후
  Task 5.1~5.6 ── 병렬 가능
```

---

## 빌드 의존성 변경 필요 여부

| 모듈             | 변경 필요  | 이유                                                               |
|----------------|--------|------------------------------------------------------------------|
| `exposed-core` | **없음** | `exposed_java_time` 이미 `compileOnly` 포함                          |
| `exposed-dao`  | **없음** | `exposed-core`에 `api` 의존, `exposed_dao`에 `api` 의존                |
| `exposed-jdbc` | **없음** | `exposed-dao`에 `api` 의존, `exposed_java_time` 이미 `compileOnly` 포함 |

---

## 리스크 및 주의사항

1. **`writeValues` 버전 종속성**: Exposed `Entity.writeValues`는 `protected` API. Exposed 메이저 업그레이드 시 검증 필요 → KDoc에 경고 명시
2. **`ScopedValue` Preview API**: Java 21에서 `--enable-preview` 필요 — 프로젝트에 이미 설정됨 (`-XX:+EnableDynamicAgentLoading` 등)
3. **`createdAt` nullable**: DB `defaultExpression`이 INSERT 시 값 설정하지만, Exposed `ResultRow` 매핑 전까지는 Kotlin 측에서 null. DAO
   `new {}` 직후 `createdAt`은 null일 수 있음 → `flush()` + `refresh()` 후 값 확인
4. **Auditable + SoftDeleted 조합**: 1차 스코프 제외. 사용자가 직접 `AuditableSoftDeletedIdTable` composition 패턴으로 조합
