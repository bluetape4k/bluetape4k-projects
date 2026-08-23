# Issue #1349: terminal 상태와 active producer 수는 함께 선형화해야 한다

## 맥락

`BufferedResumableCollector`는 producer를 mutex로 직렬화했지만 terminal 상태와
producer admission은 별도로 관리했다. `complete()`와 `error()`는 value waiter만
깨웠기 때문에 capacity를 기다리는 producer가 남을 수 있었고, drain은 빈 queue를
보는 즉시 종료해 terminal과 교차한 값을 잃을 수 있었다. 분리된 error 필드는 후속
terminal 호출이 첫 cause를 덮어쓰는 것도 허용했다.

## 결정

- terminal kind, cause, active admission 수를 하나의 immutable atomic 상태로 묶는다.
- `next`는 `Open(n) -> Open(n + 1)` CAS에 성공한 뒤에만 producer mutex에 진입하고,
  모든 반환·실패·취소 경로에서 `finally` 한 곳으로 admission을 감소시킨다.
- `Preparing -> Committed` CAS로 offer의 선형화 지점을 고정한다. `Preparing` 중 terminal이
  먼저 반영되면 offer를 거부하고, `Committed` 뒤 terminal은 실제 queue 적재가 끝날 때까지
  pending 상태로 보존한다. commit 뒤에는 cancellation을 다시 검사하지 않는다.
- 첫 terminal CAS만 상태를 바꾸고 value waiter와 capacity waiter를 모두 깨운다.
- drain은 queue가 비고 terminal 상태이며 active admission이 0일 때만 종료한다.

## 결과

capacity 1에서 terminal과 producer가 경합해도 이미 적재된 값은 순서대로 전달되고,
enqueue하지 못한 producer는 bounded time 안에 `IllegalStateException`으로 종료한다.
`error`가 먼저 선형화되면 buffered 값을 전달한 뒤 동일한 cause를 전파하며, 후속
`complete`, `error`, `error(null)`은 첫 상태를 바꾸지 않는다. collector 실패나
취소는 suspend producer를 깨우고 원인을 보존한 `CancellationException`으로 전달한다.

## 검증

- 기존 구현: `BufferedResumableCollectorTest` 10개 중 3개 실패
  - complete 경합: 5초 timeout
  - capacity 1, 8 producer, 64회 stress: 10초 timeout
  - first error identity: 후속 error로 덮어쓰기
- 변경 후 `BufferedResumableCollectorTest`: 19개 통과
- 같은 테스트 class를 `--rerun-tasks`로 3회 실행: 매회 19개 통과, timeout 0
- `:bluetape4k-coroutines:detekt :bluetape4k-coroutines:build`: 630개 통과,
  Kover 검증 포함 `BUILD SUCCESSFUL`

## 놓치기 쉬운 점

terminal boolean과 active producer counter를 서로 다른 atomic으로 두면 drain이 두 값을
서로 다른 시점에 읽어 조기 종료할 수 있다. producer mutex만 terminal 함수에 적용하면
non-suspending API가 full buffer의 producer를 깨우지 못한다. terminal과 admission은
하나의 CAS 상태로 관찰돼야 하고 mutex는 SPSC queue의 producer 직렬화에만 사용해야 한다.

## 향후 지침

buffered producer/consumer 경합 테스트는 `delay`나 반복 `yield`로 순서를 추정하지 않는다.
`CoroutineStart.UNDISPATCHED`, `CompletableDeferred`, full-buffer fixture로 producer가 실제
suspension 지점에 도달했음을 고정하고, 수락된 값 집합·terminal cause identity·producer
완료 결과를 각각 assertion한다.
