# Implementation Plan: data/exposed-clickhouse — ClickHouse Exposed Dialect

- Date: 2026-04-25
- Author: planner (general-purpose agent)
- Spec: [`docs/superpowers/specs/2026-04-25-exposed-clickhouse-design.md`](../specs/2026-04-25-exposed-clickhouse-design.md)
- Issue: #145
- Branch: `feat/exposed-clickhouse` (worktree at `.worktrees/exposed-clickhouse`)
- Total tasks: **18** (T1–T18)
- Complexity distribution: **High 5 / Medium 8 / Low 5**

---

## 0. 진행 규칙 (Plan-wide)

- 모든 작업은 worktree `.worktrees/exposed-clickhouse/` 내에서 수행
- 각 `.kt` 편집 후 `lsp_diagnostics` 즉시 점검 → import 오류는 `lsp_optimize_imports` 적용
- 각 태스크 완료 시 모듈 단위 테스트 실행:
  `./gradlew :bluetape4k-exposed-clickhouse:test` (필요 시 `--tests` 필터)
- 새 파일 생성 시 KDoc(한국어) 필수, public service/factory에 `companion object : KLogging()` 추가
- Kluent 비교 matcher 사용: `shouldBeEqualTo`, `shouldBeGreaterOrEqualTo`, `shouldBeInRange` 등
- atomicfu는 클래스 프로퍼티에서만 사용

---

## 1. 의존성 / 병렬 실행 그래프

```
T1 (worktree)
 └── T2 (skeleton)
      └── T3 (DB/ConnWrapper/Dialect/Metadata)  ─┐
            ├── T4 (Extensions)                  │
            ├── T5 (Basic+Unsigned column types) │
            │     └── T6 (DateTime64/LowCard/Array/Nullable/FixedString/Date32)
            │           └── T8 (Date functions)
            │                 └── T9 (Aggregate functions)
            ├── T7 (Engine + ClickHouseTable)    │
            │     ├── T10 (BatchInsert)          │
            │     └── T11 (SchemaUtils + DDL filter)
            └── T12 (PoC: requiresAutoCommitOnCreateDrop, commit/rollback)
T10 ─── T13 (BatchInsert benchmark)
T9, T10 ── T14 (README ko/en)
T9, T10, T14 ── T15 (examples: oltp-olap)
T11, T13, T14, T15 ──┐
                      ├── T16 (atomicfu/KLogging audit)
                      ├── T17 (code-reviewer 통과)
                      └── T18 (PR 생성)
```

### 병렬 실행 가능 그룹

- **Group A** (T3 후): T4, T5, T7, T12 동시 진행 가능
- **Group B** (T6 후): T8 시작 / T7 후: T10, T11 동시 진행 가능
- **Group C** (T10 후): T13, T14 병렬 가능; T15는 T14 완료 후 시작

---

## 2. 태스크 상세

### T1. 워크트리 생성

- **complexity**: low
- **의존성**: 없음
- **산출물**:
  - `.worktrees/exposed-clickhouse/` 디렉토리
  - 브랜치 `feat/exposed-clickhouse`
- **명령**:
  ```bash
  git worktree add .worktrees/exposed-clickhouse -b feat/exposed-clickhouse develop
  ```
- **검증 기준**:
  - `git worktree list`에 새 항목 존재
  - 새 worktree에서 `./bin/repo-status` 정상 실행

---

### T2. 모듈 골격 생성

- **complexity**: low
- **의존성**: T1
- **파일**:
  - `data/exposed-clickhouse/build.gradle.kts`
  - `data/exposed-clickhouse/README.md` (placeholder)
  - `data/exposed-clickhouse/README.ko.md` (placeholder)
  - `data/exposed-clickhouse/src/main/kotlin/io/bluetape4k/exposed/clickhouse/.gitkeep`
  - `data/exposed-clickhouse/src/test/kotlin/io/bluetape4k/exposed/clickhouse/.gitkeep`
  - `data/exposed-clickhouse/src/test/resources/junit-platform.properties`
  - `data/exposed-clickhouse/src/test/resources/logback-test.xml`
