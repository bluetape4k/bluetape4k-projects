# bluetape4k-assertions 구현 계획

**Spec**: `docs/superpowers/specs/2026-05-06-testing-assertions-design.md`
**Issue**: #322
**날짜**: 2026-05-07
**브랜치**: `feat/testing-assertions`
**Worktree**: `.worktrees/feat/testing-assertions/`
**대상 모듈**: `testing/assertions/` → Gradle 등록명 `bluetape4k-assertions`

---

## 0. 개요

본 계획은 spec 부록 B의 13단계 흐름을 세분화한 작업 항목(T1~T22)이다.
각 작업은 ID, 복잡도(`high`/`medium`/`low`), 대상 파일, 수용 기준(DoD), 의존 작업을 명시한다.

### 공통 컨벤션 (모든 구현자 필독)

> **bluetape4k-* 의존 금지 (ADR-9)**: `bluetape4k-assertions`는 `bluetape4k-logging`,
> `bluetape4k-core`, `bluetape4k-junit5` 등 어떤 `bluetape4k-*` 모듈에도 `api`/`implementation`
> 의존을 추가하지 않는다. `testImplementation`으로만 허용.
>
> **KLogging/로깅 사용 금지**: assertion 함수는 stateless이므로 logging이 불필요하다.
> `companion object : KLogging()` 또는 `KotlinLogging.logger {}` 사용 금지.
> 실패 진단은 `AssertionFailedError` 메시지와 expected/actual 값만으로 충분하다.

### 복잡도 라우팅 가이드

- `high` → Opus (핵심 로직, 동시성, contract, FlowAssertions, 가상스레드 안전 설계)
- `medium` → Sonnet (표준 assertion 구현, 테스트 작성, Spring 연동)
- `low` → Haiku (KDoc, 설정, 보일러플레이트, README)

### 작업 의존 그래프 요약

```
T1 (스캐폴드)
 └─ T2 (internal/Failures + Messages)
     └─ T3 (Basic.kt)
         ├─ T4 (Numerical)        ┐
         ├─ T5 (CharSequences)    │ 병렬 가능 (그룹 A)
         ├─ T6 (Collections)      │
         ├─ T7 (Arrays)           │
         └─ T8 (Maps)             ┘
             ├─ T9  (Exceptions)   ┐
             ├─ T10 (Reflection)   │ 병렬 가능 (그룹 B)
             └─ T11 (DateTimes)    ┘
         ├─ T12 (Softly + 가상스레드) ← T3 직접 의존
         └─ T13 (FlowAssertions) ← T3, T6, T10 의존
             ├─ T14 (coroutines bridge)  ┐ 병렬 가능 (그룹 C)
             └─ T15 (TurbineSupport)     ┘
     └─ T4~T8 → T16 (kotlin.test 포팅) ← 그룹 A 완료 후
         └─ T17 (smoke test)
             └─ T18 (junit5 의존성)
                         ┐
         T15 + T16 ──→  T19 (KDoc 패스)  [※ T15, T16 모두 완료 후]
                         └─ T20 (README)
                             └─ T21 (Detekt)
                                 + T18 ──→ T22 (최종 검증)  [※ T21, T18 모두 완료 후]
```

> **참고**: 위 그래프보다 하단 "작업 요약 표"의 의존 열이 권위 있는 기준이다.

---

## T0. Spec §4.9 bridge 예시 수정 (선행 작업)

- **복잡도**: `low`
- **대상 파일**:
  - `docs/superpowers/specs/2026-05-06-testing-assertions-design.md` (수정 — §4.9 bridge 코드 블록)
- **수용 기준**:
  - spec §4.9 "Bridge" 코드 블록이 ADR-6와 일치한다:
    - 함수 본문이 `io.bluetape4k.assertions.coroutines.*` 호출(위임)이 아닌 **기존 구현 유지** 패턴을 보여준다.
    - 코드 블록 상단에 `// ✅ 본문은 기존 구현 그대로 유지 — 새 모듈 위임 금지 (ADR-6)` 주석 포함.
  - spec commit 후 구현자가 spec만 읽어도 ADR-6 의도를 파악할 수 있다.
  - (이미 직전 커밋에서 수정 완료 — 이 항목은 DoD 체크 목적으로 기재)
- **의존**: 없음

---

## T1. 모듈 스캐폴드 + Gradle 등록

- **복잡도**: `low`
- **대상 파일**:
  - `testing/assertions/build.gradle.kts` (신규)
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/.gitkeep` (신규)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/.gitkeep` (신규)
  - `testing/assertions/src/test/resources/junit-platform.properties` (신규)
  - `testing/assertions/src/test/resources/logback-test.xml` (신규)
  - `settings.gradle.kts` (확인 — `testing/` 자동 등록 여부 검증)
