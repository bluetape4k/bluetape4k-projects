---
manualId: bluetape4k-feign
title: "Declarative HTTP Clients with Feign"
description: "bluetape4k-feign extends OpenFeign with a Kotlin DSL and Coroutines support."
kind: library
group: io
learningOrder: 410
---

# Declarative HTTP Clients with Feign

## Problem {#problem}

bluetape4k-feign extends OpenFeign with a Kotlin DSL and Coroutines support. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-feign` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-feign")
}
```

Gradle project path: `:bluetape4k-feign`. Source directory: `io/feign`.

## Concepts {#concepts}

The first source-level concepts to inspect are `FeignBuilderSupport`, `FeignRequestSupport`, `FeignResponseSupport`, `AsyncVertxHttpClient`, `VertxFeignSupport`, `VertxHttpClient`, `FeignFastjsonDecoder`, and `FeignFastjsonEncoder`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`FeignBuilderSupport`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/FeignBuilderSupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`FeignBuilderSupport`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/FeignBuilderSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`FeignRequestSupport`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/FeignRequestSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`FeignResponseSupport`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/FeignResponseSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AsyncVertxHttpClient`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/clients/vertx/AsyncVertxHttpClient.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VertxFeignSupport`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/clients/vertx/VertxFeignSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VertxHttpClient`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/clients/vertx/VertxHttpClient.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`FeignFastjsonDecoder`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/codec/FeignFastjsonDecoder.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`FeignFastjsonEncoder`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/codec/FeignFastjsonEncoder.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JacksonDecoder2`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/codec/JacksonDecoder2.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JacksonEncoder2`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/codec/JacksonEncoder2.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Architecture**, **Overall Architecture: Feign + Coroutines Integration**, **Class Structure: Feign + Coroutines**, **HTTP Transport Layer Options**, **Suspend Function HTTP Request Flow**, **Key Features**, **1. Feign Builder DSL**, **2. Coroutines Support**, and **3. HTTP Transport Layer Options**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-http"))
api(project(":bluetape4k-netty"))
compileOnly(project(":bluetape4k-coroutines"))
compileOnly(libs.kotlinx.coroutines.core)
compileOnly(libs.kotlinx.coroutines.reactor)
api(libs.feign.core)
api(libs.feign.hc5)
api(libs.feign.kotlin)
api(libs.feign.slf4j)
api(libs.feign.jackson)
compileOnly(libs.feign.reactive.wrappers)
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
./gradlew :bluetape4k-feign:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractFeignTest`](../../../../io/feign/src/test/kotlin/io/bluetape4k/feign/AbstractFeignTest.kt)
- [`FeignBuilderSupportTest`](../../../../io/feign/src/test/kotlin/io/bluetape4k/feign/FeignBuilderSupportTest.kt)
- [`FeignRequestSupportTest`](../../../../io/feign/src/test/kotlin/io/bluetape4k/feign/FeignRequestSupportTest.kt)
- [`FeignResponseSupportTest`](../../../../io/feign/src/test/kotlin/io/bluetape4k/feign/FeignResponseSupportTest.kt)
- [`RetryerRegressionTest`](../../../../io/feign/src/test/kotlin/io/bluetape4k/feign/RetryerRegressionTest.kt)
- [`AbstractClientTest`](../../../../io/feign/src/test/kotlin/io/bluetape4k/feign/clients/AbstractClientTest.kt)
- [`AbstractCoroutineClientTest`](../../../../io/feign/src/test/kotlin/io/bluetape4k/feign/clients/AbstractCoroutineClientTest.kt)
- [`AbstractHttpbinCoroutineTest`](../../../../io/feign/src/test/kotlin/io/bluetape4k/feign/clients/AbstractHttpbinCoroutineTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Overall Architecture: Feign + Coroutines Integration diagram

[![Overall Architecture: Feign + Coroutines Integration diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-feign-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-feign-diagram-01.svg)

_Release README: [`io/feign/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/feign/README.md)_

### Class Structure: Feign + Coroutines diagram

[![Class Structure: Feign + Coroutines diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-feign-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-feign-diagram-02.svg)

_Release README: [`io/feign/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/feign/README.md)_

### HTTP Transport Layer Options diagram

[![HTTP Transport Layer Options diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-feign-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-feign-diagram-03.svg)

_Release README: [`io/feign/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/feign/README.md)_

### Suspend Function HTTP Request Flow diagram

[![Suspend Function HTTP Request Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-feign-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-feign-sequence-01.svg)

_Release README: [`io/feign/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/feign/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../io/feign/README.md)
- [Module build](../../../../io/feign/build.gradle.kts)
- [`FeignBuilderSupport`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/FeignBuilderSupport.kt)
- [`FeignRequestSupport`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/FeignRequestSupport.kt)
- [`FeignResponseSupport`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/FeignResponseSupport.kt)
- [`AsyncVertxHttpClient`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/clients/vertx/AsyncVertxHttpClient.kt)
- [`VertxFeignSupport`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/clients/vertx/VertxFeignSupport.kt)
- [`VertxHttpClient`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/clients/vertx/VertxHttpClient.kt)
- [`FeignFastjsonDecoder`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/codec/FeignFastjsonDecoder.kt)
- [`FeignFastjsonEncoder`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/codec/FeignFastjsonEncoder.kt)
- [`JacksonDecoder2`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/codec/JacksonDecoder2.kt)
- [`JacksonEncoder2`](../../../../io/feign/src/main/kotlin/io/bluetape4k/feign/codec/JacksonEncoder2.kt)
- [`AbstractFeignTest`](../../../../io/feign/src/test/kotlin/io/bluetape4k/feign/AbstractFeignTest.kt)
- [`FeignBuilderSupportTest`](../../../../io/feign/src/test/kotlin/io/bluetape4k/feign/FeignBuilderSupportTest.kt)
