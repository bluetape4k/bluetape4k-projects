# Implementation Plan — Test Coverage Improvement (exposed-jdbc & utils/batch)

- **작성일**: 2026-04-26
- **대상 모듈**: `data/exposed-jdbc`, `utils/batch`
- **Spec**: [`2026-04-26-test-coverage-exposed-jdbc-batch-design.md`](../specs/2026-04-26-test-coverage-exposed-jdbc-batch-design.md)
- **목표 커버리지**: Kover 라인 기준 ≥ 70% (모듈별)
- **워크트리**: `.worktrees/test-coverage-batch`

---

## 0. 공통 사항 (모든 태스크에 적용)

- 모든 신규 테스트 클래스는 `companion object : KLogging()` 패턴 채택 (`io.bluetape4k.logging.KLogging`).
- Kluent 비교는 반드시 `shouldBeGreaterOrEqualTo`/`shouldBeGreaterThan`/`shouldBeLessThan`/`shouldBeLessOrEqualTo`/`shouldBeInRange`/`shouldBeEqualTo` 사용. `(x >= y).shouldBeTrue()` 패턴 금지.
- DB-방언 의존 테스트는 `@ParameterizedTest` + `@EnumSource(TestDB::class, names=["H2","POSTGRESQL","MYSQL_V8"])`. enum 명은 `MYSQL_V8` (구 `MYSQL8` 금지).
- 코루틴 테스트는 `runTest(timeout = 30.seconds)` + 가상 시간(`TestCoroutineScheduler`) 우선.
- 신규 테스트 파일: 200~400 라인 목표, 800 라인 절대 초과 금지.
- 모든 신규 테스트 함수에 한국어 KDoc (의도 + 대상 메서드).
- 신규 픽스처는 별도 `Schema` 객체로 분리 (`JdbcRepositoryEdgeCaseSchema` 등).

---

## Task Index

| # | Task ID | 영역 | 파일 | complexity | 예상 함수 수 |
|---|---------|------|------|-----------:|-------------:|
| 1 | T-EJ-01 | exposed-jdbc | `repository/JdbcRepositoryExistenceTest.kt` | medium | 11 |
| 2 | T-EJ-02 | exposed-jdbc | `repository/JdbcRepositoryWriteEdgeCaseTest.kt` | medium | 12 |
| 3 | T-EJ-03 | exposed-jdbc | `repository/JdbcRepositoryReadEdgeCaseTest.kt` | medium | 12 |
| 4 | T-EJ-04 | exposed-jdbc | `repository/AuditableJdbcRepositoryEdgeCaseTest.kt` | medium | 4 |
| 5 | T-EJ-05 | exposed-jdbc | `repository/SoftDeletedJdbcRepositoryEdgeCaseTest.kt` | medium | 5 |
| 6 | T-BA-01 | utils/batch | `core/BatchStepRunnerRetryTest.kt` | high | 4 |
| 7 | T-BA-02 | utils/batch | `core/BatchStepRunnerSkipTest.kt` | high | 3 |
| 8 | T-BA-03 | utils/batch | `core/BatchStepRunnerTimeoutTest.kt` | high | 4 |
| 9 | T-BA-04 | utils/batch | `core/BatchStepRunnerCheckpointTest.kt` | high | 3 |
| 10 | T-BA-05 | utils/batch | `jdbc/tables/ResultRowMappersTest.kt` | medium | 3 |
| 11 | T-BA-06 | utils/batch | `api/SkipPolicyEdgeCaseTest.kt` | low | 3 |
| 12 | T-BA-07 | utils/batch | `internal/CheckpointJsonEdgeCaseTest.kt` | medium | 3 |
| 13 | T-BA-08 | utils/batch | `jdbc/ExposedJdbcBatchWriterTest.kt` (보강) + `r2dbc/ExposedR2dbcBatchWriterTest.kt` (보강) | medium | 5 |
| 14 | T-BA-09 | utils/batch | `jdbc/ExposedJdbcBatchReaderTest.kt` (보강) + `r2dbc/ExposedR2dbcBatchReaderTest.kt` (보강) | medium | 5 |
| 15 | T-FINAL | verify | Kover 70% 검증 | medium | — |
| 16 | T-DOC | docs | README × 2 모듈 | low | — |
| 17 | T-TESTLOG | docs | `docs/testlogs/2026-04.md` | low | — |
| 18 | T-SUPERPOWERS | docs | `docs/superpowers/index/2026-04.md` | low | — |

**합계 신규 테스트 함수: 77개 (보강 10개 포함). 기존 테스트 +67~+80건 증가 예상.**

