# bluetape4k-workflow 구현 플랜

**작성일**: 2026-04-05
**스펙**: `docs/superpowers/specs/2026-04-05-workflow-design.md`
**레퍼런스**: [j-easy/easy-flows](https://github.com/j-easy/easy-flows)

---

## 소스 참조

- `utils/states/build.gradle.kts` — build.gradle.kts 템플릿 (동일 utils/ 하위 모듈)
- `utils/states/` — 유사 패턴 참조 (DSL 빌더, 동기/코루틴 이중 구현)
- `buildSrc/Libs.kt` — 의존성 버전 관리

---

## 태스크 요약

| Complexity | 태스크 수 | 설명 |
|:----------:|:---------:|------|
| **high** | 4 | 핵심 API, 동기 Workflow 구현, 코루틴 Workflow 구현, Flow 스트리밍 |
| **medium** | 5 | 동기 DSL, 코루틴 DSL, 동기 테스트, 코루틴 테스트, WorkContext/어댑터 테스트 |
| **low** | 3 | 모듈 초기화, README 작성, CLAUDE.md 업데이트 |
| **합계** | **12** | |

## 병렬 실행 그룹

```
Group A (선행): Task 1 (모듈 초기화)
    │
Group B (병렬): Task 2 (핵심 API) ─── 단독 선행
    │
Group C (병렬): Task 3 (동기 Workflow) ║ Task 5 (코루틴 Workflow + Flow)
    │                                   │
Group D (병렬): Task 4 (동기 DSL)      ║ Task 6 (코루틴 DSL)
    │                                   │
Group E (병렬): Task 7 (API 테스트)    ║ Task 8 (동기 테스트) ║ Task 9 (코루틴 테스트)
    │
Group F (병렬): Task 10 (README)       ║ Task 11 (CLAUDE.md)
    │
Group G: Task 12 (최종 빌드 검증)
```

---

## Task 1: 모듈 초기화 [complexity: low]

- `utils/workflow/` 디렉토리 생성
- `utils/workflow/build.gradle.kts` 작성
  ```kotlin
  dependencies {
      api(project(":bluetape4k-core"))
      testImplementation(project(":bluetape4k-junit5"))

      // Virtual Threads
      implementation(project(":bluetape4k-virtualthread-api"))

      // Coroutines
      implementation(project(":bluetape4k-coroutines"))
      implementation(Libs.kotlinx_coroutines_core)
      testImplementation(Libs.kotlinx_coroutines_test)
  }
  ```
- `settings.gradle.kts` — `utils/workflow` 자동 등록 확인 (`includeModules` 방식)
- 디렉토리 구조 생성:
  - `src/main/kotlin/io/bluetape4k/workflow/api/`
  - `src/main/kotlin/io/bluetape4k/workflow/core/`
  - `src/main/kotlin/io/bluetape4k/workflow/coroutines/`
  - `src/test/kotlin/io/bluetape4k/workflow/api/`
  - `src/test/kotlin/io/bluetape4k/workflow/core/`
  - `src/test/kotlin/io/bluetape4k/workflow/coroutines/`
- **AC**: `./gradlew :bluetape4k-workflow:dependencies` 성공

---

## Task 2: 핵심 API (`api/`) [complexity: high]

### 2-1. 기본 타입

| 파일 | 내용 |
|------|------|
| `WorkStatus.kt` | `enum class WorkStatus { COMPLETED, FAILED, PARTIAL }` |
| `WorkReport.kt` | `sealed interface WorkReport { Success, Failure, PartialSuccess }` — `status`, `context`, `error` 프로퍼티. `PartialSuccess`는 `failedReports: List<WorkReport>` 보유, `status = PARTIAL` |
| `WorkContext.kt` | `ConcurrentHashMap` 기반, `get<T>()`, `set()`, `remove()`, `contains()`, `compute()`, `snapshot()`, `merge()` |
| `ErrorStrategy.kt` | `enum class ErrorStrategy { STOP, CONTINUE }` (CONTINUE 전략은 `PartialSuccess`로 실패 반환, well-known 키 불필요) |
| `RetryPolicy.kt` | `data class RetryPolicy(maxAttempts, delay, backoffMultiplier, maxDelay)` + `NONE`/`DEFAULT` companion. `maxAttempts` = 총 시도 횟수(최초 1회 + 재시도), `maxRetries` 편의 프로퍼티 (`= maxAttempts - 1`) |
| `WorkflowDsl.kt` | `@DslMarker annotation class WorkflowDsl` |

### 2-2. Work 인터페이스

| 파일 | 내용 |
|------|------|
| `Work.kt` | `fun interface Work { fun execute(context: WorkContext): WorkReport }` |
| `NamedWork.kt` | `class NamedWork(name, delegate: Work) : Work` + 팩토리 `fun Work(name, block)` |
| `SuspendWork.kt` | `fun interface SuspendWork { suspend fun execute(context: WorkContext): WorkReport }` |
| `NamedSuspendWork.kt` | `class NamedSuspendWork(name, delegate: SuspendWork) : SuspendWork` + 팩토리 `fun SuspendWork(name, block)` |
| `WorkAdapters.kt` | `Work.asSuspend()` (Dispatchers.IO 래핑), `SuspendWork.asBlocking()` (runBlocking 래핑) |

### 2-3. 마커 인터페이스

| 파일 | 내용 |
|------|------|
| `WorkFlow.kt` | `interface WorkFlow : Work` |
| `SuspendWorkFlow.kt` | `interface SuspendWorkFlow : SuspendWork` |

### 2-4. 기본 상수

| 파일 | 내용 |
|------|------|
| `WorkflowDefaults.kt` | `io.bluetape4k.workflow` 패키지 — 기본 상수 (필요 시) |

- 모든 public 클래스/함수에 Korean KDoc 필수 (스펙의 코드 블록에 이미 작성되어 있으므로 그대로 사용)
- **AC**: 모든 파일 컴파일 성공, `WorkReport.Success`/`WorkReport.Failure`/`WorkReport.PartialSuccess` 생성 확인, `RetryPolicy.maxRetries` == `maxAttempts - 1` 확인

---

## Task 3: 동기 Workflow 구현 (`core/`) [complexity: high]

### 3-1. SequentialWorkFlow

- `SequentialWorkFlow(works, errorStrategy, flowName)` : `WorkFlow`
- `ErrorStrategy.STOP` — 첫 Failure 즉시 반환
- `ErrorStrategy.CONTINUE` — `failedReports: MutableList<WorkReport>` 로컬 변수에 Failure 누적 → 실패가 하나라도 있으면 `WorkReport.PartialSuccess(context, failedReports)` 반환, 모두 성공이면 `Success` 반환
- `companion object : KLogging()`

### 3-2. ParallelWorkFlow

- `ParallelWorkFlow(works, executorService?, timeout, flowName)` : `WorkFlow`
- 기본: `Executors.newVirtualThreadPerTaskExecutor()` (매 실행마다 생성/종료)
- 외부 executor 주입 시 라이프사이클 호출자 관리
- **`executor.invokeAll(callables, timeout, TimeUnit)` 방식으로 실제 timeout 동작 보장**
- timeout 초과 시 미완료 태스크는 cancel 처리 → `WorkReport.Failure(context, TimeoutException)` 변환
- `isDone && !isCancelled` 검사로 정상 완료/취소 판별
- failures 존재 시 첫 번째 Failure 반환
- `finally` 블록에서 내부 executor `shutdown()` + `awaitTermination(timeout)` + `shutdownNow()` fallback

### 3-3. ConditionalWorkFlow

- `ConditionalWorkFlow(predicate, thenWork, otherwiseWork?, flowName)` : `WorkFlow`
- `otherwiseWork` null이면 `WorkReport.Success(context)` 반환

### 3-4. RepeatWorkFlow

- `RepeatWorkFlow(work, repeatPredicate, maxIterations, flowName)` : `WorkFlow`
- `repeatPredicate(WorkReport) -> Boolean` — true이면 계속 반복
- `maxIterations` 제한으로 무한 루프 방지

### 3-5. RetryWorkFlow

- `RetryWorkFlow(work, retryPolicy, flowName)` : `WorkFlow`
- 지수 백오프: `currentDelay *= backoffMultiplier`, `minOf(currentDelay, maxDelay)` 상한
- `Thread.sleep()` 사용 (동기)
- private `Duration.times(Double)` 확장 연산자

- **AC**: 5개 클래스 컴파일 성공, 각 클래스에 Korean KDoc + 코드 예제

---

## Task 4: 동기 DSL 빌더 (`core/WorkflowDsl.kt`) [complexity: medium]

### 빌더 클래스

| 빌더 | Top-level 함수 | 중첩 지원 |
|------|----------------|-----------|
| `SequentialFlowBuilder` | `sequentialFlow(name) {}` | `parallel {}`, `conditional {}`, `repeat {}`, `retry {}` |
| `ParallelFlowBuilder` | `parallelFlow(name) {}` | - |
| `ConditionalFlowBuilder` | `conditionalFlow(name) {}` | `condition {}`, `then()`, `otherwise()` (선택적) |
| `RepeatFlowBuilder` | `repeatFlow(name) {}` | `execute()`, `repeatWhile {}` / `until {}`, `maxIterations` |
| `RetryFlowBuilder` | `retryFlow(name) {}` | `execute()`, `policy()` |
| `WorkflowBuilder` | `workflow(name) {}` | `sequential {}`, `parallel {}`, `conditional {}`, `repeat {}` |

- 모든 빌더에 `@WorkflowDsl` 어노테이션
- `build()` 메서드는 `internal`
- `requireNotNull` 검증 (condition, thenWork, work 등 필수 항목)
- **`WorkflowBuilder`에 루트 중복 선언 가드 추가**: `setRoot()` 메서드에서 `require(rootWork == null)` 체크 → 두 번째 루트 선언 시 `IllegalArgumentException`
- **AC**: DSL 문법으로 중첩 워크플로 구성 가능, 컴파일 성공, 루트 중복 선언 시 예외 확인

---

## Task 5: 코루틴 Workflow 구현 (`coroutines/`) [complexity: high]

### 5-1. SuspendSequentialFlow

- `SuspendSequentialFlow(works, errorStrategy, flowName)` : `SuspendWorkFlow`
- 각 작업 전 `coroutineContext.ensureActive()` 호출로 취소 전파
- CONTINUE 전략: `failedReports: MutableList<WorkReport>` 로컬 변수에 Failure 누적 → 실패가 하나라도 있으면 `WorkReport.PartialSuccess(context, failedReports)` 반환

### 5-2. SuspendParallelFlow

- `SuspendParallelFlow(works, flowName)` : `SuspendWorkFlow`
- `coroutineScope { works.map { async { it.execute(ctx) } }.awaitAll() }`
- 구조화된 동시성: 하나라도 예외 throw 시 나머지 자동 취소

### 5-3. SuspendConditionalFlow

- `SuspendConditionalFlow(predicate: suspend, thenWork, otherwiseWork?, flowName)` : `SuspendWorkFlow`
- suspend predicate 지원

### 5-4. SuspendRepeatFlow

- `SuspendRepeatFlow(work, repeatPredicate: suspend, maxIterations, repeatDelay, flowName)` : `SuspendWorkFlow`
- `coroutineContext.ensureActive()` + `kotlinx.coroutines.delay(repeatDelay)` 사용
- `Duration` 기반 repeatDelay 파라미터

### 5-5. SuspendRetryFlow

- `SuspendRetryFlow(work, retryPolicy, flowName)` : `SuspendWorkFlow`
- `kotlinx.coroutines.delay()` 사용 (Thread.sleep 대신)
- private `Duration.times(Double)` 확장 연산자

### 5-6. Flow\<WorkReport\> 스트리밍 (WorkReportFlowSupport.kt)

- `workReportFlow(works, context): Flow<WorkReport>` — 순차 실행, 각 결과 emit
- `SuspendWork.asFlow(context): Flow<WorkReport>` — 단일 work 실행 결과 Flow

- **AC**: 6개 파일 컴파일 성공, Korean KDoc + 코드 예제

---

## Task 6: 코루틴 DSL 빌더 (`coroutines/SuspendWorkflowDsl.kt`) [complexity: medium]

### 빌더 클래스

| 빌더 | Top-level 함수 | 중첩 지원 |
|------|----------------|-----------|
| `SuspendSequentialFlowBuilder` | `suspendSequentialFlow(name) {}` | `parallel {}`, `conditional {}`, `repeat {}`, `retry {}` |
| `SuspendParallelFlowBuilder` | `suspendParallelFlow(name) {}` | - |
| `SuspendConditionalFlowBuilder` | `suspendConditionalFlow(name) {}` | `condition {}`, `then()`, `otherwise()` (선택적) |
| `SuspendRepeatFlowBuilder` | `suspendRepeatFlow(name) {}` | `execute()`, `repeatWhile {}` / `until {}`, `maxIterations`, `repeatDelay` |
| `SuspendRetryFlowBuilder` | `suspendRetryFlow(name) {}` | `execute()`, `policy()` |
| `SuspendWorkflowBuilder` | `suspendWorkflow(name) {}` | `sequential {}`, `parallel {}`, `conditional {}`, `repeat {}` |

- 모든 빌더에 `@WorkflowDsl` 어노테이션
- suspend 람다 파라미터 (`condition`, `repeatWhile`, `until`)
- **`SuspendWorkflowBuilder`에 루트 중복 선언 가드 추가**: `setRoot()` 메서드에서 `require(rootWork == null)` 체크 → 두 번째 루트 선언 시 `IllegalArgumentException`
- **AC**: DSL 문법으로 코루틴 중첩 워크플로 구성 가능, 컴파일 성공, 루트 중복 선언 시 예외 확인

---

## Task 7: API 테스트 [complexity: medium]

### 테스트 파일

| 파일 | 시나리오 |
|------|----------|
| `api/WorkContextTest.kt` | get/set, remove, contains, compute() 원자적 갱신, merge, snapshot, 병렬 compute() 스레드 안전성 |
| `api/NamedWorkTest.kt` | NamedWork 이름 전달, Work(name) 팩토리, NamedSuspendWork, SuspendWork(name) 팩토리, SAM 변환 |
| `api/WorkAdapterTest.kt` | `Work.asSuspend()` 변환 동작, `SuspendWork.asBlocking()` 변환 동작 |

- **AC**: 모든 테스트 통과

---

## Task 8: 동기 Workflow + DSL 테스트 [complexity: medium]

### 테스트 파일

| 파일 | 시나리오 |
|------|----------|
| `core/SequentialWorkFlowTest.kt` | 전체 성공, 중간 실패 STOP, 중간 실패 CONTINUE → `PartialSuccess` 반환 + `failedReports` 검증, 전체 실패 CONTINUE → `PartialSuccess`, 빈 works |
| `core/ParallelWorkFlowTest.kt` | 전체 성공, 일부 실패, Virtual Threads 실행 확인, 커스텀 executor, **invokeAll timeout 시 미완료 태스크 Failure 처리**, timeout 내 정상 완료 |
| `core/ConditionalWorkFlowTest.kt` | true 분기, false 분기, otherwise 생략 시 no-op, 중첩 조건 |
| `core/RepeatWorkFlowTest.kt` | 조건 충족 반복, maxIterations 제한, until 조건 |
| `core/RetryWorkFlowTest.kt` | 성공까지 재시도, maxAttempts 소진 (총 시도 횟수 = maxAttempts 확인), 지수 백오프 간격 확인, maxDelay 상한, **`maxRetries` 편의 프로퍼티 검증** |
| `core/WorkflowDslTest.kt` | sequentialFlow DSL, parallelFlow DSL, conditionalFlow DSL, repeatFlow DSL, retryFlow DSL, 중첩 workflow DSL, **루트 중복 선언 시 `IllegalArgumentException` 검증** |

- **AC**: 모든 테스트 통과, `PartialSuccess` 반환 + `failedReports` 검증 포함

---

## Task 9: 코루틴 Workflow + DSL 테스트 [complexity: medium]

### 테스트 파일

| 파일 | 시나리오 |
|------|----------|
| `coroutines/SuspendSequentialFlowTest.kt` | 전체 성공, 중간 실패 STOP, **중간 실패 CONTINUE → `PartialSuccess` 반환 + `failedReports` 검증**, ensureActive 취소 전파 |
| `coroutines/SuspendParallelFlowTest.kt` | 병렬 실행 성공, 실패 전파, coroutineScope 취소 |
| `coroutines/SuspendConditionalFlowTest.kt` | suspend predicate true/false, otherwise 생략 |
| `coroutines/SuspendRepeatFlowTest.kt` | delay 반복, 코루틴 취소 전파, ensureActive, maxIterations |
| `coroutines/SuspendRetryFlowTest.kt` | suspend 재시도, delay 백오프, maxDelay 상한 |
| `coroutines/WorkReportFlowTest.kt` | workReportFlow 수집, asFlow 단일 실행, 필터링 |
| `coroutines/SuspendWorkflowDslTest.kt` | 코루틴 DSL 중첩 구성 (sequential > parallel > conditional > repeat > retry), **루트 중복 선언 시 `IllegalArgumentException` 검증** |

- 모든 코루틴 테스트는 `runTest` 사용
- **AC**: 모든 테스트 통과

---

## Task 10: README 작성 [complexity: low]

- `utils/workflow/README.md` (영어)
- `utils/workflow/README.ko.md` (한국어)
- 내용:
  - 모듈 소개 + easy-flows 레퍼런스
  - 핵심 API 설명 (Work, SuspendWork, WorkReport, WorkContext)
  - 동기 Workflow 예제 (Sequential, Parallel, Conditional, Repeat, Retry)
  - 코루틴 Workflow 예제
  - DSL 사용법 (동기 + 코루틴)
  - 중첩 워크플로 예제 (`workflow {}` / `suspendWorkflow {}`)
  - 에러 처리 전략 (ErrorStrategy, RetryPolicy, PartialSuccess)

- **AC**: README.md ↔ README.ko.md 상호 링크, 모든 플로우 타입 예제 포함

---

## Task 11: CLAUDE.md 업데이트 [complexity: low]

- 루트 `CLAUDE.md`의 `### Utilities (utils/)` 테이블에 `workflow` 행 추가:
  ```
  | `workflow` | Kotlin DSL Workflow — SequentialFlow/ParallelFlow/ConditionalFlow/RepeatFlow/RetryFlow, 동기(Virtual Threads) + 코루틴(suspend/Flow) |
  ```
- 스펙 11절의 기존 모듈 관계 반영 (states, rule-engine과 상호 보완)

- **AC**: CLAUDE.md 업데이트 완료

---

## Task 12: 최종 빌드 검증 [complexity: low]

- `./gradlew :bluetape4k-workflow:build` — 전체 빌드 + 테스트 통과
- `./gradlew :bluetape4k-workflow:detekt` — 정적 분석 통과
- 테스트 결과 `docs/testlog.md`에 기록

- **AC**: 빌드 성공, detekt 통과, 테스트 전체 통과

---

## 의존성 그래프

```
Task 1 (모듈 초기화)
  └─▶ Task 2 (핵심 API)
       ├─▶ Task 3 (동기 Workflow) ──▶ Task 4 (동기 DSL) ──▶ Task 8 (동기 테스트)
       ├─▶ Task 5 (코루틴 Workflow) ──▶ Task 6 (코루틴 DSL) ──▶ Task 9 (코루틴 테스트)
       └─▶ Task 7 (API 테스트)
            ┌──────────────────────────┘
            ├─ Task 8 완료 ─┐
            ├─ Task 9 완료 ─┤
            └───────────────┴─▶ Task 10 (README) ║ Task 11 (CLAUDE.md)
                                       └──────────▶ Task 12 (최종 빌드 검증)
```

---

## 총 파일 수 (예상)

| 카테고리 | 파일 수 |
|----------|:-------:|
| api/ (main) | 12 |
| core/ (main) | 6 |
| coroutines/ (main) | 7 |
| api/ (test) | 3 |
| core/ (test) | 6 |
| coroutines/ (test) | 7 |
| build.gradle.kts | 1 |
| README | 2 |
| **합계** | **44** |
