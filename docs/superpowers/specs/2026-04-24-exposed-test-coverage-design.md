# Exposed JDBC/R2DBC 테스트 커버리지 70% 달성 설계

- **작성일**: 2026-04-24
- **대상 모듈**: `data/exposed-jdbc`, `data/exposed-r2dbc`
- **목표**: 두 모듈의 라인/분기 커버리지를 70% 이상으로 끌어올림
- **작성자**: Claude (general-purpose agent)

---

## 1. 목표 및 범위

### 1.1 목표

1. `data/exposed-jdbc` 모듈 커버리지 ≥ 70%
2. `data/exposed-r2dbc` 모듈 커버리지 ≥ 70%
3. 현재 완전 누락 (테스트 파일 자체 없음)된 확장 함수·스키마 유틸리티를 전수 커버
4. 부분 누락 (테스트 파일은 있으나 특정 메서드 누락)된 Repository 메서드를 모두 커버
5. 회귀 방지: 동일한 `withTables` / `runTest` 패턴과 파라미터화된 dialect 매트릭스로 안전망 확보

### 1.2 범위 (IN)

- 신규 테스트 파일 2개 생성
- 기존 테스트 파일 4개 확장
- JUnit5 + bluetape4k-assertions + Testcontainers (H2/MariaDB/MySQL/PostgreSQL) 사용
- `@ParameterizedTest @MethodSource(ENABLE_DIALECTS_METHOD)` dialect 매트릭스

### 1.3 범위 (OUT)

- 새로운 프로덕션 코드 추가 (순수 테스트 보강 스펙)
- 스프링 부트 통합 테스트
- 벤치마크 / 성능 테스트
- 테스트 픽스쳐 재설계

---

## 2. 현황 분석 (연구 결과)

### 2.1 exposed-jdbc 갭

| 대상                                                                                   | 유형              | 상태                         |
|----------------------------------------------------------------------------------------|-------------------|------------------------------|
| `SchemaUtilsExtensions.execCreateMissingTablesAndColumns`                              | 확장 함수         | 테스트 파일 없음 (완전 누락) |
| `SoftDeletedJdbcRepository.findActivePage(pageNumber, pageSize, sortOrder, predicate)` | Repository 메서드 | 부분 누락                    |
| `SoftDeletedJdbcRepository.softDeleteAll(predicate)`                                   | Repository 메서드 | 부분 누락                    |
| `SoftDeletedJdbcRepository.restoreAll(predicate)`                                      | Repository 메서드 | 부분 누락                    |
| `SoftDeletedJdbcRepository.countActive(predicate)`                                     | Repository 메서드 | 부분 누락                    |
| `SoftDeletedJdbcRepository.countDeleted(predicate)`                                    | Repository 메서드 | 부분 누락                    |

### 2.2 exposed-r2dbc 갭

| 대상                                                              | 유형              | 상태                         |
|-------------------------------------------------------------------|-------------------|------------------------------|
| `TableExtensions.suspendColumnMetadata`                           | suspend 확장      | 테스트 파일 없음 (완전 누락) |
| `TableExtensions.suspendIndexes`                                  | suspend 확장      | 테스트 파일 없음 (완전 누락) |
| `TableExtensions.suspendPrimaryKeyMetadata`                       | suspend 확장      | 테스트 파일 없음 (완전 누락) |
| `TableExtensions.suspendSequences`                                | suspend 확장      | 테스트 파일 없음 (완전 누락) |
| `SoftDeletedR2dbcRepository.findActivePage(…, predicate)`         | Repository 메서드 | 부분 누락                    |
| `SoftDeletedR2dbcRepository.softDeleteAll(predicate)`             | Repository 메서드 | 부분 누락                    |
| `SoftDeletedR2dbcRepository.restoreAll(predicate)`                | Repository 메서드 | 부분 누락                    |
| `ReadableExtensions.getUuidOrNull(index / label)`                 | 확장 함수         | 부분 누락                    |
| `ReadableExtensions.getExposedBlob / getExposedBlobOrNull(index)` | 확장 함수         | 부분 누락                    |
| `QueryExtensions.Query.forEach(block)` (suspend)                  | 확장 함수         | 부분 누락 (DB 필요)          |
| `QueryExtensions.Query.forEachIndexed(block)` (suspend)           | 확장 함수         | 부분 누락 (DB 필요)          |

