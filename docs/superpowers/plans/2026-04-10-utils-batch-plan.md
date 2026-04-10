# bluetape4k-batch 구현 플랜

> **버전**: v3 (P1-A/P1-B 반영)
> **작성일**: 2026-04-10
> **스펙**: `docs/superpowers/specs/2026-04-10-utils-batch-design.md`
> **모듈**: `utils/batch` (→ `bluetape4k-batch`)
> **브랜치 권장**: `feat/utils-batch`
> **목적**: Kotlin Coroutine 네이티브 경량 배치 프레임워크. Reader → (Processor) → Writer chunk 파이프라인, `BatchJobRepository` 기반 재시작, SkipPolicy/RetryPolicy, workflow `SuspendWork` 통합, Exposed JDBC/R2DBC 이중 백엔드.

---

## Phase 구성 요약

| Phase | 주제 | 병렬성 |
|-------|------|--------|
| Phase 1 | 모듈 골격 & 빌드 설정 | 순차 |
| Phase 2 | `api/` — 인터페이스·값 객체·예외 | T03~T12 모두 병렬 |
| Phase 3 | `internal/` + `core/` 코어 엔진 + DSL | T13→T14→T15→T16→T17→T18→T19 순차 |
| Phase 4 | JDBC 백엔드 (`jdbc/`) | T20→T20b→(T21, T22, T23 병렬) |
| Phase 5 | R2DBC 백엔드 (`r2dbc/`) | T24, T25, T26 병렬 가능 |
| Phase 6 | 단위 테스트 (`api/`, `core/`) | T27~T31 병렬 가능 |
| Phase 7 | JDBC/R2DBC 통합 테스트 + Workflow 통합 | T32~T36 병렬 가능 |
| Phase 8 | 문서화 & 프로젝트 레퍼런스 업데이트 | T37~T40 병렬 가능 |
| Phase 9 | 최종 검증 | 순차 |

---

## Phase 1 — 모듈 골격 & 빌드 설정

### T01. `utils/batch/build.gradle.kts` 작성
- **complexity**: low
- **파일**: `utils/batch/build.gradle.kts`
- **내용**:
  - 최상위 블록: `configurations { testImplementation.get().extendsFrom(compileOnly.get()) }`
  - `api(project(":bluetape4k-core"))`, `api(project(":bluetape4k-coroutines"))`, `api(project(":bluetape4k-logging"))`
  - **hard** `api(project(":bluetape4k-workflow"))` (BatchJob = SuspendWork, RetryPolicy 재사용)
  - `implementation(project(":bluetape4k-virtualthread-api"))` + `runtimeOnly(project(":bluetape4k-virtualthread-jdk21"))`
  - `compileOnly(project(":bluetape4k-exposed-jdbc"))`, `compileOnly(project(":bluetape4k-exposed-r2dbc"))`, `compileOnly(Libs.exposed_java_time)` (중복 `Libs.exposed_core/jdbc/r2dbc` 선언 금지)
  - `compileOnly(project(":bluetape4k-jackson3"))` — 체크포인트 JSON 직렬화 선택 의존
  - `implementation(Libs.kotlinx_coroutines_core)`
  - `testImplementation(project(":bluetape4k-junit5"))`, `testImplementation(project(":bluetape4k-jackson3"))`, `testImplementation(Libs.kotlinx_coroutines_test)`
  - Test DB: `testImplementation(Libs.h2_v2)`, `testImplementation(Libs.hikaricp)`, `testImplementation(Libs.r2dbc_h2)`, `testImplementation(Libs.r2dbc_pool)`
- **검증**: `./gradlew :bluetape4k-batch:help` → 모듈 인식 확인, `./gradlew :bluetape4k-batch:dependencies` → workflow/exposed 의존성 확인

### T02. `BatchDefaults.kt` — 공용 디폴트 상수
- **complexity**: low
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/BatchDefaults.kt`
- **내용**:
  - `object BatchDefaults { const val CHUNK_SIZE = 100; const val READER_PAGE_SIZE = 1_000; val COMMIT_TIMEOUT = 30.seconds }`
  - KDoc (한국어) — 각 디폴트 값의 의미
- **검증**: `./gradlew :bluetape4k-batch:compileKotlin` 성공

---

## Phase 2 — `api/` 인터페이스 & 값 객체

> **병렬 수행 가능** — T03~T12 는 서로 의존하지 않으므로 병렬 실행 가능. 단 T09(BatchReport) 은 T07(JobExecution/StepExecution)과 T08(StepReport)에 의존한다.

### T03. `BatchReader<T>` 인터페이스
- **complexity**: medium
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/api/BatchReader.kt`
- **내용**: `suspend fun open()`, `suspend fun read(): T?`, `suspend fun checkpoint(): Any? = null`, `suspend fun restoreFrom(checkpoint: Any) {}`, `suspend fun onChunkCommitted() {}`, `suspend fun close() {}`
  - KDoc (한국어): 체크포인트 시맨틱 — "**마지막으로 writer.write() 가 성공한 키**" 반환 규칙, open() 이후 restoreFrom() 호출 순서 강조
  - runner 가 아이템을 추적하지 않고 reader 가 위치를 추적한다는 점 명시
- **검증**: 컴파일 성공

### T04. `BatchProcessor<I, O>` fun interface
- **complexity**: low
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/api/BatchProcessor.kt`
- **내용**: `fun interface BatchProcessor<in I : Any, out O : Any> { suspend fun process(item: I): O? }`
  - KDoc: null 반환 = filter (skipCount 증가 없음) / 예외 = SkipPolicy 평가 → skipCount 증가 구분
- **검증**: 컴파일 성공

### T05. `BatchWriter<T>` 인터페이스
- **complexity**: low
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/api/BatchWriter.kt`
- **내용**: `suspend fun open()`, `suspend fun write(items: List<T>)`, `suspend fun close() {}`
  - KDoc: chunk 단위 트랜잭션 경계 / 실패 시 전체 롤백 명시
- **검증**: 컴파일 성공

### T06. `BatchStatus` enum + 상태 전이 규칙
- **complexity**: low
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/api/BatchStatus.kt`
- **내용**: `STARTING, RUNNING, COMPLETED, FAILED, COMPLETED_WITH_SKIPS, STOPPED` + `val isTerminal: Boolean`
  - KDoc (한국어): 각 상태 의미, 재시작 대상 (RUNNING/FAILED/STOPPED) vs 재시작 skip 대상 (COMPLETED/COMPLETED_WITH_SKIPS) 명시
- **검증**: 컴파일 성공

### T07. `JobExecution` + `StepExecution` data class
- **complexity**: low
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/api/JobExecution.kt`, `utils/batch/src/main/kotlin/io/bluetape4k/batch/api/StepExecution.kt`
- **내용**:
  - `JobExecution(id, jobName, params, status, startTime, endTime): Serializable` + `companion object: KLogging() { serialVersionUID = 1L }`
  - `StepExecution(id, jobExecutionId, stepName, status, readCount, writeCount, skipCount, checkpoint, startTime, endTime): Serializable` + 동일 패턴
  - 한국어 KDoc — 분산 캐시 재구성을 위한 Serializable 목적 명시
- **검증**: 컴파일 성공; `Serializable` + `serialVersionUID` 존재 확인

