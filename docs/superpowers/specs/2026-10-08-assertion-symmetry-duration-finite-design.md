# assertion 대칭성과 Duration·유한수 검증 설계

## 상태와 목적

- 상태: 여섯 관점 통합 설계 리뷰 완료, 사용자 스펙 승인 완료
- 저장소: `bluetape4k-projects`
- 대상 모듈: `bluetape4k-assertions`, `bluetape4k-core`
- 기준 커밋: `5fc36ff930d624674d852a364f7411c26522a56a`
- 목표: assertions에서는 의미상 짝이 되는 긍정·부정 함수를 모두 제공하고, core에서는 자주 쓰는 검증만 선별하되 선택한 검증의 긍정·부정 쌍을 맞춘다.

## 문제와 요구사항

`bluetape4k-assertions`에는 일부 `shouldXxxx` 함수의 부정 짝이 없고, 일부 짝은 오버로드 전체에 제공되지 않는다. `bluetape4k-core`도 `requireXxxx`와 `checkXxxx` 중 한쪽에만 부정형이 있는 경우가 있다. 숫자 검증은 `Float`와 `Double`의 유한 여부를 부호 검증과 분리해 표현할 필요가 있다. Duration의 부호 검증은 Java와 Kotlin 타입을 모두 다뤄야 한다.

요구사항은 다음과 같다.

1. assertions의 공개 조건 assertion 전체에서 의미상 대응하는 긍정·부정 함수가 모두 존재해야 한다. 기존 쌍도 receiver 및 인자 오버로드마다 확인하고, 누락된 쌍은 전부 추가한다. `should(message, predicate)`도 이 규칙에 포함한다.
2. core는 assertions 전체를 1:1로 복제하지 않는다. 자주 쓰는 검증만 선택하고, 선택한 각 검증은 `require`와 `check` 모두에서 긍정·부정 짝을 제공한다.
3. Java `Duration`과 Kotlin `Duration`에 대해 양수 및 0 이상 검증을 제공하고, 각 검증의 부호 반대 조건도 제공한다.
4. `Float`과 `Double`에 대해 유한수 assertion과 유한 조건을 포함한 부호 검증을 제공한다. 기존 범용 Number 검증의 동작은 유지한다.
5. 공개 함수의 반환 형태, 사용자 메시지 전달 방식, 실패 예외 유형은 기존 모듈 계약을 따른다.

## 대안과 결정

| 대안 | 범위 | 결과 |
|---|---|---|
| assertions와 core 전부 1:1 복제 | assertions의 각 조건에 짝을 추가하고 core에서도 모든 assertion을 복제 | core에 불필요한 공개 API가 늘어나므로 제외 |
| assertions 전체 대칭 + core의 자주 쓰는 대칭 쌍 | assertions의 모든 조건 assertion을 오버로드까지 대칭으로 맞추고, core는 많이 쓰는 검증만 선별해 각 검증의 쌍을 제공 | 사용자 요구와 일치하므로 채택 |
| 자주 쓰는 함수만 양쪽에서 보완 | assertions와 core 모두 빈도가 높은 일부만 추가 | assertions 전체 대칭 요구를 충족하지 못하므로 제외 |

## 현재 근거

- `testing/assertions/src/main/kotlin/io/bluetape4k/assertions`의 공개 `shouldXxxx` 선언을 전수 조사하고, `should(message, predicate)`도 별도로 확인했다. 오버로드별로 대응하는 부정 함수가 있는지 검사했으며, 이름이 달라도 조건이 정확히 반대인 함수는 기존 짝으로 인정한다.
- 빠진 assertion 쌍은 `Arrays.kt`, `Basic.kt`, `CharSequences.kt`, `Collections.kt`, `DateTimesInstantZoned.kt`, `DateTimesLegacy.kt`, `DateTimesLocal.kt`, `Maps.kt`, `Numerical.kt`에서 확인했다. 반사 검사, 기본 equality/null/Boolean/identity, 일반 comparable 및 range 비교, 예외 DSL 등 나머지 공개 `shouldXxxx` family에는 대응 함수가 이미 있다.
- `CharSequences.kt`에는 `shouldMatch(String)`의 대응 overload, `shouldContainNone(vararg)`의 긍정 counterpart, `shouldContainAll`의 부정 overload와 대소문자 무시 equality의 부정형이 없다. `Numerical.kt`의 `BigDecimal.shouldBeNear`에도 같은 허용 오차를 사용하는 부정형이 없다.
- Iterable/Sequence/Map/Array 구조 비교와 predicate 조건에서도 부정형이 빠져 있다. 배열 비교는 generic equality가 참조 비교로 바뀌지 않도록 각 기존 배열 overload의 content semantics를 그대로 유지해야 한다.
- core의 Kotlin 소스에서 helper 구현 파일을 제외해 사용처를 검색했다(테스트와 문서 예제 포함). 예를 들어 `requireInRange` 148회, `requireNotEmpty` 73회, `requireZeroOrPositiveNumber` 150회, `requirePositiveNumber` 266회, `requireContains` 11회, `requireStartsWith` 8회, `requireEndsWith` 6회, `requireSizeInRange` 7회, `requireHasKey`/`requireHasValue` 각 6회가 검색된다. core 보완 범위는 빈도와 일반적인 입력 검증인지 함께 고려한다.
- `Numerical.kt`의 `Comparable` 비교 assertion은 두 Duration 타입에도 적용 가능하다. 이 비교 API를 중복 추가하지 않고, 이름 있는 부호 assertion을 보완한다.
- `DurationSupport.kt`는 Java Duration의 `isNotPositive`와 `isNotNegative`를 제공한다. `KotlinDurationSupport.kt`에는 부호 검증 helper가 없다.
- core에는 `Float.requireFinite`, `Double.requireFinite`와 `checkFinite`가 이미 있다. 일반 Number 부호 검증은 유한성 조건을 포함하지 않는다.
- 변경 전 기준 검증: `./gradlew :bluetape4k-core:test :bluetape4k-assertions:test` 성공, assertions와 core에서 1,711 tests 통과.

## 설계

### assertions의 대칭 계약

범위는 `bluetape4k-assertions`가 제공하는 공개 조건 assertion 전체다. `shouldXxxx` 함수와 이름이 `should`인 일반 predicate 함수의 모든 기존·신규 의미상 긍정·부정 쌍을 receiver 및 인자 오버로드까지 대조한다. 기존 API에서 `shouldContainAny`/`shouldContainNone`처럼 이름이 다른 함수가 정확한 반대 조건을 표현하면 그 쌍을 유지하고 중복 alias를 추가하지 않는다. 조건을 검증하지 않는 assertion context/보조 DSL과 Flow/Turbine의 별도 `assert*` API는 조건 assertion 대칭 대상이 아니며, `TestAssertionSupport.shouldFail*`도 다른 assertion을 시험하는 보조 함수라 대상에서 제외한다.

전수 조사에서 보완할 family는 다음과 같다. 기존 긍정 함수의 동작은 유지하고, 같은 오버로드에 대응하는 부정형을 추가한다.

| 영역 | 대칭을 완성할 API | 적용 범위 |
|---|---|---|
| predicate | `should(message, predicate)` ↔ `shouldNot(message, predicate)` | 같은 receiver에서 predicate를 한 번 평가한다. 부정형은 논리 부정을 직접 적용하며 긍정형 예외를 catch해 성공으로 바꾸지 않는다. predicate 예외의 타입과 동일 객체 전파를 검증한다. |
| CharSequence 다중 포함 | 새 `shouldContainAny(vararg CharSequence)` ↔ 기존 `shouldContainNone(vararg CharSequence)`; `shouldContainAll` ↔ `shouldNotContainAll` | `shouldContainAll`의 `vararg` 및 `Iterable` overload 각각 대응 |
| CharSequence 문자열 조건 | `shouldBeEqualToIgnoringCase` ↔ `shouldNotBeEqualToIgnoringCase`; `shouldMatch` ↔ `shouldNotMatch` | Regex pair는 존재; String overload의 빠진 부정형 추가 |
| Iterable 다중 포함 | `shouldContainAll` ↔ `shouldNotContainAll` | `Iterable<T>`와 `vararg T` 각각 대응 |
| Iterable predicate | `shouldMatchAllWith` ↔ `shouldNotMatchAllWith`; `shouldMatchAtLeastOneOf` ↔ `shouldNotMatchAtLeastOneOf` | null 및 빈 Iterable의 현재 의미를 정확히 반영하고 원소별 predicate 1회 평가 및 sentinel 예외의 동일 객체 전파를 검증한다. |
| 크기 | `shouldHaveSize` ↔ `shouldNotHaveSize` | 기존 Iterable, Map, generic/primitive Array overload 전체 |
| 순서 포함 내용 비교 | `shouldContentEqual` ↔ `shouldNotContentEqual` | Iterable, Sequence, generic/primitive Array overload 전체 |
| 순서 무관 동일 원소 비교 | `shouldContainSame` ↔ `shouldNotContainSame` | Iterable, Map, 그리고 기존 generic/primitive Array overload 전체 |
| 배열 equality | non-null 배열 expected에 대한 각 배열 `shouldBeEqualTo` ↔ `shouldNotBeEqualTo` | 기존 generic 및 8개 primitive 배열 overload, content equality 유지. 신규 배열 전용 negative overload가 generic `shouldNotBeEqualTo`보다 우선한다. nullable expected는 기존 generic overload를 선택하며 참조 equality를 유지한다. nullable expected의 content 비교에는 `shouldNotContentEqual`을 사용한다. |
| 시간 비교 | `shouldBeAfter/Before/OnOrAfter/OnOrBefore` ↔ 각 `shouldNotBe...` | Java `Date`, `LocalDate`, `LocalTime`, `LocalDateTime`, `Instant`, `ZonedDateTime`, `OffsetDateTime`의 전체 28 overload |
| 숫자 부호 | `shouldBePositive/Negative/Zero` ↔ 각 `shouldNotBe...` | Byte, Short, Int, Long, Float, Double의 전체 18 overload |
| 근사 비교 | Float/Double의 기존 `shouldBeNear` ↔ `shouldNotBeNear`; `BigDecimal.shouldBeNear` ↔ `BigDecimal.shouldNotBeNear` | 같은 판정 predicate와 기본 허용 오차를 사용하고 NaN/무한대 경계를 고정 |