- **수용 기준**:
  - `./gradlew :bluetape4k-assertions:tasks` 명령이 모듈을 인식하여 정상 출력한다.
  - `build.gradle.kts`에 spec §9.1 의존성이 모두 선언되어 있다 (**bluetape4k-* api 의존 금지**):
    - `api`: `platform(libs.junit.bom)`, `libs.junit.jupiter.api`,
      `libs.opentest4j` (※아래 주의),
      `libs.kotlinx.coroutines.core` (spec §9.1 우선 — §6.7의 compileOnly 표기는 오류)
    - `compileOnly`: `libs.turbine`
    - `testImplementation`: `project(":bluetape4k-junit5")`, `libs.kotlin.test.junit5`,
      `libs.junit.jupiter.engine`, `libs.junit.platform.launcher`,
      `libs.kotlinx.coroutines.test`, `libs.turbine`, `libs.mockk`, `libs.datafaker`
    - `testRuntimeOnly`: `libs.logback.classic`
  - **⚠️ opentest4j 버전 카탈로그 확인 (H1)**:
    - `gradle/libs.versions.toml` 또는 `buildSrc/Libs.kt`에 `opentest4j` 항목이 있으면
      `api(libs.opentest4j)` 형식으로 참조.
    - 없으면 먼저 버전 카탈로그에 추가한 후 참조. 버전은 `junit-bom`이 관리.
    - **bare string `api("org.opentest4j:opentest4j")` 사용 금지** (프로젝트 규약 위반).
  - `description` 필드: "Bluetape4k testing assertions — bluetape4k-assertions compatible, JUnit 5 native".
  - `junit-platform.properties`: `io.bluetape4k.assertions` 패키지 logger 이름으로 작성.
  - `logback-test.xml`: `<logger name="io.bluetape4k.assertions" level="DEBUG"/>` 포함.
  - `./gradlew :bluetape4k-assertions:build -x test` 가 빈 모듈 상태에서 통과한다.
  - **자동 등록 실패 대비**: `settings.gradle.kts`가 `testing/assertions`를 자동 인식하지 못하면
    수동으로 `include(":bluetape4k-assertions")` + `project(":bluetape4k-assertions").projectDir` 설정 추가.
- **의존**: 없음

---

## T2. `internal/Failures.kt` + `internal/Messages.kt`

- **복잡도**: `medium`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/internal/Failures.kt` (신규, ~80 lines)
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/internal/Messages.kt` (신규, ~120 lines)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/internal/FailuresTest.kt` (신규)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/internal/MessagesTest.kt` (신규)
- **수용 기준**:
  - `Failures.kt`에 다음 factory 함수가 존재한다.
    - `internal fun fail(message: String): Nothing` → `AssertionFailedError(message)` throw
    - `internal fun failComparison(message: String, expected: Any?, actual: Any?): Nothing`
      → `AssertionFailedError(message, expected, actual)` 3-arg ctor 사용 (IntelliJ diff viewer 동작 보장)
    - `internal fun failWithCause(message: String, cause: Throwable): Nothing`
  - `Messages.kt`에 spec §6.3 시그니처 헬퍼 존재: `expectedToBe(verb, expected, actual)`,
    `expectedNotToBe(verb, expected, actual)`, 그 외 stringification 헬퍼
    (`stringify(value: Any?)`, `formatCollection(c: Collection<*>)` 등).
  - 메시지 포맷 표준: `Expected <subject> to <verb> <expected>, but was <actual>.` (spec ADR-8).
  - 테스트:
    - `failComparison` 호출 시 `AssertionFailedError`의 `expected.value`, `actual.value`가 정확히 채워짐 검증.
    - `stringify` 가 `null`, `String`, `Collection`, `IntArray`, `Throwable` 등에 대해 올바른 표현을 반환.
  - 테스트 라인 커버리지 ≥ 80%.
- **의존**: T1

---

## T3. `Basic.kt` + 테스트

- **복잡도**: `medium`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Basic.kt` (신규, ~200 lines)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/BasicTest.kt` (신규)
- **수용 기준**:
  - spec §4.1의 모든 시그니처 구현:
    - `shouldBeEqualTo`, `shouldNotBeEqualTo`, `shouldBe` (===), `shouldNotBe` (!==),
      `shouldBeNull`, `shouldNotBeNull` (contract 적용), `shouldBeTrue`, `shouldBeFalse`,
      `shouldNotBeTrue`, `shouldNotBeFalse`, `should(message, predicate)`.
  - `shouldNotBeNull`은 `@OptIn(ExperimentalContracts::class)` + `contract { returns() implies (this@shouldNotBeNull != null) }` 적용.
  - 모든 함수는 통과 시 receiver를 그대로 반환(체이닝 보장).
  - `Failures.failComparison`/`Failures.fail` 사용으로 `AssertionFailedError` 일관 생성.
  - 테스트:
    - 각 함수에 대해 passing case (receiver 반환 검증) + failing case (`assertFailsWith<AssertionFailedError>` + 메시지/expected/actual 검증) 작성.
    - `shouldNotBeNull` 이후 smart-cast 컴파일 검증 (예: `val s = maybeStr.shouldNotBeNull(); s.length`).
    - `shouldBe` (referential) vs `shouldBeEqualTo` (value) 의미 차이 검증.
  - **⚠️ assertFailsWith import (H2)**: `bluetape4k-assertions` 모듈의 자체 테스트에서는
    `kotlin.test.assertFailsWith`를 사용한다. `io.bluetape4k.assertions.assertFailsWith`는
    **이 모듈 내 사용 금지** (bluetape4k-assertions를 대체하는 모듈이 bluetape4k-assertions에 역참조하는 순환 의존 발생).
    해당 memory rule은 다른 bluetape4k 모듈(bluetooth-junit5 등)에 적용된다.