- **build.gradle.kts 핵심**:
  - **사전 확인**: `buildSrc/Libs.kt`에 `clickhouse_jdbc = "com.clickhouse:clickhouse-jdbc:0.9.5"` 및 `testcontainers_clickhouse` 정의 확인. 미정의 시 추가.
  - dependencies: `bluetape4k-logging`, `exposed_core`, `exposed_jdbc`, `exposed_java_time`, `kotlinx_coroutines_core`, `clickhouse_jdbc` (0.9.5)
  - testImplementation: `bluetape4k-junit5`, `bluetape4k-testcontainers`, `kotlinx_coroutines_test`, `testcontainers_clickhouse`
  - `configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()) }`
- **검증 기준**:
  - `./gradlew :bluetape4k-exposed-clickhouse:build -x test` 통과
  - `settings.gradle.kts`가 자동으로 모듈 등록 확인 (`./gradlew projects | grep clickhouse`)

---

### T3. ClickHouseDatabase / ConnectionWrapper / Dialect / DialectMetadata

- **complexity**: high
- **의존성**: T2
- **파일** (모두 신규):
  - `src/main/.../ClickHouseDatabase.kt`
  - `src/main/.../ClickHouseConnectionWrapper.kt`
  - `src/main/.../dialect/ClickHouseDialect.kt`
  - `src/main/.../dialect/ClickHouseDialectMetadata.kt`
  - `src/test/.../AbstractClickHouseTest.kt`
  - `src/test/.../ClickHouseDatabaseTest.kt`
  - `src/test/.../ClickHouseDatabaseValidationTest.kt`
  - `src/test/.../ClickHouseConnectionWrapperTest.kt`
  - `src/test/.../ClickHouseDialectTest.kt`
  - `src/test/.../domain/Events.kt` (테스트용 최소 테이블, T7에서 확장)
- **트랜잭션 방침 (C1 고정)**: `commit()`/`rollback()`은 **항상 no-op** (Spec C1 확정). T12 PoC는 driver raw 동작 검증용 — 결과와 무관하게 ConnectionWrapper의 no-op 구현은 변경 없음.
- **PoC 통합**: `requiresAutoCommitOnCreateDrop` 는 T12에서 별도 검증 후 flag 결정. T3 구현 시 `requiresAutoCommitOnCreateDrop = true`를 **기본값**으로 설정하고, T12 결과에 따라 조정.
- **`ClickHouseDialect` 핵심 override**:
  - `supportsColumnTypeChange = false`
  - `supportsMultipleGeneratedKeys = false`
  - `supportsRestrictReferenceOption = false`
  - `supportsSetDefaultReferenceOption = false`
  - `supportsCreateSequence = false`
  - `supportsTernaryAffectedRowValues = false`
  - `requiresAutoCommitOnCreateDrop = true` (PoC 결과 따라 조정)
  - `modifyColumn(..) = emptyList()`
- **검증 기준**:
  - `ClickHouseDatabase.connect(host, port, ...)` / `connect(jdbcUrl)` 양쪽 통과
  - `connect()`에서 host blank, port out-of-range, prefix 누락 시 `IllegalArgumentException`
  - `ConnectionWrapper`: `setAutoCommit(false)` 후에도 `getAutoCommit() == true` 보장
  - `commit()` / `rollback()` no-op (예외 없이 반환)
  - `prepareStatement` 3종 overload 모두 단일 인자로 위임
  - Testcontainers `ClickHouseServer.Launcher.clickhouse` 기동 후 `SELECT 1` 통과 (max 10 retry × 500ms)
  - `dialectName == "clickhouse"` 검증
  - **DDL filter 검증** (SchemaUtilsTest의 일부): ALTER TABLE ADD CONSTRAINT, CREATE/DROP SEQUENCE 발급 차단 (이 부분은 T11에서 강화)

---

### T4. ClickHouseExtensions (suspendTransaction, queryFlow)

- **complexity**: medium
- **의존성**: T3
- **파일**:
  - `src/main/.../ClickHouseExtensions.kt`
  - `src/test/.../ClickHouseExtensionsTest.kt`
- **API**:
  ```kotlin
  suspend fun <T> suspendTransaction(db: Database, dispatcher: CoroutineDispatcher = Dispatchers.IO, block: Transaction.() -> T): T
  fun <T> queryFlow(db: Database, dispatcher: CoroutineDispatcher = Dispatchers.IO, block: Transaction.() -> Iterable<T>): Flow<T>
  ```
- **요구사항**:
  - `withContext(dispatcher) { transaction(db) { block() } }` 구조
  - `queryFlow`는 materialize-then-emit (Trino 패턴)
  - `CancellationException` 재전파, KDoc에 "트랜잭션 원자성 없음" 경고 명시
