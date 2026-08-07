---
manualId: bluetape4k-opentelemetry
title: "OpenTelemetry Tracing"
description: "OpenTelemetry is an observability framework for cloud-native software. This module provides Kotlin extension functions and utilities that make it easier and more idiomatic to use OpenTelemetry on the JVM."
kind: library
group: operations
learningOrder: 1030
---

# OpenTelemetry Tracing

## Problem {#problem}

OpenTelemetry is an observability framework for cloud-native software. This module provides Kotlin extension functions and utilities that make it easier and more idiomatic to use OpenTelemetry on the JVM. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-opentelemetry` when the application needs client lifecycle, reconnect policy, backpressure, retries, and observability. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-opentelemetry")
}
```

Gradle project path: `:bluetape4k-opentelemetry`. Source directory: `infra/opentelemetry`.

## Concepts {#concepts}

The first source-level concepts to inspect are `ContextExtensions`, `OpenTelemetrySupport`, `AttributeKeySupport`, `AttributesSupport`, `CompletableResultCodeSupport`, `ContextCoroutineSupport`, `FlowSpanSupport`, and `SpanCoroutineSupport`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`ContextExtensions`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/ContextExtensions.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`ContextExtensions`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/ContextExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`OpenTelemetrySupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/OpenTelemetrySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AttributeKeySupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/common/AttributeKeySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AttributesSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/common/AttributesSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CompletableResultCodeSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/CompletableResultCodeSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ContextCoroutineSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/ContextCoroutineSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`FlowSpanSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/FlowSpanSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SpanCoroutineSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/SpanCoroutineSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MeterProviderSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/metrics/MeterProviderSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MetricExporterSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/metrics/MetricExporterSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Features**, **Architecture Diagrams**, **Core Class Structure**, **Component Overview**, **Span Lifecycle in a Coroutine Context**, **Distributed Trace Propagation**, **Dependency**, **Key Features**, **1. OpenTelemetry SDK Setup**, and **2. Creating Tracers and Managing Spans**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-io"))
implementation(project(":bluetape4k-netty"))
api(libs.opentelemetry.api)
api(libs.opentelemetry.sdk)
api(libs.opentelemetry.extension.kotlin)
compileOnly(libs.opentelemetry.sdk.extensions.autoconfigure)
compileOnly(libs.opentelemetry.sdk.metrics)
compileOnly(libs.opentelemetry.sdk.logs)
compileOnly(libs.opentelemetry.sdk.trace)
compileOnly(libs.opentelemetry.sdk.testing)
compileOnly(libs.opentelemetry.exporter.logging)
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
./gradlew :bluetape4k-opentelemetry:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractOtelTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/AbstractOtelTest.kt)
- [`RedactionAssertions`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/RedactionAssertions.kt)
- [`AttributeKeySupportTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/common/AttributeKeySupportTest.kt)
- [`AttributesSupportTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/common/AttributesSupportTest.kt)
- [`CoroutineSupportTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/coroutines/CoroutineSupportTest.kt)
- [`FlowSpanSupportTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/coroutines/FlowSpanSupportTest.kt)
- [`SpanCoroutineSupportTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/coroutines/SpanCoroutineSupportTest.kt)
- [`TracerWithSpanTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/coroutines/TracerWithSpanTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Core Class Structure diagram

[![Core Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-diagram-01.svg)

_Release README: [`infra/opentelemetry/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/opentelemetry/README.md)_

### Component Overview diagram

[![Component Overview diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-diagram-02.svg)

_Release README: [`infra/opentelemetry/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/opentelemetry/README.md)_

### Distributed Trace Propagation diagram

[![Distributed Trace Propagation diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-diagram-03.svg)

_Release README: [`infra/opentelemetry/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/opentelemetry/README.md)_

### Span Lifecycle in a Coroutine Context diagram

[![Span Lifecycle in a Coroutine Context diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-opentelemetry-sequence-01.svg)

_Release README: [`infra/opentelemetry/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/opentelemetry/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../infra/opentelemetry/README.md)
- [Module build](../../../../infra/opentelemetry/build.gradle.kts)
- [`ContextExtensions`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/ContextExtensions.kt)
- [`OpenTelemetrySupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/OpenTelemetrySupport.kt)
- [`AttributeKeySupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/common/AttributeKeySupport.kt)
- [`AttributesSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/common/AttributesSupport.kt)
- [`CompletableResultCodeSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/CompletableResultCodeSupport.kt)
- [`ContextCoroutineSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/ContextCoroutineSupport.kt)
- [`FlowSpanSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/FlowSpanSupport.kt)
- [`SpanCoroutineSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/coroutines/SpanCoroutineSupport.kt)
- [`MeterProviderSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/metrics/MeterProviderSupport.kt)
- [`MetricExporterSupport`](../../../../infra/opentelemetry/src/main/kotlin/io/bluetape4k/opentelemetry/metrics/MetricExporterSupport.kt)
- [`AbstractOtelTest`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/AbstractOtelTest.kt)
- [`RedactionAssertions`](../../../../infra/opentelemetry/src/test/kotlin/io/bluetape4k/opentelemetry/RedactionAssertions.kt)
