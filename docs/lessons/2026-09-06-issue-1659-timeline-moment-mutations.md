# TimeLineMomentCollection mutation 경계 교훈

## 문제

`TimeLineMomentCollection`은 domain API로 period를 추가할 때 moment를 만들고 정렬했지만,
동시에 `MutableList<ITimeLineMoment>` 전체를 raw list에 위임했다. 호출자는 raw `add`, `set`,
iterator와 `subList` mutation으로 정렬·endpoint pairing·count 계약을 우회할 수 있었다.

Outer list만 unmodifiable view로 감싸는 첫 접근도 충분하지 않았다. 공개 moment의 `periods`를
변경하거나 collection에 추가한 원본 period를 나중에 `move` 또는 `setup`하면
`startCount`/`endCount`와 moment 시각이 다시 어긋났다.

## 선택

- 기존 `ITimeLineMomentCollection : MutableList<ITimeLineMoment>`와 protected JVM `(List)`
  constructor, companion `invoke(List)`, protected mutation hook은 유지한다.
- 원본 period의 `start`와 `end`를 mutation 전에 한 번씩 capture하고 `start < end`를 검증한다.
- collection은 period를 canonical immutable snapshot으로 소유하고 moment를 endpoint bucket에서
  재구성한다.
- 원본 period reference는 계산에 사용하지 않고 legacy `equals` 기반 occurrence와 remove semantics를
  판정하는 대표값으로만 보존한다. 같은 endpoint라도 다른 구현 또는 `readonly` 값이면 별도
  occurrence로 유지한다.
- 공개 moment와 그 `periods`도 immutable snapshot으로 제공한다.
- snapshot 전환 뒤에도 outer moment와 nested period의 `contains`/`indexOf` 조회는 기존
  timestamp/equality 의미로 연결한다.
- raw outer mutation과 Java default mutation은 상태 변경이나 callback 전에 거부한다.
- constructor input은 duplicate timestamp, empty/unrelated moment, endpoint 누락·중복을 검증한 뒤
  정렬된 canonical snapshot으로 복사한다.
- `addAll`은 전체 입력을 snapshot·검증한 뒤 한 번에 반영해 mixed input의 부분 추가를 막는다.
- equality 대표값 lookup은 set/map index를 사용해 대량 입력의 반복 선형 탐색을 피한다.
- immutable snapshot 내부 holder도 `Serializable` 그래프를 유지하며 Java round-trip 뒤에도
  mutation 경계와 period 기반 변경을 동일하게 보장한다.
- Java raw `containsAll(Collection<?>)`은 호환되지 않는 element를 cast하지 않고 `false`로 처리한다.

입력 moment와 period의 객체 identity는 더 이상 collection 내부 identity가 아니다. 이는 mutable
alias가 계산 상태를 사후 변경하지 못하게 하는 의도된 migration이다. private holder primary
constructor 도입으로 protected JVM descriptor는 유지되지만 Kotlin reflection에서 관찰하는
`primaryConstructor`는 달라질 수 있다.

Java serialization은 같은 라이브러리 버전 안의 round-trip만 지원한다. 기존 `moments` field가
immutable `storage` graph로 바뀌었으므로 이전 버전에서 만든 cache/stream은 호환 대상으로 보지
않으며, 라이브러리 업그레이드 시 폐기하고 다시 생성해야 한다.

## 검증

- RED: 기존 구현에서 신규 계약 5건 실패
- targeted `TimeLineMomentCollectionTest`: 26 passed, failures/errors/skipped 0
- javatimes 전체: 706 passed, 기존 pending 36, failures/errors 0
- `:bluetape4k-javatimes:check`, Kover verify/XML, detekt: 성공
- `combinePeriods`, `intersectPeriods`, `calculateGap`: validated constructor input 회귀 통과
- Java serialization round-trip 및 outer/nested collection mutation conformance matrix 통과
- `javap -protected`: protected `(java.util.List)` constructor와 기존 public bridge 유지
- `git diff --check`: 통과

## Review Miss

Domain collection의 불변식은 container 구조만 막는다고 보존되지 않는다. 반환 element가 다시
mutable collection을 노출하거나 입력 객체 자체가 mutable이면 alias를 따라 invariant가
우회된다. mutation surface 검토는 outer collection, nested collection, element lifecycle을 한
경계로 다뤄야 한다.

## 다음 변경 시 지킬 점

1. 계산용 snapshot에는 caller-owned mutable object를 그대로 저장하지 않는다.
2. 여러 endpoint를 갱신하는 작업은 입력 전체를 먼저 검증한 뒤 commit한다.
3. legacy mutable ABI를 유지할 때 허용된 domain mutation과 raw mutation을 명시적으로 분리한다.
4. protected extension hook도 public invariant보다 약한 부분 상태를 만들지 않게 한다.