---

## 3. 설계 접근법

### 3.1 접근법 결정

**접근법 2 채택**: 완전 누락된 대상은 신규 전용 테스트 파일을 생성하고, 부분 누락된 대상은 기존 테스트 파일에 테스트 메서드를 추가한다.

- 파일 크기 관리 (모듈당 800 lines 가이드 준수)
- 관심사 분리 (SRP)
- 기존 테스트 인프라 최대 재사용 (`AbstractExposedTest`, `AbstractExposedR2dbcTest`, `withTables`)

### 3.2 공통 테스트 패턴 규칙

- **Base class**: jdbc는 `AbstractExposedTest`, r2dbc는 `AbstractExposedR2dbcTest`
- **Dialect 매트릭스**: `@ParameterizedTest @MethodSource(ENABLE_DIALECTS_METHOD)` (필요 시 `ALL_DIALECTS_METHOD`)
- **Wrapper**: jdbc는 `withTables(testDB, TableA, TableB) { … }`, r2dbc는 `runTest { withTables(testDB, …) { … } }` 또는
  `runSuspendIO { }`
- **Assertions**: bluetape4k-assertions (`shouldBeEqualTo`, `shouldBeNull`, `shouldContainSame`, `shouldBeGreaterThan`)
- **Logging**: 모든 테스트 클래스에 `companion object : KLogging()` 추가
- **KDoc**: 각 테스트 클래스와 주요 테스트 메서드에 한글 KDoc (언어 정책상 skill/rules는 영어지만 테스트 KDoc은 한글 허용)
- **Fixture**: Fakers (`faker`) 사용, 시드 데이터는 테스트 내에서 생성
- **Isolation**: 각 테스트는 독립 트랜잭션; 상태 공유 금지
- **Cleanup**: `withTables`가 트랜잭션 롤백/드롭 처리

---

## 4. 신규 테스트 파일

### 4.1 `exposed-jdbc/src/test/kotlin/.../schema/SchemaUtilsExtensionsTest.kt`

**대상**: `fun JdbcTransaction.execCreateMissingTablesAndColumns(vararg tables: Table)`

**테스트 케이스**:

1. `create missing tables when tables do not exist` — 존재하지 않는 테이블을 전달하면 생성되어야 함. 생성 후
   `SchemaUtils.tableExists(table)` 가 true.
2. `no-op when tables already exist with identical columns` — 이미 존재하는 테이블을 전달하면 에러 없이 완료. 스키마 변경 없음.
3. `add missing columns to existing table` — 기존 테이블에 새 컬럼이 추가된 Table 정의를 전달하면 해당 컬럼이 실제 DB에 추가됨.
4. `handle multiple tables in one call` — 3개 이상 테이블을 가변인자로 전달하여 모두 생성되는지.
5. `idempotent on repeated calls` — 동일한 호출을 2회 반복해도 오류 없이 동작.

**검증 포인트**: 호출 전/후 `SchemaUtils.tablesInDatabase()` 혹은 `ResultSet` 기반 컬럼 메타데이터 비교.

**Dialect**: H2 + MySQL + PostgreSQL (최소 3종).

### 4.2 `exposed-r2dbc/src/test/kotlin/.../schema/TableExtensionsTest.kt`

**대상**: `suspendColumnMetadata`, `suspendIndexes`, `suspendPrimaryKeyMetadata`, `suspendSequences`

**테스트 케이스**:

1. `suspendColumnMetadata returns all columns of table` — 정의된 컬럼 개수·이름·nullable 여부 일치.
2. `suspendIndexes returns declared indexes` — `index()` 로 선언한 인덱스가 결과에 포함.
3. `suspendPrimaryKeyMetadata returns PK for table with id` — PK 컬럼명/이름 일치.
4. `suspendPrimaryKeyMetadata returns null for tables without PK` — 명시적 PK 없는 Table에서 null.
5. `suspendSequences returns sequences declared in schema` — `Sequence("seq_test")` 선언 후 목록에 포함 (PostgreSQL 우선).
6. `cancellation propagates to underlying suspend calls` — `withTimeout` 하에 취소가 정상 전파.

