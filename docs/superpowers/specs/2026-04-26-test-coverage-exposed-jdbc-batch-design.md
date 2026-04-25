# Test Coverage Improvement Spec — exposed-jdbc & utils/batch

- **작성일**: 2026-04-26
- **대상 모듈**: `data/exposed-jdbc`, `utils/batch`
- **목표 커버리지**: 라인 기준 70% 이상 (모듈별)
- **브랜치**: `test-coverage-batch` (worktree: `.worktrees/test-coverage-batch`)
- **관련 메모리**: bluetape4k 테스트 규칙(`feedback_write_and_verify_tests`, `feedback_kluent_comparison_matchers`, `feedback_no_environment_blame`)

---

## 1. 목표 및 배경

bluetape4k 의 두 핵심 인프라 모듈 `bluetape4k-exposed-jdbc` 와 `bluetape4k-batch` 는
다른 모듈(`spring-boot3/4-data-jdbc`, `data/exposed-jdbc-caffeine`, `utils/science` 등)이
의존하는 토대 모듈이지만, 현재 테스트는 happy-path 위주로 구성되어 있어
**JdbcRepository 의 26 개 public 메서드 중 16 개가 미커버**, **BatchStepRunner 의 retry/skip/timeout
복합 시나리오 미테스트** 등 정량적 커버리지 갭이 존재한다.

본 스펙의 목표는:

1. 두 모듈의 라인 커버리지를 **70% 이상**으로 끌어올린다.
2. 회귀 안전망 강화 — 향후 Exposed 0.x → 1.x 마이그레이션 및 BatchStepRunner 리팩토링 시
   바운더리 동작이 보존됨을 즉시 검증한다.
3. 멀티-DB 방언 차이(H2/PostgreSQL/MySQL_V8) 로 인한 silent regression 을 차단한다.

본 작업은 **소스 코드 수정 없이 테스트만 추가** 하는 것을 원칙으로 하되,
테스트 작성 중 발견된 명백한 버그는 별도 이슈로 분리한다.

---

## 2. 현재 상태 (커버리지 갭 테이블)

### 2.1 exposed-jdbc — JdbcRepository.kt 미커버 메서드

> 실제 시그니처는 `data/exposed-jdbc/src/main/kotlin/io/bluetape4k/exposed/jdbc/repository/JdbcRepository.kt` 참조.
> Count 계열은 `countBy(predicate: () -> Op<Boolean>)`와 `countBy(op: Op<Boolean>)` 두 오버로드가 모두 존재.
> Existence 계열은 `exists(query: AbstractQuery<*>)` (Op 직접 받지 않음), `existsBy(predicate: () -> Op<Boolean>)`, `existsById(id: ID)` 별도 메서드.
> 그 외 `batchUpsert` (Iterable/Sequence 2 오버로드), `findWithFilters`, `findBy`, `findByField`, `findByFieldOrNull`, `findAllByIds`도 존재.

| # | 메서드 시그니처 | 카테고리 | 우선순위 |
|---|------------------|----------|----------|
| 1 | `countBy(op: Op<Boolean>): Long` (Op 오버로드) | Count | High |
| 2 | `isEmpty(): Boolean` | Existence | High |
| 3 | `isNotEmpty(): Boolean` | Existence | High |
| 4 | `exists(query: AbstractQuery<*>): Boolean` | Existence | High |
| 5 | `existsBy(predicate: () -> Op<Boolean>): Boolean` | Existence | High |
| 6 | `existsById(id: ID): Boolean` | Existence | High |
| 7 | `findByIdOrNull(id: ID): E?` | Read | High |
| 8 | `findFirstOrNull(offset, predicate): E?` | Read | Medium |
| 9 | `findLastOrNull(offset, predicate): E?` | Read | Medium |
| 10 | `findAll(limit, offset, sortOrder, predicate)` 복합 인자 | Read | High |
| 11 | `findWithFilters(vararg filters, ...)` | Read | High |
| 12 | `findBy(vararg filters, ...)` | Read | Medium |
| 13 | `findByField(field, value)` / `findByFieldOrNull` | Read | High |
| 14 | `findAllByIds(ids: Iterable<ID>)` | Read | High |
| 15 | `findPage` edge case (totalCount=0, page=last) | Read | Medium |
| 16 | `batchUpsert(Iterable, vararg keys, ...)` | Write | High |
| 17 | `batchUpsert(Sequence, vararg keys, ...)` | Write | High |
| 18 | `updateAll(predicate, limit, body)` | Write | High |
| 19 | `deleteAll(limit, op): Int` | Write | High |
| 20 | `deleteAllIgnore(limit, op): Int` | Write | Medium |
| 21 | `deleteAllByIds(ids): Int` | Write | High |
| 22 | `deleteByIdIgnore(id): Int` | Write | Medium |
| 23 | `batchInsert(Sequence, ignore, shouldReturnGeneratedValues)` | Write | Medium |

