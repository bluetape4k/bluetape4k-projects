# [virtualthread] D: API 통합 — 사용성·명칭 직관성·안정성 개선

> **Issue**: #255
> **Branch**: `feat/vt-api-cleanup`
> **Date**: 2026-05-01
> **Status**: Approved

---

## 1. 현상 분석 (As-Is)

### 1.1 코드 지형도

| 모듈 | 파일 | 역할 |
|------|------|------|
| `virtualthread/api` | `StructuredScopes.kt` | `StructuredSubtask<T>`, `StructuredTaskScopeAll`, `StructuredTaskScopeAny<T>` 인터페이스, `StructuredTaskScopeProvider` SPI, `StructuredTaskScopes` 진입 object |
| `virtualthread/api` | `VirtualThreadRuntime.kt` | 런타임 추상화 인터페이스 |
| `virtualthread/api` | `VirtualThreads.kt` | ServiceLoader 기반 런타임 선택 object, `threadFactory()`, `executorService()` |
| `virtualthread/jdk21` | `Jdk21StructuredTaskScopeProvider.kt` | `ShutdownOnFailure`/`ShutdownOnSuccess` 기반 구현 |
| `virtualthread/jdk25` | `Jdk25StructuredTaskScopeProvider.kt` | `Joiner.awaitAll`/`anySuccessfulResultOrThrow` 기반 구현 |
| `bluetape4k/core` | `StructuredTaskScopeSupport.kt` | `structuredTaskScopeAll {}` / `structuredTaskScopeAny {}` 편의 함수 |

### 1.2 외부 소비처 (Blast Radius)

| 소비 모듈 | 파일 | 사용 API |
|-----------|------|----------|
| `utils/workflow` | `ParallelWorkFlow.kt:75,125` | `StructuredTaskScopes.all()`, `.any()` |
| `testing/junit5` | `StructuredTaskScopeTester.kt:147` | `StructuredTaskScopes.all()` |
| `examples/virtualthreads-demo` | `StructuredConcurrencyExamples.kt:37,72` | `structuredTaskScopeAll {}`, `structuredTaskScopeAny {}` |
| `examples/virtualthreads-demo` | `Rule5UseThreadLocalCarefully.kt:66` | `structuredTaskScopeAll {}` |
| `bluetape4k/core` (test) | `StructuredScopeSupportTest.kt` | `structuredTaskScopeAll {}`, `structuredTaskScopeAny {}` |

### 1.3 식별된 문제

| # | 범주 | 문제 | 근거 (file:line) |
|---|------|------|-----------------|
| P1 | 명칭 | `StructuredTaskScopeAll` — "All"이 "모두 완료" 의미인지 "모두 성공" 의미인지 불명확. 실제 동작은 fail-fast (하나 실패 시 전체 중단) | `StructuredScopes.kt:56` |
| P2 | 명칭 | `StructuredTaskScopeAny` — "Any"가 "아무 결과" vs "첫 성공 결과" 혼동 가능. 실제 동작은 first-success | `StructuredScopes.kt:93` |
| P3 | 사용성 | `StructuredTaskScopes.all/any`에 `factory` 기본값 없음 → 매 호출마다 `factory = Thread.ofVirtual().factory()` boilerplate 필수 | `StructuredScopes.kt:252,277` (vs Provider 인터페이스 `StructuredScopes.kt:138`에는 기본값 있음) |
| P4 | 중복 | `structuredTaskScopeAll/Any` (core) ↔ `StructuredTaskScopes.all/any` (api) 동일 기능, 진입점 혼란 | `StructuredTaskScopeSupport.kt:34-40` vs `StructuredScopes.kt:251-255` |
| P5 | 안정성 | `StructuredSubtask.get()` KDoc에 FAILED/CANCELLED/RUNNING 상태별 동작 미명시 | `StructuredScopes.kt:29` |
| P6 | 안정성 | `getOrNull()` 미제공 — 실패/취소 subtask 결과를 안전하게 조회할 방법 없음 | `StructuredScopes.kt:27-36` |
| P7 | 가이드 | `Dispatchers.VT` vs `executorService()` vs `structuredTaskScopeAll` 선택 기준 문서 없음 | README 전체 |

---

## 2. 설계 접근법 비교

### Option A: 인터페이스 이름 변경 + 기존 deprecated (권장)

새 인터페이스명(`StructuredTaskScopeFailFast`, `StructuredTaskScopeFirstSuccess`)을 추가하고, 기존 인터페이스는 `typealias` + `@Deprecated` 처리.

| 장점 | 단점 |
|------|------|
| 명칭이 동작 의도를 직접 전달 | 임시 중복: 전환기에 2세트의 이름 공존 |
| IDE가 `ReplaceWith` 자동 마이그레이션 안내 | `typealias`가 아닌 별도 인터페이스면 구현체 2벌 필요 |
| 외부 코드 즉시 깨지지 않음 (하위 호환) | |

