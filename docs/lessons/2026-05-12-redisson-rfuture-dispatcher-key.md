# Redisson RFuture Coroutine Dispatcher Key Lessons

## Context

daily bug scan에서 최근 `infra/redisson` 변경을 6-Tier로 재검토했다. `RFutureSupport.awaitAll()`은 현재 coroutine dispatcher를 `Executor`로 재사용한다는 계약을 문서화했지만, 구현이 coroutine context 조회 key를 잘못 사용하고 있었다.

## Decision or Finding

`currentCoroutineContext()[CoroutineDispatcher]`는 Kotlin coroutine context 조회 계약에 맞지 않는다. dispatcher는 `ContinuationInterceptor` key로 저장되므로, 현재 dispatcher를 재사용하려면 `currentCoroutineContext()[ContinuationInterceptor] as? CoroutineDispatcher` 형태로 꺼내야 한다.

이 문제는 런타임 flaky가 아니라 compile-time 회귀에 가깝다. RFuture adapter처럼 작은 public coroutine helper도 targeted compile test를 6-Tier review의 필수 증거로 다뤄야 한다.

## Outcome

- `RFutureSupport.awaitAll()`이 `ContinuationInterceptor` key로 현재 dispatcher를 조회하도록 고쳤다.
- `withContext(Dispatchers.IO)` 안에서도 `awaitAll()`이 입력 순서를 보존하는 regression test를 추가했다.
- README.md/README.ko.md의 RFuture coroutine 변환 예시에 현재 dispatcher 재개 계약을 보강했다.

## Verification

- GitHub workflow YAML parse 통과.
- `git diff --check` 통과.
- Gradle wrapper distribution은 sandbox 내부 `GRADLE_USER_HOME`에서 인식시켰지만, Gradle 시작 단계에서 `FileLockContentionHandler`가 local socket을 열지 못해 targeted compile/test는 실행하지 못했다. 실패 메시지: `java.net.SocketException: Operation not permitted`.

## Future Guidance

- coroutine context에서 dispatcher를 조회할 때 `CoroutineDispatcher` 자체를 key처럼 쓰지 않는다. `ContinuationInterceptor`로 조회한 뒤 `CoroutineDispatcher`로 안전하게 cast한다.
- sandbox에서 Gradle이 socket/file-lock 문제로 시작하지 못하면 wrapper lock 문제와 분리해서 기록한다. distribution 다운로드 문제와 compile/test failure를 혼동하지 않는다.