- **의존**: T2

---

## T4. `Numerical.kt` + 테스트

- **복잡도**: `medium`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Numerical.kt` (신규, ~350 lines)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/NumericalTest.kt` (신규)
- **수용 기준**:
  - spec §4.2 모든 시그니처:
    - 비교: `shouldBeGreaterThan`, `shouldBeGreaterOrEqualTo`, `shouldBeLessThan`,
      `shouldBeLessOrEqualTo` + `Not*` 4개 (Comparable<T> 제약).
    - 부호: `shouldBePositive`, `shouldBeNegative` × {Byte, Short, Int, Long, Float, Double}.
    - 범위: `shouldBeInRange(ClosedRange)`, `shouldNotBeInRange(ClosedRange)`.
    - 근사: `Double.shouldBeNear(expected, tolerance)`, `Float.shouldBeNear(...)`.
  - spec §4.11 1순위 추가:
    - `shouldBeIn(ClosedRange/OpenEndRange)`, `shouldNotBeIn` 동일.
    - primitive range overload: `Int/Long/Char/UInt/ULong` × `IntRange/LongRange/...`.
  - spec §4.11 2순위 추가:
    - `Double/Float.shouldNotBeNear(expected, delta)`.
  - 모든 함수 chaining (receiver 반환).
  - `shouldBeNear`는 named `tolerance` parameter (bluetape4k-assertions와 차이 — KDoc 명시).
  - 테스트: 각 함수 passing/failing case + parameterized 테스트로 6개 primitive 부호 검증.
- **의존**: T3

---

## T5. `CharSequences.kt` + 테스트

- **복잡도**: `medium`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/CharSequences.kt` (신규, ~350 lines)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/CharSequencesTest.kt` (신규)
- **수용 기준**:
  - spec §4.3 시그니처 모두 구현:
    - `shouldStartWith`, `shouldEndWith`, `shouldContain`, `shouldContainIgnoringCase`,
      `shouldNotContain`, `shouldBeEmpty`, `shouldNotBeEmpty`, `shouldBeBlank`, `shouldNotBeBlank`,
      `shouldBeNullOrEmpty`, `shouldNotBeNullOrEmpty`, `shouldMatch(Regex)`, `shouldMatch(String)`,
      `shouldNotMatch(Regex)`, `shouldContainAll(Iterable)`, `shouldContainAll(vararg)`,
      `shouldContainNone`, `shouldBeEqualToIgnoringCase`.
  - spec §4.11 2순위: `shouldContainRegex(Regex)` (부분 포함, vs `shouldMatch`는 전체 매치),
    `shouldNotContainRegex(Regex)`.
  - `shouldNotBeNullOrEmpty` 반환 타입은 non-null `CharSequence` (smart-cast 효과).
  - 모든 함수 chaining.
  - 테스트: 각 함수 passing/failing case, regex 매치/포함 차이 검증.
- **의존**: T3

---

## T6. `Collections.kt` + 테스트

- **복잡도**: `medium`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Collections.kt` (신규, ~300 lines)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/CollectionsTest.kt` (신규)
- **수용 기준**:
  - spec §4.4 Collections 시그니처:
    - `shouldContain`, `shouldNotContain`, `shouldContainAll(Iterable)`, `shouldContainAll(vararg)`,
      `shouldContainAny(Iterable)`, `shouldContainAny(vararg)`, `shouldContainNone`,
      `shouldHaveSize`, `shouldBeEmpty`, `shouldNotBeEmpty`, `shouldBeIn`, `shouldNotBeIn`,
      `shouldMatchAllWith(predicate)`, `shouldMatchAtLeastOneOf(predicate)`.
  - spec §4.11 1순위: `Iterable<T>?.shouldContentEqual(expected)`,
    `Sequence<T>?.shouldContentEqual(expected)` — null-safe, 순서 포함 동등.
  - `shouldContainSame`은 v1에서 제외 (spec §4.4 차이 표 명시).
  - 테스트: 각 함수 passing/failing case + `shouldContentEqual` null/순서 케이스.
- **의존**: T3

---

## T7. `Arrays.kt` + 테스트

- **복잡도**: `medium`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Arrays.kt` (신규, ~400 lines)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/ArraysTest.kt` (신규)
- **수용 기준**:
  - 8 primitive array 타입 × {`shouldContain`, `shouldNotContain`, `shouldHaveSize`,
    `shouldBeEmpty`, `shouldNotBeEmpty`} 모두 구현
    (Int/Long/Double/Float/Short/Byte/Char/BooleanArray).
  - spec §4.11 1순위 `shouldContentEqual`:
    - `IntArray?`, `LongArray?`, `ByteArray?`, `CharArray?`, `BooleanArray?`, `ShortArray?`,
      `Array<T>?` — null-safe.
    - `DoubleArray?`, `FloatArray?`: NaN 비트 비교 (`java.lang.Double.doubleToRawLongBits` /
      `Float.floatToRawIntBits`) — kotlin.test 동작과 동일. `NaN == NaN` → true,
      `-0.0 != 0.0` 보장.
  - 테스트: parameterized 테스트로 8 primitive 타입 일괄 검증, NaN 케이스 별도 테스트,
    null 양쪽/한쪽 케이스 검증.
