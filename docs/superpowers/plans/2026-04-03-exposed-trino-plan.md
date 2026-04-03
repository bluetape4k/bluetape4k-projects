# exposed-trino 모듈 구현 플랜 (Phase 1)

**날짜**: 2026-04-03
**대상 모듈**: `data/exposed-trino/` (`bluetape4k-exposed-trino`, 신규)
**설계 문서**: `docs/superpowers/specs/2026-04-03-exposed-trino-design.md`
**브랜치**: `feat/exposed-trino`
**참고 패턴**: `data/exposed-duckdb/` (DuckDB Dialect + JDBC 연동)

---

## Context

Trino JDBC를 통한 범용 Exposed Dialect 모듈을 신규 생성한다.
DuckDB 모듈(`data/exposed-duckdb/`)과 동일한 구조(커스텀 Dialect + Metadata + ConnectionWrapper + Database 팩토리 + suspend/Flow 확장)를 따르되,
Trino 고유 특성(트랜잭션 미지원, autocommit 강제, Memory 커넥터 UPDATE/DELETE 미지원)을 반영한다.

Phase 1에서는 `exposed-trino` 모듈만 독립 구현하며, `exposed-bigquery` 변경 없음.
Phase 2에서 `exposed-bigquery-trino` 별도 모듈로 통합 예정.

---

## Work Objectives

1. `data/exposed-trino/` 모듈 scaffolding + `build.gradle.kts`
2. `TrinoDialect` + `TrinoDialectMetadata` (PostgreSQLDialect 상속, capability 플래그)
3. `TrinoConnectionWrapper` (autocommit 강제, commit/rollback no-op, prepareStatement 위임)
4. `TrinoDatabase` 팩토리 (connect 2종: host/port, jdbcUrl)
5. `TrinoExtensions` (suspendTransaction, queryFlow)
6. `@TrinoUnsupported` 마커 어노테이션
7. Testcontainer 기반 통합 테스트 6종
8. README.md (트랜잭션 주의사항 섹션 필수)
9. CLAUDE.md Architecture 섹션 업데이트

---

## Guardrails

### Must Have
- 모든 public 클래스/메서드에 한국어 KDoc 필수
- `TrinoConnectionWrapper`는 `internal class` — DuckDB 패턴 동일
- `TrinoDatabase`의 모든 `connect()` 오버로드에서 `TrinoConnectionWrapper` 강제 적용
- 모든 KDoc에 autocommit 주의사항 경고 포함 (suspendTransaction, queryFlow, TrinoDatabase.connect)
- `TrinoTransactionAtomicityTest`로 부분 반영 동작 명시적 검증
- `TrinoServer.Launcher` 싱글턴 사용 (Testcontainer 재사용)
- 테스트는 INSERT + SELECT 위주 (Memory 커넥터 UPDATE/DELETE 미지원)

### Must NOT Have
- `connect(dataSource)` 오버로드 — Phase 2에서 `TrinoDataSourceWrapper` 구현 후 제공
- `exposed-bigquery` 모듈 변경 없음
- HikariCP 커넥션 풀 지원 — Phase 2
- nested transaction / savepoint 지원 시도 없음

---

## Task Flow

```
T1 (scaffolding) ─┐
                   ├── T2 (Dialect) ──┐
                   │                  ├── T3 (ConnectionWrapper) ──┐
                   │                  │                            ├── T4 (Database) ──┐
                   │                  │                            │                   ├── T5 (Extensions)
                   │                  │                            │                   └── T6 (Annotation)
                   │                  │                            │
                   │                  │                            └── T7 (AbstractTrinoTest + Events)
                   │                  │
T7 ────────────────┼──────────────────┼── T8~T13 (테스트 6종, T7 이후 병렬 가능)
                   │
T14 (CLAUDE.md) ───┘ (독립, 언제든)
T15 (patterns 체크리스트) ── (마지막 전)
T16 (README.md) ──── (마지막)
```

---

## Tasks

### T1: 모듈 scaffolding
`complexity: low`

- `data/exposed-trino/` 디렉토리 생성
- `data/exposed-trino/build.gradle.kts` 작성

```kotlin
dependencies {
    api(project(":bluetape4k-logging"))
    api(Libs.exposed_core)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_java_time)
    api(Libs.kotlinx_coroutines_core)

    // Trino JDBC 드라이버
    api(Libs.trino_jdbc)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_trino)
}
```

