# Issue #1616: owner interrupt와 subtask failure를 같은 실패 슬롯에 넣지 않는다

## 맥락

JDK25 `StructuredTaskScope` adapter는 JDK21의 직접 위임 방식과 달리
`FailedException`을 저장했다가 기존 bluetape4k API의 `throwIfFailed` 또는 `result`
mapper 경계에서 처리한다. 이때 `runCatching`이나 `catch (Throwable)`로 `join()` 전체를
감싸면 owner thread의 `InterruptedException`까지 subtask 실패로 분류된다. 그 결과
`withAll.join()`은 취소 신호를 즉시 던지지 않고, `withAny.result()`는 cancellation을
업무 실패 mapper로 변환할 수 있었다.

## 결정

- JDK25 `withAll`의 `join`과 `joinUntil`은 `InterruptedException`을 failure slot에
  저장하지 않고 즉시 다시 던진다.
- `withAll`은 `StructuredTaskScope.FailedException`만 잡아 cause를 기존 방식대로
  `throwIfFailed`에 보관한다.
- JDK25 `withAny`의 `join`과 `result` 직접 호출은 하나의 `joinResult` 경계를 공유한다.
- `joinResult`는 owner `InterruptedException`을 즉시 던지고, subtask
  `FailedException`만 `Result.failure`로 저장해 mapper에 전달한다.
- timeout scheduler가 만든 interrupt를 `TimeoutException`으로 변환하는 기존
  `interruptJoinUntil` 계약은 유지한다.

## 결과

pre-interrupt와 외부 interrupt는 `withAll.join`, `withAll.joinUntil`,
`withAny.join().result`, `withAny.result` 직접 호출에서 raw `InterruptedException`으로
관찰된다. mapper는 owner interrupt에 호출되지 않는다. subtask 실패는 계속 cause가
unwrap되고 fail-fast 또는 mapper 계약에 따라 처리된다. public API와 JDK21 provider
구현은 변경하지 않았다.

## 검증

- 수정 전 회귀: pre-interrupted `withAll.join()`이 예외 없이 반환해 1 test failed.
- `Jdk25StructuredTaskScopeProviderExtTest`: 27 passing. pre/external interrupt,
  `join`/`result`/`joinUntil`, subtask failure와 timeout을 포함한다.
- `:bluetape4k-virtualthread-jdk25:test`: 43 passing, failure/error/skipped 0.
- `:bluetape4k-virtualthread-jdk21:test`: 26 passing, failure/error/skipped 0.
- `:bluetape4k-virtualthread-jdk25:detekt`: `BUILD SUCCESSFUL`; 변경 경로의 generic
  catch 진단은 제거됐고 기존 baseline 진단만 남았다.
- `git diff --check`: 통과.

## 향후 지침

structured concurrency adapter에서 owner control-flow 예외와 subtask 결과 예외를 먼저
분류한다. `InterruptedException`은 저장, 래핑, mapper 변환 전에 즉시 전파한다.
`Result`나 failure slot에는 API가 명시한 subtask failure만 넣고, timeout처럼 별도
control-flow 의미가 있는 예외는 해당 public 경계에서 보존한다.