### Option B: 인터페이스 유지, 진입 함수(StructuredTaskScopes.*)만 개선

인터페이스명은 `All`/`Any` 그대로 두고, `StructuredTaskScopes.failFast {}`/`firstSuccess {}` 진입 함수만 추가.

| 장점 | 단점 |
|------|------|
| 최소 변경량 (함수 2개 + KDoc) | 인터페이스 이름 `StructuredTaskScopeAll` 은 여전히 혼란 유발 |
| jdk21/jdk25 구현체 변경 불필요 | Provider SPI의 `withAll`/`withAny` 이름도 여전히 불명확 |
| 안정성 매우 높음 | 장기적으로 코드 리딩 시 불편 지속 |

### Option C: 완전 새 API (v2) 별도 패키지

`io.bluetape4k.concurrent.virtualthread.v2` 패키지에 완전히 새로운 API 도입.

| 장점 | 단점 |
|------|------|
| 깨끗한 설계, 레거시 잔재 없음 | **YAGNI 위반**: 현 사용자 규모에서 과도한 투자 |
| 향후 확장 자유도 높음 | 패키지 분리 → 의존성 복잡도 증가 |
| | ServiceLoader 이중 관리 필요 |
| | 기존 v1 유지보수 부담 잔존 |

### 선택: Option A (변형)

**Option A를 기반으로 하되, `typealias` 전략으로 구현체 중복을 제거한다.**

근거:
1. 인터페이스 이름이 라이브러리 사용자에게 가장 많이 노출되는 API 표면이므로 명칭 개선 효과 극대화
2. `typealias`를 사용하면 jdk21/jdk25 구현체를 수정하지 않아도 됨 (바이너리 호환)
3. `StructuredTaskScopes` object의 진입 함수도 동시 개선하여 진입점 명확화
4. Option B의 장점(최소 변경)도 `typealias`로 대부분 확보

### Antithesis (Option A 반론 — steelman)

Option B가 더 나은 경우: `typealias` + `@Deprecated`를 인터페이스에 적용하면, 인터페이스를 구현하는 **외부 코드**(bluetape4k 외부에서 `StructuredTaskScopeAll`을 직접 implements 하는 코드)가 존재할 때 deprecation 경고가 해당 코드에도 전파된다. 현재 SPI가 내부용이므로 실질적 위험은 낮지만, 라이브러리 특성상 예상치 못한 외부 소비자가 있을 수 있다. Option B는 이 위험을 완전히 회피한다.

### Tradeoff Tension

명칭 직관성(사용자 경험) vs 전환 비용(deprecation 노이즈). 인터페이스까지 개선하면 장기 가독성은 좋아지지만, 전환기에 모든 기존 사용처에 deprecation 경고가 발생한다. `typealias` 방식은 이 tension을 최소화하지만 완전히 제거하지는 못한다.

---

## 3. 상세 설계

### 3.1 명칭 개선 (Naming)

#### 3.1.1 새 인터페이스 (canonical)

> **[REJECTED]** — 아래 독립 인터페이스 접근법은 반환 타입 불일치(`join(): StructuredTaskScopeFailFast` vs `join(): StructuredTaskScopeAll`) 문제로 기각됨. **최종 결정: typealias** (line 154 이하 참조).

```kotlin
// StructuredScopes.kt 에 추가

/**
 * 모든 작업 완료를 기다리고, 하나라도 실패하면 즉시 전체를 중단하는 fail-fast scope입니다.
 * 
 * 기존 [StructuredTaskScopeAll]과 동일한 인터페이스이며, 동작 의도를 명확히 전달합니다.
 */
interface StructuredTaskScopeFailFast : AutoCloseable {
    fun <T> fork(task: () -> T): StructuredSubtask<T>
    fun join(): StructuredTaskScopeFailFast
    fun joinUntil(deadline: java.time.Instant): StructuredTaskScopeFailFast = join()
    fun throwIfFailed(handler: (e: Throwable) -> Unit = {}): StructuredTaskScopeFailFast
    override fun close()
}

/**
 * 첫 번째 성공 결과를 선택하는 first-success scope입니다.
 * 
 * 기존 [StructuredTaskScopeAny]와 동일한 인터페이스이며, 동작 의도를 명확히 전달합니다.
 */
interface StructuredTaskScopeFirstSuccess<T> : AutoCloseable {
    fun <V : T> fork(task: () -> V): StructuredSubtask<V>
    fun join(): StructuredTaskScopeFirstSuccess<T>
    fun result(mapper: (Throwable) -> RuntimeException): T
    override fun close()
}
```

