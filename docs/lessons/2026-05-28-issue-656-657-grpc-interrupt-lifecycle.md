# Issues 656 and 657 - gRPC shutdown interruption

## Context

The port-based and in-process gRPC server base classes used
`runCatching { awaitTermination(...) }.getOrDefault(false)` during shutdown.
That preserved forced shutdown on timeout, but it also collapsed
`InterruptedException` into a normal timeout and lost the caller thread's
interrupt status.

## Decision

Use explicit `try/catch` around timed `awaitTermination` in both server base
classes. On `InterruptedException`, restore the interrupt flag with
`Thread.currentThread().interrupt()` and return `false` so `shutdownNow()` still
runs. When graceful termination times out without interruption, force shutdown
and wait once more with the same bounded timeout.

## Outcome

Both shutdown implementations now keep the existing timeout behavior and
preserve interrupt status when graceful termination is interrupted. Forced
shutdown now also gets a bounded termination wait instead of being fire-and-forget.

## Verification

- `./gradlew :bluetape4k-grpc:compileKotlin :bluetape4k-grpc:compileTestKotlin :bluetape4k-grpc:test --tests io.bluetape4k.grpc.GrpcServerTest`
- `./gradlew :bluetape4k-grpc:koverXmlReport`
- `git diff --check`

## Future guard

Do not use broad `runCatching` around blocking lifecycle APIs that can throw
`InterruptedException`. Preserve or propagate interruption explicitly before
falling back to cleanup.