### T08. `StepReport` data class
- **complexity**: low
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/api/StepReport.kt`
- **내용**: `data class StepReport(stepName, status, readCount, writeCount, skipCount, error, checkpoint): Serializable` + `companion object: KLogging() { serialVersionUID = 1L }`
- **검증**: 컴파일 성공

### T09. `BatchReport` sealed interface (Success/PartiallyCompleted/Failure/Stopped)
- **complexity**: medium
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/api/BatchReport.kt`
- **의존**: T07, T08
- **내용**:
  - `sealed interface BatchReport { val jobExecution; val stepReports; val status }`
  - `Success`, `PartiallyCompleted(failedSteps helper)`, `Failure(error, failedSteps helper)`, `Stopped(reason)` — 각 `status` override
  - 한국어 KDoc — `Stopped` 는 v2 명시적 stop() API 용, v1 런타임에서는 생성되지 않음 명시
- **검증**: 컴파일 성공; sealed 타입 4종 모두 존재

### T10. `SkipPolicy` data class
- **complexity**: medium
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/api/SkipPolicy.kt`
- **내용**:
  - `data class SkipPolicy(maxSkips=0, skipOn=emptySet(), isolateOnSkip=false): Serializable`
  - `companion object: KLogging() { serialVersionUID = 1L; val NONE; val ALL; fun skipAll(maxSkips: Int) { maxSkips.requirePositiveNumber("maxSkips"); ... } }`
  - `fun matchesException(e: Throwable): Boolean` — skipOn 비어있으면 true, 아니면 `isAssignableFrom` 검사
  - KDoc: 아이템 단위 (processor) vs 청크 단위 (writer retry 소진 후) 적용 구분 + isolateOnSkip = v2 예정 명시
- **검증**: 컴파일 성공; `SkipPolicy.NONE`, `ALL`, `skipAll(3)` 모두 사용 가능

### T11. `BatchStepFailedException`
- **complexity**: low
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/api/BatchStepFailedException.kt`
- **내용**: `class BatchStepFailedException(val stepReport: StepReport, cause: Throwable? = stepReport.error): RuntimeException("Step '${stepReport.stepName}' failed: status=${stepReport.status}", cause) { companion object: KLogging() }`
- **검증**: 컴파일 성공

### T12. `BatchJobRepository` 인터페이스
- **complexity**: medium
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/api/BatchJobRepository.kt`
- **의존**: T06, T07, T08
- **내용**:
  - `suspend findOrCreateJobExecution(jobName, params = emptyMap()): JobExecution`
  - `suspend completeJobExecution(execution, status)`
  - `suspend findOrCreateStepExecution(jobExecution, stepName): StepExecution`
  - `suspend completeStepExecution(execution, report)`
  - `suspend saveCheckpoint(stepExecutionId, checkpoint)` / `suspend loadCheckpoint(stepExecutionId): Any?`
  - KDoc: 구현체 3종 (InMemory/ExposedJdbc/ExposedR2dbc) 참조
- **검증**: 컴파일 성공

---

## Phase 3 — `core/` 실행 엔진 + DSL

### T13. `internal/CheckpointJson.kt` + `internal/Jackson3CheckpointJson.kt` — 체크포인트 JSON 직렬화
- **complexity**: medium
- **파일**:
  - `utils/batch/src/main/kotlin/io/bluetape4k/batch/internal/CheckpointJson.kt`
  - `utils/batch/src/main/kotlin/io/bluetape4k/batch/internal/Jackson3CheckpointJson.kt` (같은 파일도 가능)
- **내용**:
  - `internal data class TypedCheckpoint(val className: String, val payload: String)` — 타입 봉투 (Jackson 3 Default Typing 제거 대응)
  - `interface CheckpointJson { fun write(obj: Any): String; fun read(json: String): Any }`
  - **`CheckpointJson.Default` 제공하지 않음** (P1-B: toString() fallback 은 round-trip 불가 → silent 재시작 실패)
  - `companion object { fun jackson3(): CheckpointJson }`:
    - `Class.forName("tools.jackson.databind.json.JsonMapper")` — **Jackson 3 는 `tools.jackson.*` 패키지** (`com.fasterxml.jackson.*` 아님)
    - 없으면 `IllegalStateException` 즉시 throw ("bluetape4k-jackson3 (tools.jackson) on classpath" 메시지)
  - `internal class Jackson3CheckpointJson : CheckpointJson`:
    - `private val mapper = io.bluetape4k.jackson3.Jackson.defaultJsonMapper` — `bluetape4k-jackson3` 의 공유 매퍼 재사용
    - `write(obj)`: `TypedCheckpoint(obj.javaClass.name, mapper.writeValueAsString(obj))` → 봉투 JSON
    - `read(json)`: 봉투 파싱 → `Class.forName(className)` → `mapper.readValue(payload, clazz)`
    - **round-trip 보장**: `Long(42)` → `{"className":"java.lang.Long","payload":"42"}` → `Long(42)`
  - 한국어 KDoc — round-trip 규칙, tools.jackson 패키지 주의, InMemoryBatchJobRepository 불필요 이유
- **검증**: `./gradlew :bluetape4k-batch:compileKotlin` 성공; `CheckpointJson.jackson3()` 미존재 시 `IllegalStateException` 발생 단위 테스트 (T31 에 포함)

### T14. `core/InMemoryBatchJobRepository`
- **complexity**: medium
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/core/InMemoryBatchJobRepository.kt`
- **의존**: T12
- **내용**:
  - `ConcurrentHashMap<Long, JobExecution>`, `ConcurrentHashMap<Long, StepExecution>`, `ConcurrentHashMap<Long, Any>` (checkpoints), `AtomicLong idCounter`
  - `jobName.requireNotBlank("jobName")`, `stepName.requireNotBlank("stepName")`
  - `findOrCreateJobExecution`: `jobName` + `params` 일치 + status ∈ {RUNNING, FAILED, STOPPED} → copy(status=RUNNING) 저장 후 반환; 없으면 새 생성
  - `findOrCreateStepExecution`:
    - 기존 row 조회
    - `COMPLETED` / `COMPLETED_WITH_SKIPS` → **변경 없이 그대로 반환** (runner 가 즉시 skip 처리 — P1-A)
    - `FAILED` / `STOPPED` / `RUNNING` → `copy(status = RUNNING).also { stepExecutions[it.id] = it }` 반환
    - 없으면 새 row 생성 (status = RUNNING)
  - `completeJobExecution/completeStepExecution/saveCheckpoint/loadCheckpoint`
  - `companion object: KLogging()`
- **검증**: 컴파일 성공

