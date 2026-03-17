# Resilience4j Retry Refresh Plan

## Goal

`bluetape4k-resilience4j`에서 Retry 엔진은 `resilience4j`를 그대로 유지하고, 다음 두 문제를 해결한다.

- backoff 설정을 `debop4k` 수준의 조합성으로 보강한다.
- `RetryExtensions`의 `Executors.newSingleThreadScheduledExecutor()` 기본 생성/자동 종료 패턴을 제거해 scheduler 수명 관리와 성능 문제를 개선한다.

## Background

- 현재 coroutine 경로는 `Retry.executeSuspendFunction` 기반이라 방향은 맞다. `/Users/debop/work/bluetape4k/bluetape4k-projects/infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/retry/RetryCoroutines.kt:25`
- `SuspendDecorators`도 `Retry.decorateSuspendFunction`을 사용하므로 Retry 엔진 교체는 필요 없다. `/Users/debop/work/bluetape4k/bluetape4k-projects/infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SuspendDecorators.kt:244`
- 반면 `CompletionStage`/`CompletableFuture` helper는 기본값으로 매 호출마다 `newSingleThreadScheduledExecutor()`를 만들고, 완료 시 shutdown 한다. `/Users/debop/work/bluetape4k/bluetape4k-projects/infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/retry/RetryExtensions.kt:184`, `/Users/debop/work/bluetape4k/bluetape4k-projects/infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/retry/RetryExtensions.kt:216`, `/Users/debop/work/bluetape4k/bluetape4k-projects/infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/retry/RetryExtensions.kt:264`
- 실사용 모듈은 이미 `RetryConfig` + `IntervalFunction` 조합을 직접 반복 작성하고 있다. `/Users/debop/work/bluetape4k/bluetape4k-projects/infra/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/ResilientSuspendNearCache.kt:85`

## Requirements Summary

- Retry 엔진은 `resilience4j`로 유지한다.
- backoff 관련 DSL/확장으로 다음 기능을 쉽게 구성할 수 있어야 한다.
  - fixed delay
  - exponential backoff
  - first retry no delay
  - min/max clamp
  - uniform jitter
  - proportional jitter
- async helper는 기본 scheduler를 내부에서 생성/종료하지 않아야 한다.
- coroutine 경로에서는 cancellation retry를 명시적으로 다뤄야 한다.
- 기존 공개 API는 가능한 한 최소 diff로 보강하고, 필요 시 deprecated 경로를 둔다.

## Non-goals

- Retry 엔진을 자체 구현으로 교체
- CircuitBreaker/RateLimiter/Bulkhead의 동작 재설계
- 대규모 패키지 구조 변경

## Acceptance Criteria

- `RetryExtensions`에서 기본값으로 `Executors.newSingleThreadScheduledExecutor()`를 생성하는 API가 제거되거나 deprecated 처리된다.
- 호출자는 명시적인 `ScheduledExecutorService` 또는 공유 scheduler provider를 사용한다.
- backoff DSL 또는 factory가 추가되어 `RetryConfig` 구성 중복을 줄인다.
- jitter/clamp/first-no-delay 조합을 검증하는 테스트가 추가된다.
- coroutine retry 경로에서 `CancellationException`은 재시도 대상에서 제외하는 정책 또는 helper가 문서화되고 테스트된다.
- README 예제가 새 API와 일치한다.

## Viable Options

### Option A. Shared Scheduler Provider + Backoff DSL

- 접근
  - `infra/resilience4j` 내부에 공유 `ScheduledExecutorService` provider를 두고 async helper 기본값으로 사용한다.
  - `RetryConfig` builder helper 또는 Kotlin DSL을 추가한다.
- 장점
  - 기존 API 형태를 크게 깨지 않는다.
  - 사용자 편의성과 운영 효율을 둘 다 개선한다.
  - cache/vertx/spring 쪽 반복 코드를 줄일 수 있다.
- 단점
  - shared scheduler lifecycle 정책을 문서화해야 한다.
  - 테스트에서 전역 상태 정리가 필요할 수 있다.

### Option B. No Default Scheduler, Explicit Injection Only

- 접근
  - async helper에서 scheduler 파라미터를 필수로 바꾼다.
  - 내부 기본값 제공을 중단한다.
- 장점
  - lifecycle ownership이 명확하다.
  - 숨은 thread 생성 비용이 사라진다.
- 단점
  - API 변경 폭이 커진다.
  - 사용처 수정량이 늘어난다.

### Recommendation

- 1차는 Option A.
- 단, 내부 구현은 “공유 scheduler + 명시적 override 허용”으로 두고, 새 helper 이름 또는 deprecated 경로로 API migration을 유도한다.

## Decision Drivers

- coroutine 친화성 유지
- thread/scheduler lifecycle 명확화
- 최소 diff로 운영 안정성 개선

## Implementation Steps

