# #1278 Blocking loaded-map close interruption 복원

## Context

`LettuceSuspendedLoadedMap.close()`는 `Closeable` 호환을 위해 `runBlocking`으로
write-behind drain을 기다립니다. 호출 스레드가 대기 중 interrupt되면
`runBlocking`이 `InterruptedException`을 던지는데, 기존 broad `catch (Exception)`이
이를 일반 drain 실패로 처리하면서 thread interrupt flag를 소비했습니다.

## Decision or Finding

- blocking close의 `InterruptedException`을 timeout 또는 일반 drain 실패와 분리해
  처리하고, 로그를 남기기 전에 `Thread.currentThread().interrupt()`로 상태를
  복원합니다.
- interruption이 발생해도 기존 `finally` 경로에서 owned job과 Redis connection을
  계속 정리합니다.
- `suspendClose()`의 caller cancellation 전파와 내부 timeout 동작은 변경하지
  않습니다.

## Outcome

외부 shutdown 또는 executor coordination이 전달한 interruption 신호가
`close()`를 통과해도 호출 스레드에 보존됩니다. interruption, timeout, 일반 drain
실패를 로그에서 구분할 수 있고, interruption 중에도 소유 리소스 정리는 유지됩니다.

## Verification

- RED: 새 blocking-close 회귀 테스트가 수정 전 interrupt flag 소실(`false`)로
  실패했습니다.
- GREEN: 회귀 테스트와 MockK failure-path 테스트 7개가 통과했습니다.
- `:bluetape4k-lettuce:test` 전체 870개가 통과했습니다.
- `:bluetape4k-lettuce:detekt` task와 `git diff --check`가 통과했습니다. detekt
  출력의 기존 finding은 변경 파일 외부에 남아 있습니다.

## Future Guidance

`runBlocking`을 사용하는 blocking bridge에서 `InterruptedException`을
`Exception`으로만 처리하지 않습니다. interruption은 호출자 제어 신호이므로
별도 catch에서 상태를 복원하고, non-suspending cleanup은 모든 실패 경로의
`finally`에서 수행합니다. timeout과 interruption의 로그·예외 semantics를 함께
검증하고, suspend 경로의 cancellation 계약을 별도 회귀 테스트로 보존합니다.
