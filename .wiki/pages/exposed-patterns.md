# Exposed 패턴

> 마지막 업데이트: 2026-04-07 | 관련 specs: 3개

## 개요

Bluetape4k의 Exposed 기반 모듈이 공유하는 핵심 설계 패턴을 정리한다.
Repository 제네릭 선언 방식, Cache Repository 공통 인터페이스(exposed-redis-api), 캐시 전략 열거형,
testFixtures 공유 패턴, 그리고 Read/Write-through/behind 패턴을 다룬다.

---

## 핵심 설계 결정 (ADR)

| 결정 | 이유 | 날짜 | 관련 spec |
|------|------|------|----------|
| Repository 제네릭 `<ID, E>` — 테이블 타입 제거 | 테이블 타입이 제네릭에 포함되면 구현체마다 다른 Table 타입 인자가 필요하여 인터페이스 통일 불가. `AuditableJdbcRepository`처럼 테이블 접근이 꼭 필요한 경우에만 `T` 추가 | 2026-04-07 | exposed-cache-repository-unification |
| `exposed-redis-api` 신규 모듈 분리 | Lettuce/Redisson 4개 모듈이 동일 기능을 제공하면서 인터페이스가 불일치. 공통 인터페이스 + testFixtures를 별도 API 모듈로 분리하여 중복 제거 | 2026-04-07 | exposed-cache-repository-unification |
| `javax.cache.Cache` API 관례 기반 메서드 명명 | Cache Repository는 Spring Data CRUD 패턴이 아닌 캐시 전략(Read/Write-through) 도구이므로 `get`/`put`/`invalidate`/`clear` 등 JCache 관례를 따름 | 2026-04-07 | exposed-cache-repository-unification |
| `CacheMode` / `CacheWriteMode` 공통 열거형 | Lettuce의 `WriteMode`와 Redisson의 `CacheMode+WriteMode` 조합을 단일 열거형으로 통합. 인터페이스 수준에서만 사용하고 구현체는 native Config 유지 | 2026-04-07 | exposed-cache-repository-unification |
| `invalidateByPattern`을 Redisson 전용 확장 인터페이스로 분리 | Lettuce `LettuceLoadedMap`은 keySet pattern을 지원하지 않음 → 공통 인터페이스에 포함하면 LSP(리스코프 치환 원칙) 위반 | 2026-04-07 | exposed-cache-repository-unification |
| `updateEntity`/`insertEntity`를 Abstract 클래스에 유지 | Lettuce는 Repository가 직접 DB에 기록하고, Redisson은 `MapWriter`에 외부 주입. 두 메커니즘이 달라 공통 인터페이스화 불가 | 2026-04-07 | exposed-cache-repository-unification |
| Fork & Adapt — Spring Boot 3/4 모듈 코드 공유 전략 불채택 | 차이점이 3~5개 파일, 각 1~3줄 수준으로 매우 작아 shared source set 도입 시 빌드 복잡성이 이득을 초과 | 2026-03-29 | spring-data-exposed-migration |

---

## 패턴 & 사용법

### Repository 제네릭 패턴 — `<ID: Any, E: Any>`

모든 Repository 클래스는 테이블 타입 제네릭 없이 `<ID, E>` 두 타입 파라미터만 사용한다.

```kotlin
// 표준 패턴 — 테이블 타입 없음
class ArticleRepository : AbstractJdbcRepository<Long, ArticleRecord>() {
    override val table = ArticleTable
    override fun extractId(entity: ArticleRecord) = entity.id
    override fun ResultRow.toEntity() = ArticleRecord(
        id = this[ArticleTable.id].value,
        title = this[ArticleTable.title],
    )
}
```

**예외**: `SoftDeleted*` Repository는 `table.isDeleted` 접근이 필요하므로 테이블 타입 `T`를 유지한다.

`AuditableJdbcRepository`도 `table.updatedAt` 직접 접근이 필요하여 `T: AuditableIdTable<ID>`를 제네릭으로 유지한다:

```kotlin
interface AuditableJdbcRepository<ID: Any, E: Any, T: AuditableIdTable<ID>>
    : JdbcRepository<ID, E> {
    override val table: T
    fun auditedUpdateById(id: ID, ..., updateStatement: T.(UpdateStatement) -> Unit): Int
}
```

### exposed-redis-api 모듈 구조

```
data/exposed-redis-api/
├── src/main/kotlin/io/bluetape4k/exposed/redis/
│   ├── CacheMode.kt
│   ├── CacheWriteMode.kt
│   └── repository/
│       ├── JdbcCacheRepository.kt          # 동기 JDBC
│       ├── SuspendedJdbcCacheRepository.kt # suspend JDBC
│       ├── R2dbcCacheRepository.kt         # suspend R2DBC
│       ├── JdbcRedissonCacheRepository.kt  # Redisson 전용 확장
│       └── R2dbcRedissonCacheRepository.kt
└── src/testFixtures/kotlin/               # 공통 테스트 시나리오
```

### CacheMode / CacheWriteMode 열거형

```kotlin
enum class CacheMode {
    READ_ONLY,   // Read-through만 수행, DB 쓰기 없음
    READ_WRITE,  // Write-through 또는 Write-behind로 DB에도 반영
}

enum class CacheWriteMode {
    NONE,           // READ_ONLY에서 사용
    WRITE_THROUGH,  // 캐시와 DB에 동시 반영
    WRITE_BEHIND,   // 캐시에 즉시, DB에 비동기 반영
}
```

