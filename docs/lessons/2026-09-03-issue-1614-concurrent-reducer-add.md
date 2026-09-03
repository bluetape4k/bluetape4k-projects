# Issue #1614: 비동기 반환 API의 실행 경계를 enqueue 뒤로 분리한다

## 맥락

`ConcurrentReducer.add()`는 job을 queue에 넣은 뒤 호출 스레드에서 `pump()`를 직접
실행했다. 첫 job의 task lambda가 blocking하면 `CompletableFuture`를 반환하기 전에
호출자가 함께 차단되었고, queue의 timed polling도 호출 경계에서 수행될 수 있었다.

## 결정

- `add()`는 `admissionLock` 안에서 close 상태 확인, queue admission, pump scheduling만
  수행하고 promise를 즉시 반환한다.
- task invocation과 queue polling은 기존 단일 `pumpExecutor`에서만 수행한다.
- `pumpScheduled` atomic flag로 중복 pump 제출을 합치고, pump 종료 직전에 처리 가능한
  queue 항목이 남으면 한 번만 다시 예약한다.
- executor에 새 admission이 전달되므로 빈 queue를 기다리는 10ms timed polling을
  제거하고 non-blocking `poll()`을 사용한다.
- `add()`와 `close()`는 같은 lock으로 선형화한다. add가 먼저 승인되면 job은 실행되거나
  close에서 취소되고, close가 먼저 완료되면 add는 `RejectedExecutionException` future를
  반환한다.

## 결과

호출자는 task가 아직 실행 중이어도 미완료 promise를 즉시 받는다. 기존
`maxConcurrency` permit, 입력 queue capacity, 취소된 queued job 건너뛰기, task 결과와
예외 전달, close 시 queued promise 취소 계약은 유지된다. completion callback은 직접
`pump()`를 호출하지 않고 coalesced scheduler만 깨운다.

## 검증

- 수정 전 latch 회귀: task release 전 `add()` 호출 future가 완료되지 않아 실패.
- 수정 후 `ConcurrentReducerTest`: 15 passing.
- `:bluetape4k-core:test`: 1647 passing, `BUILD SUCCESSFUL`.
- `:bluetape4k-core:detekt`: `BUILD SUCCESSFUL`, 변경 파일 신규 진단 없음.
- `add/close` 동시 실행 32회에서 반환 promise가 모두 terminal 상태에 도달함을 확인.

## 향후 지침

`CompletableFuture`를 반환하는 admission API에서 task invocation, blocking poll, `get`,
`join`을 호출자 경계에 두지 않는다. close와 add가 같은 queue를 변경하면 하나의 짧은
linearization lock으로 admission을 결정하고, executor wake-up은 coalescing flag와
queue/permit 재검사로 lost wake-up을 방지한다.
