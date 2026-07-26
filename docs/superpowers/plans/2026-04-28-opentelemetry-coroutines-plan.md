# OpenTelemetry Coroutines 통합 강화 — Implementation Plan

- **Spec**: `docs/superpowers/specs/2026-04-28-opentelemetry-coroutines-design.md`
- **Issue**: #150
- **Branch**: `feat/opentelemetry-coroutines`
- **Worktree**: `.worktrees/feat/opentelemetry-coroutines`
- **Module**: `infra/opentelemetry` (`bluetape4k-opentelemetry`)
- **작성일**: 2026-04-28
- **작성자**: Claude (Opus 4.7)

---

## 개요

본 plan은 spec (2026-04-28)에서 정의한 4가지 추가 API의 구현 단계를 task 단위로 분해한다. 모든 task는 worktree 안에서 수행하며, Plan Task는 선택 없이 전수 완료 후 PR을 생성한다.

- 기존 API 시그니처/동작은 변경하지 않는다 (binary + source compatible).
- 신규 의존성은 `compileOnly` 로 추가하여 transitive 영향 0 을 보장한다.
- CancellationException 계약, configure footgun, 민감 attribute 보안 가이드를 KDoc + 테스트로 강제한다.

---

## Task List

### Task 1 — 기존 시그니처 정찰 및 충돌 검사

- **complexity**: low
- **목표**: spec §"작업 순서" 1단계. 추가 대상 파일의 현존 시그니처를 정확히 파악해 중복/충돌 여부를 결정한다.
- **수행 내용**:
    - `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/trace/SpanSupport.kt` Read.
    - `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/SpanCoroutineSupport.kt` Read.
    - `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/ContextCoroutineSupport.kt` Read — `withOtelContext` 등 OTel 컨텍스트 전파 헬퍼 위치.
    - `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/CompletableResultCodeSupport.kt` Read — `await` + Span 완료 헬퍼 위치.
    - `TracerSupport.kt` Read.
    - `Tracer.withSpan` 이름의 함수가 이미 존재하는지 `ide_find_references` 또는 `ast-grep` 로 확인.
    - 현존 `useSuspending`, `useSpanSuspending`, `withSpanContext` 시그니처를 plan 본문에 기록 (paramName + default + return).
- **DoD**:
    - [ ] 두 파일의 public API 표 작성 완료
    - [ ] 충돌 0 또는 충돌이 있다면 spec 미세 조정안 명시
- **산출물**: 본 plan 의 `## 정찰 결과` 섹션에 추가 (Task 1 완료 후).

---

### Task 1.5 — `SpanSupport.recordFailure` 보안 수정 (Option A)

- **complexity**: medium
- **위치**: `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/trace/SpanSupport.kt`
-

**목표**: 기존 `recordFailure` 함수의 fallback 메시지가 `error::class.java.simpleName` 을 사용하여 내부 클래스명을 OTLP 백엔드에 노출하는 보안 결함을 수정한다.
- **수정 내용**:
  ```kotlin
  // 수정 전
  private const val UNSPECIFIED_ERROR = "unspecified error"

  fun Span.recordFailure(error: Throwable, message: String? = null) {
      recordException(error)
      setStatus(StatusCode.ERROR, message ?: error.message ?: error::class.java.simpleName)
  //                                                           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 제거
  }

  // 수정 후
  fun Span.recordFailure(error: Throwable, message: String? = null) {
      recordException(error)
      setStatus(StatusCode.ERROR, message ?: error.message ?: UNSPECIFIED_ERROR)
  }
  ```
    - `private const val UNSPECIFIED_ERROR = "unspecified error"` 상수 파일 최상단에 정의 (Task 3, 4 에서도 재사용).
    - 기존 API 시그니처 유지 — binary + source compatible.
- **회귀 테스트**:
    - `src/test/kotlin/io/bluetape4k/opentelemetry/trace/SpanSupportTest.kt` (신규 또는 기존 확장)
    - `throw RuntimeException(null)` → `recordFailure()` 호출 → Span status description 이 `"unspecified error"` 임을 검증 (`.javaClass.simpleName` 누출 안 됨).