- **검증 기준**:
  - 정상 결과 / 예외 전파 / cancellation 전파 테스트 모두 통과
  - `runTest(timeout = 30.seconds)` 사용

---

### T5. Basic + Unsigned + Signed Int + Float Column Types

- **complexity**: high
- **의존성**: T3
- **파일** (신규):
  - `src/main/.../types/BasicColumnTypes.kt`
    - `ClickHouseStringColumnType` (sqlType = `"String"`)
    - `ClickHouseFixedStringColumnType(n)` (sqlType = `"FixedString(n)"`)
    - `ClickHouseFloat32ColumnType` / `ClickHouseFloat64ColumnType`
    - `ClickHouseInt8/16/32/64ColumnType`
    - `ClickHouseNullableColumnType<T>(inner)` — `sqlType() = "Nullable(${inner.sqlType()})"`
    - 빌더: `chString`, `fixedString(n)`, `chFloat32`, `chFloat64`, `chInt8`, `chInt16`, `chInt32`, `chInt64`, `chNullable(name, inner)`
  - `src/main/.../types/UnsignedColumnTypes.kt`
    - `ClickHouseUByteColumnType` / `ClickHouseUShortColumnType` / `ClickHouseUIntColumnType` / `ClickHouseULongColumnType`
    - `ClickHouseUInt64BigIntColumnType` (BigInteger fallback)
    - `valueFromDB(value: Any)` — `Number` 폭넓게 수용 후 `toUByte()/toUShort()/toUInt()/toULong()`
    - 빌더: `chUByte`, `chUShort`, `chUInt`, `chULong`, `chUInt64BigInt`
  - `src/test/.../types/UnsignedTypesTest.kt`
    - UInt8/16/32/64 round-trip 테스트
    - `chFloat32`/`chFloat64` round-trip 테스트 케이스 포함 (별도 파일 만들지 않음)
    - ULong overflow → BigInt fallback 테스트
- **검증 기준** (T5는 `ColumnType.sqlType()` 단위 검증에 집중 — 실 DB DDL 검증은 T7/T11로 이동):
  - `sqlType()` 반환값 정확성: `UInt8`, `UInt16`, `UInt32`, `UInt64`, `Int8`, `Int16`, `Int32`, `Int64`, `Float32`, `Float64`, `String`, `FixedString(n)`, `Nullable(Int32)` 등
  - Round-trip (실 DB): 0 / max value / overflow boundary 모두 통과
  - `Number` cross-type cast (Short → UByte) 테스트 통과
  - **⚠ 전체 테이블 DDL 검증 (`CREATE TABLE` SQL)은 T7 완료 후 T11에서 수행** — T5 단계에서는 JDBC round-trip만 검증

---

### T6. DateTime64 + LowCardinality + Array + Date32

- **complexity**: high
- **의존성**: T5
- **파일** (신규):
  - `src/main/.../types/DateTime64ColumnType.kt` — `dateTime64(name, precision: Int = 3)`, `Instant` 매핑
  - `src/main/.../types/Date32ColumnType.kt` — `date32(name)` (LocalDate, ClickHouse Date32 DDL)
  - `src/main/.../types/LowCardinalityColumnType.kt`
    - `LowCardinalityColumnType<T>(inner)` — `sqlType() = "LowCardinality(${inner.sqlType()})"`
    - 빌더 1: `lowCardinalityString(name)` — inner = `ClickHouseStringColumnType` (안전 권장)
    - 빌더 2: `lowCardinality(name, innerType)` — caller 책임
  - `src/main/.../types/ArrayColumnType.kt`
    - `ClickHouseArrayColumnType<T>(inner)` — `sqlType() = "Array(${inner.sqlType()})"`
    - `Table.chArray(name, innerType)`
    - **JDBC 매핑 PoC 필요** (T6 구현 전 검증): ClickHouse JDBC 0.9.5가 `Array(T)` 컬럼을 `java.sql.Array` / `Array<*>` / `List<*>` / primitive array 중 어떤 타입으로 반환하는지 확인
    - `valueFromDB(value: Any)` — `java.sql.Array`, `Array<*>`, `List<*>`, `String` literal 모두 수용하는 방어적 구현 (UInt 패턴 답습)
    - `notNullValueToDB(value: List<T>)` — `java.sql.Array` 또는 `Array<Any?>` 형태로 변환 (드라이버 요구에 따라)
  - `src/test/.../types/DateTime64Test.kt` — precision 0/3/6/9
  - `src/test/.../types/LowCardinalityTest.kt` — DDL 검증 + value round-trip + **`ClickHouseNullableColumnType` round-trip 케이스 포함**
  - `src/test/.../types/ArrayTypeTest.kt` — `Array(Int32)`, `Array(String)`, 빈 리스트, `chArray(name, ClickHouseNullableColumnType(...))` 미지원 명시
  - `src/test/.../types/Date32Test.kt` — Date32 DDL/round-trip