### 2.1.1 exposed-jdbc — AuditableJdbcRepository / SoftDeletedJdbcRepository gap

`AuditableJdbcRepository` (실파일: `AuditableJdbcRepository.kt`) public API:
- `auditedUpdateById(id, limit, updateStatement): Int` — `updatedAt = CURRENT_TIMESTAMP`, `updatedBy = UserContext.getCurrentUser()` 자동 주입
- `auditedUpdateAll(predicate, limit, updateStatement): Int` — 동일

기존 `AuditableJdbcRepositoryTest.kt`는 `auditedUpdateById` happy-path 위주.
미커버 시나리오:
- `auditedUpdateAll` predicate로 batch 업데이트 + audit 필드 자동 주입 검증
- `UserContext.withUser(...)` 미설정(null) 상태에서 `updatedBy`가 null/default 처리되는지
- `auditedUpdateById`에 `limit` 인자 명시 시 동작
- `IntAuditableJdbcRepository` / `UUIDAuditableJdbcRepository` 변종에서도 동일 동작 (Long 외 타입 검증)

`SoftDeletedJdbcRepository` (실파일: `SoftDeletedJdbcRepository.kt`) public API:
- `softDeleteById(id)` / `restoreById(id)`
- `countActive(predicate)` / `countDeleted(predicate)`
- `findActive(limit, offset, sortOrder, predicate)` / `findDeleted(...)`
- `softDeleteAll(predicate): Int` / `restoreAll(predicate): Int`
- `findActivePage(pageNumber, pageSize, sortOrder, predicate): ExposedPage<E>`

기존 `SoftDeletedJdbcRepositoryTest.kt` 미커버 항목:
- `countActive` / `countDeleted` predicate 조합 검증
- `softDeleteAll` / `restoreAll` 의 영향 row 수 반환값 검증
- `findActivePage` 페이징 + soft-delete 필터 결합 검증
- `findDeleted` 의 sortOrder DESC + offset 동작

### 2.2 utils/batch — 미커버 시나리오

| # | 영역 | 미커버 시나리오 | 우선순위 |
|---|------|------------------|----------|
| 1 | `BatchStepRunner` | retry policy: 지수 백오프 동작 | High |
| 2 | `BatchStepRunner` | skip policy: 임계치 초과 시 step fail | High |
| 3 | `BatchStepRunner` | write timeout → `WriteTimeoutException` | High |
| 4 | `BatchStepRunner` | checkpoint 복원 후 재시작 (resume from offset) | High |
| 5 | `BatchStepRunner` | 코루틴 취소 시 graceful shutdown + commit | Medium |
| 6 | `ExposedJdbcBatchReader` | empty result, single chunk, partial last chunk | Medium |
| 7 | `ExposedJdbcBatchWriter` | duplicate key (ON CONFLICT 동작) | Medium |
| 8 | `ExposedR2dbcBatchReader/Writer` | 동일 edge cases (R2DBC 측) | Medium |
| 9 | `ResultRowMappers` | nullable column, type coercion, missing column | High |
| 10 | `SkipPolicy` | `LimitedSkipPolicy` 카운트 누적 / 초기화 | High |
| 11 | `BatchJobBuilder` / `BatchStepBuilder` | step 여러 개 조합, retryPolicy/skipPolicy/commitTimeout 결합 | Medium |
| 12 | `CheckpointJson` | malformed JSON, missing fields, 버전 불일치 | Medium |

