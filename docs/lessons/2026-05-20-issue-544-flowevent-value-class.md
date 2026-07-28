# 이슈 544 FlowEvent Value-Class 평가

## 배경

Issue #544는 Flow operator hot path allocation을 줄이기 위해 `FlowEvent.Value`와 `FlowEvent.Error`를
data class에서 Kotlin/JVM value class로 옮길지 묻는 항목이었다.

## 결정

두 wrapper 모두 data class로 유지한다. Kotlin/JVM value class는 interface를 구현할 수 있지만, 다른
type으로 사용될 때 box된다. 현재 API는 이 event들을 `FlowEvent<T>`로 emit/consume하므로 중요한
interface-typed path에서 value-class implementation도 여전히 allocation을 만든다.

## 결과

기존 TODO comment를 explicit KDoc decision으로 교체했고, value class로 바꾸면 사라질 source convenience인
`component1()` destructuring과 `copy()`를 test로 고정했다.

## 검증

- Kotlin official documentation에서 inline value class의 interface inheritance와 boxing rule 확인.
- `rg`와 IDE reference lookup으로 `FlowEvent` call site 확인. IDE는 dumb mode 때문에 일부 unavailable했고,
  current usage의 source of truth는 `rg`였다.

## 향후 agent 가이드

이를 단순 `@JvmInline value class ... : FlowEvent<T>` 변경으로 다시 검토하지 않는다. Public API가 hot
path에서 `FlowEvent<T>` interface upcast를 피할 수 있거나 Kotlin/JVM이 interface/generic 사용의 boxing
behavior를 바꿀 때만 다시 연다.