---

## T-EJ-01: JdbcRepositoryExistenceTest (complexity: medium)

- **파일**: `data/exposed-jdbc/src/test/kotlin/io/bluetape4k/exposed/jdbc/repository/JdbcRepositoryExistenceTest.kt`
- **대상 소스**: `data/exposed-jdbc/src/main/kotlin/io/bluetape4k/exposed/jdbc/repository/JdbcRepository.kt`
- **테스트 DB**: H2 (default), POSTGRESQL, MYSQL_V8 (지정 함수만)
- **테스트 함수 (11개)**:
  1. `isEmpty 는 빈 테이블에서 true 를 반환한다` — H2, POSTGRESQL
  2. `isEmpty 는 데이터 존재 시 false 를 반환한다` — H2, POSTGRESQL
  3. `isNotEmpty 는 isEmpty 의 부정값을 반환한다` — H2
  4. `existsBy 는 조건에 맞는 row 가 있으면 true` — H2, POSTGRESQL, MYSQL_V8
  5. `existsBy 는 조건에 맞지 않으면 false` — H2
  6. `existsById 는 존재하는 id 에 true` — H2
  7. `existsById 는 없는 id 에 false` — H2
  8. `exists(query) 는 서브쿼리 결과 존재 여부 반환` — H2
  9. `countBy(op) 는 Op 조건에 맞는 row 수를 반환` — H2, POSTGRESQL, MYSQL_V8
  10. `countBy(predicate) 람다 오버로드 동작` — H2
  11. `countBy 결과 0 일 때도 정상 동작` — H2
- **준비물**: `JdbcRepositoryEdgeCaseSchema` 신규 객체 (`testFixtures` 또는 별도 .kt) — `EdgeCaseTable` (id Long, name String, age Int, isActive Boolean) + `EdgeCaseRepository: AbstractJdbcRepository<Long, EdgeCaseEntity>`.

---

## T-EJ-02: JdbcRepositoryWriteEdgeCaseTest (complexity: medium)

- **파일**: `data/exposed-jdbc/src/test/kotlin/io/bluetape4k/exposed/jdbc/repository/JdbcRepositoryWriteEdgeCaseTest.kt`
- **대상 소스**: `JdbcRepository.kt` write 계열 (`upsert`, `batchUpsert`, `updateAll`, `deleteAll*`, `batchInsert`)
- **테스트 DB**: 함수별로 명시
- **테스트 함수 (12개)**:
  1. `batchUpsert(Iterable) 는 신규 record INSERT` — H2, POSTGRESQL, MYSQL_V8
  2. `batchUpsert(Iterable) 는 기존 record UPDATE` — H2, POSTGRESQL, MYSQL_V8
  3. `batchUpsert(Sequence) 는 lazy 평가로 INSERT/UPDATE` — H2, POSTGRESQL, MYSQL_V8
  4. `batchUpsert 의 onUpdate 람다로 충돌 시 컬럼 값 제어` — H2, POSTGRESQL
  5. `batchUpsert 의 onUpdateExclude 로 일부 컬럼 보존` — H2, POSTGRESQL
  6. `updateAll 은 조건에 맞는 row 만 업데이트` — H2, POSTGRESQL
  7. `updateAll 결과로 영향받은 row 수 반환` — H2
  8. `deleteAll(op) 은 조건 매칭 row 만 삭제` — H2, POSTGRESQL
  9. `deleteAllByIds 는 id 리스트 모두 삭제` — H2, POSTGRESQL
  10. `deleteAllIgnore 는 IGNORE 절로 실패 무시` — MYSQL_V8 only
  11. `deleteByIdIgnore 는 단건 IGNORE 삭제` — MYSQL_V8 only
  12. `batchInsert(Sequence) 는 lazy 평가로 처리` — H2, POSTGRESQL
- **참고**: `EdgeCaseTable` 에 unique constraint (예: `name`) 추가하여 upsert 충돌 시나리오 구현.

---

## T-EJ-03: JdbcRepositoryReadEdgeCaseTest (complexity: medium)