`Iterable.shouldContainAny`/`shouldContainNone`과 기존 `shouldNotContainAny` alias는 현재 의미를 유지한다. `shouldNotContainNone` 같은 불필요한 중복 alias는 만들지 않는다. `shouldThrow`/`shouldNotThrow`, equality/null/Boolean/identity, 비교/range, collection/array element containment, reflection은 현재 대칭 쌍을 유지한다. non-null expected에 대한 배열 전용 `shouldNotBeEqualTo`는 generic overload보다 우선하며, 배열 equality positive와 같은 content semantics를 적용한다. 따라서 별도 인스턴스라도 같은 배열 내용이면 negative assertion이 실패한다. expected가 nullable 타입으로 선언된 호출은 기존 generic overload를 선택하고 참조 equality를 유지한다. nullable expected까지 content semantics가 필요하면 `shouldNotContentEqual`을 사용한다. 기존 Float/Double 근사 비교는 현재 기본 허용 오차와 무한대 판정이 positive/negative 사이에서 달라 대칭이 아니므로 이번 범위에서 공통 판정으로 맞춘다.

#### 누락된 공개 overload 목록

다음은 전체 assertion 대칭 감사에서 새로 보완할 positive API의 실제 receiver/argument overload와 그 negative overload다. 기존에 짝이 있는 함수도 위 공개 조건 API 전체 목록에서 빠뜨리지 않고 유지 여부를 확인한다. 표에 없는 `BooleanArray.shouldContainSame` overload는 현재 없으므로 새로 만들지 않는다.

| Receiver | 현재 overload | 추가하거나 보완할 overload |
|---|---|---|
| `T : CharSequence` nullable | `shouldContainAll(vararg CharSequence)`, `shouldContainAll(Iterable<CharSequence>)` | 각 인자 형태의 `shouldNotContainAll` |
| `T : CharSequence` nullable | `shouldContainNone(vararg CharSequence)` | `shouldContainAny(vararg CharSequence)` |
| `T : CharSequence` nullable | `shouldMatch(Regex)`, `shouldMatch(String)`, `shouldNotMatch(Regex)` | `shouldNotMatch(String)` |
| `T : CharSequence` nullable | `shouldBeEqualToIgnoringCase(String)` | `shouldNotBeEqualToIgnoringCase(String)` |
| `Iterable<T>` nullable | `shouldContainAll(Iterable<T>)`, `shouldContainAll(vararg T)` | 각각 같은 인자 형태의 `shouldNotContainAll` |
| `Iterable<T>` nullable | `shouldMatchAllWith((T) -> Boolean)`, `shouldMatchAtLeastOneOf((T) -> Boolean)` | 각각 `shouldNotMatchAllWith`, `shouldNotMatchAtLeastOneOf` |
| `Iterable<T>` nullable | `shouldHaveSize(Int)`, `shouldContentEqual(Iterable<T>?)`, `shouldContainSame(Iterable<T>?)` | 각각 `shouldNotHaveSize`, `shouldNotContentEqual`, `shouldNotContainSame` |
| `Sequence<T>` nullable | `shouldContentEqual(Sequence<T>?)` | `shouldNotContentEqual(Sequence<T>?)` |
| `Map<K,V>` nullable | `shouldHaveSize(Int)`, `shouldContainSame(Map<K,V>?)` | 각각 `shouldNotHaveSize`, `shouldNotContainSame` |
| `Array<T>` nullable | `shouldHaveSize(Int)`, `shouldContentEqual(Array<T>?)`, `shouldContainSame(Array<T>?)` | 각각 동일 인자형의 부정 overload |
| `Array<out T>` nullable | `shouldBeEqualTo(expected: Array<out T>)` | non-null expected를 받는 `shouldNotBeEqualTo(expected: Array<out T>)`; receiver-null legacy 결과의 반대. Nullable expected 선언은 generic `shouldNotBeEqualTo(expected: T?)` fallback을 선택 |
| `IntArray`, `LongArray`, `DoubleArray`, `FloatArray`, `ByteArray`, `ShortArray`, `CharArray`, `BooleanArray` nullable | 각 `shouldHaveSize(Int)`, `shouldContentEqual(<same array>?)`, `shouldBeEqualTo(expected: <same array>)` | 각 positive 선언과 같은 인자형으로 `shouldNotHaveSize`, `shouldNotContentEqual`; non-null expected를 받는 배열 전용 `shouldNotBeEqualTo`. Nullable expected는 generic overload fallback |
| `IntArray`, `LongArray`, `DoubleArray`, `FloatArray`, `ByteArray`, `ShortArray`, `CharArray` nullable | 각 `shouldContainSame(<same array>?)` | 각 positive 선언과 같은 인자형의 `shouldNotContainSame` |
| `LongArray` nullable | `shouldContainSame(Iterable<Long>?)` | `shouldNotContainSame(Iterable<Long>?)` |
| `Date`, `LocalDate`, `LocalTime`, `LocalDateTime`, `Instant`, `ZonedDateTime`, `OffsetDateTime` | 각 receiver의 `shouldBeAfter/Before/OnOrAfter/OnOrBefore(same type)` | 각 receiver 및 함수에 같은 type argument를 받는 네 `shouldNotBe...` |
| `Byte`, `Short`, `Int`, `Long`, `Float`, `Double` | 각 `shouldBePositive`, `shouldBeNegative`, `shouldBeZero` | 같은 receiver의 각 `shouldNotBePositive`, `shouldNotBeNegative`, `shouldNotBeZero` |
| `BigDecimal` | `shouldBeNear(BigDecimal, tolerance: BigDecimal = BIGDECIMAL_EPSILON)` | `shouldNotBeNear` 같은 인자형·기본 tolerance |
| `Double`, `Float` | positive `shouldBeNear(expected, tolerance = DOUBLE_EPSILON/FLOAT_EPSILON)`; negative `shouldNotBeNear(expected, delta)`와 별도 단일 인자 overload | 기존 negative의 두 source/JVM overload와 인자명 `delta`를 보존한다. 단일 인자 구현의 tolerance 상수만 positive 기본값과 맞추며 두 함수가 같은 predicate를 사용 |

이 목록은 전체 공개 조건 assertion 대칭 감사와 누락된 overload를 구현 중 다시 확인할 기준이다. 기존에 짝이 확인된 family도 모두 포함한 최종 대조표를 작성하고, 여기에 표시한 누락 overload와 이후 명시한 새 finite/Duration assertion을 추가한다. Flow/Turbine의 별도 `assert*`, assertion context/예외 보조 DSL, `withMessage`는 조건 assertion이 아니므로 대상이 아니다.

일반 assertion pair는 유효한 receiver 입력 도메인에서 서로 정확한 논리 부정이다. nullable receiver의 동작은 아래 표의 overload별 계약을 따른다. 특히 `shouldNotMatch(Regex/String)`과 `shouldNotBeEqualToIgnoringCase`는 null receiver에서 성공하는 대칭 API로 보존한다. Float/Double의 일반 positive/negative sign assertion은 엄격한 비교 연산자와 그 논리 부정을 사용한다. 따라서 `shouldNotBePositive`, `shouldNotBeNegative`, `shouldNotBeZero`는 각각 `shouldBePositive`, `shouldBeNegative`, `shouldBeZero`의 정확한 부정이며 NaN은 세 negative assertion에서 통과한다. 유한수 sign pair는 finite 입력을 공통 domain으로 삼고 NaN/±∞는 모두 거부한다. 기존 positive API 동작과 반환/nullability 형태는 명시한 Map key-value, 배열 overload 및 Float/Double 동작 보완 외에는 유지한다. 배열 equality의 negative overload resolution은 expected의 정적 nullable 여부에 따른다. Non-null expected에는 배열 content overload를 선택하고 nullable expected에는 기존 generic reference-equality overload를 유지한다.

