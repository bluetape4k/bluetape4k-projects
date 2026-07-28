# 이슈 #831 Ktor TimeLimiter failure metric 교훈 (2026-06-24)

관련 이슈: #831
module: `:bluetape4k-ktor-resilience4j`

## L1: TimeLimiter cancellation과 failure accounting은 catch path를 분리해야 한다

### 문제

`withTimeLimiterPreservingStatusMapping()`은 실제 timeout을 Resilience4j
`TimeoutException`으로 변환하고 success event를 기록했지만, 일반 handler failure는
`timeLimiter.onError(e)` 호출 없이 빠져나갔다.

그 결과 handler가 일반 non-cancellation exception으로 실패하면 TimeLimiter-protected Ktor
invocation이 Resilience4j TimeLimiter event에서 보이지 않았다.

### 교훈

coroutine TimeLimiter wrapper에서는 catch order를 명시적으로 유지한다.

1. `TimeoutCancellationException`을 TimeLimiter timeout exception으로 변환하고 `onError`로
   기록한다.
2. 다른 `CancellationException` 값은 policy failure로 기록하지 않고 rethrow한다.
3. 일반 non-cancellation failure는 rethrow 전에 `onError`로 기록한다.

TimeLimiter public API는 이 module의 verification surface로 event를 노출하므로, regression
test는 TimeLimiter가 노출하지 않는 CircuitBreaker-style metric 대신
`eventPublisher.onError`를 assert해야 한다.

## 증거

- RED: `time limiter records ordinary handler failures`는 TimeLimiter error event count가
  0에 머물러 실패했다.
- GREEN: `KtorResilienceSupportTest`가 7 tests로 통과했다.
- module verification:
  `./gradlew :bluetape4k-ktor-resilience4j:compileKotlin :bluetape4k-ktor-resilience4j:compileTestKotlin :bluetape4k-ktor-resilience4j:test --no-build-cache`
  통과했다.

## Local review evidence

원래 문서에는 `fix/ktor-timelimiter-failures-831` branch의 review note fragment가 함께
남아 있었다. 그 기록의 의미는 다음과 같다.

- P0/P1 findings: 0.
- catch ordering은 `TimeoutCancellationException`을 generic `CancellationException`보다
  먼저 처리해 timeout mapping을 보존한다.
- external coroutine cancellation은 policy failure로 기록하지 않는다.
- 일반 non-cancellation failure는 rethrow 전에 `timeLimiter.onError(e)`를 호출한다.
- test는 기존 `runSuspendIO`와 bluetape4k assertion을 사용했다.
- bug가 deterministic exception-boundary regression이므로 ad hoc concurrency stress helper는
  필요하지 않았다.
- full repository build는 실행하지 않았고, 변경은 `ktor/resilience4j` TimeLimiter exception
  boundary에 격리되어 module compile/test로 검증했다.
