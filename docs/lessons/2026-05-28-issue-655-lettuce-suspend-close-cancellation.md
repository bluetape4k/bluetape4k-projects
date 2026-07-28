# 이슈 655: Lettuce suspended loaded-map shutdown cancellation 보존

## 배경

`LettuceSuspendedLoadedMap.suspendClose()`는 shutdown timeout 안에서 write-behind job을
기다렸지만, 기존의 넓은 `Exception` catch가 pending write를 drain하는 동안 coroutine
cancellation을 숨길 수 있었다.

## 결정

caller cancellation을 관찰 가능하게 유지한다. map-owned shutdown timeout에는
`withTimeoutOrNull`을 사용해 internal drain timeout만 warning으로 낮춘다. generic
failure logging 전에 `CancellationException`을 다시 던지고, connection/job cleanup은
`NonCancellable`에서 유지한다.

## 결과

`suspendClose()`는 이제 external cancellation과 internal drain timeout을 구분한다.
regression test는 writer가 아직 blocked 상태일 때 caller cancellation이
`writeBehindShutdownTimeout`보다 먼저 반환됨을 검증한다.

## 검증

- `./gradlew :bluetape4k-lettuce:test --tests 'io.bluetape4k.redis.lettuce.map.LettuceSuspendedLoadedMapTest.suspendClose - caller cancellation is propagated before internal shutdown timeout'`
- `./gradlew :bluetape4k-lettuce:test`

## 향후 지침

suspending cleanup path에서는 `CancellationException`을 먼저 rethrow하지 않는 한 suspend
call 주변에서 `Exception`을 catch하지 않는다. operation-owned timeout을 caller
cancellation과 혼동하지 않고 local에서 처리해야 할 때는 `withTimeoutOrNull`을 우선한다.
