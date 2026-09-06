# Issue #1657: deque 앞쪽에 batch를 복원할 때는 역순으로 삽입한다

## 맥락

`LettuceLoadedMap`은 write-behind batch를 queue 앞에서 꺼내고, 동일 키가 여러 번
등장하면 마지막 값을 `MapWriter`에 전달한다. 쓰기가 실패했을 때 꺼낸 entry를 원래
순서대로 `offerFirst`하면 deque에는 역순으로 복원된다. `(key, A), (key, B)`가
`(key, B), (key, A)`로 바뀌므로 다음 batch는 오래된 `A`를 선택한다.

## 결정

- deque 앞쪽에 실패한 batch를 복원할 때는 entry를 역순으로 순회한다.
- queue 여유가 부족하면 같은 키의 최신 retry entry를 먼저 보존한다.
- entry별 retry count와 기존 dead-letter 경계는 바꾸지 않는다.
- 동일 키의 마지막 값 선택은 재시도 전후와 재시도 중 새 값이 도착한 경우에도 유지한다.

## 검증

- 기존 구현에서 `A -> B -> 실패 -> 재시도`가 `B -> A`로 바뀌는 RED를 확인했다.
- 실패 중 `C`가 도착하는 경로에서 재시도 batch가 `B`, 후속 batch가 `C`를 선택함을
  검증했다.
- retry count 소진과 queue 포화 dead-letter 테스트를 함께 실행해 기존 경계를 확인했다.
- `:bluetape4k-lettuce:test` 926개와 `:bluetape4k-lettuce:detekt`가 통과했다.

## 향후 지침

FIFO batch를 deque의 앞쪽에 복원할 때는 `addFirst`나 `offerFirst` 호출 순서만 보지
말고, 복원 뒤 전체 queue 순서와 동일 키 병합 결과를 함께 검증한다. append-only
channel은 앞쪽 삽입을 지원하지 않으므로 같은 정책을 그대로 적용하지 않는다. channel
기반 구현은 accepted entry 보존, 용량 상한, close와 retry의 경합을 먼저 정의한 뒤
별도 회귀 테스트로 고정한다. 이 후속 작업은 Issue #1664에서 추적한다.
