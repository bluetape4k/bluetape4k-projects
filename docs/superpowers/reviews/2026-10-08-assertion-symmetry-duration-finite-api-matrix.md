# Assertions/Core 검증 API 대칭 행렬

> 상태: Issue #1824의 최종 로컬 API 행렬. 아래 API 표의 `존재`/`누락`은 기준 commit의 상태이며, 최종 구현과 검증 결과는 뒤의 DoD 표에 기록한다. 독립 최종 리뷰와 원격 전달은 아직 끝나지 않았다.

## 기준과 범위

- Baseline source: `5fc36ff930d624674d852a364f7411c26522a56a`.
- 현재 원격 이슈: [Issue #1824](https://github.com/bluetape4k/bluetape4k-projects/issues/1824), 2026-10-11 조회 시 OPEN, milestone 2.1.0. 이슈 완료 조건은 PR/merge를 제외한다.
- 대상: `testing/assertions`의 공개 조건 함수 `shouldXxx` 및 `should(message, predicate)`, Core에서 승인 설계가 선택한 공통 `requireXxx`/`checkXxx` family.
- 행렬은 receiver와 인자 형태를 overload 단위로 분류한다. 기존 source API의 nullability, 반환형, parameter name, lazy-message shape는 source와 compile fixture에서 대조하고 JVM descriptor는 `javap` 결과에서 비교한다.
- 이슈의 모든 Assertions 조건 API는 overload별 긍정·부정 쌍을 제공한다. Core는 자주 쓰는 공통 family만 선택하며, 선택한 family 안에서는 `require`와 `check` 모두 긍정·부정 쌍을 제공한다.
- `Exceptions.kt`와 `Reflection.kt`는 기존 대칭 API를 읽기 전용 감사한다. 별도 예외 DSL, assertion context/보조 DSL, Flow/Turbine의 `assert*`, `TestAssertionSupport.shouldFail*`는 조건 assertion API가 아니므로 제외한다.

## Assertions API matrix

| Source owner / receiver | Positive family와 overload | 대응 negative / 상태 | Evidence owner |
|---|---|---|---|
| `Basic.kt`, `T` | `shouldBeEqualTo(T?)`; `shouldBe(T?)` | 각각 `shouldNotBeEqualTo(T?)`, `shouldNotBe(T?)` 존재 | `BasicTest` |
| `Basic.kt`, `T : Any` nullable | `shouldBeNull()` | `shouldNotBeNull()` 존재 | `BasicTest` |
| `Basic.kt`, `Boolean?` | `shouldBeTrue()`, `shouldBeFalse()` | 각각 `shouldNotBeTrue()`, `shouldNotBeFalse()` 존재 | `BasicTest` |
| `Basic.kt`, `T?` | `shouldBeSameInstanceAs(T?)` | `shouldNotBeSameInstanceAs(T?)` 존재 | `BasicTest` |
| `Basic.kt`, `T` | `fun <T> T.should(message: String, predicate: (T) -> Boolean): T` | `fun <T> T.shouldNot(message: String, predicate: (T) -> Boolean): T` 누락; predicate를 한 번 평가하는 정확한 논리 부정 추가 | `BasicTest`, Assertions compile fixture |
| `CharSequences.kt`, `T : CharSequence` nullable | `shouldStartWith(CharSequence)`, `shouldEndWith(CharSequence)`, `shouldContain(CharSequence)`, `shouldContainIgnoringCase(CharSequence)` | 각각의 `shouldNot...` pair 존재 | `CharSequencesTest` |
| `CharSequences.kt`, `T : CharSequence` nullable | `shouldBeEmpty()`, `shouldBeBlank()`, `shouldBeNullOrEmpty()`, `shouldBeNullOrBlank()` | 각각 `shouldNotBeEmpty()`, `shouldNotBeBlank()`, `shouldNotBeNullOrEmpty()`, `shouldNotBeNullOrBlank()` 존재 | `CharSequencesTest` |
| `CharSequences.kt`, `T : CharSequence` nullable | `shouldMatch(Regex)` | `shouldNotMatch(Regex)` 존재 | `CharSequencesTest` |
| `CharSequences.kt`, non-null `T : CharSequence` | `shouldContainRegex(Regex)`, `shouldContainRegex(String)` | 각각 `shouldNotContainRegex(Regex)`, `shouldNotContainRegex(String)` 존재; source signatures reject nullable receivers | `CharSequencesTest` |
| `CharSequences.kt`, `T : CharSequence` nullable | `shouldContainAll(vararg CharSequence)`, `shouldContainAll(Iterable<CharSequence>)`, `shouldContainNone(vararg CharSequence)`, `shouldMatch(String)`, `shouldBeEqualToIgnoringCase(String)` | 각 overload의 negative/positive counterpart 누락: `shouldNotContainAll` 두 overload, `shouldContainAny(vararg CharSequence)`, `shouldNotMatch(String)`, `shouldNotBeEqualToIgnoringCase(String)` | `CharSequencesTest`, Assertions compile fixture |
| `Arrays.kt`, `IntArray?`, `LongArray?`, `DoubleArray?`, `FloatArray?`, `ByteArray?`, `ShortArray?`, `CharArray?`, `BooleanArray?` | 각 `shouldContain(element)`, `shouldBeEmpty()` | 각 `shouldNotContain(element)`, `shouldNotBeEmpty()` 존재 | `ArraysTest` |
| `Arrays.kt`, 위 primitive 8종 + `Array<T>?` | 각 `shouldHaveSize(Int)` | 같은 receiver/인자의 `shouldNotHaveSize(Int)` 누락 | `ArraysTest`, Assertions compile fixture |
| `Arrays.kt`, 위 primitive 8종 + `Array<T>?` | 각 `shouldContentEqual(same-array-type?)` | 같은 receiver/인자의 `shouldNotContentEqual` 누락 | `ArraysTest`, Assertions compile fixture |
| `Arrays.kt`, `Array<T>?`, 7 primitive arrays (`Int`, `Long`, `Byte`, `Float`, `Double`, `Short`, `Char`) | 각 `shouldContainSame(same-array-type?)`; 추가로 `LongArray?.shouldContainSame(Iterable<Long>?)` | 각 overload의 `shouldNotContainSame` 누락. `BooleanArray.shouldContainSame`은 positive API도 없으므로 추가하지 않음 | `ArraysTest`, Assertions compile fixture |
| `Arrays.kt`, `Array<out T>?` 및 primitive array nullable | object-array content `shouldBeEqualTo(non-null expected)` 1개 + primitive-array content overload 8개 | content 의미를 유지하는 배열 전용 `shouldNotBeEqualTo(non-null expected)` 9개 누락. Nullable expected는 generic `BasicKt.shouldNotBeEqualTo(T?)` 참조 비교 overload를 계속 선택 | `ArraysTest`, Assertions compile fixture |
| `Collections.kt`, `Iterable<T>?` | `shouldContain(T)`, `shouldContainAny(Iterable<T>)`, `shouldContainAny(vararg T)`, `shouldBeEmpty()` | 각각 `shouldNotContain`, matching `shouldNotContainAny`, `shouldNotBeEmpty` overload가 존재 | `CollectionsTest` |
| `Collections.kt`, `Iterable<String>?` | `shouldContainIgnoringCase(String)` | `shouldNotContainIgnoringCase(String)` 존재 | `CollectionsTest` |
| `Collections.kt`, receiver `T` | `shouldBeIn(Iterable<T>)`, `shouldBeIn(Array<T>)` | `shouldNotBeIn(Iterable<T>)`, `shouldNotBeIn(Array<T>)` 존재 | `CollectionsTest` |
| `Collections.kt`, `Iterable<T>?` | `shouldContainAll(Iterable<T>)`, `shouldContainAll(vararg T)`, `shouldContainNone(Iterable<T>)`, `shouldNotContainAny(Iterable<T>)`, `shouldNotContainAny(vararg T)` | `shouldContainAll` 두 overload의 matching `shouldNotContainAll` 누락. 기존 `shouldContainAny`/`shouldContainNone` 의미와 두 `shouldNotContainAny` 호환 alias는 유지; 불필요한 `shouldNotContainNone`은 추가하지 않음 | `CollectionsTest`, Assertions compile fixture |
| `Collections.kt`, `Iterable<T>?` | `shouldMatchAllWith(predicate)`; `shouldMatchAtLeastOneOf(predicate)` | 각각 `shouldNotMatchAllWith(predicate)`, `shouldNotMatchAtLeastOneOf(predicate)` 누락 | `CollectionsTest`, Assertions compile fixture |
| `Collections.kt`, `Iterable<T>?` | `shouldHaveSize(Int)`, `shouldContentEqual(Iterable<T>?)`, `shouldContainSame(Iterable<T>?)` | 각 동일 overload의 `shouldNotHaveSize`, `shouldNotContentEqual`, `shouldNotContainSame` 누락 | `CollectionsTest`, Assertions compile fixture |
| `Collections.kt`, `Sequence<T>?` | `shouldContentEqual(Sequence<T>?)` | `shouldNotContentEqual(Sequence<T>?)` 누락 | `CollectionsTest`, Assertions compile fixture |
| `Maps.kt`, `Map<K,V>?` | `infix fun Map<K,V>?.shouldContainKey(K): Map<K,V>`; `shouldContainValue(V): Map<K,V>`; `shouldContain(Pair<K,V>): Map<K,V>`; `shouldBeEmpty(): Map<K,V>?` | 각각 `shouldNotContainKey(K): Map<K,V>?`, `shouldNotContainValue(V): Map<K,V>?`, `shouldNotContain(Pair<K,V>): Map<K,V>?`, `shouldNotBeEmpty(): Map<K,V>` 존재 | `MapsTest` |
| `Maps.kt`, `Map<K,V>?` | `infix fun Map<K,V>?.shouldHaveSize(Int): Map<K,V>`; `shouldContainSame(Map<K,V>?): Map<K,V>` | `shouldNotHaveSize(Int): Map<K,V>?` 및 `shouldNotContainSame(Map<K,V>?): Map<K,V>?` 누락 | `MapsTest`, Assertions compile fixture |
| `DateTimesInstantZoned.kt`, `Instant`, `ZonedDateTime`, `OffsetDateTime` | 각 receiver × `shouldBeAfter/Before/OnOrAfter/OnOrBefore(same type)` | 각 12 positive overload의 `shouldNotBeAfter/Before/OnOrAfter/OnOrBefore(same type)` 누락 | `JavaDateTimesTest` nested owners |
| `DateTimesLocal.kt`, `LocalDateTime`, `LocalDate`, `LocalTime` | 각 receiver × `shouldBeAfter/Before/OnOrAfter/OnOrBefore(same type)` | 각 12 positive overload의 matching negative 누락 | `JavaDateTimesTest` nested owners |
| `DateTimesLegacy.kt`, `Date` | `shouldBeAfter/Before/OnOrAfter/OnOrBefore(Date)` | 네 matching `shouldNotBe... (Date)` 누락 | `DateTimesTest.JavaUtilDateAssertions` |
| `Numerical.kt`, `T : Comparable<T>` | `shouldBeGreaterThan`, `shouldBeGreaterOrEqualTo`, `shouldBeLessThan`, `shouldBeLessOrEqualTo` | 각각 `shouldNotBeGreaterThan`, `shouldNotBeGreaterOrEqualTo`, `shouldNotBeLessThan`, `shouldNotBeLessOrEqualTo` 존재 | `NumericalTest` |
| `Numerical.kt`, comparable + unsigned/numeric range receivers | `shouldBeInRange(ClosedRange<T>)`; `shouldBeInRange(UIntRange)` for `UByte/UShort/UInt`; `shouldBeInRange(ULongRange)`; `shouldBeIn(ClosedRange<T>)`, `shouldBeIn(OpenEndRange<T>)`, and primitive `IntRange/LongRange/CharRange/UIntRange/ULongRange` | 각 overload의 `shouldNotBeInRange`/`shouldNotBeIn` 존재 | `NumericalTest` |
| `Numerical.kt`, `Byte`, `Short`, `Int`, `Long`, `Float`, `Double` | 각 `shouldBePositive`, `shouldBeNegative`, `shouldBeZero` (18 positive overload) | 같은 receiver별 `shouldNotBePositive`, `shouldNotBeNegative`, `shouldNotBeZero` (18 overload) 누락. Float/Double은 strict-comparison 논리 부정이며 NaN은 negative에서 성공 | `NumericalTest`, Assertions compile fixture |
| `Numerical.kt`, `Float`, `Double` | finite-sign positive/negative pair 및 `shouldBeFinite()` | `shouldNotBeFinite()`와 finite-sign 반대 조건 추가. General sign API에는 finite 조건을 넣지 않음; finite-sign에서 NaN/±∞는 네 조건 모두 거부 | `NumericalTest`, Assertions compile fixture |
| `Numerical.kt`, `Float`, `Double` | `shouldBeNear(expected, tolerance = default)` | 기존 `shouldNotBeNear(expected, delta)`와 1-argument overload 보존; 두 API가 같은 비음수·비NaN tolerance를 쓰며 양수 무한 tolerance도 허용. Negative 조건은 positive의 정확한 부정이며 기본 tolerance 일치 | `NumericalTest`, Assertions compile fixture |
| `Numerical.kt`, `BigDecimal` | `shouldBeNear(expected, tolerance = BIGDECIMAL_EPSILON)` | 같은 tolerance의 `shouldNotBeNear` 누락 | `NumericalTest`, Assertions compile fixture |
| `DurationAssertions.kt`, `java.time.Duration`, `kotlin.time.Duration` | 신규 `shouldBePositive`, `shouldBeZeroOrPositive` | 신규 `shouldNotBePositive`, `shouldBeNegative`와 각 의미상 pair. Kotlin ±infinity는 sign-only; finite timeout 검사와 별개 | 신규 `JavaDurationsTest`, Assertions compile fixture |
| `Exceptions.kt`, `InvokingBlock` | `infix fun <T : Throwable> shouldThrow(expectedType: KClass<T>): T`; `fun shouldNotThrow(): Any?` | 두 선언이 존재하는 동기 예외 pair로 감사 완료; 구현 변경 없음 | `ExceptionsTest`, Assertions compile fixture |
| `Exceptions.kt`, `CoInvokingBlock` | suspend `infix fun <T : Throwable> shouldThrow(expectedType: KClass<T>): T`; `suspend fun shouldNotThrow(): Any?` | 두 선언이 존재하는 suspend 예외 pair로 감사 완료; 구현 변경 없음 | `ExceptionsTest`, Assertions compile fixture |
| `Reflection.kt`, `Any?` | `inline fun <reified T : Any> Any?.shouldBeInstanceOf(): T` | `inline fun <reified T : Any> Any?.shouldNotBeInstanceOf(): Any?` 존재 | `ReflectionTest`, Assertions compile fixture |
| `Reflection.kt`, `Any?` | `infix fun Any?.shouldBeInstanceOf(klass: KClass<*>): Any` | `infix fun Any?.shouldNotBeInstanceOf(klass: KClass<*>): Any?` 존재 | `ReflectionTest`, Assertions compile fixture |

### Assertions contract notes

- Array equality positive/negative overload는 non-null expected에서 content comparison을 사용한다. Null receiver의 기존 positive legacy 성공 결과는 negative에서 실패한다. Nullable expected는 기존 generic reference comparison을 유지하며 nullable content comparison은 `shouldContentEqual` pair로 한다.
- CharSequence의 `shouldContainAll`은 empty candidate에서 성공하고 `shouldNotContainAll`은 실패한다. `shouldContainAny()`는 후보가 없으면 실패하고 `shouldContainNone()`은 성공한다. Null receiver의 family별 기존 결과와 nullable 반환형은 runtime tests 및 compile fixture에서 기록한다.
- `Iterable.shouldMatchAllWith`는 null을 빈 입력으로 취급한다. `shouldMatchAtLeastOneOf`는 null/empty에서 실패한다. Negative는 모든 입력에서 해당 positive 조건의 정확한 논리 부정이고, 방문 원소당 predicate는 최대 한 번 평가한다.
- Sequence/Iterable equality와 same-content family는 null, empty, identity, one-shot, mismatch, return receiver를 각 overload에 맞춰 확인한다. Float/Double array content 비교의 NaN과 signed-zero 의미를 보존한다.
- Baseline `Map.shouldContain(Pair<K,V>)`/`shouldNotContain(Pair<K,V>)`는 `this[key]` 결과만 비교하므로 missing key + `null` value를 existing null-valued key와 구분하지 못한다. 구현 시 `containsKey`와 value equality를 함께 확인하고 회귀 테스트를 추가한다.
- `Map.shouldHaveSize(Int)`는 null receiver에서 실패한다. `Map.shouldBeEmpty()`는 null을 empty로 보아 성공하고 nullable 반환형을 쓴다. `shouldNotBeEmpty()`는 null에서 실패한다. `shouldContainSame(Map?)`은 양쪽 null에서 성공하고 nullable receiver를 non-null Map으로 반환하며 한쪽 null에서는 실패한다. `shouldNotContainSame`은 이 성공/실패를 반대로 하고 실제 null receiver가 성공하는 경우 null을 반환한다.
- `Map.shouldNotHaveSize(Int)`는 null receiver에서 성공하고 null을 반환한다. 이는 null에서 실패하는 `shouldHaveSize(Int)`의 논리적 부정이며 compile fixture가 nullable 반환형을 고정한다.
- Exceptions의 `withMessage*`, `withCause`, `with`와 `assertFailsWith`, `assertFails`, `assertNotFailsWith`, `assertNotFails`, `assertTimeout`은 보조 DSL/유틸리티이므로 이 조건 API 행렬에서 제외한다.

## Core 선택 API matrix

Core는 Assertions 전체를 복제하지 않는다. 아래 선택한 common helper에만 각 positive/negative pair를 `require`와 `check` 모두 제공한다. 기존 overload shape, parameter name, nullable 반환형과 lazy-message callback 규칙을 compile fixture/runtime test에서 확인한다.

| Family / receiver | `require` pair | `check` pair | Overload coverage / status |
|---|---|---|---|
| Equality, `T` | `requireEquals` / `requireNotEquals` | `checkEquals` / `checkNotEquals` | `requireNotEquals` 누락; `check` pair 존재 |
| Null-or-empty / not-empty, `CharSequence?`, `Array<*>?`, `Collection<*>?`, `Map<*,*>?` | `requireNullOrEmpty` / `requireNotEmpty` | `checkNullOrEmpty` / `checkNotEmpty` | CharSequence pair 존재; Array/Collection/Map의 null-or-empty overload 추가. 성공 nullable return 및 기존 nullable input 의미 확인 |
| Blank, `CharSequence?` | `requireNullOrBlank` / `requireNotBlank` | `checkNullOrBlank` / `checkNotBlank` | 함수 pair 존재; 잘못된 null-only contracts 제거하고 runtime semantics 유지 |
| CharSequence contains/starts/ends | `requireContains` / `requireNotContains`; `requireStartsWith` / `requireNotStartsWith`; `requireEndsWith` / `requireNotEndsWith` | 대응하는 `check` pairs | negative counterparts 누락. EndsWith의 lazy-message overload shape와 named overload의 receiver predicate를 확인 |
| Map contains/key/value | `requireContains(key,value)` / `requireNotContains`; `requireHasKey` / `requireNotHasKey`; `requireHasValue` / `requireNotHasValue` | 대응하는 `check` pairs | negative counterparts 누락; missing key + null value 처리에서 `containsKey` 우선 확인 |
| Numeric ranges | `requireInRange` / `requireNotInRange`; `requireInOpenRange` / `requireNotInOpenRange` | 대응하는 `check` pairs | negative counterparts 추가; 빈 range와 양끝 경계 고정 |
| Length/size ranges | `requireLengthInRange` / `requireNotLengthInRange`; `requireSizeInRange` / `requireNotSizeInRange` | 대응하는 `check` pairs | `size`의 Collection, Map, Array overload 모두 대칭; 기존 parameter/lazy shape 유지 |
| Float/Double finite | `requireFinite` / `requireNotFinite` | `checkFinite` / `checkNotFinite` | Float/Double primitive overload 추가 |
| Sign-only numbers, `Number` overloads | `requirePositiveNumber` / `requireZeroOrNegativeNumber`; `requireZeroOrPositiveNumber` / `requireNegativeNumber` | 대응하는 `check` pairs | 기존 pair 유지. Float/Double NaN 및 infinities 결과를 finite-sign과 분리 검증 |
| Finite sign, `Float`, `Double` | `requireFinitePositiveNumber` / `requireFiniteZeroOrNegativeNumber`; `requireFiniteZeroOrPositiveNumber` / `requireFiniteNegativeNumber` | 대응하는 `check` pairs | 신규 primitive-only overload. NaN/±∞는 모두 실패, finite 값에서는 pair complement |
| Java Duration | `requirePositiveDuration` / `requireZeroOrNegativeDuration`; `requireZeroOrPositiveDuration` / `requireNegativeDuration` | 대응하는 `check` pairs | 신규 `java.time.Duration` overload |
| Kotlin Duration | `requirePositiveDuration` / `requireZeroOrNegativeDuration`; `requireZeroOrPositiveDuration` / `requireNegativeDuration` | 대응하는 `check` pairs | 신규 `kotlin.time.Duration` overload; ±`Duration.INFINITE`는 sign-only |

### Core overload and callback shapes

- `requireEquals`/`requireNotEquals` and `checkEquals`/`checkNotEquals` use the existing generic receiver/expected type and both call shapes: `(expected, parameterName: String)` and `(expected, lazyMessage: () -> Any)`.
- `NullOrEmpty`/`NotEmpty` and `NullOrBlank`/`NotBlank` use nullable `CharSequence<T>`, generic `Array<T>?`, `Collection<T>?`, and `Map<K,V>?` receivers as approved. Match each positive helper's `(parameterName: String)` and `lazyMessage: () -> Any` overloads; preserve nullable return from `NullOrEmpty`/`NullOrBlank` and non-null return from `NotEmpty`/`NotBlank`.
- CharSequence `Contains` uses `(other: CharSequence, parameterName: String)` and `(other: CharSequence, lazyMessage: () -> Any)`. `StartsWith`/`EndsWith` preserve `(prefixOrSuffix: CharSequence, parameterName: String, ignoreCase: Boolean = false)` and `(prefixOrSuffix: CharSequence, ignoreCase: Boolean = false, lazyMessage: () -> Any)` call shapes. The approved `EndsWith` correction also adds the currently missing lazy-message overload and validates the receiver.
- Map contains/key/value pairs preserve `(keyOrValue, parameterName: String)` and `(keyOrValue, lazyMessage: () -> Any)` shapes; key/value `Contains` includes both key and value. Null Map is rejected by both signs, while non-null empty Map passes negative predicates.
- `InRange`/`InOpenRange` preserve generic `T : Comparable<T>` receiver, `(start, end, parameterName: String)` and `(start, end, lazyMessage: () -> Any)` overloads. `LengthInRange` preserves nullable `T : CharSequence`; `SizeInRange` preserves nullable `Collection<T>`, `Map<K,V>`, and `Array<T>` overloads. Each uses `(start, endInclusive, parameterName: String, noinline lazyMessage: (() -> Any)? = null)`.
- General sign helpers use `T where T : Number, T : Comparable<T>` with both `(parameterName: String)` and `(lazyMessage: () -> Any)` overloads. Preserve these sign-only predicates and their existing `require`/`check` pairs.
- `Finite`/`NotFinite` and all four finite-sign helpers use primitive `Float` and `Double` receiver overloads with `(parameterName: String, noinline lazyMessage: (() -> Any)? = null)`. No generic `Number` finite-sign overload is allowed.
- Java/Kotlin Duration quartets each use one `(parameterName: String, noinline lazyMessage: (() -> Any)? = null)` receiver declaration per function and package. Every successful helper returns the original receiver.

## Final implementation evidence

| Evidence | Baseline | Final | Status |
|---|---|---|---|
| Source declarations and overloads | 기준 commit source와 공개 owner inventory | Assertions 전체 대칭 쌍, Core 선택 family의 require/check 쌍, Java/Kotlin Duration 및 Float/Double 유한수 API를 표·fixture와 대조 | PASS |
| Runtime contracts | 기존 테스트 결과 | Assertions 707건, Core 1,717건; 실패·오류·skip 0. nullable receiver, 비교 경계, callback, NaN/Infinity, Duration 및 Map 테스트 포함 | PASS |
| Compile/API fixtures | 기존 API 사용 코드 | Assertions와 Core test compile fixture에서 반환형·nullability·overload를 고정; 전체 테스트에서 컴파일 | PASS |
| JVM ABI | Assertions 기존 owner 12개, Core 기존 owner 6개 | 기준 `javap` 대비 기존 descriptor 제거 0개. Assertions 기존 owner에서 120개, Core owner에서 256개 descriptor 추가; Duration owner 1개 추가 | PASS |
| Kotlin source contract | 기준 source와 공개 signature | parameter name과 nullable 반환 계약을 source 및 compile fixture에서 대조. 잘못된 null-only contract 2개 제거 | SOURCE/FIXTURE PASS; Kotlin metadata diff 미수행 |
| Detekt | Assertions 36건, Core 269건 | Assertions 35건, Core 269건. Assertions `TooManyFunctions` 6→7은 Map API 추가로 `Maps.kt`가 10→12 함수가 된 예상 진단; 다른 새 rule 진단 0. `MagicNumber` 2건 해소 | PASS WITH EXPECTED `TooManyFunctions` |
| Runner checks | task-local runner 기반 | exact head `f31dd91a0c426a7213e69d75a7f79c11dad04feb`: Assertions 707건 (`20261011T205532Z-a91d8200`), Core 1,718건 (`20261011T205618Z-b73c0012`); runner unittest 16건 및 py_compile, `git diff --check` 통과 | PASS |

## 리뷰 finding과 남은 gate

| Finding | 근거와 조치 | 상태 |
|---|---|---|
| `requireEndsWith` named overload가 receiver 대신 `parameterName`을 null 검증 | 기준 코드의 `requireNotNull(parameterName)`을 `this.requireNotNull(parameterName)`로 고치고 named/lazy overload의 null receiver 회귀를 추가 | 수정 및 테스트 |
| Float/Double `shouldNotBeNear`가 positive predicate의 부정과 달라질 수 있음 | 유효 tolerance에서 `!(abs(this - expected) > tolerance)`로 보완; NaN operands, 같은 infinities, 양의 무한 tolerance 경계를 테스트 | 수정 및 테스트 |
| Double infix negative-near KDoc/default epsilon 불일치 | KDoc을 `1e-9`로 고치고 `DOUBLE_EPSILON` 상수 사용 | 수정 |
| `Map.shouldNotHaveSize(Int)`의 null 결과와 nullable 반환형 | positive 조건의 정확한 부정에 맞춰 null receiver를 통과시키고 `Map<K,V>?` 반환을 compile/runtime fixture로 고정 | 수정 및 테스트 |
| Duration README의 존재하지 않는 `shouldNotBeNegative()` 항목 | 잘못된 행을 삭제하고 실제 Duration quartet을 두 README locale에서 대조 | 수정 |
| 독립 최종 코드 리뷰 lane | 등록된 reviewer lane은 취소 또는 BLOCKED 상태로 남아 있음. 아래 주 세션 inline fallback 결과는 독립 리뷰 증거를 대체하지 않음 | PENDING — inline 결과 별도 기록 |

## 주 세션 inline fallback 최종 코드 리뷰 (2026-10-12)

독립 reviewer lane이 완료되지 않아 주 세션에서 승인된 inline fallback 리뷰를 수행했다. `d9e175f240b16b4f6d6964ff35867ec9d09cd739..f31dd91a0c426a7213e69d75a7f79c11dad04feb` 변경을 검토했다. 최신 base의 추가 변경은 `ClassSupport`와 그 테스트에 한정되어 이 PR의 Assertions/Core API 영역과 겹치지 않음을 확인한 뒤, 변경된 API, 테스트, fixture, README, runner 및 계획·검토 문서에서 정확성, 부정 쌍의 경계, null 동작, 유한수·Duration 의미, JVM/API 호환성, 검증 artifact 연결을 다시 확인했다. 아래 P2 두 건과 검증 추적성 한 건은 모두 수정했다.

**Inline verdict: PASS; P0=0, P1=0.**

| Finding | 근거와 조치 | 상태 |
|---|---|---|
| P2 — `shouldNotContainAll` KDoc의 null/빈 후보 설명이 구현과 불일치 | `CharSequences.kt:419-420` 설명을 null receiver는 빈 후보 목록에서도 통과한다고 바로잡음. `AssertionSymmetryTest.kt:38-50`에 빈 후보·vararg·null receiver 경계를 추가 | 수정 및 Assertions 테스트 통과 |
| P2 — 날짜/시간 부정 비교의 equality 경계 검증 누락 | `AssertionSymmetryTest.kt:108-179`에서 `Instant`, `ZonedDateTime`, `OffsetDateTime`, `LocalDateTime`, `Date`, `LocalDate`, `LocalTime` 각각에 strict 비교 equality 통과와 inclusive 비교 equality 실패를 추가 | 수정 및 Assertions 테스트 통과 |
| 검증 추적성 — runner가 새 symmetry 테스트 결과를 필수 artifact로 확인하지 않음 | `assertion_symmetry_runner.py:63-89,217-220`에서 baseline/final 클래스 목록을 나누고 최종 Assertions에 `AssertionSymmetryTest`를 요구. `test_assertion_symmetry_runner.py:119-127`에 계약 테스트 추가 | 수정; runner unittest 16건 통과 |

독립 reviewer lane 세 개의 cancelled/BLOCKED 상태는 workflow receipt에 그대로 보존한다. 이 inline 리뷰는 같은 주 세션에서 수행했으므로 독립성 증거가 아니다. Workflow completion receipt는 구현/final-verification 구성요소와 reviewer lane 흔적이 남아 있어 완료 처리하지 않았다.

## 작성 및 통합 검토 DoD

`docs/superpowers/reviews/2026-10-08-assertion-symmetry-duration-finite-api-matrix.md`와 `docs/lessons/2026-10-12-issue-1824-assertion-complements.md`에 각각 SPW-01..05와 KO-01..07을 적용했다. 아래 PASS는 두 문서를 각각 점검했다는 기록이다.

### SPW-01..05

| Gate | API matrix | Lesson |
|---|---|---|
| SPW-01 | PASS — maintainer 대상, baseline source, live issue, final runner output을 고정 | PASS — Kotlin 기여자 대상, 기준 코드·회귀·사용자 수정 범위 고정 |
| SPW-02 | PASS — 기준 범위, API 행렬, final evidence, finding disposition, 미완료 gate 포함 | PASS — 맥락·결정·결과·검증·실패 가정·재발 방지 guard 포함 |
| SPW-03 | PASS — 기술 용어와 API 토큰을 일관되게 유지; KO checklist 완료 | PASS — 엔지니어링 한국어로 사실과 불확실성 분리; KO checklist 완료 |
| SPW-04 | PASS — 이슈·기준 source·final test/ABI/Detekt 증거와 대조 | PASS — 현재 source, 테스트 XML, runner/Detekt 보고서와 대조 |
| SPW-05 | PASS — 전체 표와 finding을 다시 읽음; inline verdict와 독립 리뷰 PENDING을 구분 | PASS — 문서 전체 재독 및 링크/식별자 확인 |

### KO-01..07

| Gate | API matrix | Lesson |
|---|---|---|
| KO-01 | PASS — baseline `5fc36ff`, issue #1824, runner IDs와 36→35/269→269 결과 고정 | PASS — 같은 기준·검증 결과 및 리뷰 lane 불확실성 보존 |
| KO-02 | PASS — 효과 주장을 API·테스트·진단 수치로 제한 | PASS — 교훈을 다섯 구체 finding과 예방 절차로 제한 |
| KO-03 | PASS — 번역투와 불필요한 전환 표현 점검 | PASS — 사건 일지 대신 반복 가능한 guard로 정리 |
| KO-04 | PASS — 긍정/부정, receiver, tolerance, 유한수 용어 확인 | PASS — receiver, nullable, 논리 부정 용어를 동일한 뜻으로 사용 |
| KO-05 | PASS — 은유·농담 없음 | PASS — 은유·농담 없음 |
| KO-06 | PASS — 제목·표·issue 링크·API 이름 재확인 | PASS — 제목·표·코드 token·issue 링크 재확인 |
| KO-07 | PASS — `node .../audit-korean-terms.mjs` 결과 기록 | PASS — 동일 audit의 해당 파일 결과 기록 |
