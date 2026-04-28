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
 * - block 정상 완료: Span 상태 OK 후 end()
 * - block 일반 예외: Span.recordException + setStatus(ERROR) 후 end() — 예외는 재던짐
 * - block CancellationException: Span 상태 변경 없이 end() 후 예외 재던짐 (구조적 동시성 존중)
 *
 * @param spanName Span 이름
 * @param coroutineContext block 실행 컨텍스트 (기본: 호출자 컨텍스트 유지)
 * @param configure SpanBuilder 추가 설정 DSL (attribute, kind, parent, links 등)
 * @param block Span 객체를 인자로 받는 suspend 람다
 */
public suspend fun <T> Tracer.withSpan(
    spanName: String,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    configure: SpanBuilder.() -> Unit = {},
    block: suspend (Span) -> T,
): T
```

구현 노트:
- 내부적으로 `spanBuilder(spanName).apply(configure).useSpanSuspending(coroutineContext, block)` 으로 위임.
- `useSpanSuspending` 의 기존 예외 처리 규약을 그대로 따른다(이미 CancellationException 안전).
- `coroutineContext` 가 `EmptyCoroutineContext` 인 경우 `withSpanContext` 만 적용하여 dispatcher 전환을 피한다.

#### 1.2 blocking 변형 — `SpanSupport.kt`

```kotlin
/**
 * [Tracer] 로부터 [spanName] Span 을 시작하고, 동기 [block] 의 라이프사이클과 동기화한다.
 *
 * suspend 가 아닌 일반 코드용. 내부적으로 makeCurrent() 스코프를 사용한다.
 */
public inline fun <T> Tracer.withSpan(
    spanName: String,
    configure: SpanBuilder.() -> Unit = {},
    block: (Span) -> T,
): T
```

구현 노트:
- `spanBuilder(spanName).apply(configure).startSpan()` 후 `span.makeCurrent().use { block(span) }`.
- 예외 발생 시 `span.recordException(t)` + `span.setStatus(StatusCode.ERROR, t.message ?: "")` 후 재던짐.
- 정상 완료 시 `span.setStatus(StatusCode.OK)` 후 `span.end()`.
- `inline` 적용으로 람다 호출 비용 제거.

### 2. `Flow<T>.traced` 확장 — `FlowSpanSupport.kt` (신규 파일)

```kotlin
/**
 * 업스트림 [Flow] 전체 수명을 [spanName] Span 으로 감싼 새 Flow 를 반환한다.
 *
 * - 첫 collect 시작 시 Span 시작
 * - Flow 정상 완료 시 OK, end()
 * - Flow 일반 예외 시 ERROR + recordException, end(), 예외 그대로 propagate
 * - downstream cancellation (CancellationException) 시 상태 변경 없이 end()
 *
 * 주의: 이 helper 는 Span 을 "Flow 전체"에 대해 1개 만든다. 각 emit 별 Span 이 필요하면
 * downstream 에서 `tracer.withSpan` 을 직접 사용해야 한다.
 */
public fun <T> Flow<T>.traced(
    tracer: Tracer,
    spanName: String,
    configure: SpanBuilder.() -> Unit = {},
): Flow<T>

/**
 * [Flow] 를 collect 하면서 그 전체 수명을 [spanName] Span 으로 감싼다.
 *
 * `traced(...).collect(action)` 의 편의 버전. Span 의 makeCurrent 컨텍스트는
 * `action` 람다에까지 전파된다.
 */
public suspend fun <T> Flow<T>.tracedCollect(
    tracer: Tracer,
    spanName: String,
    configure: SpanBuilder.() -> Unit = {},
    action: suspend (T) -> Unit,
)
```

#### 구현 스케치

```kotlin
public fun <T> Flow<T>.traced(
    tracer: Tracer,
    spanName: String,
    configure: SpanBuilder.() -> Unit = {},
): Flow<T> = flow {
    val span = tracer.spanBuilder(spanName).apply(configure).startSpan()
    var failure: Throwable? = null
    try {
        withSpanContext(span) {
            this@traced.collect { value -> emit(value) }
        }
        span.setStatus(StatusCode.OK)
    } catch (ce: CancellationException) {
        // 정상 cancellation: Span ERROR 처리하지 않음
        throw ce
    } catch (t: Throwable) {
        failure = t
        span.recordException(t)
        span.setStatus(StatusCode.ERROR, t.message ?: t.javaClass.simpleName)
        throw t
    } finally {
        span.end()
    }
}
```

> 위 스케치는 `flow { ... }` 빌더가 emit 시 동일한 코루틴 컨텍스트에서 동작함을 가정한다. `withSpanContext` 가 `ThreadContextElement` 로 동작하므로 dispatcher 전환에도 Span 이 보존된다. 실제 구현 시 `flowOn` 과의 상호작용을 테스트로 검증한다.

`tracedCollect` 는 단순히 `traced(tracer, spanName, configure).collect(action)` 으로 위임한다.

### 3. WebFlux 편의 helper — `WebfluxTracingSupport.kt` (Optional, LOW)

OpenTelemetry 공식 `opentelemetry-spring-webflux-5.3` instrumentation 의 진입점을 한 줄로 노출.

```kotlin
/**
 * [SpringWebfluxServerTelemetry] 인스턴스를 생성한다.
 * 일반적으로 한 번만 호출하여 Bean 으로 등록한다.
 */
public fun OpenTelemetry.webfluxServerTelemetry(): SpringWebfluxServerTelemetry =
    SpringWebfluxServerTelemetry.create(this)