---

## 3. 설계 결정 (브레인스토밍 결과)

### 3.1 위험/실패 모드

1. **DB 방언 차이로 인한 flakiness**
   - 위험: `batchUpsert` 는 H2(MERGE) 와 PostgreSQL(ON CONFLICT) 와 MySQL_V8(ON DUPLICATE KEY UPDATE) 가 다른 구문 생성. 한 DB 에서 통과해도 다른 DB 에서 실패 가능.
   - 완화: `batchUpsert` 테스트는 `@ParameterizedTest` 로 `TestDB.H2`, `TestDB.POSTGRESQL`, `TestDB.MYSQL_V8` 전수 적용. (실제 enum 명은 `TestDB.MYSQL_V8` — `MYSQL8` 아님. `data/exposed-jdbc-tests/.../TestDB.kt` 참조)
   - 검증 방법: `./gradlew :bluetape4k-exposed-jdbc:test` 단일 실행에서 모든 DB 컨테이너 기동 확인.

2. **코루틴 취소/타임아웃 테스트의 flakiness**
   - 위험: `BatchStepRunner` 의 timeout/cancellation 테스트가 머신 부하에 따라 간헐 실패.
   - 완화: `runTest(timeout = 30.seconds)` + `TestCoroutineScheduler` 기반 가상 시간 사용. 실제 wall-clock 의존 금지. `withTimeout` 동작 검증은 `delay()` mock 로 결정성 확보.
   - 검증 방법: 동일 테스트를 로컬에서 5회 반복 실행하여 재현성 확인.

3. **기존 테스트 코드를 깨지 않고 새 테스트 추가**
   - 위험: 같은 `Schema` 객체를 공유하는 테스트가 동시 실행 시 데이터 격리 실패.
   - 완화: 새 테스트는 `withTables(testDB, ...)` 블록 안에서 transaction 격리 유지. 새 도메인이 필요하면 별도 `Schema` 객체 신설(`JdbcRepositoryEdgeCaseSchema` 등).
   - 검증 방법: 기존 테스트 + 신규 테스트를 함께 실행하여 모두 pass 확인.

4. **r2dbc 테스트의 LeakingHikari/connection pool exhaustion**
   - 위험: 새 r2dbc 테스트가 connection 누수 시 후속 테스트 cascade 실패.
   - 완화: `runTest { withTables { ... } }` 로 트랜잭션 종료 후 connection 반환 보장. `AbstractBatchR2dbcTest` 의 lifecycle 활용.

5. **ResultRowMappers 단위 테스트의 Mock 복잡도**
   - 위험: `ResultRow` 는 internal API 가 많아 mocking 비용이 큼.
   - 완화: 실제 `Database.connect(H2)` + 임시 테이블 + `selectAll()` 결과로 통합 테스트화. mockk 최소화.

### 3.2 접근 방식 비교

| 방식 | 장점 | 단점 | 채택 |
|------|------|------|------|
| **A. 기존 파일 확장** | 기존 패턴/픽스처 재활용, diff 작음 | 파일 비대화(800 라인 룰 위반 위험), 무관한 테스트 섞임 | 부분 채택 |
| **B. 신규 파일 분리** | 책임 분리 명확, KISS, 파일 <400 라인 유지 | 픽스처 중복 가능 | 부분 채택 |
| **C. 혼합** | 자연스러운 응집도 — happy-path 보강은 기존 파일, 새 카테고리는 신규 파일 | 분류 기준 합의 필요 | **채택** |

**채택안 (C)**: 다음 규칙으로 분기.
- **happy-path 보강 (1~5 메서드 추가)** → 기존 파일에 메서드 추가
- **새로운 카테고리 (edge case / failure mode)** → 신규 파일 분리, 파일명 suffix 로 의도 표현 (`*EdgeCaseTest`, `*FailureTest`)
- **신규 파일은 200~400 라인 목표, 800 라인 절대 초과 금지**