**Dialect**: 기본 H2 + PostgreSQL (시퀀스 테스트 필수).

---

## 5. 기존 파일 확장

### 5.1 `exposed-jdbc` → `SoftDeletedJdbcRepositoryTest.kt`

추가할 테스트:

1.

`findActivePage returns paginated active rows respecting predicate` — 활성 엔티티 30건, 삭제 5건 삽입 후 페이지 크기 10 으로 3페이지 조회 · predicate 조건 검증.

2. `findActivePage honors sortOrder ASC vs DESC` — 동일 조건에서 sortOrder 변경 시 결과 순서 역순.
3. `softDeleteAll marks matching rows as deleted` — predicate 매칭 행만 `isDeleted=true` · 비매칭 행은 그대로.
4. `softDeleteAll returns updated count` — 반환값이 실제 영향 행 수와 일치.
5. `restoreAll reverts soft-deleted rows matching predicate` — 소프트 삭제된 행 중 predicate 매칭만 복원.
6. `restoreAll does not touch active rows` — 이미 활성인 행은 변경되지 않음.
7. `countActive with predicate filters correctly` — 조건부 카운트 정확성.
8. `countDeleted with predicate filters correctly` — 삭제된 행 중 조건 매칭만 카운트.
9. `countActive and countDeleted sum to total for partition predicate` — 보완관계 검증.

### 5.2 `exposed-r2dbc` → `SoftDeletedR2dbcRepositoryTest.kt`

추가할 테스트 (모두 `runTest { withTables(testDB, …) { … } }`):

1. `findActivePage returns paginated active rows respecting predicate` (suspend 버전)
2. `findActivePage with different sortOrder returns reversed sequence`
3. `softDeleteAll soft-deletes rows matching predicate`
4. `softDeleteAll returns correct affected row count`
5. `restoreAll restores only predicate-matched deleted rows`
6. `restoreAll is no-op when no rows match`

### 5.3 `exposed-r2dbc` → `ReadableExtensionsTest.kt`

추가할 테스트:

1. `getUuidOrNull by index returns value when present` — UUID 컬럼이 있는 row에서 인덱스 기반 조회.
2. `getUuidOrNull by index returns null when column is null` — NULL 값이 저장된 경우 null.
3. `getUuidOrNull by label returns value when present`
4. `getUuidOrNull by label returns null when null stored`
5. `getExposedBlob by index returns ExposedBlob wrapper`
6. `getExposedBlobOrNull by index returns null when column null`
7. `getExposedBlobOrNull by index returns ExposedBlob when present`

### 5.4 `exposed-r2dbc` → `QueryExtensionsTest.kt`

추가할 테스트 (DB 연결 필수):

1. `Query.forEach suspends and visits all rows exactly once` — 삽입한 N 건이 모두 방문되고 block 호출 횟수 = N.
2. `Query.forEach propagates exception from block` — block 내부에서 예외 던지면 전파.
3. `Query.forEachIndexed provides monotonically increasing index` — 0, 1, 2 … N-1.
4. `Query.forEachIndexed with where clause yields filtered subset` — 조건절 결합 시 인덱스도 매칭 행 기준.
5. `Query.forEach respects coroutine cancellation` — `withTimeout` 조기 취소 시 CancellationException.

---

## 6. 테스트 패턴 규칙 (보강)

### 6.1 Class skeleton (jdbc)

```
class XxxTest : AbstractExposedTest() {
    companion object : KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `description in backticks`(testDB: TestDB) {
        withTables(testDB, TableA) {
            // Arrange
            // Act
            // Assert
        }
    }
}
```

### 6.2 Class skeleton (r2dbc)

```
class XxxTest : AbstractExposedR2dbcTest() {
    companion object : KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `description in backticks`(testDB: TestDB) = runTest {
        withTables(testDB, TableA) {
            // Arrange / Act / Assert
        }
    }
}
```

### 6.3 규칙 요약

