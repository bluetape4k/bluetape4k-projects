---
manualId: bluetape4k-retrofit2
title: "Retrofit 2 Coroutine Extensions"
description: "bluetape4k-retrofit2 is a module that extends Retrofit2 with Kotlin DSL and Coroutines support."
kind: library
group: io
learningOrder: 420
---

# Retrofit 2 Coroutine Extensions

## Problem {#problem}

bluetape4k-retrofit2 is a module that extends Retrofit2 with Kotlin DSL and Coroutines support. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-retrofit2` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-retrofit2")
}
```

Gradle project path: `:bluetape4k-retrofit2`. Source directory: `io/retrofit2`.

## Concepts {#concepts}

The first source-level concepts to inspect are `ExceptionSupport`, `RetrofitCallSupport`, `RetrofitSupport`, `SuspendRetrofitCallSupport`, `Hc5CallFactory`, `Hc5OkHttp3Support`, `VertxCallFactory`, and `VertxOkHttp3Support`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`ExceptionSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/ExceptionSupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`ExceptionSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/ExceptionSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RetrofitCallSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/RetrofitCallSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RetrofitSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/RetrofitSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendRetrofitCallSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/SuspendRetrofitCallSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Hc5CallFactory`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/hc5/Hc5CallFactory.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Hc5OkHttp3Support`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/hc5/Hc5OkHttp3Support.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VertxCallFactory`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/vertx/VertxCallFactory.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VertxOkHttp3Support`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/vertx/VertxOkHttp3Support.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ResultCall`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/result/ResultCall.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ResultCallAdapterFactory`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/result/ResultCallAdapterFactory.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Architecture**, **Retrofit2 Module Architecture**, **Retrofit2 + Result Pattern Integration**, **Suspend Result HTTP Request Flow**, **Key Features**, **1. Retrofit Builder DSL**, **2. Result Pattern Support**, **3. Coroutines Support**, and **4. Multiple HTTP Backends (CallFactory)**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-http"))
api(project(":bluetape4k-okio"))
api(project(":bluetape4k-netty"))
api(project(":bluetape4k-coroutines"))
compileOnly(libs.kotlinx.coroutines.core)
compileOnly(libs.kotlinx.coroutines.reactive)
compileOnly(libs.kotlinx.coroutines.reactor)
api(libs.retrofit2)
api(libs.retrofit2.converter.jackson)
api(libs.retrofit2.converter.scalars)
api(libs.retrofit2.adapter.java8)
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
./gradlew :bluetape4k-retrofit2:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractRetrofitTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/AbstractRetrofitTest.kt)
- [`ExceptionSupportTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/ExceptionSupportTest.kt)
- [`RetrofitBuilderSupportTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/RetrofitBuilderSupportTest.kt)
- [`RetrofitSupportTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/RetrofitSupportTest.kt)
- [`RetryRetrofitCallSupportTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/RetryRetrofitCallSupportTest.kt)
- [`SuspendRetrofitCallSupportTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/SuspendRetrofitCallSupportTest.kt)
- [`AbstractClientTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/client/AbstractClientTest.kt)
- [`AbstractDetectTempEmailTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/client/AbstractDetectTempEmailTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Retrofit2 Module Architecture diagram

[![Retrofit2 Module Architecture diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-retrofit2-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-retrofit2-diagram-01.svg)

_Release README: [`io/retrofit2/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/retrofit2/README.md)_

### Retrofit2 + Result Pattern Integration diagram

[![Retrofit2 + Result Pattern Integration diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-retrofit2-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-retrofit2-diagram-02.svg)

_Release README: [`io/retrofit2/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/retrofit2/README.md)_

### Suspend Result HTTP Request Flow diagram

[![Suspend Result HTTP Request Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-retrofit2-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-retrofit2-sequence-01.svg)

_Release README: [`io/retrofit2/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/retrofit2/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../io/retrofit2/README.md)
- [Module build](../../../../io/retrofit2/build.gradle.kts)
- [`ExceptionSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/ExceptionSupport.kt)
- [`RetrofitCallSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/RetrofitCallSupport.kt)
- [`RetrofitSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/RetrofitSupport.kt)
- [`SuspendRetrofitCallSupport`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/SuspendRetrofitCallSupport.kt)
- [`Hc5CallFactory`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/hc5/Hc5CallFactory.kt)
- [`Hc5OkHttp3Support`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/hc5/Hc5OkHttp3Support.kt)
- [`VertxCallFactory`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/vertx/VertxCallFactory.kt)
- [`VertxOkHttp3Support`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/vertx/VertxOkHttp3Support.kt)
- [`ResultCall`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/result/ResultCall.kt)
- [`ResultCallAdapterFactory`](../../../../io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/result/ResultCallAdapterFactory.kt)
- [`AbstractRetrofitTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/AbstractRetrofitTest.kt)
- [`ExceptionSupportTest`](../../../../io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/ExceptionSupportTest.kt)