### 3.3 멀티-DB 테스트 전략

- exposed-jdbc 는 **H2 / H2_MYSQL / MYSQL_V8 / POSTGRESQL** 4개 조합으로 `@ParameterizedTest` (`TestDB::class`) 적용. 실제 enum 이름은 `MYSQL_V8` (또한 `MYSQL_V5`, `H2_PSQL`, `H2_MARIADB` 등 존재).
- DB 의존적 동작 (`batchUpsert`, `batchInsert` with returning, `deleteIgnoreWhere`, isolation level) 은 **반드시 4 DB 전수**.
- DB 무관 로직 (`countBy`, `existsBy`, `existsById`, `isEmpty`) 은 **H2 단독 또는 H2 + POSTGRESQL** 2개로 충분.
- batch 모듈 jdbc 는 H2 우선 검증 후 PostgreSQL 통합 (`AbstractBatchJdbcTest` 패턴 활용).
- 활성 DB 집합 기본값은 `TestDB.enabledDialects()` = `{H2, POSTGRESQL, MYSQL_V8}` (useFastDB=true 시 `{H2}`).

---

## 4. exposed-jdbc 추가 테스트 목록

### 4.1 신규 파일: `JdbcRepositoryExistenceTest.kt`

| 테스트 함수 | 대상 메서드 | 테스트 DB |
|--------------|--------------|-----------|
| `isEmpty 는 빈 테이블에서 true 를 반환한다` | `isEmpty()` | H2, POSTGRESQL |
| `isEmpty 는 데이터 존재 시 false 를 반환한다` | `isEmpty()` | H2, POSTGRESQL |
| `isNotEmpty 는 isEmpty 의 부정값을 반환한다` | `isNotEmpty()` | H2 |
| `existsBy 는 조건에 맞는 row 가 있으면 true` | `existsBy(predicate)` | H2, POSTGRESQL, MYSQL_V8 |
| `existsBy 는 조건에 맞지 않으면 false` | `existsBy(predicate)` | H2 |
| `existsById 는 존재하는 id 에 true` | `existsById(id)` | H2 |
| `existsById 는 없는 id 에 false` | `existsById(id)` | H2 |
| `exists(query) 는 서브쿼리 결과 존재 여부 반환` | `exists(AbstractQuery<*>)` | H2 |
| `countBy(op) 는 Op 조건에 맞는 row 수를 반환` | `countBy(op: Op<Boolean>)` | H2, POSTGRESQL, MYSQL_V8 |
| `countBy(predicate) 람다 오버로드 동작` | `countBy(() -> Op)` | H2 |
| `countBy 결과 0 일 때도 정상 동작` | `countBy` | H2 |

### 4.2 신규 파일: `JdbcRepositoryReadEdgeCaseTest.kt`

| 테스트 함수 | 대상 메서드 | 테스트 DB |
|--------------|--------------|-----------|
| `findByIdOrNull 은 존재하지 않는 id 에 null 반환` | `findByIdOrNull` | H2 |
| `findByIdOrNull 은 존재하는 id 에 엔티티 반환` | `findByIdOrNull` | H2, POSTGRESQL |
| `findFirstOrNull 은 predicate + offset 조합 동작` | `findFirstOrNull(offset, predicate)` | H2 |
| `findLastOrNull 은 PK 역순 첫 매칭 row 반환` | `findLastOrNull(offset, predicate)` | H2 |
| `findAll 은 limit + offset + sortOrder + predicate 조합 동작` | `findAll(limit, offset, sortOrder, predicate)` | H2, POSTGRESQL |
| `findWithFilters 는 vararg filter 들을 and 로 결합` | `findWithFilters` | H2 |
| `findBy 는 findWithFilters 와 동일 동작` | `findBy` | H2 |
| `findByField 는 컬럼-값 매칭 row 모두 반환` | `findByField(field, value)` | H2 |
| `findByFieldOrNull 은 매칭 없으면 null` | `findByFieldOrNull` | H2 |
| `findAllByIds 는 inList 로 일괄 조회` | `findAllByIds(ids)` | H2, POSTGRESQL |
| `findPage 는 totalCount=0 시 빈 페이지 반환` | `findPage` | H2 |
| `findPage 는 마지막 페이지 경계 검증` | `findPage` | H2 |