### T15. `core/BatchStep<I, O>`
- **complexity**: medium
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/core/BatchStep.kt`
- **의존**: T03, T04, T05, T10
- **내용**:
  - `class BatchStep<I : Any, O : Any>(name, chunkSize, reader, processor, writer, skipPolicy = NONE, retryPolicy = RetryPolicy.NONE, commitTimeout = 30.seconds)`
  - `init { name.requireNotBlank("name"); chunkSize.requirePositiveNumber("chunkSize") }`
  - `companion object: KLoggingChannel()`
  - `retryPolicy` 타입: `io.bluetape4k.workflow.api.RetryPolicy`
  - 한국어 KDoc
- **검증**: 컴파일 성공

### T16. `core/WriteTimeoutException` + `writeWithTimeout` 헬퍼
- **complexity**: medium
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/core/WriteTimeoutException.kt` (헬퍼와 같은 파일)
- **의존**: T05
- **내용**:
  - `internal class WriteTimeoutException(message: String, cause: Throwable): RuntimeException(message, cause)`
  - `internal suspend fun writeWithTimeout(writer: BatchWriter<*>, items: List<Any>, timeout: Duration)`
  - `Duration.ZERO` 이하 → 타임아웃 미적용
  - `withTimeout` 의 `TimeoutCancellationException` → `WriteTimeoutException` 로 변환 (retry/skip 경로로 보내기 위함)
  - KDoc: 외부 취소(CancellationException) 는 영향받지 않음 명시
- **검증**: 컴파일 성공; `WriteTimeoutException !is CancellationException` 확인

### T17. `core/BatchStepRunner` — chunk 루프 구현
- **complexity**: high
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/core/BatchStepRunner.kt`
- **의존**: T12, T15, T16
- **구현 절차 (스펙 §5.3 완전 반영)**:
  1. `stepExecution = repository.findOrCreateStepExecution(jobExecution, step.name)`
  2. `if (stepExecution.status in {COMPLETED, COMPLETED_WITH_SKIPS})` → 즉시 기존 리포트 반환 (skip)
  3. `try` 블록: reader.open() → writer.open() → checkpoint 복원 (null guard 필수)
     - (3a) `val checkpoint = repository.loadCheckpoint(stepExecution.id)`
     - (3b) `if (checkpoint != null) step.reader.restoreFrom(checkpoint)`   // 신규 실행 시 restoreFrom 호출 금지
     - KDoc: "checkpoint 가 null 인 경우 restoreFrom() 을 호출하지 않는다 — 신규 실행에서는 reader 초기 상태를 유지한다."
  4. 카운터 누적 초기화 (readCount/writeCount/skipCount 는 `stepExecution` 값에서 시작)
  5. `mainLoop`:
     - chunk 수집 repeat(chunkSize) — reader.read() → null 이면 `eofReached = true` 후 repeat 탈출
     - processor 호출 — `CancellationException` 즉시 재전파, 다른 예외는 `SkipPolicy` 아이템 단위 평가
     - chunk 비었고 EOF → mainLoop break; chunk 비었지만 EOF 아님 → 다음 윈도우로 continue (write 스킵)
  6. writer 호출 루프 — `attempts++` / `writeWithTimeout(writer, chunk, step.commitTimeout)` → `reader.onChunkCommitted()` → `reader.checkpoint()?.let { repository.saveCheckpoint(...) }` → `writeCount += chunk.size` → break
  7. `CancellationException` 즉시 재전파; 그 외 예외 → retryPolicy 평가 (`attempts < maxAttempts` 면 `delay(currentDelay)`, `currentDelay = min(currentDelay * backoffMultiplier, maxDelay)`, continue) → 소진 시 skipPolicy 청크 단위 평가 (`skipCount + chunk.size <= maxSkips`) → 모두 소진 시 throw
  8. 정상 종료 → `StepReport(status = if (skipCount > 0) COMPLETED_WITH_SKIPS else COMPLETED, ...)` 리포트 생성, `repository.completeStepExecution`
  9. `catch (e: CancellationException)` — `withContext(NonCancellable) { runCatching { repository.completeStepExecution(stoppedReport) } }` → `throw e` (절대 삼키지 않음)
  10. `catch (e: Throwable)` — FAILED 리포트 저장 후 반환 (throw 하지 않음 — 호출자가 FAILED 리포트 판단)
  11. `finally { withContext(NonCancellable) { runCatching { reader.close() }; runCatching { writer.close() } } }` — 각 리소스 독립 runCatching
- **coding 패턴**:
  - `companion object: KLoggingChannel()`
  - `internal class BatchStepRunner<I : Any, O : Any>(private val step, private val jobExecution, private val repository)`
- **검증**: 컴파일 성공; import 없음 확인 (`ide_optimize_imports` 적용)

### T18. `core/BatchJob` — `SuspendWork` 구현
- **complexity**: high
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/core/BatchJob.kt`
- **의존**: T09, T12, T15, T17
- **내용**:
  - `class BatchJob(name, params = emptyMap(), steps, repository): SuspendWork`
  - `init { name.requireNotBlank("name"); steps.requireNotEmpty("steps") }`
  - `companion object: KLoggingChannel()`
  - `suspend fun run(): BatchReport`:
    1. `jobExecution = repository.findOrCreateJobExecution(name, params)`
    2. `stepReports = mutableListOf<StepReport>()` — for each step `BatchStepRunner(step, jobExecution, repository).run()` → 추가; `status == FAILED` → `throw report.error ?: IllegalStateException(...)`
    3. 정상 완료:
       - `hasSkips = stepReports.any { it.skipCount > 0 }` → `finalStatus` 결정
       - `repository.completeJobExecution(jobExecution, finalStatus)`
       - `return if (hasSkips)`
         - `BatchReport.PartiallyCompleted(jobExecution.copy(status = BatchStatus.COMPLETED_WITH_SKIPS), stepReports)`
       - `else`
         - `BatchReport.Success(jobExecution.copy(status = BatchStatus.COMPLETED), stepReports)`
       - `// jobExecution.copy(status=...) 필수 — 결과 객체가 올바른 상태를 반영해야 함`
    4. `catch (e: CancellationException) { withContext(NonCancellable) { runCatching { completeJobExecution(STOPPED) } }; throw e }`
    5. `catch (e: Throwable) { withContext(NonCancellable) { runCatching { completeJobExecution(FAILED) } }; return Failure(jobExecution.copy(status = FAILED), stepReports, e) }`
  - `override suspend fun execute(context: WorkContext): WorkReport` — §5.4 매핑:
    - `context["batch.${name}.startTime"] = Instant.now()`
    - `Success` → `WorkReport.success(context)` + `context["batch.${name}.report"] = report`
    - `PartiallyCompleted` → `WorkReport.success(context)` + `context["batch.${name}.skipCount"]` + `context["batch.${name}.report"]`
    - `Failure` → `WorkReport.failure(context, report.error)`
    - `Stopped` → `WorkReport.cancelled(context, "명시적 중단 (v2)")`
    - `catch (e: CancellationException) { throw e }` — 절대 삼키지 않음
- **검증**: 컴파일 성공; `ide_diagnostics` 통과

### T19. `core/dsl/BatchJobBuilder` + `BatchStepBuilder` + `BatchDsl` + `batchJob()` 진입점
- **complexity**: medium
- **파일**:
  - `utils/batch/src/main/kotlin/io/bluetape4k/batch/core/dsl/BatchDsl.kt` — `@DslMarker annotation class BatchDsl` + `inline fun batchJob(...)`
  - `utils/batch/src/main/kotlin/io/bluetape4k/batch/core/dsl/BatchJobBuilder.kt`
  - `utils/batch/src/main/kotlin/io/bluetape4k/batch/core/dsl/BatchStepBuilder.kt`