구현체는 native Config(`LettuceCacheConfig`, `RedissonCacheConfig`)를 그대로 사용하고,
인터페이스의 `cacheMode`/`cacheWriteMode` 프로퍼티에서 변환만 수행한다.

### JdbcCacheRepository 인터페이스 핵심 메서드

| 메서드 | 설명 |
|--------|------|
| `get(id)` | 캐시 조회, 미스 시 DB Read-through |
| `getAll(ids)` | 일괄 조회, Map 반환 |
| `put(id, entity)` | 캐시 저장, `cacheWriteMode`에 따라 DB에도 반영 |
| `putAll(entities, batchSize)` | 일괄 저장 |
| `invalidate(id)` | **캐시에서만** 제거 (DB 삭제 아님) |
| `invalidateAll(ids)` | 캐시 일괄 무효화 |
| `clear()` | 캐시 전체 비우기 (DB 영향 없음) |
| `containsKey(id)` | 캐시 존재 확인 |
| `findByIdFromDb(id)` | 캐시 우회 DB 직접 조회 |
| `countFromDb()` | DB 전체 건수 |

### Read/Write-through/behind 패턴

**Read-Through**: `get(id)` 호출 시 캐시 미스면 DB에서 자동 로드 후 캐시에 저장.

**Write-Through**: `put(id, entity)` 시 캐시와 DB에 동시 반영. Lettuce는 `AbstractJdbcLettuceRepository.updateEntity()`가 직접 처리, Redisson은 `MapWriter`에 외부 주입.

**Write-Behind**: 캐시에 즉시 저장하고 DB 반영은 비동기. `cacheWriteMode = WRITE_BEHIND`로 설정.

```kotlin
// Lettuce Abstract 클래스 패턴 (Write-Through/Behind 구현)
abstract class AbstractJdbcLettuceRepository<ID: Any, E: Serializable>(
    client: RedisClient,
    val config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
) : JdbcCacheRepository<ID, E> {

    // Lettuce가 직접 DB에 기록하는 메서드 — Abstract 클래스에만 존재
    abstract fun UpdateStatement.updateEntity(entity: E)
    abstract fun BatchInsertStatement.insertEntity(entity: E)
}

// Redisson — MapWriter에 외부 주입, updateEntity/insertEntity 불필요
abstract class AbstractJdbcRedissonRepository<ID: Any, E: Serializable>(
    val redissonClient: RedissonClient,
    override val cacheName: String,
    protected val config: RedissonCacheConfig,
) : JdbcRedissonCacheRepository<ID, E>
```

### testFixtures 공유 패턴

`exposed-redis-api` 모듈의 `testFixtures`에서 공통 시나리오 인터페이스를 제공한다.
각 구현 모듈(jdbc-lettuce, jdbc-redisson, r2dbc-lettuce, r2dbc-redisson)은 이를 상속하여 12가지 테스트 조합을 자동으로 커버한다.

```kotlin
// build.gradle.kts 소비 모듈
testImplementation(testFixtures(project(":bluetape4k-exposed-redis-api")))

// 테스트 클래스에서
class LettuceLongIdReadThroughTest
    : ReadThroughScenario<Long, ArticleRecord>,
      AbstractLettuceCacheTest() {
    override val repository = ArticleLettuceRepository(redisClient)
    override fun getExistingId() = 1L
    // ...
}
```

**테스트 매트릭스**: 2개 테이블 종류 × 2개 캐시 저장 방식 × 3개 캐시 기법 = 12가지 조합

### AbstractJdbcRepository / AbstractSuspendedJdbcRepository 사용법

```kotlin
// 동기 JDBC
class CustomerRepository : AbstractJdbcRepository<Long, CustomerRecord>() {
    override val table = CustomerTable
    override fun extractId(entity: CustomerRecord) = entity.id
    override fun ResultRow.toEntity() = CustomerRecord(
        id = this[CustomerTable.id].value,
        name = this[CustomerTable.name],
    )
}

// Suspend JDBC
class CustomerSuspendRepository : AbstractSuspendedJdbcRepository<Long, CustomerRecord>() {
    override val table = CustomerTable
    override fun extractId(entity: CustomerRecord) = entity.id
    override fun ResultRow.toEntity() = CustomerRecord(/* ... */)
}
```

---

## 선택하지 않은 방식 / 트레이드오프

| 방식 | 선택하지 않은 이유 |
|------|-----------------|
| Repository 제네릭에 테이블 타입 포함 `<ID, E, T: Table>` | 인터페이스 통일 불가, 소비 코드가 복잡해짐 |
| `invalidateByPattern`을 공통 인터페이스에 포함 | Lettuce 구현 불가 → LSP 위반, `UnsupportedOperationException` 발생 위험 |
| `@Deprecated` 브릿지 메서드 유지 | 1.5.x 최신 버전이므로 하위 호환 불필요, 즉시 교체로 코드베이스 단순화 |
| Spring Data 관례 메서드명(`findById`, `save`, `delete`) | Cache Repository의 목적(캐시 전략)과 어울리지 않음, DB 삭제와 캐시 무효화 혼동 위험 |
| Shared source set으로 Spring Boot 3/4 코드 공유 | `includeModules` 함수와 충돌, 빌드 복잡성 증가 |

---

## 관련 페이지

- [cache-architecture.md](cache-architecture.md) — NearCacheOperations 공통 인터페이스 및 백엔드 구현체
- [auditable-pattern.md](auditable-pattern.md) — Exposed 감사 패턴 (createdBy/updatedBy 자동 추적)
- [dependency-decisions.md](dependency-decisions.md) — 라이브러리 버전 선택 히스토리