**결정 변경: `typealias` 대신 독립 인터페이스 채택**

`typealias`는 `@Deprecated` 어노테이션을 붙일 수 없고, 기존 인터페이스에 deprecated를 걸면 새 이름(typealias)도 함께 deprecated 처리되는 문제가 있다. 따라서:

- 새 인터페이스를 canonical로 정의
- 기존 인터페이스 `StructuredTaskScopeAll`/`StructuredTaskScopeAny`는 새 인터페이스를 상속하면서 `@Deprecated` 처리
- jdk21/jdk25 구현체는 **새 인터페이스를 구현**하도록 변경 (기존 인터페이스 extends 새 인터페이스이므로 하위 호환)

```kotlin
// 기존 인터페이스를 deprecated wrapper로 전환
@Deprecated(
    message = "StructuredTaskScopeAll → StructuredTaskScopeFailFast 로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("StructuredTaskScopeFailFast")
)
interface StructuredTaskScopeAll : StructuredTaskScopeFailFast
```

**주의**: 이 접근은 기존 `StructuredTaskScopeAll`의 반환 타입이 `StructuredTaskScopeAll` → `StructuredTaskScopeFailFast`로 변경되므로 메서드 시그니처 불일치가 발생한다. 이를 해결하기 위해 **실질적으로는 기존 인터페이스를 유지하되, `typealias`로 새 이름을 제공하는 방식**이 가장 안전하다.

**최종 결정: typealias + 새 진입 함수**

```kotlin
// StructuredScopes.kt — 기존 인터페이스 유지, typealias로 새 이름 제공
/** fail-fast 동작을 하는 [StructuredTaskScopeAll]의 의도 명확 별칭입니다. */
typealias StructuredTaskScopeFailFast = StructuredTaskScopeAll

/** first-success 동작을 하는 [StructuredTaskScopeAny]의 의도 명확 별칭입니다. */
typealias StructuredTaskScopeFirstSuccess<T> = StructuredTaskScopeAny<T>
```

이유:
- `typealias`는 **Kotlin source-level 별칭**이며, JVM 바이너리에는 기존 타입(`StructuredTaskScopeAll`)만 남음 → Java 소비자는 새 이름 사용 불가 (Kotlin-only 개선)
- 기존 코드 **전혀 변경 불필요** (바이너리 호환)
- jdk21/jdk25 구현체 수정 없음
- 새 Kotlin 사용자는 `StructuredTaskScopeFailFast` 이름으로 진입 가능
- 기존 `StructuredTaskScopeAll` 이름은 KDoc에 `@see StructuredTaskScopeFailFast` 안내 추가

> **한계**: Java 소비자가 있는 환경에서는 바이너리 수준의 명칭 이전 효과 없음. 이 프로젝트의 주 소비자가 Kotlin이므로 허용 가능한 트레이드오프.

#### 3.1.2 Provider 인터페이스 확장

```kotlin
// StructuredTaskScopeProvider 에 추가
interface StructuredTaskScopeProvider {
    // ... 기존 멤버 유지 ...

    /**
     * 실패 전파형(fail-fast) 블록을 실행합니다.
     * 기본 구현은 [withAll]에 위임합니다.
     */
    fun <T> withFailFast(
        name: String? = null,
        factory: ThreadFactory = Thread.ofVirtual().factory(),
        block: (scope: StructuredTaskScopeFailFast) -> T,
    ): T = withAll(name, factory, block)

    /**
     * 성공 우선형(first-success) 블록을 실행합니다.
     * 기본 구현은 [withAny]에 위임합니다.
     */
    fun <T> withFirstSuccess(
        name: String? = null,
        factory: ThreadFactory = Thread.ofVirtual().factory(),
        block: (scope: StructuredTaskScopeFirstSuccess<T>) -> T,
    ): T = withAny(name, factory, block)
}
```

**SPI 설계 결정**: `withAll`/`withAny`는 **deprecated하지 않음**.

근거:
- `StructuredTaskScopeProvider`는 jdk21/jdk25 구현체가 `override`하는 SPI 메서드
- deprecated로 표시하면 두 구현체가 deprecated 메서드를 계속 `override`해야 하고, 외부 Provider 구현자에게도 경고 전파
- **목표는 사용자 진입점 개선**이므로 `StructuredTaskScopes.all/any`(object 레벨)와 core helper만 deprecated 처리로 충분
- `withAll`/`withAny`는 KDoc에 `@see withFailFast`/`@see withFirstSuccess` 안내만 추가

#### 3.1.3 StructuredTaskScopes object 확장

