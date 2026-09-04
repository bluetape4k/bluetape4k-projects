# Progression partition overflow 교훈

이 문서는 [Issue #1622](https://github.com/bluetape4k/bluetape4k-projects/issues/1622)의 회귀 원인과 재발 방지 규칙을 기록한다.

## 문제

`IntProgression.partitioning`은 `self.last - self.first`를 `Int`로 계산한 뒤 partition 크기를 구했다. `Int.MIN_VALUE..Int.MAX_VALUE`처럼 전체 표현 범위를 포함하는 progression에서는 차이가 `Int` 범위를 넘어 음수로 wrap되어 빈 partition 또는 잘못된 경계를 만들었다.

`LongProgression.partitioning`도 같은 차이를 `Long`으로 계산한 뒤 `.toInt()`로 축소했다. `Long.MIN_VALUE..Long.MAX_VALUE`의 요소 수는 `Long.MAX_VALUE`를 넘으므로 결과가 음수·0으로 변환되었다. `chunked`는 이 잘못된 `count()` 결과를 재사용해 잘못된 partition 수를 계산하거나 `ArithmeticException`을 노출했다.

대표적인 재현 입력은 다음과 같다.

```kotlin
val ints = intProgressionOf(Int.MIN_VALUE, Int.MAX_VALUE)
val intParts = ints.partitioning(2).toList()

val longs = longProgressionOf(Long.MIN_VALUE, Long.MAX_VALUE)
val longParts = longs.partitioning(2).toList()
```

두 경우 모두 원본의 모든 요소를 보존하는 두 개의 유효한 progression이 필요하다. 전체 요소를 실제로 순회하지 않고도 각 partition의 `first`, `last`, `step` 경계로 보존 여부를 검증할 수 있다.

## 원인

범위의 양 끝을 먼저 같은 정수 타입으로 빼는 방식은 다음 두 단계에서 실패했다.

1. `Int`에서는 `last - first` 자체가 overflow한다.
2. `Long`에서는 차이가 `Long`으로 표현되더라도 요소 수가 `Long.MAX_VALUE`보다 클 수 있는데 `.toInt()`가 이를 검증하지 않고 축소한다.
3. `endInclusive + step`으로 다음 시작점을 만들면 마지막 경계에서 다시 overflow할 수 있다.

## 결정

- 요소 수는 JDK의 `java.math.BigInteger`로 `abs(last - first) / abs(step) + 1`을 계산한다. 방향이 맞지 않는 빈 progression은 0으로 처리한다.
- partition 경계는 이전 `endInclusive`에 `step`을 더하지 않고, 원본의 요소 인덱스에 대해 `first + step * index`를 확장 정밀도로 계산한 뒤 `Int` 또는 `Long`으로 정확히 변환한다.
- `partitioning(partitionCount)`은 `partitionCount`가 양수인 한 전체 `Int`/`Long` 표현 범위에서도 경계를 보존한다. `partitionCount == 1`은 원본을 그대로 반환한다.
- `chunked(chunk)`는 안전하게 계산한 요소 수에서 partition 수를 구한다. 결과 partition 수가 `Int.MAX_VALUE`를 넘으면 표현할 수 없으므로 명시적으로 `IllegalArgumentException`을 던진다. 빈 progression은 빈 `Sequence`를 반환한다. 빈 progression의 `partitioning(1)`은 원본을 보존하고 `partitioning(2)` 이상은 빈 `Sequence`를 반환한다.
- 새 외부 의존성은 추가하지 않았다. `BigInteger`는 JDK 표준 라이브러리다.

## TDD 및 검증

초기 RED에서는 기존 기준 테스트 23개에 극단적인 Int/Long 오름차순·내림차순 partition과 `chunked` 표현 한계 검증 6개를 추가해 중간 상태의 `ProgressionSupportTest` 29개를 실행했고, 추가한 6개가 실패했다. `chunked` 경계에서는 기대한 `IllegalArgumentException` 대신 `ArithmeticException`이 노출되었다. 구현 단계에서 extreme non-unit/descending 경계 2개를 추가해 31개로 만든 뒤 GREEN을 확인했다. 이후 리뷰에서 확인된 계약 공백을 해소하기 위해 Int/Long의 빈 progression `chunked`·`partitioning(1/2)`와 정상 descending/non-unit `chunked` 테스트 4개를 추가했다.

구현 후 다음 검증을 완료했다.

- `ProgressionSupportTest`: 35/35 통과
- `:bluetape4k-core:check`: 1,670개 테스트 통과, `BUILD SUCCESSFUL` (리뷰 후 추가 계약 테스트는 targeted suite에서 별도 확인)
- `git diff --check`: 통과
- `code_quality_checker.py`: findings 0

테스트는 양의·음의 step, descending/non-unit `chunked`, 빈 progression의 `chunked`·`partitioning(1/2)`, `partitionCount` 1/2 및 요소 수보다 많은 partition, 전체 Int/Long 범위, `chunked`의 표현 한계를 포함한다. 전체 범위의 모든 요소를 메모리에 올리는 대신 partition 경계와 progression 계약을 검증했다.

## 재발 방지 규칙

1. 범위 차이와 요소 수를 계산하기 전에 대상 타입의 표현 범위를 확인하고, 필요하면 즉시 더 넓은 표현으로 승격한다.
2. 더 넓은 타입의 값을 `toInt()`로 줄일 때는 먼저 상한을 검증하고, 표현 불가능하면 호출자에게 명시적인 예외를 반환한다.
3. progression의 다음 경계를 값 누적으로 계산하지 말고 원본 인덱스에서 독립적으로 계산해 마지막 `step` overflow를 차단한다.
4. 양의·음의 step과 표현 범위의 양 끝을 같은 회귀 테스트 묶음으로 유지한다.
