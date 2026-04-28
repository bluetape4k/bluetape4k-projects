# OpenTelemetry Coroutines 통합 강화 — Design Spec

- **Issue**: #150
- **Branch**: `feat/opentelemetry-coroutines`
- **Worktree**: `.worktrees/feat/opentelemetry-coroutines`
- **Module**: `infra/opentelemetry` (`bluetape4k-opentelemetry`)
- **작성일**: 2026-04-28
- **작성자**: Claude (Opus 4.7)

---

## 배경 / 목적

`infra/opentelemetry` 모듈은 현재 OpenTelemetry SDK 위에 다음과 같은 Kotlin/Coroutines 친화 API를 이미 제공한다.

- `Span`/`SpanBuilder`에 대한 suspend 빌더(`useSuspending`, `useSpanSuspending`)
- `withSpanContext` / `withOtelContext` — 코루틴 컨텍스트 안에서 OpenTelemetry `Context`를 안전하게 전파
- `CompletableResultCode.await()` — Provider flush/shutdown을 코루틴에서 대기

그러나 다음의 사용 시나리오에서 보일러플레이트 또는 부재한 API가 발견되었다.

1. **`Tracer`에서 직접 시작하는 한 번 쓰는 Span 패턴이 길다.**
   - 사용자는 `tracer.spanBuilder(name).useSpanSuspending { ... }` 또는 `tracer.spanBuilder(name).startSpan().useSuspending { ... }` 처럼 두 단계 호출을 반복적으로 작성해야 한다.
   - `Tracer.withSpan { ... }` 한 줄 DSL이 있으면 가독성이 크게 개선된다.
2. **Kotlin `Flow`를 Span으로 감싸는 표준 방법이 없다.**
   - 현재는 사용자가 `flow { ... }.onStart { span = tracer... }.onCompletion { span?.end() }` 형태를 직접 작성해야 한다.
   - `CancellationException`을 ERROR 상태로 잘못 기록하는 사례가 빈번하므로 표준 도우미가 필요하다.
3. **Spring WebFlux 통합이 verbose 하다.**
   - 공식 instrumentation `SpringWebfluxServerTelemetry.create(otel).createWebFilterAndRegisterReactorHook()` 호출이 길고, Spring `WebFilter` 빈으로 노출하는 표준 helper가 없다.

본 spec 은 위 GAP을 해결하는 **추가 API**를 정의한다. 기존 API의 시그니처/동작은 변경하지 않는다(하위 호환 유지).

### 비목표 (Non-goals)

- OpenTelemetry Java agent 자동 instrumentation 대체.
- Reactor `Mono`/`Flux` 전반에 대한 커스텀 instrumentation (공식 reactor instrumentation에 위임).
- Logging bridge / Metrics 신규 API (별도 이슈).

---

## 현존 API 정리

| API | 위치 | 역할 |
|---|---|---|
| `Span.useSuspending(waitTimeout, coroutineContext, block)` | `SpanCoroutineSupport.kt` | 이미 시작된 Span을 suspend 블록 안에서 활성화 + end 보장 |
| `SpanBuilder.useSpanSuspending(coroutineContext, block)` (오버로드 다수) | `SpanCoroutineSupport.kt` | SpanBuilder에서 시작 → suspend 블록 → end 까지 한 번에 |
| `withSpanContext(span, coroutineContext, block)` | `SpanCoroutineSupport.kt` | suspend 블록 내부에서 Span을 current 로 전파 (코어) |
| `withOtelContext(coroutineContext, otelContext, block)` | `OpenTelemetryCoroutineSupport.kt` | 임의의 OpenTelemetry `Context`를 코루틴에서 활성화 |
| `Context.withOtelContext(block)` | `OpenTelemetryCoroutineSupport.kt` | 위의 receiver 변형 |
| `CompletableResultCode.await()` | `OpenTelemetryCoroutineSupport.kt` | flush/shutdown 결과 await |
| `Tracer.withSpan(spanName, ...) { span -> ... }` | `SpanSupport.kt` (blocking) | 동기 스코프용 한 줄 DSL — **존재 여부 확인 필요** |

