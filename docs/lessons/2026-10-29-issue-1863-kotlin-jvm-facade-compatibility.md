# Kotlin 파일 facade 변경과 Java ABI 호환성

관련 이슈: [#1863](https://github.com/bluetape4k/bluetape4k-projects/issues/1863)

## 맥락과 놓친 점

Kotlin top-level 함수의 Java 호출 계약은 소스 파일명이 아니라 컴파일된
`*Kt` facade owner와 JVM descriptor에 고정된다. `@JvmName` 없이 파일을
분리하거나 이름을 바꾸면 Java 소비자가 참조하던 owner가 사라질 수 있다.
마찬가지로 `Long` 인자를 `Duration`으로 바꾸거나 `ExecutorService` 인자를
추가하면 Kotlin 호출은 컴파일되어도 기존 Java descriptor가 제거될 수 있다.

`compileOnly` 의존성도 제품 코드가 컴파일되는 것과 published runtime
계약을 보장하는 것은 다르다. `FlowAssertions`가 공개 타입에서
`kotlinx.coroutines`를 사용한다면 POM과 Gradle Module Metadata의 API/runtime
variant에 직접 의존성이 남아야 한다.

## 결정

- `TimeoutSupportKt`에는 기존
  `asyncRunWithTimeout(long, Function0)` descriptor를 유지하는 JVM bridge를
  두고, executor를 명시하는 새 본문 API는 그대로 보존한다.
- 날짜 assertion 파일은 `@file:JvmName`으로 기존
  `DateTimesInstantZonedKt`, `DateTimesLegacyKt`, `DateTimesLocalKt` owner를
  유지한다.
- `KotlinDurations.kt`는 기존 `DurationAssertionsKt` owner를 유지하고,
  기존 Java `java.time.Duration` 메서드도 명시적 `@JvmName` bridge로 같은
  owner에 남긴다. 새 `JavaDurationsKt` owner도 별도로 보존한다.
- `FlowAssertions`가 소비자 classpath에서 필요로 하는
  `kotlinx-coroutines-core`는 `api` 의존성으로 선언한다.
- source-to-facade owner map, `javap`, Kotlin/Java consumer fixture, 생성
  POM/GMM 검사를 함께 유지해 파일 이동·signature 변경·publication 변경을
  서로 다른 회귀로 검출한다.

## 검증

- 수정 전 RED: 기존 timeout descriptor와 `DateTimesInstantZonedKt` 로딩이
  각각 `NoSuchMethodException`/`ClassNotFoundException`으로 실패했다.
- `:bluetape4k-core:cleanTest :bluetape4k-core:test`: 1,723 passing.
- `:bluetape4k-assertions:cleanTest :bluetape4k-assertions:test`: 724 passing.
- Java consumer fixture가 timeout bridge와 기존 assertions facade를 실제
  Java 소스에서 컴파일하고 실행했다.
- `javap`에서 timeout 2-인자 descriptor와 네 기존 assertions owner를
  확인했고, final assertion symmetry runner의 baseline/final owner 단계가
  모두 통과했다.
- 생성 POM의 compile-scope coroutines 의존성과 GMM `apiElements` 항목을
  회귀 테스트로 확인했다.

## 향후 지침

- public Kotlin top-level API를 이동·분리할 때는 소스 diff보다 먼저 기존
  `javap -public -s` owner와 descriptor를 고정하고, 필요하면
  `@file:JvmName` 또는 명시적 JVM bridge를 추가한다.
- 기본 인자 변경은 Kotlin metadata만 확인하지 말고 Java descriptor와
  Java/Kotlin consumer fixture를 함께 검증한다.
- 공개 타입의 compile-only 의존성은 생성 POM과 GMM의 실제 variant를
  확인한다. 컴파일 성공만으로 publication 계약을 완료했다고 판단하지
  않는다.
