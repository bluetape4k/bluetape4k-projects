---
manualId: bluetape4k-grpc
title: "Coroutine gRPC Extensions"
description: "A Kotlin extension library for implementing gRPC servers and clients."
kind: library
group: io
learningOrder: 440
---

# Coroutine gRPC Extensions

## Problem {#problem}

A Kotlin extension library for implementing gRPC servers and clients. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-grpc` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-grpc")
}
```

Gradle project path: `:bluetape4k-grpc`. Source directory: `io/grpc`.

## Concepts {#concepts}

The first source-level concepts to inspect are `AbstractGrpcClient`, `AbstractGrpcServer`, `GrpcChannelSecurity`, `GrpcServer`, `ManagedChannelSupport`, `ServerSupport`, `AbstractGrpcInprocessClient`, and `AbstractGrpcInprocessServer`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`AbstractGrpcClient`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/AbstractGrpcClient.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`AbstractGrpcClient`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/AbstractGrpcClient.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AbstractGrpcServer`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/AbstractGrpcServer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`GrpcChannelSecurity`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/GrpcChannelSecurity.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`GrpcServer`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/GrpcServer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ManagedChannelSupport`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/ManagedChannelSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ServerSupport`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/ServerSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AbstractGrpcInprocessClient`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/inprocess/AbstractGrpcInprocessClient.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AbstractGrpcInprocessServer`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/inprocess/AbstractGrpcInprocessServer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ServerInterceptorSupport`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/interceptor/ServerInterceptorSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Architecture**, **gRPC Class Structure**, **Component Overview**, **gRPC Server-Client Communication Sequence**, **In-process Test Sequence**, **Key Features**, **Usage Examples**, **1. Implementing a gRPC Server**, and **2. Implementing a gRPC Client**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-protobuf"))
api(project(":bluetape4k-core"))
api(project(":bluetape4k-io"))
api(project(":bluetape4k-jackson3"))
api(project(":bluetape4k-netty"))
api(libs.grpc.api)
api(libs.grpc.alts)
api(libs.grpc.netty)
api(libs.grpc.protobuf)
api(libs.grpc.stub)
api(libs.grpc.auth)
api(libs.grpc.grpclb)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track payload size, allocation, latency, malformed-input rate, resource closure, and protocol errors. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-grpc:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractGrpcTest`](../../../../io/grpc/src/test/kotlin/io/bluetape4k/grpc/AbstractGrpcTest.kt)
- [`GrpcChannelSecurityTest`](../../../../io/grpc/src/test/kotlin/io/bluetape4k/grpc/GrpcChannelSecurityTest.kt)
- [`GrpcServerTest`](../../../../io/grpc/src/test/kotlin/io/bluetape4k/grpc/GrpcServerTest.kt)
- [`GrpcSupportValidationTest`](../../../../io/grpc/src/test/kotlin/io/bluetape4k/grpc/GrpcSupportValidationTest.kt)
- [`ManagedChannelSupportTest`](../../../../io/grpc/src/test/kotlin/io/bluetape4k/grpc/ManagedChannelSupportTest.kt)
- [`GreeterClient`](../../../../io/grpc/src/test/kotlin/io/bluetape4k/grpc/examples/helloworld/GreeterClient.kt)
- [`GreeterServer`](../../../../io/grpc/src/test/kotlin/io/bluetape4k/grpc/examples/helloworld/GreeterServer.kt)
- [`GreeterService`](../../../../io/grpc/src/test/kotlin/io/bluetape4k/grpc/examples/helloworld/GreeterService.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### gRPC Class Structure diagram

[![gRPC Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-grpc-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-grpc-diagram-01.svg)

_Release README: [`io/grpc/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/grpc/README.md)_

### Component Overview diagram

[![Component Overview diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-grpc-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-grpc-diagram-02.svg)

_Release README: [`io/grpc/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/grpc/README.md)_

### gRPC Server-Client Communication Sequence diagram

[![gRPC Server-Client Communication Sequence diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-grpc-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-grpc-sequence-01.svg)

_Release README: [`io/grpc/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/grpc/README.md)_

### In-process Test Sequence diagram

[![In-process Test Sequence diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-grpc-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-grpc-sequence-02.svg)

_Release README: [`io/grpc/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/grpc/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../io/grpc/README.md)
- [Module build](../../../../io/grpc/build.gradle.kts)
- [`AbstractGrpcClient`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/AbstractGrpcClient.kt)
- [`AbstractGrpcServer`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/AbstractGrpcServer.kt)
- [`GrpcChannelSecurity`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/GrpcChannelSecurity.kt)
- [`GrpcServer`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/GrpcServer.kt)
- [`ManagedChannelSupport`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/ManagedChannelSupport.kt)
- [`ServerSupport`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/ServerSupport.kt)
- [`AbstractGrpcInprocessClient`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/inprocess/AbstractGrpcInprocessClient.kt)
- [`AbstractGrpcInprocessServer`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/inprocess/AbstractGrpcInprocessServer.kt)
- [`ServerInterceptorSupport`](../../../../io/grpc/src/main/kotlin/io/bluetape4k/grpc/interceptor/ServerInterceptorSupport.kt)
- [`AbstractGrpcTest`](../../../../io/grpc/src/test/kotlin/io/bluetape4k/grpc/AbstractGrpcTest.kt)
- [`GrpcChannelSecurityTest`](../../../../io/grpc/src/test/kotlin/io/bluetape4k/grpc/GrpcChannelSecurityTest.kt)
- [`GrpcServerTest`](../../../../io/grpc/src/test/kotlin/io/bluetape4k/grpc/GrpcServerTest.kt)