> 본 작업 직전 일부 API는 이미 존재할 수 있다. 구현 단계 첫 step 에서 `SpanSupport.kt` / `SpanCoroutineSupport.kt` 의 실제 시그니처를 확인하고 중복 시 spec 을 미세 조정한다.

내부적으로 `withSpanContext` 가 모든 Span 전파의 구심점이며, `opentelemetry-extension-kotlin` 의 `Context.asContextElement()` 가 `ThreadContextElement` 로 동작하여 `Dispatchers.IO` 등 디스패처 전환에도 Span 이 보존된다.

---

## 추가 API 범위

### 1. `Tracer.withSpan` DSL

#### 1.1 suspend 변형 — `SpanCoroutineSupport.kt`

```kotlin
/**
 * [Tracer] 로부터 [spanName] Span 을 시작하고, suspend [block] 의 라이프사이클과 동기화한다.
 *
 * ## 동작 계약
 * - block 정상 완료: Span setStatus(OK) + end()
 * - block 일반 예외: recordException + setStatus(ERROR) + end() → 예외 재던짐
 * - block CancellationException: **Span 상태 UNSET 유지** + end() → 예외 재던짐 (구조적 동시성 존중)
 *   ⚠️ 계약: CancellationException 은 반드시 일반 Throwable 보다 먼저 catch 해야 한다.
 *   이 순서를 바꾸면 Cancellation 이 ERROR 로 잘못 기록된다.
 *
 * ## 파라미터 순서 (H2 반영)
 * `configure` 가 `coroutineContext` 앞에 위치한다.
 * `coroutineContext` 를 바꾸지 않는 일반 호출에서 `configure` 를 trailing lambda 로 자연스럽게 사용.
 *
 * ## 보안 경고
 * ⚠️ `configure` 람다에서 PII, 인증 토큰, 비밀번호, Authorization 헤더 등 민감 정보를
 * attribute 로 추가하지 마십시오. Span attribute 는 OTLP exporter 를 통해 외부 trace
 * backend 로 전송되며 회수 불가합니다.
 *
 * ⚠️ `configure` 람다 안에서 `.startSpan()` 또는 `.build()` 를 직접 호출하지 마십시오.
 * Span 시작은 `withSpan` 이 처리합니다. 직접 호출 시 이중 Span 이 생성됩니다.
 *
 * ⚠️ `configure` 는 blocking 컨텍스트에서 동기적으로 실행됩니다.
 * 람다 안에서 `async`, `launch` 등 비동기 연산을 수행하지 마십시오.
 *
 * ⚠️ 이 함수는 suspend 전용입니다. 코루틴 외부 (일반 blocking 코드) 에서는
 * blocking 변형 `Tracer.withSpan(spanName, configure, block)` 을 사용하십시오.
 * blocking 환경에서 이 suspend 버전을 호출하면 ThreadLocal 기반 OTel Context 가
 * Dispatcher 전환 시 유실될 수 있습니다.
 *
 * @param spanName Span 이름
 * @param configure SpanBuilder 추가 설정 DSL (attribute, kind, parent, links 등). non-throwing 이어야 함
 * @param coroutineContext block 실행 추가 컨텍스트 (기본: 호출자 컨텍스트 유지)
 * @param block Span 객체를 인자로 받는 suspend 람다
 */
public suspend fun <T> Tracer.withSpan(
    spanName: String,
    configure: SpanBuilder.() -> Unit = {},
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend (Span) -> T,
): T
```

구현 노트:
- 내부적으로 `spanBuilder(spanName).apply(configure).useSpanSuspending(coroutineContext, block)` 으로 위임.
- `useSpanSuspending` 의 기존 예외 처리 규약을 그대로 따른다(이미 CancellationException 안전).
- `EmptyCoroutineContext` 인 경우에도 `withContext` 는 호출되나 dispatcher 는 호출자 그대로 유지된다(`getOrCurrent()` 동작).

#### 1.2 blocking 변형 — `SpanSupport.kt`