- **DoD**:
    - [ ] `SpanSupport.kt` 에 `UNSPECIFIED_ERROR` 상수 추가
    - [ ] `recordFailure` fallback 수정 (`error::class.java.simpleName` 제거)
    - [ ] 회귀 테스트 추가 + `null` message 케이스 통과
    - [ ] `ide_diagnostics` 0 error
    - [ ] 컴파일 통과

---

### Task 2 — `Tracer.withSpan` (suspend) 구현

- **complexity**: high
- **위치**: `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/SpanCoroutineSupport.kt`
- **목표**: spec §1.1. suspend 한 줄 DSL.
- **시그니처**:
  ```kotlin
  public suspend fun <T> Tracer.withSpan(
      spanName: String,
      configure: SpanBuilder.() -> Unit = {},
      coroutineContext: CoroutineContext = EmptyCoroutineContext,
      block: suspend (Span) -> T,
  ): T
  ```
- **구현 노트**:
    - 진입 즉시 `requireNotBlank(spanName) { "spanName must not be blank" }` 검사.
    - 내부적으로 `spanBuilder(spanName).apply(configure).useSpanSuspending(coroutineContext, block)` 위임.
    - 파라미터 순서: `(spanName, configure, coroutineContext, block)` — configure 가 coroutineContext 앞.
    - `suspend fun` 이므로 `inline` 불가; `block` 파라미터는 `crossinline` 불가 (`useSpanSuspending` 내부에서 다른 suspend 컨텍스트로 호출될 수 있음). `noinline`으로 선언하거나 일반 suspend 람다로 유지.
    - `configure` 람다 안에서 `.startSpan()` 직접 호출은 이중 Span 생성 — KDoc 경고만 (코드로 강제 불가).
    - CancellationException 처리는 `useSpanSuspending` 의 기존 규약에 위임.
- **DoD**:
    - [ ] 파일에 함수 추가, KDoc (Korean) 포함 (보안 경고 + cancellation 계약 + configure footgun)
    - [ ] `requireNotBlank(spanName)` 진입 검사 포함
    - [ ] `ide_diagnostics` 0 error / 0 import warning
    - [ ] 컴파일 통과: `./gradlew :bluetape4k-opentelemetry:compileKotlin`

---

### Task 3 — `Tracer.withSpan` (blocking) 구현

- **complexity**: high
- **위치**: `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/trace/SpanSupport.kt`
- **목표**: spec §1.2. blocking 한 줄 DSL.
- **시그니처**:
  ```kotlin
  public inline fun <T> Tracer.withSpan(
      spanName: String,
      configure: SpanBuilder.() -> Unit = {},
      block: (Span) -> T,
  ): T
  ```
- **구현 노트**:
    - 진입 즉시 `requireNotBlank(spanName) { "spanName must not be blank" }` 검사.
    - `val span = spanBuilder(spanName).apply(configure).startSpan()`
    - `span.makeCurrent().use { block(span) }` 패턴.
    - 정상 종료: `span.setStatus(StatusCode.OK)` → `span.end()` (finally 안에서 보장).
    - 예외: `span.recordException(t)` + `span.setStatus(StatusCode.ERROR, t.message ?: UNSPECIFIED_ERROR)` → 재던짐.
        - **`UNSPECIFIED_ERROR` 상수
          사용** — `SpanSupport.kt` 파일 최상단에 `private const val UNSPECIFIED_ERROR = "unspecified error"` 정의 (Task 1.5에서 추가). `t.javaClass.simpleName` 절대 금지 (내부 클래스명 외부 노출 방지).
    - `inline` 적용 — 람다 호출 비용 제거. `inline` 함수 내부에서 비-public-API 호출이 필요하면 `@PublishedApi internal` 헬퍼로 분리.
- **DoD**:
    - [ ] 함수 추가, KDoc (Korean) 포함 (보안 경고 + 코루틴 사용 주의 — Dispatcher 전환 시 ThreadLocal 유실)
    - [ ] `requireNotBlank(spanName)` 진입 검사 포함
    - [ ] `UNSPECIFIED_ERROR` 상수 사용 (리터럴 `"unspecified error"` 직접 기재 금지)
    - [ ] `ide_diagnostics` 0 error
    - [ ] 컴파일 통과

---

### Task 4 — `FlowSpanSupport.kt` 신규 생성

