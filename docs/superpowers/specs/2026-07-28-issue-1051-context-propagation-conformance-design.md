# Issue #1051 Coroutine/Reactive Context Propagation Conformance 설계

- Date: 2026-07-28
- Repository: `bluetape4k/bluetape4k-projects`
- Issue: [#1051](https://github.com/bluetape4k/bluetape4k-projects/issues/1051)
- Parent: [#1049](https://github.com/bluetape4k/bluetape4k-projects/issues/1049)
- Milestone: `1.12.0`

## 1. 문제

서버 요청은 coroutine suspension, Reactor subscriber, task executor 같은 비동기 경계를 통과한다. 각 통합 모듈에는 개별 전파 테스트가 있지만, 다음 계약을 동일한 의미와 실패 메시지로 검증하는 공통 conformance fixture가 없다.

- 명시한 parent context가 비동기 작업 내부에서 활성화된다.
- 작업이 성공, 예외, 취소로 끝난 뒤 호출자와 worker의 이전 context가 복원된다.
- 동시에 실행되거나 동일 worker를 재사용하는 독립 요청이 서로의 context를 관찰하지 않는다.
- Spring Boot와 Ktor 어댑터가 같은 의미 계약을 검증한다.

이 공백 때문에 각 프레임워크 테스트가 서로 다른 조건을 검증하거나, 전파 성공만 확인하고 cleanup/leakage 회귀를 놓칠 수 있다.

## 2. 현재 코드 근거

### 2.1 OpenTelemetry coroutine

`infra/opentelemetry`의 `withOtelContext`는 `Context.asContextElement()`를 coroutine context에 결합한다.

- `ContextCoroutineSupport.kt`
- `CoroutineSupportTest.kt`

기존 테스트는 명시한 OTel context가 coroutine 내부에 보이는지 확인하지만, 성공·예외·취소 종료 후 복원과 동일 worker 재사용을 하나의 공통 의미 계약으로 묶지 않는다.

### 2.2 Spring Boot observation

`spring-boot/core`의 `observeSpringSuspending`은 다음 두 bridge를 함께 사용한다.

- `ObservationThreadLocalAccessor.KEY`를 담은 Reactor `Context`
- `Observation.openScope()`를 열고 닫는 `ThreadContextElement`

기존 `SpringObservationSupportTest`는 observation lifecycle과 일부 cancellation cleanup을 검증한다. 하지만 Reactor/coroutine/executor 경계를 공통 snapshot으로 표현하거나 Ktor와 같은 assertion을 실행하지 않는다.

### 2.3 Ktor OpenTelemetry

`ktor/observability`는 `testApplication`과 in-memory OpenTelemetry SDK/exporter로 server span을 검증한다. 기존 `Bluetape4kKtorObservabilityTest`는 성공·오류·취소 HTTP 동작을 다루지만, handler 내부 parent visibility와 독립 요청 간 context isolation을 공통 fixture로 검증하지 않는다.

### 2.4 재사용 가능한 fixture 선례

`testing/junit5`의 `HttpOperationObservabilityConformance.kt`는 provider-neutral snapshot과 assertion만 공유하고, Spring/Ktor가 자신의 관측 결과를 snapshot으로 변환한다. Issue #1051도 같은 경계를 따른다.

## 3. 목표

1. `testing/junit5`에 provider-neutral context propagation snapshot과 assertion을 제공한다.
2. coroutine, Reactor, task executor 경계에 대해 동일한 전파·종료·복원 의미를 표현한다.
3. 성공, 일반 예외, 취소를 구분한다.
4. 동일 single-thread worker 재사용과 동시 요청을 통해 context leakage를 결정적으로 검증한다.
5. `infra/opentelemetry`, `spring-boot/core`, `ktor/observability`가 프레임워크별 bridge를 유지하면서 같은 assertion을 실행한다.
6. production exporter, collector, 외부 HTTP server 없이 테스트가 실행된다.

## 4. 비목표

- MDC 또는 전역 `ThreadLocal` lifecycle을 fixture가 소유하지 않는다.
- scheduler, coroutine dispatcher, executor를 production 코드에서 교체하지 않는다.
- 인증·인가·tenant 정책을 정의하지 않는다.
- OpenTelemetry, Micrometer, Spring, Reactor, Ktor 타입을 shared fixture의 public API에 노출하지 않는다.
- tracing SDK나 exporter를 `testing/junit5`의 새 의존성으로 추가하지 않는다.
- 기존 HTTP observability conformance fixture를 대체하거나 합치지 않는다.
- 테스트가 실제 결함을 증명하지 않는 한 production API를 변경하지 않는다.

## 5. 선택한 설계

### 5.1 위치와 의존성 경계

새 fixture는 `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/observability/`에 둔다.

이 모듈은 이미 assertion, JUnit 5, coroutine core를 제공한다. 새 module이나 새 외부 의존성을 추가하지 않는다. shared fixture는 문자열 marker와 enum, immutable snapshot만 다루며 실제 context의 설치·조회·복원은 각 소비 모듈의 어댑터가 담당한다.

### 5.2 예상 public surface

저장소의 기존 observability fixture naming을 따라 public surface를 다음과 같이 고정한다. 모든 data class는 `Serializable`이며 명시적 `serialVersionUID`를 둔다. 이 타입은 test-support snapshot이지 영속 포맷이 아니므로 직렬화 결과를 장기 저장하거나 wire contract로 사용하지 않는다.

```kotlin
enum class ContextPropagationBoundary {
    COROUTINE,
    REACTOR,
    TASK_EXECUTOR,
    SPRING_OBSERVATION,
    KTOR_REQUEST,
}

enum class ContextPropagationScenario {
    SUCCESS,
    FAILURE,
    CANCELLATION,
    DEADLINE,
    ISOLATION,
}

enum class ContextPropagationTerminal {
    SUCCESS,
    FAILURE,
    CANCELLATION,
    DEADLINE_EXCEEDED,
}

enum class ContextProbeLocation {
    CALLER,
    WORKER,
    REQUEST,
}

enum class ContextRequestAlias {
    SINGLE,
    REQUEST_A,
    REQUEST_B,
    PROBE,
}

enum class ContextObservationPoint {
    BOUNDARY_ENTER,
    AFTER_SUSPENSION,
    BEFORE_TERMINAL,
}

enum class ContextMarkerExpectationMode {
    EXACT,
    ABSENT,
    NOT_IN,
}

data class ContextMarkerObservation(
    val point: ContextObservationPoint,
    val observedMarker: String?,
)

data class ContextMarkerExpectation(
    val point: ContextObservationPoint,
    val expectedMarker: String,
)

data class ContextCleanupProbe(
    val location: ContextProbeLocation,
    val observedMarker: String?,
)

data class ContextCleanupExpectation(
    val location: ContextProbeLocation,
    val expectedMarker: String?,
)

data class ContextPropagationObservation(
    val boundary: ContextPropagationBoundary,
    val scenario: ContextPropagationScenario,
    val requestAlias: ContextRequestAlias,
    val markerObservations: List<ContextMarkerObservation>,
    val cleanupProbes: List<ContextCleanupProbe>,
    val terminal: ContextPropagationTerminal,
)

data class ContextPropagationExpectation(
    val boundary: ContextPropagationBoundary,
    val scenario: ContextPropagationScenario,
    val requestAlias: ContextRequestAlias,
    val markerExpectations: List<ContextMarkerExpectation>,
    val cleanupExpectations: List<ContextCleanupExpectation>,
    val expectedTerminal: ContextPropagationTerminal,
)

data class ContextIsolationSample(
    val requestAlias: ContextRequestAlias,
    val observedMarkers: List<String?>,
)

data class ContextIsolationSampleExpectation(
    val requestAlias: ContextRequestAlias,
    val mode: ContextMarkerExpectationMode,
    val expectedMarker: String? = null,
    val forbiddenMarkers: List<String> = emptyList(),
    val minimumObservationCount: Int = 1,
)

data class ContextIsolationObservation(
    val boundary: ContextPropagationBoundary,
    val samples: List<ContextIsolationSample>,
    val cleanupProbes: List<ContextCleanupProbe>,
)

data class ContextIsolationExpectation(
    val boundary: ContextPropagationBoundary,
    val samples: List<ContextIsolationSampleExpectation>,
    val cleanupExpectations: List<ContextCleanupExpectation>,
)

fun assertContextPropagationConformance(
    observation: ContextPropagationObservation,
    expectation: ContextPropagationExpectation,
)

fun assertContextIsolation(
    observation: ContextIsolationObservation,
    expectation: ContextIsolationExpectation,
)
```

계약은 다음과 같다.

- propagation observation과 expectation의 observation-point 집합은 일치해야 하며 각 point의 `observedMarker == expectedMarker`여야 한다. suspension 경계는 `BOUNDARY_ENTER`와 `AFTER_SUSPENSION`을 모두 포함한다.
- observation의 `boundary`, `scenario`, `requestAlias`, `terminal`은 expectation과 각각 일치한다.
- caller, worker, request cleanup은 `ContextProbeLocation`별 actual/expected 목록으로 비교한다. 해당 경계에 존재하지 않는 probe는 expectation에 넣지 않는다.
- 작업 시작 전 context가 root였으면 expected cleanup marker는 오직 `null`로 표현한다. 문자열 `"root"`는 marker로 허용하지 않는다.
- `FAILURE`, `CANCELLATION`, `DEADLINE_EXCEEDED`는 서로 바꿔 기록할 수 없다.
- isolation observation과 expectation의 boundary 및 request alias 집합은 일치해야 한다. `EXACT`는 별도로 구성한 하나의 non-null expected marker만, `ABSENT`는 `null`만, `NOT_IN`은 non-empty forbidden marker 목록에 없는 non-null 값만 허용한다. 모든 관측 목록은 `minimumObservationCount` 이상이어야 한다. expectation의 request alias와 `EXACT` marker는 모두 유일하며, alias 중복이나 빈 관측 목록, mode와 맞지 않는 필드 조합은 assertion 전에 거부한다.
- Ktor A/B request는 `EXACT`, 마지막 무부모 probe request는 A/B trace ID를 forbidden 목록으로 둔 `NOT_IN` expectation을 사용한다. 따라서 probe 기대값을 probe 관측에서 역으로 만들지 않는다.
- assertion 실패 메시지는 marker 원문을 노출하지 않는다. enum 기반 boundary/scenario/terminal/request alias/observation point/probe location/expectation mode와 `match`/`mismatch` 관계만 기록한다.
- 민감 문자열과 CR/LF canary를 넣은 negative unit test로 실패 메시지 redaction을 고정한다.

snapshot에는 test-owned synthetic marker만 넣는다. production 요청 ID, 실제 사용자 입력, 외부 trace ID는 넣지 않는다. Ktor W3C 테스트의 고정 trace/span ID도 test fixture가 생성한 synthetic 값만 사용하며 assertion/log에는 원문을 출력하지 않는다.

모든 public enum, snapshot, assertion에는 English KDoc과 최소 사용 예제를 둔다. enum 확장은 additive test-support 변경으로 허용하지만 caller는 exhaustive persistence contract로 사용하지 않는다. data class constructor 변경은 compatibility 검토 없이 수행하지 않는다.

### 5.3 경계별 어댑터 책임

| 소비 모듈 | 실제 경계 | 어댑터가 수집할 evidence | shared fixture가 검증할 계약 |
|---|---|---|---|
| `infra/opentelemetry` | coroutine suspension/dispatcher 전환 | `ContextKey<String>`를 넣은 OTel `Context`를 `withOtelContext`로 설치하고 suspension 전후, caller, worker에서 읽은 값 | parent visibility, terminal 분류, caller/worker 복원 |
| `infra/opentelemetry` | Reactor subscriber context | test-private Reactor key에 capture한 OTel `Context`를 넣고 `deferContextual` callback에서 `Context.wrap(...)`으로 명시적으로 활성화한 값 | subscriber 격리, 실제 signal terminal, cleanup |
| `infra/opentelemetry` | single-thread executor | 제출 시 capture한 `Context.wrap(...)` task, entered/cancelled/finally-completed handshake, 후속 unwrapped probe | submission-time parent, terminal, 동일 worker 복원 |
| `spring-boot/core` | `observeSpringSuspending` + Reactor context | 고정 observation name을 marker로 사용하고 `ObservationRegistry.currentObservation`을 suspension 전후와 caller에서 읽은 값 | coroutine/Reactor visibility, failure/cancellation/deadline 구분, registry cleanup |
| `ktor/observability` | `testApplication` handler/request | W3C `traceparent`로 주입한 synthetic parent trace ID와 handler의 current server-span trace ID, request별 terminal, 후속 probe request | HTTP parent extraction, A/B/probe isolation, request 종료 cleanup |

shared fixture는 프레임워크를 실행하지 않는다. 각 모듈의 테스트가 실제 API를 실행하고 관측 snapshot만 fixture에 전달한다.

Spring adapter 순서는 `TestObservationRegistry` 생성 → 고정 observation marker로 `observeSpringSuspending` 실행 → suspension 전후 current observation 읽기 → 실제 throwable/cancellation/deadline을 terminal로 변환 → caller/registry cleanup probe 순서로 고정한다.

Ktor adapter는 test-local `OpenTelemetrySdk`에 `W3CTraceContextPropagator`를 명시적으로 등록한다. 고정된 test-only `SpanContext` A/B를 text-map propagator로 client request header에 주입하고, `KtorServerTelemetry`가 추출한 server span의 trace ID를 handler에서 읽는다. A/B request는 barrier로 겹쳐 실행하며, 마지막 unparented probe request가 A/B와 다른 고유 marker를 관찰하는지 확인한다. setup → header injection → handler read → terminal capture → request completion → caller/probe request 순서를 바꾸지 않는다. 외부 server와 production exporter는 사용하지 않는다.

### 5.4 전파·종료 행렬

| 경계 | 성공 | 일반 예외 | 취소 | 실제 deadline | 종료 후 복원 | 독립 실행 격리 |
|---|---:|---:|---:|---:|---:|---:|
| Coroutine dispatcher | 필수 | 필수 | `Job.cancel` | `withTimeout` | caller/worker | A/B coroutine |
| Reactor subscriber | 필수 | `onError` | subscription `cancel` | Reactor `timeout` | caller/scheduler worker | A/B subscriber |
| Single-thread executor | 필수 | task exception | running `Future.cancel(true)` | `Future.get(timeout)` 후 cancel | caller/same worker | 동일 worker A/B |
| Spring suspending observation | 필수 | block exception | child coroutine cancel | `withTimeout` | caller/registry | A/B coroutine |
| Ktor request handler | 필수 | handler exception | request coroutine cancel | route block `withTimeout` | caller/probe request | 동시 A/B request |

모든 조합을 프레임워크마다 중복 구현하지 않는다. 저수준 OTel 테스트가 세 비동기 경계의 완전한 terminal matrix를 소유하고, Spring/Ktor는 자신이 제공하는 server-operation bridge에 해당하는 행과 공통 assertion 재사용을 증명한다.

## 6. 결정적 테스트 설계

### 6.1 synthetic marker

테스트 전용 `ContextKey<String>` 또는 observation key에 `parent-A`, `parent-B` 같은 고정 marker를 사용한다. root/absence는 항상 `null`이다. Ktor의 trace/span ID는 W3C 형식에 맞는 고정 test-only 값 A/B를 사용한다. production trace ID, request ID, 사용자 데이터는 사용하지 않는다.

### 6.2 동일 worker 재사용

single-thread executor에서 다음 순서를 지킨다.

1. parent-A를 capture한 wrapped task를 제출한다.
2. `entered` gate로 task가 실제 시작됐음을 확인하고 task 내부에서 parent-A를 관찰한다.
3. 성공·예외 경로는 terminal을 직접 관찰한다. 취소는 running future에 `cancel(true)`를 호출하며, deadline은 `Future.get(timeout)`이 만료된 뒤 같은 취소 절차를 수행한다.
4. task의 `finally`가 `finallyCompleted` gate를 열 때까지 기다린다. 이 gate 전에 cleanup probe를 실행하지 않는다.
5. 같은 executor에 unwrapped probe를 제출해 작업 시작 전 worker marker를 관찰한다. root였다면 `null`이어야 한다.
6. parent-B task를 같은 worker에 실행해 parent-A가 보이지 않음을 확인한다.

executor와 dispatcher는 테스트가 소유한다. teardown은 모든 gate를 해제하고 outstanding task를 취소한 뒤 `shutdown()`과 bounded `awaitTermination`을 수행한다. 첫 대기 실패 시 `shutdownNow()`를 호출하고 다시 bounded 대기한 뒤 termination을 assertion한다.

### 6.3 동시 요청 격리

A/B 작업이 실제로 겹치도록 `CompletableDeferred`, `CountDownLatch`, Reactor synchronization primitive 같은 명시적 barrier를 사용한다. `delay`나 `Thread.sleep`의 시간 추정으로 순서를 만들지 않는다.

각 작업은 barrier 전후로 자신의 marker를 두 번 이상 관찰한다. 종료 후 worker/root probe를 수행한다.

barrier는 failure-aware여야 한다. 한 participant가 barrier 도달 전 실패해도 `finally`에서 peer gate를 완료 또는 취소하고 모든 child task를 join/cancel한다. assertion failure가 다른 participant를 suite timeout까지 가두어서는 안 된다.

### 6.4 bounded completion

모든 barrier, cancellation, deadline, resource termination 대기는 `testing/junit5`의 `DEFAULT_CANCELLATION_CONTRACT_TIMEOUT`과 같은 명시적 5초 상한을 사용한다. Testcontainers용 180초 기본값을 상속하지 않는다. timeout은 hang을 탐지하는 안전장치이며 ordering 수단이 아니다.

semantic deadline은 1초 이하로 두고 5초 hang guard보다 반드시 짧게 설정한다(`semanticDeadline < hangGuard`). coroutine `withTimeout`, Reactor `timeout`, `Future.get(timeout)`, Ktor route `withTimeout`가 먼저 실제 terminal을 발생시키고, adapter가 `DEADLINE_EXCEEDED`와 cleanup 완료를 관찰한 뒤에만 바깥 hang guard를 해제한다. hang guard가 먼저 발화한 실행은 deadline 성공이 아니라 fixture failure다. cancellation과 deadline 모두 원래 예외/signal을 삼키지 않고 cleanup 뒤 caller에게 전파한다.

각 conformance test case 전체에는 JUnit `@Timeout(value = 15, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SAME_THREAD)` aggregate guard를 적용한다. 별도 timeout thread가 context 관측 자체를 바꾸지 않도록 `SAME_THREAD`를 고정한다. 이 상한은 setup, 경계 실행, assertion, 모든 `finally` teardown과 fallback termination을 포함하며 개별 5초 대기가 합산되어 case 전체 상한을 연장할 수 없다. aggregate guard가 발화하면 해당 scenario는 conformance 성공이 아니라 fixture failure다.

### 6.5 resource lifecycle ledger

| Adapter | Test-owned resource | 필수 teardown |
|---|---|---|
| Coroutine | dispatcher/executor, jobs, gates | child cancel/join, executor bounded termination |
| Reactor | subscription/disposable, scheduler, gates | dispose/cancel, scheduler bounded disposal, peer gate release |
| Task executor | futures, single-thread executor, gates | future cancel, finally handshake, `shutdownNow()` fallback, termination assertion |
| Spring | `TestObservationRegistry`, coroutine jobs, optional scheduler | child cancel/join, remaining current observation 없음 확인, scheduler 정리 |
| Ktor | `testApplication`, client requests, local SDK/exporter/provider, gates | request completion/cancel, application 종료, `AutoCloseable` tracing fixture close |

모든 adapter는 success, failure, cancellation, deadline 경로에서 동일한 teardown을 실행한다. test-local SDK/registry만 사용하며 `globalOpenTelemetry`, `GlobalOpenTelemetry.set/resetForTest`, Reactor global hook, automatic context-propagation hook을 호출하지 않는다.

## 7. 실패 모드와 방어

| 실패 모드 | 잘못된 통과 가능성 | 방어 |
|---|---|---|
| 작업 내부 marker만 확인하고 종료 후 복원을 확인하지 않음 | ThreadLocal/OTel scope leak를 놓침 | 모든 terminal 경로 뒤 caller/worker probe 필수 |
| Reactor `Context` 값이 있다는 이유로 OTel `Context.current()`도 전파됐다고 가정 | 실제 bridge 누락을 놓침 | adapter가 실제 consumer API와 OTel current 값을 각각 필요한 위치에서 읽음 |
| `CancellationException`이나 deadline을 일반 오류로 분류 | 취소/timeout telemetry가 오류로 오염됨 | actual terminal과 expected `CANCELLATION`/`DEADLINE_EXCEEDED`를 별도 비교 |
| 전역 Reactor/OTel 상태에 의존 | 테스트 순서와 JVM 전역 상태에 따라 결과 변동 | 명시적 bridge와 test-local SDK만 사용하고 global hook/setter를 금지 |
| A/B 요청을 순차 실행 | request 간 context 혼입을 검출하지 못함 | barrier로 중첩 실행하고 각 요청에서 반복 관측 |
| participant 실패 후 peer barrier를 해제하지 않음 | flaky hang과 장시간 CI stall | failure-aware gate와 child cancel/join을 `finally`에서 실행 |
| 취소 task의 `finally` 전에 worker probe 실행 | 늦게 복원되는 leak를 놓치거나 race 발생 | entered/cancelled/finally-completed handshake 뒤 probe |
| 테스트 executor/SDK/registry 미종료 | 다음 테스트 오염 또는 JVM hang | lifecycle ledger와 bounded termination assertion 적용 |
| raw marker를 assertion/log에 출력 | 실제 값으로 교체될 때 민감정보 노출 위험 | redacted assertion 메시지와 synthetic marker만 허용 |

## 8. 고려한 대안

### 8.1 `infra/opentelemetry`에 OTel 전용 fixture 추가

거절한다. Spring observation과 Ktor adapter가 OTel 테스트 타입에 결합되고 “framework-specific interception은 해당 module에 둔다”는 issue 경계를 흐린다.

### 8.2 별도 `testing/context-propagation` module 추가

거절한다. provider-neutral 값과 assertion만 필요하므로 module registration, catalog, publication, CI surface를 늘릴 근거가 없다.

### 8.3 framework별 테스트만 보강

거절한다. assertion 의미와 terminal 분류가 다시 분기되어 issue의 “same behavioral assertions” acceptance criterion을 만족하지 못한다.

### 8.4 production 공통 context abstraction 추가

거절한다. 이 issue는 conformance test 확장이며 production API 통합은 비목표다. 테스트가 실제 production 결함을 재현할 때만 최소 수정안을 별도로 평가한다.

## 9. 호환성 및 운영 영향

- 새 shared fixture는 additive test-support API이며 기존 public API와 동작을 변경하지 않는다. 공개 enum에는 이후 값을 추가할 수 있으므로 소비자는 exhaustive `when`에 `else`를 두도록 KDoc에 명시한다.
- 공개 타입, 필드, assertion 함수에는 영어 KDoc과 최소 예제를 제공하고 `testing/junit5/README.md`, `testing/junit5/README.ko.md`에 동일한 사용 흐름을 기록한다.
- 새 외부 의존성, 설정 키, exporter, collector, server port가 없다.
- production runtime 동작과 배포 절차는 바뀌지 않는다.
- OpenTelemetry SDK/propagator는 테스트별 로컬 인스턴스로 생성·종료한다. `GlobalOpenTelemetry.set/resetForTest`, Reactor 전역 hook, 그 밖의 JVM 전역 registry를 사용하지 않는다.
- Testcontainers를 사용하지 않는다.
- Ktor는 in-process `testApplication`을 사용하며 외부 HTTP server를 열지 않는다.
- 실제 결함으로 production 수정이 필요해지면 변경 이유, compatibility, rollback을 구현 계획에 추가하고 spec review를 다시 연다.

## 10. Acceptance criteria 추적

| Issue #1051 기준 | 설계 evidence |
|---|---|
| coroutine, Reactor, task-executor parent propagation | 저수준 OTel adapter가 caller/worker 관측값과 사전 marker expectation을 분리해 세 경계의 공통 assertion 실행 |
| cancellation/deadline cleanup | 실제 cancellation은 `CANCELLATION`, 실제 deadline은 `DEADLINE_EXCEEDED`로 구분하고 종료 후 caller/worker/request probe가 기대 terminal 및 복원을 검증 |
| request leakage 없음 | failure-aware barrier 기반 A/B 중첩 실행, 순서가 보존되는 isolation sample 목록, 동일 worker 또는 무부모 request probe |
| deterministic propagation/cancellation/exception/isolation tests | sleep 없는 synchronization, 5초 hang guard와 별도의 실제 deadline, synthetic marker |
| Spring Boot와 Ktor가 같은 assertion 실행 | Spring Observation과 W3C `traceparent` Ktor adapter가 `testing/junit5`의 동일 assertion을 직접 호출 |
| production exporter/collector/HTTP server 불필요 | in-memory registry/SDK, Ktor `testApplication`, 외부 endpoint 없음 |
| framework interception은 각 module에 유지 | shared fixture에 framework 타입 없음; adapter는 소비 module test에 위치 |
| 실패 진단에 민감값이 노출되지 않음 | enum·관계·probe 위치만 포함한 고정 진단과 CR/LF·synthetic canary 비노출 단위 테스트 |

## 11. 구현 및 검증 경계

예상 변경 범위:

- `testing/junit5`: provider-neutral observation/expectation/isolation 타입과 assertion, 자체 unit test, 영문·한글 README 예제
- `infra/opentelemetry`: coroutine/Reactor/executor conformance adapter tests
- `spring-boot/core`: Spring observation conformance adapter tests
- `ktor/observability`: Ktor request conformance adapter tests

기존 build script와 source tree에서 네 Gradle project path 및 필요한 coroutine/Reactor/Spring/Ktor/로컬 OTel SDK 테스트 의존성을 확인했다. 새 의존성은 추가하지 않는다. 구현할 테스트 class와 최소 케이스 수는 다음과 같이 고정한다.

| Module | Test class | 최소 케이스 |
|---|---|---:|
| `testing/junit5` | `io.bluetape4k.junit5.observability.ContextPropagationConformanceTest` | 10 |
| `infra/opentelemetry` | `io.bluetape4k.opentelemetry.context.ContextPropagationConformanceTest` | 15 (5 scenarios × 3 boundaries) |
| `spring-boot/core` | `io.bluetape4k.spring.observability.SpringContextPropagationConformanceTest` | 5 |
| `ktor/observability` | `io.bluetape4k.ktor.observability.KtorContextPropagationConformanceTest` | 5 |

최소 35개 conformance 케이스를 구현한다. 아래 targeted 명령은 변경 중 해당 adapter의 빠른 피드백 또는 실패 재현용이고, 최종 검증 게이트는 네 module 전체 test와 `detekt`다. 같은 변경 상태에서 targeted와 module 전체 실행을 모두 필수 evidence로 중복 요구하지 않는다.

```bash
# Development/debug loop: run only the affected class.
./gradlew :bluetape4k-junit5:test --tests "io.bluetape4k.junit5.observability.ContextPropagationConformanceTest"
./gradlew :bluetape4k-opentelemetry:test --tests "io.bluetape4k.opentelemetry.context.ContextPropagationConformanceTest"
./gradlew :bluetape4k-spring-boot-core:test --tests "io.bluetape4k.spring.observability.SpringContextPropagationConformanceTest"
./gradlew :bluetape4k-ktor-observability:test --tests "io.bluetape4k.ktor.observability.KtorContextPropagationConformanceTest"

# Required final verification: run each module suite once.
./gradlew :bluetape4k-junit5:test
./gradlew :bluetape4k-opentelemetry:test
./gradlew :bluetape4k-spring-boot-core:test
./gradlew :bluetape4k-ktor-observability:test
./gradlew detekt
git diff --check
```

Testcontainers verification은 이 범위에 포함하지 않는다. production 수정이나 새 의존성이 필요해지면 구현을 중단하고 spec review를 다시 연다.

## 12. 외부 계약 근거

- [OpenTelemetry Java API](https://opentelemetry.io/docs/languages/java/api/)
- [OpenTelemetry Context specification](https://opentelemetry.io/docs/specs/otel/context/)
- [OpenTelemetry library instrumentation guidance](https://opentelemetry.io/docs/concepts/instrumentation/libraries/)
- [Project Reactor Context](https://projectreactor.io/docs/core/release/reference/advancedFeatures/context.html)
- [Project Reactor context propagation](https://projectreactor.io/docs/core/release/reference/advanced-contextPropagation.html)
- [kotlinx-coroutines-reactor API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-reactor/)
- [Kotlin cancellation](https://kotlinlang.org/docs/cancellation-and-timeouts.html)

핵심 적용 원칙은 context를 비동기 경계 안에서만 활성화하고 종료 시 이전 상태를 복원하는 것이다. Reactor subscriber context와 thread-local context는 같은 저장소가 아니므로 adapter가 사용하는 bridge를 명시적으로 검증한다.