### 4.3 신규 파일: `JdbcRepositoryWriteEdgeCaseTest.kt`

| 테스트 함수 | 대상 메서드 | 테스트 DB |
|--------------|--------------|-----------|
| `batchUpsert(Iterable) 는 신규 record INSERT` | `batchUpsert(Iterable, ...)` | H2, POSTGRESQL, MYSQL_V8 |
| `batchUpsert(Iterable) 는 기존 record UPDATE` | `batchUpsert(Iterable, ...)` | H2, POSTGRESQL, MYSQL_V8 |
| `batchUpsert(Sequence) 는 lazy 평가로 INSERT/UPDATE` | `batchUpsert(Sequence, ...)` | H2, POSTGRESQL, MYSQL_V8 |
| `batchUpsert 의 onUpdate 람다로 충돌 시 컬럼 값 제어` | `batchUpsert(onUpdate=...)` | H2, POSTGRESQL |
| `batchUpsert 의 onUpdateExclude 로 일부 컬럼 보존` | `batchUpsert(onUpdateExclude=...)` | H2, POSTGRESQL |
| `updateAll 은 조건에 맞는 row 만 업데이트` | `updateAll(predicate, limit, body)` | H2, POSTGRESQL |
| `updateAll 결과로 영향받은 row 수 반환` | `updateAll` | H2 |
| `deleteAll(op) 은 조건 매칭 row 만 삭제` | `deleteAll(limit, op)` | H2, POSTGRESQL |
| `deleteAllByIds 는 id 리스트 모두 삭제` | `deleteAllByIds(ids)` | H2, POSTGRESQL |
| `deleteAllIgnore 는 IGNORE 절로 실패 무시` | `deleteAllIgnore(limit, op)` | MYSQL_V8 (IGNORE 지원) |
| `deleteByIdIgnore 는 단건 IGNORE 삭제` | `deleteByIdIgnore(id)` | MYSQL_V8 |
| `batchInsert(Sequence) 는 lazy 평가로 처리` | `batchInsert(Sequence, ignore, shouldReturnGeneratedValues)` | H2, POSTGRESQL |

### 4.4 신규/보강 파일: `AuditableJdbcRepositoryEdgeCaseTest.kt`

| 테스트 함수 | 대상 메서드 | 테스트 DB |
|--------------|--------------|-----------|
| `auditedUpdateAll 은 predicate 매칭 row 의 updatedAt/updatedBy 자동 설정` | `auditedUpdateAll` | H2, POSTGRESQL |
| `auditedUpdateAll 은 영향 row 수 반환` | `auditedUpdateAll` | H2 |
| `UserContext 미설정 시 updatedBy 는 null/default 처리` | `auditedUpdateById` | H2 |
| `auditedUpdateById 의 limit 인자가 적용된다` | `auditedUpdateById(limit=)` | H2 |

### 4.5 신규/보강 파일: `SoftDeletedJdbcRepositoryEdgeCaseTest.kt`

| 테스트 함수 | 대상 메서드 | 테스트 DB |
|--------------|--------------|-----------|
| `countActive / countDeleted 는 predicate 결합 동작` | `countActive`, `countDeleted` | H2 |
| `softDeleteAll 은 영향 row 수 반환` | `softDeleteAll` | H2, POSTGRESQL |
| `restoreAll 은 영향 row 수 반환` | `restoreAll` | H2 |
| `findActivePage 는 soft-delete 필터 + 페이징 결합` | `findActivePage` | H2 |
| `findDeleted 는 sortOrder DESC + offset 동작` | `findDeleted` | H2 |

### 4.6 기존 파일 보강