| Nullable assertion family | 기존 positive 동작과 새 negative 경계 |
|---|---|
| `Iterable.shouldHaveSize` | null은 빈 Iterable처럼 취급하므로 기대 크기 0만 통과한다. Negative는 이를 반전하고, null 성공 시 기존 empty receiver 반환 형태를 따른다. |
| generic `Array.shouldHaveSize` | null은 크기 0처럼 취급한다. Negative는 이를 반전하고 기존 empty array 반환 형태를 따른다. |
| `Map.shouldHaveSize`, primitive array `shouldHaveSize` | null receiver는 invalid로 거부하며 negative도 같은 null 거부 계약을 따른다. non-null receiver에서는 size 비교를 반전한다. |
| `shouldContentEqual` (Iterable, Sequence, Array) | 양쪽 null은 positive 성공, 한쪽만 null은 positive 실패다. Negative는 두 결과를 반전한다. Sequence와 nullable receiver의 반환 형태를 유지한다. |
| `shouldContainSame` (Iterable, Map, Array) | 양쪽 null은 positive 성공, 한쪽만 null은 positive 실패다. Negative는 두 결과를 반전한다. Map과 nullable receiver의 반환 형태를 유지한다. |
| `T : CharSequence` nullable `shouldContainAny` / `shouldContainNone` | positive any는 null receiver 또는 빈 candidate list에서 실패하고, none은 같은 경우 성공한다. 비어 있지 않은 candidate list에서 하나라도 발견되면 any가 성공하고 none은 실패한다. |
| `T : CharSequence` nullable `shouldContainAll` / `shouldNotContainAll` | null receiver는 invalid input으로 양쪽 모두 실패한다. non-null receiver에서는 expected가 비어 있으면 positive 성공/negative 실패이며, 나머지는 결과를 반전한다. |
| `T : CharSequence` nullable `shouldMatch(Regex/String)` / `shouldNotMatch(Regex/String)` | null receiver는 positive에서 실패하고 negative에서는 성공한다. String overload는 `Regex(pattern)` 생성 후 receiver를 검사하므로 receiver가 null이어도 pattern이 잘못되면 양쪽 모두 `java.util.regex.PatternSyntaxException`을 먼저 던진다. |
| `T : CharSequence` nullable `shouldBeEqualToIgnoringCase` / `shouldNotBeEqualToIgnoringCase` | null receiver는 positive에서 실패하고 negative에서는 성공한다. non-null receiver에서는 결과를 반전한다. |
| Iterable `shouldContainAll` / `shouldNotContainAll` | receiver가 null일 때 빈 expected는 positive 성공/negative 실패, 비어 있지 않은 expected는 positive 실패/negative 성공이다. non-null receiver에서도 결과를 정확히 반전한다. |
| generic `Array<out T>` 및 8개 primitive array의 `shouldBeEqualTo` | 현재 모든 nullable array receiver는 null이면 non-null expected와 무관하게 positive가 성공한다. 새 negative는 그 결과를 반전하여 각 overload의 null receiver에서 실패한다. 이 기존 동작은 이번 작업에서 정상화하지 않고, 각 overload KDoc과 `testing/assertions/README.md`, `README.ko.md`에 legacy 동작으로 명시하고 9개 overload 모두 회귀 테스트로 고정한다. 이 API는 null 검증 용도가 아니며 null 여부는 별도 assertion으로 확인하라고 경고한다. |
| Map key-value `shouldContain` / `shouldNotContain` | entry 존재와 값 일치를 함께 검사하도록 positive/negative pair를 보완한다. key가 없고 기대 값이 null인 경우에는 포함으로 간주하지 않는다. null Map은 기존 exact-complement 동작대로 positive에서 실패하고 negative에서 성공한다. |

`Iterable.shouldMatchAllWith` 및 `shouldMatchAtLeastOneOf`의 null/empty 경계를 고정하고, 부정형은 전체 경계에서 기존 positive 결과를 반전한다.

| Receiver | 함수 | null | 빈 Iterable | 일부/전체 원소 predicate true | predicate가 전부 false |
|---|---|---|---|---|---|
| `Iterable<T>?` | `shouldMatchAllWith` | 성공 (현재 null을 빈 입력으로 취급) | 성공 | 전부 true면 성공, 일부 false면 실패 | 실패 |
| `Iterable<T>?` | `shouldNotMatchAllWith` | 실패 | 실패 | 일부 false가 있으면 성공, 전부 true면 실패 | 성공 |
| `Iterable<T>?` | `shouldMatchAtLeastOneOf` | 실패 | 실패 | 하나 이상 true면 성공 | 실패 |
| `Iterable<T>?` | `shouldNotMatchAtLeastOneOf` | 성공 | 성공 | 하나 이상 true면 실패 | 성공 |

predicate를 방문 원소마다 최대 한 번 평가하고, 반복 가능한 receiver도 실패 메시지 생성을 위해 재순회하지 않는다.

Float/Double 각각에 `shouldBeFinite()`/`shouldNotBeFinite()`를 추가한다. finite-sign assertions는 `shouldBeFinitePositive`, `shouldBeFiniteZeroOrNegative`, `shouldBeFiniteZeroOrPositive`, `shouldBeFiniteNegative`로 하며, 이는 두 쌍의 네 함수다. core에서는 기존 Number helper 명명과 맞춰 `require/checkFinitePositiveNumber`, `require/checkFiniteZeroOrNegativeNumber`, `require/checkFiniteZeroOrPositiveNumber`, `require/checkFiniteNegativeNumber`를 사용한다. 양쪽 모두 finite-sign 카테고리를 유한수 입력 도메인으로 한정한다. 즉 두 양수/0-이상 sign pair는 유한수일 때 서로 반대이고, NaN/±∞는 두 쪽 모두 실패한다. 이는 `shouldNotXxx`/`requireNotXxx` 일반 부정 함수가 전체 입력 도메인의 exact complement여야 한다는 규칙과 별도다. 일반 Float/Double `shouldBePositive`, `shouldBeNegative`, `shouldBeZero`는 엄격한 `> 0`, `< 0`, `== 0`을 사용하고 새 `shouldNotBePositive`, `shouldNotBeNegative`, `shouldNotBeZero`는 각 조건의 논리적 부정을 사용한다. 따라서 NaN은 일반 positive/negative/zero에는 실패하고 대응하는 `shouldNotBe...`에는 성공한다. 양·음의 무한대는 일반 부호 assertion에는 해당 부호로 통과하지만 finite 조건에서는 거부한다.

| Float/Double 입력 | `shouldBePositive` | `shouldNotBePositive` | `shouldBeNegative` | `shouldNotBeNegative` | `shouldBeZero` | `shouldNotBeZero` |
|---|---:|---:|---:|---:|---:|---:|
| `NaN` | 실패 | 성공 | 실패 | 성공 | 실패 | 성공 |
| `+∞` | 성공 | 실패 | 실패 | 성공 | 실패 | 성공 |
| `-∞` | 실패 | 성공 | 성공 | 실패 | 실패 | 성공 |
| `+0.0` / `-0.0` | 실패 | 성공 | 실패 | 성공 | 성공 | 실패 |

Float/Double의 근사 비교는 positive와 negative가 같은 판정 함수 `near(value, expected, tolerance)`의 결과와 부정을 각각 사용한다. 판정은 NaN operand에서는 false이며, tolerance 검증은 양수·음수 모두 동일하게 음수와 모든 비유한 값(NaN/±∞)을 거부한다. 비유한 tolerance로 근사 검증을 무력화하지 않는다. 동일 부호 무한대도 차이가 NaN이 되므로 positive 근사에는 실패하고 negative 근사에는 성공한다. positive는 기존 `expected, tolerance = EPSILON` 선언과 `$default` bridge를 보존한다. negative는 기존 `expected, delta` 선언과 별도 infix 단일 인자 선언 및 두 JVM arity를 보존한다. 기존 단일 인자 overload 내부의 Double 기본 tolerance는 `DOUBLE_EPSILON`, Float는 `FLOAT_EPSILON`으로 맞춘다. 기존 negative 인자명 `delta`는 Kotlin named-argument source compatibility를 위해 유지한다. 새 `BigDecimal.shouldNotBeNear`는 positive처럼 `expected, tolerance = BIGDECIMAL_EPSILON` 단일 source 선언으로 제공하고 `$default` bridge를 검증한다.

