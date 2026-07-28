# 이슈 540 gRPC Server Lifecycle Test

## 배경

Issue #540은 `GrpcServer`, `AbstractGrpcServer`, in-process server variant에 start, stop, shutdown
fallback, binding conflict에 대한 직접 lifecycle coverage가 부족하다고 지적했다.

## 결정

Bind와 replacement behavior에는 real Netty 및 in-process server를 쓰고, graceful/forced shutdown
assertion에는 fake `io.grpc.Server` implementation을 쓰는 focused `GrpcServerTest`를 추가한다.
실제 transport를 열지 않고 stop semantics를 test할 수 있도록 `AbstractGrpcInprocessServer`에는 protected
`createServer()` seam을 추가한다.

## 결과

Port-based server는 start, stop, close delegation, port conflict, graceful shutdown, forced
`shutdownNow()`, released port에서 replacement startup을 cover한다. In-process server는 start, stop,
name conflict, released name에서 replacement startup, termination timeout 이후 forced shutdown을
cover한다.

## 검증

- `./gradlew :bluetape4k-grpc:compileKotlin :bluetape4k-grpc:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-grpc:test --tests 'io.bluetape4k.grpc.GrpcServerTest' --no-configuration-cache --rerun-tasks` (8 passing)
- `./gradlew :bluetape4k-grpc:test --no-configuration-cache` (61 passing, 4 pending)

## 향후 agent 가이드

Lifecycle behavior는 직접 `GrpcServerTest` coverage에 유지한다. Timeout 및 forced-shutdown branch에는
fake `io.grpc.Server` implementation을 사용해 test가 deterministic하게 유지되고 실제 5초 shutdown
timeout을 기다리지 않게 한다.