```kotlin
object StructuredTaskScopes : KLogging() {
    // ... 기존 providers, provider(), providerName() 유지 ...

    /**
     * 실패 전파형(fail-fast) scope 블록을 실행합니다.
     *
     * 하나의 subtask라도 실패하면 나머지를 즉시 중단하고 예외를 전파합니다.
     *
     * @param name scope 이름 (디버깅용)
     * @param factory subtask 실행용 스레드 팩토리 (기본값: `VirtualThreads.threadFactory()`)
     * @param block scope 실행 블록
     */
    fun <T> failFast(
        name: String? = null,
        factory: ThreadFactory = VirtualThreads.threadFactory(),
        block: (scope: StructuredTaskScopeFailFast) -> T,
    ): T = provider().withFailFast(name, factory, block)

    /**
     * 성공 우선형(first-success) scope 블록을 실행합니다.
     *
     * 첫 번째 성공한 subtask 결과를 반환하고 나머지를 취소합니다.
     *
     * @param name scope 이름 (디버깅용)
     * @param factory subtask 실행용 스레드 팩토리 (기본값: `VirtualThreads.threadFactory()`)
     * @param block scope 실행 블록
     */
    fun <T> firstSuccess(
        name: String? = null,
        factory: ThreadFactory = VirtualThreads.threadFactory(),
        block: (scope: StructuredTaskScopeFirstSuccess<T>) -> T,
    ): T = provider().withFirstSuccess(name, factory, block)

    // 기존 all/any에 @Deprecated 추가
    @Deprecated(
        message = "failFast()를 사용하세요.",
        replaceWith = ReplaceWith("failFast(name, factory, block)")
    )
    fun <T> all(
        name: String? = null,
        factory: ThreadFactory,  // 기본값 없음 — 기존 시그니처 유지
        block: (scope: StructuredTaskScopeAll) -> T,
    ): T = provider().withAll(name, factory, block)

    @Deprecated(
        message = "firstSuccess()를 사용하세요.",
        replaceWith = ReplaceWith("firstSuccess(name, factory, block)")
    )
    fun <T> any(
        name: String? = null,
        factory: ThreadFactory,  // 기본값 없음 — 기존 시그니처 유지
        block: (scope: StructuredTaskScopeAny<T>) -> T,
    ): T = provider().withAny(name, factory, block)
}
```

**핵심**: 새 함수 `failFast()`/`firstSuccess()`에는 `factory` 기본값 추가 → P3 해결.

### 3.2 사용성 개선 (Usability)

#### 3.2.1 core 편의 함수 교체

```kotlin
// StructuredTaskScopeSupport.kt

/**
 * 실패 전파형(fail-fast) 구조화된 동시성 블록을 실행합니다.
 */
fun <T> structuredTaskScopeFailFast(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory("sts-failfast-"),
    block: (scope: StructuredTaskScopeFailFast) -> T,
): T = StructuredTaskScopes.failFast(name, factory, block)

/**
 * 성공 우선형(first-success) 구조화된 동시성 블록을 실행합니다.
 */
fun <T> structuredTaskScopeFirstSuccess(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory("sts-first-"),
    block: (scope: StructuredTaskScopeFirstSuccess<T>) -> T,
): T = StructuredTaskScopes.firstSuccess(name, factory, block)

// 기존 함수 deprecated
@Deprecated(
    message = "structuredTaskScopeFailFast()를 사용하세요.",
    replaceWith = ReplaceWith(
        "structuredTaskScopeFailFast(name, factory, block)",
        "io.bluetape4k.concurrent.virtualthread.structuredTaskScopeFailFast"
    )
)
fun <T> structuredTaskScopeAll(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory("sts-all-"),
    block: (scope: StructuredTaskScopeAll) -> T,
): T = StructuredTaskScopes.all(name, factory, block)

@Deprecated(
    message = "structuredTaskScopeFirstSuccess()를 사용하세요.",
    replaceWith = ReplaceWith(
        "structuredTaskScopeFirstSuccess(name, factory, block)",
        "io.bluetape4k.concurrent.virtualthread.structuredTaskScopeFirstSuccess"
    )
)
fun <T> structuredTaskScopeAny(
    name: String? = null,
    factory: ThreadFactory = VirtualThreads.threadFactory("sts-any-"),
    block: (scope: StructuredTaskScopeAny<T>) -> T,
): T = StructuredTaskScopes.any(name, factory, block)
```

### 3.3 안정성 개선 (Stability)

#### 3.3.1 `StructuredSubtask.getOrNull()` 추가