Java `Duration`과 Kotlin `Duration`에 각각 `shouldBePositive`/`shouldNotBePositive` 및 `shouldBeZeroOrPositive`/`shouldBeNegative` pair를 제공한다. Core의 선택된 Duration 가족도 두 타입 각각에서 `requirePositiveDuration`/`requireZeroOrNegativeDuration` 및 `requireZeroOrPositiveDuration`/`requireNegativeDuration` pair와 각 `check` 대응 함수를 제공한다. Java Duration에는 무한대가 없고 Kotlin에서는 `Duration.INFINITE`와 `-Duration.INFINITE`가 각각 양·음 무한대다. 아래 truth table은 Assertions와 대응 Core `require`/`check` 판정에 공통으로 적용한다. `shouldNotBePositive`와 `ZeroOrNegativeDuration`는 positive 조건의 부정이며 `shouldBeNegative`와 `NegativeDuration`는 zero-or-positive 조건의 부정이다. 이 sign helper는 유한 timeout을 보장하지 않으므로 제한 시간이 유한해야 하는 호출자는 별도로 `isFinite()`를 확인한다. Assertions 함수는 `io.bluetape4k.assertions` package의 `DurationAssertions.kt`에 둔다.

| 입력 | Positive (shouldBePositive / requirePositiveDuration) | ZeroOrNegative (shouldNotBePositive / requireZeroOrNegativeDuration) | ZeroOrPositive (shouldBeZeroOrPositive / requireZeroOrPositiveDuration) | Negative (shouldBeNegative / requireNegativeDuration) |
|---|---:|---:|---:|---:|
| Java/Kotlin 유한 음수 | 실패 | 성공 | 실패 | 성공 |
| Kotlin `-Duration.INFINITE` | 실패 | 성공 | 실패 | 성공 |
| Java/Kotlin `Duration.ZERO` | 실패 | 성공 | 성공 | 실패 |
| Java/Kotlin 유한 양수 | 성공 | 실패 | 성공 | 실패 |
| Kotlin `Duration.INFINITE` | 성공 | 실패 | 성공 | 실패 |

표에 적힌 require 이름의 check 대응 함수도 같은 결과를 낸다. shouldNotBePositive와 requireZeroOrNegativeDuration은 positive 조건의 부정이며, 각각의 check 대응 함수도 같은 보완 관계를 이룬다.

### core에서 제공할 공통 쌍

core의 각 선택 가족은 `require` 및 `check` 변형을 모두 제공한다. 배열·컬렉션 내용 비교 등 테스트 assertion 목적의 API는 core에 복제하지 않는다.

| 검증 가족 | 선택 근거와 추가할 쌍 또는 보완 |
|---|---|
| equality | `requireEquals`의 사용 23회. 빠진 `requireNotEquals` 추가. `checkEquals`/`checkNotEquals`는 이미 존재 |
| 비어 있지 않음 | `requireNotEmpty` 사용 73회. CharSequence의 기존 `requireNullOrEmpty`에 맞춰 Array, Collection, Map에도 `requireNullOrEmpty`/`checkNullOrEmpty` 추가; 각 `NotEmpty`의 정확한 부정과 짝을 이룸 |
| 문자 시퀀스 | `requireContains` 11회, `requireStartsWith` 8회, `requireEndsWith` 6회. `require/check` 각각에 `NotContains`, `NotStartsWith`, `NotEndsWith` 추가; `requireEndsWith`의 lazy-message overload도 `checkEndsWith`와 대칭을 이루도록 보완 |
| Map | `requireHasKey`와 `requireHasValue` 각 6회. `require/check`의 `NotHasKey`, `NotHasValue`, key-value `NotContains` 추가. 기존 `requireContains`/`checkContains`도 `containsKey(key)`와 값 일치를 함께 검사하도록 바로잡아 missing key + null value가 positive 조건을 통과하지 않게 한다. nullable receiver의 null은 positive/negative 모두 거부하고, non-null empty Map은 부정 조건을 만족 |
| 범위와 크기 | `requireInRange` 148회, `requireSizeInRange` 7회, `requireLengthInRange` 6회. `require/checkNotInRange`, `require/checkNotInOpenRange`, `require/checkNotLengthInRange`, `require/checkNotSizeInRange` 추가 |
| 유한수 | Float/Double의 기존 `requireFinite`/`checkFinite` 사용 9회. `requireNotFinite`/`checkNotFinite` 추가 |
| Float/Double 부호 | 일반 Number helper에는 finite 조건을 넣지 않는다. `requireFinitePositiveNumber` ↔ `requireFiniteZeroOrNegativeNumber`, `requireFiniteZeroOrPositiveNumber` ↔ `requireFiniteNegativeNumber` 두 쌍의 네 함수를 각각 `check` 대응형과 함께 제공한다. 네 family 모두에 `Float`와 `Double` primitive overload를 둔다. |
| Duration | 사용자 요구. Java/Kotlin 각각 `requirePositiveDuration` ↔ `requireZeroOrNegativeDuration`, `requireZeroOrPositiveDuration` ↔ `requireNegativeDuration`를 `require`/`check`에 제공. Java는 `DurationSupport.kt`의 `io.bluetape4k.javatimes`, Kotlin은 `KotlinDurationSupport.kt`의 `io.bluetape4k.support` |

`requireNotBlank`/`requireNullOrBlank`, 기존 CharSequence `requireNotEmpty`/`requireNullOrEmpty`, null 검사, Boolean, 비교 및 Number 부호 helper는 이미 require/check 양쪽에서 의미상 짝을 이룬다. Array/Collection/Map에도 nullable receiver를 받는 `NullOrEmpty`를 제공한다. `NotEmpty`는 null과 빈 값을 모두 거부하고, `NullOrEmpty`는 null 또는 빈 값만 허용하므로 nullable 입력 전체에서 서로 보완한다. 새 타입 overload에는 성공 시 receiver가 null임을 암시하는 contract를 추가하지 않는다. 기존 `requireNullOrEmpty`와 `requireNullOrBlank`의 잘못된 null-only contract도 제거한다. 두 함수는 non-null empty/blank 값도 성공할 수 있으므로 해당 contract가 사실이 아니다. 반환값은 nullable을 유지하고, 호출자가 non-null을 필요로 하면 반환값을 별도로 검증한다. 그 밖의 새 부정형은 core validator의 기존 null 계약을 따른다. 특히 `Contains`, `StartsWith`, `EndsWith`, Map 검증은 nullable receiver의 null을 긍정·부정 helper 모두 거부한다. Map의 부정형은 non-null empty Map에서 통과한다. key-value `requireContains`/`checkContains`는 `containsKey(key)`와 값 일치를 함께 검사하며, 대응하는 `requireNotContains`/`checkNotContains`는 그 조건을 정확히 부정한다. 이로써 missing key + null value는 positive에서 실패하고 negative에서 성공하며, 실제로 존재하는 null-valued key는 positive에서 성공하고 negative에서 실패한다. 모든 helper는 성공 시 원 receiver를 반환한다.

범위 함수의 이름·경계는 다음과 같다. `require/checkInRange(start, endInclusive)`는 `[start, endInclusive]`이며 부정형 `NotInRange`는 그 구간 밖이다. 기존 이름 `InOpenRange(start, endExclusive)`는 Kotlin의 half-open range `[start, endExclusive)`를 뜻한다. 이름은 호환성을 위해 유지하고, 부정형 `NotInOpenRange`는 그 구간 밖이다. 두 부정형은 양 끝 경계에서 정확한 complement를 이룬다. 역전된 closed range(`start > endInclusive`)와 빈 half-open range(`start >= endExclusive`)는 기존 비교 의미대로 빈 범위다. 별도 경계 검증 예외를 추가하지 않아 기존 positive는 usual `IllegalArgumentException`으로 실패하고 negative는 성공한다. `NotLengthInRange`와 `NotSizeInRange`도 기존 inclusive `[start, endInclusive]`를 부정하며, nullable receiver는 positive와 동일하게 null을 거부한다.

| API 가족 | 소유 package / 파일 | 공개 surface |
|---|---|---|
| Java Duration core validators | `io.bluetape4k.javatimes` / `DurationSupport.kt` | Java `Duration`의 require/check quartet |
| Kotlin Duration core validators | `io.bluetape4k.support` / `KotlinDurationSupport.kt` | Kotlin `Duration`의 require/check quartet |
| Duration assertions | `io.bluetape4k.assertions` / 새 `DurationAssertions.kt` | Java 및 Kotlin `Duration` extension |
| Float/Double finite assertions | `io.bluetape4k.assertions` / `Numerical.kt` | Float/Double 각각 `shouldBeFinite` ↔ `shouldNotBeFinite` |
| Float/Double finite-sign assertions | `io.bluetape4k.assertions` / `Numerical.kt` | `shouldBeFinitePositive` ↔ `shouldBeFiniteZeroOrNegative`; `shouldBeFiniteZeroOrPositive` ↔ `shouldBeFiniteNegative` |
| Float/Double finite-sign core validators | `io.bluetape4k.support` / `RequireSupport.kt`, `CheckSupport.kt` | `requireFinitePositiveNumber` ↔ `requireFiniteZeroOrNegativeNumber`; `requireFiniteZeroOrPositiveNumber` ↔ `requireFiniteNegativeNumber`, 그리고 각 이름의 `check` 대응형. 각 family는 Float/Double primitive overload를 제공 |