- `settings.gradle.kts` 자동 등록 확인 (`includeModules("data", withBaseDir = false)` → `bluetape4k-exposed-trino`)
- 소스 디렉토리 구조 생성:
  - `src/main/kotlin/io/bluetape4k/exposed/trino/`
  - `src/main/kotlin/io/bluetape4k/exposed/trino/dialect/`
  - `src/test/kotlin/io/bluetape4k/exposed/trino/`
  - `src/test/kotlin/io/bluetape4k/exposed/trino/domain/`
  - `src/test/kotlin/io/bluetape4k/exposed/trino/query/`
  - `src/test/kotlin/io/bluetape4k/exposed/trino/insert/`

**검증**: `./gradlew :bluetape4k-exposed-trino:dependencies` 성공

---

### T2: TrinoDialect + TrinoDialectMetadata
`complexity: medium`

**파일**:
- `src/main/kotlin/io/bluetape4k/exposed/trino/dialect/TrinoDialect.kt`
- `src/main/kotlin/io/bluetape4k/exposed/trino/dialect/TrinoDialectMetadata.kt`

**TrinoDialect**:
- `PostgreSQLDialect(name = dialectName)` 상속
- `companion object : KLogging()` + `const val dialectName = "trino"`
- `supportsColumnTypeChange = false` (ALTER COLUMN TYPE 미지원)
- `supportsMultipleGeneratedKeys = false` (SERIAL/시퀀스 미지원)
- `supportsWindowFrameGroupsMode = true`
- `modifyColumn()` → `emptyList()` (no-op, BigQuery 패턴)

**TrinoDialectMetadata**:
- `PostgreSQLDialectMetadata()` 상속
- `fillConstraintCacheForTables()` → no-op (Trino JDBC getImportedKeys 미지원)

**참고**: 구조는 DuckDB 패턴(`data/exposed-duckdb/src/main/kotlin/io/bluetape4k/exposed/duckdb/dialect/`), capability 플래그(`supportsColumnTypeChange`, `supportsMultipleGeneratedKeys`, `modifyColumn()` 등)는 BigQueryDialect 참조

> **주의**: `modifyColumn()` override 시 `@OptIn(InternalApi::class)` 어노테이션 필요

**검증**: 컴파일 성공

---

### T3: TrinoConnectionWrapper
`complexity: high`

**파일**: `src/main/kotlin/io/bluetape4k/exposed/trino/TrinoConnectionWrapper.kt`

핵심 Exposed 프레임워크 호환 어댑터:
- `internal class TrinoConnectionWrapper(private val conn: Connection) : Connection by conn`
- `getAutoCommit()` → 항상 `true`
- `setAutoCommit(autoCommit: Boolean)` → no-op
- `commit()` → no-op (Exposed 프레임워크 호환 어댑터)
- `rollback()` → no-op (Exposed 프레임워크 호환 어댑터)
- `prepareStatement(sql, autoGeneratedKeys)` → `conn.prepareStatement(sql)` 위임
- `prepareStatement(sql, columnIndexes)` → `conn.prepareStatement(sql)` 위임
- `prepareStatement(sql, columnNames)` → `conn.prepareStatement(sql)` 위임
- `@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")` 어노테이션

**KDoc**: autocommit 주의사항 경고 전문 포함 (원자성 미보장, rollback 불가, nested transaction 미지원, 부분 반영 위험)

**참고**: `DuckDBConnectionWrapper` + 스펙 D8 섹션 코드 블록

**검증**: 컴파일 성공

---

### T4: TrinoDatabase 팩토리
`complexity: high`

**파일**: `src/main/kotlin/io/bluetape4k/exposed/trino/TrinoDatabase.kt`

- `object TrinoDatabase : KLogging()`
- `val DRIVER = "io.trino.jdbc.TrinoDriver"` (`val` — 객체 초기화 보장)
- `init {}`: `Database.registerJdbcDriver`, `DatabaseApi.registerDialect`, `Database.registerDialectMetadata`
- `connect(host, port, catalog, schema, user)`: `DriverManager.getConnection` → `TrinoConnectionWrapper` 래핑
- `connect(jdbcUrl, user)`: 동일 패턴
- 모든 connect에 `Properties().apply { setProperty("user", user) }` 전달
- Phase 2 `connect(dataSource)` 주석으로 남김

**KDoc**: 사용 예시(transaction, suspendTransaction, queryFlow) + autocommit 주의사항

**참고**: `DuckDBDatabase.kt` 동일 구조

**검증**: 컴파일 성공

---

### T5: TrinoExtensions
`complexity: medium`

**파일**: `src/main/kotlin/io/bluetape4k/exposed/trino/TrinoExtensions.kt`