- **complexity**: high
- **위치**: `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/FlowSpanSupport.kt` (신규)
- **목표**: spec §2. `Flow<T>.traced` + `Flow<T>.tracedCollect`.
- **핵심 설계 원칙 (절대 위반 금지)**:
    1. **`channelFlow {}` 사용
       필수** — `flow {}` 안에서 `withContext` 후 `emit()` 은 Flow invariant 위반 (`IllegalStateException`).
    2. **`withSpanContext` 재사용
       금지** — 내부 `finally` 에서 `endSafely()` 를 이미 호출 → 이중 `end()` 발생. OTel Context 전파는 `Context.current().with(span).asContextElement()` 직접 사용.
    3. **catch 순서 계약**: `CancellationException` 을 일반 `Throwable` 보다 먼저 catch. UNSET 유지, 구조적 동시성 보존.
    4. **1 collect = 1 Span** — emit 횟수와 무관하게 collect 한 번 = Span 한 개.
- **시그니처**:
  ```kotlin
  public fun <T> Flow<T>.traced(
      tracer: Tracer,
      spanName: String,
      configure: SpanBuilder.() -> Unit = {},
  ): Flow<T>

  public suspend fun <T> Flow<T>.tracedCollect(
      tracer: Tracer,
      spanName: String,
      configure: SpanBuilder.() -> Unit = {},
      action: suspend (T) -> Unit,
  )
  ```
- **진입 검사**: `traced`/`tracedCollect` 모두 `requireNotBlank(spanName)` 호출.
- **`UNSPECIFIED_ERROR`
  상수**: `SpanSupport.kt` 에 정의된 상수를 재사용하거나, 이 파일에 `internal const val` 로 별도 정의 (`"unspecified error"` 리터럴 직접 기재 금지).
- **구현 골격** (spec §2 구현 스케치 그대로):
  ```kotlin
  channelFlow {
      val span = tracer.spanBuilder(spanName).apply(configure).startSpan()
      val otelContext = span.storeInContext(Context.current())
      try {
          withContext(otelContext.asContextElement()) {
              this@traced.collect { value -> send(value) }
          }
          span.setStatus(StatusCode.OK)
      } catch (ce: CancellationException) {
          throw ce          // UNSET 유지, ERROR 미기록
      } catch (t: Throwable) {
          span.recordException(t)
          span.setStatus(StatusCode.ERROR, t.message ?: "unspecified error")
          throw t
      } finally {
          span.end()        // 모든 경로 exactly-once
      }
  }
  ```
  `tracedCollect` 는 `traced(...).collect(action)` 으로 위임.
- **DoD**:
    - [ ] 파일 생성, KDoc (Korean) — 보안 경고 + 1-collect-1-Span 의도 + traced/tracedCollect 선택 기준 표
    - [ ] import: `kotlinx.coroutines.flow.Flow`, `channelFlow`, `kotlinx.coroutines.withContext`, `io.opentelemetry.context.Context`, `io.opentelemetry.extension.kotlin.asContextElement`, OpenTelemetry API
    - [ ] `ide_diagnostics` 0 error
    - [ ] 컴파일 통과

---

### Task 5 — WebFlux helper + 의존성 승격

- **complexity**: medium
- **위치**:
    - 신규: `infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/webflux/WebfluxTracingSupport.kt`
    - 수정: `infra/opentelemetry/build.gradle.kts`
- **목표**: spec §3. `OpenTelemetry.webfluxServerTelemetry()` + `createTracingWebFilter()`.
- **build.gradle.kts 변경**:
    - `compileOnly(Libs.spring_webflux)` 추가 (현재 `testRuntimeOnly` 라면 승격, 신규라면 추가)
    - `compileOnly(Libs.opentelemetry_spring_webflux)` 추가 — `buildSrc/Libs.kt` 에 상수가 없으면 추가. 검증된 Maven 좌표: `io.opentelemetry.instrumentation:opentelemetry-spring-webflux-5.3` (OTel instrumentation BOM 버전 관리).
      **주의**: `opentelemetry-spring-boot-starter`가 아닌 raw `spring-webflux-5.3` 아티팩트 사용.
    - `testImplementation(Libs.spring_webflux)` 추가 — 테스트가 webflux 환경 필요시
    - `testImplementation(Libs.opentelemetry_spring_webflux)` — 테스트용