- **의존**: T3

---

## T8. `Maps.kt` + 테스트

- **복잡도**: `low`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Maps.kt` (신규, ~150 lines)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/MapsTest.kt` (신규)
- **수용 기준**:
  - spec §4.4 Maps 시그니처:
    - `shouldContainKey`, `shouldContainValue`, `shouldContain(Pair)`, `shouldHaveSize`,
      `shouldBeEmpty`, `shouldNotBeEmpty`.
  - 모든 함수 chaining.
  - 테스트: passing/failing case + `Pair` 매칭 검증.
- **의존**: T3

---

## T9. `Exceptions.kt` + 테스트

- **복잡도**: `high`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Exceptions.kt` (신규, ~200 lines)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/ExceptionsTest.kt` (신규)
- **수용 기준**:
  - spec §4.5 시그니처 모두 구현:
    - `class InvokingBlock(val block: () -> Any?)`, `fun invoking(block)`.
    - `class CoInvokingBlock(val block: suspend () -> Any?)`, `fun coInvoking(block)`.
    - `infix fun <T : Throwable> InvokingBlock.shouldThrow(expected: KClass<T>): T`.
    - `fun InvokingBlock.shouldNotThrow(): Any?`.
    - `suspend infix fun ... CoInvokingBlock.shouldThrow(...)`, `suspend fun ... shouldNotThrow()`.
    - `withMessage(message)` (정확 일치), `withCause(KClass)` (assignable),
      `with(block: T.() -> Unit)`.
  - 추가 (bluetape4k-assertions에 없는, spec §4.5):
    - `withMessageContaining(substring: String)`, `withMessageMatching(regex: Regex)`.
  - 의미론 (spec §4.5):
    - `withMessage`: 정확 문자열 일치, 실패 시 명확 메시지.
    - `withCause`: `cause == null`이면 실패.
    - **CancellationException 처리**: `coInvoking { }.shouldThrow(SomeException::class)` 에서
      `SomeException`이 `CancellationException` 비파생인데 실제로 `CancellationException`이 던져지면
      **즉시 rethrow** (catch 안 함, coroutine cancellation 보존). 테스트로 검증.
  - `AnyException`/`AnyExceptionType` sentinel은 미구현 (KDoc에 `Throwable::class` 사용 안내).
  - 테스트:
    - 동기 `invoking` shouldThrow / shouldNotThrow / 타입 불일치 / 미발생 시 실패 메시지.
    - `coInvoking` 동일 (suspend 환경, runTest 사용).
    - `withMessage`/`withMessageContaining`/`withMessageMatching` 통과/실패.
    - `withCause` cause null 시 실패 검증.
    - **CancellationException rethrow 테스트**: `coInvoking { throw CancellationException() } shouldThrow IllegalStateException::class` → `CancellationException` 자체가 catch 없이 외부로 전파됨을 검증.
- **의존**: T3

---

## T10. `Reflection.kt` + 테스트

- **복잡도**: `medium`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Reflection.kt` (신규, ~100 lines)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/ReflectionTest.kt` (신규)
- **수용 기준**:
  - spec §4.6 시그니처 모두 구현:
    - `inline fun <reified T : Any> Any?.shouldBeInstanceOf(): T` — contract 적용
      (`returns() implies (this@shouldBeInstanceOf is T)`), `@OptIn(ExperimentalContracts::class)`.
    - `inline fun <reified T : Any> Any?.shouldNotBeInstanceOf(): Any?`.
    - `infix fun Any?.shouldBeInstanceOf(klass: KClass<*>): Any?`.
    - `infix fun Any?.shouldNotBeInstanceOf(klass: KClass<*>): Any?`.
  - 테스트:
    - reified 버전 사용 후 smart-cast 컴파일 검증.
    - KClass 버전 — Comparable, sealed 등 다양한 타입.
    - null receiver 동작 (Kotlin 의미론: `null is T` = false):
      - `null.shouldBeInstanceOf<String>()` → **실패** (null은 어떤 타입도 아님)
      - `null.shouldNotBeInstanceOf<String>()` → **통과** (null이 String이 아님은 참)
      - 두 동작을 별도 테스트 케이스로 명시.
- **의존**: T3

---

## T11. `DateTimes.kt` + 테스트

- **복잡도**: `medium`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/DateTimes.kt` (신규, ~500 lines)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/DateTimesTest.kt` (신규)
- **수용 기준**:
  - spec §4.7 7 타입 × 4 함수 = 28개 시그니처 모두 구현:
    - 타입: `Instant`, `ZonedDateTime`, `OffsetDateTime`, `LocalDateTime`, `LocalDate`,
      `LocalTime`, `java.util.Date`.
    - 함수: `shouldBeAfter`, `shouldBeBefore`, `shouldBeOnOrAfter`, `shouldBeOnOrBefore`.
  - 모든 함수 receiver 반환.
  - `java.util.Date` 비교는 `before`/`after` 메서드 또는 `compareTo` 사용 (밀리초 비교).
  - 테스트: parameterized 테스트로 7 타입 일괄 검증 (각 타입마다 passing/failing case 4개씩).
