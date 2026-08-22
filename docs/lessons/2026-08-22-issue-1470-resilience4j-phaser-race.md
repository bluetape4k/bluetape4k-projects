# Issue #1470: cancellation 테스트의 `Phaser` phase race

## 현상

Nightly run `32550558776`의 `Test / Infra (kafka-resilience)` 첫 시도가
`FlowCircuitBreakerTest` 출력 도중 멈췄고, workflow retry의 60분 timeout으로
종료됐다. 두 번째 시도는 동일 명령을 1분 13초에 완료했다.

## 원인

`Phaser(1)`과 `awaitAdvance(1)`은 one-shot 시작 신호가 아니다. child가 먼저
`arrive()`하면 phase가 `0`에서 `1`로 바뀐다. 이후 parent가
`awaitAdvance(1)`을 호출하면 다음 phase 전환을 기다리지만, 전환 주체가 없어
무기한 block된다. parent가 먼저 호출하면 현재 phase가 `0`이므로 즉시
반환해 실행 순서에 따라 테스트가 통과하거나 멈췄다.

`runSuspendTest`의 `withTimeout`도 blocking `Phaser.awaitAdvance`를 중단할 수
없어, 테스트 helper의 3분 timeout이 아니라 workflow의 60분 timeout까지
runner를 점유했다.

## 수정 원칙

- coroutine 테스트의 one-shot 시작 신호는 `CompletableDeferred<Unit>`의
  `complete(Unit)`과 `await()`를 사용한다.
- cancellation 테스트는 명시적인 10초 `runSuspendTest` timeout 안에서
  실행해 이후 동기화 회귀도 fail-fast로 드러나게 한다.
- 같은 패턴이 있던 `FlowCircuitBreakerTest`와 `BulkheadFlowTest` 네 경로를
  함께 수정한다.

## 검증

- Hosted RED: run `32550558776`, attempt 1 `Timeout of 3600000ms hit`
- Retry 비교: attempt 2 `BUILD SUCCESSFUL in 1m 13s`
- Targeted GREEN: 두 테스트 클래스 11개 테스트 통과
