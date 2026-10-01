# 공유 `SuspendLazy` timeout worker의 context 경계

관련 이슈: https://github.com/bluetape4k/bluetape4k-projects/issues/1808

## 맥락

`SuspendLazy.getUntil` 계열은 waiter에만 timeout을 적용한다. `suspendBlockingLazy`는 초기화를 lazy 소유 worker로 실행하며, 한 번 시작된 작업을 여러 waiter가 공유하고 첫 waiter가 timeout된 뒤에도 계속 실행한다.

## 실패한 가정과 근거

timeout worker가 첫 getter의 `CoroutineContext`를 가져도 이후 waiter에게 영향이 없다고 가정했다. 구현은 첫 호출자의 context에서 `Job`을 제거한 뒤 생성 시 설정한 context와 합쳐 worker를 만들었다. 그 결과 첫 호출자의 `ThreadContextElement`가 공유 worker에 남았고, 그 호출이 timeout되어도 초기화는 계속됐다. 새 회귀 테스트는 첫 waiter의 `ThreadLocal` 값이 worker에서 관측되는 것을 수정 전 실패로 확인했다.

## 결정

worker context는 lazy 생성 시 설정한 context에서만 구성한다. 설정 dispatcher는 `Dispatchers.IO`로 바꾸고, 설정한 `Job`은 lazy 소유 `SupervisorJob`의 parent로 연결한다. 첫 waiter와 이후 waiter의 context 요소는 공유 worker에 전달하지 않는다.

설정 context의 `ThreadContextElement`는 waiter 종료와 무관하게 초기화 작업의 전체 수명에 맞아야 한다. 요청별 MDC나 tenant 값이 필요한 작업은 요청 context를 캡처하는 공유 `SuspendLazy`로 표현하지 않는다. 직접 `invoke()`의 실행 방식과 `SuspendLazy.cancel()`의 소유권은 변경하지 않는다.

## 결과

실패 후 재시도는 매번 waiter가 아니라 생성 시 설정한 context를 사용한다. 첫 waiter가 timeout되어도 두 번째 waiter는 같은 초기화를 기다리며, worker는 설정 context를 유지한다. `suspendBlockingLazyIO`의 timeout worker는 호출자의 context 요소를 상속하지 않는다.

## 검증

- 수정 전 회귀 테스트: `timeout worker keeps configured context when the first waiter times out`에서 설정하지 않은 caller 값이 관측되어 실패했다.
- 수정 후 targeted 검증: context 공유 및 실패 후 재시도 테스트 2개 통과.
- `SuspendBlockingLazyTimeoutTest` 16개와 `:bluetape4k-coroutines:test` 685개 통과.
- `SuspendLazyBinaryCompatibilityTest` 1개 통과. 저장소에는 `checkBinaryCompatibility`/`checkProductionAbi` Gradle task와 ABI baseline이 없어 전체 ABI task 검증은 수행할 수 없다. 공개 JVM signature는 변경하지 않았다.
- `:bluetape4k-coroutines:detekt --rerun-tasks` 성공. 출력의 기존 진단은 변경 파일 밖에 있다.
- 독립 코드 리뷰 통과 ([리뷰 기록](../superpowers/reviews/2026-10-01-issue-1808-code-review.md)). exact-head PR CI는 PR 생성 뒤 확인한다.

## 향후 지침

- 여러 호출자가 공유하는 lazy 작업은 첫 waiter의 context에서 만들지 않는다. task/lazy 소유 context와 waiter의 deadline/cancellation/context를 분리한다.
- `ThreadContextElement` 전파를 검증할 때는 caller와 configured context에 서로 다른 `ThreadLocal` 값을 두고, worker가 설정값만 보는지 확인한다.
- 첫 waiter timeout, 후속 waiter의 공유, 초기화 횟수, 설정 `Job`의 정리를 latch로 동기화해 검증한다. 고정 sleep은 동시성 순서를 증명하지 않는다.
- 마지막 소스 수정 뒤에 compile/test를 다시 실행해 최종 증거를 갱신한다. 이전 성공 결과를 나중 편집에 그대로 적용하지 않는다.
