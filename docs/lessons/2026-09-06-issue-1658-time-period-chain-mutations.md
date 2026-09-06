# #1658: MutableList 위임은 불변식을 자동으로 지켜주지 않는다

## 문제

`TimePeriodChain`은 `TimePeriodContainer`의 `MutableList` 위임을 상속한다. 직접 구현한
`set`은 기존 원소를 `removeAt`으로 먼저 삭제한 뒤 지원하지 않는 indexed `add`를 호출했다.
따라서 `UnsupportedOperationException`이 발생해도 chain은 이미 손상됐다.

직접 override하지 않은 `removeAt`, `clear`, iterator, list iterator, `subList`, `removeIf`,
`replaceAll`, 정렬 API와 공개 `periods` view도 raw backing list를 변경할 수 있었다.
`remove(element)`와 `add(index, element)` 두 메서드만 막아서는 `MutableList` 계약의 우회
경로를 닫을 수 없다.

## 결정

- 기존 `MutableList` ABI와 public no-arg constructor는 유지한다.
- chain이 지원하는 collection mutation은 [끝에 추가하는 `add`와 `addAll`]로 한정한다.
- private storage는 mutable backing과 `Collections.unmodifiableList` view를 함께 소유한다.
- 상위 `TimePeriodContainer`의 delegate와 공개 `periods`에는 unmodifiable view만 전달한다.
- `TimePeriodChain.add`만 private backing에 접근해 기존 contiguity 재배치 뒤 append한다.
- 기존 기간과 같은 값을 다시 추가하면 setup 전에 `false`를 반환해 동일 객체를 이동하거나
  self collection을 순회 중 변경하지 않는다.
- chain 자신이나 동일한 `periods` view를 `addAll` source로 받으면 snapshot 전에 `false`를
  반환한다.
- `set`, indexed add, remove와 outer Java default mutation은 상태 변경이나 callback 호출 전에
  같은 종류의 예외를 던진다.

custom collection wrapper는 iterator의 변경 감지까지 다시 구현해야 하므로 사용하지 않았다.
JDK unmodifiable view가 delegate와 collection view mutation을 한 경계에서 거부하고, outer
`removeIf`, `replaceAll`, sort는 `TimePeriodChain`이 직접 override해 callback 전에 거부한다.

## RED/GREEN

RED에서 targeted 16개 중 2개가 실패했다.

- 실패한 `set` 뒤 chain 크기가 3개에서 2개로 줄었다.
- `removeAt(0)`은 예외 없이 첫 원소를 삭제했다.

GREEN에서는 다음 경로를 각각 새 chain에서 실행하고 `UnsupportedOperationException`과 원본
순서 보존을 함께 검증했다.

- `set`, `remove`, `removeAt`, `clear`, `removeAll`, `retainAll`, `removeIf`, `replaceAll`
- iterator/list iterator의 `remove`, `set`, `add`
- `subList`의 `clear`, `set`, `add`
- indexed `addAll`, `reset`, 공개 `periods`의 직접·iterator mutation
- start/end/duration 정렬
- 기존 원소 재추가와 `addAll(chain)`, `addAll(chain.periods)`의 no-op 계약
- Java default mutation의 predicate, operator, comparator가 호출되지 않는 계약
- 상속한 `periods` 구현을 사용하는 subclass에도 같은 mutation guard가 적용되는 계약

`TimePeriodChain`은 기존 공개 상속 계약을 유지한다. 따라서 subclass가 `periods`를 직접
재정의하면 이 mutation과 contiguity 계약을 해당 subclass가 보존해야 한다.

## 결과와 검증

- targeted `TimePeriodChainTest`: 20 passed, failures/errors/skipped 0
- javatimes 전체: 696 passed, 36 기존 pending, failures/errors 0
- `:bluetape4k-javatimes:check`, Kover verify, detekt: 성공
- `javap -public`: 기존 public no-arg constructor와 `MutableList` bridge 유지
- production caller 검색: 저장소 안에서 destructive chain mutation 사용 없음
- `git diff --check`: 통과

## Review Miss

초기 구현은 눈에 보이는 override만 chain 계약이라고 간주했다. Kotlin delegation이 생성한
bridge, collection view가 반환하는 iterator와 `subList`, Java default mutation까지 같은
backing을 공유한다는 점을 함께 검토해야 했다.

## 다음 변경 시 지킬 점

1. invariant가 있는 collection은 직접 선언한 mutation뿐 아니라 모든 view와 default method를
   검토한다.
2. 실패를 지원하지 않는 계약으로 표현할 때 상태를 먼저 바꾸지 않는다.
3. 공개 mutable 타입을 즉시 바꿀 수 없다면 ABI는 유지하되 backing 접근 권한을 분리한다.
4. 허용된 mutation과 거부된 mutation을 같은 delegate에 섞지 않는다.