- **API**:
  ```kotlin
  public fun OpenTelemetry.webfluxServerTelemetry(): SpringWebfluxServerTelemetry =
      SpringWebfluxServerTelemetry.create(this)

  public fun OpenTelemetry.createTracingWebFilter(): WebFilter =
      webfluxServerTelemetry().createWebFilterAndRegisterReactorHook()
  ```
- **운영 제약 (KDoc 필수)**:
    - 반드시 `@Bean` 으로 ApplicationContext 초기화 시 1회만 호출.
    - Reactor `Hooks.onEachOperator` 전역 등록 — 테스트 격리 시 `Hooks.resetOnEachOperator(key)` 권장.
    - 민감 헤더 캡처 제한: `OTEL_INSTRUMENTATION_HTTP_CAPTURE_HEADERS_SERVER_REQUEST` 환경변수 가이드.
- **DoD**:
    - [ ] `Libs.kt` 에 `opentelemetry_spring_webflux` 정의 (필요 시) — 좌표 검증
    - [ ] `build.gradle.kts` 의존성 추가
    - [ ] `webflux/` 패키지로 격리 (classpath 미존재 시 다른 기능에 영향 없도록)
    - [ ] KDoc (Korean) — 1회 호출 제약 + 민감 헤더 가이드
    - [ ] 컴파일 통과

---

### Task 6 — 테스트: `Tracer.withSpan` (suspend & blocking)

- **complexity**: medium
- **위치**: `infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/coroutines/TracerWithSpanTest.kt` (신규)
    - blocking 테스트는 `infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/trace/TracerWithSpanBlockingTest.kt` (또는 같은 클래스 내 nested)
- **공통 픽스처**: `InMemorySpanExporter` + `SdkTracerProvider` (기존 모듈 패턴 재사용)
    - `@BeforeEach fun setup() { inMemoryExporter.reset() }` — 테스트 간 Span 데이터 격리 필수.
- **suspend 케이스** (spec §5.1):
    - [ ] 정상 완료 → name 일치, status OK, parent context 검증
    - [ ] `withContext(Dispatchers.IO)` 내부 호출 → IO 디스패처에서 current 유지, finished span 1개
    - [ ] 중첩 호출 (Dispatchers 경계 포함) → child traceId == parent traceId, child parentSpanId == parent spanId
    - [ ] 일반 예외 → status ERROR, recordException 이벤트 기록, 재던짐
    - [ ] `CancellationException` → status UNSET, end () 호출, 재던짐 (ERROR 미기록)
    - [ ] `configure` 로 attribute 추가 → 종료 Span attribute 검증
    - [ ] **negative**: `configure` 안에서 `.startSpan()` 직접 호출 → 이중 Span 생성 확인 (footgun 문서화)
    - [ ] **negative**: 빈 문자열 `spanName` → `IllegalArgumentException` 발생 검증 (`requireNotBlank`)
- **blocking 케이스** (spec §5.2):
    - [ ] 정상 완료 → OK + end ()
    - [ ] 예외 → ERROR + recordException + `message ?: "unspecified error"` 검증 + 재던짐
    - [ ] 
      **negative**: `throw RuntimeException(null)` 처럼 message=null 인 경우 → fallback `"unspecified error"` 검증 (javaClass.simpleName 누출 안 됨)
    - [ ] **negative**: 빈 문자열 `spanName` → `IllegalArgumentException` 발생 검증 (`requireNotBlank`)
    - [ ] 동일 스레드 nested 호출 → parent-child 관계 검증
- **테스트 도구**: JUnit 5 + MockK + bluetape4k-assertions + `kotlinx-coroutines-test` (`runTest`)
- **bluetape4k-assertions
  matcher**: 비교는 `shouldBeEqualTo`, `shouldNotBeNull`, `shouldBeGreaterThan` 등 사용 (`(x == y).shouldBeTrue()` 금지).
- **DoD**:
    - [ ] 모든 케이스 통과 — `./gradlew :bluetape4k-opentelemetry:test --tests "*TracerWithSpan*"`

---

### Task 7 — 테스트: `Flow<T>.traced` / `tracedCollect`