```kotlin
interface StructuredSubtask<T> {
    /** subtask 성공 결과를 반환합니다.
     *
     * ## 상태별 동작 (JDK `Subtask.get()` 계약 그대로)
     * - `SUCCESS`: 결과를 반환합니다.
     * - `FAILED`: **`IllegalStateException`을 던집니다** (실패 원인은 `exceptionOrNull()`로 조회).
     * - `UNAVAILABLE` (미완료/취소/join 이전): **`IllegalStateException`을 던집니다**.
     *
     * > **주의**: FAILED 상태에서 실패 원인 예외 자체를 던지지 않습니다.
     * > 원인 예외 조회는 `exceptionOrNull()`을, null-safe 결과 조회는 [getOrNull]을 사용하세요.
     */
    fun get(): T

    /**
     * subtask 성공 결과를 반환하고, 실패/취소/미완료 상태에서는 `null`을 반환합니다.
     *
     * **전제 조건**: scope의 `join()`이 완료된 이후에 호출해야 합니다.
     * `join()` 전에 호출하면 subtask가 `SUCCESS`이더라도 [get]이 `IllegalStateException`을 던질 수 있습니다
     * (JDK 계약상 "join 이후" 조건도 요구하기 때문).
     *
     * ## 상태별 동작 (join() 이후 호출 기준)
     * - `SUCCESS`: 결과를 반환합니다.
     * - `FAILED`: `null`을 반환합니다 (예외를 던지지 않음).
     * - `UNAVAILABLE` (join 전 또는 취소): `null`을 반환합니다.
     *
     * ```kotlin
     * // failFast 예제: b가 실패해도 a 결과를 안전하게 수집
     * val results = StructuredTaskScopes.failFast { scope ->
     *     val a = scope.fork { 42 }
     *     // b는 join 전에 반드시 실패하도록 즉시 throw
     *     val b = scope.fork<Int> { Thread.sleep(1); throw RuntimeException("fail") }
     *     try {
     *         scope.join()
     *         scope.throwIfFailed()
     *     } catch (_: Exception) { }
     *     // join() 완료 후 안전 조회
     *     listOfNotNull(a.getOrNull(), b.getOrNull())
     * }
     * // a는 SUCCESS → 42, b는 FAILED → null
     * ```
     *
     * > join 전 호출이 우려되면 `runCatching { get() }.getOrNull()`을 직접 사용하세요.
     */
    fun getOrNull(): T? {
        return when (state()) {
            StructuredTaskScope.Subtask.State.SUCCESS -> get()
            else -> null
        }
    }

    fun state(): StructuredTaskScope.Subtask.State
    fun exceptionOrNull(): Throwable?
}
```

`getOrNull()`은 인터페이스 default 메서드로 제공하므로 **jdk21/jdk25 구현체 변경 불필요**.

#### 3.3.2 `join()` 타임아웃 없는 호출 warn 로그

Provider 구현체 레벨(jdk21/jdk25)의 `AllScope.join()` 내부에 warn 로그를 추가하는 것은 **과도한 노이즈**를 유발한다. 모든 정상 사용에서도 `join()` → `throwIfFailed()` 순서로 호출하므로 매번 warn이 발생한다.

**대안**: KDoc 경고로 대체하고, `joinUntil()` 사용 권장 가이드를 README에 포함한다.

```kotlin
// StructuredScopes.kt — StructuredTaskScopeAll.join() KDoc 보강
// (typealias에는 멤버 KDoc을 붙일 수 없으므로, 실제 수정 대상은 StructuredTaskScopeAll)
interface StructuredTaskScopeAll : AutoCloseable {
    /**
     * 등록된 subtask 완료를 대기합니다.
     *
     * **주의**: 타임아웃 없이 호출하면 subtask가 무한 차단될 수 있습니다.
     * 프로덕션 코드에서는 [joinUntil]을 사용하여 데드라인을 설정하세요.
     *
     * @see StructuredTaskScopeFailFast
     */
    fun join(): StructuredTaskScopeAll
}
```

> **구현 주의**: `StructuredTaskScopeFailFast`는 `typealias`이므로 별도 인터페이스 선언이나 KDoc 멤버를 가질 수 없다. join() 타임아웃 경고는 **`StructuredTaskScopeAll.join()`에 직접 추가**해야 한다.

#### 3.3.3 `autoJoin` 파라미터 평가

`withFailFast(autoJoin = true)` 파라미터를 추가하면 `block` 실행 전 자동으로 `join()` + `throwIfFailed()`를 호출하는 편의를 제공할 수 있다. 그러나:

- `fork()` → `join()` → `get()` 순서가 사용자에게 명시적이어야 구조화된 동시성의 의미가 전달됨
- `autoJoin`이 `true`이면 block 안에서 subtask 결과를 조합하는 일반 패턴이 깨짐 (join 후 block에서 get 호출 불필요해지는 것이 아니라, block 반환 시점 이후에 join이 일어나야 하므로 의미론적 혼란)

**결정**: `autoJoin` 파라미터 **도입하지 않음** (YAGNI).