- **검증 기준**:
  - DateTime64: `Instant` ↔ DB ms/us/ns 정밀도 보존 (precision=3 기본). **timezone 처리**: `DateTime64(3, 'UTC')` 명시 — ClickHouse server timezone 설정 무관 UTC 고정. T6 구현 전 JDBC 드라이버의 timezone 변환 동작 확인.
  - LowCardinality: DDL `LowCardinality(String)` 발급 확인
  - Array: PoC 결과 기반 `valueFromDB` 구현. 빈 리스트, 단일 원소, 다중 원소 round-trip. 드라이버가 예상 외 타입 반환 시 명확한 `IllegalStateException` 발생 확인
  - Nullable wrapping: `chNullable("col", chInt32)` → DDL `Nullable(Int32)` 정확 발급, null 값 round-trip

---

### T7. ClickHouseEngine + mergeTree DSL + ClickHouseTable

- **complexity**: high
- **의존성**: T3
- **파일** (신규):
  - `src/main/.../engine/ClickHouseEngine.kt`
    - sealed interface `ClickHouseEngine { fun toClause(): String }`
    - `Memory`, `Log` (object, `Serializable` + `serialVersionUID`)
    - `MergeTree(orderBy, partitionBy?, primaryKey, sampleBy?, settings)` (data class, `Serializable`)
    - `ReplacingMergeTree(versionColumn?, ...)`, `SummingMergeTree(sumColumns)`, `AggregatingMergeTree(...)` (data class)
  - `src/main/.../engine/EngineDsl.kt`
    - `MergeTreeBuilder` + `mergeTree { orderBy(...); partitionBy(...); settings(...) }`
    - `replacingMergeTree { ... }`, `summingMergeTree { ... }`, `aggregatingMergeTree { ... }`
  - `src/main/.../ClickHouseTable.kt`
    - `abstract class ClickHouseTable(name, engine: ClickHouseEngine) : Table(name)`
    - `createStatement()` override → **filter (CREATE TABLE만 유지) + sanitize + ENGINE 절 부착**
      - `filter { sql -> sql.trimStart().startsWith("CREATE TABLE", ignoreCase = true) }` — ALTER TABLE ADD CONSTRAINT, CREATE SEQUENCE, COMMENT ON 드롭
    - `sanitizeForClickHouse(sql: String): String` — 정규식 파이프라인 (참고: `data/exposed-trino/src/.../TrinoTable.kt` 의 `sanitizeForTrino`):
      1. `PRIMARY KEY ...` 제약 절 제거
      2. `CONSTRAINT ... PRIMARY KEY (...)` 절 제거
      3. **`NOT NULL` AND `NULL` 토큰 모두 제거** — Exposed 1.2 `Column.descriptionDdl()`은 nullable이면 ` NULL`, 아니면 ` NOT NULL`을 sqlType() 뒤에 추가함. ClickHouse에서 nullable 여부는 `Nullable(T)` sqlType으로 표현하므로 두 토큰 모두 제거해야 깨끗한 DDL 생성
      4. `REFERENCES ...` 절 제거 (FK 미지원)
    - **ClickHouseNullableColumnType 계약**: `sqlType() = "Nullable(${inner.sqlType()})"` 반환. `columnType.nullable = true` 설정 불필요 (오히려 Exposed가 ` NULL`을 붙여 혼란 발생). 구현 시 `columnType.nullable = false`로 유지하고, sanitize에서 NOT NULL/NULL 모두 제거하는 방식으로 통일.
  - `src/main/.../ClickHouseUnsupported.kt` — helper functions (insertIgnore/upsert 등 호출 시 명확한 에러 메시지)
  - `src/test/.../engine/MergeTreeDslTest.kt`