- **의존**: T10, T14, T15, T18
- **내용**:
  - `@BatchDsl class BatchJobBuilder(private val name: String)` — `private var _repository = InMemoryBatchJobRepository()`, `private var _params = emptyMap()`, `steps = mutableListOf<BatchStep<*, *>>()`
    - `fun repository(...)`, `fun params(...)` (apply 패턴)
    - `inline fun <reified I, reified O> step(name, block)` → `@PublishedApi internal fun addStep(step)`
    - `fun build(): BatchJob`
  - `@BatchDsl class BatchStepBuilder<I : Any, O : Any>(val name: String)` — private var _reader/_processor/_writer/_chunkSize(=100)/_skipPolicy/_retryPolicy/_commitTimeout(=30.seconds)
    - `init { name.requireNotBlank("name") }`
    - 함수 setter: `fun reader(...)`, `fun processor(processor: BatchProcessor)`, `fun processor(block: suspend (I) -> O?)`, `fun writer(...)`, `fun chunkSize(size) { size.requirePositiveNumber("chunkSize"); ... }`, `fun skipPolicy/retryPolicy/commitTimeout`
    - `fun build(): BatchStep<I, O>` — `requireNotNull(_reader) { "reader must be set for step '$name'" }` / `requireNotNull(_writer)`
  - top-level: `inline fun batchJob(name: String, block: BatchJobBuilder.() -> Unit): BatchJob = BatchJobBuilder(name).apply(block).build()`
  - `companion object: KLogging()` 양쪽 모두
- **검증**: 컴파일 성공; DSL 으로 `batchJob("x") { step<Int, Int>("s") { ... } }` 사용 가능

---

## Phase 4 — JDBC 백엔드 (`jdbc/`)

### T20. `jdbc/tables/BatchJobExecutionTable` + `BatchStepExecutionTable` + `toParamsHash()` 헬퍼
- **complexity**: medium
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/jdbc/tables/BatchJobExecutionTable.kt`, `BatchStepExecutionTable.kt`
- **내용**:
  - `object BatchJobExecutionTable: LongIdTable("batch_job_execution")` — `jobName: varchar(100).index()`, `paramsHash: varchar(64).nullable()`, `status: enumerationByName<BatchStatus>("status", 20)`, `params: text.nullable()`, `startTime: org.jetbrains.exposed.v1.javatime.timestamp`, `endTime: timestamp.nullable()`
  - KDoc — partial unique index 권장 + SELECT FOR UPDATE 금지 이유 명시
  - `internal fun Map<String, Any>.toParamsHash(): String` — stdlib만 사용 (MessageDigest SHA-256), 정렬 보장
  - `object BatchStepExecutionTable: LongIdTable("batch_step_execution")` — `jobExecutionId reference CASCADE`, `stepName varchar(100)`, `status enumerationByName`, `readCount/writeCount/skipCount long default 0`, `checkpoint text.nullable()`, `startTime/endTime`
  - `init { uniqueIndex(jobExecutionId, stepName) }`
- **검증**: 컴파일 성공; Exposed `javatime.timestamp` 사용 확인 (kotlinx-datetime 금지)

### T20b. 공유 `ResultRow` 매퍼 확장 함수
- **complexity**: medium
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/jdbc/tables/ResultRowMappers.kt`
- **의존**: T07, T13, T20
- **내용**:
  - `ResultRow.toJobExecution(checkpointJson: CheckpointJson): JobExecution` — Exposed v1 `ResultRow`(core 타입)를 `JobExecution` data class 로 변환
  - `ResultRow.toStepExecution(checkpointJson: CheckpointJson): StepExecution` — 동일 변환, `checkpoint` text 는 `checkpointJson.read(json)` 으로 역직렬화
  - JDBC 와 R2DBC 모두 동일한 `org.jetbrains.exposed.v1.core.ResultRow` 를 사용하므로 **단일 파일로 공유 가능** — T21(JDBC Repository) 과 T24(R2DBC Repository) 양쪽에서 import
  - `checkpointJson.read(json)` 으로 JSON → `Any?` 역직렬화 (null safe)
  - 한국어 KDoc — JDBC/R2DBC 공용 매퍼임을 명시
- **검증**: 컴파일 성공; T21/T24 에서 import 만으로 사용 가능 (중복 구현 없음)

### T21. `jdbc/ExposedJdbcBatchJobRepository`
- **complexity**: high
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/jdbc/ExposedJdbcBatchJobRepository.kt`
- **의존**: T12, T13, T20, T20b
- **내용**:
  - `class ExposedJdbcBatchJobRepository(private val database: Database, private val checkpointJson: CheckpointJson): BatchJobRepository`
    (기본값 없음 — P1-B: toString() fallback 금지)
  - `companion object: KLoggingChannel()`
  - 모든 DB 호출: `withContext(Dispatchers.VT) { transaction(database) { ... } }`
  - `findOrCreateJobExecution`:
    1. `jobName.requireNotBlank("jobName")`
    2. `val hash = params.toParamsHash()`
    3. SELECT 존재 (`(jobName, hash) AND status IN (RUNNING, FAILED, STOPPED)` `.orderBy(id DESC).limit(1)`) → exist 면 C3: status != RUNNING 인 경우 UPDATE → `copy(status = RUNNING)` 반환
    4. INSERT — try { insertAndGetId } catch (ExposedSQLException) → `isUniqueViolation()` → 재조회 (winner row)
  - `private fun ExposedSQLException.isUniqueViolation()`: `sqlState == "23505" || errorCode == 1062 || message?.contains("unique", ignoreCase = true) == true`
  - `completeJobExecution` / `completeStepExecution` / `saveCheckpoint` / `loadCheckpoint` — 동일 `withContext(Dispatchers.VT) { transaction(database) { ... } }`
  - `findOrCreateStepExecution` (P1-A 4-case 분기):
    - `COMPLETED` / `COMPLETED_WITH_SKIPS` → UPDATE 없이 그대로 반환 (runner skip 처리)
    - `FAILED` / `STOPPED` / `RUNNING` → `RUNNING` 아닌 경우 UPDATE status=RUNNING; `copy(status=RUNNING)` 반환
    - 없으면 INSERT (status=RUNNING)
  - `ResultRow.toJobExecution()` / `ResultRow.toStepExecution(checkpointJson)` — **T20b 에서 구현된 공유 매퍼 import 만 사용** (중복 정의 금지)
  - 한국어 KDoc — partial unique index 권장 + FOR UPDATE 금지 사유 + catch-and-retry 동작
- **검증**: `./gradlew :bluetape4k-batch:compileKotlin` 성공; Dispatchers.VT 경로 확인 (`newVirtualThreadJdbcTransaction` 사용 안함)

### T22. `jdbc/ExposedJdbcBatchReader`
- **complexity**: high
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/jdbc/ExposedJdbcBatchReader.kt`
- **의존**: T03
- **내용**:
  - `class ExposedJdbcBatchReader<K : Comparable<K>, T : Any>(database, table, keyColumn, pageSize = 1_000, rowMapper, keyExtractor): BatchReader<T>`
  - `init { pageSize.requirePositiveNumber("pageSize") }`
  - 내부 상태: `buffer: ArrayDeque<T>`, `lastFetchedKey: K?` (다음 페이지 WHERE), `lastReadKey: K?` (read() 호출 때마다 전진), `lastCommittedKey: K?` (checkpoint), `exhausted: Boolean`
  - `read()`: buffer 비었고 !exhausted → `fetchNextPage()`; removeFirstOrNull → null = 종료; `lastReadKey = keyExtractor(item)`
  - `checkpoint(): Any? = lastCommittedKey`
  - `onChunkCommitted()`: `lastCommittedKey = lastReadKey; lastFetchedKey = lastCommittedKey`
  - `restoreFrom(checkpoint)`: `@Suppress("UNCHECKED_CAST")`, `lastCommittedKey = checkpoint as K; lastFetchedKey = ...; lastReadKey = ...; buffer.clear()`
  - `fetchNextPage()`: `withContext(Dispatchers.VT) { transaction(database) { ... selectAll().let { q -> lastFetchedKey?.let { q.andWhere { keyColumn greater it } }; q.orderBy(keyColumn, ASC).limit(pageSize).map(rowMapper) } } }`
    - 빈 페이지 → `exhausted = true` / 아니면 buffer addAll + `lastFetchedKey = keyExtractor(page.last())`
  - `close()` — `runCatching { buffer.clear() }.onFailure { log.warn(it) { "..." } }`
  - `companion object: KLoggingChannel()`