```kotlin
/**
 * [Tracer] 로부터 [spanName] Span 을 시작하고, 동기 [block] 의 라이프사이클과 동기화한다.
 *
 * ## 동작 계약
 * - block 정상 완료: setStatus(OK) + end()
 * - block 예외: recordException + setStatus(ERROR, message) + end() → 예외 재던짐
 *
 * ## 보안 경고
 * ⚠️ `configure` 람다에서 PII, 인증 토큰 등 민감 정보를 attribute 로 추가하지 마십시오.
 * ⚠️ `configure` 안에서 `.startSpan()` 을 직접 호출하지 마십시오 (이중 Span 생성).
 *
 * ## 코루틴 사용 주의
 * ⚠️ 이 함수는 `makeCurrent()` (ThreadLocal 기반) 를 사용합니다.
 * `Dispatchers.IO` 등으로 스레드가 전환되는 코루틴 컨텍스트에서 호출하면 OTel Context 가
 * 유실됩니다. suspend 코드에서는 반드시 suspend 변형을 사용하십시오.
 *
 * @param spanName Span 이름
 * @param configure SpanBuilder 추가 설정 DSL. non-throwing 이어야 함
 * @param block Span 객체를 인자로 받는 람다
 */
public inline fun <T> Tracer.withSpan(
    spanName: String,
    configure: SpanBuilder.() -> Unit = {},
    block: (Span) -> T,
): T
```

구현 노트:
- `spanBuilder(spanName).apply(configure).startSpan()` 후 `span.makeCurrent().use { ... }` 패턴.
- 예외 발생 시 `span.recordException(t)` + `span.setStatus(StatusCode.ERROR, t.message ?: "unspecified error")` 후 재던짐.
  - fallback 을 `t.javaClass.simpleName` 이 아닌 `"unspecified error"` 로 한다 — 내부 클래스명 외부 노출 방지(H2, M5).
- 정상 완료 시 `span.setStatus(StatusCode.OK)` 후 `span.end()`.
- `inline` 적용으로 람다 호출 비용 제거.

### 2. `Flow<T>.traced` 확장 — `FlowSpanSupport.kt` (신규 파일)

#### 설계 원칙 (H1 + H2 반영)

- **1 collect = 1 Span**: `traced()` 는 한 번의 collect 전체를 단일 Span 으로 감싼다.
  각 emit 별 Span 이 필요하면 `onEach { tracer.withSpan(...) { ... } }` 를 사용한다.
- **`channelFlow` 사용 필수**: `flow {}` 빌더 안에서 `withContext` 후 `emit()` 하면
  Flow 불변식 위반(`IllegalStateException`) 이 발생한다. `channelFlow {}` 는 cross-context `send()` 를 허용한다.
- **`withSpanContext` 직접 사용 금지**: `withSpanContext` 는 내부 `finally` 에서 이미 `endSafely()` 를 호출한다.
  `Flow.traced` 에서 재사용하면 `span.end()` 이중 호출 + 이미 종료된 Span 에 `setStatus()` 호출이 발생한다.
  따라서 OTel Context 전파는 `Context.current().with(span).asContextElement()` 만 직접 사용한다.