- **complexity**: medium
- **위치**: `infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/coroutines/FlowSpanSupportTest.kt` (신규)
- **픽스처**: `@BeforeEach fun setup() { inMemoryExporter.reset() }` — 테스트 간 Span 데이터 격리 필수.
- **케이스** (spec §5.3):
    - [ ] Flow 정상 완료 → finished Span **1개**, status OK
    - [ ] emit 3회 `.toList()` → finished Span **1개** (아이템별 아님)
    - [ ] Upstream 예외 → ERROR + recordException + 재던짐
    - [ ] Downstream `take(2)` 정상 종료 → OK + end ()
    - [ ] Downstream cancellation (`scope.cancel()` / `withTimeout`) → status UNSET + end () + ERROR 미기록
    - [ ] `flowOn(Dispatchers.IO)` 결합 → Span 정상 전파, finished Span 1개
    - [ ] `tracedCollect` action 안에서 `Span.current()` == traced Span 검증
    - [ ] **negative**: `Authorization: Bearer xxx` 가 Span attribute 에 없음 검증 (민감 attribute 가이드 강제)
    - [ ] **negative**: 빈 문자열 `spanName` → `IllegalArgumentException` 발생 검증 (`requireNotBlank`)
- **runTest 사용 시 주의**:
    - `runTest(timeout = 30.seconds)` — coroutines-test 패턴
    - `@BeforeEach`/`@AfterEach` 도 `runTest { ... }` 로 감싸기
- **DoD**:
    - [ ] 모든 케이스 통과 — `./gradlew :bluetape4k-opentelemetry:test --tests "*FlowSpanSupport*"`

---

### Task 8 — 테스트: WebFlux helper

- **complexity**: medium
- **위치**: `infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/webflux/WebfluxTracingSupportTest.kt` (신규)
- **케이스** (spec §5.4):
    - [ ] `OpenTelemetry.createTracingWebFilter()` 호출 성공 (NPE/ClassNotFound 없음) — 의존성 정상 노출
    - [ ] WebFlux mock 핸들러 GET (Spring `WebTestClient` 또는 직접 `MockServerHttpRequest` + `WebFilterChain`) → 서버 Span 1개, HTTP method/path attribute 검증
    - [ ] **negative**: `Authorization` 헤더 미포함 — 기본 설정에서 민감 헤더가 Span attribute 로 캡처되지 않음 검증
- **테스트 격리**: `Hooks.resetOnEachOperator(key)` `@AfterAll` 로 호출 — JVM 전역 hook 정리.
-

**의존성**: `testImplementation(Libs.spring_webflux)`, `testImplementation(Libs.opentelemetry_spring_webflux)` (Task 5 에서 추가).
- **DoD**:
    - [ ] 모든 케이스 통과 — `./gradlew :bluetape4k-opentelemetry:test --tests "*WebfluxTracing*"`

---

### Task 9 — 회귀 테스트 + 전체 모듈 테스트

- **complexity**: low
- **목표**: spec §5.5. 기존 테스트가 변경 없이 통과해야 한다.
- **수행 내용**:
    - `./gradlew :bluetape4k-opentelemetry:test` 전수 실행
    - 통과/실패 카운트 + duration 기록
    - 실패 시 회귀 원인 분석 — 신규 코드가 기존 API 동작에 영향을 줬는지 점검 (특히 `useSpanSuspending` 위임으로 인한 side effect)
- **DoD**:
    - [ ] 전수 통과 (passing count 기록)
    - [ ] `./gradlew :bluetape4k-opentelemetry:detekt` 통과

---

### Task 9.5 — bluetape4k-patterns 체크리스트 검증

- **complexity**: low
- **목표**: 코드 완성 후 bluetape4k 공통 패턴 준수 여부를 체계적으로 점검한다.
- **수행 내용**:
    - [ ] **KLogging**: 신규 파일에 `companion object : KLogging()` 포함 여부 (또는 top-level 파일이면 파일 레벨 logger 사용)
    - [ ] **requireNotBlank**: 모든 `spanName` 파라미터 진입부 검사 적용 확인
    - [ ] **bluetape4k-assertions
      matchers**: 테스트에서 `(x == y).shouldBeTrue()` 패턴 0건 확인 — `shouldBeEqualTo` / `shouldBeGreaterThan` 등 사용
    - [ ] **불변성**: 신규 data class 없음 (extension function 전용 파일), 확인 후 pass
    - [ ] **`@Deprecated` 없음**: `ide_diagnostics` 에서 deprecated API 사용 0건
    - [ ] **import 정리**: `ide_optimize_imports` 실행 후 불필요 import 0건
    - [ ] **atomicfu scope**: 메서드 로컬에 `kotlinx.atomicfu.atomic()` 사용 없음 (클래스 프로퍼티만 허용)
    - [ ] **테스트
      resources**: `src/test/resources/junit-platform.properties` + `logback-test.xml` 존재 확인 (신규 모듈이 아니므로 기존 파일 재사용)