- **검증**: 컴파일 성공

### T23. `jdbc/ExposedJdbcBatchWriter`
- **complexity**: medium
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/jdbc/ExposedJdbcBatchWriter.kt`
- **의존**: T05
- **내용**:
  - `class ExposedJdbcBatchWriter<T : Any>(database, table, ignore = false, bind: BatchInsertStatement.(T) -> Unit): BatchWriter<T>`
  - `suspend fun write(items)`: empty → return; `withContext(Dispatchers.VT) { transaction(database) { table.batchInsert(items, ignore = ignore) { bind(it) } } }`
  - `companion object: KLoggingChannel()`
- **검증**: 컴파일 성공

---

## Phase 5 — R2DBC 백엔드 (`r2dbc/`)

### T24. `r2dbc/ExposedR2dbcBatchJobRepository`
- **complexity**: high
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/r2dbc/ExposedR2dbcBatchJobRepository.kt`
- **의존**: T12, T13, T20, T20b
- **선행 검증 (구현 전 필수)**:
  - `ide_find_class` 로 `ExposedR2dbcException` 검색 — 없으면 `R2dbcException` (`io.r2dbc.spi`) 또는 Exposed 가 던지는 실제 예외 타입 확인
  - `ide_find_references` 로 Exposed R2DBC 소스에서 실제 예외 타입·SQLSTATE 접근 경로 확인
  - fallback: `private fun Throwable.isUniqueViolation(): Boolean` 을 `Throwable` 확장으로 작성하여 다형성 처리 (`ExposedR2dbcException` / `R2dbcException` / `SQLException` 모두 포괄)
- **내용**:
  - `class ExposedR2dbcBatchJobRepository(private val database: R2dbcDatabase, private val checkpointJson: CheckpointJson): BatchJobRepository`
    (기본값 없음 — P1-B: toString() fallback 금지)
  - `companion object: KLoggingChannel()`
  - 모든 DB 호출: `suspendTransaction(db = database) { ... }` — `withContext` 불필요 (native suspend)
  - `findOrCreateJobExecution` — JDBC 동일 시맨틱 + `Throwable.isUniqueViolation()` catch-and-retry (위 선행 검증 결과에 따라 확장 함수 배치)
  - 테이블 참조는 `io.bluetape4k.batch.jdbc.tables.BatchJobExecutionTable/BatchStepExecutionTable` + `toParamsHash()` 재사용
  - `ResultRow.toJobExecution(checkpointJson)` / `toStepExecution(checkpointJson)` — **T20b 에서 구현된 공유 매퍼 import 만 사용** — Flow `map { }.firstOrNull()` 경로에서 그대로 호출
  - `findOrCreateStepExecution` (P1-A 4-case 분기):
    - `COMPLETED` / `COMPLETED_WITH_SKIPS` → UPDATE 없이 그대로 반환
    - `FAILED` / `STOPPED` / `RUNNING` → RUNNING 아닌 경우 UPDATE status=RUNNING; `copy(status=RUNNING)` 반환
    - 없으면 INSERT (status=RUNNING)
  - `completeJobExecution` / `completeStepExecution` / `saveCheckpoint` / `loadCheckpoint` — `suspendTransaction(db = database)` + Flow `map { it[checkpoint] }.firstOrNull()?.let(checkpointJson::read)`
  - 한국어 KDoc
- **검증**: 컴파일 성공; `Dispatchers.VT` / `withContext` 경로 없음 확인

### T25. `r2dbc/ExposedR2dbcBatchReader`
- **complexity**: high
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/r2dbc/ExposedR2dbcBatchReader.kt`
- **의존**: T03
- **내용**:
  - `class ExposedR2dbcBatchReader<K : Comparable<K>, T : Any>(database, table, keyColumn, pageSize = 1_000, rowMapper: suspend (ResultRow) -> T, keyExtractor): BatchReader<T>`
  - `init { pageSize.requirePositiveNumber("pageSize") }`
  - 내부 상태는 JDBC 구현과 동일 (buffer/lastFetchedKey/lastReadKey/lastCommittedKey/exhausted)
  - `fetchNextPage()`: `suspendTransaction(db = database) { table.selectAll().let { q -> ... }.orderBy(..., ASC).limit(pageSize).map(rowMapper).toList() }`
  - 한국어 KDoc — "internal transport Flow 일 뿐 reactive streaming 아님" 명시
  - `companion object: KLoggingChannel()`
- **검증**: 컴파일 성공

### T26. `r2dbc/ExposedR2dbcBatchWriter`
- **complexity**: medium
- **파일**: `utils/batch/src/main/kotlin/io/bluetape4k/batch/r2dbc/ExposedR2dbcBatchWriter.kt`
- **의존**: T05
- **내용**:
  - `class ExposedR2dbcBatchWriter<T : Any>(database, table, bind: BatchInsertStatement.(T) -> Unit): BatchWriter<T>`
  - `suspend fun write(items)`: empty → return; `suspendTransaction(db = database) { table.batchInsert(items) { bind(it) } }`
  - `companion object: KLoggingChannel()`
- **검증**: 컴파일 성공

---

## Phase 6 — 단위 테스트

> T27~T31 병렬 수행 가능. 모든 테스트는 `runTest { }` (가상 시간) 또는 `runSuspendIO { }` (IO suspend) 사용.

### T27. `core/InMemoryBatchJobRepositoryTest`
- **complexity**: medium
- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/core/InMemoryBatchJobRepositoryTest.kt`
- **내용**:
  - `findOrCreateJobExecution` — 신규 생성 / 재시작 (RUNNING/FAILED/STOPPED 복원) / 동일 params 매칭 / 다른 params 는 독립
  - `findOrCreateStepExecution` — 신규 / 재시작 시 RUNNING 복원
  - `saveCheckpoint` + `loadCheckpoint` — null → 값 → null 흐름
  - `completeJobExecution` / `completeStepExecution` — status/endTime 반영
  - `runTest { }` 사용
- **검증**: 테스트 통과

