# 이슈 797: gRPC client plaintext opt-in

## 배경

`AbstractGrpcClient(host, port)`는 arbitrary host에 대해 조용히 plaintext channel을
만들었다. Convenience constructor를 remote service에 사용하는 subclass는 call site에
insecure 선택이 드러나지 않은 채 RPC traffic과 metadata를 TLS 없이 보낼 수 있었다.

## 결정

Convenience constructor는 기본적으로 secure하게 만들고, plaintext는 명시적인
local/test opt-in이 필요하게 한다.

- 기본값: `GrpcChannelSecurity.TRANSPORT_SECURITY`
- Local/test escape hatch: `GrpcChannelSecurity.LOCAL_PLAINTEXT`
- Plaintext opt-in은 loopback host로 제한한다.

## 결과

- Host/port `AbstractGrpcClient` construction은 더 이상 조용히 plaintext를 선택하지 않는다.
- Remote plaintext opt-in은 channel construction 전에 거부된다.
- README 영어/한국어 예시는 production transport security와 local/test plaintext를 분리해서 보여준다.

## 검증

- 구현 전 red test: 새 channel security API reference가 compile에 실패했다.
- `./gradlew :bluetape4k-grpc:compileTestKotlin --warning-mode all --rerun-tasks --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-grpc:test --tests "io.bluetape4k.grpc.GrpcChannelSecurityTest" --tests "io.bluetape4k.grpc.GrpcSupportValidationTest" --tests "io.bluetape4k.grpc.ManagedChannelSupportTest" --rerun-tasks --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-grpc:test --no-daemon --no-configuration-cache`: 72 tests, 0 failures, 0 errors, 4 skipped.
- `git diff --check`

## 향후 방지책

Arbitrary remote target에 대해 insecure transport를 조용히 선택하는 host/port
convenience constructor를 추가하지 않는다. Local test에 plaintext가 필요하면 API 이름
또는 enum value가 명시적으로 드러나게 하고 loopback-only임을 문서화한다.

## 동시성 helper gate

`MultithreadingTester`, `SuspendedJobTester`, `StructuredTaskScopeTester`는 적용 대상이
아니었다. 이는 concurrent state, coroutine lifecycle, structured task-scope behavior가
없는 synchronous channel-construction security 선택이다.
