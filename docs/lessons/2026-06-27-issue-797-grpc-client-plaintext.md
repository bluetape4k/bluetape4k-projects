# Issue 797: gRPC client plaintext opt-in

## Context

`AbstractGrpcClient(host, port)` silently built a plaintext channel for arbitrary hosts. Subclasses using the convenience constructor for remote services could therefore send RPC traffic and metadata without TLS even though the insecure choice was not visible at the call site.

## Decision

Make the convenience constructor secure by default and require explicit local/test opt-in for plaintext:

- Default: `GrpcChannelSecurity.TRANSPORT_SECURITY`
- Local/test escape hatch: `GrpcChannelSecurity.LOCAL_PLAINTEXT`
- Plaintext opt-in is restricted to loopback hosts.

## Outcome

- Host/port `AbstractGrpcClient` construction no longer silently chooses plaintext.
- Remote plaintext opt-in is rejected before channel construction.
- README English/Korean examples now show production transport security and local/test plaintext separately.

## Verification

- Red test before implementation: new channel security API references failed compilation.
- `./gradlew :bluetape4k-grpc:compileTestKotlin --warning-mode all --rerun-tasks --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-grpc:test --tests "io.bluetape4k.grpc.GrpcChannelSecurityTest" --tests "io.bluetape4k.grpc.GrpcSupportValidationTest" --tests "io.bluetape4k.grpc.ManagedChannelSupportTest" --rerun-tasks --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-grpc:test --no-daemon --no-configuration-cache`: 72 tests, 0 failures, 0 errors, 4 skipped.
- `git diff --check`

## Future Guard

Do not add host/port convenience constructors that silently select insecure transports for arbitrary remote targets. If plaintext is needed for local tests, make the API name or enum value explicit and document that it is loopback-only.

## Concurrency Helper Gate

`MultithreadingTester`, `SuspendedJobTester`, and `StructuredTaskScopeTester` were not applicable. This is a synchronous channel-construction security choice with no concurrent state, coroutine lifecycle, or structured task-scope behavior.