### T28. `core/BatchStepRunnerTest` — chunk 루프 경계 케이스
- **complexity**: high
- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/core/BatchStepRunnerTest.kt`
- **의존**: T27
- **내용**: `FakeReader`, `FakeWriter`, `FailingWriter`, `ThrowingProcessor` helper 클래스 정의 + 시나리오
  - (1) 정상 — 1000 아이템 chunkSize=100 → 10 chunk writer.write 호출 / writeCount=1000
  - (2) EOF 직전 chunk 가 chunkSize 미만 — 105 아이템 chunkSize=100 → 2 chunk (100 + 5)
  - (3) Processor null 반환 = filter — skipCount 증가 없음
    - 추가 어서션 (filter null write path 검증):
      - `writer.total == readCount - filteredCount` (writer 가 받은 아이템 수 = 읽은 수 - 필터된 수)
      - `skipCount == 0` (filter 는 skip 카운트 증가 없음)
      - `writeCount == readCount - filteredCount` (성공 write 수 = 필터 제외 수)
  - (4) Processor 예외 + SkipPolicy.ALL — skipCount 증가, 해당 아이템은 writer 미전달
  - (5) Processor 예외 + SkipPolicy.NONE — 최초 예외에서 Step FAILED, report.error 존재
  - (6) Writer 예외 + RetryPolicy(maxAttempts=3, delay=1s) — `runTest` 가상 시간으로 3회 재시도, `advanceTimeBy(3.seconds)` 검증
  - (7) Writer retry 소진 후 SkipPolicy 청크 단위 평가 — skipCount += chunk.size
  - (8) `CancellationException` 전파 검증 — `runTest { job = launch { runner.run() }; job.cancelAndJoin() }` → STOPPED 저장 + 예외 재전파
  - (9) 재시작 시나리오 — 이미 `COMPLETED` 인 StepExecution 주입 → runner 가 즉시 기존 리포트 반환 (skip 처리)
  - (10) `commitTimeout` 동작 — Writer 가 `delay(60.seconds)` 호출 시 `WriteTimeoutException` 으로 래핑되어 retry 경로 진입
  - (11) `commitTimeout = Duration.ZERO` → writer 가 임의 지연 후 실행되어도 `WriteTimeoutException` 미발생 (타임아웃 비활성 확인)
  - (12) `reader.open()` throws → Step `FAILED`, `finally` 블록에서 `writer.close()` 호출 시도 (독립 `runCatching`)
  - (13) `writer.close()` throws in `finally` → `COMPLETED` 상태 유지, 예외 로깅 후 삼키기 (`runCatching`)
- **검증**: 모든 시나리오 통과

### T29. `core/BatchJobTest` — 다중 스텝 + SuspendWork 매핑
- **complexity**: high
- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/core/BatchJobTest.kt`
- **의존**: T27
- **내용**:
  - 2단계 Step — 순차 실행 확인
  - 1단계 실패 → `BatchReport.Failure`, `jobExecution.status == FAILED`
  - 1단계 skip → `PartiallyCompleted` + `failedSteps` helper
  - `execute(WorkContext)` 매핑 — Success/PartiallyCompleted/Failure → WorkReport 변환 검증, context key (`batch.{name}.report`, `batch.{name}.skipCount`, `batch.{name}.startTime`)
  - CancellationException — `launch { job.execute(ctx) }.cancelAndJoin()` → 취소 전파, `JobExecution.status == STOPPED` 확인
- **검증**: 테스트 통과

### T30. `api/SkipPolicyTest`
- **complexity**: low
- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/api/SkipPolicyTest.kt`
- **내용**:
  - `NONE` — maxSkips = 0
  - `ALL` — `matchesException(RuntimeException())` == true
  - `skipAll(0)` → `requirePositiveNumber` 예외
  - `skipOn = setOf(IOException::class.java)` — IOException ok, RuntimeException false
- **검증**: 통과

### T31. `core/dsl/BatchDslTest`
- **complexity**: medium
- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/core/dsl/BatchDslTest.kt`
- **내용**:
  - `batchJob("x") { step<Int, Int>("s") { chunkSize(10); reader(...); writer(...) } }` → BatchJob instance
  - reader/writer 미설정 → `requireNotNull` 예외
  - `chunkSize(0)` → requirePositiveNumber 예외
  - `name = ""` → requireNotBlank 예외
  - `repository(InMemoryBatchJobRepository())` + `params(mapOf("k" to "v"))` 적용 확인
- **검증**: 통과

---

## Phase 7 — JDBC/R2DBC 통합 테스트 + Workflow 통합

### T32. `jdbc/ExposedJdbcBatchJobRepositoryTest` (H2)
- **complexity**: high
- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/jdbc/ExposedJdbcBatchJobRepositoryTest.kt`
- **내용**:
  - `@BeforeAll` H2 in-memory `Database.connect(HikariDataSource)` + `SchemaUtils.create(BatchJobExecutionTable, BatchStepExecutionTable)`
  - `findOrCreateJobExecution` 신규 INSERT + 동일 params → 재조회 + params 다르면 독립
  - RUNNING/FAILED/STOPPED 재시작 → copy(status=RUNNING) 업데이트 확인
  - `findOrCreateStepExecution` — C3 패턴 확인
  - `saveCheckpoint` + `loadCheckpoint` — JSON 왕복
  - 동시 INSERT 경쟁 (2 코루틴 parallel launch) — 동일 jobName/params → 하나의 레코드만 생성 (partial unique index 또는 catch-and-retry) 검증
  - `runSuspendIO { }` 사용
- **검증**: 통과

### T33. `jdbc/ExposedJdbcBatchIntegrationTest` — end-to-end
- **complexity**: high
- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/jdbc/ExposedJdbcBatchIntegrationTest.kt`
- **의존**: T32
- **내용**:
  - 시나리오 1: 10,000 rows INSERT → `ExposedJdbcBatchReader` + `ExposedJdbcBatchWriter` 복제 → 원본과 count 비교
  - 시나리오 2: 재시작 — 5,000 건 처리 후 강제 `FailingWriter` → 두 번째 실행 시 나머지 5,000 건만 처리, 총 writer.total == 10,000 (checkpoint 재시작)
    - sub-case (mid-buffer crash):
      - `pageSize=500, chunkSize=100`
      - Reader 가 500건 버퍼링 (`lastFetchedKey=500`), Writer 2회 성공 (200건 커밋, `lastCommittedKey=200`)
      - → 강제 예외 발생
      - → 재시작 시 201~500 범위 재처리 (`lastFetchedKey=lastCommittedKey=200` 에서 재개)
      - → 최종 write 합계: 800건 (중복 없음, 누락 없음)
      - 이 케이스는 `lastFetchedKey` vs `lastCommittedKey` 분리 설계의 핵심 검증임
  - 시나리오 3: 다중 Step — step1 COMPLETED 저장 후 두 번째 실행 시 step1 skip 확인
  - 시나리오 4: processor 적용 (`row.toDto()`) — 변환된 타입이 writer 로 전달
- **검증**: 통과