- `MovieJdbcRepositoryTest.kt`: 기존 happy-path 옆에 **transaction rollback 시 모든 변경 무효** 테스트 추가
- `ActorJdbcRepositoryTest.kt`: `findPage` 의 페이지 경계 검증 1~2 케이스 추가

---

## 5. utils/batch 추가 테스트 목록

### 5.1 신규 파일: `core/BatchStepRunnerRetryTest.kt`

| 테스트 함수 | 대상 시나리오 |
|--------------|----------------|
| `retry policy 가 maxAttempts 도달까지 재시도한다` | retry 동작 |
| `retry policy 의 exponential backoff 가 적용된다` | 가상 시간 기반 |
| `retry 후 최종 실패 시 step status 는 FAILED` | 실패 종료 |
| `retry 성공 시 attempts 카운트가 step report 에 기록` | 메트릭 |

### 5.2 신규 파일: `core/BatchStepRunnerSkipTest.kt`

| 테스트 함수 | 대상 시나리오 |
|--------------|----------------|
| `LimitedSkipPolicy 는 임계치 도달 시 step fail` | skip 임계 |
| `skip 카운트가 step report 에 누적된다` | 메트릭 |
| `processor 예외 시 reader 는 영향받지 않는다` | error isolation |

### 5.3 신규 파일: `core/BatchStepRunnerTimeoutTest.kt`

| 테스트 함수 | 대상 시나리오 |
|--------------|----------------|
| `writer timeout 시 WriteTimeoutException throw` | timeout |
| `coroutine cancellation 시 commit 후 종료` | graceful shutdown |
| `cancel 후 step status 는 STOPPED` | 상태 전이 |

### 5.4 신규 파일: `core/BatchStepRunnerCheckpointTest.kt`

| 테스트 함수 | 대상 시나리오 |
|--------------|----------------|
| `checkpoint 에서 재시작 시 lastOffset 부터 처리` | resume |
| `checkpoint 가 chunk 단위로 갱신` | 저장 |
| `checkpoint 손상(JSON 파싱 실패) 시 처음부터 재시작` | corruption recovery |

### 5.5 신규 파일: `jdbc/tables/ResultRowMappersTest.kt`

| 테스트 함수 | 대상 시나리오 |
|--------------|----------------|
| `BatchJobExecution 매퍼는 nullable column 을 정상 처리` | nullable |
| `BatchStepExecution 매퍼는 모든 필드 매핑` | 전수 매핑 |
| `매퍼는 missing column 시 명확한 예외` | failure mode |

### 5.6 신규 파일: `api/SkipPolicyEdgeCaseTest.kt`

| 테스트 함수 | 대상 시나리오 |
|--------------|----------------|
| `LimitedSkipPolicy 는 0 limit 시 즉시 실패` | edge |
| `SkipPolicy.never 는 어떤 예외도 skip 하지 않음` | 기본 동작 |
| `SkipPolicy.always 는 모든 예외 skip` | 기본 동작 |

### 5.7 신규 파일: `internal/CheckpointJsonEdgeCaseTest.kt`

| 테스트 함수 | 대상 시나리오 |
|--------------|----------------|
| `malformed JSON 입력 시 명확한 예외` | parsing |
| `필수 필드 누락 시 IllegalArgumentException` | validation |
| `unknown field 는 무시하고 파싱 성공` | forward compat |

### 5.8 기존 파일 보강

> 참고: `BatchDsl`은 `@DslMarker` 어노테이션 클래스이며 (실파일: `BatchDsl.kt`),
> 실제 빌더는 `BatchJobBuilder` / `BatchStepBuilder` (각각 `BatchJobBuilder.kt`, `BatchStepBuilder.kt`).
> 기존 테스트 파일은 `BatchDslTest.kt`, `BatchBuilderTest.kt` 가 존재.

