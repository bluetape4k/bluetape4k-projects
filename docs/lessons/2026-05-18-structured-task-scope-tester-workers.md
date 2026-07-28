# StructuredTaskScopeTester workers() Concurrency Control

**날짜**: 2026-05-18
**이슈**: #522
**브랜치**: fix/structured-task-scope-workers

## 근본 원인

`StructuredTaskScopeTester`는 `StressTester`(rounds only)를 구현했고 `workers()` method가 없었다.
Fork된 모든 virtual thread가 concurrency cap 없이 실행되어 `workers(n)`이 조용히 효과가 없었다.

## 결정

- `WorkerStressTester<StructuredTaskScopeTester>`로 upgrade해 `workers()` contract를 얻는다.
- 내부 `Semaphore(workerSize)`를 추가하고 각 forked task **안에서** acquire한다(fork 전이 아님).
  - Fork 내부 acquire는 모든 task를 즉시 submit하게 하고, semaphore가 실제 실행을 gate한다.
  - `finally { semaphore.release() }`는 exception 상황에서도 release를 보장한다.
  - Scope cancellation 시 `semaphore.acquire()`에서 blocked 된 thread는 permit을 얻기 전에
    `InterruptedException`을 받으므로 permit leak이 없다.
- 기본값: `Runtime.getRuntime().availableProcessors() * 2`(issue acceptance criteria와 동일).
  - 첫 선택은 `Systemx.availableProcessors`였지만 `bluetape4k-core`는 `bluetape4k-junit5`의
    dependency가 아니다. Dependency를 추가하지 않고 `Runtime`을 직접 사용한다.

## 검증

- `StructuredTaskScopeTesterTest`의 10 tests 통과(failure 0).
- `StressTesterContractTest`의 3 tests 통과(`configureWorkerTester` 사용으로 갱신).

## 리뷰 지적 해결

| 지적 | 수정 |
|---------|-----|
| Non-atomic peak-concurrency measurement(`incrementAndGet` + `getAndUpdate`) | Block 내부 `Semaphore.tryAcquire()`로 교체해 limit 초과 시 atomic하게 실패 |
| Interface upgrade 후에도 `StressTesterContractTest`가 `configureRoundsTester` 사용 | `configureWorkerTester(workers=2, rounds=4)`로 갱신 |

## 향후 가이드

- Tester가 `workers()`를 얻으면 contract test도 항상 `configureWorkerTester`로 upgrade한다.
- Concurrency-limit test에서는 atomic counter + peak-update pair보다 `Semaphore.tryAcquire()`를
  선호한다. 두 단계 pattern에는 semaphore bug를 숨길 수 있는 TOCTOU gap이 있다.
- `bluetape4k-junit5`에서 `bluetape4k-core`에 의존하지 않는다. JDK stdlib equivalent를 사용한다.
