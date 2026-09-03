# Issue #1615: 취소 가능한 본문과 트랜잭션 정리를 분리한다

## 맥락

`withSuspendedTransaction`은 action 또는 commit 실패를 잡은 뒤 같은 coroutine
context에서 `rollbackAsync().await()`를 실행했다. caller가 이미 취소된 경우 rollback
대기가 즉시 취소되고 그 `CancellationException`이 주 예외를 덮을 수 있었다. 일반
rollback 실패도 `runCatching` 안에서 버려져 장애 진단 정보가 사라졌다.

## 결정

- action과 commit은 기존 caller context에서 실행해 정상적인 취소 전파를 유지한다.
- 실패 후 rollback만 `NonCancellable` context로 분리해 caller 취소와 관계없이 정리를
  시도한다.
- rollback 대기는 `withTimeout(5_000L)`로 제한해 응답 없는 Redis 때문에 무한히
  정지하지 않도록 한다.
- action, commit, caller cancellation을 항상 주 예외로 다시 던진다.
- rollback 실패와 timeout은 주 예외의 suppressed 예외로 연결한다. 동일한 예외
  인스턴스는 self-suppression을 피하기 위해 다시 추가하지 않는다.

## 결과

caller가 취소된 뒤에도 rollback future의 완료를 기다릴 수 있으며, 정리가 끝나면
원래 cancellation을 그대로 관찰한다. action 또는 commit이 실패한 경우에도 그 원인이
유지되고 rollback 장애 정보는 suppressed chain에서 확인할 수 있다. 정상 commit 경로와
public API signature는 변경하지 않았다.

## 검증

- 수정 전 회귀: rollback 실패가 suppressed에 남지 않아 1 test failed.
- contract test: action 실패, caller cancellation, commit 실패, rollback 실패/timeout
  4건 통과.
- 실제 Redis Testcontainers transaction/batch 검증: 3건 통과.
- `:bluetape4k-redisson:test`: 320 passing, failure/error/skipped 0.
- `:bluetape4k-redisson:detekt`: `BUILD SUCCESSFUL`; 변경 전부터 존재한
  `TooGenericExceptionCaught`를 포함한 baseline 진단만 보고됨.
- `git diff --check`: 통과.

## 향후 지침

coroutine transaction helper는 취소 가능한 업무 본문과 반드시 시도해야 하는 정리
단계를 분리한다. 정리는 `NonCancellable`만으로 무제한 보호하지 말고 timeout을 함께
설정한다. 정리 실패는 주 예외를 교체하지 말고 suppressed 또는 별도 diagnostic으로
남겨 원인과 cleanup 상태를 동시에 보존한다.
