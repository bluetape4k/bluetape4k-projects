# Issue #1612: 미수집 groupBy 그룹은 terminal 상태로 닫아야 한다

## 맥락

`groupBy`는 새 그룹을 방출한 뒤 `FlowGroup.next()`에서 consumer가 준비될 때까지
대기한다. consumer가 준비되지 않으면 기존 5초 timeout 경로가 그룹을 map에서
제거하고 `cancelled`만 설정했다. 이미 방출된 `GroupedFlow`를 나중에 수집하면
`collectSafely()`가 `valueReady.await()`에서 영구 대기할 수 있었다.

## 결정

- consumer timeout을 단순 취소가 아니라 그룹의 terminal 전이로 취급한다.
- `map.remove(key, group)`으로 동일 키의 새 그룹을 실수로 제거하지 않도록 조건부
  삭제한다.
- `done = true`를 설정한 뒤 `valueReady.resume()`으로 현재 또는 late collector를
  깨운다. late collector는 빈 Flow로 정상 종료하며 업스트림에는 timeout을 전파하지
  않는다.
- 이 계약을 `groupBy` KDoc에 명시하고, virtual time 회귀 테스트로 고정한다.

## 결과

포기된 그룹은 map에서 제거되고 terminal 상태를 관찰할 수 있다. late collector는
bounded time 안에 빈 목록을 받고, 기존 그룹 수집·오류·다운스트림 취소 동작은
그대로 유지된다.

## 검증

- 수정 전 회귀 테스트: 100ms timeout으로 실패(미수집 그룹의 `valueReady.await()` 영구 대기).
- 수정 후 targeted regression: 1 passing.
- `GroupByTest`: 13 passing.
- `:bluetape4k-coroutines:test`: 631 passing, `BUILD SUCCESSFUL`.
- `:bluetape4k-coroutines:detekt`: 기존 `groupBy.kt` 포함 저장소 전반의 사전 존재 위반으로
  실패했으며, 변경으로 새 진단을 추가하지 않았다.

## 향후 지침

bounded wait가 producer를 포기시키는 모든 Flow 그룹/collector 구현은 취소 플래그만
설정하지 말고 terminal 상태와 waiter wake-up을 함께 선형화해야 한다. map 정리와
terminal 신호가 경합할 때는 조건부 삭제와 늦게 도착한 collector의 종료 증거를 각각
테스트한다.