- **검증 기준**:
  - 모든 ENGINE의 `toClause()` SQL 정확 발급
  - `mergeTree { orderBy("a", "b"); partitionBy("toYYYYMM(c)"); settings("index_granularity" to "8192") }` → `ENGINE = MergeTree() ORDER BY (a, b) PARTITION BY toYYYYMM(c) SETTINGS index_granularity = 8192`
  - `Table.createStatement()`가 PRIMARY KEY/FK/NOT NULL/NULL 절 모두 제거 후 ENGINE 부착
  - `Nullable(Int32)` 타입 DDL 정확 발급 (`Nullable(Int32)` — ` NULL` suffix 없음)
  - `Serializable` 직렬화 round-trip 테스트
  - **column comment 손실 caveat**: `createStatement()` filter가 `commentStatements`도 제거함 — 의도된 동작. README Caveats + KDoc에 "ClickHouseTable은 column comment DDL을 지원하지 않음" 명시 (T14에서)

---

### T8. Date Functions

- **complexity**: medium
- **의존성**: T6
- **파일**:
  - `src/main/.../functions/DateFunctions.kt`
    - `enum class DateDiffUnit { second, minute, hour, day, week, month, quarter, year }`
    - `Expression<T>.toYYYYMM(): Function<Int>`
    - `Expression<T>.toYYYYMMDD(): Function<Int>`
    - `dateDiff(unit, from, to): Function<Long>`
    - `Expression<T>.toStartOfInterval(intervalSeconds): Function<Instant>`
  - `src/test/.../functions/DateFunctionsTest.kt`
- **검증 기준**:
  - 각 Function의 `toQueryBuilder` SQL 정확 출력
  - 실 DB 쿼리 결과 검증 (Events 테이블 활용)

---

### T9. Aggregate Functions

- **complexity**: medium
- **의존성**: T8
- **파일**:
  - `src/main/.../functions/AggregateFunctions.kt`
    - `argMax(value, key): Function<V>`
    - `argMin(value, key): Function<V>`
    - `quantile(level: Double, expr): Function<Double>` — `require(level in 0.0..1.0)` 검증
    - `uniq(vararg exprs): Function<Long>`
    - `uniqExact(vararg exprs): Function<Long>`
  - `src/test/.../functions/AggregateFunctionsTest.kt`
- **검증 기준**:
  - level 범위 외 값 → `IllegalArgumentException`
  - 실 데이터 집계 결과 검증 (1만 건 INSERT 후 quantile 0.5/0.95)

---

### T10. BatchInsert + 트랜잭션 원자성 부재 명시 테스트

- **complexity**: medium
- **의존성**: T7
- **파일**:
  - `src/test/.../insert/BatchInsertTest.kt`
- **검증 기준**:
  - 1만 건 `Events.batchInsert(...)` 정상 INSERT
  - `transaction { batchInsert(half); throw RuntimeException() }` 후 → 이미 커밋된 행이 남아있음을 확인 (트랜잭션 원자성 없음 명시)
  - Spec R3 KDoc 경고와 일치하는 동작 검증

---

### T11. SchemaUtilsTest + DDL Filter 검증

- **complexity**: medium
- **의존성**: T7
- **파일**:
  - `src/test/.../SchemaUtilsTest.kt`
- **검증 기준**:
  - MergeTree 테이블 `SchemaUtils.create(Events)` → ENGINE 절 포함 SQL 발급 확인
  - **ALTER TABLE ADD CONSTRAINT 발급 차단 검증** (FK 관계 테이블 케이스)
  - **CREATE SEQUENCE / DROP SEQUENCE 발급 차단 검증** (Exposed sequence 사용 시)
  - `SchemaUtils.drop(Events)` → 정상 DROP
  - 테이블 재생성 후 데이터 손실 확인

---

### T12. PoC: ClickHouse 트랜잭션 동작 + requiresAutoCommitOnCreateDrop

- **complexity**: medium
- **의존성**: T3
- **목표**:
  - **(C1)** ClickHouse JDBC 0.9.5에서 raw `Connection.commit()` / `rollback()` 호출이 throw하는지 / no-op인지 검증
  - **(C2)** `requiresAutoCommitOnCreateDrop = true` PoC 검증 — DDL CREATE/DROP 시 autoCommit이 필요한지 확인