### T34. `r2dbc/ExposedR2dbcBatchJobRepositoryTest` (H2 R2DBC)
- **complexity**: high
- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/r2dbc/ExposedR2dbcBatchJobRepositoryTest.kt`
- **내용**:
  - `R2dbcDatabase.connect("r2dbc:h2:mem:///test")` + schema 생성
  - T32 와 동일 시나리오 (신규/재시작/동시 INSERT/checkpoint 왕복)
  - `suspendTransaction(db = database) { ... }` 경로 확인
- **검증**: 통과

### T35. `r2dbc/ExposedR2dbcBatchIntegrationTest`
- **complexity**: high
- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/r2dbc/ExposedR2dbcBatchIntegrationTest.kt`
- **의존**: T34
- **내용**: T33 시나리오와 동일하나 `ExposedR2dbcBatchReader/Writer/BatchJobRepository` 사용
  - **시나리오 2 의 sub-case (mid-buffer crash) 포함 필수**:
    - `pageSize=500, chunkSize=100`
    - Reader 가 500건 버퍼링 (`lastFetchedKey=500`), Writer 2회 성공 (200건 커밋, `lastCommittedKey=200`)
    - → 강제 예외 발생
    - → 재시작 시 201~500 범위 재처리 (`lastFetchedKey=lastCommittedKey=200` 에서 재개)
    - → 최종 write 합계: 800건 (중복 없음, 누락 없음)
    - 이 케이스는 `lastFetchedKey` vs `lastCommittedKey` 분리 설계의 핵심 검증임
- **검증**: 통과

### T36. `workflow/BatchJobWorkflowIntegrationTest`
- **complexity**: medium
- **파일**: `utils/batch/src/test/kotlin/io/bluetape4k/batch/workflow/BatchJobWorkflowIntegrationTest.kt`
- **내용**:
  - `suspendSequentialFlow("etl") { execute(batchJob("..") { ... }) }` — BatchJob 이 Workflow 안에서 동작
  - `errorStrategy = CONTINUE` 시 BatchJob failure 후 다음 step 실행
  - `PartiallyCompleted` → `WorkReport.success` 반환 확인, `context["batch.extract.skipCount"]` 값 존재
  - 외부 코루틴 취소 시 Workflow 전체 `CancellationException` 전파
  - `STOPPED` → v1 에서는 external cancel 로 대체되므로 StopReport는 생성되지 않는 점 확인
- **검증**: 통과

---

## Phase 8 — 문서화 & 프로젝트 레퍼런스 업데이트

> T37~T40 병렬 수행 가능.

### T37. `README.md` (영어) 작성
- **complexity**: medium
- **파일**: `utils/batch/README.md`
- **구조**: Architecture → UML → Features → Examples
  - Architecture: 개요, 설계 원칙, 모듈 구조 (api/core/jdbc/r2dbc/internal), 의존성
  - UML — 필수 Mermaid 다이어그램 4종:
    - class diagram (BatchReader/BatchProcessor/BatchWriter/BatchStep/BatchJob/BatchJobRepository)
    - sequence diagram (chunk loop + restart — 스펙 §5.5 참고)
    - flowchart (SkipPolicy 결정 트리 — §10.2 참고)
    - state diagram (BatchStatus 전이 — §4.1 참고)
  - Features: Reader/Processor/Writer, chunk 루프, SkipPolicy/RetryPolicy, Restart, Workflow 통합, Exposed JDBC/R2DBC 지원
  - Examples: §14.1 JDBC 마이그레이션, §14.2 R2DBC 파이프라인, §14.3 재시작, §14.4 Workflow 임베딩
- **검증**: Mermaid 다이어그램 4종 모두 포함, 한국어 README 와 내용 일치

### T38. `README.ko.md` (한국어) 작성
- **complexity**: medium
- **파일**: `utils/batch/README.ko.md`
- **내용**: T37 와 동일 구조/예제, 한국어 서술
- **검증**: T37 와 섹션 구조 일치

### T39. 루트 `CLAUDE.md` 업데이트
- **complexity**: low
- **파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/CLAUDE.md`
- **내용**:
  - `### Utilities (utils/)` 표에 `batch` 행 추가 — "Kotlin Coroutine 네이티브 배치 프레임워크 — BatchReader/Processor/Writer chunk 파이프라인, BatchJobRepository 재시작, SkipPolicy/RetryPolicy, Exposed JDBC/R2DBC 이중 백엔드, workflow SuspendWork 통합"
- **검증**: 표 정렬 유지, 다른 항목 변경 없음

