# Issue #1613: CompletableFuture 반환 전 blocking 호출을 executor 경계 안에 둔다

## 맥락

`withWorkStealingPool(parallelism, tasks)`는 호출 스레드에서 `invokeAll()`을 직접
실행한 뒤에야 `CompletableFuture`를 반환했다. 따라서 반환 타입은 비동기였지만 실제
호출자는 모든 task 완료까지 차단되었고, 반환 future를 얻기 전에는 취소할 수도 없었다.

## 결정

- `tasks`를 호출 시점에 `Callable` 목록으로 snapshot해 입력 순서와 collection 변경
  경합을 보존한다.
- `invokeAll()`과 결과 `Future.get()` 집계를 같은 WorkStealingPool의
  `CompletableFuture.supplyAsync` 안에서 실행한다.
- 반환되는 원본 future에 lifecycle callback을 연결한다. 정상·실패 완료는
  `shutdown()`, cancellation은 `shutdownNow()`로 실행 중 task에 취소를 전파한다.
- `Future.get()`의 `ExecutionException` 원인을 다시 던져 기존 task 예외 전파 형태를
  유지한다. 호출별 executor는 `parallelism`과 소유 lifecycle을 보존하기 위해 유지한다.

## 결과

호출자는 task가 실행 중이어도 아직 완료되지 않은 future를 즉시 받는다. 결과 순서는
입력 순서를 따르고, task 예외는 future의 실패로 전파되며, 실행 중 future 취소는
worker task interrupt와 executor 종료를 유발한다.

## 검증

- 수정 전 latch 회귀: 호출을 감싼 future가 task release 전까지 완료되지 않아 실패.
- 수정 후 `ExecutorSupportTest`: 7 passing.
- `:bluetape4k-core:test`: 1648 passing, `BUILD SUCCESSFUL`.
- `:bluetape4k-core:detekt`: 기존 core 전반의 사전 존재 위반으로 실패했으며
  `ExecutorSupport.kt` 신규 진단은 없었다.

## 향후 지침

비동기 반환 API에서 내부 `invokeAll`, `Future.get`, `join`을 호출자 경계에 두지 않는다.
반환 future를 취소할 수 있는 시점이 작업 실행보다 빠르므로, cancellation callback이
실행 중 작업과 executor lifecycle까지 닫는지 latch 기반 테스트로 고정한다.