- **방법**:
  - 임시 테스트 (`docs/superpowers/research/2026-04-25-clickhouse-jdbc-tx-poc.md` 작성)
  - JDBC raw connection으로 `commit()` / `rollback()` 호출 → 결과 기록
  - `requiresAutoCommitOnCreateDrop = false` vs `true` 양쪽으로 SchemaUtils.create 시도
- **산출물**:
  - PoC 결과 markdown
  - T3 `ClickHouseDialect`의 flag 최종 결정 반영
  - `ClickHouseConnectionWrapper` KDoc에 측정 결과 기록
- **검증 기준**:
  - PoC 결과를 spec OQ1에 반영 (spec → "RESOLVED" 표기)

---

### T13. BatchInsert 성능 측정

- **complexity**: low
- **의존성**: T10
- **목표**: Spec C7 결정 — Exposed 기본 batchInsert vs `INSERT ... FORMAT Values` 직발급
- **방법**:
  - 10만 건 INSERT 시간 측정 (3회 평균)
  - `docs/superpowers/research/2026-04-25-clickhouse-batchinsert-bench.md` 기록
- **검증 기준**:
  - 결과가 임계치(예: 500ms/만건) 초과 시 → 후속 issue 등록
  - 임계치 이내 → C7 = 기본 batchInsert 사용 확정

---

### T14. README.md + README.ko.md (Mermaid 포함)

- **complexity**: medium
- **의존성**: T10
- **파일**:
  - `data/exposed-clickhouse/README.md` (영어)
  - `data/exposed-clickhouse/README.ko.md` (한국어)
- **요구사항**:
  - 5섹션: Overview / Architecture / Features / Examples / Caveats
  - 언어 전환 링크: 제목 바로 아래 `[한국어](./README.ko.md) | English` / `한국어 | [English](./README.md)`
  - Mermaid 다이어그램 (no Vega-Lite):
    - 모듈 의존성 그래프
    - MergeTree DDL flow
  - Caveats 섹션:
    - 트랜잭션 원자성 없음
    - ALTER 제한 (modifyColumn empty)
    - R2DBC 미지원
    - LowCardinality inner sqlType 계약
    - HikariCP 권장 설정 메모 (OQ5)
- **검증 기준**:
  - GitHub/IntelliJ에서 Mermaid 렌더링 확인
  - `qmd` 인덱싱 정상 (자동)

---

### T15. 예제: examples/exposed-clickhouse-oltp-olap

- **complexity**: high
- **의존성**: T9, T10, T14
- **파일** (신규):
  - `examples/exposed-clickhouse-oltp-olap/build.gradle.kts`
  - `examples/exposed-clickhouse-oltp-olap/README.md` + `README.ko.md`
  - `src/main/kotlin/io/bluetape4k/examples/clickhouse/oltpolap/App.kt`
  - `domain/postgres/Orders.kt`, `OrdersRepository.kt`
  - `domain/clickhouse/OrderEventsAnalytics.kt`, `AnalyticsRepository.kt`
  - `pipeline/OrderEventForwarder.kt` (코루틴 worker, at-least-once)
  - `api/DashboardService.kt` (argMax/quantile/uniqExact)
- **검증 기준**:
  - 실제 Gradle 모듈명 확인: `./gradlew projects | grep clickhouse-oltp` → `bluetape4k-examples-exposed-clickhouse-oltp-olap`
  - 예제 컴파일 통과: `./gradlew :bluetape4k-examples-exposed-clickhouse-oltp-olap:build -x test`
  - 최소 1개 통합 테스트 (Testcontainers PostgreSQL + ClickHouse 동시) — Orders → OrderEventsAnalytics 흐름 검증
  - `argMax`, `quantile(0.95)`, `uniqExact` 결과 검증
  - settings.gradle.kts가 examples/ 자동 등록 확인 (별도 수정 불필요)

---

### T16. Code Quality Audit

- **complexity**: low
- **의존성**: T11, T13, T14, T15
- **체크리스트**:
  - [ ] 모든 public 클래스/extension function에 한국어 KDoc
  - [ ] Public service/factory class에 `companion object : KLogging()`
  - [ ] sealed object(Memory/Log)에 KLogging 미적용 확인
  - [ ] `ClickHouseEngine` 모든 구현체 `Serializable` + `serialVersionUID = 1L`
  - [ ] atomicfu 사용 시 클래스 프로퍼티만
  - [ ] Detekt 통과 (`./gradlew :bluetape4k-exposed-clickhouse:detekt`)
  - [ ] `lsp_diagnostics` 0 errors
