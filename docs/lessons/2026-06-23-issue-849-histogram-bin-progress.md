# 이슈 #849 Histogram bin progress guard

issue #849는 histogram bin builder가 caller-provided bin increment를 신뢰한다는 점을
찾았다. zero 또는 negative bin size는 shared comparable bin loop가 range를 무한히
append하게 만들 수 있었고, non-finite `Double` bin size에는 public guard가 없었다.

## 결정

shared bin loop에 들어가기 전에 invalid typed bin size를 거부한다.

- `Double` bin size는 finite이고 0보다 커야 한다.
- `BigDecimal` bin size는 0보다 커야 한다.
- custom `binByComparable` incrementer는 매 loop iteration마다 current range end를 반드시
  증가시켜야 한다.

## 교훈

- public numeric histogram API는 typed boundary에서 progress를 검증해야 한다. caller가
  간단한 zero 또는 negative size로 non-terminating bin을 만들 수 있으면 안 된다.
- shared generic loop도 defensive invariant가 필요하다. caller가 typed helper 밖에서
  custom incrementer를 제공할 수 있기 때문이다.
- non-progressing loop의 RED test는 test JVM을 고갈시킬 수 있다. 실패 재현을 하나
  확보한 뒤 full suite 전에 fail-fast guard를 추가한다.

## 검증

- RED: `./gradlew :bluetape4k-math:test --tests "io.bluetape4k.math.DoubleHistogramTest.binByDouble rejects non progressing bin sizes"`가 zero bin size로 range가 계속 커지며 test JVM `Java heap space`로 실패했다.
- GREEN targeted: `./gradlew :bluetape4k-math:test --tests "io.bluetape4k.math.DoubleHistogramTest.binByDouble rejects non progressing bin sizes" --tests "io.bluetape4k.math.BigDecimalHistogramTest.binByBigDecimal rejects non progressing bin sizes" --tests "io.bluetape4k.math.ComparableHistogramTest.binByComparable rejects non progressing incrementers"`가 3 tests로 통과했다.
- module: `./gradlew :bluetape4k-math:test`가 573 tests, 1 pending으로 통과했다.
- build: `./gradlew :bluetape4k-math:build`가 통과했다.
- hygiene: `git diff --check`가 통과했다.
- static analysis: `./gradlew detekt`가 `:detekt NO-SOURCE`와 함께 통과했다.
