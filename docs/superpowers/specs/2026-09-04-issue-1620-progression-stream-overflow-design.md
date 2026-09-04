# Issue #1620: Progression stream 경계 overflow 수정 설계

## 상태와 범위

- 대상 저장소: `bluetape4k-projects`
- 대상 모듈: `bluetape4k-core`
- 대상 버전: `2.1.0`
- 작업 유형: Type C bugfix
- 기준 ref: `develop` / `0be7daa7a6d98937126486ff4b84a76e416696d6`
- 연결 이슈: [#1620](https://github.com/bluetape4k/bluetape4k-projects/issues/1620)

이번 변경은 `IntProgression.asStream()`과 `LongProgression.asStream()`이
정수 경계를 넘을 때 progression에 없는 값을 방출하거나 무한히 진행하는
문제만 다룬다. `partitioning`(#1622), `Period` 연산(#1621), Ignite2 지원
정책은 범위에 포함하지 않는다.

## 문제와 계약

현재 구현은 `IntStream.iterate`/`LongStream.iterate`의 update 함수에서
`current + step`을 직접 계산한다. Kotlin progression이 계산한 `last`가
마지막으로 도달 가능한 값이어도 update 함수는 마지막 원소를 방출한 뒤
primitive overflow를 수행한다. 다음 predicate는 wrap-around된 값을 정상
범위로 오인한다.

재현 결과는 다음과 같다.

```kotlin
intProgressionOf(Int.MAX_VALUE - 1, Int.MAX_VALUE, 2)
    .asStream().limit(2).toArray()
// 현재: [2147483646, -2147483648]
// 기대: [2147483646]

longProgressionOf(Long.MIN_VALUE, Long.MIN_VALUE, -1)
    .asStream().limit(2).toArray()
// 현재: [-9223372036854775808, 9223372036854775807]
// 기대: [-9223372036854775808]
```

`asStream()`의 계약은 Kotlin `IntProgression`/`LongProgression`의 순서와
요소를 그대로 유지하는 것이다. 경계에서 다음 값이 표현 범위를 벗어나면
overflow된 값을 방출하지 않고 정상적으로 종료해야 한다. 기존 public
함수명, 반환 타입, 순차 stream 기본 동작, 정상 범위의 결과는 유지한다.

## 대안 비교

| 대안 | 장점 | 단점 | 결정 |
|---|---|---|---|
| `Stream.Builder`로 되돌림 | diff가 작고 Kotlin iterator의 종료를 그대로 사용 | 전체 요소를 eager materialization하고 이전 성능 개선을 되돌림 | 채택하지 않음 |
| Kotlin progression iterator를 lazy primitive `Spliterator`로 감쌈 | overflow 계산을 Java update lambda에서 제거하고 지연 평가·primitive stream을 유지 | private adapter 코드가 소량 추가됨 | **채택** |
| widened arithmetic 또는 `Math.addExact`/sentinel 조합 | 일부 경계 계산을 직접 제어할 수 있음 | `Int`와 `Long` 종료 규칙이 달라지고 예외·sentinel 상태가 복잡함 | 채택하지 않음 |

## 권장 설계

1. `step == 1`인 `IntProgression`/`LongProgression`은 기존
   `rangeClosed` 최적화를 유지한다.
2. 그 외의 경우에는 각 Kotlin progression의 `iterator()`를 사용하는
   private `AbstractIntSpliterator`/`AbstractLongSpliterator`를 생성한다.
3. `tryAdvance`는 iterator의 `hasNext()`를 확인한 뒤 `nextInt()` 또는
   `nextLong()`을 한 번 방출한다. 종료와 마지막 원소 판정은 Kotlin
   progression iterator에 위임하며, Java 쪽에서 값을 더하지 않는다.
4. `StreamSupport.intStream`/`longStream`으로 순차 primitive stream을
   반환한다. spliterator는 `ORDERED`만 선언하고 크기를 추정하지 않아,
   기존 `iterate` 경로의 lazy·unsized 특성을 보존한다.
5. overflow 예외를 만들거나 삼키지 않는다. Kotlin iterator가 계산한
   progression의 `last`에서 정상 종료하므로 `Math.addExact`와 sentinel이
   필요하지 않다.

이 방식은 현재 저장소에 이미 존재하는 Kotlin progression의 종료 계약을
재사용하고, 새 dependency나 public API를 추가하지 않는다. primitive
전용 adapter를 선택하는 이유는 기존 `Sequence.asStream().mapToInt` 방식의
boxing 비용을 피하기 위해서다.

## 테스트 설계

`ProgressionSupportTest`에 다음 회귀를 추가한다.

- Int: 양의 `step > 1`에서 `Int.MAX_VALUE`, 음의 `step == -1`과
  `step < -1`에서 `Int.MIN_VALUE` 경계
- Long: 양의 `step > 1`에서 `Long.MAX_VALUE`, 음의 `step == -1`과
  `step < -1`에서 `Long.MIN_VALUE` 경계
- 기존 정상 범위의 `step == 1` 결과와 추가하는 정상 `step == -1` 결과

RED 단계에서는 `.limit(2)` 또는 `.limit(3)`을 사용해 현재 wrap-around된
원소가 빠르게 드러나도록 한다. 무제한 `toArray()`/`count()`를 RED 테스트에
사용하지 않아 회귀 테스트 자체가 hang되지 않게 한다. GREEN 단계에서는
같은 테스트로 경계 원소가 정확히 한 번만 방출되는지 확인하고, 기존
15개 테스트와 모듈 검사를 순서대로 실행한다.

## 문서와 호환성

- 두 public KDoc에 경계 overflow 시 정상 종료한다는 문장을 추가한다.
- README/API surface에는 현재 `Progression` 예제가 없으므로 별도 README
  변경은 하지 않는다.
- public JVM descriptor와 함수 signature는 변경하지 않는다.
- Testcontainers, workflow, catalog, Ignite2 runtime, `partitioning` 및
  `Period` 구현은 수정하지 않는다.

## 수용 기준과 DoD

- [ ] Int/Long 양·음수 경계에서 wrap-around 값이 방출되지 않는다.
- [ ] `step > 1`, `step < -1`, `step == -1` 회귀가 모두 통과한다.
- [ ] 정상 범위와 `step == 1` 최적화 결과가 유지된다.
- [ ] 변경된 Kotlin source/test가 `bluetape-kotlin-patterns` 계약을 따른다.
- [ ] targeted test, `:bluetape4k-core:check`, `git diff --check`가 통과한다.
- [ ] P0/P1 finding이 없고, PR/merge는 별도 exact-head gate로 남는다.

## 결정되지 않은 항목

없음. Issue #1619의 Ignite2 복구 제안은 사용자가 확정한 `2.1.0` 전면
폐기 정책과 충돌하므로 이 설계에서 다루지 않으며, 해당 live issue의
상태 변경도 수행하지 않는다.
