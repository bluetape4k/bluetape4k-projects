# Redis Synchronizer의 세대 결합 정리 경계

## Context

Issue #1080의 Lettuce `CountDownLatch`는 sync/async/suspend 대기를 Redis waiter 집합에 등록하고,
완료·취소·timeout 시 등록을 정리한다. 동일한 latch 이름을 삭제한 뒤 다시 만들 수 있으므로
request id만으로 waiter를 식별하면 이전 세대의 늦은 cleanup이 새 세대 waiter를 제거할 수 있다.

## Decision

- Waiter member는 `generation|requestId`로 만들고, 등록·조회·해제 모두 generation을 함께 검증한다.
- Redis `TIME`을 기준으로 waiter 만료 시각을 기록하고, 등록·조회·삭제 전에 bounded stale cleanup을 수행한다.
- `maxWaiters`로 활성 waiter 수를 제한하며 capacity 초과를 공개 결과로 반환한다.
- Async 결과는 durable unregister가 끝난 뒤 terminal 상태가 되도록 한다.
- Suspend registration 중 cancellation은 `NonCancellable` cleanup 후 반드시 다시 던진다.
- Polling delay는 최소 1ms로 보정해 sub-millisecond 설정이 hot loop가 되지 않게 한다.

## Unexpected Failure / Review Miss

초기 수정은 stale waiter와 최대 수를 추가했지만 unregister가 request id만 사용했다. 보안 재검토에서
이전 세대 cleanup이 같은 request id의 현재 waiter를 삭제할 수 있음이 발견됐다. 성능/안정성 재검토에서는
registration cancellation을 일반 예외처럼 삼키는 경로와 1ms 미만 delay가 0ms 반복 실행으로 변하는
경로가 함께 발견됐다. 세대 결합 해제, cancellation 재전파, 최소 delay를 각각 회귀 테스트로 고정했다.

## Outcome

이전 세대 cleanup은 현재 세대 waiter를 건드리지 못하고, 취소된 coroutine은 호출자에게 취소를 보존한다.
Waiter 저장소와 polling 작업은 모두 명시적으로 bounded되며 object close가 pending async wait를 종료한다.

## Future Guard

Latch generation, waiter member 형식, cancellation adapter, polling delay를 바꾸면 generation 재생성 경쟁,
registration cancellation, stale cleanup, capacity, close 시 다중 waiter 종료 테스트를 함께 실행한다.
