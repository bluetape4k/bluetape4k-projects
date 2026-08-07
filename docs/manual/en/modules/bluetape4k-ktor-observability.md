---
manualId: bluetape4k-ktor-observability
title: "Ktor Observability"
description: "Explicit Ktor observability defaults for bluetape4k applications."
kind: library
group: web
learningOrder: 840
---

# Ktor Observability

## Problem {#problem}

Explicit Ktor observability defaults for bluetape4k applications. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-ktor-observability` when the application needs request lifecycle, cancellation, routing, context propagation, and test boundaries. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-ktor-observability")
}
```

Gradle project path: `:bluetape4k-ktor-observability`. Source directory: `ktor/observability`.

## Concepts {#concepts}

The first source-level concepts to inspect are `Bluetape4kKtorObservability`, `Bluetape4kKtorObservabilityConfig`, `CallLoggingSettings`, `CorrelationIdSettings`, `KtorCallIdSupport`, `KtorCallLoggingSupport`, `KtorOpenTelemetryTracingConfig`, and `KtorOpenTelemetryTracingSupport`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`Bluetape4kKtorObservability`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/Bluetape4kKtorObservability.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`Bluetape4kKtorObservability`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/Bluetape4kKtorObservability.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Bluetape4kKtorObservabilityConfig`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/Bluetape4kKtorObservabilityConfig.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CallLoggingSettings`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/CallLoggingSettings.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CorrelationIdSettings`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/CorrelationIdSettings.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KtorCallIdSupport`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/KtorCallIdSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KtorCallLoggingSupport`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/KtorCallLoggingSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KtorOpenTelemetryTracingConfig`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/KtorOpenTelemetryTracingConfig.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KtorOpenTelemetryTracingSupport`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/KtorOpenTelemetryTracingSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KtorPrometheusRoutes`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/KtorPrometheusRoutes.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Component Diagram**, **Features**, **Dependency**, **Micrometer and Prometheus Usage**, **OpenTelemetry Tracing Usage**, and **Dependency Policy**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-ktor-core"))
api(libs.ktor.server.core)
api(libs.ktor.server.call.id)
api(libs.ktor.server.call.logging)
api(libs.ktor.server.metrics.micrometer)
api(libs.micrometer.core)
api(libs.opentelemetry.api)
compileOnly(libs.micrometer.registry.prometheus)
compileOnly(libs.opentelemetry.ktor)
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
./gradlew :bluetape4k-ktor-observability:test --no-configuration-cache
```

Representative test anchors:

- [`Bluetape4kKtorObservabilityTest`](../../../../ktor/observability/src/test/kotlin/io/bluetape4k/ktor/observability/Bluetape4kKtorObservabilityTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Ktor Observability Components

[![Ktor Observability Components](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/ktor-observability-component-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/ktor-observability-component-01.svg)

_Release README: [`ktor/observability/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/ktor/observability/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../ktor/observability/README.md)
- [Module build](../../../../ktor/observability/build.gradle.kts)
- [`Bluetape4kKtorObservability`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/Bluetape4kKtorObservability.kt)
- [`Bluetape4kKtorObservabilityConfig`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/Bluetape4kKtorObservabilityConfig.kt)
- [`CallLoggingSettings`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/CallLoggingSettings.kt)
- [`CorrelationIdSettings`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/CorrelationIdSettings.kt)
- [`KtorCallIdSupport`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/KtorCallIdSupport.kt)
- [`KtorCallLoggingSupport`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/KtorCallLoggingSupport.kt)
- [`KtorOpenTelemetryTracingConfig`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/KtorOpenTelemetryTracingConfig.kt)
- [`KtorOpenTelemetryTracingSupport`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/KtorOpenTelemetryTracingSupport.kt)
- [`KtorPrometheusRoutes`](../../../../ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/KtorPrometheusRoutes.kt)
- [`Bluetape4kKtorObservabilityTest`](../../../../ktor/observability/src/test/kotlin/io/bluetape4k/ktor/observability/Bluetape4kKtorObservabilityTest.kt)
