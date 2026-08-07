---
manualId: bluetape4k-vertx
title: "Vert.x Coroutine Extensions"
description: "A unified module for async and Coroutines-based development with Vert.x."
kind: library
group: io
learningOrder: 450
---

# Vert.x Coroutine Extensions

## Problem {#problem}

A unified module for async and Coroutines-based development with Vert.x. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-vertx` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-vertx")
}
```

Gradle project path: `:bluetape4k-vertx`. Source directory: `io/vertx`.

## Concepts {#concepts}

The first source-level concepts to inspect are `CoroutineSupport`, `FutureExtensions`, `VertxSupport`, `VertxDecorators`, `VertxFutureBulkheadSupport`, `VertxFutureCircuitBreakerSupport`, `VertxFutureRateLimiterSupport`, and `VertxFutureRetrySupport`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`CoroutineSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/CoroutineSupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`CoroutineSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/CoroutineSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`FutureExtensions`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/FutureExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VertxSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/VertxSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VertxDecorators`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxDecorators.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VertxFutureBulkheadSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureBulkheadSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VertxFutureCircuitBreakerSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureCircuitBreakerSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VertxFutureRateLimiterSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureRateLimiterSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VertxFutureRetrySupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureRetrySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VertxFutureSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VertxFutureTimeLimiterSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureTimeLimiterSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **What's Included**, **Vert.x Core (formerly vertx/core)**, **Vert.x SQL Client (formerly vertx/sqlclient)**, **Resilience4j Integration (formerly vertx/resilience4j)**, **Architecture Diagrams**, **Module Dependency Structure**, **Vert.x Event Loop + Coroutines Processing Flow**, **Circuit Breaker + Resilience4j Integration Flow**, **Vert.x Core Component Class Structure**, and **Installation**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-io"))
api(project(":bluetape4k-netty"))
api(project(":bluetape4k-coroutines"))
api(project(":bluetape4k-jdbc"))
api(libs.vertx.core)
api(libs.vertx.lang.kotlin)
api(libs.vertx.lang.kotlin.coroutines)
compileOnly(libs.vertx.web)
compileOnly(libs.vertx.web.client)
compileOnly(libs.vertx.junit5)
api(project(":bluetape4k-resilience4j"))
compileOnly(libs.resilience4j.reactor)
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
./gradlew :bluetape4k-vertx:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractVertxTest`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/AbstractVertxTest.kt)
- [`SampleVerticle`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/SampleVerticle.kt)
- [`VertxSupportTest`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/VertxSupportTest.kt)
- [`LifecycleExamples`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/examples/LifecycleExamples.kt)
- [`SampleVerticleTest`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/examples/SampleVerticleTest.kt)
- [`VertxJunit5Examples`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/examples/VertxJunit5Examples.kt)
- [`AbstractVertxFutureTest`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/resilience4j/AbstractVertxFutureTest.kt)
- [`VertxDecoratorsTest`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/resilience4j/VertxDecoratorsTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Module Dependency Structure diagram

[![Module Dependency Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-diagram-01.svg)

_Release README: [`io/vertx/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/vertx/README.md)_

### Vert.x Event Loop + Coroutines Processing Flow diagram

[![Vert.x Event Loop + Coroutines Processing Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-diagram-02.svg)

_Release README: [`io/vertx/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/vertx/README.md)_

### Vert.x Core Component Class Structure diagram

[![Vert.x Core Component Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-diagram-03.svg)

_Release README: [`io/vertx/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/vertx/README.md)_

### Circuit Breaker + Resilience4j Integration Flow diagram

[![Circuit Breaker + Resilience4j Integration Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-vertx-sequence-01.svg)

_Release README: [`io/vertx/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/vertx/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../io/vertx/README.md)
- [Module build](../../../../io/vertx/build.gradle.kts)
- [`CoroutineSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/CoroutineSupport.kt)
- [`FutureExtensions`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/FutureExtensions.kt)
- [`VertxSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/VertxSupport.kt)
- [`VertxDecorators`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxDecorators.kt)
- [`VertxFutureBulkheadSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureBulkheadSupport.kt)
- [`VertxFutureCircuitBreakerSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureCircuitBreakerSupport.kt)
- [`VertxFutureRateLimiterSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureRateLimiterSupport.kt)
- [`VertxFutureRetrySupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureRetrySupport.kt)
- [`VertxFutureSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureSupport.kt)
- [`VertxFutureTimeLimiterSupport`](../../../../io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j/VertxFutureTimeLimiterSupport.kt)
- [`AbstractVertxTest`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/AbstractVertxTest.kt)
- [`SampleVerticle`](../../../../io/vertx/src/test/kotlin/io/bluetape4k/vertx/SampleVerticle.kt)
