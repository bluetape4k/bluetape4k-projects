# bluetape4k-assertions 설계 스펙

**Issue**: #322 **날짜**: 2026-05-06 **상태**: DRAFT **대상 모듈**: `testing/assertions/` → Gradle 등록명 `bluetape4k-assertions`
**기본 패키지**: `io.bluetape4k.assertions`

---

## 1. 배경 및 목적

### 1.1 배경

bluetape4k 프로젝트는 테스트 작성에 광범위하게 [bluetape4k-assertions](https://github.com/bluetape4k-assertions/bluetape4k-assertions)
fluent assertion 라이브러리를 사용해왔다. 그러나 bluetape4k-assertions는 다음과 같은 문제를 안고 있다.

- **유지보수 중단**: 2023년 이후 사실상 신규 릴리스가 끊겼고, Kotlin 2.x / JUnit Jupiter 최신 버전 대응이 정체되어 있다.
- **JUnit 4 잔재**: `ComparisonFailedException`이 JUnit 4에 종속되어 있어, JUnit 5의 `AssertionFailedError`
  기반 IDE diff viewer를 충분히 활용하지 못한다.
- **Thread-local
  기반 `assertSoftly`**: Kotlin 가상 스레드 (JEP 444 / JDK 21+) 환경에서 thread-local pinning과 carrier-thread leak 위험을 가지고 있다.
- **백틱 API 중복**: `shouldBeEqualTo` / `should be equal to` 두 형식이 공존해 IntelliJ 자동완성 및 detekt 경고가 늘어난다.

bluetape4k는 모든 모듈이 JVM 21+ / Kotlin 2.3+ / JUnit Jupiter 5.10+를 강제하므로, 더 이상 bluetape4k-assertions의 호환 부담을 안고 갈 필요가 없다.

### 1.2 목적

`bluetape4k-assertions` 모듈을 신설하여 다음을 달성한다.

1. **드롭인 대체**: 기존 `io.bluetape4k.assertions.*` import만 `io.bluetape4k.assertions.*`로 바꾸면 대부분의 테스트 코드가 컴파일/통과되도록 한다.
2. **JUnit 5 일급
   연동**: `org.opentest4j.AssertionFailedError`를 직접 던져 IntelliJ / Gradle / Surefire 모두에서 expected/actual diff를 보여준다.
3. **가상 스레드 안전**: `assertSoftly`가 thread-local에 의존하지 않도록 `Assertions.assertAll()`을 위임 사용한다.
4. **Coroutines/Flow 지원의
   일원화**: `bluetape4k-coroutines`에 흩어져 있던 Flow assertion을 본 모듈로 모으고, 기존 위치는 `@Deprecated` bridge로 둔다.
5. **Turbine 옵션**: Turbine 통합 헬퍼는 `compileOnly`로만 노출하여 사용자가 명시적으로 의존성을 추가했을 때만 활성화되도록 한다.

### 1.3 성공 지표 (Acceptance Signals)

- 기존 bluetape4k 테스트 코드 100개 이상에서 `import io.bluetape4k.assertions.*` →
  `import io.bluetape4k.assertions.*` 단순 치환만으로 컴파일이 성공한다.
- `./gradlew :bluetape4k-assertions:build` 통과, `./gradlew :bluetape4k-coroutines:test` 통과.
- 256 concurrent 가상 스레드에서 `assertSoftly` 사용 시 deadlock/crash 없음.
- IntelliJ에서 실패한 assertion이 expected/actual side-by-side diff로 표시된다.

---

## 2. 범위 (Scope)

### 2.1 In Scope (v1)

- `Basic`, `Numerical`, `CharSequences`, `Collections`, `Arrays`, `Maps`, `Exceptions`,
  `Reflection`, `DateTimes`, `Softly`, `coroutines/FlowAssertions`, `coroutines/TurbineSupport`.
- DateTime 지원 타입: `Instant`, `ZonedDateTime`, `OffsetDateTime`, `LocalDateTime`, `LocalDate`,
  `LocalTime`, `java.util.Date`.
- bluetape4k-assertions Keep 목록 (§4) 전체 함수의 1:1 미러링.
- Korean KDoc 작성.
- Mermaid UML이 포함된 `README.md` + `README.ko.md`.

### 2.2 Out of Scope (v1, 후속 버전 검토)

- 백틱 형 API (`should be equal to` 등) — 의도적 제외.
- bluetape4k-assertions의 `Equivalency` / `EquivalencyAssertionOptions` — 객체 그래프 비교는 v2 검토.
- File / Path assertion (`shouldExist`, `shouldBeFile` 등) — v2.
- `Char.shouldBeDigit` / `shouldNotBeDigit` — v2.
- AnyException / AnyExceptionType sentinel — `Throwable::class`로 대체.
- ComparisonFailedException (JUnit4) — 사용하지 않음.
- 멀티플랫폼 (KMP) 지원 — JVM 전용.

### 2.3 Non-goals

- Kotest Assertion 라이브러리와의 매핑은 제공하지 않는다.
- Spring/MockK/Awaitility와의 통합 모듈 분리는 본 스펙의 범위가 아니다.

---

## 3. 설계 결정 (Architecture Decisions)

본 절은 변경 불가능한 결정 사항을 기록한다.

### ADR-1: API 호환 전략 — bluetape4k-assertions 이름 그대로 미러링

-

**결정**: bluetape4k-assertions의 백틱 없는 카멜케이스 이름 (`shouldBeEqualTo`, `shouldContain` 등)을 그대로 사용한다. 시그니처도 bluetape4k-assertions와 동일하게 유지하되, 반환 타입이 bluetape4k-assertions에서 receiver였다면 본 모듈에서도 동일하게 receiver를 반환한다 (chaining 지원).
- **근거**: 마이그레이션 비용을 최소화한다. `import` 줄 한 줄만 바꾸면 되어야 한다.
- **결과**: API 표면은 bluetape4k-assertions와 거의 동일. 단, 의도적 편차 (§5.2)는 명시한다.

### ADR-2: 실패 처리 — `org.opentest4j.AssertionFailedError` 직접 생성

- **결정**: 모든 assertion 실패는 다음 중 하나로 던진다.
    - `AssertionFailedError(message, expected, actual)` — 값 비교 (`shouldBeEqualTo` 등).
    - `AssertionFailedError(message)` — 단순 조건 실패 (`shouldBeTrue` 등).
- **근거**: IntelliJ / Gradle / Surefire 모두 opentest4j를 인식해 diff viewer를 띄운다.
- **결과**: JUnit 4 `ComparisonFailedException`은 사용하지 않는다.

### ADR-3: `assertSoftly` — JUnit 5 `Assertions.assertAll()` 위임

-

**결정**: `assertSoftly { add { ... } }` DSL을 제공하되, 내부적으로 `Executable` 리스트를 쌓아 `org.junit.jupiter.api.Assertions.assertAll(executables)`에 위임한다.
-
**근거**: thread-local 기반의 bluetape4k-assertions 구현은 가상 스레드에서 carrier pinning을 일으킬 수 있다. JUnit 5 표준 API는 직렬 실행이고 어떤 스레드에서 호출해도 안전하다.

- **결과**: bluetape4k-assertions의 `verify { }` 자동 누적 방식과 달리, 본 모듈은 `add { ... }` 명시적 등록을 요구한다 (§5.2 참조).

### ADR-4: Turbine — `compileOnly` 의존성

-

**결정**: `app.cash.turbine:turbine`은 `compileOnly` 스코프로 선언한다. 사용자는 별도로 Turbine을 testImplementation에 추가해야 `TurbineSupport.kt`의 함수를 호출할 수 있다.
- **근거**: Turbine을 사용하지 않는 모듈에 강제로 의존성을 끌어오지 않기 위함.
- **결과**: `coroutines/TurbineSupport.kt`는 별도 파일로 분리되어 있으며, 사용자가 Turbine을 classpath에 추가하지 않으면 해당 함수만 컴파일 오류가 난다 (격리).

### ADR-5: DateTime 지원 범위

- **결정**: v1에 포함되는 타입은 `Instant`, `ZonedDateTime`, `OffsetDateTime`,
  `LocalDateTime`, `LocalDate`, `LocalTime`, `java.util.Date`. 각 타입에 대해
  `shouldBeAfter`, `shouldBeBefore`, `shouldBeOnOrAfter`, `shouldBeOnOrBefore`를 제공한다.
- **근거**: bluetape4k-assertions에는 일부 타입만 있었으나, bluetape4k는 모든 표준 java.time 타입과 legacy
  `Date`까지 일관되게 다룬다.
- **결과**: 7 타입 × 4 함수 = 28개 DateTime assertion.

### ADR-6: FlowAssertions 이전 + Bridge (단일 구현 우선)

- **결정**: **새 assertions 모듈이 정식 (canonical) API 위치다.**
    - `bluetape4k-assertions`의 `io.bluetape4k.assertions.coroutines.FlowAssertions.kt`가 공식 사용 위치.
    - `bluetape4k-coroutines`의 기존 `FlowAssertions.kt`는 독립적인 호환 복사본으로 유지되며,
      `@Deprecated(level = WARNING)` 마커를 추가한다. **함수 본문은 기존 구현 그대로 유지한다**
      (inline suspend 함수는 위임 불가 — §4.9 bridge 예시 참조).
    - `bluetape4k-coroutines`는 `bluetape4k-assertions`에 **의존성을 추가하지
      않는다**. bridge는 기존 bluetape4k-assertions 의존성만으로 독립 동작.
- **이유**: inline suspend 함수는 다른 모듈로 위임 불가이며,
  `bluetape4k-coroutines` → `bluetape4k-assertions` 의존성 추가 시
  `junit-jupiter-api` / `opentest4j`가 production classpath에 오염된다.
- **마이그레이션 전략**: 사용자는 `import io.bluetape4k.coroutines.tests.assertResult` →
  `import io.bluetape4k.assertions.coroutines.assertResult` 로 변경. bridge는 1 마이너 버전 유지 후 제거.
- **모듈 아키텍처**: `bluetape4k-assertions`는 독립 신규 모듈.
  `bluetape4k-junit5`에서 `api(project(":bluetape4k-assertions"))` 추가로 기존 사용자는 의존성 변경 없이 새 assertions 접근 가능.

### ADR-7: Contracts 적용 범위

- **결정**: `kotlin.contracts.contract` 적용은 다음 두 함수에 한정한다.
    - `shouldNotBeNull()` — `returns() implies (this@shouldNotBeNull != null)`.
    - `shouldBeInstanceOf<T>()` — `returns() implies (this@shouldBeInstanceOf is T)`.
- **근거**: smart-cast 효과가 의미 있는 곳에만 적용. 함수 레벨 `@OptIn(ExperimentalContracts::class)`로 격리한다.
- **결과**: 그 외 함수는 contract 없이 정상 반환.

### ADR-8: 메시지 포맷 — `internal/Messages.kt` 단일 책임

- **결정**: 실패 메시지 포맷팅은 `io.bluetape4k.assertions.internal.Messages.kt`에 모은다.
  ```
  Expected <subject> to <verb> <expected>, but was <actual>.
  ```
  형식을 기본으로 한다.
- **근거**: 메시지 일관성. 향후 i18n 또는 컬러링 도입 시 단일 지점에서 변경.

### ADR-9: 모듈 의존성 — bluetape4k-* 모듈 의존 금지

- **결정**: `bluetape4k-assertions`는 어떤 `bluetape4k-*` 모듈에도 의존하지 않는다.
  `bluetape4k-logging`, `bluetape4k-core`, `bluetape4k-junit5` 모두 **의존 금지**.
  `api` 범위는 이 모듈의 public API를 사용하는 데 반드시 필요한 순수 외부 라이브러리만 허용:
    - `junit.bom` (platform) + `junit.jupiter.api` — `AssertionFailedError` 타입 (api)
    - `opentest4j` — `MultipleFailuresError` 타입 (api)
    - `kotlinx.coroutines.core` — FlowAssertions가 main 소스에 있어 소비자 classpath 필요 (api)
    - `turbine` — TurbineSupport 사용 소비자가 직접 추가 (compileOnly)
- **근거**:
    - assertions 모듈은 bluetape4k 생태계 전체의 최하위 테스팅 기반이므로, 어느 bluetape4k 모듈이 이 모듈을 흡수해도 순환 의존이 발생해선 안 된다.
    - `bluetape4k-logging`을 추가하면 소비자 classpath에 logging 의존성이 강제 전파된다.
    - assertion 함수는 stateless이므로 로깅 자체가 불필요하다.
    - **KLogging 사용
      금지** — `internal/Failures.kt`, `Softly.kt` 등 모든 내부 코드에서 logging을 사용하지 않는다. 예외 메시지와 `AssertionFailedError`만으로 진단 가능.

---

## 4. API 카탈로그

본 절의 모든 함수는 `package io.bluetape4k.assertions`에 속한다 (coroutines 함수만
`io.bluetape4k.assertions.coroutines`).

### 4.1 Basic (`Basic.kt`)

#### 시그니처

```kotlin
infix fun <T> T.shouldBeEqualTo(expected: T?): T
infix fun <T> T.shouldNotBeEqualTo(expected: T?): T
infix fun <T> T.shouldBe(expected: T?): T          // referential equality (===)
infix fun <T> T.shouldNotBe(expected: T?): T

fun Any?.shouldBeNull()
@OptIn(ExperimentalContracts::class)
fun <T : Any> T?.shouldNotBeNull(): T              // contract: returns() implies (this != null)

fun Boolean?.shouldBeTrue(): Boolean
fun Boolean?.shouldBeFalse(): Boolean
fun Boolean?.shouldNotBeTrue(): Boolean?
fun Boolean?.shouldNotBeFalse(): Boolean?

inline fun <T> T.should(message: String = "", predicate: (T) -> Boolean): T
```

#### 사용 예

```kotlin
val name = "Alice"
name shouldBeEqualTo "Alice"
name shouldNotBeEqualTo "Bob"

val a = listOf(1, 2)
val b = a
a shouldBe b                  // 같은 참조여야 통과

val maybe: String? = lookupName()
maybe.shouldNotBeNull()       // 이후 코드에서 maybe는 String으로 smart-cast

(2 + 2 == 4).shouldBeTrue()
"abc".should("must contain b") { "b" in it }
```

#### 실패 메시지 예

```
Expected: "Alice"
Actual:   "Bob"
Expected <"Alice"> to be equal to <"Bob">, but was <"Alice">.
```

#### bluetape4k-assertions 차이점

- `shouldBe`는 referential equality (`===`). bluetape4k-assertions의 `shouldBe`도 동일하나 일부 사용자는
  `shouldBeEqualTo`와 혼동했음 — KDoc에 명시한다.

---

### 4.2 Numerical (`Numerical.kt`)

#### 시그니처 (대표)

```kotlin
infix fun <T : Comparable<T>> T.shouldBeGreaterThan(other: T): T
infix fun <T : Comparable<T>> T.shouldBeGreaterOrEqualTo(other: T): T
infix fun <T : Comparable<T>> T.shouldBeLessThan(other: T): T
infix fun <T : Comparable<T>> T.shouldBeLessOrEqualTo(other: T): T
// + Not* 4개

fun Byte.shouldBePositive(): Byte
fun Short.shouldBePositive(): Short
fun Int.shouldBePositive(): Int
fun Long.shouldBePositive(): Long
fun Float.shouldBePositive(): Float
fun Double.shouldBePositive(): Double
// + shouldBeNegative 6 types

infix fun <T : Comparable<T>> T.shouldBeInRange(range: ClosedRange<T>): T
infix fun <T : Comparable<T>> T.shouldNotBeInRange(range: ClosedRange<T>): T

fun Double.shouldBeNear(expected: Double, tolerance: Double): Double
fun Float.shouldBeNear(expected: Float, tolerance: Float): Float
```

#### 사용 예

```kotlin
val ms = elapsed()
ms shouldBeLessThan 1_000L
ms shouldBeInRange 0L..500L

val ratio = 0.3333
ratio.shouldBeNear(1.0 / 3, tolerance = 1e-6)

42.shouldBePositive()
(-1).shouldBeNegative()
```

#### 실패 메시지 예

```
Expected <100> to be greater than <1000>, but was not.
Expected <0.3333> to be near <0.5> (tolerance: 1.0E-6), but difference was <0.1667>.
```

#### bluetape4k-assertions 차이점

- `shouldBeNear`는 bluetape4k-assertions에서는 `Double, Double, Double` 셋만 받는데, 본 모듈은 명시적으로 named parameter `tolerance`를 받는다 (가독성).

---

### 4.3 CharSequence (`CharSequences.kt`)

#### 시그니처

```kotlin
infix fun <T : CharSequence> T.shouldStartWith(prefix: CharSequence): T
infix fun <T : CharSequence> T.shouldEndWith(suffix: CharSequence): T
infix fun <T : CharSequence> T.shouldContain(substring: CharSequence): T
infix fun <T : CharSequence> T.shouldContainIgnoringCase(substring: CharSequence): T
fun <T : CharSequence> T.shouldNotContain(substring: CharSequence): T
fun <T : CharSequence> T.shouldBeEmpty(): T
fun <T : CharSequence> T.shouldNotBeEmpty(): T
fun <T : CharSequence> T.shouldBeBlank(): T
fun <T : CharSequence> T.shouldNotBeBlank(): T
fun CharSequence?.shouldBeNullOrEmpty(): CharSequence?
fun CharSequence?.shouldNotBeNullOrEmpty(): CharSequence
infix fun <T : CharSequence> T.shouldMatch(regex: Regex): T
infix fun <T : CharSequence> T.shouldMatch(pattern: String): T
infix fun <T : CharSequence> T.shouldNotMatch(regex: Regex): T
infix fun <T : CharSequence> T.shouldContainAll(parts: Iterable<CharSequence>): T
fun <T : CharSequence> T.shouldContainAll(vararg parts: CharSequence): T
fun <T : CharSequence> T.shouldContainNone(parts: Iterable<CharSequence>): T
infix fun <T : CharSequence> T.shouldBeEqualToIgnoringCase(other: CharSequence): T
```

#### 사용 예

```kotlin
val msg = "Hello, World!"
msg shouldStartWith "Hello"
msg shouldEndWith "!"
msg shouldContainIgnoringCase "WORLD"
msg shouldMatch Regex("""^Hello.*!$""")
msg.shouldContainAll("Hello", "World")

val empty: String? = null
empty.shouldBeNullOrEmpty()
```

#### 실패 메시지 예

```
Expected <"Hello"> to start with <"Bye">, but did not.
Expected <"abc"> to match regex <[0-9]+>, but did not.
```

---

### 4.4 Collections + Arrays + Maps (`Collections.kt`, `Arrays.kt`, `Maps.kt`)

#### Collections (Iterable / Array<T>) 시그니처

```kotlin
infix fun <T, C : Iterable<T>> C.shouldContain(element: T): C
infix fun <T, C : Iterable<T>> C.shouldNotContain(element: T): C
infix fun <T, C : Iterable<T>> C.shouldContainAll(elements: Iterable<T>): C
fun <T, C : Iterable<T>> C.shouldContainAll(vararg elements: T): C
infix fun <T, C : Iterable<T>> C.shouldContainAny(elements: Iterable<T>): C
fun <T, C : Iterable<T>> C.shouldContainAny(vararg elements: T): C
infix fun <T, C : Iterable<T>> C.shouldContainNone(elements: Iterable<T>): C
infix fun <C : Collection<*>> C.shouldHaveSize(expected: Int): C
fun <C : Collection<*>> C.shouldBeEmpty(): C
fun <C : Collection<*>> C.shouldNotBeEmpty(): C
infix fun <T> T.shouldBeIn(container: Iterable<T>): T
infix fun <T> T.shouldNotBeIn(container: Iterable<T>): T
inline infix fun <T, C : Iterable<T>> C.shouldMatchAllWith(predicate: (T) -> Boolean): C
inline infix fun <T, C : Iterable<T>> C.shouldMatchAtLeastOneOf(predicate: (T) -> Boolean): C
```

#### Arrays.kt — primitive array overloads

```kotlin
fun IntArray.shouldContain(value: Int): IntArray
fun IntArray.shouldNotContain(value: Int): IntArray
fun IntArray.shouldHaveSize(expected: Int): IntArray
fun IntArray.shouldBeEmpty(): IntArray
fun IntArray.shouldNotBeEmpty(): IntArray
// + LongArray, DoubleArray, FloatArray, ShortArray, ByteArray, CharArray, BooleanArray
```

#### Maps.kt 시그니처

```kotlin
infix fun <K, V, M : Map<K, V>> M.shouldContainKey(key: K): M
infix fun <K, V, M : Map<K, V>> M.shouldContainValue(value: V): M
infix fun <K, V, M : Map<K, V>> M.shouldContain(pair: Pair<K, V>): M
infix fun <K, V, M : Map<K, V>> M.shouldHaveSize(expected: Int): M
fun <K, V, M : Map<K, V>> M.shouldBeEmpty(): M
fun <K, V, M : Map<K, V>> M.shouldNotBeEmpty(): M
```

#### 사용 예

```kotlin
val users = listOf("alice", "bob", "carol")
users shouldContain "alice"
users shouldContainAll listOf("alice", "bob")
users.shouldContainAll("alice", "bob")
users shouldHaveSize 3
users.shouldMatchAllWith { it.length >= 3 }

val ints = intArrayOf(1, 2, 3)
ints.shouldContain(2)
ints.shouldHaveSize(3)

val cfg = mapOf("host" to "localhost", "port" to "8080")
cfg shouldContainKey "host"
cfg shouldContain ("port" to "8080")
```

#### 실패 메시지 예

```
Expected collection [alice, bob] to contain <carol>, but did not.
Expected collection size to be <3>, but was <2>.
```

#### bluetape4k-assertions 차이점

- `shouldContainSame` 는 v1에서 제외 (bluetape4k-assertions의 의미가 모호 — `==` vs `containsAll && size`). 대안: `(actual.toSet() shouldBeEqualTo expected.toSet())`.

---

### 4.5 Exceptions (`Exceptions.kt`)

#### 시그니처

```kotlin
class InvokingBlock(val block: () -> Any?)
fun invoking(block: () -> Any?): InvokingBlock

class CoInvokingBlock(val block: suspend () -> Any?)
fun coInvoking(block: suspend () -> Any?): CoInvokingBlock

infix fun <T : Throwable> InvokingBlock.shouldThrow(expected: KClass<T>): T
fun InvokingBlock.shouldNotThrow(): Any?
suspend infix fun <T : Throwable> CoInvokingBlock.shouldThrow(expected: KClass<T>): T
suspend fun CoInvokingBlock.shouldNotThrow(): Any?

infix fun <T : Throwable> T.withMessage(message: String): T
infix fun <T : Throwable> T.withCause(cause: KClass<out Throwable>): T
inline fun <T : Throwable> T.with(block: T.() -> Unit): T
```

#### 사용 예

```kotlin
val ex = invoking { repository.get(invalidId) } shouldThrow IllegalArgumentException::class
ex withMessage "id must be > 0"

invoking { service.compute(0) }
    .shouldThrow(ArithmeticException::class)
    .withCause(IOException::class)
    .with { message shouldContain "divide" }

coInvoking { suspendingRepo.fetch() } shouldThrow CancellationException::class

invoking { harmlessOp() }.shouldNotThrow()
```

#### 실패 메시지 예

```
Expected block to throw <IllegalArgumentException>, but threw <NullPointerException>.
Expected block to throw <IllegalArgumentException>, but no exception was thrown.
Expected exception message <"id must be > 0">, but was <"id is null">.
```

#### 의미론 (명시적 계약)

-

**`withMessage(message)`**: 정확한 문자열 일치 (`exception.message == message`). 부분 일치는 `withMessageContaining(substring)` 또는 `withMessageMatching(regex)` 사용.
- **`withCause(klass)`**: assignable 매칭 (`klass.isInstance(exception.cause)`).
  `cause == null`이면 assertion 실패 (명확한 메시지 포함).
- **`CancellationException` 처리**: `coInvoking { }.shouldThrow(SomeException::class)` 에서
  `SomeException`이 `CancellationException`이 아닌데 실제로 `CancellationException`이 던져지면 **즉시
  rethrow** (잡지 않음). 이는 coroutine cancellation을 숨기지 않기 위함.

#### bluetape4k-assertions 차이점

- `AnyException` / `AnyExceptionType` sentinel은 제거. 임의의 예외를 잡으려면
  `Throwable::class`를 사용한다. KDoc에 명시.
- `withMessageContaining(substring)`, `withMessageMatching(regex)` 오버로드 추가 (bluetape4k-assertions에 없음).

---

### 4.6 Reflection (`Reflection.kt`)

#### 시그니처

```kotlin
@OptIn(ExperimentalContracts::class)
inline fun <reified T : Any> Any?.shouldBeInstanceOf(): T
// contract: returns() implies (this@shouldBeInstanceOf is T)

inline fun <reified T : Any> Any?.shouldNotBeInstanceOf(): Any?

infix fun Any?.shouldBeInstanceOf(klass: KClass<*>): Any?  // non-reified variant
infix fun Any?.shouldNotBeInstanceOf(klass: KClass<*>): Any?
```

#### 사용 예

```kotlin
val raw: Any = "hello"
val s: String = raw.shouldBeInstanceOf<String>()   // smart-cast via contract
s.length shouldBeGreaterThan 0

raw shouldBeInstanceOf String::class               // KClass overload
```

#### 실패 메시지 예

```
Expected <"hello"> to be instance of <java.lang.Integer>, but was <java.lang.String>.
```

---

### 4.7 DateTime (`DateTimes.kt`)

#### 지원 타입

`Instant`, `ZonedDateTime`, `OffsetDateTime`, `LocalDateTime`, `LocalDate`, `LocalTime`,
`java.util.Date`.

#### 시그니처 패턴 (Instant 예시 — 다른 타입도 동일 패턴)

```kotlin
infix fun Instant.shouldBeAfter(other: Instant): Instant
infix fun Instant.shouldBeBefore(other: Instant): Instant
infix fun Instant.shouldBeOnOrAfter(other: Instant): Instant
infix fun Instant.shouldBeOnOrBefore(other: Instant): Instant
```

총 7 타입 × 4 함수 = 28개 함수. 각각 동일 receiver 반환.

#### 사용 예

```kotlin
val now = Instant.now()
val past = now.minusSeconds(10)
val future = now.plusSeconds(10)

now shouldBeAfter past
now shouldBeBefore future
now shouldBeOnOrAfter now
now shouldBeOnOrBefore future

val today = LocalDate.now()
today shouldBeAfter today.minusDays(1)

val date = Date()
date shouldBeBefore Date(date.time + 1000)
```

#### 실패 메시지 예

```
Expected <2026-05-06T10:00:00Z> to be after <2026-05-06T11:00:00Z>, but was not.
```

#### bluetape4k-assertions 차이점

- bluetape4k-assertions는 `LocalDate`, `LocalDateTime`만 지원했지만 본 모듈은 `Instant` / `ZonedDateTime` /
  `OffsetDateTime` / `LocalTime` / `Date`까지 포함한다.

---

### 4.8 Softly (`Softly.kt`)

#### 시그니처

```kotlin
class SoftAssertionScope {
    private val executables = mutableListOf<Executable>()
    fun add(block: () -> Unit) { executables += Executable { block() } }
    @PublishedApi internal fun executables(): List<Executable> = executables
}

inline fun assertSoftly(block: SoftAssertionScope.() -> Unit) {
    val scope = SoftAssertionScope().apply(block)
    Assertions.assertAll(scope.executables())
}
```

#### 사용 예

```kotlin
assertSoftly {
    add { user.name shouldBeEqualTo "alice" }
    add { user.age shouldBeGreaterOrEqualTo 18 }
    add { user.email shouldEndWith "@example.com" }
}
```

여러 assertion이 한 번에 평가되어, 모든 실패가 `MultipleFailuresError`로 묶여 표시된다.

#### 실패 메시지 예

```
org.opentest4j.MultipleFailuresError: Multiple Failures (2 failures)
  Expected <"alex"> to be equal to <"alice">, but was <"alex">.
  Expected <16> to be greater or equal to <18>, but was not.
```

#### bluetape4k-assertions 차이점 (의도적)

- bluetape4k-assertions는 `assertSoftly { user.name shouldBeEqualTo "alice" }` 스타일로 thread-local에 자동 누적하지만, 본 모듈은 명시적으로 `add { ... }`를 호출해야 한다.
- **이유**: thread-local 기반 자동 누적은 가상 스레드에서 carrier pinning을 일으킬 수 있고, 중첩 호출 시 의도와 다르게 동작한다. 명시적 등록이 더 안전하고 가독성이 좋다.
-

**마이그레이션**: 기존 bluetape4k-assertions `assertSoftly { foo shouldBe bar }` → `assertSoftly { add { foo shouldBe bar } }`. 자동 변환 가능 (정규식 또는 IDE structural search).

---

### 4.9 Coroutines (Flow) (`coroutines/FlowAssertions.kt`)

#### 시그니처

```kotlin
suspend fun <T> Flow<T>.assertEmpty()
suspend fun <T> Flow<T>.assertResult(expected: Flow<T>)
suspend fun <T> Flow<T>.assertResult(vararg values: T)
suspend fun <T> Flow<T>.assertResultSet(vararg values: T)
suspend fun <T> Flow<T>.assertResultSet(values: Iterable<T>)
suspend inline fun <T, reified E : Throwable> Flow<T>.assertFailure(vararg values: T)
suspend inline fun <reified E : Throwable> Flow<*>.assertError()
```

본 함수들은 기존 `bluetape4k-coroutines`의 `FlowAssertions.kt`에서 그대로 이전된다. 구현은 새 모듈의 assertion DSL (`shouldBeEmpty`, `shouldBeEqualTo`, `shouldBeInstanceOf`)을 재호출하도록 변경한다.

#### 사용 예

```kotlin
@Test
fun `flow emits expected sequence`() = runTest {
    flowOf(1, 2, 3).assertResult(1, 2, 3)
    flowOf(2, 1, 2).assertResultSet(1, 2)
    emptyFlow<Int>().assertEmpty()
}

@Test
fun `flow fails with expected exception`() = runTest {
    flow {
        emit(1)
        throw IllegalStateException("oops")
    }.assertFailure<Int, IllegalStateException>(1)
}
```

#### Bridge (deprecated, 기존 위치 유지 — ADR-6 적용)

> ⚠️ **ADR-6 주의**: bridge 함수 본문은 기존 구현 그대로 유지한다.
> 새 모듈로 위임 (`io.bluetape4k.assertions.coroutines.*` 호출)하지 않는다.
> inline suspend 함수는 cross-module 위임 불가이며,
> `bluetape4k-coroutines`에 `bluetape4k-assertions` 의존성을 추가하면
> production classpath에 JUnit/opentest4j가 오염된다.

```kotlin
// bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/tests/FlowAssertions.kt
@Deprecated(
    message = "Moved to io.bluetape4k.assertions.coroutines package",
    replaceWith = ReplaceWith("assertResult(*values)", "io.bluetape4k.assertions.coroutines.assertResult"),
    level = DeprecationLevel.WARNING
)
suspend inline fun <T> Flow<T>.assertResult(vararg values: T) {
    // ✅ 본문은 기존 구현 그대로 유지 — 새 모듈 위임 금지 (ADR-6)
    val result = toList()
    assertEquals(values.toList(), result)
}
// ... 7개 함수 모두 동일 패턴 (각 함수의 원래 구현 본문 유지)
```

위 bridge는 **임시**(1 minor 버전 후 제거 예정)이며, level = WARNING으로 컴파일은 통과시킨다.

---

### 4.10 Turbine 통합 (`coroutines/TurbineSupport.kt`)

#### 시그니처

```kotlin
// app.cash.turbine.ReceiveTurbine 가 classpath에 있어야 컴파일됨.
suspend fun <T> ReceiveTurbine<T>.awaitItemAndAssert(expected: T): T
suspend fun <T> ReceiveTurbine<T>.awaitItemMatching(predicate: (T) -> Boolean): T
suspend inline fun <reified E : Throwable> ReceiveTurbine<*>.awaitErrorOfType(): E
```

#### 사용 예

```kotlin
// 사용자의 build.gradle.kts에 testImplementation(libs.turbine) 필요
viewModel.state.test {
    awaitItemAndAssert(initialState)
    viewModel.load()
    awaitItem().isLoading.shouldBeTrue()
    awaitItemMatching { it.items.isNotEmpty() }
}
```

#### 동작 보장

- Turbine이 classpath에 없으면 본 파일의 함수만 compile 오류 (`ReceiveTurbine` 미해결).
- 다른 assertion 모듈 코드는 정상 컴파일/사용 가능.

---

### 4.11 kotlin.test 포팅 (`KotlinTestSupport.kt`, `Arrays.kt` 확장)

kotlin.test (`org.jetbrains.kotlin:kotlin-test`)가 제공하는 assertion 중 bluetape4k-assertions에 없거나 부족한 기능을 bluetape4k-assertions 스타일 (extension function + infix)로 포팅한다.

#### 포팅 대상 (우선순위순)

**1순위 — bluetape4k-assertions에 완전히 없는 기능**

```kotlin
// Reflection.kt — 타입 체크 + smart cast 반환 (kotlin.test.assertIs<T>)
@OptIn(ExperimentalContracts::class)
inline fun <reified T : Any> Any?.shouldBeInstanceOf(): T    // 기존 함수와 통합 (리뷰 수정 대상)

// Arrays.kt / Collections.kt — 순서 포함 내용 동등 (kotlin.test.assertContentEquals)
infix fun <T> Iterable<T>?.shouldContentEqual(expected: Iterable<T>?)
infix fun <T> Sequence<T>?.shouldContentEqual(expected: Sequence<T>?)
infix fun IntArray?.shouldContentEqual(expected: IntArray?)
infix fun LongArray?.shouldContentEqual(expected: LongArray?)
infix fun DoubleArray?.shouldContentEqual(expected: DoubleArray?)    // NaN 비트 비교
infix fun FloatArray?.shouldContentEqual(expected: FloatArray?)      // NaN 비트 비교
infix fun ByteArray?.shouldContentEqual(expected: ByteArray?)
infix fun CharArray?.shouldContentEqual(expected: CharArray?)
infix fun BooleanArray?.shouldContentEqual(expected: BooleanArray?)
infix fun ShortArray?.shouldContentEqual(expected: ShortArray?)
infix fun <T> Array<T>?.shouldContentEqual(expected: Array<T>?)      // null-safe

// Numerical.kt — 범위 포함 (kotlin.test.assertContains → shouldBeIn)
// ClosedRange/OpenEndRange overloads (bluetape4k-assertions는 값→범위만, 여기서는 타입 명확)
infix fun <T: Comparable<T>> T.shouldBeIn(range: ClosedRange<T>): T
infix fun <T: Comparable<T>> T.shouldNotBeIn(range: ClosedRange<T>): T
infix fun <T: Comparable<T>> T.shouldBeIn(range: OpenEndRange<T>): T
infix fun <T: Comparable<T>> T.shouldNotBeIn(range: OpenEndRange<T>): T
// Primitive range overloads
infix fun Int.shouldBeIn(range: IntRange): Int
infix fun Long.shouldBeIn(range: LongRange): Long
infix fun Char.shouldBeIn(range: CharRange): Char
infix fun UInt.shouldBeIn(range: UIntRange): UInt
infix fun ULong.shouldBeIn(range: ULongRange): ULong
```

**2순위 — 부가 가치 있는 기능**

```kotlin
// CharSequences.kt — 정규식 부분 포함 (kotlin.test.assertContains(regex))
// 기존 shouldMatch(regex)는 전체 매치 — 이것은 부분 포함
infix fun CharSequence.shouldContainRegex(regex: Regex): CharSequence
infix fun CharSequence.shouldNotContainRegex(regex: Regex): CharSequence

// Basic.kt — cause 포함 실패 (kotlin.test.fail에 cause 추가)
fun fail(message: String? = null, cause: Throwable? = null): Nothing

// Basic.kt — 블록 반환값 단언 (kotlin.test.expect의 이름 충돌 방지 버전)
fun <T> expectThat(expected: T, block: () -> T)
fun <T> expectThat(expected: T, message: String, block: () -> T)

// Numerical.kt — 근사값 불일치 (shouldBeNear의 역함수, kotlin.test에는 있음)
fun Double.shouldNotBeNear(expected: Double, delta: Double): Double
fun Float.shouldNotBeNear(expected: Float, delta: Float): Float
```

**3순위 — 명시성 개선 (선택적)**

```kotlin
// shouldBe(===)보다 의도를 명확히 드러냄
infix fun <T> T?.shouldBeSameInstanceAs(expected: T?): T?
infix fun <T> T?.shouldNotBeSameInstanceAs(expected: T?): T?
```

#### `shouldContentEqual` 특수 처리

- `DoubleArray`/`FloatArray`: `NaN == NaN` → `true`, `-0.0 != 0.0` — 비트 비교 (`java.lang.Double.doubleToRawLongBits`) 사용 (kotlin.test 동작과 동일).
- null 처리: 양쪽 null → pass, 한쪽만 null → fail with message.

#### 파일 배치

| 함수                                          | 파일               |
|-----------------------------------------------|--------------------|
| `shouldContentEqual` (Iterable/Sequence)      | `Collections.kt`   |
| `shouldContentEqual` (primitive + Array<T>)   | `Arrays.kt`        |
| `shouldBeIn(range: ClosedRange/OpenEndRange)` | `Numerical.kt`     |
| `shouldBeIn(IntRange/LongRange/...)`          | `Numerical.kt`     |
| `shouldContainRegex`                          | `CharSequences.kt` |
| `fail(msg, cause)`, `expectThat`              | `Basic.kt`         |
| `shouldNotBeNear`                             | `Numerical.kt`     |
| `shouldBeSameInstanceAs`                      | `Basic.kt`         |

#### kotlin.test 라이선스

- kotlin.test: Apache-2.0. Port는 기능 영감이며, 코드 직접 복사 아님. 각 구현은 `org.opentest4j.AssertionFailedError` 기반으로 독자 작성.

---

## 5. bluetape4k-assertions 마이그레이션 가이드

### 5.1 Import 변경표

| 기존 (bluetape4k-assertions)                             | 신규 (bluetape4k-assertions)                             |
|----------------------------------------------------------|----------------------------------------------------------|
| `import io.bluetape4k.assertions.shouldBeEqualTo`        | `import io.bluetape4k.assertions.shouldBeEqualTo`        |
| `import io.bluetape4k.assertions.shouldBe`               | `import io.bluetape4k.assertions.shouldBe`               |
| `import io.bluetape4k.assertions.shouldBeNull`           | `import io.bluetape4k.assertions.shouldBeNull`           |
| `import io.bluetape4k.assertions.shouldNotBeNull`        | `import io.bluetape4k.assertions.shouldNotBeNull`        |
| `import io.bluetape4k.assertions.shouldBeTrue` / `False` | `import io.bluetape4k.assertions.shouldBeTrue` / `False` |
| `import io.bluetape4k.assertions.shouldBeGreaterThan` 등 | `import io.bluetape4k.assertions.shouldBeGreaterThan`    |
| `import io.bluetape4k.assertions.shouldStartWith` 등     | `import io.bluetape4k.assertions.shouldStartWith`        |
| `import io.bluetape4k.assertions.shouldContain` 등       | `import io.bluetape4k.assertions.shouldContain`          |
| `import io.bluetape4k.assertions.shouldContainKey`       | `import io.bluetape4k.assertions.shouldContainKey`       |
| `import io.bluetape4k.assertions.invoking`               | `import io.bluetape4k.assertions.invoking`               |
| `import io.bluetape4k.assertions.coInvoking`             | `import io.bluetape4k.assertions.coInvoking`             |
| `import io.bluetape4k.assertions.shouldThrow`            | `import io.bluetape4k.assertions.shouldThrow`            |
| `import io.bluetape4k.assertions.shouldBeInstanceOf`     | `import io.bluetape4k.assertions.shouldBeInstanceOf`     |
| `import io.bluetape4k.assertions.shouldBeAfter`          | `import io.bluetape4k.assertions.shouldBeAfter`          |
| `import io.bluetape4k.assertions.assertSoftly`           | `import io.bluetape4k.assertions.assertSoftly`           |
| `import io.bluetape4k.assertions.assertFailsWith`        | `import io.bluetape4k.assertions.assertFailsWith` (유지) |

> 메모: `assertFailsWith`는 별도 마이그레이션 대상 아님 (bluetape4k-assertions의 internal 헬퍼는 그대로 사용
> 가능하거나, 본 모듈의 `shouldThrow`로 점진 치환).

#### Sed/IDE 일괄 변환 예

```bash
fd '*.kt' -e kt | xargs sd 'org\.amshove\.bluetape4k-assertions\.' 'io.bluetape4k.assertions.'
```

### 5.2 의도적 편차 (Intentional Deviations)

다음 항목은 마이그레이션 시 단순 import 변경만으로는 해결되지 않으며, 코드 수정이 필요하다.

| bluetape4k-assertions                           | 본 모듈                                            | 마이그레이션 방법                            |
|-------------------------------------------------|----------------------------------------------------|----------------------------------------------|
| `assertSoftly { foo shouldBe bar }` (자동 누적) | `assertSoftly { add { foo shouldBe bar } }` (명시) | 각 statement를 `add { }`로 감싸기            |
| `... shouldThrow AnyException`                  | `... shouldThrow Throwable::class`                 | `AnyException` → `Throwable::class`          |
| 백틱 API: `value should be equal to expected`   | `value shouldBeEqualTo expected`                   | 백틱 폼은 v1에서 미지원                      |
| `shouldEqual` (deprecated alias)                | `shouldBeEqualTo`                                  | 직접 치환                                    |
| `Char.shouldBeDigit()`                          | (v1 미지원)                                        | `it.isDigit().shouldBeTrue()` 또는 v2 대기   |
| `File.shouldExist()` 등                         | (v1 미지원)                                        | `file.exists().shouldBeTrue()` 또는 v2 대기  |
| Equivalency / EquivalencyAssertionOptions       | (v1 미지원)                                        | 객체 비교는 `data class equals` 또는 v2 대기 |

---

## 6. 구현 제약 사항

### 6.1 코딩 스타일

- bluetape4k 표준 KDoc (한국어) 작성. 각 함수 최소 항목:
    1. 목적 한 줄.
    2. `## 동작/계약` 섹션.
    3. 사용 예 코드 블록.
    4. `@param` / `@return`.
- 함수 50줄 이하, 파일 800줄 이하 (한 도메인이 800줄 넘으면 sub-file 분할 — 예: `Numerical.kt`
  → `NumericalComparisons.kt` + `NumericalSign.kt`).
- 깊은 중첩 금지 (>4 레벨).
- `@Synchronized` 사용 금지. `assertSoftly`도 thread-confined로 설계.

### 6.2 Kotlin 기능 사용 정책

- **infix**: 단일 파라미터 assertion에 적용 (`shouldBeEqualTo`, `shouldBeAfter` 등).
- **inline**: lambda 받는 함수만 (`should`, `shouldMatchAllWith`, `assertSoftly`,
  `assertFailure`).
- **reified**: 예외 타입 / 인스턴스 타입에 사용 (`shouldBeInstanceOf<T>()`,
  `assertFailure<T, E>`).
- **contract**: §3 ADR-7 명시 두 함수만.
- **value class**: 본 모듈에서는 사용 안 함 (bluetape4k-assertions 호환 우선).

### 6.3 실패 메시지 일관성

- 모든 실패 메시지는 `Messages.kt`의 헬퍼를 거쳐 생성한다:
  ```kotlin
  internal fun expectedToBe(verb: String, expected: Any?, actual: Any?): String
  internal fun expectedNotToBe(verb: String, expected: Any?, actual: Any?): String
  ```
- IntelliJ가 expected/actual diff를 띄우려면 `AssertionFailedError(message, expected, actual)`
  3-arg ctor를 사용해야 한다. `Failures.kt`에서 강제.

### 6.4 Detekt

- 본 모듈 전용 baseline 허용 항목:
    - `LongParameterList` — `add` DSL이 여러 parameter를 받지 않으므로 일반적으로 발생 X.
    - `MagicNumber` — 테스트 예시에서 발생 가능; 메인 코드에는 적용 안 됨.
    - `TooManyFunctions` — 28개 DateTime 함수 등으로 인해 정당한 baseline 가능.

### 6.5 Atomicfu / Synchronized

- 본 모듈에는 atomic 변수가 없어야 한다 (assertion은 stateless).
- `synchronized {}` / `@Synchronized` 사용 금지.

### 6.6 가시성 / 패키지 구조

- 모든 public 함수는 `io.bluetape4k.assertions` 또는 `io.bluetape4k.assertions.coroutines`.
- `internal` 헬퍼는 `io.bluetape4k.assertions.internal`.
- `@PublishedApi internal`은 inline 함수에서 internal 호출이 필요한 경우에만 사용.

### 6.7 의존성 정책

- `api`: `bluetape4k-logging`, `junit-jupiter-api`, `opentest4j`.
- `compileOnly`: `kotlinx-coroutines-core`, `app.cash.turbine:turbine`.
- `testImplementation`: junit engine/launcher, kotlinx-coroutines-test, mockk, datafaker, turbine.

---

## 7. 테스트 전략

### 7.1 테스트 구조

```
src/test/kotlin/io/bluetape4k/assertions/
├── BasicTest.kt
├── NumericalTest.kt
├── CharSequencesTest.kt
├── CollectionsTest.kt
├── ArraysTest.kt
├── MapsTest.kt
├── ExceptionsTest.kt
├── ReflectionTest.kt
├── DateTimesTest.kt
├── SoftlyTest.kt
├── SoftlyVirtualThreadTest.kt    ← 가상 스레드 안전성 검증
├── coroutines/
│   ├── FlowAssertionsTest.kt
│   └── TurbineSupportTest.kt
└── internal/
    ├── FailuresTest.kt
    └── MessagesTest.kt
```

### 7.2 테스트 패턴 (AAA + bluetape4k-assertions-호환 자기참조)

각 assertion에 대해 두 종류 테스트 작성:

1. **passing case**: 통과 시 receiver가 그대로 반환되어야 한다.
2. **failing case**: `assertFailsWith<AssertionFailedError>`로 실패 메시지 / expected / actual을 검증.

```kotlin
@Test
fun `shouldBeEqualTo passes for equal values`() {
    val actual = "hello"
    val returned = actual shouldBeEqualTo "hello"
    returned shouldBe actual  // chaining 보장
}

@Test
fun `shouldBeEqualTo fails for unequal values`() {
    val ex = assertFailsWith<AssertionFailedError> {
        "hello" shouldBeEqualTo "world"
    }
    ex.expected.stringRepresentation shouldBeEqualTo "\"world\""
    ex.actual.stringRepresentation shouldBeEqualTo "\"hello\""
}
```

### 7.3 가상 스레드 안전성 테스트 (필수)

```kotlin
@Test
fun `assertSoftly is safe under 256 concurrent virtual threads`() {
    val errors = ConcurrentHashMap.newKeySet<Throwable>()
    val factory = Thread.ofVirtual().factory()
    Executors.newThreadPerTaskExecutor(factory).use { exec ->
        repeat(256) { i ->
            exec.submit {
                runCatching {
                    assertSoftly {
                        add { i shouldBeGreaterOrEqualTo 0 }
                        add { i shouldBeLessThan 256 }
                    }
                }.onFailure { errors += it }
            }
        }
    }
    errors.shouldBeEmpty()
}
```

### 7.4 FlowAssertions 이전 검증

- 기존 `bluetape4k-coroutines` 의 `FlowAssertions` 테스트가 그대로 통과해야 한다.
- bridge 함수는 별도 `FlowAssertionsBridgeTest`로 컴파일 가능 + WARNING 발생 확인.

### 7.5 호환성 검증 (Compatibility Smoke Test)

별도 test source set 또는 `bluetape4k-junit5-tests` 등의 모듈에서 다음 시나리오 1회 실행:

```kotlin
// 기존 bluetape4k-assertions 사용 예 → import만 io.bluetape4k.assertions.*로 변경 후 컴파일 + 실행
import io.bluetape4k.assertions.*

class CompatibilitySmokeTest {
    @Test fun `bluetape4k-assertions style still works`() {
        "abc" shouldBeEqualTo "abc"
        listOf(1, 2, 3) shouldContainAll listOf(1, 2)
        invoking { error("x") } shouldThrow IllegalStateException::class
    }
}
```

### 7.6 커버리지

- 라인 커버리지 80% 이상.
- 28개 DateTime 함수, 7개 primitive array 타입 등 boilerplate에는 parameterized 테스트 권장.

---

## 8. 파일 구조 (Implementation Map)

```
testing/assertions/
├── build.gradle.kts
├── README.md
├── README.ko.md
└── src/
    ├── main/kotlin/io/bluetape4k/assertions/
    │   ├── Basic.kt                      (~ 200 lines)
    │   ├── Numerical.kt                  (~ 350 lines)
    │   ├── CharSequences.kt              (~ 350 lines)
    │   ├── Collections.kt                (~ 300 lines)
    │   ├── Arrays.kt                     (~ 400 lines, 8 primitive overloads)
    │   ├── Maps.kt                       (~ 150 lines)
    │   ├── Exceptions.kt                 (~ 200 lines)
    │   ├── Reflection.kt                 (~ 100 lines)
    │   ├── DateTimes.kt                  (~ 500 lines, 7 types × 4 fns)
    │   ├── Softly.kt                     (~ 80 lines)
    │   ├── coroutines/
    │   │   ├── FlowAssertions.kt         (~ 150 lines, 이전됨)
    │   │   └── TurbineSupport.kt         (~ 100 lines, compileOnly 격리)
    │   └── internal/
    │       ├── Failures.kt               (~ 80 lines, AssertionFailedError factory)
    │       └── Messages.kt               (~ 120 lines, 메시지 포맷팅)
    └── test/kotlin/io/bluetape4k/assertions/
        └── (각 main 파일별 *Test.kt — §7.1 참조)

bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/tests/
└── FlowAssertions.kt                     (@Deprecated bridge, ~ 150 lines)
```

### 8.1 파일별 책임 요약

| 파일                           | 주요 책임                                                                       |
|--------------------------------|---------------------------------------------------------------------------------|
| `Basic.kt`                     | 동등성 / 참조 / null / boolean / 일반 predicate                                 |
| `Numerical.kt`                 | 비교 / 부호 / 범위 / 근사값                                                     |
| `CharSequences.kt`             | 문자열 prefix/suffix/substring/regex/blank 처리                                 |
| `Collections.kt`               | Iterable, Collection, Array<T> 공통                                             |
| `Arrays.kt`                    | 8 primitive array 타입 overload                                                 |
| `Maps.kt`                      | Key/Value/Pair/Size                                                             |
| `Exceptions.kt`                | invoking/coInvoking + shouldThrow + chaining (withMessage 등)                   |
| `Reflection.kt`                | shouldBeInstanceOf (contract + KClass)                                          |
| `DateTimes.kt`                 | 7 타입 × shouldBeAfter/Before/OnOrAfter/OnOrBefore                              |
| `Softly.kt`                    | SoftAssertionScope + assertSoftly DSL (assertAll 위임)                          |
| `coroutines/FlowAssertions.kt` | Flow assertEmpty / assertResult / assertResultSet / assertFailure / assertError |
| `coroutines/TurbineSupport.kt` | Turbine ReceiveTurbine 헬퍼 (compileOnly)                                       |
| `internal/Failures.kt`         | `org.opentest4j.AssertionFailedError` factory                                   |
| `internal/Messages.kt`         | 메시지 포맷팅 헬퍼 (expected/actual stringification)                            |

---

## 9. 의존성

### 9.1 build.gradle.kts (확정안)

```kotlin
plugins {
    `java-library`
    kotlin("jvm")
}

description = "Bluetape4k testing assertions — bluetape4k-assertions compatible, JUnit 5 native"

dependencies {
    // ⚠️ bluetape4k-* 모듈 의존 금지 (ADR-9)
    api(platform(libs.junit.bom))

    // JUnit 5
    api(libs.junit.jupiter.api)
    api(libs.opentest4j)   // 버전 카탈로그 등록 후 libs.opentest4j 사용

    // Coroutines — api (FlowAssertions가 main 소스에 있어 소비자 classpath에도 필요)
    api(libs.kotlinx.coroutines.core)
    // Turbine — compileOnly (TurbineSupport 사용자는 직접 testImplementation 추가 필요)
    compileOnly(libs.turbine)

    // Test (bluetape4k-junit5는 testImplementation으로만 사용)
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.datafaker)
    testRuntimeOnly(libs.logback.classic)
}
```

### 9.2 Reverse dependency 영향 (확정)

- `bluetape4k-coroutines` → `bluetape4k-assertions` **의존성 없음** (ADR-6 참조). bridge 함수는 독립 구현, 새 모듈에 위임 안 함.
- **`bluetape4k-junit5`**: `api(project(":bluetape4k-assertions"))` 추가 (v1 필수). 기존 사용자는 의존성 변경 없이 새 assertions 사용 가능.
  `api(libs.bluetape4k.assertions)` 는 단계적 제거 예정 (v2).
- `mock-web-server`, `mock-webflux-server`, `testcontainers`: 추후 추가 (v1 이후).

### 9.3 라이브러리 버전 요건

- Kotlin 2.3+
- JVM 21+
- JUnit Jupiter 5.10+
- opentest4j 1.3+
- kotlinx-coroutines 1.8+
- turbine 1.1+

(`buildSrc/Libs.kt`는 본 스펙에서 변경하지 않음 — 이미 위 버전들이 정의되어 있음을 가정.)

---

## 10. DoD (Definition of Done)

본 스펙의 구현이 완료되었다고 선언하려면 다음이 모두 만족되어야 한다.

- [ ] `bluetape4k-assertions` 모듈 빌드 성공: `./gradlew :bluetape4k-assertions:build`
- [ ] 모든 테스트 통과: 0 failures, 0 errors (테스트 수와 소요시간 보고)
- [ ] 각 assertion 함수에 Korean KDoc 작성 (목적 / 동작·계약 / 사용 예 / @param / @return)
- [ ] §4의 bluetape4k-assertions Keep 목록 모든 API 구현 (Basic / Numerical / CharSequence / Collections+Arrays+Maps / Exceptions / Reflection / DateTime / Softly / FlowAssertions / TurbineSupport)
- [ ] `assertSoftly` 가상 스레드 안전성 검증: 256 concurrent virtual threads 테스트 통과 (§7.3 참조)
- [ ] FlowAssertions 이전 완료 (`io.bluetape4k.assertions.coroutines.*`) +
  `bluetape4k-coroutines`의 기존 위치에 `@Deprecated(level = WARNING)` bridge 작성
- [ ] `./gradlew :bluetape4k-coroutines:test` 통과 (bridge deprecation 경고만, 컴파일 오류 없음)
- [ ] `README.md` + `README.ko.md` 작성: Mermaid UML 다이어그램, API 카탈로그, bluetape4k-assertions 마이그레이션 가이드 (§5), 사용 예 포함
- [ ] Detekt 통과 (또는 정당화된 baseline 등록)
- [ ] bluetape4k-assertions 이름 호환 검증: 샘플 테스트 파일에서 `import io.bluetape4k.assertions.*`만
  `import io.bluetape4k.assertions.*`로 바꾼 후 컴파일/실행 통과 (§7.5 smoke test)
- [ ] Turbine `compileOnly` 격리 검증: TurbineSupport를 사용하지 않는 소비자는 Turbine 없이
  `bluetape4k-assertions`를 정상 사용 가능. TurbineSupport 사용 시 소비자가
  `testImplementation(libs.turbine)` 직접 추가 필요 (KDoc + README에 명시 확인)
- [ ] `./gradlew :bluetape4k-assertions:detekt` 통과
- [ ] PR 본문에 테스트 결과 (통과 수, 시간), 호환성 검증 결과, README 업데이트 명시
- [ ] code-reviewer agent 실행 후 HIGH/CRITICAL 이슈 0건
- [ ] kotlin.test 포팅 함수 (§4.11) 구현 및 테스트 통과
- [ ] `bluetape4k-junit5`에 `api(project(":bluetape4k-assertions"))` 추가 확인

### 10.1 검증 명령 모음 (구현자용)

```bash
# 빌드 + 테스트
./gradlew :bluetape4k-assertions:build

# 모듈 단위 테스트
./gradlew :bluetape4k-assertions:test

# 가상 스레드 테스트만
./gradlew :bluetape4k-assertions:test --tests "*SoftlyVirtualThreadTest*"

# Detekt
./gradlew :bluetape4k-assertions:detekt

# coroutines bridge 검증
./gradlew :bluetape4k-coroutines:test

# 호환성 smoke test
./gradlew :bluetape4k-assertions:test --tests "*CompatibilitySmokeTest*"
```

---

## 부록 A. 리스크 및 완화 전략

| 리스크                                                         | 영향                                               | 완화 전략                                                                                                                                                    |
|----------------------------------------------------------------|----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `shouldBe` (===) vs `shouldBeEqualTo` (==) 혼동                | 사용자가 잘못된 함수 호출                          | KDoc 첫 줄에 큰 경고; README 마이그레이션 표에 명시                                                                                                          |
| `kotlin.contracts` smart-cast 가 Kotlin 버전에 의존            | 컴파일 환경 차이로 smart-cast 실패                 | Kotlin 2.3+ 강제, Detekt 룰로 contract 사용 함수 추적                                                                                                        |
| FlowAssertions bridge 경고 폭발                                | 기존 코드 빌드시 콘솔 노이즈                       | DeprecationLevel.WARNING + ReplaceWith 정확히 작성, 2 minor 이내 제거 일정 README에 명시                                                                     |
| bluetape4k-assertions의 백틱 API 사용 코드                     | import 변경만으로는 빌드 실패                      | §5.2 의도적 편차 표에 명시; sed 스크립트 제공 (백틱 → 카멜케이스 변환)                                                                                       |
| `assertSoftly` 자동 누적을 사용한 코드                         | 동작 변경 (silent fail → MultipleFailures)         | §5.2에 명시; IDE structural search/replace 가이드 제공                                                                                                       |
| Turbine compileOnly 누락 시 사용자 혼란                        | 사용자가 Turbine 추가 안 하면 NoClassDefFoundError | KDoc 상단에 "Turbine 의존성 필요" 표시; README에 별도 섹션                                                                                                   |
| 기존 `bluetape4k-coroutines` 사용자가 새 모듈 의존성 추가 거부 | 새 assertions 접근 불가                            | ADR-6 결정: bridge는 독립 구현(@Deprecated), 의존성 없음. 사용자는 새 함수로 마이그레이션하거나 기존 구현 계속 사용 가능 — README 마이그레이션 가이드에 명시 |

---

## 부록 B. 다음 단계

본 스펙 승인 후의 작업 순서는 별도 plan 문서 (`/docs/superpowers/plans/2026-05-06-testing-assertions-plan.md`)에서 다룬다. 개략 흐름:

1. 모듈 골격 + build.gradle.kts + settings.gradle.kts 등록
2. `internal/Failures.kt` + `internal/Messages.kt` 구현
3. `Basic.kt` 구현 + 테스트 (가장 의존성 적음)
4. `Numerical.kt` / `CharSequences.kt` / `Collections.kt` / `Arrays.kt` / `Maps.kt`
5. `Exceptions.kt` / `Reflection.kt` / `DateTimes.kt`
6. `Softly.kt` + 가상 스레드 테스트
7. `coroutines/FlowAssertions.kt` 이전 + `bluetape4k-coroutines` bridge
8. `coroutines/TurbineSupport.kt`
9. 호환성 smoke test
10. README.md + README.ko.md (Mermaid UML 포함)
11. Detekt baseline 정리
12. code-reviewer 실행 및 HIGH/CRITICAL 해결
13. PR 작성

---

(끝)