Java Duration와 Kotlin Duration core extension은 같은 이름을 사용하지만 package가 다르다. 둘을 한 소스 파일에서 함께 쓰는 예제는 Kotlin import alias를 적용한다. Kotlin `Duration` helper는 Kotlin extension API로 제공하며 Java facade는 추가하지 않는다.

### ABI 검증 절차

구현 후 정확한 로컬 checkout에서 아래 compile을 실행한 다음

```bash
./gradlew :bluetape4k-core:compileKotlin :bluetape4k-assertions:compileKotlin :bluetape4k-core:compileTestKotlin
javap -classpath bluetape4k/core/build/classes/kotlin/main -p -s \
  io.bluetape4k.javatimes.DurationSupportKt \
  io.bluetape4k.support.KotlinDurationSupportKt \
  io.bluetape4k.support.RequireSupportKt \
  io.bluetape4k.support.CheckSupportKt
javap -classpath testing/assertions/build/classes/kotlin/main -p -s \
  io.bluetape4k.assertions.DurationAssertionsKt \
  io.bluetape4k.assertions.BasicKt \
  io.bluetape4k.assertions.ArraysKt \
  io.bluetape4k.assertions.CharSequencesKt \
  io.bluetape4k.assertions.CollectionsKt \
  io.bluetape4k.assertions.DateTimesInstantZonedKt \
  io.bluetape4k.assertions.DateTimesLegacyKt \
  io.bluetape4k.assertions.DateTimesLocalKt \
  io.bluetape4k.assertions.MapsKt \
  io.bluetape4k.assertions.NumericalKt
  io.bluetape4k.assertions.ReflectionKt
```

기대 결과는 다음과 같다. Java Duration 함수의 extension receiver와 반환 descriptor는 `Ljava/time/Duration;`이고 Kotlin Duration 함수의 receiver/반환은 underlying `J`이며 method name에는 Kotlin/JVM value-class mangling이 적용된다. Float/Double API는 primitive `F`/`D` descriptor를 유지한다. 각 core validator descriptor는 아래 오버로드 계약에 적힌 parameter-name와 lazy-message call shape를 따른다. 이 인자들이 별도 overload면 descriptor도 별도로 남고, 한 선언에서 기본값을 쓰면 기본 인자는 JVM descriptor에 포함되며 필요 시 `$default` synthetic method가 생긴다. Duration/finite-sign assertion은 receiver-only, near는 matrix에 적힌 expected/tolerance 인자를 가지며 나머지 assertions는 각 overload matrix의 실제 인자형을 따른다. owner는 파일 대응 표의 `*Kt` class다. 모든 신규 source declaration은 matrix에 지정된 함수명, owner, 인자·반환 descriptor로 존재해야 하며 기존 signature와 충돌하지 않아야 한다. API overload 수는 Kotlin source declaration 기준으로 비교하고 `$default`, `$annotations`, bridge 같은 compiler synthetic method는 별도 표기해 source overload 수에 합산하지 않는다. 기본 인자가 허용하는 축약 호출 arity와 source overload 선언 수는 구분한다.

| API | 예상 JVM owner | signature 확인 조건 |
|---|---|---|
| Java Duration core `require/check` quartet | `io.bluetape4k.javatimes.DurationSupportKt` | receiver와 반환 `Ljava/time/Duration;`; `String`, `Function0` parameters |
| Kotlin Duration core `require/check` quartet | `io.bluetape4k.support.KotlinDurationSupportKt` | mangled method name; receiver와 반환 `J`; `String`, `Function0` parameters |
| 선택 core validators | `io.bluetape4k.support.RequireSupportKt`, `CheckSupportKt` | positive/negative 각 overload가 기존 대응 함수의 동일 인자 descriptor와 고유 JVM signature를 가짐 |
| Float/Double finite-sign core validators | `io.bluetape4k.support.RequireSupportKt`, `CheckSupportKt` | `requireFinitePositiveNumber`, `requireFiniteZeroOrNegativeNumber`, `requireFiniteZeroOrPositiveNumber`, `requireFiniteNegativeNumber`와 각각의 `check` 대응 함수 모두에 별도 `Float -> Float` 및 `Double -> Double` overload를 둔다. 각 overload는 `(parameterName: String, lazyMessage: Function0?)` signature이며 generic `Number` overload는 없다. |
| Java/Kotlin Duration assertions | `io.bluetape4k.assertions.DurationAssertionsKt` | `shouldBePositive`, `shouldNotBePositive`, `shouldBeZeroOrPositive`, `shouldBeNegative`는 receiver-only; Java receiver/return은 `Ljava/time/Duration;`, Kotlin receiver/return은 `J`와 mangled method name |
| Float/Double finite assertions | `io.bluetape4k.assertions.NumericalKt` | Float/Double 각각 `shouldBeFinite`와 `shouldNotBeFinite`의 receiver-only `F`→`F`, `D`→`D` descriptor |
| Float/Double finite-sign assertions | `io.bluetape4k.assertions.NumericalKt` | 두 쌍의 네 함수는 각각 receiver-only이며 `F`→`F`, `D`→`D` |
| Float/Double near assertions | `io.bluetape4k.assertions.NumericalKt` | positive `(receiver, expected, tolerance)`와 `$default`; negative `(receiver, expected, delta)` 및 별도 `(receiver, expected)` 선언/JVM descriptor 보존 |
| BigDecimal near assertions | `io.bluetape4k.assertions.NumericalKt` | positive/negative 모두 `(receiver, expected, tolerance)`와 각 `$default` synthetic method |
| 기존 assertion condition overload 전체 | `BasicKt`, `ArraysKt`, `CharSequencesKt`, `CollectionsKt`, `JavaDateTimesInstantZonedKt`, `JavaDateTimesLegacyKt`, `JavaDateTimesLocalKt`, `MapsKt`, `NumericalKt`, `JavaDurationsKt` | overload matrix의 모든 신규 함수명·receiver/인자·반환 타입이 해당 facade owner에 나타남; primitive array descriptor와 Java time descriptor 확인 |

Assertions compile fixture는 testing/assertions/src/test/kotlin/io/bluetape4k/assertions/api/AssertionsApiCompileFixture.kt에 둔다. Assertions module API만 import/call하고, core validation 호출은 포함하지 않는다. Core compile fixture는 bluetape4k/core/src/test/kotlin/io/bluetape4k/api/CoreValidationApiCompileFixture.kt에 두고 core API만 호출한다. 각각 :bluetape4k-assertions:compileTestKotlin과 :bluetape4k-core:compileTestKotlin으로 따로 확인한다. Assertions fixture에는 finite와 near API, 문자열·배열·collection·Map·시간 비교 pair, nullable/non-null 배열 expected overload 선택, nullable 반환형을 포함한다. Core fixture에는 선택된 require/check helper의 positive/negative overload, alias-import한 Java/Kotlin Duration quartet, Float/Double finite-sign overload와 parameter-name/lazy-message 호출 형태를 포함한다. 두 fixture는 compile 전용이며 함수 본문은 실행하지 않는다. 모든 변경 source declaration은 API matrix 및 javap facade 목록으로 대조하고, ReflectionKt도 포함한다. Negative near named argument delta와 README의 대표 호출 형태가 각각 compile되는지 확인한다.

PASS 조건은 production compile과 두 module-owned fixture compile 성공, 각 owner의 예상 overload와 descriptor 존재, 문서 예제의 대표 source resolution 성공, delta named argument compile 성공, 중복/충돌 없음이다. 현재 작업의 evidence는 검증한 local implementation HEAD에 연결한다. 향후 PR이 별도로 승인되면 exact PR-head 검증을 별도 gate로 추가한다. Java Duration ABI는 unmangled descriptor로 하고 Kotlin Duration은 value-class mangling을 확인한다.

기존 `requirePositiveNumber`, `requireZeroOrPositiveNumber`, `checkPositiveNumber`, `checkZeroOrPositiveNumber`의 일반 동작은 바꾸지 않는다. 이 helper들은 부호만 검사하므로 Float/Double의 양의 무한대는 통과할 수 있다. 유한성이 필요한 호출자는 finite 전용 helper를 사용해야 한다. finite-sign helper는 generic Number 하나로 뭉치지 않고 `Float`/`Double` 각각의 primitive receiver와 동일한 반환형으로 선언한다. core에서 assertions의 모든 배열·컬렉션·시간·predicate API를 1:1 복제하지 않는다.

### 오버로드 계약