### 3.4 중복 제거 및 사용 가이드

#### 3.4.1 Deprecation 계획

| 기존 API | 새 API | 위치 | Deprecated 시점 |
|----------|--------|------|----------------|
| `StructuredTaskScopes.all()` | `StructuredTaskScopes.failFast()` | `virtualthread/api` | 이번 PR |
| `StructuredTaskScopes.any()` | `StructuredTaskScopes.firstSuccess()` | `virtualthread/api` | 이번 PR |
| `structuredTaskScopeAll {}` | `structuredTaskScopeFailFast {}` | `bluetape4k/core` | 이번 PR |
| `structuredTaskScopeAny {}` | `structuredTaskScopeFirstSuccess {}` | `bluetape4k/core` | 이번 PR |

> **Provider SPI 제외**: `StructuredTaskScopeProvider.withAll()`/`withAny()`는 deprecated 대상에서 **제외**한다.
> jdk21/jdk25 구현체가 `override`하는 SPI 메서드이므로, deprecated 표시 시 구현체와 외부 Provider 구현자에게 경고가 전파된다.
> 대신 KDoc에 `@see withFailFast`/`@see withFirstSuccess` 안내만 추가한다.

**제거 시점**: 다음 major 버전 (2.0) — 현재 PR에서는 제거하지 않음.

#### 3.4.2 README 결정 트리

`virtualthread/api/README.md` 및 `README.ko.md`에 다음 결정 트리 추가:

```
## API 선택 가이드 (Decision Tree)

어떤 API를 사용해야 할까?

┌─ Coroutines 기반 코드인가?
│  ├─ YES → `Dispatchers.VT` 또는 `withVirtualContext {}`
│  │         (코루틴 스케줄러로 가상 스레드 활용)
│  └─ NO ──┐
│           │
├─ 여러 작업을 동시에 실행하고 구조적으로 관리해야 하는가?
│  ├─ YES ──┐
│  │        ├─ 모두 성공해야 하는가? (하나 실패 시 전체 중단)
│  │        │  └─ `StructuredTaskScopes.failFast { scope -> ... }`
│  │        │
│  │        └─ 가장 먼저 성공한 결과만 필요한가?
│  │           └─ `StructuredTaskScopes.firstSuccess { scope -> ... }`
│  │
│  └─ NO ──┐
│           │
├─ 단일 비동기 작업을 Future로 실행하고 싶은가?
│  └─ YES → `virtualFuture { ... }` (VirtualFuture<T> 반환)
│
└─ Java ExecutorService와 통합해야 하는가?
   └─ YES → `VirtualThreads.executorService()` (.use { } 권장)
```

---

## 4. 변경 파일 목록

### virtualthread/api (`StructuredScopes.kt`)

| 변경 | 설명 |
|------|------|
| `typealias StructuredTaskScopeFailFast` 추가 | `= StructuredTaskScopeAll` |
| `typealias StructuredTaskScopeFirstSuccess<T>` 추가 | `= StructuredTaskScopeAny<T>` |
| `StructuredSubtask.getOrNull()` default 메서드 추가 | 안전한 null 반환 |
| `StructuredSubtask.get()` KDoc 보강 | 상태별 동작 명시 |
| `StructuredTaskScopeAll` KDoc에 `@see StructuredTaskScopeFailFast` 추가 | 마이그레이션 안내 |
| `StructuredTaskScopeAny` KDoc에 `@see StructuredTaskScopeFirstSuccess` 추가 | 마이그레이션 안내 |
| `StructuredTaskScopeAll.join()` KDoc에 타임아웃 경고 추가 | 안정성 |
| `StructuredTaskScopeProvider.withFailFast()` default 메서드 추가 | `withAll()` 위임 |
| `StructuredTaskScopeProvider.withFirstSuccess()` default 메서드 추가 | `withAny()` 위임 |
| `StructuredTaskScopeProvider.withAll()` KDoc에 `@see withFailFast` 추가 | 마이그레이션 안내 (deprecated 하지 않음 — SPI) |
| `StructuredTaskScopeProvider.withAny()` KDoc에 `@see withFirstSuccess` 추가 | 마이그레이션 안내 (deprecated 하지 않음 — SPI) |
| `StructuredTaskScopes.failFast()` 추가 (factory 기본값 포함) | 새 진입점 |
| `StructuredTaskScopes.firstSuccess()` 추가 (factory 기본값 포함) | 새 진입점 |
| `StructuredTaskScopes.all()` `@Deprecated` 추가 | 마이그레이션 |
| `StructuredTaskScopes.any()` `@Deprecated` 추가 | 마이그레이션 |

### virtualthread/api (테스트)