- **검증 기준**: 위 모든 항목 ✓

---

### T17. Code Reviewer 통과

- **complexity**: medium
- **의존성**: T16
- **방법**:
  - `/oh-my-claudecode:code-reviewer` (또는 `pr-review-toolkit:code-reviewer`) 에이전트 실행
  - HIGH/CRITICAL 이슈 모두 해결
  - MEDIUM 이슈는 가능한 만큼 해결
- **산출물**: 리뷰 보고서 + 수정 사항 적용된 commit
- **검증 기준**: 리뷰어 재검증 ✓

---

### T18. PR 생성 (Issue #145)

- **complexity**: low
- **의존성**: T17
- **사전 체크리스트** (CLAUDE.md "Before Creating a PR"):
  - [ ] `./gradlew :bluetape4k-exposed-clickhouse:test` 전수 통과 (passing count + duration 기록)
  - [ ] README.md + README.ko.md 둘 다 갱신
  - [ ] KDoc 신규 public API 모두 작성
  - [ ] Worktree 안에서 작업 완료
  - [ ] superpowers index 업데이트: `docs/superpowers/index/2026-04.md` + `INDEX.md` 카운트
  - [ ] **CLAUDE.md 루트 갱신**: `data/exposed-clickhouse` 모듈을 `data/` 모듈 그룹표에 추가
- **PR 본문**:
  - Issue #145 link
  - 테스트 결과 표
  - C1/C2/C7 PoC 결정 요약 (T12, T13)
  - DDL filter / sanitize 동작 설명
  - 검증 명령어 목록
- **명령**:
  ```bash
  gh pr create --base develop --head feat/exposed-clickhouse \
    --title "feat: data/exposed-clickhouse — ClickHouse Exposed Dialect (#145)" \
    --body-file <PR_BODY>
  ```
- **검증 기준**: PR open + CI 통과

---

## 3. Spec → Plan 매핑 표

| Spec 항목 | Plan 태스크 |
|-----------|-------------|
| 4.1 모듈 구조 | T2 |
| 4.2.1 ClickHouseDatabase | T3 |
| 4.2.2 ClickHouseConnectionWrapper | T3 |
| 4.2.3 ClickHouseDialect | T3 |
| 4.2.4 ClickHouseExtensions | T4 |
| 4.3 Column Types — String/FixedString/Float/SignedInt/Unsigned/Nullable | **T5 (확장)** |
| 4.3 Column Types — DateTime64/LowCardinality/Array/Date32 | **T6 (확장)** |
| 4.4 Date functions | T8 |
| 4.4 Aggregate functions | T9 |
| 4.4 LowCardinality 계약 | T6 + T14 |
| 4.5 MergeTree DSL + ClickHouseTable | T7 |
| 4.6 테스트 전략 + AbstractClickHouseTest | T3 |
| 4.6 SchemaUtilsTest + DDL filter | **T11 (확장)** |
| 4.6 BatchInsertTest | T10 |
| 4.7 examples 예제 | T15 |
| 4.8 build.gradle.kts | T2 |
| 4.9 README | T14 |
| 4.10 KDoc + Serializable | T16 |
| OQ1 (commit/rollback) | **T12 (확장: requiresAutoCommitOnCreateDrop)** |
| C7 (batch 성능) | T13 |

---

## 4. 완료 기준 (Definition of Done)

- [ ] 18개 태스크 모두 완료
- [ ] `./gradlew :bluetape4k-exposed-clickhouse:test` 전체 통과
- [ ] `./gradlew :bluetape4k-exposed-clickhouse:detekt` 통과
- [ ] code-reviewer HIGH/CRITICAL 이슈 0개
- [ ] PR #(생성됨) merged to `develop`
- [ ] Issue #145 close
- [ ] `docs/superpowers/index/2026-04.md` + `INDEX.md` 카운트 갱신
- [ ] `git worktree remove .worktrees/exposed-clickhouse` 실행
- [ ] `./bin/clean-branches` 실행

---

## 5. Complexity 요약

| Complexity | Count | 태스크 |
|------------|-------|--------|
| **High** | 5 | T3, T5, T6, T7, T15 |
| **Medium** | 8 | T4, T8, T9, T10, T11, T12, T14, T17 |
| **Low** | 5 | T1, T2, T13, T16, T18 |
| **Total** | **18** | |