- `suspend fun <T> suspendTransaction(db, dispatcher, block)`: `withContext(dispatcher) { transaction(db) { block() } }`
- `fun <T> queryFlow(db, dispatcher, block)`: `flow { ... withContext(dispatcher) { transaction(db) { block().toList() } } ... emit }`
- 기본 dispatcher: `Dispatchers.IO`

**KDoc**: autocommit 주의사항 경고 + 사용 예시 (DuckDBExtensions.kt 동일 패턴)

**참고**: `DuckDBExtensions.kt` 동일 구현

**검증**: 컴파일 성공

---

### T6: @TrinoUnsupported 어노테이션
`complexity: low`

**파일**: `src/main/kotlin/io/bluetape4k/exposed/trino/TrinoUnsupported.kt`

```kotlin
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class TrinoUnsupported(val reason: String = "")
```

**KDoc**: 한국어 설명 (Trino 미지원 기능 마커, 컴파일 타임 경고/테스트 건너뛰기 용도)

**검증**: 컴파일 성공

---

### T7: AbstractTrinoTest + 테스트 테이블 정의
`complexity: medium`

**파일**:
- `src/test/kotlin/io/bluetape4k/exposed/trino/AbstractTrinoTest.kt`
- `src/test/kotlin/io/bluetape4k/exposed/trino/domain/Events.kt`

**AbstractTrinoTest**:
- `companion object : KLogging()` + `val trino: TrinoServer by lazy { TrinoServer.Launcher.trino }`
- `val db: Database by lazy { TrinoDatabase.connect(host, port, catalog="memory", schema="default", user=trino.username) }`
- `withEventsTable(block)` 헬퍼: `SchemaUtils.create(Events)` → block 실행
  - Note: Memory 커넥터는 DELETE 미지원이므로 `deleteAll()` 대신 `SchemaUtils.drop` + `SchemaUtils.create` 패턴 사용

**Events 테이블**:
```kotlin
object Events : Table("events") {
    val eventId = long("event_id")
    val eventName = varchar("event_name", 255)
    val region = varchar("region", 50)
    val createdAt = timestamp("created_at").nullable()
    override val primaryKey = PrimaryKey(eventId)
}
```

**참고**: `AbstractDuckDBTest` 패턴 (단, duplicate 연결 불필요 — Trino는 서버 기반)

**검증**: 컴파일 성공

---

### T8: TrinoDatabaseTest
`complexity: medium`

**파일**: `src/test/kotlin/io/bluetape4k/exposed/trino/TrinoDatabaseTest.kt`

테스트 시나리오:
- Dialect 등록 확인: `db.dialect` 인스턴스가 `TrinoDialect`임을 검증
- 연결 성공: `transaction(db) { exec("SELECT 1") }` 정상 실행
- 기본 메타데이터: `db.dialect.name == "trino"` 검증
- `connect(host, port, catalog, schema)` 방식 연결 성공
- `connect(jdbcUrl)` 방식 연결 성공
- capability 플래그 검증: `supportsColumnTypeChange == false`, `supportsMultipleGeneratedKeys == false`

**검증**: `./gradlew :bluetape4k-exposed-trino:test --tests "*.TrinoDatabaseTest"` 통과

---

### T8.1: TrinoConnectionWrapperTest
`complexity: high`

**파일**: `src/test/kotlin/io/bluetape4k/exposed/trino/TrinoConnectionWrapperTest.kt`

단위 테스트 (mock/proxy 기반 단위 테스트):
- `getAutoCommit()` 반환값이 항상 `true`
- `setAutoCommit(false)` 호출 시 no-op (예외 없이 무시, 이후 `getAutoCommit()` 여전히 `true`)
- `commit()` 호출 시 no-op (예외 없이 완료)
- `rollback()` 호출 시 no-op (예외 없이 완료)
- `prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)` 호출 시 `prepareStatement(sql)` 위임 검증 (T13 포함)
- `prepareStatement(sql, intArrayOf(1))` 호출 시 예외 없이 반환
- `prepareStatement(sql, arrayOf("col"))` 호출 시 예외 없이 반환

> **참고**: `DuckDBConnectionWrapperTest` 패턴 참조 (`data/exposed-duckdb/src/test/kotlin/io/bluetape4k/exposed/duckdb/DuckDBConnectionWrapperTest.kt`)
> mock/proxy 기반으로 작성하여 prepareStatement 위임이 wrapper 때문인지 드라이버 때문인지 분리 검증

**검증**: `./gradlew :bluetape4k-exposed-trino:test --tests "*.TrinoConnectionWrapperTest"` 통과

---

### T8.2: TrinoTransactionAtomicityTest
`complexity: high`