- **DoD**:
    - [ ] 위 체크리스트 항목 전수 통과 기록

---

### Task 10 — KDoc 보강 (모든 신규 public API)

- **complexity**: low
- **목표**: spec §DoD 3. Korean KDoc + 보안 경고 + 계약 명시.
- **각 API 별 KDoc 필수 항목**:
    - 동작 계약 (정상/예외/CancellationException 분기)
    - 보안 경고 (PII / Authorization 토큰 / 민감 헤더 attribute 추가 금지)
    - configure footgun (`.startSpan()` 직접 호출 금지, `async`/`launch` 금지)
    - 코루틴 사용 주의 (suspend vs blocking 변형 선택)
    - 파라미터 설명 + 예시 코드 (선택)
- **대상**:
    - `Tracer.withSpan(suspend)`
    - `Tracer.withSpan(blocking)`
    - `Flow<T>.traced`
    - `Flow<T>.tracedCollect`
    - `OpenTelemetry.webfluxServerTelemetry()`
    - `OpenTelemetry.createTracingWebFilter()`
- **DoD**:
    - [ ] 모든 신규 public API 에 Korean KDoc — 보안/계약/footgun 누락 0
    - [ ] 예시 코드 (선택) — 컴파일 가능한 형태로 작성

---

### Task 11 — README.md + README.ko.md 업데이트

- **complexity**: low
- **위치**: `infra/opentelemetry/README.md`, `infra/opentelemetry/README.ko.md`
- **구성 순서 (필수)**: Architecture → UML (Mermaid) → Features → Examples
- **추가 내용**:
    - **Features** 섹션에 4개 신규 API 항목 추가
    - **Examples**:
        - `Tracer.withSpan(suspend)` 예시
        - `Tracer.withSpan(blocking)` 예시
        - `Flow<T>.traced` + `tracedCollect` 예시 (`flowOn` 결합 포함)
        - `WebFlux` `@Bean` 등록 예시 (Spring `@Configuration`)
    - **Mermaid 다이어그램**:
        - `withSpan` DSL 흐름 (sequenceDiagram: caller → Tracer → SpanBuilder → Span lifecycle)
        - `Flow.traced` 생명주기 (sequenceDiagram: collect → channelFlow → withContext (asContextElement) → upstream collect → send → end)
    - **운영/보안 가이드**:
        - OTLP exporter TLS (`https://` + `OTEL_EXPORTER_OTLP_CERTIFICATE`)
        - `SdkTracerProvider.shutdown()` timeout 5–10s 권장
        - WebFlux 1회 호출 제약 + `Hooks.resetOnEachOperator()` 테스트 격리
        - `OTEL_INSTRUMENTATION_HTTP_CAPTURE_HEADERS_SERVER_REQUEST` 헤더 화이트리스트 예시
    - **언어 전환 링크** (제목 바로 아래): `[한국어](./README.ko.md) | English` / `한국어 | [English](./README.md)`
- **DoD**:
    - [ ] README.md (영문) + README.ko.md (한국어) 동기화
    - [ ] 두 README 모두 Architecture → UML → Features → Examples 순서
    - [ ] Mermaid 다이어그램 GitHub 렌더링 검증 (vega-lite 사용 금지)
    - [ ] 언어 전환 링크 포함

---

### Task 12 — CLAUDE.md / 모듈 테이블 확인

