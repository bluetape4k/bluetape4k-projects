# Issue 540 gRPC Server Lifecycle Tests

## Context

Issue #540 identified that `GrpcServer`, `AbstractGrpcServer`, and the
in-process server variant had little direct lifecycle coverage for start, stop,
shutdown fallback, and binding conflicts.

## Decision

Add a focused `GrpcServerTest` with real Netty and in-process servers for bind
and replacement behavior, plus fake `io.grpc.Server` implementations for
deterministic graceful and forced shutdown assertions. Add a protected
`createServer()` seam to `AbstractGrpcInprocessServer` so its stop semantics can
be tested without opening real transports.

## Outcome

Port-based servers now have coverage for start, stop, close delegation, port
conflicts, graceful shutdown, forced `shutdownNow()`, and replacement startup on
the released port. In-process servers now have coverage for start, stop, name
conflicts, replacement startup on the released name, and forced shutdown after
termination timeout.

## Verification

- `./gradlew :bluetape4k-grpc:compileKotlin :bluetape4k-grpc:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-grpc:test --tests 'io.bluetape4k.grpc.GrpcServerTest' --no-configuration-cache --rerun-tasks` (8 passing)
- `./gradlew :bluetape4k-grpc:test --no-configuration-cache` (61 passing, 4 pending)

## Future Agents

Keep lifecycle behavior in direct `GrpcServerTest` coverage. Use fake
`io.grpc.Server` implementations for timeout and forced-shutdown branches so
tests stay deterministic and do not wait for the real five-second shutdown
timeout.