- `companion object : KLogging()` 필수
- `@ParameterizedTest @MethodSource(ENABLE_DIALECTS_METHOD)` 기본
- AAA 구조 (Arrange-Act-Assert)
- 한 테스트 하나의 책임
- bluetape4k-assertions 단일 assertion 우선, 복합 상태는 묶어서
- `Faker`로 동적 데이터 생성 → 결정적 검증이 필요하면 seeded 값 사용
- 외부 리소스 누수 금지 (`withTables`가 처리)
- Blob/UUID/Sequence 등 dialect 의존 기능은 `TestDB` 필터링

---

## 7. 태스크 목록 초안 (작업 단위)

| #   | 작업                                                              | 산출물          | 의존성 |
|-----|-------------------------------------------------------------------|-----------------|--------|
| T1  | jdbc `SchemaUtilsExtensionsTest.kt` 신규 작성 (5 케이스)          | 신규 파일       | -      |
| T2  | jdbc `SoftDeletedJdbcRepositoryTest.kt` 확장 (9 케이스 추가)      | 기존 파일 편집  | -      |
| T3  | r2dbc `TableExtensionsTest.kt` 신규 작성 (6 케이스)               | 신규 파일       | -      |
| T4  | r2dbc `SoftDeletedR2dbcRepositoryTest.kt` 확장 (6 케이스)         | 기존 파일 편집  | -      |
| T5  | r2dbc `ReadableExtensionsTest.kt` 확장 (7 케이스)                 | 기존 파일 편집  | -      |
| T6  | r2dbc `QueryExtensionsTest.kt` 확장 (5 케이스)                    | 기존 파일 편집  | -      |
| T7  | `./gradlew :bluetape4k-exposed-jdbc:test` 전체 실행 및 결과 확인  | 로그            | T1, T2 |
| T8  | `./gradlew :bluetape4k-exposed-r2dbc:test` 전체 실행 및 결과 확인 | 로그            | T3-T6  |
| T9  | 두 모듈 커버리지 측정 및 70% 달성 여부 검증                       | 커버리지 리포트 | T7, T8 |
| T10 | README.md + README.ko.md 갱신 (테스트 매트릭스 추가 시)           | 문서            | T9     |
| T11 | testlog 기록 (`docs/testlogs/2026-04.md` 맨 위 행)                | 문서            | T7, T8 |
| T12 | 커밋 + PR (worktree `.worktrees/exposed-test-coverage`)           | PR              | T1-T11 |

총 신규/수정 케이스: **38개** (5 + 9 + 6 + 6 + 7 + 5)

---

## 8. 검증 계획

1. **빌드**: `./gradlew :bluetape4k-exposed-jdbc:test :bluetape4k-exposed-r2dbc:test`
2. **커버리지**: 각 모듈별 라인/분기 커버리지 70% 이상 확인 (JaCoCo 또는 kover 대체 없음 → gradle 기본 리포트).
3. **Dialect 매트릭스**: H2 필수, 가능한 케이스는 MariaDB/MySQL/PostgreSQL 로 확장.
4. **Regression**: 기존 테스트 통과 유지 (`:test` 그린).
5. **Detekt**: `./gradlew :bluetape4k-exposed-jdbc:detekt :bluetape4k-exposed-r2dbc:detekt` (exposed-jdbc-tests 제외).

---

## 9. 리스크 및 완화

| 리스크                                        | 완화                                                      |
|-----------------------------------------------|-----------------------------------------------------------|
| 일부 dialect 에서 Sequence/Blob 미지원        | `TestDB` 화이트리스트로 스킵 처리                         |
| Testcontainers 기동 지연                      | 기존 컨테이너 재사용 (`@Container` companion static)      |
| R2DBC suspend 컨텍스트에서 트랜잭션 전파 이슈 | `runSuspendIO` 또는 `suspendTransaction` 일관 사용        |
| 커버리지 70% 미달                             | 태스크 T9 단계에서 누락 라인 추가 테스트 작성 (iteration) |

---

## 10. 완료 조건 (Definition of Done)

- [ ] T1-T6 모든 테스트 파일 생성/수정 완료
- [ ] `./gradlew :bluetape4k-exposed-jdbc:test :bluetape4k-exposed-r2dbc:test` green
- [ ] 커버리지 ≥ 70% (두 모듈 모두)
- [ ] Detekt 통과
- [ ] testlog 기록
- [ ] README 동기화 (필요 시)
- [ ] Worktree 기반 PR 생성