- **의존**: T3

---

## T12. `Softly.kt` + 가상 스레드 안전성 테스트

- **복잡도**: `high`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Softly.kt` (신규, ~80 lines)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/SoftlyTest.kt` (신규)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/SoftlyVirtualThreadTest.kt` (신규)
- **수용 기준**:
  - spec §4.8 구현:
    - `class SoftAssertionScope` — 내부 `mutableListOf<Executable>`, `fun add(block: () -> Unit)`,
      `@PublishedApi internal fun executables(): List<Executable>`.
    - `inline fun assertSoftly(block: SoftAssertionScope.() -> Unit)` —
      `Assertions.assertAll(scope.executables())` 위임.
  - **thread-local / @Synchronized / synchronized {} 사용 절대 금지** (spec §6.5).
  - 모든 상태는 `SoftAssertionScope` 인스턴스 내부에 한정 (thread-confined).
  - 테스트:
    - **passing case**: 모든 add 통과 시 정상 종료.
    - **failing case**: 일부 실패 시 `MultipleFailuresError` 발생, 모든 실패 메시지 포함.
    - **가상 스레드 안전성** (spec §7.3 — DoD 필수):
      - 256 concurrent virtual threads에서 각 스레드가 독립 `assertSoftly` 호출.
      - `Thread.ofVirtual().factory()` + `Executors.newThreadPerTaskExecutor` 사용.
      - `ConcurrentHashMap.newKeySet<Throwable>()`로 에러 수집, deadlock/crash 없음 검증.
  - 가상 스레드 테스트는 별도 클래스 (`SoftlyVirtualThreadTest`)로 분리.
- **의존**: T3

---

## T13. `coroutines/FlowAssertions.kt` + 테스트 (canonical 위치)

- **복잡도**: `high`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/coroutines/FlowAssertions.kt` (신규, ~150 lines)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/coroutines/FlowAssertionsTest.kt` (신규)
- **수용 기준**:
  - spec §4.9 시그니처 모두 구현 (suspend):
    - `Flow<T>.assertEmpty()`
    - `Flow<T>.assertResult(expected: Flow<T>)`
    - `Flow<T>.assertResult(vararg values: T)`
    - `Flow<T>.assertResultSet(vararg values: T)`
    - `Flow<T>.assertResultSet(values: Iterable<T>)`
    - `inline fun <T, reified E : Throwable> Flow<T>.assertFailure(vararg values: T)`
    - `inline fun <reified E : Throwable> Flow<*>.assertError()`
  - 구현은 본 모듈 assertion DSL (`shouldBeEmpty`, `shouldBeEqualTo`, `shouldBeInstanceOf`)을 재호출.
  - 테스트 (`runTest` 사용):
    - 각 함수 passing/failing case.
    - `assertResult(vararg)`: 정확 순서 일치 검증.
    - `assertResultSet`: 순서 무관, 집합 동등 검증.
    - `assertFailure<T, E>`: 부분 emit 후 예외 발생 케이스.
    - `assertError<E>`: 예외 발생 검증.
- **의존**: T3, T6, T10

---

## T14. `bluetape4k-coroutines` FlowAssertions @Deprecated bridge

- **복잡도**: `medium`
- **대상 파일**:
  - `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/tests/FlowAssertions.kt` (수정)
- **수용 기준**:
  - **⚠️ spec §4.9 코드 예시 무시 (H3)**: spec §4.9의 bridge 예시 코드는
    새 모듈 함수 위임(`io.bluetape4k.assertions.coroutines.assertResult(this, *values)`)을 보여주지만
    이것은 ADR-6와 충돌하는 잘못된 예시다. **ADR-6 우선 적용**: 본문 위임 절대 금지.
  - 기존 7개 함수 시그니처는 변경하지 않는다 (소비자 binary 호환).
  - 각 함수에 `@Deprecated(message = "Moved to io.bluetape4k.assertions.coroutines.<fn>", replaceWith = ReplaceWith("...", "io.bluetape4k.assertions.coroutines.<fn>"), level = DeprecationLevel.WARNING)` 추가.
  - **함수 본문은 기존 구현 그대로 유지** (spec ADR-6: inline suspend는 위임 불가, 의존성 추가도 불가).
  - `bluetape4k-coroutines/build.gradle.kts`에 `bluetape4k-assertions` 의존성 **추가하지 않음**.
  - `./gradlew :bluetape4k-coroutines:compileKotlin` 통과 (deprecation WARNING만, 오류 없음).
  - `./gradlew :bluetape4k-coroutines:test` 통과 (기존 테스트 deprecation 경고만 발생).
  - 별도 `FlowAssertionsBridgeTest`로 deprecated 함수 호출 시 정상 동작 검증.
- **의존**: T13

---

## T15. `coroutines/TurbineSupport.kt` (compileOnly 격리)

- **복잡도**: `medium`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/coroutines/TurbineSupport.kt` (신규, ~100 lines)
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/coroutines/TurbineSupportTest.kt` (신규)
- **수용 기준**:
  - spec §4.10 시그니처 구현:
    - `suspend fun <T> ReceiveTurbine<T>.awaitItemAndAssert(expected: T): T`
    - `suspend fun <T> ReceiveTurbine<T>.awaitItemMatching(predicate: (T) -> Boolean): T`
    - `suspend inline fun <reified E : Throwable> ReceiveTurbine<*>.awaitErrorOfType(): E`
  - `app.cash.turbine.ReceiveTurbine` import 사용. build.gradle.kts에 `compileOnly(libs.turbine)` 명시.
  - KDoc 상단에 "Turbine 의존성 필요. `testImplementation(libs.turbine)` 추가" 명시.
  - 테스트:
    - `runTest` + `flow.test { ... }` 환경에서 각 함수 passing/failing case 검증.
    - 잘못된 타입 (`awaitItemAndAssert` 불일치 시) `AssertionFailedError` 발생.
  - **Turbine 격리 검증 (소비자 관점)**:
    - `CompatibilitySmokeTest` (T17)는 `TurbineSupport` import 없이 `Basic`/`Numerical`/`Collections`
      등 비-Turbine API만 사용한다. 이 테스트가 통과하면 Turbine 없는 소비자가
      `bluetape4k-assertions`를 정상 사용할 수 있음을 증명한다.
    - T17 DoD에 "Turbine import 없음" 체크 항목 추가 (T17 담당).
- **의존**: T13

---

## T16. kotlin.test 포팅 함수 추가 (3순위까지)

- **복잡도**: `medium`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Basic.kt` (수정 — `fail(msg, cause)`, `expectThat`, `shouldBeSameInstanceAs` / `shouldNotBeSameInstanceAs` 추가)
  - 위 외 1순위/2순위는 T4~T7에서 이미 포함됨; 본 작업은 누락 검증과 3순위 추가.