**파일**: `src/test/kotlin/io/bluetape4k/exposed/trino/TrinoTransactionAtomicityTest.kt`

핵심 테스트 — autocommit 모드의 부분 반영 동작을 명시적으로 검증:
- **부분 반영 테스트**: `transaction {}` 블록 내에서 INSERT 1건 실행 후 의도적 예외 발생 → 해당 INSERT가 DB에 **남아있음(롤백 안 됨)**을 검증
- **정상 흐름 베이스라인**: INSERT → INSERT → 성공 → 양쪽 모두 반영됨 검증
- **nested transaction 원자성 부재 검증**: Exposed `transaction { transaction { DML } }` 패턴이 예외 없이 호출은 허용되나 원자성 없음을 검증 — inner block의 DML이 outer rollback으로 취소되지 않음을 확인 (KDoc에 명시 필수)

목적: autocommit 모드의 부분 반영 위험 및 nested transaction 원자성 미보장을 테스트로 명문화

**검증**: `./gradlew :bluetape4k-exposed-trino:test --tests "*.TrinoTransactionAtomicityTest"` 통과

---

### T9: SchemaUtilsTest
`complexity: medium`

**파일**: `src/test/kotlin/io/bluetape4k/exposed/trino/SchemaUtilsTest.kt`

테스트 시나리오:
- `SchemaUtils.create(Events)` 성공
- `SchemaUtils.drop(Events)` 성공
- create 후 `Events.selectAll().count() == 0` (빈 테이블)
- create → drop → create 재생성 가능

**검증**: `./gradlew :bluetape4k-exposed-trino:test --tests "*.SchemaUtilsTest"` 통과

---

### T10: SelectTest
`complexity: medium`

**파일**: `src/test/kotlin/io/bluetape4k/exposed/trino/query/SelectTest.kt`

테스트 시나리오 (INSERT 후 SELECT):
- `selectAll()` — 전체 조회
- `where { region eq "kr" }` — 조건 조회
- `orderBy(eventId)` — 정렬
- `limit(n)` — 페이징
- `count()` — 집계
- `groupBy(region)` + `count()` — 그룹 집계

**검증**: `./gradlew :bluetape4k-exposed-trino:test --tests "*.SelectTest"` 통과

---

### T11: InsertTest
`complexity: medium`

**파일**: `src/test/kotlin/io/bluetape4k/exposed/trino/insert/InsertTest.kt`

테스트 시나리오:
- 단건 `insert` — 1건 INSERT 후 SELECT 검증
- `batchInsert` — N건 일괄 INSERT 후 count 검증
- nullable 컬럼 INSERT (`createdAt = null`)

**검증**: `./gradlew :bluetape4k-exposed-trino:test --tests "*.InsertTest"` 통과

---

### T12: TrinoExtensionsTest
`complexity: medium`

**파일**: `src/test/kotlin/io/bluetape4k/exposed/trino/TrinoExtensionsTest.kt`

테스트 시나리오:
- `suspendTransaction(db) { Events.selectAll().toList() }` — suspend 트랜잭션 정상 동작
- `suspendTransaction` with custom dispatcher — Virtual Thread dispatcher 등
- `queryFlow(db) { Events.selectAll() }.toList()` — Flow 수집 결과 검증
- `queryFlow` 빈 결과 시 빈 리스트 반환

**검증**: `./gradlew :bluetape4k-exposed-trino:test --tests "*.TrinoExtensionsTest"` 통과

---

### T13: Trino JDBC prepareStatement 호환성 검증
`complexity: low`

T8.1(`TrinoConnectionWrapperTest`) 내에서 함께 검증. 별도 파일 불필요.

---

### T14: CLAUDE.md 업데이트
`complexity: low`

루트 `CLAUDE.md`의 Architecture > Data > Exposed 테이블에 추가:

```markdown
| `exposed-trino` | Trino JDBC Dialect, TrinoDatabase, suspendTransaction/queryFlow; autocommit 전용 (트랜잭션 미지원) |
```

---

### T15: bluetape4k-patterns 체크리스트 검증 + 인접 모듈 회귀 검증
`complexity: low`

구현 완료 후 bluetape4k-patterns 체크리스트를 확인한다:

- 모든 public 클래스/인터페이스/확장 함수에 한국어 KDoc 작성 여부
- `companion object : KLogging()` 패턴 적용 여부 (TrinoDialect, TrinoDatabase, AbstractTrinoTest 등)
- public API 파라미터/반환 타입에 불필요한 `Any?` 또는 `Any` 사용 없는지 확인
- `data class` Record/Model에 `Serializable` + `serialVersionUID` 적용 여부 (해당 시)
- deprecated API 사용 없는지 `ide_diagnostics` 확인