새 core helper는 선택된 사용 빈도 높은 검증에만 추가하고, 각 가족의 positive/negative `require`/`check` pair를 모두 제공한다. 대응하는 `require`/`check`의 호출 signature 및 반환형은 동일하게 유지한다. 모든 함수에 인위적으로 두 overload를 만들지 않는다. 기존 positive가 두 overload를 가지면 두 형태를 보완한다. 구체적으로 `InRange`/`InOpenRange`는 `(start, end, parameterName: String)`과 `(start, end, lazyMessage: () -> Any)` 형태를 각각 추가한다. `LengthInRange` 및 `SizeInRange`의 CharSequence/Collection/Map/Array overload는 `(start, endInclusive, parameterName: String, noinline lazyMessage: (() -> Any)? = null)` 단일 선언형을 그대로 따른다. 새 `NotLengthInRange`/`NotSizeInRange`에도 같은 형태를 사용한다. `Finite`와 새 finite-sign 함수는 `(parameterName: String, noinline lazyMessage: (() -> Any)? = null)`를 사용한다. 일반 Number positive helper처럼 별도 message overload를 가진 family는 각 기존 signature를 그대로 짝지어 추가한다. `requireNotFinite`/`checkNotFinite`는 기존 `requireFinite`/`checkFinite`의 signature를 따른다. 새 Duration quartet는 각 package에서 `(parameterName: String, noinline lazyMessage: (() -> Any)? = null)` 한 선언을 사용한다. `EndsWith` 계열은 기존 `ignoreCase` 기본값과 parameter-name/lazy-message 형태를 유지하며, `requireEndsWith`에 누락된 lazy-message overload를 보완한다. `NullOrEmpty`는 `NotEmpty`와 같은 입력 타입별 parameter-name 및 lazy-message call shape를 제공하고 nullable receiver/반환형을 유지한다.

### 오류와 반환 계약

- Predicate 기반 assertion은 predicate를 한 번 평가하고, 부정형은 논리 부정을 직접 적용한다. 긍정형을 실행한 뒤 실패 예외를 catch해 부정형 성공으로 바꾸지 않는다. 사용자 predicate 예외는 타입과 동일 객체 identity를 보존해 전파한다.
- Assertion failure 진단은 호출자가 전달한 값과 formatter를 포함할 수 있고 사용자 toString()을 호출할 수 있다. 테스트 assertion 문맥에서 사용하며 민감 정보 제거, 출력 이스케이프, 출력 크기·비용 제한 또는 순환 참조 감지를 보장하지 않는다. 사용자 toString() 예외는 삼키거나 변환하지 않고 그대로 전파한다.

- Assertion이 실패하면 기존 `Failures`를 통해 기존 AssertionFailedError 계열 오류를 발생시킨다. Negative assertion도 입력과 기대 조건을 설명하는 메시지를 포함한다.
- `require` 부정형은 기존 helper와 동일하게 `IllegalArgumentException`을, `check` 부정형은 `IllegalStateException`을 사용한다.
- 새 assertion overload는 계약상 receiver를 반환하고, 반환 타입은 해당 overload가 성공할 수 있는 null 입력을 안전하게 표현해야 한다. Null receiver를 양쪽에서 거부하는 pair는 positive의 non-null 반환 형태를 따른다. Null receiver에서 negative가 성공하는 pair는 nullable receiver 반환형을 사용하고 실제 null을 반환한다. 크기처럼 기존 family가 null을 빈 값으로 정규화해 non-null receiver를 반환하는 경우에는 그 반환 계약을 유지한다. 특히 새 `shouldNotMatch(String)`과 `shouldNotBeEqualToIgnoringCase`는 `T?`를 반환한다. 모든 신규 overload를 compile fixture에서 선언된 반환 타입 변수에 대입해 확인하며, `!!`나 unsafe cast로 null 반환을 숨기지 않는다. Core에서는 `NotEmpty`/`NullOrEmpty`만 nullable 전체 영역에서 상호보완한다. 나머지 새 nullable core 검증 쌍은 기존 필수 입력 계약을 따라 null을 양쪽 모두 거부한다. Core helper의 lazy-message callback은 조건 성공 시 실행하지 않는다. 조건 자체가 실패해 message를 만드는 경로에서는 한 번 실행한다. Null 및 parameter-name 사전 검증에서 기존 positive가 callback 전에 실패하는 가족은 예외 종류와 callback 실행 횟수도 그대로 보존한다.
- finite-sign 쌍은 유한 domain 안에서만 서로 반대이며 NaN/무한대는 양쪽 모두 실패한다. `-0.0f`와 `-0.0`은 일반 부호 assertion에서 zero로 취급한다. primitive array equality/content equality의 기존 signed-zero와 NaN 비교 의미는 그대로 보존한다. `shouldNotBeFinite`/`requireNotFinite`/`checkNotFinite`는 NaN/±∞에서 성공하고 ±0에서 실패한다. 일반 Number sign helper는 finite 검증이 아니며 Float/Double의 양의 무한대를 허용할 수 있다.
- 단일 순회 보장은 `shouldContentEqual`, `shouldContainSame`의 Iterable/array overload, `shouldHaveSize`, predicate pair에만 적용한다. 서로 다른 Sequence의 `shouldContentEqual`은 각 입력을 한 번만 저장한 원소 목록으로 바꾸고 identity fast path와 기존 반환 receiver를 유지한다. `shouldMatchAllWith`/`shouldMatchAtLeastOneOf`와 부정형은 방문 원소마다 predicate를 최대 한 번 호출한다. 실패 메시지는 이미 방문한 원소나 저장한 원소 목록을 사용해 Collection receiver도 다시 순회하지 않는다. `shouldContainAll`/`shouldContainAny`/`shouldContainNone`의 일반 Iterable 반복 횟수에는 one-pass 보장을 추가하지 않으며, Map `shouldContainSame`도 이 성능 계약 범위 밖이다.
- `shouldHaveSize`는 `Collection.size`를 직접 읽고 iterator를 열지 않는다. 표준 Collection 구현에서는 O(1)이며, 사용자 정의 Collection의 `size` 복잡도는 해당 구현 계약을 따른다. 그 밖의 일반 `Iterable`은 기존 positive 동작과 같은 전체 크기 계산을 위해 정확히 한 번 끝까지 순회한다. negative도 같은 횟수만큼 순회해 positive와 부정형의 관측 가능한 소비 동작을 맞춘다. 배열 overload는 `size`를 직접 사용한다.
- 긍정·부정 `shouldContainSame`의 Iterable/array overload는 각 입력을 최대 한 번 순회한다. Iterable의 원소 빈도 계산과 실패 메시지용 목록은 같은 순회에서 만들어 재사용한다. 배열 overload는 불필요한 중간 List 변환을 피한다. HashMap 기반 원소 빈도 비교의 기대 시간은 일반적인 hash 분포에서 O(n), 추가 공간은 O(n)이며 hash 충돌이 많은 최악의 시간은 원소의 `hashCode`/`equals` 비용에 따른다. Map overload에는 이 복잡도/순회 보장을 부여하지 않는다.
- `FloatArray`/`DoubleArray.shouldContainSame`은 기존 boxed equality를 유지한다. 따라서 두 NaN 원소는 같은 값으로 비교하고 `-0.0`/`-0.0f`와 양의 zero는 서로 다른 값으로 비교한다. 배열 구현을 바꿔도 이 경계 동작은 변하지 않는다.

### 공개 API 문서와 사용 예시

모든 신규 공개 extension overload에는 한국어 KDoc을 둔다. 각 API 가족의 문서에는 조건, receiver/인자 제약, 반환형, nullable 입력과 실패 예외를 설명한다. lazy message를 받는 core helper는 callback 평가 규칙도 설명한다. 예시는 대표 overload마다 실제 import와 compile 가능한 Kotlin 호출을 포함한다.

| 예시 영역 | 반드시 설명할 경계 |
|---|---|
| Float/Double finite | finite assertion의 `shouldBeFinite` ↔ `shouldNotBeFinite`와 finite-sign 두 쌍을 각각 Float/Double에 제공함; 일반 `requirePositiveNumber`는 무한대를 허용할 수 있고 finite helper는 NaN/±∞를 거부함 |
| Java/Kotlin Duration | 각 package의 import, alias로 두 extension을 함께 사용하는 방법, positive/zero-or-positive 및 `shouldNotBePositive`/`requireZeroOrNegativeDuration`, `shouldBeNegative`/`requireNegativeDuration`의 반대 부호 사용법과 대응 `check` helper |
| Kotlin Duration timeout | `Duration.INFINITE`가 positive 부호 검사에 통과함과 `isFinite()` guard를 함께 쓰는 예 |
| nullable `NullOrEmpty` | null과 non-null empty 모두 성공하고 nullable 원 receiver를 반환함; null-only contract를 보장하지 않음 |
| `NotInRange`/`NotInOpenRange` | `[start, endInclusive]` 및 `[start, endExclusive)`의 시작·끝 경계 예 |
| generic `Array<out T>` 및 8개 primitive array equality | null receiver와 non-null expected의 legacy 결과, non-null expected에서 배열 overload가 선택되는 content semantics, nullable expected에서 generic 참조 equality fallback이 선택되는 규칙. Nullable expected content 비교에는 `shouldNotContentEqual`을 사용함 |

두 Java/Kotlin Duration extension을 함께 사용하는 Kotlin 호출 예시는 import 충돌을 alias로 해결하고, 제한 시간 검증 전에 무한대를 거부한다.

