---
manualId: bluetape4k-ktor-core
title: "Ktor Application Foundations"
description: "Small Ktor server defaults for bluetape4k applications."
kind: library
group: web
learningOrder: 800
---

# Ktor Application Foundations

## Problem {#problem}

Small Ktor server defaults for bluetape4k applications. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-ktor-core` when the application needs request lifecycle, cancellation, routing, context propagation, and test boundaries. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-ktor-core")
}
```

Gradle project path: `:bluetape4k-ktor-core`. Source directory: `ktor/core`.

## Concepts {#concepts}

The first source-level concepts to inspect are `ApiErrorResponse`, `Bluetape4kKtorCore`, `Bluetape4kKtorCoreConfig`, `Bluetape4kKtorJson`, `Bluetape4kStatusPages`, `HealthResponse`, `KtorHealthRoutes`, and `KtorRequestParameters`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`ApiErrorResponse`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/ApiErrorResponse.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`ApiErrorResponse`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/ApiErrorResponse.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Bluetape4kKtorCore`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/Bluetape4kKtorCore.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Bluetape4kKtorCoreConfig`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/Bluetape4kKtorCoreConfig.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Bluetape4kKtorJson`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/Bluetape4kKtorJson.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Bluetape4kStatusPages`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/Bluetape4kStatusPages.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HealthResponse`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/HealthResponse.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KtorHealthRoutes`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/KtorHealthRoutes.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KtorRequestParameters`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/KtorRequestParameters.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture Diagram**, **Features**, **Dependency**, and **Usage**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
api(libs.ktor.server.core)
api(libs.ktor.server.content.negotiation)
api(libs.ktor.server.status.pages)
api(libs.ktor.serialization.kotlinx.json)
api(libs.kotlinx.serialization.json)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track request latency, status codes, cancellation, queueing, dependency failures, and shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-ktor-core:test --no-configuration-cache
```

Representative test anchors:

- [`Bluetape4kKtorCoreTest`](../../../../ktor/core/src/test/kotlin/io/bluetape4k/ktor/core/Bluetape4kKtorCoreTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Ktor Core Architecture

[![Ktor Core Architecture](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/ktor-core-architecture-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/ktor-core-architecture-01.svg)

_Release README: [`ktor/core/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/ktor/core/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../ktor/core/README.md)
- [Module build](../../../../ktor/core/build.gradle.kts)
- [`ApiErrorResponse`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/ApiErrorResponse.kt)
- [`Bluetape4kKtorCore`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/Bluetape4kKtorCore.kt)
- [`Bluetape4kKtorCoreConfig`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/Bluetape4kKtorCoreConfig.kt)
- [`Bluetape4kKtorJson`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/Bluetape4kKtorJson.kt)
- [`Bluetape4kStatusPages`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/Bluetape4kStatusPages.kt)
- [`HealthResponse`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/HealthResponse.kt)
- [`KtorHealthRoutes`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/KtorHealthRoutes.kt)
- [`KtorRequestParameters`](../../../../ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/KtorRequestParameters.kt)
- [`Bluetape4kKtorCoreTest`](../../../../ktor/core/src/test/kotlin/io/bluetape4k/ktor/core/Bluetape4kKtorCoreTest.kt)
