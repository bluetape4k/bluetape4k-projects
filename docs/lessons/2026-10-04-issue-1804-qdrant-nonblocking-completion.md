# 완료 콜백에서 blocking 대기를 하지 않는다

## 맥락

Qdrant coroutine adapter가 Guava `ListenableFuture` 완료를
`MoreExecutors.directExecutor()`로 받으면서 `runBlocking(Dispatchers.IO) { get() }`를
호출했다. Future는 이미 완료됐지만, `runBlocking`은 결과 조회를 IO dispatcher에
보내고 그 작업이 끝날 때까지 direct-executor 호출자를 동기적으로 붙잡았다.

## 결정과 발견

- direct executor는 listener를 별도 스레드로 옮기지 않고 Future를 완료한 호출 경로에서
  실행할 수 있다. 그 listener에서 `get()`, `join()`, `runBlocking` 또는 동등한 blocking
  대기를 하면 외부 호출자의 완료까지 지연될 수 있다.
- Guava `Futures.addCallback`으로 성공 값이나 원인 예외를 바로 continuation에 전달한다.
  continuation이 원래 coroutine context의 dispatch를 담당하므로 callback에서 dispatcher로
  다시 옮긴 뒤 기다리지 않는다.
- caller cancellation은 원본 Future의 `cancel(true)`로 전달하고, Future cancellation은
  coroutine cancellation으로 전달한다. 실패는 Guava callback이 제공하는 원인 예외를
  그대로 전달해 cancellation과 timeout의 의미를 보존한다.
- 회귀 테스트는 IO worker를 모두 점유한 상태에서 Future 완료 호출이 제한 시간 안에
  반환하는지 확인한다. 이 조건에서 기존 `runBlocking(Dispatchers.IO)` 경로는 RED가 되고,
  callback 직접 전달은 GREEN이 된다.

## 향후 지침

`directExecutor()` 또는 동기 실행이 가능한 callback을 추가할 때 listener의 전체 경로를
검토한다. 완료 신호를 받은 뒤 결과를 읽기 위한 blocking 대기를 두지 말고 callback API로
값·실패를 전달한다. coroutine bridge에서는 양방향 cancellation, 원인 예외 identity,
동시 완료와 취소, callback 호출자 liveness를 회귀 테스트로 고정한다. 임의 sleep보다
latch와 bounded wait를 사용하고, 점유한 worker와 latch는 `finally`에서 해제한다.

## 검증

- 수정 전 포화 IO 상태의 completion-thread 회귀 테스트는 2초 이내 반환 assertion에서
  실패했다.
- 수정 후 `QdrantCoroutinesTest` 17개와
  `:bluetape4k-qdrant:test -PexcludeIntegrationTests=true` 17개가 통과했다.
- `:bluetape4k-qdrant:detekt`와 `git diff --check`가 통과했다.
