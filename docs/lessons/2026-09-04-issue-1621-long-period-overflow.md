# Issue #1621: Long 기반 Period 연산에서 범위 축소를 허용하지 않는다

## 맥락

`NumberExtensions.kt`의 `Long` 기반 `Period` 변환과 연산자는
`java.time.Period`의 `Int` 구성 요소를 사용하면서 입력을 `toInt()`로 변환했다.
따라서 호출자는 `Long` API를 사용해도 값이 보존된다는 보장을 받을 수 없었다.

## 원인

Kotlin의 `Long.toInt()`는 범위를 검사하지 않고 하위 32비트만 보존한다.
`Long.MAX_VALUE`는 `-1`이 되고, `4_294_967_296L`은 `0`이 된다. 그 결과
`dayPeriod()`가 `P-1D`를 반환하거나, 큰 주 값이 `Period.ZERO`가 되며,
곱셈은 음수 기간으로 바뀌고 나눗셈은 `/ by zero`를 발생시켰다.

## 결정

- 이미 제공 중인 `io.bluetape4k.support.toIntExact()`를 모든 `Long` 기반
  `Period` 변환과 곱셈에 재사용한다.
- `Period.div(Long)`는 `toIntExact()`보다 먼저 `scalar != 0L`을 요구해 0 divisor를
  일관된 `IllegalArgumentException`으로 거부한다.
- 정상 범위의 결과와 음수 divisor의 Java 정수 나눗셈 의미는 유지하고,
  public API signature와 JVM descriptor는 변경하지 않는다.

## 결과

`Int.MIN_VALUE..Int.MAX_VALUE` 밖의 `Long` 값은 조용히 잘리지 않고
`IllegalArgumentException`을 발생시킨다. 변환, 양방향 곱셈, 나눗셈 모두 같은
범위 계약을 사용하며, 0 divisor는 Java 산술 예외에 도달하기 전에 거부된다.

## 검증

- RED: 수정 전 `NumberExtensionsTest`는 23개 중 2개가 범위/0 divisor 예외를
  발생시키지 않아 실패했다.
- GREEN: 수정 후 `NumberExtensionsTest` 23/23 통과.
- 전체 core 테스트: 1,662/1,662 통과, failure/error/skipped 0.
- `:bluetape4k-core:check`: `BUILD SUCCESSFUL`.

## 향후 지침

`Long` 값을 `Int` 기반 Java API에 전달할 때는 `toInt()`를 사용하지 말고
`toIntExact()` 또는 동등한 명시적 범위 검증을 재사용한다. 변환·곱셈·나눗셈처럼
같은 축소 경계를 공유하는 overload는 한쪽만 고치지 말고 모두 회귀 테스트에
포함한다. 테스트에는 `Int` 경계 초과, `Long.MIN_VALUE`, `Long.MAX_VALUE`,
음수 divisor, 0 divisor를 함께 넣어 wrap-around와 예외 형태를 구분한다.
