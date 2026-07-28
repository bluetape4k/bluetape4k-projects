# 이슈 656과 657 - gRPC shutdown interruption 보존

## 배경

port-based와 in-process gRPC server base class는 shutdown 중
`runCatching { awaitTermination(...) }.getOrDefault(false)`를 사용했다. timeout 시
forced shutdown은 보존됐지만, `InterruptedException`이 일반 timeout으로 접히면서
caller thread의 interrupt status가 사라졌다.

## 결정

두 server base class 모두 timed `awaitTermination` 주변에 명시적인 `try/catch`를
사용한다. `InterruptedException`에서는 `Thread.currentThread().interrupt()`로 interrupt
flag를 복원하고 `false`를 반환해 `shutdownNow()`가 계속 실행되게 한다. graceful
termination이 interruption 없이 timeout되면 force shutdown 후 같은 bounded timeout으로
한 번 더 기다린다.

## 결과

두 shutdown implementation은 이제 기존 timeout behavior를 유지하면서 graceful
termination이 interrupted될 때 interrupt status를 보존한다. forced shutdown도
일방 실행이 아니라 bounded termination wait를 갖는다.

## 검증

- `./gradlew :bluetape4k-grpc:compileKotlin :bluetape4k-grpc:compileTestKotlin :bluetape4k-grpc:test --tests io.bluetape4k.grpc.GrpcServerTest`
- `./gradlew :bluetape4k-grpc:koverXmlReport`
- `git diff --check`

## 향후 가드

`InterruptedException`을 던질 수 있는 blocking lifecycle API 주변에서 넓은
`runCatching`을 사용하지 않는다. cleanup fallback으로 가기 전에 interruption을
명시적으로 보존하거나 전파한다.