1. 현재 async helper 호출처를 전수 조사한다.
   - 대상: `RetryExtensions.kt`를 사용하는 `CompletionStage`/`CompletableFuture` 기반 코드
   - 확인 범위: `infra/resilience4j`, `vertx/resilience4j`, 기타 직접 호출 모듈

2. scheduler lifecycle 정책을 정한다.
   - 후보 1: `internal object RetrySchedulers`에 공유 scheduler 1개
   - 후보 2: `ScheduledExecutorService`를 필수 주입하고 기존 편의 함수는 deprecated
   - 권고: 내부 shared scheduler + 명시적 주입 overload 병행

3. `retry/backoff` 보조 API를 설계한다.
   - 예시
     - `retryConfig { exponentialBackoff(...); maxAttempts(3) }`
     - `IntervalFunction.withMinDelay(...)`
     - `IntervalFunction.withMaxDelay(...)`
     - `IntervalFunction.withUniformJitter(...)`
     - `IntervalFunction.withProportionalJitter(...)`
     - `IntervalFunction.firstRetryNoDelay()`
   - 기존 `debop4k`의 아이디어만 차용하고 구현은 `Duration`/`IntervalFunction` 기준으로 재구성

4. cancellation 정책을 helper에 반영한다.
   - suspend helper에서 `CancellationException`은 retry 대상에서 제외하는 기본 helper 제공 검토
   - 필요 시 `RetryConfig.retryOnException` 예제/팩토리로 표준화

5. 사용처 리팩터링을 적용한다.
   - `infra/cache-core`, `infra/cache-hazelcast`, `infra/cache-lettuce`, `infra/cache-redisson`의 반복 `RetryConfig.custom + IntervalFunction` 구문을 새 helper로 축약할 수 있는지 검토

6. 테스트를 보강한다.
   - `RetryExtensionsTest`
   - `RetryCoroutinesTest`
   - 필요 시 scheduler reuse/lifecycle 테스트 추가

7. README를 갱신한다.
   - Retry 생성 예제
   - backoff DSL 예제
   - async helper의 scheduler ownership 규칙

## File Targets

- `/Users/debop/work/bluetape4k/bluetape4k-projects/infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/retry/RetryExtensions.kt`
- `/Users/debop/work/bluetape4k/bluetape4k-projects/infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/retry/RetryCoroutines.kt`
- `/Users/debop/work/bluetape4k/bluetape4k-projects/infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SuspendDecorators.kt`
- `/Users/debop/work/bluetape4k/bluetape4k-projects/infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/retry/RetryExtensionsTest.kt`
- `/Users/debop/work/bluetape4k/bluetape4k-projects/infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/retry/RetryCoroutinesTest.kt`
- `/Users/debop/work/bluetape4k/bluetape4k-projects/infra/resilience4j/README.md`

## Risks and Mitigations

- Risk: shared scheduler가 테스트 격리를 해칠 수 있음
  - Mitigation: package-internal override 또는 test hook 제공

- Risk: async helper API 변경이 호출처에 영향
  - Mitigation: 기존 함수는 deprecated 후 새 함수로 위임

- Risk: jitter/clamp 구현이 기대와 다를 수 있음
  - Mitigation: deterministic 테스트와 경계값 테스트 추가

- Risk: cancellation retry가 섞이면 coroutine 취소 계약이 깨질 수 있음
  - Mitigation: `CancellationException` 비재시도 정책을 기본값으로 문서화하고 테스트

## Verification Steps

1. `infra/resilience4j` 테스트 실행
2. backoff helper 단위 테스트 확인
3. cache 계열 최소 1개 모듈에서 새 DSL 적용 후 회귀 테스트 확인
4. README 예제가 컴파일 가능한지 검토

## ADR

### Decision

Retry 엔진은 `resilience4j`를 유지하고, `bluetape4k`는 backoff ergonomics와 scheduler lifecycle만 보강한다.

### Drivers

- coroutine/CompletionStage 지원을 유지해야 한다.
- thread 생성/종료 비용과 ownership ambiguity를 제거해야 한다.
- 기존 사용처를 크게 깨지 않아야 한다.

### Alternatives Considered

- `debop4k Retry` 재이관
- `resilience4j` 유지 + shared scheduler + backoff DSL
- `resilience4j` 유지 + explicit scheduler only

### Why Chosen

`debop4k Retry`는 Kovenant 기반이라 현재 스택과 맞지 않고, 운영 기능도 약하다. `resilience4j`는 이미 metrics/event ecosystem과 coroutine integration을 갖추고 있어, 보강 포인트만 손보는 것이 투자 대비 효과가 가장 크다.

### Consequences

- 내부 유틸리티는 늘어나지만 외부 의존성은 증가하지 않는다.
- Retry 사용 예제가 더 일관돼진다.
- scheduler ownership이 명시되어 운영 안정성이 좋아진다.

### Follow-ups

- `vertx/resilience4j`에도 동일한 backoff helper 적용 여부 검토
- Micrometer 태깅/registry 예제를 Retry README에 추가
