# 이슈 #850 Measured composite reduction conversion

issue #850은 generic composite-unit reduction이 right-hand amount를 composite measure에
포함된 unit으로 변환하지 않고 raw value 그대로 사용한다는 점을 찾았다. mixed-scale
reduction은 scale ratio만큼 잘못된 값을 만들었다.

## 결정

reduce하기 전에 operand를 composite unit component로 변환한다.

- `(A/B) * B -> A`는 `B`를 ratio denominator로 변환한다.
- `(A*B) / A -> B`는 `A`를 product first unit으로 변환한다.

이렇게 기존 generic unit algebra를 보존하면서 mixed-scale reduction을 `Measure.in`과
일관되게 만든다.

## 교훈

- composite measure reduction은 raw amount뿐 아니라 양쪽 operand의 unit ratio를 모두
  존중해야 한다.
- `Length * Length -> Area` 같은 domain-specific operator는 lower-level generic algebra의
  bug를 숨길 수 있다. regression test는 generic `UnitsProduct`와 `UnitsRatio` path를
  직접 exercise해야 한다.
- RED case에는 `km/hr * minutes`, `km*km / meters`처럼 reduced-away dimension에
  asymmetric scale을 포함해야 한다.

## 검증

- RED: `./gradlew :bluetape4k-measured:test --tests "io.bluetape4k.measured.MotionTest.속도와 다른 시간 단위로 거리를 계산한다" --tests "io.bluetape4k.measured.AreaTest.면적을 다른 길이 단위로 나누어 길이를 계산한다"`가 `1080.0` vs `18.0`, `1.0` vs `1000.0`으로 실패했다.
- GREEN targeted: 같은 two-test command가 2 tests로 통과했다.
- module: `./gradlew :bluetape4k-measured:test`가 181 tests로 통과했다.
- build: `./gradlew :bluetape4k-measured:build`가 통과했다.
- hygiene: `git diff --check`가 통과했다.
- static analysis: `./gradlew detekt`가 `:detekt NO-SOURCE`와 함께 통과했다.
