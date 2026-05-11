# bluetape4k-coroutines 6-Tier Review Gate

**날짜**: 2026-05-11
**모듈**: `bluetape4k-coroutines`
**결론**: PASS
**P0/P1**: 0

## Tier 1. Public API / Contract

- `DeferredSupport.awaitAnyAndCancelOthers`는 child `Deferred` 취소와 caller coroutine 취소를 분리한다.
- Subject public 종료 API(`emitError`, `complete`)는 caller cancellation을 보존한다.
- P0/P1: 없음

## Tier 2. Coroutine / Concurrency

- suspend 호출을 감싼 `runCatching` 경로를 재검토했다.
- `BehaviorSubject.complete`, `PublishSubject.emitError`, `PublishSubject.complete`에서 `CancellationException` 발생 시 `currentCoroutineContext().ensureActive()`로 부모 취소를 다시 전파한다.
- 개별 collector 취소는 기존 Subject semantics대로 전체 caller 취소로 승격하지 않는다.
- P0/P1: 없음

## Tier 3. Validation / Failure Semantics

- 빈 입력 검증은 기존 `requireNotEmpty` 테스트가 유지된다.
- winner 실패/취소 시 loser cancellation contract는 기존 tests와 새 cancellation tests로 유지된다.
- P0/P1: 없음

## Tier 4. Tests / Edge Cases

- 추가 edge tests:
  - busy collector 상태에서 `BehaviorSubject.complete` timeout cancellation 전파
  - busy collector 상태에서 `PublishSubject.complete` timeout cancellation 전파
  - busy collector 상태에서 `PublishSubject.emitError` timeout cancellation 전파
- Verification:
  - `./gradlew :bluetape4k-coroutines:test --tests "io.bluetape4k.coroutines.flow.extensions.subject.SubjectCancellationTest"`: PASS, 9 tests
  - `./gradlew :bluetape4k-coroutines:test`: PASS, 559 tests
- P0/P1: 없음

## Tier 5. Documentation / KDoc

- `README.md`와 `README.ko.md`에 Deferred cancellation contract와 Subject hot-flow usage/termination contract를 동기화했다.
- 기존 public API KDoc는 계약과 예제를 이미 포함하며, 이번 변경 API의 cancellation semantics와 README가 일치한다.
- P0/P1: 없음

## Tier 6. Maintainability / Patterns

- 새 dependency 또는 abstraction은 추가하지 않았다.
- 기존 `MulticastSubject`의 cancellation 패턴과 맞춰 `currentCoroutineContext().ensureActive()`를 사용했다.
- 변경 범위는 coroutines cancellation contract, tests, docs에 제한된다.
- P0/P1: 없음