- **수용 기준**:
  - spec §4.11 표 기준 모든 항목이 적절한 파일에 존재함을 확인:
    - `Basic.kt`: `fail(message: String? = null, cause: Throwable? = null): Nothing`,
      `expectThat(expected, block)`, `expectThat(expected, message, block)`,
      `shouldBeSameInstanceAs`, `shouldNotBeSameInstanceAs`.
    - `Numerical.kt`: `shouldBeIn(ClosedRange/OpenEndRange/IntRange/LongRange/CharRange/UIntRange/ULongRange)`, `shouldNotBeNear`.
    - `CharSequences.kt`: `shouldContainRegex`, `shouldNotContainRegex`.
    - `Collections.kt`/`Arrays.kt`: `shouldContentEqual` 시리즈.
  - 각 추가 함수에 대한 테스트 케이스 작성 (passing/failing).
  - kotlin.test 코드는 직접 복사 안 함 — 독자 작성, opentest4j 기반 (Apache-2.0 영감만).
- **의존**: T4, T5, T6, T7, T8

---

## T17. 호환성 smoke test

- **복잡도**: `medium`
- **대상 파일**:
  - `testing/assertions/src/test/kotlin/io/bluetape4k/assertions/CompatibilitySmokeTest.kt` (신규)
- **수용 기준**:
  - spec §7.5 시나리오 구현:
    - `import io.bluetape4k.assertions.*` 만으로 다음 패턴이 컴파일/실행 통과:
      - `"abc" shouldBeEqualTo "abc"`
      - `listOf(1, 2, 3) shouldContainAll listOf(1, 2)`
      - `invoking { error("x") } shouldThrow IllegalStateException::class`
      - `mapOf("k" to "v") shouldContainKey "k"`
      - `intArrayOf(1, 2) shouldContentEqual intArrayOf(1, 2)`
      - `Instant.now() shouldBeAfter Instant.now().minusSeconds(1)`
      - `assertSoftly { add { 1 shouldBeEqualTo 1 }; add { "a" shouldStartWith "a" } }`
  - 본 테스트가 통과하면 spec §1.3 "import만 단순 치환으로 컴파일/통과" 성공 지표 충족.
  - **Turbine 격리 확인**: `CompatibilitySmokeTest.kt` 파일 내 `turbine` / `ReceiveTurbine` /
    `TurbineSupport` import가 0건임을 확인. 이로써 Turbine 미사용 소비자가 정상 동작함을 증명.
  - 파일에 `import io.bluetape4k.assertions.*` 줄 없음 (bluetape4k-assertions 미참조 검증).
- **의존**: T16

---

## T18. `bluetape4k-junit5`에 `bluetape4k-assertions` api 의존성 추가

- **복잡도**: `low`
- **대상 파일**:
  - `testing/junit5/build.gradle.kts` (수정)
- **수용 기준**:
  - `dependencies` 블록에 `api(project(":bluetape4k-assertions"))` 추가.
  - `api(libs.bluetape4k.assertions)`은 v1 단계에서 유지 (v2에서 제거 예정 — README에 명시).
  - `./gradlew :bluetape4k-junit5:build` 통과.
  - `./gradlew :bluetape4k-junit5:dependencies` 출력에서 `bluetape4k-assertions` 가
    api configuration에 표시됨을 확인.
- **의존**: T17

---

## T19. KDoc 한국어 패스 (전 함수)