### T40. 루트 `README.md` / `README.ko.md` 유틸 섹션 동기화
- **complexity**: low
- **파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/README.md`, `/Users/debop/work/bluetape4k/bluetape4k-projects/README.ko.md`
- **내용**: `Utilities` 섹션에 `bluetape4k-batch` 항목 추가 (workflow 바로 뒤)
- **검증**: 두 파일 내용 일치

---

## Phase 9 — 최종 검증

### T41. 파라미터 검증 일괄 점검
- **complexity**: medium
- **파일**: (코드 전체)
- **내용**: `BatchStep`, `BatchJob`, `BatchJobBuilder`, `BatchStepBuilder`, `SkipPolicy.skipAll`, `InMemoryBatchJobRepository`, `Exposed*BatchJobRepository`, `Exposed*BatchReader/Writer` 의 모든 진입점이 `requireNotBlank` / `requirePositiveNumber` / `requireNotEmpty` (from `io.bluetape4k.support.*`) 를 사용하는지 확인. 맨손 `require(...)` 사용 금지.
- **검증**: `Grep` 로 `require(` 직접 호출 탐색 → 0건

### T42. `bluetape4k-patterns` 체크리스트 적용 검토
- **complexity**: medium
- **파일**: (코드 전체)
- **체크 항목 (스펙 부록 A)**:
  - [ ] `companion object: KLogging()` — `JobExecution/StepExecution/StepReport/SkipPolicy/InMemoryBatchJobRepository/BatchStepFailedException/BatchJobBuilder/BatchStepBuilder`
  - [ ] `companion object: KLoggingChannel()` — `BatchStep/BatchJob/BatchStepRunner/ExposedJdbcBatch*/ExposedR2dbcBatch*`
  - [ ] `data class` (`JobExecution/StepExecution/StepReport/SkipPolicy`) → `Serializable + serialVersionUID = 1L + companion object: KLogging()`
  - [ ] `init { }` 에서 `requireNotBlank/requirePositiveNumber` — 직접 `require(...)` 금지
  - [ ] DSL 진입점은 `inline fun` + `@DslMarker`
  - [ ] DSL builder 는 `var` public 프로퍼티 대신 함수 setter
  - [ ] `close()` 는 각 리소스마다 독립 `runCatching { }` 블록
  - [ ] `CancellationException` catch 금지 → 항상 rethrow, finally 는 `NonCancellable`
  - [ ] `runBlocking` 금지, JDBC 는 `withContext(Dispatchers.VT) { transaction(database) { ... } }`
  - [ ] `org.jetbrains.exposed.v1.javatime.timestamp` 사용 — kotlinx-datetime 금지
  - [ ] 테스트는 Kluent + JUnit5 + `runTest { }` / `runSuspendIO { }`
- **검증**: 모든 체크박스 통과, `Grep` 로 `runBlocking\|kotlinx-datetime\|newVirtualThreadJdbcTransaction` 탐색 → 0건

### T43. 컴파일 + 전체 테스트 실행
- **complexity**: medium
- **내용**:
  - `./gradlew :bluetape4k-batch:compileKotlin :bluetape4k-batch:compileTestKotlin` 성공
  - `./gradlew :bluetape4k-batch:test` 전체 통과
  - `ide_diagnostics` 로 주요 `.kt` 파일 clean 확인 (import 경고/deprecated 없음)
- **검증**: BUILD SUCCESSFUL

### T44. `docs/testlogs/YYYY-MM.md` 기록
- **complexity**: low
- **파일**: `docs/testlogs/2026-04.md`
- **내용**: 현재 달 파일 맨 위에 `bluetape4k-batch` 테스트 결과 행 추가 (날짜/모듈/테스트 수/통과/실패/비고)
- **검증**: 허브 `docs/testlog.md` 링크 유효

### T45. `docs/superpowers/index/YYYY-MM.md` 항목 추가 + `INDEX.md` 갱신
- **complexity**: low
- **파일**: `docs/superpowers/index/2026-04.md`, `docs/superpowers/INDEX.md`
- **내용**:
  - `2026-04.md` 맨 위에 spec + plan + 구현 완료 항목 추가
  - `INDEX.md` 건수 갱신
- **검증**: 허브에서 신규 항목 보임

---

## 태스크 목록 요약

| # | 제목 | Complexity |
|---|------|------------|
| T01 | `utils/batch/build.gradle.kts` 작성 | low |
| T02 | `BatchDefaults.kt` — 공용 디폴트 상수 | low |
| T03 | `BatchReader<T>` 인터페이스 | medium |
| T04 | `BatchProcessor<I, O>` fun interface | low |
| T05 | `BatchWriter<T>` 인터페이스 | low |
| T06 | `BatchStatus` enum + 상태 전이 규칙 | low |
| T07 | `JobExecution` + `StepExecution` data class | low |
| T08 | `StepReport` data class | low |
| T09 | `BatchReport` sealed interface | medium |
| T10 | `SkipPolicy` data class | medium |
| T11 | `BatchStepFailedException` | low |
| T12 | `BatchJobRepository` 인터페이스 | medium |
| T13 | `internal/CheckpointJson.kt` 체크포인트 JSON 직렬화 | medium |
| T14 | `core/InMemoryBatchJobRepository` | medium |
| T15 | `core/BatchStep<I, O>` | medium |
| T16 | `core/WriteTimeoutException` + `writeWithTimeout` 헬퍼 | medium |
| T17 | `core/BatchStepRunner` — chunk 루프 구현 | high |
| T18 | `core/BatchJob` — `SuspendWork` 구현 | high |
| T19 | `core/dsl/BatchJobBuilder` + `BatchStepBuilder` + `BatchDsl` + `batchJob()` | medium |
| T20 | `jdbc/tables/BatchJobExecutionTable` + `BatchStepExecutionTable` + `toParamsHash()` | medium |
| T20b | `jdbc/tables/ResultRowMappers.kt` — 공유 `ResultRow` 매퍼 (JDBC/R2DBC 공용) | medium |
| T21 | `jdbc/ExposedJdbcBatchJobRepository` | high |
| T22 | `jdbc/ExposedJdbcBatchReader` | high |
| T23 | `jdbc/ExposedJdbcBatchWriter` | medium |
| T24 | `r2dbc/ExposedR2dbcBatchJobRepository` | high |
| T25 | `r2dbc/ExposedR2dbcBatchReader` | high |
| T26 | `r2dbc/ExposedR2dbcBatchWriter` | medium |
| T27 | `core/InMemoryBatchJobRepositoryTest` | medium |
| T28 | `core/BatchStepRunnerTest` — chunk 루프 경계 케이스 | high |
| T29 | `core/BatchJobTest` — 다중 스텝 + SuspendWork 매핑 | high |
| T30 | `api/SkipPolicyTest` | low |
| T31 | `core/dsl/BatchDslTest` | medium |
| T32 | `jdbc/ExposedJdbcBatchJobRepositoryTest` (H2) | high |
| T33 | `jdbc/ExposedJdbcBatchIntegrationTest` — end-to-end | high |
| T34 | `r2dbc/ExposedR2dbcBatchJobRepositoryTest` (H2 R2DBC) | high |
| T35 | `r2dbc/ExposedR2dbcBatchIntegrationTest` | high |
| T36 | `workflow/BatchJobWorkflowIntegrationTest` | medium |
| T37 | `README.md` (영어) 작성 | medium |
| T38 | `README.ko.md` (한국어) 작성 | medium |
| T39 | 루트 `CLAUDE.md` 업데이트 | low |
| T40 | 루트 `README.md` / `README.ko.md` 유틸 섹션 동기화 | low |
| T41 | 파라미터 검증 일괄 점검 | medium |
| T42 | `bluetape4k-patterns` 체크리스트 적용 검토 | medium |
| T43 | 컴파일 + 전체 테스트 실행 | medium |
| T44 | `docs/testlogs/YYYY-MM.md` 기록 | low |
| T45 | `docs/superpowers/index/YYYY-MM.md` + `INDEX.md` 갱신 | low |

**총 태스크 수**: 46 (T20b 추가)
**complexity high**: 12 (T17, T18, T21, T22, T24, T25, T28, T29, T32, T33, T34, T35)
**complexity medium**: 20 (T20b 포함)
**complexity low**: 14

---

## 병렬 실행 가이드

- **Phase 2**: T03~T12 동시 병렬 가능 (T09 만 T07/T08 의존, 나머지 독립)
- **Phase 3**: 선형 의존 (T13 → T14 → T15 → T16 → T17 → T18 → T19)
- **Phase 4**: T20 → T20b → (T21, T22, T23 병렬) — T21 은 T20b(공유 매퍼)에 의존
- **Phase 5**: (T24, T25, T26 병렬) — T24 는 T20 + T20b 에 의존
- **Phase 6**: T27~T31 모두 병렬 가능
- **Phase 7**: T32 → T33 순차, T34 → T35 순차, T36 독립 — Phase 7 내 3개 계열 병렬 가능
- **Phase 8**: T37, T38, T39, T40 전부 병렬
- **Phase 9**: T41 → T42 → T43 → T44 → T45 순차

---

## 검증 기준 (Definition of Done)

1. `./gradlew :bluetape4k-batch:compileKotlin :bluetape4k-batch:compileTestKotlin` 성공
2. `./gradlew :bluetape4k-batch:test` 전체 통과 — JDBC/R2DBC 통합 포함
3. 모든 public API 에 한국어 KDoc 작성
4. `README.md` + `README.ko.md` 작성 (Mermaid 다이어그램 4종 포함)
5. 루트 `CLAUDE.md` + `README.md` / `README.ko.md` 에 `bluetape4k-batch` 반영
6. `docs/testlogs/2026-04.md` 및 `docs/superpowers/index/2026-04.md` 항목 추가 + `INDEX.md` 갱신
7. `Grep` 로 `runBlocking`, `kotlinx-datetime`, `newVirtualThreadJdbcTransaction`, 맨손 `require(` (support 패키지 외) 탐색 결과 0건
8. `bluetape4k-patterns` 체크리스트 (T42) 전부 통과

---

**End of Plan**
