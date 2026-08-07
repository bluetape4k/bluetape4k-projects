---
manualId: bluetape4k-mock-web-server
title: "HTTP Mock Server"
description: "A self-contained Spring Boot 4 + Virtual Threads HTTP mock server that replaces external HTTP dependencies in integration tests."
kind: library
group: testing
learningOrder: 1130
---

# HTTP Mock Server

## Problem {#problem}

A self-contained Spring Boot 4 + Virtual Threads HTTP mock server that replaces external HTTP dependencies in integration tests. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-mock-web-server` when the application needs fixture ownership, isolation, deterministic cleanup, and failure diagnostics. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-mock-web-server")
}
```

Gradle project path: `:bluetape4k-mock-web-server`. Source directory: `testing/mock-web-server`.

## Concepts {#concepts}

The first source-level concepts to inspect are `MockServerApplication`, `AdminController`, `PingController`, `GlobalExceptionHandler`, `HttpsConfiguration`, `HttpbinAdvancedController`, `HttpbinController`, and `HttpbinStreamController`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`MockServerApplication`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/MockServerApplication.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`MockServerApplication`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/MockServerApplication.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AdminController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/admin/AdminController.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`PingController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/admin/PingController.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`GlobalExceptionHandler`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/config/GlobalExceptionHandler.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HttpsConfiguration`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/config/HttpsConfiguration.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HttpbinAdvancedController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinAdvancedController.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HttpbinController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinController.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HttpbinStreamController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinStreamController.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HttpbinSupport`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ImageLoaderService`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/ImageLoaderService.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Diagrams**, **Request Routing Overview**, **Class Diagram**, **Sequence Diagram — httpbin GET**, **Features**, **Configuration**, **Examples**, **Run via Docker**, and **Build Docker image with Jib**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
implementation(platform(libs.jackson3.bom))
implementation("org.springframework.boot:spring-boot-starter-web")
implementation("org.springframework.boot:spring-boot-starter-cache")
implementation(libs.caffeine)
implementation(libs.jackson3.module.kotlin)
implementation(project(":bluetape4k-core"))
implementation(project(":bluetape4k-logging"))
implementation(project(":bluetape4k-jackson3"))
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`application.yml`](../../../../testing/mock-web-server/src/main/resources/application.yml)
- [`localhost.p12`](../../../../testing/mock-web-server/src/main/resources/certs/localhost.p12)
- [`rootCA.pem`](../../../../testing/mock-web-server/src/main/resources/certs/rootCA.pem)
- [`albums.json`](../../../../testing/mock-web-server/src/main/resources/jsonplaceholder/albums.json)
- [`comments.json`](../../../../testing/mock-web-server/src/main/resources/jsonplaceholder/comments.json)
- [`photos.json`](../../../../testing/mock-web-server/src/main/resources/jsonplaceholder/photos.json)
- [`posts.json`](../../../../testing/mock-web-server/src/main/resources/jsonplaceholder/posts.json)
- [`todos.json`](../../../../testing/mock-web-server/src/main/resources/jsonplaceholder/todos.json)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Keep fixtures isolated, bound resource use, expose diagnostics, and close shared services deterministically. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-mock-web-server:test --no-configuration-cache
```

Representative test anchors:

- [`MockServerTestBase`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/MockServerTestBase.kt)
- [`ReadmeHttpsPortContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/ReadmeHttpsPortContractTest.kt)
- [`AdminResetContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/admin/AdminResetContractTest.kt)
- [`PingContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/admin/PingContractTest.kt)
- [`HttpbinAdvancedContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinAdvancedContractTest.kt)
- [`HttpbinContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinContractTest.kt)
- [`HttpbinStreamContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinStreamContractTest.kt)
- [`HttpbinSupportTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinSupportTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Mock Web Server routing overview

[![Mock Web Server routing overview](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-web-server-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-web-server-diagram-01.svg)

_Release README: [`testing/mock-web-server/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/mock-web-server/README.md)_

### Mock Web Server class structure

[![Mock Web Server class structure](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-web-server-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-web-server-diagram-02.svg)

_Release README: [`testing/mock-web-server/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/mock-web-server/README.md)_

### httpbin GET request sequence

[![httpbin GET request sequence](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-web-server-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-mock-web-server-sequence-01.svg)

_Release README: [`testing/mock-web-server/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/mock-web-server/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../testing/mock-web-server/README.md)
- [Module build](../../../../testing/mock-web-server/build.gradle.kts)
- [`MockServerApplication`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/MockServerApplication.kt)
- [`AdminController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/admin/AdminController.kt)
- [`PingController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/admin/PingController.kt)
- [`GlobalExceptionHandler`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/config/GlobalExceptionHandler.kt)
- [`HttpsConfiguration`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/config/HttpsConfiguration.kt)
- [`HttpbinAdvancedController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinAdvancedController.kt)
- [`HttpbinController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinController.kt)
- [`HttpbinStreamController`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinStreamController.kt)
- [`HttpbinSupport`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/HttpbinSupport.kt)
- [`ImageLoaderService`](../../../../testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver/httpbin/ImageLoaderService.kt)
- [`MockServerTestBase`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/MockServerTestBase.kt)
- [`ReadmeHttpsPortContractTest`](../../../../testing/mock-web-server/src/test/kotlin/io/bluetape4k/mockserver/ReadmeHttpsPortContractTest.kt)