- **파일**: `data/exposed-jdbc/src/test/kotlin/io/bluetape4k/exposed/jdbc/repository/JdbcRepositoryReadEdgeCaseTest.kt`
- **대상 소스**: `JdbcRepository.kt` read 계열 (`findByIdOrNull`, `findFirstOrNull`, `findLastOrNull`, `findAll`, `findWithFilters`, `findBy`, `findByField*`, `findAllByIds`, `findPage`)
- **테스트 DB**: H2 위주, 일부 POSTGRESQL 추가
- **테스트 함수 (12개)**:
  1. `findByIdOrNull 은 존재하지 않는 id 에 null 반환` — H2
  2. `findByIdOrNull 은 존재하는 id 에 엔티티 반환` — H2, POSTGRESQL
  3. `findFirstOrNull 은 predicate + offset 조합 동작` — H2
  4. `findLastOrNull 은 PK 역순 첫 매칭 row 반환` — H2
  5. `findAll 은 limit + offset + sortOrder + predicate 조합 동작` — H2, POSTGRESQL
  6. `findWithFilters 는 vararg filter 들을 and 로 결합` — H2
  7. `findBy 는 findWithFilters 와 동일 동작` — H2
  8. `findByField 는 컬럼-값 매칭 row 모두 반환` — H2
  9. `findByFieldOrNull 은 매칭 없으면 null` — H2
  10. `findAllByIds 는 inList 로 일괄 조회` — H2, POSTGRESQL
  11. `findPage 는 totalCount=0 시 빈 페이지 반환` — H2
  12. `findPage 는 마지막 페이지 경계 검증` — H2

---

## T-EJ-04: AuditableJdbcRepositoryEdgeCaseTest (complexity: medium)

- **파일**: `data/exposed-jdbc/src/test/kotlin/io/bluetape4k/exposed/jdbc/repository/AuditableJdbcRepositoryEdgeCaseTest.kt`
- **대상 소스**: `AuditableJdbcRepository.kt` (`auditedUpdateById`, `auditedUpdateAll`)
- **테스트 DB**: H2, POSTGRESQL
- **테스트 함수 (4개)**:
  1. `auditedUpdateAll 은 predicate 매칭 row 의 updatedAt/updatedBy 자동 설정` — H2, POSTGRESQL
  2. `auditedUpdateAll 은 영향 row 수 반환` — H2
  3. `UserContext 미설정 시 updatedBy 는 null/default 처리` — H2
  4. `auditedUpdateById 의 limit 인자가 적용된다` — H2
- **준비물**: `AuditableEdgeCaseSchema` (AuditableLongIdTable 기반) + `UserContext.withUser(...)` 헬퍼 사용.

---

## T-EJ-05: SoftDeletedJdbcRepositoryEdgeCaseTest (complexity: medium)

- **파일**: `data/exposed-jdbc/src/test/kotlin/io/bluetape4k/exposed/jdbc/repository/SoftDeletedJdbcRepositoryEdgeCaseTest.kt`
- **대상 소스**: `SoftDeletedJdbcRepository.kt` (`countActive`, `countDeleted`, `softDeleteAll`, `restoreAll`, `findActivePage`, `findDeleted`)
- **테스트 DB**: H2, POSTGRESQL (일부)
- **테스트 함수 (5개)**:
  1. `countActive / countDeleted 는 predicate 결합 동작` — H2
  2. `softDeleteAll 은 영향 row 수 반환` — H2, POSTGRESQL
  3. `restoreAll 은 영향 row 수 반환` — H2
  4. `findActivePage 는 soft-delete 필터 + 페이징 결합` — H2
  5. `findDeleted 는 sortOrder DESC + offset 동작` — H2
- **준비물**: `SoftDeletedEdgeCaseSchema` (isDeleted 컬럼 포함 테이블).

---

## T-BA-01: BatchStepRunnerRetryTest (complexity: high)

- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/core/BatchStepRunnerRetryTest.kt`
- **대상 소스**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/core/BatchStepRunner.kt` (retry 분기 ~line 148)
- **전략**: `runTest(timeout = 30.seconds)` + 가상 시간; fake `ItemWriter` 호출 횟수 카운터 사용. exponential backoff 검증은 `testScheduler.currentTime` 측정.
- **테스트 함수 (4개)**:
  1. `retry policy 가 maxAttempts 도달까지 재시도한다` — fake writer 호출 횟수 검증
  2. `retry policy 의 exponential backoff 가 적용된다` — `testScheduler.currentTime` delta 검증 (`shouldBeGreaterOrEqualTo`)
  3. `retry 후 최종 실패 시 step status 는 FAILED` — `StepReport.status` 비교
  4. `retry 성공 (N번째 시도) 시 writeCount 는 정상` — `StepReport.writeCount`

---

## T-BA-02: BatchStepRunnerSkipTest (complexity: high)

- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/core/BatchStepRunnerSkipTest.kt`
- **대상 소스**: `BatchStepRunner.kt` skip 분기, `SkipPolicy.maxSkips(n)`
- **테스트 함수 (3개)**:
  1. `maxSkips(n) 초과 시 step status 는 FAILED` — 임계 초과 시점 종료
  2. `skip 카운트가 step report 에 누적된다` — `StepReport.skipCount`
  3. `processor 예외 시 reader 는 영향받지 않음` — error isolation (reader 호출 횟수)

---

## T-BA-03: BatchStepRunnerTimeoutTest (complexity: high)

- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/core/BatchStepRunnerTimeoutTest.kt`
- **대상 소스**: `BatchStepRunner.kt` timeout/cancellation 경로 (line 148 주변)
- **전략**: `withTimeout`/`delay` 가상 시간 + suspending fake writer로 timeout 발생.
- **테스트 함수 (4개)**:
  1. `writer timeout, retry=0, skipPolicy=NONE → step status FAILED`
  2. `writer timeout, skipPolicy=maxSkips(>=chunkSize) → COMPLETED_WITH_SKIPS`
  3. `coroutine cancellation 시 commit 후 종료` — graceful shutdown
  4. `cancel 후 step status 는 STOPPED` — 상태 전이

---

## T-BA-04: BatchStepRunnerCheckpointTest (complexity: high)

- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/core/BatchStepRunnerCheckpointTest.kt`
- **대상 소스**: `BatchStepRunner.kt` checkpoint 갱신 / 복원
- **테스트 함수 (3개)**:
  1. `checkpoint 에서 재시작 시 lastOffset 부터 처리` — resume
  2. `checkpoint 가 chunk 단위로 갱신` — saveCheckpoint 호출 횟수
  3. `saveCheckpoint 실패 시 retry → skipPolicy → 소진 시 FAILED` — catch(Throwable) 경로

---

## T-BA-05: ResultRowMappersTest (complexity: medium)

- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/jdbc/tables/ResultRowMappersTest.kt`
- **대상 소스**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/jdbc/tables/ResultRowMappers.kt` (`BatchJobExecution`, `BatchStepExecution` 매퍼)
- **전략**: 실제 H2 `Database.connect` + 임시 테이블 + `selectAll().single()` 통합 테스트로 mocking 최소화.
- **테스트 함수 (3개)**:
  1. `BatchJobExecution 매퍼는 nullable column 을 정상 처리`
  2. `BatchStepExecution 매퍼는 모든 필드 매핑`
  3. `매퍼는 missing column 시 명확한 예외`

---

## T-BA-06: SkipPolicyEdgeCaseTest (complexity: low)

- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/api/SkipPolicyEdgeCaseTest.kt`
- **대상 소스**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/api/SkipPolicy.kt`
- **테스트 함수 (3개)**:
  1. `maxSkips(0) 는 어떤 예외도 skip 하지 않음`
  2. `NONE 은 shouldSkip 항상 false`
  3. `ALL 은 shouldSkip 항상 true`
- **참고**: 기존 `SkipPolicyTest.kt` 와 중복 시 제외 (작업 시 Read 로 확인 필수).

---

## T-BA-07: CheckpointJsonEdgeCaseTest (complexity: medium)

- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/internal/CheckpointJsonEdgeCaseTest.kt`
- **대상 소스**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/internal/CheckpointJson.kt`
- **테스트 함수 (3개)**:
  1. `malformed JSON 입력 시 명확한 예외`
  2. `필수 필드 누락 시 IllegalArgumentException`
  3. `unknown field 는 무시하고 파싱 성공` — forward compat

---

## T-BA-08: BatchWriter 보강 (complexity: medium)

- **파일들**:
  - `utils/batch/src/test/kotlin/io/bluetape4k/batch/jdbc/ExposedJdbcBatchWriterTest.kt`
  - `utils/batch/src/test/kotlin/io/bluetape4k/batch/r2dbc/ExposedR2dbcBatchWriterTest.kt`
- **대상 소스**: `ExposedJdbcBatchWriter.kt`, `ExposedR2dbcBatchWriter.kt`
- **테스트 DB**: H2, POSTGRESQL
- **추가 테스트 함수 (5개)**:
  1. (jdbc) `중복 키 INSERT 시 ignore=false 면 예외 발생`
  2. (jdbc) `중복 키 INSERT 시 ignore=true 면 무시`
  3. (r2dbc) `중복 키 INSERT 시 예외 전파`
  4. (r2dbc) `writer 가 트랜잭션 종료 후 connection 반환`
  5. (r2dbc) `빈 chunk 입력 시 즉시 0 반환`

---

## T-BA-09: BatchReader 보강 (complexity: medium)

- **파일들**:
  - `utils/batch/src/test/kotlin/io/bluetape4k/batch/jdbc/ExposedJdbcBatchReaderTest.kt`
  - `utils/batch/src/test/kotlin/io/bluetape4k/batch/r2dbc/ExposedR2dbcBatchReaderTest.kt`
- **대상 소스**: `ExposedJdbcBatchReader.kt`, `ExposedR2dbcBatchReader.kt`
- **테스트 DB**: H2, POSTGRESQL
- **추가 테스트 함수 (5개)**:
  1. (jdbc) `읽을 데이터가 없으면 빈 결과`
  2. (jdbc) `chunkSize 보다 적은 데이터 → 단일 partial chunk`
  3. (r2dbc) `읽을 데이터가 없으면 빈 Flow 반환`
  4. (r2dbc) `chunkSize 보다 적은 데이터 → 단일 partial chunk`
  5. (r2dbc) `chunkSize 정확한 배수 → 마지막 chunk fullsize`

---

## T-FINAL: Kover 커버리지 측정 + 검증 (complexity: medium)

- **명령**:
  ```bash
  ./gradlew :bluetape4k-exposed-jdbc:koverHtmlReport :bluetape4k-batch:koverHtmlReport
  ./gradlew :bluetape4k-exposed-jdbc:koverXmlReport  :bluetape4k-batch:koverXmlReport
  ```
- **검증**:
  - HTML 리포트 (`build/reports/kover/html/index.html`) 라인 커버리지 ≥ 70% 확인.
  - XML 리포트의 `<counter type="LINE" .../>` 비율 계산 후 PR description 첨부.
  - Before/After Δ 기록.
- **실패 시**: 미달 모듈에 대해 §2.1 / §2.2 의 미커버 항목 중 추가 가능 영역 재검토 → 부족분 테스트 추가 → 재측정.

---

## T-DOC: README 업데이트 (complexity: low)

- **파일들**:
  - `data/exposed-jdbc/README.md`
  - `data/exposed-jdbc/README.ko.md`
  - `utils/batch/README.md`
  - `utils/batch/README.ko.md`
- **변경 내용**: "Test Coverage" 섹션 추가 (Kover 70%+ 달성, 신규 테스트 카테고리 요약 — Edge Case / Retry / Skip / Timeout / Checkpoint / Mappers / R2DBC).
- **이중언어 동기화 필수.**

---

## T-TESTLOG: testlog 기록 (complexity: low)

- **파일**: `docs/testlogs/2026-04.md`
- **변경**: 표 맨 위에 새 행 추가.
  - 컬럼: 일자(2026-04-26), 모듈(`bluetape4k-exposed-jdbc`, `bluetape4k-batch`), 명령, 결과(passing/skip/fail count + duration), 비고(coverage 70%+).

---

## T-SUPERPOWERS: superpowers 인덱스 (complexity: low)

- **파일**: `docs/superpowers/index/2026-04.md`
- **변경**: 맨 위 행 추가 — `2026-04-26 / Test Coverage exposed-jdbc + utils/batch / spec + plan + 신규 테스트 ~77 + Kover 70%+`.
- **허브 갱신**: `docs/superpowers/INDEX.md` 카운트 +1.

---

## 작업 순서 (실행 권장 순)

1. 픽스처 신설 (`JdbcRepositoryEdgeCaseSchema`, `AuditableEdgeCaseSchema`, `SoftDeletedEdgeCaseSchema`)
2. T-EJ-01, T-EJ-02 (가장 큰 갭 / High 우선)
3. T-BA-01, T-BA-02, T-BA-03 (코어 로직)
4. T-BA-05, T-BA-06 (mappers + skip policy)
5. T-EJ-03 (read edge cases) / T-BA-04 (checkpoint)
6. T-EJ-04, T-EJ-05 (auditable / soft-deleted)
7. T-BA-07 (CheckpointJson) / T-BA-08, T-BA-09 (보강)
8. T-FINAL (Kover 측정) — 미달 시 부족분 추가 후 재측정
9. T-DOC, T-TESTLOG, T-SUPERPOWERS

---

## 검증 게이트

- [ ] 신규 테스트 함수 ≥ 77개 (보강 10개 포함)
- [ ] `./gradlew :bluetape4k-exposed-jdbc:test` 전수 pass
- [ ] `./gradlew :bluetape4k-batch:test` 전수 pass
- [ ] Kover HTML/XML 리포트 라인 커버리지 ≥ 70% (두 모듈)
- [ ] 신규 파일 모두 200~400 라인 (800 라인 절대 초과 금지)
- [ ] code-reviewer agent 통과 (HIGH/CRITICAL 0건)
- [ ] PR description 에 Before/After 테스트 카운트 + Kover % 첨부