**인접 모듈 회귀 테스트** (신규 모듈로 인한 사이드 이펙트 없음 확인):

```bash
./gradlew :bluetape4k-exposed-duckdb:test :bluetape4k-exposed-bigquery:test
```

**검증**: 체크리스트 항목 전체 이상 없음 확인 + 인접 모듈 테스트 통과

---

### T16: README.md 작성
`complexity: low`

**파일**: `data/exposed-trino/README.md`

필수 포함 섹션:
1. 모듈 개요 (Trino JDBC + Exposed Dialect)
2. 설치 방법 (`implementation(project(":bluetape4k-exposed-trino"))`)
3. 사용 예시 (transaction, suspendTransaction, queryFlow)
4. **트랜잭션 동작 주의사항** (autocommit 모드 표, 원자성 미보장, rollback no-op, 부분 반영 위험, nested transaction 허용되나 원자성 없음)
5. 지원/미지원 기능 표 — 두 섹션으로 구분:
   - **Trino 일반 계약** — dialect 수준의 지원/미지원 (범용, 모든 Trino 커넥터에 해당)
   - **Memory 커넥터 테스트 범위** — 테스트에서 사용하는 커넥터 기준 (테스트 환경 한정)
6. Phase 2 로드맵 (connect(dataSource), exposed-bigquery-trino)

---

## Execution Order

| 순서 | 태스크 | complexity | 병렬 가능 |
|------|--------|-----------|----------|
| 1 | T1: 모듈 scaffolding | low | - |
| 2 | T2: TrinoDialect + TrinoDialectMetadata | medium | - |
| 3 | T3: TrinoConnectionWrapper | high | T6과 병렬 |
| 3 | T6: @TrinoUnsupported | low | T3과 병렬 |
| 4 | T4: TrinoDatabase | high | - |
| 5 | T5: TrinoExtensions | medium | - |
| 6 | T7: AbstractTrinoTest + Events | medium | - |
| 7 | T8: TrinoDatabaseTest | medium | T8.1, T8.2, T9와 병렬 |
| 7 | T8.1: TrinoConnectionWrapperTest (T13 포함) | high | T8, T8.2, T9와 병렬 |
| 7 | T8.2: TrinoTransactionAtomicityTest | high | T8, T8.1, T9와 병렬 |
| 7 | T9: SchemaUtilsTest | medium | T8, T8.1, T8.2와 병렬 |
| 8 | T10: SelectTest | medium | T11, T12와 병렬 |
| 8 | T11: InsertTest | medium | T10, T12와 병렬 |
| 8 | T12: TrinoExtensionsTest | medium | T10, T11과 병렬 |
| 9 | T14: CLAUDE.md | low | T15, T16와 병렬 |
| 9 | T15: bluetape4k-patterns 체크리스트 | low | T14, T16와 병렬 |
| 9 | T16: README.md | low | T14, T15와 병렬 |

---

## Verification

- `./gradlew :bluetape4k-exposed-trino:build` 성공
- `./gradlew :bluetape4k-exposed-trino:test` 전체 테스트 통과
- `./gradlew :bluetape4k-exposed-trino:detekt` 통과
- 기존 `exposed-bigquery`, `exposed-duckdb` 모듈 영향 없음 확인
- TrinoServer Testcontainer 시작/재사용 확인

---

## Risks & Mitigations

| 리스크 | 완화 |
|--------|------|
| Trino Memory 커넥터 UPDATE/DELETE 미지원 (테스트 환경 한정) | INSERT + SELECT 위주 테스트, withEventsTable에서 drop+create 패턴 |
| Trino 컨테이너 시작 시간 (30-60초) | `TrinoServer.Launcher` 싱글턴 + `reuse = true` |
| Trino JDBC prepareStatement 오버로드 미지원 | `TrinoConnectionWrapper`에서 위임 처리 (T3) |
| PostgreSQLDialect SQL 생성 비호환 | Phase 1은 기본 SELECT/INSERT만 → 점진적 오버라이드 추가 |
| `Libs.trino_jdbc` 버전 475 기준 | TrinoServer TAG와 동일 버전 사용으로 호환 보장 |
| nested transaction 원자성 오해 가능성 | KDoc + README에 "호출 허용, 원자성 없음" 명시; T8.2로 동작 명문화 |
| Memory 커넥터 제약을 범용 Trino 제약으로 오해 | README 지원/미지원 표를 "Trino 일반 계약"과 "Memory 커넥터 테스트 범위(테스트 환경 한정)" 두 섹션으로 분리 |