- **복잡도**: `low`
- **대상 파일**:
  - `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/**/*.kt` (전체)
- **수용 기준**:
  - 모든 public 함수/클래스에 한국어 KDoc 작성. 최소 항목 (spec §6.1):
    1. 목적 한 줄 (요약).
    2. `## 동작/계약` 섹션 — 정확한 의미론, 실패 조건.
    3. 사용 예 코드 블록 (```kotlin ... ```).
    4. `@param` 모든 파라미터.
    5. `@return` 반환 값 (체이닝 receiver 명시).
  - 특별 KDoc 강조:
    - `shouldBe` (===) vs `shouldBeEqualTo` (==) 차이 첫 줄 큰 경고 (spec 부록 A 리스크).
    - `withMessage` 정확 일치 vs `withMessageContaining` 부분 일치.
    - `coInvoking.shouldThrow`의 `CancellationException` rethrow 동작.
    - `TurbineSupport`: Turbine `testImplementation` 필요 명시.
    - `assertSoftly`: 가상 스레드 안전, `add { }` 명시 필요.
  - IntelliJ KDoc 검사 통과 (linkable references 모두 해석됨).
  - **⚠️ T14 bridge 함수 KDoc**: `bluetape4k-coroutines` deprecated 함수에도 KDoc 업데이트 필요
    — 이전 위치, 대체 함수 참조, 제거 예정 버전(v2) 명시.
  - **KLogging/로깅 import 없음**: assertions 모듈은 `bluetape4k-logging` 미참조이므로
    어떤 파일에도 `KLogging`/`KotlinLogging` import가 없음을 확인.
- **의존**: T15, T16  ← T16이 Basic.kt에 신규 함수 추가하므로 T16 완료 후 KDoc 패스 실행

---

## T20. README.md + README.ko.md (Mermaid UML 포함)

- **복잡도**: `low`
- **대상 파일**:
  - `testing/assertions/README.md` (신규, English)
  - `testing/assertions/README.ko.md` (신규, Korean)
- **수용 기준**:
  - 두 파일 모두 다음 섹션 포함 (memory rule: Architecture → UML → Features → Examples 순):
    1. 제목 + 언어 토글 링크 (memory rule).
    2. **Overview / 개요**: 목적, 핵심 가치, JVM 21+/Kotlin 2.3+ 요구.
    3. **Architecture / 아키텍처**: 모듈 구성, 의존성 다이어그램.
    4. **Mermaid UML**:
       - 클래스 다이어그램: `SoftAssertionScope`, `InvokingBlock`, `CoInvokingBlock`, `Failures`, `Messages`.
       - 시퀀스 또는 flow 다이어그램: `assertSoftly` → `assertAll` 위임.
       - **Vega-Lite 사용 금지** (memory rule), 차트는 Mermaid `xychart-beta` 사용.
    5. **Features / 기능 카탈로그**: spec §4 모든 영역 (Basic, Numerical, CharSequences, Collections, Arrays, Maps, Exceptions, Reflection, DateTimes, Softly, FlowAssertions, TurbineSupport).
    6. **Examples / 사용 예**: spec §4의 사용 예 발췌.
    7. **bluetape4k-assertions 마이그레이션 가이드**: spec §5의 import 변경표 + 의도적 편차 표.
    8. **Turbine 사용 안내**: `testImplementation(libs.turbine)` 별도 추가 필요.
    9. **DateTime 지원 타입 표**: 7타입 × 4함수.
  - 각 섹션 코드 블록은 컴파일 가능한 형식.
  - `README.md`는 영문, `README.ko.md`는 한국어. 두 파일 동일 구조 유지.
- **의존**: T19

---

## T21. Detekt baseline 정리

- **복잡도**: `low`
- **대상 파일**:
  - `testing/assertions/detekt-baseline.xml` (필요 시 신규)
  - `testing/assertions/build.gradle.kts` (detekt 설정 확인)
- **수용 기준**:
  - `./gradlew :bluetape4k-assertions:detekt` 실행 시 0 issues 또는 정당화된 baseline 등록.
  - Baseline 허용 후보 (spec §6.4):
    - `LongParameterList` — 일반적으로 발생하지 않음, 발생 시 정당화.
    - `MagicNumber` — 메인 코드는 회피, 테스트는 자동 제외 (관례 확인).
    - `TooManyFunctions` — 28개 DateTime 함수 등 발생 시 정당화 baseline.
  - Baseline 추가 시 PR 본문에 사유 명시.
- **의존**: T20

---

## T22. code-reviewer agent 실행 + HIGH/CRITICAL 해결