```kotlin
/**
 * 업스트림 [Flow] 전체 수명을 단일 [spanName] Span 으로 감싼 새 Flow 를 반환한다.
 *
 * ## 동작 계약
 * - collect 시작 시 Span 시작
 * - Flow 정상 완료 시 setStatus(OK) + end()
 * - Flow 일반 예외 시 recordException + setStatus(ERROR) + end() → 예외 그대로 propagate
 * - CancellationException(downstream cancel) 시 **Span 상태 UNSET 유지** + end()
 *   ⚠️ 계약: CancellationException 은 반드시 일반 Throwable 보다 먼저 catch 해야 한다.
 *
 * ## 설계 의도
 * - 이 함수는 **1 collect = 1 Span** 이다. 아이템별 Span 이 필요하면 `onEach` 를 사용하라.
 * - 반환된 Flow 를 여러 번 collect 하면 각 collect 마다 별도 Span 이 생성된다 (cold Flow 자연 동작).
 *
 * ## 보안 경고
 * ⚠️ `configure` 람다에서 PII, 인증 토큰, 비밀번호 등 민감 정보를 attribute 로 추가하지 마십시오.
 * ⚠️ `configure` 안에서 `.startSpan()` 을 직접 호출하지 마십시오 (이중 Span 생성).
 * ⚠️ `configure` 는 non-throwing blocking 람다여야 합니다.
 *
 * @param tracer Span 을 생성할 [Tracer]
 * @param spanName Span 이름
 * @param configure SpanBuilder 추가 설정 DSL
 */
public fun <T> Flow<T>.traced(
    tracer: Tracer,
    spanName: String,
    configure: SpanBuilder.() -> Unit = {},
): Flow<T>

/**
 * [Flow] 를 collect 하면서 그 전체 수명을 단일 [spanName] Span 으로 감싼다.
 *
 * `traced(tracer, spanName, configure).collect(action)` 의 편의 버전.
 *
 * ## traced() vs tracedCollect() 선택 기준
 * | 상황 | 권장 |
 * |------|------|
 * | 다운스트림 연산자(map/filter/take 등)와 결합 | `traced()` |
 * | 단일 collect + action 안에서 current Span 사용 | `tracedCollect()` |
 * | 다중 terminal 연산 (collect 두 번) | `traced()` — collect 마다 새 Span |
 *
 * ⚠️ `configure` 람다에서 PII, 인증 토큰 등 민감 정보를 attribute 로 추가하지 마십시오.
 */
public suspend fun <T> Flow<T>.tracedCollect(
    tracer: Tracer,
    spanName: String,
    configure: SpanBuilder.() -> Unit = {},
    action: suspend (T) -> Unit,
)
```

#### 구현 스케치 (H1 + H2 반영)

```kotlin
public fun <T> Flow<T>.traced(
    tracer: Tracer,
    spanName: String,
    configure: SpanBuilder.() -> Unit = {},
): Flow<T> = channelFlow {   // ← flow{} 아님: cross-context emit 허용
    val span = tracer.spanBuilder(spanName).apply(configure).startSpan()
    // withSpanContext 재사용 금지: 내부에서 endSafely() 를 이미 호출하므로
    // OTel Context 전파만 직접 수행
    val otelContext = span.storeInContext(Context.current())
    try {
        withContext(otelContext.asContextElement()) {
            this@traced.collect { value -> send(value) }   // channelFlow: send() 사용
        }
        span.setStatus(StatusCode.OK)
    } catch (ce: CancellationException) {
        // ⚠️ 계약: 반드시 Throwable 보다 먼저 — UNSET 유지, 구조적 동시성 보존
        throw ce
    } catch (t: Throwable) {
        span.recordException(t)
        span.setStatus(StatusCode.ERROR, t.message ?: "unspecified error")
        throw t
    } finally {
        span.end()   // 모든 경로(정상/예외/cancel)에서 exactly-once
    }
}

// tracedCollect 는 traced() 에 위임
public suspend fun <T> Flow<T>.tracedCollect(
    tracer: Tracer,
    spanName: String,
    configure: SpanBuilder.() -> Unit = {},
    action: suspend (T) -> Unit,
): Unit = traced(tracer, spanName, configure).collect(action)
```

### 3. WebFlux 편의 helper — `WebfluxTracingSupport.kt` (이번 PR 포함)

OpenTelemetry 공식 `opentelemetry-spring-webflux-5.3` instrumentation 의 진입점을 한 줄로 노출한다.

#### 운영 제약 (H7 반영)

> ⚠️ `createTracingWebFilter()` 는 내부적으로 Reactor global hook(`Hooks.onEachOperator`) 을 등록한다.
> - **반드시 Spring ApplicationContext 초기화 시(`@Configuration` `@Bean`) 한 번만 호출**해야 한다.
> - 앱 시작 후 lazy 로 호출하면 이미 생성된 Reactor pipeline 에는 hook 이 적용되지 않아 silent failure 가 발생한다.
> - 테스트에서 ApplicationContext 를 재생성하면 hook 이 중복 등록될 수 있다. 테스트 격리 시 `Hooks.resetOnEachOperator(key)` 호출 권장.