```kotlin
import io.bluetape4k.javatimes.requirePositiveDuration as requireJavaPositiveDuration
import io.bluetape4k.support.requirePositiveDuration as requireKotlinPositiveDuration
import io.bluetape4k.support.requireZeroOrPositiveDuration as requireKotlinZeroOrPositiveDuration
import kotlin.time.Duration

fun validateTimeout(javaTimeout: java.time.Duration, kotlinTimeout: Duration) {
    javaTimeout.requireJavaPositiveDuration("javaTimeout")
    require(kotlinTimeout.isFinite()) { "timeout must be finite" }
    kotlinTimeout.requireKotlinPositiveDuration("kotlinTimeout")
}

fun validateZeroOrPositiveDuration() {
    Duration.ZERO.requireKotlinZeroOrPositiveDuration("timeout")
    // Duration.ZERO.requireKotlinPositiveDuration("timeout") would fail: zero is not positive.
}

fun validateSignOnlyDuration() {
    // Sign-only validation accepts INFINITE. Bounded timeouts must check isFinite() first.
    Duration.INFINITE.requireKotlinPositiveDuration("duration")
}
```

`testing/assertions/README.md`와 `testing/assertions/README.ko.md`에는 새 assertion API 그룹, Float/Double의 finite assertion 및 finite-sign 두 쌍, Duration positive/negative 사용 예시와 generic/primitive array equality 9개 overload의 nullable legacy 동작을 각각 문서화한다. non-null expected의 배열 negative overload가 content 비교를 하고 nullable expected 선언은 generic 참조 비교로 남는다는 점, nullable expected content 비교에는 `shouldNotContentEqual`을 사용한다는 예시, 배열 equality가 null validation API가 아니므로 null 확인에는 별도 assertion을 사용하라는 경고를 포함한다. `bluetape4k/core/README.md`와 `bluetape4k/core/README.ko.md`에는 선택된 새 `require/check` 검증 가족만 기록하고, Float/Double의 finite 및 finite-sign 양·음 assertions와 두 Duration 타입의 require/check positive·negative 예시를 각각 추가한다. 저장소 `CHANGELOG.md`에는 새 API와 Float/Double NaN 부호 assertion, Float/Double 근사 비교 기본 허용 오차와 비유한 tolerance 거부, 배열 negative equality의 content semantics 및 nullable expected generic fallback, Map missing-key + null-value 판정의 호환성 변경을 한국어로 기록한다.

## 실패 가능성과 대응

1. **NaN이 양수·음수 assertion을 통과함** — Float/Double의 기존 부호 assertion을 엄격한 `> 0` 및 `< 0` 조건으로 고정하고 NaN 경계를 테스트한다. finite-sign helper는 유한 여부와 부호를 모두 검사한다.
2. **nullable 긍정·부정 함수가 서로 반대 결과를 내지 않음** — null receiver를 포함한 입력을 모든 새 쌍에서 테스트한다.
3. **Sequence 또는 일회성 Iterable이 두 번 소비됨** — 단일 순회 보장이 있는 API를 명시적으로 한정한다. positive/negative `shouldContainSame` Iterable은 빈도 계산과 오류 메시지에서 같은 순회의 원소 목록을 재사용하고, `LongArray × Iterable<Long>`도 expected Iterable을 한 번만 소비한다. Iterable `shouldContentEqual`과 predicate 실패 메시지도 counting/single-use 입력으로 검증한다. containment 함수와 Map same 비교에는 one-pass promise를 추가하지 않는다.
4. **크기 검사와 배열 비교가 불필요하게 순회·할당함** — `Collection.size`는 iterator 없이 사용하고, 일반 `Iterable.shouldHaveSize` positive/negative는 각각 기존 positive와 같은 전체 입력을 한 번 순회한다. 배열 `shouldContainSame`은 중간 List 변환 없이 빈도 비교를 수행하되 Float/Double boxed equality 동작을 보존한다.
5. **Kotlin Duration 무한대를 유한수와 혼동함** — Duration 부호 검증은 finite 조건을 암묵적으로 적용하지 않는다. `INFINITE`는 양수, `-Duration.INFINITE`는 음수로 판정한다. 제한 시간이 유한해야 하는 호출자는 이 부호 helper만으로 유한 상한을 검증하지 말고 별도 `isFinite()` 조건을 적용한다.
6. **오버로드 하나가 누락되거나 배열 참조 equality를 사용함** — 함수별 인자 타입 표와 전체 모듈 컴파일을 사용하고 모든 array primitive의 동등/비동등 케이스를 검증한다.
7. **실패 진단이 민감한 값을 출력하거나 사용자 formatter가 실패함** — 진단은 호출자가 전달한 값에 좌우되는 테스트 출력이며 보안 경계로 취급하지 않는다. 사용자 formatter 예외를 변환하지 않는지 검증한다.

## 호환성 및 범위 밖

