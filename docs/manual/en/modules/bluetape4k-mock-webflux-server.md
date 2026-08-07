---
manualId: bluetape4k-mock-webflux-server
title: "WebFlux Mock Server"
description: "A standalone Spring Boot 4 + WebFlux mock server for integration testing. It provides HTTP endpoints compatible with httpbin.org and jsonplaceholder.typicode.com, implemented with Kotlin Coroutines (suspend fun, Flow)."
kind: library
group: testing
learningOrder: 1140
---

# WebFlux Mock Server

## Problem {#problem}

A standalone Spring Boot 4 + WebFlux mock server for integration testing. It provides HTTP endpoints compatible with httpbin.org and jsonplaceholder.typicode.com, implemented with Kotlin Coroutines (suspend fun, Flow). This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-mock-webflux-server` when the application needs fixture ownership, isolation, deterministic cleanup, and failure diagnostics. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-mock-webflux-server")
}
```

Gradle project path: `:bluetape4k-mock-webflux-server`. Source directory: `testing/mock-webflux-server`.

## Concepts {#concepts}

The first source-level concepts to inspect are `MockWebfluxServerApplication`, `AdminController`, `PingController`, `GlobalExceptionHandler`, `HttpsServerLifecycle`, `WebFluxJacksonConfig`, `HttpbinAdvancedController`, and `HttpbinController`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`MockWebfluxServerApplication`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/MockWebfluxServerApplication.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`MockWebfluxServerApplication`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/MockWebfluxServerApplication.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AdminController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/admin/AdminController.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`PingController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/admin/PingController.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`GlobalExceptionHandler`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/config/GlobalExceptionHandler.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HttpsServerLifecycle`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/config/HttpsServerLifecycle.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`WebFluxJacksonConfig`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/config/WebFluxJacksonConfig.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HttpbinAdvancedController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinAdvancedController.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HttpbinController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinController.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HttpbinStreamController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinStreamController.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HttpbinSupport`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Comparison with bluetape4k-mock-web-server**, **Diagrams**, **Request Routing Overview**, **Class Diagram**, **Sequence Diagram — httpbin GET**, **Features**, **Configuration**, **Examples**, and **Run via Docker**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
implementation(platform(libs.jackson3.bom))
implementation("org.springframework.boot:spring-boot-starter-webflux")
implementation("org.springframework.boot:spring-boot-starter-cache")
implementation("org.springframework.boot:spring-boot-starter-actuator")
implementation(libs.caffeine)
implementation(libs.jackson3.module.kotlin)
implementation(libs.kotlinx.coroutines.core)
implementation(libs.kotlinx.coroutines.reactor)
implementation(project(":bluetape4k-core"))
implementation(project(":bluetape4k-coroutines"))
implementation(project(":bluetape4k-logging"))
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`application.yml`](../../../../testing/mock-webflux-server/src/main/resources/application.yml)
- [`localhost.p12`](../../../../testing/mock-webflux-server/src/main/resources/certs/localhost.p12)
- [`rootCA.pem`](../../../../testing/mock-webflux-server/src/main/resources/certs/rootCA.pem)
- [`albums.json`](../../../../testing/mock-webflux-server/src/main/resources/jsonplaceholder/albums.json)
- [`comments.json`](../../../../testing/mock-webflux-server/src/main/resources/jsonplaceholder/comments.json)
- [`photos.json`](../../../../testing/mock-webflux-server/src/main/resources/jsonplaceholder/photos.json)
- [`posts.json`](../../../../testing/mock-webflux-server/src/main/resources/jsonplaceholder/posts.json)
- [`todos.json`](../../../../testing/mock-webflux-server/src/main/resources/jsonplaceholder/todos.json)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Keep fixtures isolated, bound resource use, expose diagnostics, and close shared services deterministically. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-mock-webflux-server:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractMockWebfluxServerTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/AbstractMockWebfluxServerTest.kt)
- [`ReadmeRouteContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/ReadmeRouteContractTest.kt)
- [`AdminResetContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/admin/AdminResetContractTest.kt)
- [`PingContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/admin/PingContractTest.kt)
- [`GlobalExceptionHandlerTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/config/GlobalExceptionHandlerTest.kt)
- [`HttpbinAdvancedContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinAdvancedContractTest.kt)
- [`HttpbinContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinContractTest.kt)
- [`HttpbinStreamContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinStreamContractTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Mock WebFlux Server routing overview

[![Mock WebFlux Server routing overview](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-webflux-server-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-webflux-server-diagram-01.svg)

_Release README: [`testing/mock-webflux-server/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/mock-webflux-server/README.md)_

### Mock WebFlux Server class structure

[![Mock WebFlux Server class structure](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-webflux-server-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-webflux-server-diagram-02.svg)

_Release README: [`testing/mock-webflux-server/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/mock-webflux-server/README.md)_

### WebFlux httpbin GET request sequence

[![WebFlux httpbin GET request sequence](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-webflux-server-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-webflux-server-sequence-01.svg)

_Release README: [`testing/mock-webflux-server/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/mock-webflux-server/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../testing/mock-webflux-server/README.md)
- [Module build](../../../../testing/mock-webflux-server/build.gradle.kts)
- [`MockWebfluxServerApplication`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/MockWebfluxServerApplication.kt)
- [`AdminController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/admin/AdminController.kt)
- [`PingController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/admin/PingController.kt)
- [`GlobalExceptionHandler`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/config/GlobalExceptionHandler.kt)
- [`HttpsServerLifecycle`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/config/HttpsServerLifecycle.kt)
- [`WebFluxJacksonConfig`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/config/WebFluxJacksonConfig.kt)
- [`HttpbinAdvancedController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinAdvancedController.kt)
- [`HttpbinController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinController.kt)
- [`HttpbinStreamController`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinStreamController.kt)
- [`HttpbinSupport`](../../../../testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux/httpbin/HttpbinSupport.kt)
- [`AbstractMockWebfluxServerTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/AbstractMockWebfluxServerTest.kt)
- [`ReadmeRouteContractTest`](../../../../testing/mock-webflux-server/src/test/kotlin/io/bluetape4k/mockwebflux/ReadmeRouteContractTest.kt)