| 변경 | 설명 |
|------|------|
| `StructuredScopesTest` — `failFast`/`firstSuccess` 경로 테스트 추가 | 새 API 커버리지 |
| `getOrNull()` 테스트 추가 | SUCCESS/FAILED/UNAVAILABLE 각 상태 |

### virtualthread/jdk21 (`Jdk21StructuredTaskScopeProvider.kt`)

| 변경 | 설명 |
|------|------|
| 변경 없음 | `withFailFast()`/`withFirstSuccess()`는 Provider 인터페이스 default 메서드로 위임 |

### virtualthread/jdk25 (`Jdk25StructuredTaskScopeProvider.kt`)

| 변경 | 설명 |
|------|------|
| 변경 없음 | `withFailFast()`/`withFirstSuccess()`는 Provider 인터페이스 default 메서드로 위임 |

### bluetape4k/core (`StructuredTaskScopeSupport.kt`)

| 변경 | 설명 |
|------|------|
| `structuredTaskScopeFailFast {}` 추가 | 새 편의 함수 |
| `structuredTaskScopeFirstSuccess {}` 추가 | 새 편의 함수 |
| `structuredTaskScopeAll {}` `@Deprecated` 추가 | 마이그레이션 |
| `structuredTaskScopeAny {}` `@Deprecated` 추가 | 마이그레이션 |

### README 파일

| 파일 | 변경 | 근거 |
|------|------|------|
| `virtualthread/api/README.md` | 결정 트리 추가 + API 레퍼런스에서 `all`/`any` → `failFast`/`firstSuccess` 교체 | 주 문서 |
| `virtualthread/api/README.ko.md` | 결정 트리 추가 + API 레퍼런스에서 `all`/`any` → `failFast`/`firstSuccess` 교체 | 주 문서 (한국어) |
| `virtualthread/jdk21/README.md` | 예제 코드 `StructuredTaskScopes.all` → `StructuredTaskScopes.failFast` 교체 | 실제 사용 예제 포함 (line 187) |
| `virtualthread/jdk21/README.ko.md` | 예제 코드 `StructuredTaskScopes.all` → `StructuredTaskScopes.failFast` 교체 | 실제 사용 예제 포함 (line 186) |
| `virtualthread/jdk25/README.md` | 예제 코드 `StructuredTaskScopes.all` → `StructuredTaskScopes.failFast` 교체 | 실제 사용 예제 포함 (line 214) |
| `virtualthread/jdk25/README.ko.md` | 예제 코드 `StructuredTaskScopes.all` → `StructuredTaskScopes.failFast` 교체 | 실제 사용 예제 포함 (line 212) |
| `virtualthread/README.md` | 상위 모듈 개요 — 새 API 이름 반영 | 모듈 루트 README |
| `virtualthread/README.ko.md` | 상위 모듈 개요 — 새 API 이름 반영 (한국어) | 모듈 루트 README |

---

## 5. 작업 순서 (Task List)

| # | 태스크 | 모듈 | 의존성 |
|---|--------|------|--------|
| T1 | `StructuredSubtask.getOrNull()` default 메서드 추가 + `get()` KDoc 보강 | `virtualthread/api` | — *(T2와 병렬 실행 가능)* |
| T2 | `typealias` 추가 (`StructuredTaskScopeFailFast`, `StructuredTaskScopeFirstSuccess`) | `virtualthread/api` | — *(T1과 병렬 실행 가능)* |
| T3 | `StructuredTaskScopeProvider`에 `withFailFast()`/`withFirstSuccess()` default 메서드 추가 + `withAll`/`withAny` KDoc `@see` 안내 추가 (deprecated 아님) | `virtualthread/api` | T2 |
| T4 | `StructuredTaskScopes`에 `failFast()`/`firstSuccess()` 추가 (factory 기본값) + `all`/`any` `@Deprecated` | `virtualthread/api` | T3 |
| T5 | `StructuredTaskScopeAll`/`StructuredTaskScopeAny` KDoc 보강 (`@see`, join 타임아웃 경고) | `virtualthread/api` | T2 |
| T6 | API 모듈 테스트: `getOrNull()`, `failFast()`, `firstSuccess()` 테스트 추가 | `virtualthread/api` | T1,T4 |
| T7 | `structuredTaskScopeFailFast {}`/`structuredTaskScopeFirstSuccess {}` 추가 + 기존 deprecated | `bluetape4k/core` | T4 |
| T8 | core 모듈 테스트: 새 편의 함수 테스트 추가 | `bluetape4k/core` | T7 |
| T9 | 전체 빌드 검증: `./gradlew :bluetape4k-virtualthread-api:test :bluetape4k-virtualthread-jdk21:test :bluetape4k-virtualthread-jdk25:test :bluetape4k-core:test` | — | T6,T8 |
| T10 | README 업데이트: 결정 트리 추가 및 API 예제 교체 (8개 파일: api×2, jdk21×2, jdk25×2, root×2) | `virtualthread/*` | T4 |