#### 보안 경고 (H5 반영)

> ⚠️ 공식 Spring WebFlux instrumentation 은 기본 설정에서 일부 HTTP 헤더를 Span attribute 로 기록할 수 있다.
> `Authorization`, `Cookie`, `Set-Cookie` 등 민감 헤더가 trace backend 로 유출되지 않도록
> `OTEL_INSTRUMENTATION_HTTP_CAPTURE_HEADERS_SERVER_REQUEST` 환경변수로 화이트리스트를 명시적으로 제한해야 한다.
> 기본값에서 민감 헤더가 캡처되지 않음을 단위 테스트로 검증한다.

```kotlin
/**
 * [SpringWebfluxServerTelemetry] 인스턴스를 생성한다.
 * 일반적으로 한 번만 호출하여 Spring `@Bean` 으로 등록한다.
 *
 * ⚠️ `createWebFilterAndRegisterReactorHook()` 는 JVM 전역 Reactor hook 을 등록한다.
 * ApplicationContext 초기화 시점에 한 번만 호출할 것.
 */
public fun OpenTelemetry.webfluxServerTelemetry(): SpringWebfluxServerTelemetry =
    SpringWebfluxServerTelemetry.create(this)

/**
 * Spring WebFlux 서버용 추적 [WebFilter] 를 생성하고 Reactor hook 을 등록한다.
 * 반환된 [WebFilter] 를 `@Bean` 으로 노출하면 자동 추적이 활성화된다.
 *
 * ⚠️ 반드시 Spring Bean 생명주기 내에서 1회만 호출할 것.
 * ⚠️ 기본 설정에서 캡처되는 HTTP 헤더를 확인하고 민감 헤더를 환경변수로 제한할 것.
 */
public fun OpenTelemetry.createTracingWebFilter(): WebFilter =
    webfluxServerTelemetry().createWebFilterAndRegisterReactorHook()
```

의존성:
- `spring-webflux` → `compileOnly` 로 추가 (현재 `testRuntimeOnly` → 승격)
- `opentelemetry-spring-webflux-5.3` → `compileOnly` 추가
- `webflux/` 패키지로 격리하여 classpath 에 없을 때도 다른 기능을 깨지 않도록 한다.

---

## 제외 항목 및 이유

| 후보 API | 제외 이유 |
|---|---|
| 자체 `TracingWebFilter` 구현 | `SpringWebfluxServerTelemetry.createWebFilterAndRegisterReactorHook()` 가 Reactor hook 등록까지 공식 지원. 재구현 시 유지보수 부담 + 비표준화 위험. |
| 자체 `SpanContextElement: ThreadContextElement` | `opentelemetry-extension-kotlin` 의 `Context.asContextElement()` 가 동일 역할을 표준으로 제공. 중복 구현 비추천. |
| 자체 `OpenTelemetryCoroutineContextRestorer` | 위 extension 모듈이 이미 `ThreadContextElement.updateThreadContext` / `restoreThreadContext` 로 처리. |
| `Mono<T>.traced(...)` / `Flux<T>.traced(...)` 확장 | Reactor 는 공식 `opentelemetry-reactor` instrumentation 영역. WebFlux helper 만으로 충분. |
| Span 자동 attribute (HTTP method 등) 헬퍼 | 도메인별 instrumentation 의 책임. 이 모듈은 코루틴-Span 결합에 집중. |

---

## 기술 스택

- **언어/런타임**: Kotlin 2.3, JVM 21
- **OpenTelemetry**: `opentelemetry-api`, `opentelemetry-sdk`, `opentelemetry-extension-kotlin` (기존 의존성 그대로)
- **Coroutines**: `kotlinx-coroutines-core` (Flow API 포함)
- **테스트**:
  - `opentelemetry-sdk-testing` (`InMemorySpanExporter`)
  - JUnit 5 + MockK + Kluent
  - `kotlinx-coroutines-test` (`runTest`, `TestDispatcher`)