- `BatchBuilderTest.kt` (또는 `BatchJobBuilderTest.kt` 신설): step 2개 + retryPolicy/skipPolicy/commitTimeout 결합 테스트 1건 추가
- `BatchStepBuilderTest.kt` (신설): `processor(suspend (I) -> O?)` 람다 오버로드, `chunkSize(0)` 검증 실패 케이스
- `ExposedJdbcBatchReaderTest.kt`: empty result + partial last chunk 케이스 추가
- `ExposedJdbcBatchWriterTest.kt`: duplicate key conflict 케이스 추가 (ON CONFLICT)
- `ExposedR2dbcBatchReaderTest.kt`: 다음 명시적 테스트 추가
  - `읽을 데이터가 없으면 빈 Flow 반환`
  - `chunkSize 보다 적은 데이터에서 단일 partial chunk 발행`
  - `chunkSize 의 정확한 배수에서 마지막 chunk 가 fullsize`
- `ExposedR2dbcBatchWriterTest.kt`: 다음 명시적 테스트 추가
  - `중복 키 INSERT 시 ON CONFLICT 동작 검증`
  - `writer 가 trans 종료 후 connection 반환`
  - `빈 chunk 입력 시 즉시 0 반환`

---

## 6. 멀티-DB 테스트 전략 (재정리)

| 모듈 | 기본 DB | 추가 DB | 비고 |
|------|---------|---------|------|
| exposed-jdbc 단순 read 계열 | H2 | POSTGRESQL | DB 무관 로직 |
| exposed-jdbc 방언 의존 (`batchUpsert`/`deleteIgnoreWhere`/`batchInsert` returning) | H2 | POSTGRESQL, MYSQL_V8 | 전수 검증 |
| utils/batch jdbc 통합 | H2 | POSTGRESQL | `AbstractBatchJdbcTest` 활용 |
| utils/batch r2dbc 통합 | H2 | POSTGRESQL | `AbstractBatchR2dbcTest` 활용 |

`@ParameterizedTest` + `@EnumSource(TestDB::class, names=[...])` 패턴 통일.

---

## 7. 커버리지 달성 검증 방법

본 프로젝트는 이미 **Kover (`kotlinx-kover` 0.9.1) 가 루트 `build.gradle.kts` 에서 모든 서브모듈에 적용**되어 있다.
(`id(Plugins.kover) version Plugins.Versions.kover`, `subprojects { plugin(Plugins.kover) }`,
루트에서 `kover(project(sub.path))` 로 집계 등록.) 별도 플러그인 추가 작업은 불필요.

1. **Kover 라인 커버리지 측정 (정량 1차)**
   ```bash
   ./gradlew :bluetape4k-exposed-jdbc:koverHtmlReport
   ./gradlew :bluetape4k-batch:koverHtmlReport
   # 또는 집계: ./gradlew koverHtmlReport
   ```
   결과: `data/exposed-jdbc/build/reports/kover/html/index.html`,
   `utils/batch/build/reports/kover/html/index.html`.
   **목표: 두 모듈 모두 라인 커버리지 ≥ 70%.**

2. **Kover XML 리포트로 임계치 가드(선택)**
   ```bash
   ./gradlew :bluetape4k-exposed-jdbc:koverXmlReport
   ./gradlew :bluetape4k-batch:koverXmlReport
   ```
   필요 시 `koverVerify` task 의 `minBound = 70` 룰을 PR 게이트로 추가.

3. **메서드 매트릭스 기반 정성 검증**
   - `JdbcRepository.kt` 의 public 메서드 중 본 스펙 §2.1 표의 23개 미커버 항목 → 신규 테스트로 ✅ 처리.
   - `BatchStepRunner` / `BatchJobBuilder` / `BatchStepBuilder` 의 분기점(if/when, retry/skip/timeout) 별 테스트 존재 확인.

4. **체크리스트 기반 정량 검증**
   - 본 스펙의 4·5 절 테이블의 모든 항목 → ✅ 체크.
   - 각 신규 테스트 파일은 단독 실행으로 pass 확인.

5. **회귀 안전망 확인**
   - `./gradlew :bluetape4k-exposed-jdbc:test` 전수 pass.
   - `./gradlew :bluetape4k-batch:test` 전수 pass.
   - 두 모듈 합산 테스트 수 +50 이상 증가.

---

## 8. 완료 기준 (Definition of Done)