---

## 6. DoD (Definition of Done)

- [ ] `virtualthread/api` — `typealias StructuredTaskScopeFailFast`, `StructuredTaskScopeFirstSuccess` 추가
- [ ] `virtualthread/api` — `StructuredTaskScopeProvider.withFailFast()`/`withFirstSuccess()` default 메서드 추가
- [ ] `virtualthread/api` — `StructuredTaskScopes.failFast()`/`firstSuccess()` 추가 (factory 기본값 포함)
- [ ] `virtualthread/api` — `all()`/`any()`에 `@Deprecated(message, ReplaceWith)` 추가
- [ ] `virtualthread/api` — `withAll()`/`withAny()`에 `@see` KDoc 안내 추가 (deprecated 하지 않음 — SPI 안정성 유지)
- [ ] `virtualthread/jdk21` — 변경 없음 확인 (기존 테스트 그대로 통과)
- [ ] `virtualthread/jdk25` — 변경 없음 확인 (기존 테스트 그대로 통과)
- [ ] `bluetape4k/core` — `structuredTaskScopeFailFast {}`/`structuredTaskScopeFirstSuccess {}` 추가
- [ ] `bluetape4k/core` — 기존 `structuredTaskScopeAll`/`structuredTaskScopeAny`에 `@Deprecated(message, ReplaceWith)` 추가
- [ ] `StructuredSubtask.getOrNull()` default 메서드 구현 + 테스트
- [ ] `StructuredSubtask.get()` / `StructuredTaskScopeAll.join()` KDoc 보강
- [ ] 기존 테스트 전부 통과 (회귀 없음)
- [ ] 새 API 테스트 추가 (`failFast`, `firstSuccess`, `getOrNull`)
- [ ] KDoc 갱신 (모든 public API — 새 추가 + 기존 보강)
- [ ] README.md / README.ko.md 결정 트리 추가 및 API 예제 교체 (8개 파일: `virtualthread/api` ×2, `virtualthread/jdk21` ×2, `virtualthread/jdk25` ×2, `virtualthread/` root ×2)
- [ ] `./gradlew :bluetape4k-virtualthread-api:test :bluetape4k-virtualthread-jdk21:test :bluetape4k-virtualthread-jdk25:test :bluetape4k-core:test` 통과
- [ ] `./gradlew :bluetape4k-virtualthread-jdk21:test` 통과
- [ ] `./gradlew :bluetape4k-core:test` 통과
- [ ] 브레이킹 체인지 없음 확인 (`@Deprecated`만 추가, 삭제 없음)

---

## 7. 변경하지 않는 것 (Out of Scope)

| 항목 | 이유 |
|------|------|
| `VirtualThreadRuntime`/`VirtualThreads` 이름 변경 | 현재 명확함, 개선 불필요 |
| `VirtualFuture`/`VirtualFutureExtensions` 변경 | 이번 이슈 범위 밖 |
| `VirtualThreadDispatcher`/`VirtualThreadReactorScheduler` 변경 | 이번 이슈 범위 밖 |
| `autoJoin` 파라미터 | YAGNI (섹션 3.3.3 참조) |
| join() warn 로그 | 노이즈 과다 (섹션 3.3.2 참조) — KDoc 경고로 대체 |
| `CoroutineSupport.kt` 변경 | 이름 명확, 변경 불필요 |
| `examples/` 코드 마이그레이션 | deprecated 경고만 발생, 별도 PR로 처리 가능 |
| `utils/workflow`, `testing/junit5` 마이그레이션 | deprecated 경고만 발생, 별도 후속 작업 |

---

## 8. 리스크 및 완화

| 리스크 | 확률 | 영향 | 완화 |
|--------|------|------|------|
| 기존 인터페이스를 deprecated하지 않는 한 기존 이름(`StructuredTaskScopeAll`) 사용자에게 경고를 줄 수 없음 (`@Deprecated typealias`는 새 이름에 경고를 붙이는 반대 방향이라 실익 없음) | 확실 | 낮음 | KDoc `@see StructuredTaskScopeFailFast` + 진입 함수(`all`/`any`) deprecated로 대체 효과 확보 |
| Provider default 메서드 추가 시 외부 구현체 영향 | 매우 낮음 | 낮음 | default 구현 제공으로 기존 구현체 변경 불필요 |
| `getOrNull()` default 메서드의 UNAVAILABLE 상태 판별 | 낮음 | 중간 | `StructuredTaskScope.Subtask.State.SUCCESS`만 체크, 나머지 전부 null 반환 |
