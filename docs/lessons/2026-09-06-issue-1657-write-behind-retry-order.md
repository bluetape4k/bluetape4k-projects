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

## Suspended channel 후속 결정

Issue #1664에서 append-only `Channel` 뒤에 실패 batch를 다시 보내는 방식도 같은 키의
최신 순서를 깨뜨린다는 점을 재현했다. 실패 처리 중 `(key, C)`가 도착하면 channel이
`C, A, B`가 되고, 다음 batch에서 오래된 `A`가 `C`를 덮을 수 있었다.

- consumer coroutine만 접근하는 retry queue에 실패 batch를 원래 순서대로 보관한다.
- consumer는 retry queue를 신규 channel entry보다 먼저 비운다.
- 이미 channel에 accepted된 entry는 재시도 시 channel 용량과 다시 경쟁시키지 않는다.
- shutdown timeout이나 caller cancellation로 consumer를 취소할 때는 처리 중 batch를
  `NonCancellable` 경계에서 dead-letter에 기록한 뒤 retry queue와 channel 잔여분도
  latest-key-wins로 병합해 보존한다.
- retry count, dead-letter 한계와 public API는 변경하지 않는다.

회귀 테스트는 첫 실패 batch가 `B`로 병합되고 재시도도 `B`, 실패 중 도착한 신규 값은
그 뒤의 `C`로 기록되는 순서를 고정한다. channel이 가득 찬 경우에도 accepted retry가
내부 queue에 남고 dead-letter로 조기 이동하지 않는 계약을 함께 검증했다.
또한 `close()`와 `suspendClose()` timeout, `suspendClose()` caller cancellation에서도
처리 중 entry와 channel 잔여분이 dead-letter recovery value로 남는지 검증했다.

## 향후 지침

FIFO batch를 deque의 앞쪽에 복원할 때는 `addFirst`나 `offerFirst` 호출 순서만 보지
말고, 복원 뒤 전체 queue 순서와 동일 키 병합 결과를 함께 검증한다. append-only
channel은 앞쪽 삽입을 지원하지 않으므로 같은 정책을 그대로 적용하지 않는다. channel
기반 구현에서는 consumer 전용 retry queue를 우선 처리하고, accepted entry 보존과
close·retry 경합을 별도 회귀 테스트로 고정한다.