- **WebFlux helper (선택)**:
  - `spring-webflux` — `compileOnly` 승격 검토
  - `opentelemetry-spring-webflux-5.3` — `compileOnly`
- **빌드 설정**: detekt, IntelliJ formatter, .editorconfig (no ktlint)

### 모듈 의존성 변경

```kotlin
// infra/opentelemetry/build.gradle.kts
dependencies {
    api(Libs.opentelemetry_api)
    api(Libs.opentelemetry_sdk)
    api(Libs.opentelemetry_extension_kotlin)
    api(Libs.kotlinx_coroutines_core)

    // 신규 (선택): WebFlux helper 활성화 시
    compileOnly(Libs.spring_webflux)
    compileOnly(Libs.opentelemetry_spring_webflux)

    testImplementation(Libs.opentelemetry_sdk_testing)
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.spring_webflux) // helper 테스트용
}
```

---

## 테스트 전략

테스트는 `InMemorySpanExporter` 를 SDK 에 등록한 뒤 finish 된 Span 을 검증한다. 공통 fixture 는 기존 모듈의 패턴을 재사용한다.

### 5.1 `Tracer.withSpan` (suspend)

| Case | 검증 |
|---|---|
| 정상 완료 | Span name 일치, status OK, parent context 가 외부 current 와 동일 |
| Dispatcher 경계 (`withContext(Dispatchers.IO)`) 내부에서 호출 | Span 이 IO 디스패처에서도 current 로 유지, finished span 1개 |
| 중첩 호출 (Dispatchers 경계 포함) | child traceId == parent traceId, child parentSpanId == parent spanId |
| 예외 발생 | status ERROR, recordException 이벤트 기록, 예외 재던짐 |
| `CancellationException` | Span status UNSET 유지, end() 호출, 예외 재던짐 |
| `configure` 람다로 attribute 추가 | 종료된 Span 의 attribute 검증 |
| `configure` 람다에서 `.startSpan()` 직접 호출 | 테스트에서 이중 Span 생성 확인 → 문서화 (negative test) |

### 5.2 `Tracer.withSpan` (blocking)

| Case | 검증 |
|---|---|
| 정상 완료 | Span OK, end() 호출 |
| 예외 발생 | Span ERROR, recordException, `message ?: "unspecified error"` 검증, end() 후 재던짐 |
| 동일 스레드 nested 호출 | parent-child 관계 검증 |

### 5.3 `Flow<T>.traced`

| Case | 검증 |
|---|---|
| Flow 정상 완료 | finished Span **1개**, status OK |
| emit 3회 `.toList()` | finished Span **1개** (아이템별 아님) |
| Upstream 예외 | Span ERROR + recordException, 예외 재던짐 |
| Downstream `take(2)` (정상 종료) | Span OK, end() 호출 |
| Downstream cancellation (`scope.cancel()`) | Span status **UNSET**, end() 호출, ERROR 미기록 |
| `flowOn(Dispatchers.IO)` 와 결합 | Span 정상 전파, finished Span 1개 |
| `tracedCollect` — action 안에서 parent Span current 검증 | `Span.current()` == traced Span |
| 민감 attribute negative test | 예: `Authorization: Bearer xxx` 가 Span attribute 에 없음 검증 |

### 5.4 WebFlux helper

| Case | 검증 |
|---|---|
| `OpenTelemetry.createTracingWebFilter()` — NPE/ClassNotFound 없음 | 의존성 정상 노출 |
| WebFlux mock 핸들러 GET | 서버 Span 1개 기록, HTTP method/path attribute 검증 |
| `Authorization` 헤더가 Span attribute 에 미포함 | 민감 헤더 필터링 negative test |

### 5.5 회귀 (Regression)

- 기존 `useSuspending`, `useSpanSuspending`, `withSpanContext`, `withOtelContext`, `CompletableResultCode.await()` 테스트가 그대로 통과해야 한다.
- 변경된 시그니처 없음을 보장한다(API 추가만 수행).

