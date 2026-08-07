---
manualId: bluetape4k-resilience4j
title: "Resilience Patterns with Resilience4j"
description: "Resilience4j is a lightweight, fault-tolerance library for isolation and recovery."
kind: library
group: operations
learningOrder: 1000
---

# Resilience Patterns with Resilience4j

## Problem {#problem}

Resilience4j is a lightweight, fault-tolerance library for isolation and recovery. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-resilience4j` when the application needs client lifecycle, reconnect policy, backpressure, retries, and observability. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-resilience4j")
}
```

Gradle project path: `:bluetape4k-resilience4j`. Source directory: `infra/resilience4j`.

## Concepts {#concepts}

The first source-level concepts to inspect are `RetryAsyncContextBridge`, `CallableSupport`, `CancellationSupport`, `CompletionStageSupport`, `DecoratorsSupport`, `SupplierSupport`, `SuspendDecorators`, and `SuspendSupplierExtensions`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`RetryAsyncContextBridge`](../../../../infra/resilience4j/src/main/java/io/bluetape4k/resilience4j/retry/RetryAsyncContextBridge.java) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`RetryAsyncContextBridge`](../../../../infra/resilience4j/src/main/java/io/bluetape4k/resilience4j/retry/RetryAsyncContextBridge.java) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CallableSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/CallableSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CancellationSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/CancellationSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CompletionStageSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/CompletionStageSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DecoratorsSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/DecoratorsSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SupplierSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SupplierSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendDecorators`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SuspendDecorators.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendSupplierExtensions`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SuspendSupplierExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BulkheadCoroutines`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/bulkhead/BulkheadCoroutines.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BulkheadExtensions`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/bulkhead/BulkheadExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Class Structure**, **Resilience4j Coroutine Class Structure**, **Architecture**, **CircuitBreaker + Retry Combination Sequence Diagram**, **SuspendCache Operation Sequence Diagram**, **Features**, **Module Boundary**, **Coroutine Contract**, **Dependency**, and **Key Features**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
compileOnly(project(":bluetape4k-coroutines"))
compileOnly(project(":bluetape4k-cache-redisson"))
api(libs.resilience4j.all)
api(libs.resilience4j.cache)
api(libs.resilience4j.kotlin)
compileOnly(libs.resilience4j.reactor)
compileOnly(libs.resilience4j.micrometer)
compileOnly(libs.kotlinx.coroutines.core)
compileOnly(libs.kotlinx.coroutines.reactor)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track connection state, queue depth, retries, timeouts, remote errors, and graceful shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-resilience4j:test --no-configuration-cache
```

Representative test anchors:

- [`AsyncHelloWorldService`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/AsyncHelloWorldService.kt)
- [`CallableSupportTest`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/CallableSupportTest.kt)
- [`CompletionStageSupportTest`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/CompletionStageSupportTest.kt)
- [`DecoratorExtensionsTest`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/DecoratorExtensionsTest.kt)
- [`DecoratorsTest`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/DecoratorsTest.kt)
- [`HelloWorldException`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/HelloWorldException.kt)
- [`HelloWorldService`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/HelloWorldService.kt)
- [`Resilience4jCancellationContractTest`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/Resilience4jCancellationContractTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Resilience4j Coroutine Class Structure diagram

[![Resilience4j Coroutine Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-resilience4j-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-resilience4j-diagram-01.svg)

_Release README: [`infra/resilience4j/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/resilience4j/README.md)_

### CircuitBreaker and Retry sequence diagram

[![CircuitBreaker and Retry sequence diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-resilience4j-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-resilience4j-sequence-01.svg)

_Release README: [`infra/resilience4j/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/resilience4j/README.md)_

### SuspendCache operation sequence diagram

[![SuspendCache operation sequence diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-resilience4j-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-resilience4j-sequence-02.svg)

_Release README: [`infra/resilience4j/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/resilience4j/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../infra/resilience4j/README.md)
- [Module build](../../../../infra/resilience4j/build.gradle.kts)
- [`RetryAsyncContextBridge`](../../../../infra/resilience4j/src/main/java/io/bluetape4k/resilience4j/retry/RetryAsyncContextBridge.java)
- [`CallableSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/CallableSupport.kt)
- [`CancellationSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/CancellationSupport.kt)
- [`CompletionStageSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/CompletionStageSupport.kt)
- [`DecoratorsSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/DecoratorsSupport.kt)
- [`SupplierSupport`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SupplierSupport.kt)
- [`SuspendDecorators`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SuspendDecorators.kt)
- [`SuspendSupplierExtensions`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/SuspendSupplierExtensions.kt)
- [`BulkheadCoroutines`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/bulkhead/BulkheadCoroutines.kt)
- [`BulkheadExtensions`](../../../../infra/resilience4j/src/main/kotlin/io/bluetape4k/resilience4j/bulkhead/BulkheadExtensions.kt)
- [`AsyncHelloWorldService`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/AsyncHelloWorldService.kt)
- [`CallableSupportTest`](../../../../infra/resilience4j/src/test/kotlin/io/bluetape4k/resilience4j/CallableSupportTest.kt)
