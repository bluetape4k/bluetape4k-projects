---
manualId: bluetape4k-http
title: "Module bluetape4k-http"
description: "bluetape4k-http integrates multiple HTTP client libraries through Kotlin extension functions and DSLs."
kind: library
group: io
---

# Module bluetape4k-http

## Problem {#problem}

bluetape4k-http integrates multiple HTTP client libraries through Kotlin extension functions and DSLs. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-http` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-http")
}
```

Gradle project path: `:bluetape4k-http`. Source directory: `io/http`.

## Concepts {#concepts}

The first source-level concepts to inspect are `AsyncClientConnectionManager`, `CloseableHttpAsyncClientCoroutines`, `HttpAsyncClient`, `HttpAsyncClientCoroutines`, `MinimalHttpAsyncClient`, `ConfigurableHttpRequest`, `SimpleHttpRequest`, and `SimpleHttpResponse`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`AsyncClientConnectionManager`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/AsyncClientConnectionManager.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`AsyncClientConnectionManager`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/AsyncClientConnectionManager.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CloseableHttpAsyncClientCoroutines`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/CloseableHttpAsyncClientCoroutines.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HttpAsyncClient`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/HttpAsyncClient.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HttpAsyncClientCoroutines`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/HttpAsyncClientCoroutines.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MinimalHttpAsyncClient`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/MinimalHttpAsyncClient.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ConfigurableHttpRequest`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/ConfigurableHttpRequest.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SimpleHttpRequest`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleHttpRequest.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SimpleHttpResponse`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleHttpResponse.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SimpleRequestProducer`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleRequestProducer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SimpleResponseConsumer`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleResponseConsumer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Architecture**, **Overall Architecture: Multi-Backend HTTP Client**, **HTTP Client Hierarchy (HC5)**, **OkHttp3 Client Hierarchy**, **Async HTTP Request Flow (HC5 Async + Coroutines)**, **Key Features**, **1. Apache HttpComponents 5 (HC5)**, **2. OkHttp3**, and **3. Vert.x HttpClient**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-io"))
api(project(":bluetape4k-netty"))
api(project(":bluetape4k-resilience4j"))
api(project(":bluetape4k-coroutines"))
compileOnly(libs.kotlinx.coroutines.core)
compileOnly(libs.kotlinx.coroutines.reactive)
compileOnly(libs.kotlinx.coroutines.reactor)
compileOnly(libs.okhttp3)
compileOnly(libs.okhttp3.coroutines)
compileOnly(libs.okhttp3.logging.interceptor)
compileOnly(libs.okhttp3.mockwebserver)
compileOnly(libs.httpclient5)
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
./gradlew :bluetape4k-http:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractHttpTest`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/AbstractHttpTest.kt)
- [`HttpClientBenchmark`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/benchmark/HttpClientBenchmark.kt)
- [`HttpClientBenchmarkTest`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/benchmark/HttpClientBenchmarkTest.kt)
- [`HttpClientCompressionCacheBenchmark`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/benchmark/HttpClientCompressionCacheBenchmark.kt)
- [`HttpClientLatencyBenchmark`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/benchmark/HttpClientLatencyBenchmark.kt)
- [`AbstractHc5Test`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/hc5/AbstractHc5Test.kt)
- [`AsyncHttpClientCoroutinesTest`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/hc5/async/AsyncHttpClientCoroutinesTest.kt)
- [`AsyncHttpClientTest`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/hc5/async/AsyncHttpClientTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../io/http/README.md)
- [Module build](../../../../io/http/build.gradle.kts)
- [`AsyncClientConnectionManager`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/AsyncClientConnectionManager.kt)
- [`CloseableHttpAsyncClientCoroutines`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/CloseableHttpAsyncClientCoroutines.kt)
- [`HttpAsyncClient`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/HttpAsyncClient.kt)
- [`HttpAsyncClientCoroutines`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/HttpAsyncClientCoroutines.kt)
- [`MinimalHttpAsyncClient`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/MinimalHttpAsyncClient.kt)
- [`ConfigurableHttpRequest`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/ConfigurableHttpRequest.kt)
- [`SimpleHttpRequest`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleHttpRequest.kt)
- [`SimpleHttpResponse`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleHttpResponse.kt)
- [`SimpleRequestProducer`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleRequestProducer.kt)
- [`SimpleResponseConsumer`](../../../../io/http/src/main/kotlin/io/bluetape4k/http/hc5/async/methods/SimpleResponseConsumer.kt)
- [`AbstractHttpTest`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/AbstractHttpTest.kt)
- [`HttpClientBenchmark`](../../../../io/http/src/test/kotlin/io/bluetape4k/http/benchmark/HttpClientBenchmark.kt)