---

## DoD (Definition of Done)

1. **구현 완료**
   - [ ] `Tracer.withSpan(spanName, configure, coroutineContext, block)` suspend — `SpanCoroutineSupport.kt`
   - [ ] `Tracer.withSpan(spanName, configure, block)` blocking — `SpanSupport.kt`
   - [ ] `Flow<T>.traced` / `Flow<T>.tracedCollect` — `FlowSpanSupport.kt` (`channelFlow` 기반)
   - [ ] `OpenTelemetry.webfluxServerTelemetry()` / `createTracingWebFilter()` — `webflux/WebfluxTracingSupport.kt`
2. **테스트**
   - [ ] §5.1 케이스 전수 — Dispatcher 경계 + 중첩 Span + negative(configure footgun)
   - [ ] §5.2 케이스 전수 — `"unspecified error"` fallback 검증 포함
   - [ ] §5.3 케이스 전수 — **1 collect = 1 Span** + 민감 attribute negative test
   - [ ] §5.4 WebFlux helper — 민감 헤더 미포함 negative test 포함
   - [ ] §5.5 기존 테스트 회귀 zero
   - [ ] `./gradlew :bluetape4k-opentelemetry:test` 전수 통과
3. **문서**
   - [ ] 모든 신규 public API 에 Korean KDoc (보안 경고 + CancellationException 계약 + configure footgun 금지 포함)
   - [ ] `README.md` (영문) + `README.ko.md` — Architecture → UML → Features → Examples 순서
   - [ ] Mermaid 다이어그램: `withSpan` DSL 흐름 + `Flow.traced` 생명주기
4. **품질 게이트**
   - [ ] `./gradlew :bluetape4k-opentelemetry:detekt` 통과
   - [ ] IntelliJ 포맷터 + `.editorconfig` 적용 (no ktlint)
   - [ ] `ide_diagnostics` import 에러 0
   - [ ] `code-reviewer` 에이전트 리뷰 후 HIGH/CRITICAL 이슈 해소
5. **호환성**
   - [ ] 기존 API 시그니처 변경 없음 (binary + source compatible)
   - [ ] 신규 의존성은 `compileOnly` (transitive 영향 0)
6. **운영 / 보안 가이드 (README 포함)**
   - [ ] OTLP exporter TLS 설정 가이드: 운영 환경에서 `https://` 엔드포인트 및 `OTEL_EXPORTER_OTLP_CERTIFICATE` 검증 권장
   - [ ] `SdkTracerProvider.shutdown()` timeout 권장값(5~10초) 예제
   - [ ] WebFlux `createTracingWebFilter()` — 1회 호출 제약 + `Hooks.resetOnEachOperator()` 테스트 격리 가이드
   - [ ] 민감 HTTP 헤더 캡처 제한: `OTEL_INSTRUMENTATION_HTTP_CAPTURE_HEADERS_SERVER_REQUEST` 환경변수 예시
   - [ ] worktree 내 commit + PR 생성 후 `oh-my-claudecode:code-reviewer` 결과 첨부

---

## 작업 순서 (참고용 outline)

1. 현행 `SpanSupport.kt` / `SpanCoroutineSupport.kt` 시그니처 정확 확인 → 중복/충돌 조정
2. `Tracer.withSpan(suspend)` 구현 + 테스트
3. `Tracer.withSpan(blocking)` 구현 + 테스트
4. `FlowSpanSupport.kt` 신규 + 테스트
5. (선택) WebFlux helper + 의존성 승격
6. README 양언어 + KDoc
7. detekt / 테스트 / code review
8. PR 생성

---

## 참고 자료

- [OpenTelemetry Java — `opentelemetry-extension-kotlin`](https://github.com/open-telemetry/opentelemetry-java/tree/main/extensions/kotlin)
- [`SpringWebfluxServerTelemetry`](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/spring/spring-webflux/spring-webflux-5.3/library)
- 기존 모듈 코드: `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/`
