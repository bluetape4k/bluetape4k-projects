# Cache-Core Suspend 복구 계약 교훈

## 배경

`cache/cache-core` 모듈은 이전 bluetape4k 모듈 리뷰와 동일하게 6-Tier
P0/P1 gate를 적용해 리뷰했다. 실제 위험은 캐시 provider 연결 코드보다
coroutine lifecycle 계약에 있었다. 특히 suspend resilience decorator와
suspend memoizer에서 취소, 실패, 동시 호출이 만나는 지점이 취약했다.

이번 리뷰 범위는 다음 코드였다.

- `ResilientSuspendNearCacheDecorator`
- `InMemorySuspendMemoizer`
- `CaffeineSuspendMemoizer`
- `Cache2kSuspendMemoizer`
- `EhCacheSuspendMemoizer`
- public `SuspendMemoizer` 계약 KDoc
- `cache/cache-core/README.md`, `README.ko.md`

## 결정 및 발견

P0는 없었다.

P1은 2건이었다.

첫째, suspend retry wrapper가 `Exception`을 catch하는 외부 라이브러리에
의존하면 `CancellationException`도 retry될 수 있다. 이번 경우
resilience4j-kotlin의 `executeSuspendFunction` 경로가 문제였다.
coroutine 취소는 실패가 아니라 취소 신호이므로 retry 대상에서 명시적으로
제외해야 한다.

둘째, suspend memoizer의 in-flight map에 실패하거나 취소된 `Deferred`가
남으면 안 된다. 실패한 `Deferred`는 성공한 캐시 값이 아니다. 이것이 같은
key에 계속 남아 있으면 이후 호출이 새 계산을 시작하지 못하고 이전 실패만
반복해서 재사용할 수 있다.

`runCatching`도 같은 이유로 조심해야 한다. `runCatching`은
`CancellationException`을 잡기 때문에 suspend `close()`에서 일반 예외를
삼키려면 `try/catch`를 직접 쓰고 cancellation은 먼저 재전파해야 한다.

## 결과

이번 cache-core 수정으로 다음 계약을 코드와 테스트에 반영했다.

- suspend resilience wrapper는 `CancellationException`을 retry하지 않는다.
- suspend lifecycle 메서드는 일반 close 실패를 기록하고 무시하더라도
  cancellation은 반드시 다시 던진다.
- suspend memoizer는 실패하거나 취소된 in-flight `Deferred`를 제거한다.
- transient failure 이후 같은 key 호출은 새 계산으로 복구되고, 성공 결과는
  다시 정상 캐시된다.
- public KDoc과 README 예제에 evaluator 실패와 coroutine 취소가 성공 값처럼
  memoize되지 않는다는 계약을 명시했다.

Caffeine 테스트에서는 추가 교훈도 있었다. 동시 복구를 검증할 때 provider
executor 스케줄링에 따라 계산 횟수가 약간 흔들릴 수 있다. 이런 테스트는
정확한 실행 횟수보다 사용자에게 보이는 계약과 최종 캐시 상태를 먼저
검증해야 한다.

## 검증

이번 작업에서 확인한 로컬 증거는 다음과 같다.

- Caffeine 복구 edge test:
  `./gradlew :bluetape4k-cache-core:test --tests '*CaffeineSuspendMemoizerTest'`
- cache-core 전체 테스트:
  `./gradlew :bluetape4k-cache-core:test`
- 전체 결과: `444 passing`
- 커밋 전 `git diff --check` 통과
- PR #400 본문에 최종 gate 결과 `P0=0`, `P1=0` 기록

## 향후 지침

- suspend retry/circuit-breaker wrapper를 리뷰할 때는
  `CancellationException`이 retry되지 않고 즉시 전파되는 테스트를 반드시
  추가한다.
- suspend lifecycle 코드에서 `runCatching`을 쓰지 않는다. 꼭 써야 한다면
  cancellation 재전파가 먼저 보장되는 구조인지 별도 테스트로 증명한다.
- in-flight memoization에서는 실패하거나 취소된 `Deferred`를 `finally`에서
  제거한다. transient failure가 특정 key를 영구적으로 오염시키면 안 된다.
- `SuspendedJobTester`로 동시성 테스트를 추가할 때는 최종 상태와 public
  contract를 우선 검증한다. scheduler 경계를 완전히 소유하지 않는 구현에서
  정확한 evaluator 실행 횟수를 강하게 단정하지 않는다.
- public 계약이 바뀌면 코드 수정과 같은 PR에서 KDoc, README.md,
  README.ko.md를 함께 갱신한다.
