# 이슈 #744: Suspending observation lifecycle

## 배경

`Observation.withObservationContextSuspending`은 Micrometer observation을 coroutine
context에 bind했지만 catch block에서만 observation을 stop했다. 성공한 suspend path가
`onStop` 없이 완료될 수 있었다.

## 결정

suspending observation context helper에서는 `finally`에서 `observation.stop()`을
호출한다. cancellation과 non-cancellation error를 분리한다. cancellation은 error 기록
없이 rethrow하고, non-cancellation exception은 final stop 전에 계속
`observation.error(e)`를 호출한다.

## 검증

- pre-fix regression은 `handler.stopped == 0`으로 실패했다.
- 같은 focused regression은 fix 후 통과했다.
- 관련 observation coroutine과 event telemetry test가 통과했다.
- full `:bluetape4k-micrometer:test`가 통과했다.

## 향후 가드

`Observation`을 시작하거나 소유하는 모든 helper는 `finally`에서 stop해야 한다. test는
current context cleanup뿐 아니라 success path의 handler `onStop` count도 assert해야
한다.