- **complexity**: low
- **목표**: 모듈 테이블 변경 불필요 — 기존 `infra/opentelemetry` 보강이므로 루트 CLAUDE.md 모듈 그룹 표 수정 없음.
- **수행 내용**:
    - 루트 `CLAUDE.md` 의 `infra/` 행이 `opentelemetry` 를 포함하는지 확인 (이미 포함되어 있음).
    - 새 디자인 패턴 (예: WebFlux helper 사용 가이드) 가 다른 모듈에서 재사용된다면 `Key Design Patterns` 섹션에 한 줄 추가 — 단, 본 PR 범위에서는 미루고 후속 PR에서 처리하는 것이 안전.
- **DoD**:
    - [ ] 모듈 테이블 검토 후 변경 불필요 확인 기록

---

### Task 13 — `wiki-update` 스킬 실행

- **complexity**: low
- **목표**: spec/plan 신규 생성 → Obsidian wiki 페이지 업데이트.
- **수행 내용**:
    - `/oh-my-claudecode:wiki-update` 또는 `wiki-update` 스킬 호출
    - spec + plan 파일을 wiki 에 색인하여 후속 검색 (`gno query`) 가능하게 함
- **DoD**:
    - [ ] wiki 업데이트 완료 (skill 응답 확인)

---

### Task 14 — Code Review + PR 생성

- **complexity**: medium
- **수행 내용**:
    1. `./bin/repo-status` 로 변경 파일 확인
    2. Korean + prefix commit (`feat: opentelemetry-coroutines DSL 강화 (#150)`)
    3. **Code review 실행 (필수)**: `oh-my-claudecode:code-reviewer` 에이전트 호출 → HIGH/CRITICAL 이슈 해소
    4. `./gradlew :bluetape4k-opentelemetry:test` 결과 + duration PR 본문에 기재
    5. PR 본문에 spec/plan 링크 + 변경 요약 + 테스트 결과 + DoD 체크리스트 포함
    6. `gh pr create --base develop` 비대화식 호출
- **DoD**:
    - [ ] 모든 plan task 완료 표 (완료 후 plan 대비 비교 표 작성)
    - [ ] code-reviewer 에이전트 결과 첨부
    - [ ] 로컬 테스트 결과 PR 본문 포함
    - [ ] CI 통과

---

## 작업 순서 (의존성 기반)

```
1 (정찰)
 → 1.5 (recordFailure 보안 수정 — SpanSupport.kt 변경 + 회귀 테스트)
 → 2, 3, 4 (병렬 가능 — 서로 다른 파일, 다른 함수)
 → 5 (WebFlux helper, 빌드 의존성 변경 필요)
 → 6, 7, 8 (테스트, 병렬 가능)
 → 9 (회귀 테스트)
 → 9.5 (bluetape4k-patterns 체크리스트 검증)
 → 10 (KDoc 보강 — 1~5 산출물 대상)
 → 11 (README) → 12 (CLAUDE.md 검토) → 13 (wiki-update)
 → 14 (commit + code review + PR)
```

---

## Plan 대비 완료 보고 양식 (Task 14 에서 작성)

| Task | 상태         | 비고 |
|------|--------------|------|
| 1    | ✅ / ⏳ / ❌ | …    |
| 2    | …            | …    |
| …    | …            | …    |
| 14   | …            | …    |

---

## 리스크 및 완화

| 리스크                                               | 완화                                                              |
|------------------------------------------------------|-------------------------------------------------------------------|
| `Tracer.withSpan` 이미 존재 → 시그니처 충돌          | Task 1 정찰에서 발견 → spec 미세 조정 + 본 plan 업데이트          |
| `channelFlow` 사용 missing → `Flow invariant` 위반   | Task 7 테스트에서 emit 3회 + Dispatcher 경계 결합으로 검증        |
| `withSpanContext` 재사용 → 이중 `end()`              | Task 4 구현 노트 명시 + Task 7 테스트로 finished span 카운트 검증 |
| WebFlux `Hooks` 전역 등록 → 테스트 격리 실패         | Task 8 `@AfterAll Hooks.resetOnEachOperator(key)`                 |
| 민감 헤더 (`Authorization`) 누출                     | Task 7 + Task 8 negative test + README 운영 가이드                |
| `inline` blocking `withSpan` 내부 비-public-API 접근 | `@PublishedApi internal` 헬퍼 분리                                |
| Reactor `opentelemetry-spring-webflux-5.3` 좌표 부재 | Task 5 에서 `Libs.kt` 추가 + context7 로 좌표 검증                |