- [ ] 4 절 exposed-jdbc 테스트 표의 모든 함수 작성 + pass
- [ ] 5 절 utils/batch 테스트 표의 모든 함수 작성 + pass
- [ ] **Kover 라인 커버리지: `bluetape4k-exposed-jdbc` ≥ 70%, `bluetape4k-batch` ≥ 70%** (`koverHtmlReport` 캡처를 PR 에 첨부)
- [ ] 신규 테스트 파일 모두 200~400 라인, 800 라인 절대 초과 금지
- [ ] 기존 테스트 모두 회귀 없이 pass (`./gradlew :bluetape4k-exposed-jdbc:test` / `:bluetape4k-batch:test`)
- [ ] 모든 테스트는 `runTest(timeout = 30.seconds)` 또는 `@Test` 표준 사용
- [ ] **Kluent 비교 matcher 규칙 준수** — `shouldBeGreaterOrEqualTo`, `shouldBeGreaterThan`, `shouldBeLessThan`, `shouldBeLessOrEqualTo`, `shouldBeInRange`, `shouldBeEqualTo` 등을 사용한다. **`(x >= y).shouldBeTrue()` / `shouldBeTrue()` + 비교식 패턴은 금지** (실패 시 값 맥락이 사라짐). `feedback_kluent_comparison_matchers` 메모리 참조.
- [ ] **모든 신규 테스트 클래스는 `companion object: KLogging()` 패턴 채택** (`io.bluetape4k.logging.KLogging` 사용, 디버그 로그용 `log` 프로퍼티 노출). 기존 패턴과 일관성 유지.
- [ ] DB-방언 의존 테스트는 `@ParameterizedTest` + `@MethodSource` 또는 `@EnumSource(TestDB::class, names=["H2","POSTGRESQL","MYSQL_V8"])` 적용. **enum 이름은 `MYSQL_V8` (구 `MYSQL8` 표기 금지).**
- [ ] 신규 픽스처/도메인은 별도 `Schema` 파일로 분리 (테스트 격리)
- [ ] 모든 신규 테스트 함수에 한국어 KDoc 주석 (의도 + 대상 메서드)
- [ ] code-reviewer agent 통과 후 commit/push
- [ ] PR description 에 테스트 카운트 변화 (Before/After) + Kover 커버리지 % (Before/After) 기록

---

## 9. 작업 순서 (우선순위)

1. **High** — `JdbcRepositoryExistenceTest.kt`, `JdbcRepositoryWriteEdgeCaseTest.kt` (가장 큰 갭)
2. **High** — `BatchStepRunnerRetryTest.kt`, `BatchStepRunnerSkipTest.kt`, `BatchStepRunnerTimeoutTest.kt`
3. **High** — `ResultRowMappersTest.kt`, `SkipPolicyEdgeCaseTest.kt`
4. **Medium** — `JdbcRepositoryReadEdgeCaseTest.kt`, `BatchStepRunnerCheckpointTest.kt`
5. **Medium** — `AuditableJdbcRepositoryEdgeCaseTest.kt`, `SoftDeletedJdbcRepositoryEdgeCaseTest.kt`
6. **Medium** — `CheckpointJsonEdgeCaseTest.kt`, R2DBC reader/writer edge cases
7. **Low** — 기존 파일 보강(`BatchBuilderTest`/`BatchStepBuilderTest`, MovieJdbc, ActorJdbc, ExposedJdbcBatchReader/Writer)

---

## 10. 참고 자료

- 기존 표준 테스트: `data/exposed-jdbc/src/test/kotlin/io/bluetape4k/exposed/jdbc/repository/MovieJdbcRepositoryTest.kt`
- BatchStepRunner 본체: `utils/batch/src/main/kotlin/io/bluetape4k/batch/core/BatchStepRunner.kt`
- TestDB enum: bluetape4k-testing-junit5 / bluetape4k-exposed-cache testFixtures
- 사용자 메모리: `feedback_kluent_comparison_matchers`, `feedback_write_and_verify_tests`, `feedback_no_environment_blame`