- 새 API는 기존 패키지와 지정된 기존 또는 신규 `shouldXxxx`, `requireXxxx`, `checkXxxx` 파일에 extension으로 추가한다. Duration assertions는 위 표에 정한 신규 `DurationAssertions.kt`를 사용한다. 새 모듈이나 의존성은 없다.
- 기존 범용 Number helper 동작은 그대로 둔다. Float/Double sign assertion에서 NaN이 통과하는 문제, positive/negative near 기본 tolerance 불일치와 같은 부호 무한대가 `shouldBeNear`와 `shouldNotBeNear` 양쪽에서 통과하는 비대칭, Map key-value assertion이 없는 key의 null value를 존재하는 entry로 오인하는 문제는 바로잡는다. Map 동작 변경은 assertions의 `shouldContain`뿐 아니라 core의 기존 `requireContains`/`checkContains`에도 적용하며, 새 `shouldNotContain`/`requireNotContains`/`checkNotContains`는 수정된 positive 조건의 정확한 부정으로 동작한다. 이 호환성 변경을 `CHANGELOG.md`에 모듈과 함수명을 밝혀 기록하고, missing key + null value 및 existing null-valued key를 양·음 helper 각각에서 회귀 테스트한다. non-null expected의 배열 전용 `shouldNotBeEqualTo`는 generic 참조 equality 대신 배열 content semantics를 사용한다. nullable expected는 기존 generic reference-equality fallback을 유지하고, nullable content 비교에는 `shouldNotContentEqual`을 쓴다. 두 overload 경계와 동작 변경은 `CHANGELOG.md`, 관련 KDoc/README, compile fixture와 회귀 테스트에 기록한다.
- 기존 `requireNullOrEmpty`/`requireNullOrBlank`의 잘못된 null-only Kotlin contract를 제거한다. runtime 판정과 반환형은 유지하되, compile-time smart cast가 달라질 수 있으므로 CHANGELOG와 함수 KDoc에 기록하고 non-null empty/blank 입력의 compile/source regression을 검증한다.
- PR 경로 CI는 testing/assertions/** 변경에서 assertions test가 자동 실행되지 않을 수 있다. 이 실행은 PR 권한이 없으므로 local module tests를 직접 실행한다. 향후 PR이 별도로 승인되면 exact PR head에서 assertions/core tests를 실행하고 PR CI green이나 다른 head의 Nightly를 대체 근거로 삼지 않는다.
- 출시 전 복구는 변경 커밋 revert로 한다. 신규 API에 의존해 컴파일된 소비자는 이전 artifact로 단순 downgrade할 수 없으므로, 출시 후 문제는 patch-forward를 기본 복구 경로로 삼는다.
- `shouldXxxx`가 아닌 예외 메시지 체이닝(`withMessage` 등), Flow/Turbine의 별도 `assert*` DSL, `TestAssertionSupport.shouldFail*` 같은 assertion 시험 보조 함수, core에서 제공할 필요가 낮은 assertion 전용 비교 API는 범위에 넣지 않는다.
- PR, push, merge, release는 이번 실행 권한에 포함되지 않는다. 향후 별도 승인을 받아 진행한다.

## 승인 기준과 DoD

1. `bluetape4k-assertions` 공개 조건 assertion 전체의 긍정·부정 의미상 쌍을 전수 대조하고, 기존 쌍 및 누락 overload를 포함한 receiver/인자 matrix를 정리한다. 완성한 matrix는 `docs/superpowers/reviews/2026-10-08-assertion-symmetry-duration-finite-api-matrix.md`에 저장한다. 이 문서에는 assertions 전체의 overload별 쌍과 core에서 선택한 helper만의 `require`/`check` 긍정·부정 쌍을 별도 표로 기록한다.
2. 모든 공개 `shouldXxxx` 조건 함수와 `should(message, predicate)`에서 receiver/인자 오버로드마다 긍정·부정 짝이 있으며 null, 배열, 문자열, temporal, 숫자 입력에서 경계 동작을 검증한다. predicate 예외는 catch 없이 같은 객체로 전파된다.
3. nullable assertion의 overload별 동작 표를 테스트로 고정한다. `shouldHaveSize`, `shouldContentEqual`, `shouldContainSame`, 배열 equality의 null/empty/non-empty 경계를 포함하며, generic `Array<out T>`와 8개 primitive array의 9개 `shouldBeEqualTo` overload 모두 null receiver에서 non-null expected가 성공하는 legacy 결과를 positive/negative로 검증한다. 배열 negative overload는 non-null expected에서 별도 인스턴스의 같은 content면 실패하고 다른 content면 성공해야 한다. Compile fixture에서 non-null expected는 배열 content overload, nullable expected 변수는 기존 generic reference-equality fallback을 선택하는지 확인한다. 런타임 테스트는 서로 다른 내용 같은 배열 인스턴스, 같은 참조, null receiver와 nullable/non-null expected를 다루고 nullable expected의 `shouldBeEqualTo`/`shouldNotBeEqualTo`가 참조 equality를 유지하는지 검증한다. Nullable expected의 content 비교에는 `shouldContentEqual`/`shouldNotContentEqual`을 사용한다. 배열 equality를 null 검증에 사용하지 말라는 KDoc/README 경고가 있고 별도 null assertion 동작과 구별되는지도 확인한다. Map pair는 missing key + null value와 existing null value를 구분한다. null receiver에서 malformed String pattern을 준 `shouldMatch(String)` 및 `shouldNotMatch(String)`이 모두 `PatternSyntaxException`을 먼저 던지는지 검증한다.
4. `shouldHaveSize`의 `Collection.size` 직접 참조/iterator 0회 경로를 검증한다. 표준 Collection 구현은 O(1)이며 사용자 정의 `Collection.size` 복잡도는 구현 계약을 따른다는 것을 문서와 일치시킨다. 일반 Iterable positive/negative가 각각 iterator를 한 번 열어 전체 입력을 끝까지 순회하는지 counting/single-use 입력으로 검증한다. `shouldContainSame`의 Iterable/array 단일 순회, 일반 hash 분포에서 expected O(n) 시간 및 O(n) 추가 공간을 검증하고, Map overload는 이 보장에서 제외한다. `FloatArray`/`DoubleArray`의 NaN 동등성과 signed-zero 구분, `LongArray × Iterable<Long>` expected 1회 소비도 검증한다. Sequence `shouldContentEqual` 양·음 함수의 identity, same-reference, mismatch 및 one-shot 경로를 검증한다. Predicate pair는 원소별 predicate 1회 및 Collection failure-message 경로에서 재순회 0회를 counting Collection으로 검증한다.
5. Float/Double 일반 positive/negative sign pair가 NaN, 양·음의 무한대, `-0.0`, `-0.0f`, `+0.0`, `+0.0f`, 양수에서 명시된 truth table과 exact complement를 만족한다. Float와 Double 각각의 `shouldBeFinite`/`shouldNotBeFinite` 및 core `require/checkFinite`/`require/checkNotFinite`는 같은 값 집합에서 서로 반대다. finite-sign validators는 두 쌍의 네 함수로, 유한수 안에서 exact complement를 이루고 NaN/±∞는 양쪽 모두 거부한다. `shouldNotBeFinite`/`requireNotFinite`/`checkNotFinite`는 NaN 및 ±∞에 성공하고 ±0에는 실패한다. 일반 `shouldBeNear`/`shouldNotBeNear`는 같은 tolerance에서 상호 보완되고 기본 tolerance도 일치한다. NaN operand와 tolerance, 같은/반대 부호 무한대, 음수 및 ±∞ tolerance를 검증한다. 양·음수 near API는 같은 비유한 tolerance를 모두 `IllegalArgumentException`으로 거부한다. `BigDecimal.shouldBeNear`/`shouldNotBeNear`도 같은 tolerance에서 상호 보완된다. 기존 generic Number sign helper는 sign-only이며 양의 무한대를 허용할 수 있음을 KDoc으로 알린다.
6. core에서 선택된 공통 helper 가족만 제공되고 각 가족이 `require`와 `check` 변형 모두에서 positive/negative 쌍을 갖는다. 선택되지 않은 assertion 전용 API를 core에 1:1 복제하지 않았는지 확인한다. 각 가족의 부정 쌍, nullable receiver 계약, parameter name/lazy message overload가 검증된다. lazy message는 성공 시 0회, 조건 실패에서 메시지를 만들 때 정확히 1회 평가한다. null 및 parameter-name 사전 검증 실패는 대응 positive helper의 기존 예외와 callback 실행 횟수를 보존한다. Map의 `Contains`/`NotContains`, `NotHasKey`, `NotHasValue` 각각에 대해 `require`/`check`의 non-null empty Map 성공/실패, null Map의 기존 null 거부, missing key + null value와 existing null value의 정확한 positive/negative 판정, empty 성공 시 callback 0회 및 해당 조건 실패 시 callback 정확히 1회를 검증한다. 새 `NullOrEmpty`의 non-null empty 성공 경로는 null-only Kotlin contract를 선언하지 않는다. 기존 `requireNullOrEmpty`/`requireNullOrBlank`의 잘못된 contract 제거는 non-null empty/blank receiver를 통해 compile/source regression으로 확인한다.
7. Java/Kotlin Duration의 positive/negative와 zero-or-positive/zero-or-negative pair가 truth table에 따라 검증된다. Kotlin `Duration.INFINITE`/`-Duration.INFINITE`는 sign-only로 판정되며 bounded timeout은 별도 `isFinite()` 검증을 요구한다.
8. `requireNotInRange`/`checkNotInRange`는 `[start, endInclusive]`의 시작·끝 포함을, `requireNotInOpenRange`/`checkNotInOpenRange`는 `[start, endExclusive)`의 시작 포함·끝 제외를 각 경계 값에서 검증한다. 역전된 closed range(`start > endInclusive`)와 빈 half-open range(`start >= endExclusive`)는 기존 비교 의미대로 빈 범위로 처리하고 별도 경계 예외를 추가하지 않는다. 이때 positive는 통상 실패 예외를 던지고 negative는 성공하는 exact-complement 결과를 검증한다. Length/Size positive/negative pair도 inclusive 끝 경계를 검증한다.
9. 검증한 최종 local implementation HEAD에서 `:bluetape4k-assertions:test`와 `:bluetape4k-core:test`를 각각 bounded runner의 `test-assertions`/`test-core` 단계로 실행해 통과한다. 정적 검사는 `:bluetape4k-assertions:detekt`와 `:bluetape4k-core:detekt`를 모듈별로 실행해 통과하고 새 XML report의 finding을 기준선과 비교한다. 향후 PR이 승인되면 그 PR head에서 이 검증을 반복한다.
10. 모든 신규 public assertion/core source overload의 Kotlin/JVM owner와 signature를 compile, `javap -public -s`, 두 module-owned fixture 및 overload matrix로 대조하고 출력을 최종 local implementation HEAD에 연결한다. Matrix에는 overload별 compile/runtime 근거를 적는다. Java Duration descriptor, Kotlin Duration value-class mangling, Float/Double primitive descriptor를 확인한다. Float/Double shouldBeFinite/shouldNotBeFinite는 F -> F와 D -> D receiver-only descriptor가 있어야 한다. finite-sign core helper의 각 require/check 이름에는 Float -> Float, Double -> Double overload 및 (String, Function0?) message descriptor가 있어야 하며 generic Number overload는 없어야 한다. Kotlin Duration Java facade는 범위 밖이다.
11. spec 및 plan 리뷰가 P0=0/P1=0이고, 최종 diff에 미해결 API 짝 누락이 없다.
12. 모든 신규 public overload에 한국어 KDoc이 있고, Duration alias, Kotlin Duration infinity guard, Float/Double finite assertion과 finite-sign pair, Duration positive/negative require/check, nullable NullOrEmpty, range boundary, 배열 equality의 null receiver legacy, non-null expected content overload, nullable expected generic fallback 예시가 각각 compile된다. Assertions와 Core의 영어/한국어 README 및 root CHANGELOG를 갱신한다.
13. `CHANGELOG.md`와 관련 KDoc/README가 NaN sign assertion, Float/Double near 기본 tolerance·비유한 tolerance 거부, non-null expected 배열 negative overload의 content semantics, nullable expected의 generic reference-equality fallback, missing key + null value 동작, 잘못된 Kotlin contract 제거를 설명하고, 회귀 테스트 이름 또는 결과를 구현 검증 기록에 남긴다. 배열 equality가 null 검증 API가 아니라는 경고도 포함한다. 출시 전 revert와 출시 후 patch-forward 복구 조건이 확인된다.

## 설계 근거 파일

- `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Basic.kt`
- `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Arrays.kt`
- `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Collections.kt`
- `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/CharSequences.kt`
- `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Maps.kt`
- `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Numerical.kt`
- `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/DateTimesInstantZoned.kt`
- `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/DateTimesLocal.kt`
- `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/DateTimesLegacy.kt`
- `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Exceptions.kt`
- `bluetape4k/core/src/main/kotlin/io/bluetape4k/support/RequireSupport.kt`
- `bluetape4k/core/src/main/kotlin/io/bluetape4k/support/CheckSupport.kt`
- `bluetape4k/core/src/main/kotlin/io/bluetape4k/javatimes/DurationSupport.kt`
- `bluetape4k/core/src/main/kotlin/io/bluetape4k/support/KotlinDurationSupport.kt`