/**
 * Spring WebFlux 서버용 추적 [WebFilter] 를 생성하고 Reactor hook 을 등록한다.
 * 반환된 [WebFilter] 를 `@Bean` 으로 노출하면 자동 추적이 활성화된다.
 */
public fun OpenTelemetry.createTracingWebFilter(): WebFilter =
    webfluxServerTelemetry().createWebFilterAndRegisterReactorHook()
```

조건:
- `spring-webflux` 가 classpath 에 없으면 import 가 깨지므로, `spring-webflux` 의존성을 `compileOnly` 로 추가한다(현재는 `testRuntimeOnly` 일 가능성 있음 → 확인 필요).
- 클래스 파일이 누락되어도 모듈의 다른 기능을 깨지 않도록, 별도 `webflux/` 패키지로 격리한다.
- `opentelemetry-spring-webflux-5.3` 의존성도 `compileOnly` 로 추가.

복잡도가 추가 의존성 정렬을 요구하면 본 helper 는 follow-up 이슈로 미룬다.

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
| 중첩 호출 | child Span 이 부모 trace id 공유, parent span id 일치 |
| 예외 발생 | status ERROR, recordException 호출(이벤트로 기록), 예외 재던짐 |
| `CancellationException` | Span status UNSET 유지, end() 호출, 예외 재던짐 |
| `configure` 람다로 attribute 추가 | 종료된 Span 의 attribute 검증 |

### 5.2 `Tracer.withSpan` (blocking)

| Case | 검증 |
|---|---|
| 정상 완료 | Span OK, end() 호출 |
| 예외 발생 | Span ERROR, recordException, end() 후 재던짐 |
| 동일 스레드 nested 호출 | parent-child 관계 검증 |

### 5.3 `Flow<T>.traced`

| Case | 검증 |
|---|---|
| Flow 정상 완료 | 1개의 finished Span, status OK |
| `.toList()` 로 collect — emit 3회 | finished Span 1개 (Flow 단위) |
| Upstream 예외 | Span ERROR + recordException, downstream 으로 예외 재던짐 |
| Downstream `take(2)` (정상 종료) | Span OK, end() 호출 |
| Downstream cancellation (`scope.cancel()`) | Span status UNSET, end() 호출, ERROR 안 기록 |
| `flowOn(Dispatchers.IO)` 와 결합 | Span 정상 전파 + 1개 finished |
| `tracedCollect` | 위와 동일 + `action` 안에서 current Span 이 일치 |

### 5.4 WebFlux helper (선택)

| Case | 검증 |
|---|---|
| `OpenTelemetry.createTracingWebFilter()` 호출 시 NPE/ClassNotFound 없음 | Spring + OpenTelemetry-WebFlux 의존성 정상 노출 |
| WebFlux mock 핸들러를 통한 단순 GET | 서버 Span 이 InMemoryExporter 에 1개 기록 |

> WebFlux helper 가 LOW priority 이므로, 의존성 도입 비용이 크면 단위 테스트만(인스턴스 생성/null 아님 검증) 유지한다.

### 5.5 회귀 (Regression)

- 기존 `useSuspending`, `useSpanSuspending`, `withSpanContext`, `withOtelContext`, `CompletableResultCode.await()` 테스트가 그대로 통과해야 한다.
- 변경된 시그니처 없음을 보장한다(API 추가만 수행).

---

## DoD (Definition of Done)

1. **구현 완료**
   - [ ] `Tracer.withSpan(suspend)` — `SpanCoroutineSupport.kt`
   - [ ] `Tracer.withSpan(blocking)` — `SpanSupport.kt`
   - [ ] `Flow<T>.traced` / `Flow<T>.tracedCollect` — `FlowSpanSupport.kt`
   - [ ] (선택) `OpenTelemetry.webfluxServerTelemetry()` / `createTracingWebFilter()` — `WebfluxTracingSupport.kt`
2. **테스트**
   - [ ] §5.1 / §5.2 / §5.3 케이스 100% 커버
   - [ ] (선택) §5.4 WebFlux helper 검증
   - [ ] §5.5 기존 테스트 회귀 zero
   - [ ] 모듈 단위 `./gradlew :bluetape4k-opentelemetry:test` 전수 통과 (passing count + duration 기록)
3. **문서**
   - [ ] 모든 신규 public API 에 Korean KDoc + 예제 포함
   - [ ] `infra/opentelemetry/README.md` (영문) + `README.ko.md` 동시 업데이트 — Architecture / Features / Examples 섹션
   - [ ] 필요 시 모듈 README 의 Mermaid 다이어그램에 Flow 추적/Tracer DSL 흐름 반영
4. **품질 게이트**
   - [ ] `./gradlew :bluetape4k-opentelemetry:detekt` 통과
   - [ ] IntelliJ 포맷터 + `.editorconfig` 적용 (no ktlint)
   - [ ] `ide_diagnostics` / `lsp_diagnostics` import 에러 0
   - [ ] `code-reviewer` 에이전트 리뷰 후 HIGH/CRITICAL 이슈 해소
5. **호환성**
   - [ ] 기존 API 시그니처 변경 없음 (binary + source compatible)
   - [ ] 의존성 추가는 `compileOnly` (transitive 영향 0)
6. **운영**
   - [ ] worktree 안에서 작업 + commit (`feat: opentelemetry coroutines DSL 강화`)
   - [ ] PR 본문에 테스트 결과 / 수정 근거 / 검증 명령 명시
   - [ ] PR 생성 후 `oh-my-claudecode:code-reviewer` 결과 첨부

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
