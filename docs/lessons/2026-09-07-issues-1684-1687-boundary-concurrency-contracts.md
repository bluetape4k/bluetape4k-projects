# Issues #1684–#1687: 경계 상태는 최종 효과까지 검증한다

## 맥락

2.1.0 개발선의 7-Tier 리뷰에서 기존 테스트가 모두 통과했지만 네 가지 경계 결함이
남아 있었다. caller가 취소한 promise와 내부 stage의 lifecycle, 단위 환산 뒤의 숫자
범위, coroutine cleanup 실패와 cancellation의 우선순위, Redis transaction과 공유
connection의 command stream이 각각 분리돼 검증되고 있었다.

## 결정

- `ConcurrentReducer`는 수락한 promise의 cancellation을 내부 job 상태 전이와 source
  stage 취소 요청에 연결한다. 원본 취소가 보장되지 않는 `CompletionStage`는 terminal
  상태까지 active permit을 유지하고, 기존 CAS와 admission lock으로 registry와 permit을
  exactly-once 정리한다.
- `quarterPeriod()`는 입력 타입의 범위가 아니라 월 환산 결과의 범위를 검사한다.
  `Int`와 `Long` overload는 같은 exact arithmetic 계약을 사용한다.
- `closeSuspending()`은 `NonCancellable` cleanup의 일반 예외를 값으로 회수한 뒤 caller
  cancellation을 먼저 확인한다. cancellation은 primary, cleanup 실패는 suppressed로
  남기며 `Error`는 즉시 전파한다.
- Redis transaction 상태는 connection 전체에 적용되므로 `LettuceMap`의 모든 sync/async
  command dispatch, `LettuceSuspendMap`의 coroutine dispatch, lock-owned transaction을
  같은 connection gate로 조정한다. sync 호출은 caller thread에서 gate를 기다리지만,
  async/suspend 호출은 경합 시 virtual thread에서 대기해 Netty event-loop를 막지 않는다.
  distributed-lock 대기와 사용자 callback은 gate 밖에서 실행한다. raw connection과 다른
  wrapper는 gate를 우회하므로 transaction과의 동시 사용을 지원하지 않는다.

## 결과

active cancellation 뒤 cancellable source는 취소되고 다음 reducer 작업이 실행된다. 취소를
전파하지 않는 source는 실제 terminal 전까지 permit을 유지해 동시성 상한을 넘지 않는다.
분기 환산은 양수·음수 양쪽 경계 밖을 명시적으로 거부한다. 취소된 coroutine의 pool
cleanup이 실패해도 cancellation 의미와 cleanup 진단 정보가 함께 보존된다. 같은 Lettuce
connection을 여러 thread가 사용하는 반복 `EntryProcessor` 호출은 transaction response와
lock command response를 서로 다른 output decoder로 읽지 않는다.

## 검증

- #1684: 수정 전 source 미취소와 active slot 점유로 10초 timeout, 수정 후 회귀 테스트 통과.
  취소 전파를 보장하지 않는 stage는 terminal까지 permit을 유지하고 close에서 정리됨을 함께 검증.
- #1685: 수정 전 월 환산 overflow 입력이 예외 없이 wrap-around, 수정 후 양수·음수 경계 통과.
- #1686: 수정 전 cleanup 예외가 cancellation을 대체, 수정 후 cancellation identity와
  두 cleanup 실패의 suppressed chain 검증 통과.
- #1687: 같은 cache instance에서 12 workers × 20 rounds를 실행하면 수정 전
  `StatusOutput does not support set(long)` 재현, connection 직렬화 후 같은 stress와
  두 cache instance 회귀 테스트 통과. 동일 connection의 일반 sync/async command 및
  `LettuceSuspendMap` command와 transaction을 섞은 회귀 테스트도 통과. sync 응답 대기 중
  event-loop 역할 callback의 async 호출이 500ms 안에 pending future를 반환하는 liveness
  테스트는 수정 전 실패하고 비차단 gate 적용 후 통과. gate 대기 중 취소된 async 요청이
  lock 해제 뒤 뒤늦게 dispatch되는 회귀도 수정 전 재현하고 취소 전파 후 통과.
- 전체 모듈 재실행: core 1,683건, R2DBC 220건, Lettuce 940건, cache-lettuce 462건,
  합계 3,305건 통과. Lettuce 첫 전체 실행의 기존 write-behind 500ms timeout 1건은
  단독 재실행과 두 번째 전체 실행에서 통과했다.
- 네 모듈 Detekt task는 exit 0으로 완료됐다. 기존 대형 파일·magic number baseline
  진단은 남아 있으며 exact-head CI와 Full Nightly는 push 전이므로 아직 검증하지 않았다.

## 향후 지침

비동기 API는 반환 객체의 terminal 상태뿐 아니라 upstream 작업, permit, registry까지
최종 효과가 수렴하는지 확인한다. 숫자 변환은 입력 검증 뒤의 산술도 exact해야 한다.
취소 불가능한 cleanup은 실패를 버리지 않되 원래 cancellation을 교체하지 않는다.
Redis의 `WATCH/MULTI/EXEC`처럼 connection-scoped 상태를 사용하는 기능은 logical lock만
검증하지 말고 같은 connection의 일반 sync/async command를 포함한 dispatch 교차 가능성을
stress 테스트로 고정한다. sync transaction을 직렬화하더라도 event-loop callback은 lock을
기다리지 않고 pending future를 반환해야 하며, sync API 자체는 event-loop에서 호출하지 않는다.