- **복잡도**: `medium`
- **대상 파일**: 본 모듈 전체
- **수용 기준**:
  - code-reviewer 실행 (`oh-my-claudecode:code-reviewer` 또는 Codex native code-review 도구).
  - 보고된 모든 CRITICAL 이슈 해결.
  - 보고된 모든 HIGH 이슈 해결.
  - MEDIUM 이슈는 가능한 범위에서 해결, 미해결 항목은 PR 본문에 사유 명시.
  - Spec 준수 확인:
    - 함수 < 50 lines, 파일 < 800 lines (분할 필요 시 sub-file).
    - 깊은 중첩 (> 4 레벨) 없음.
    - `@Synchronized` / `synchronized {}` 사용 0건.
    - atomicfu 사용 0건 (assertion stateless).
    - `bluetape4k-*` 모듈 api/implementation 의존 0건 (ADR-9).
    - KLogging/KotlinLogging import 0건.
    - 모든 public API에 한국어 KDoc.
  - 최종 검증:
    - `./gradlew :bluetape4k-assertions:build` 통과.
    - `./gradlew :bluetape4k-assertions:test` 통과 (테스트 수, 시간 보고).
    - `./gradlew :bluetape4k-assertions:detekt` 통과.
    - `./gradlew :bluetape4k-coroutines:test` 통과 (bridge deprecation WARNING만).
    - `./gradlew :bluetape4k-junit5:build` 통과.
    - 가상 스레드 테스트 별도 실행: `./gradlew :bluetape4k-assertions:test --tests "*SoftlyVirtualThreadTest*"` 통과.
    - 호환성 smoke test 별도 실행: `./gradlew :bluetape4k-assertions:test --tests "*CompatibilitySmokeTest*"` 통과.
- **의존**: T21, T18  ← `bluetape4k-junit5:build` 검증이 T18(junit5 의존성 추가) 완료 후에 의미 있음

---

## DoD 매핑 (spec §10 ↔ 작업)

| Spec DoD 항목                                           | 담당 작업       |
|---------------------------------------------------------|-----------------|
| 모듈 빌드 성공                                          | T1, T22         |
| 모든 테스트 통과                                        | T22             |
| Korean KDoc 작성                                        | T19             |
| bluetape4k-assertions Keep 목록 모든 API 구현                          | T3~T13, T15     |
| `assertSoftly` 가상 스레드 안전성 (256 vthreads)        | T12             |
| FlowAssertions 이전 + bridge @Deprecated                | T13, T14        |
| `:bluetape4k-coroutines:test` 통과                      | T14, T22        |
| README.md + README.ko.md (Mermaid UML)                  | T20             |
| Detekt 통과                                             | T21             |
| bluetape4k-assertions 이름 호환 smoke test                             | T17             |
| Turbine compileOnly 격리                                | T15             |
| `:bluetape4k-assertions:detekt` 통과                    | T21, T22        |
| PR 본문 테스트 결과 / 호환성 결과 / README 업데이트     | T22 (PR 단계)   |
| code-reviewer HIGH/CRITICAL 0건                         | T22             |
| kotlin.test 포팅 함수 구현 + 테스트                     | T4, T5, T6, T7, T16 |
| `:bluetape4k-junit5`에 api 의존성 추가                  | T18             |

---

## 작업 요약 표

| ID  | 작업                                            | 복잡도 | 의존         |
|-----|-------------------------------------------------|--------|--------------|
| T0  | Spec §4.9 bridge 예시 수정 (선행)               | low    | -            |
| T1  | 모듈 스캐폴드 + Gradle 등록                     | low    | T0           |
| T2  | internal/Failures + Messages                    | medium | T1           |
| T3  | Basic.kt + 테스트                               | medium | T2           |
| T4  | Numerical.kt + 테스트                           | medium | T3           |
| T5  | CharSequences.kt + 테스트                       | medium | T3           |
| T6  | Collections.kt + 테스트                         | medium | T3           |
| T7  | Arrays.kt + 테스트 (8 primitive overload)       | medium | T3           |
| T8  | Maps.kt + 테스트                                | low    | T3           |
| T9  | Exceptions.kt + 테스트 (CancellationException 처리) | high | T3           |
| T10 | Reflection.kt + 테스트 (contract)               | medium | T3           |
| T11 | DateTimes.kt + 테스트 (7×4)                     | medium | T3           |
| T12 | Softly.kt + 가상 스레드 256 동시 테스트         | high   | T3           |
| T13 | coroutines/FlowAssertions.kt (canonical)        | high   | T3, T6, T10  |
| T14 | bluetape4k-coroutines bridge @Deprecated        | medium | T13          |
| T15 | coroutines/TurbineSupport.kt (compileOnly)      | medium | T13          |
| T16 | kotlin.test 포팅 (3순위 마무리)                 | medium | T4~T8        |
| T17 | 호환성 smoke test                               | medium | T16          |
| T18 | bluetape4k-junit5 api 의존성 추가               | low    | T17          |
| T19 | KDoc 한국어 패스                                | low    | T15, T16     |
| T20 | README.md + README.ko.md (Mermaid)              | low    | T19          |
| T21 | Detekt baseline                                 | low    | T20          |
| T22 | code-reviewer + 최종 검증                       | medium | T21, T18     |

총 23개 작업 (high 3, medium 12, low 8).

---

## 병렬 실행 권장 그룹

- **그룹 A (T3 완료 후 동시 실행 가능)**: T4, T5, T6, T7, T8
- **그룹 B (그룹 A 완료 후 동시 실행 가능)**: T9, T10, T11
- **그룹 C (T13 완료 후 동시 실행 가능)**: T14, T15

각 그룹 내 작업은 서로 독립적이므로 ultrawork 또는 team skill로 병렬 실행하면 효율적이다.

---

(끝)
