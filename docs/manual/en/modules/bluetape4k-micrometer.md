---
manualId: bluetape4k-micrometer
title: "Micrometer Metrics Extensions"
description: "A module that provides application performance measurement and observability features using Micrometer and the Observation API."
kind: library
group: operations
learningOrder: 1020
---

# Micrometer Metrics Extensions

## Problem {#problem}

A module that provides application performance measurement and observability features using Micrometer and the Observation API. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-micrometer` when the application needs client lifecycle, reconnect policy, backpressure, retries, and observability. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-micrometer")
}
```

Gradle project path: `:bluetape4k-micrometer`. Source directory: `infra/micrometer`.

## Concepts {#concepts}

The first source-level concepts to inspect are `KeyValueSupport`, `TimerExtensions`, `Cache2kCacheMetrics`, `MeasuredCall`, `MeasuredCallAdapter`, `MetricsRecorder`, `MicrometerRetrofitMetricsFactory`, and `MicrometerRetrofitMetricsRecorder`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`KeyValueSupport`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/common/KeyValueSupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`KeyValueSupport`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/common/KeyValueSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TimerExtensions`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/TimerExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Cache2kCacheMetrics`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/cache/Cache2kCacheMetrics.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MeasuredCall`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MeasuredCall.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MeasuredCallAdapter`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MeasuredCallAdapter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MetricsRecorder`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MetricsRecorder.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MicrometerRetrofitMetricsFactory`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MicrometerRetrofitMetricsFactory.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MicrometerRetrofitMetricsRecorder`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MicrometerRetrofitMetricsRecorder.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Outcome`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/Outcome.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RetrofitCallMetricsCollector`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/RetrofitCallMetricsCollector.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Architecture**, **Core Class Structure**, **Metric Collection Flow**, **Retrofit2 Metric Collection Sequence**, **Coroutine Observation Flow**, **Dependency**, **Key Features**, **1. Timer Extensions**, and **Measuring Execution Time of Suspend Functions**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-core"))
implementation(project(":bluetape4k-cache-core"))
api(libs.micrometer.core)
implementation(libs.micrometer.registry.prometheus)
implementation(libs.micrometer.registry.datadog)
api(libs.micrometer.observation)
implementation(libs.micrometer.observation.test)
implementation(libs.micrometer.tracing.bridge.otel)
api(libs.micrometer.context.propagation)  // thread local <-> reactor 등 상이한 환경에서 context 전파를 위해 사용
implementation(libs.cache2k.core)
implementation(project(":bluetape4k-retrofit2"))
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
./gradlew :bluetape4k-micrometer:test --no-configuration-cache
```

Representative test anchors:

- [`KeyValueSupportTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/common/KeyValueSupportTest.kt)
- [`AbstractMicrometerTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/AbstractMicrometerTest.kt)
- [`TimerExtensionsTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/TimerExtensionsTest.kt)
- [`Cache2kCacheMetricsTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/cache/Cache2kCacheMetricsTest.kt)
- [`OutcomeTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/OutcomeTest.kt)
- [`Retrofit2MetricsTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/Retrofit2MetricsTest.kt)
- [`RetrofitMetricsSupportTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/RetrofitMetricsSupportTest.kt)
- [`RetrofitMetricsUnitTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/RetrofitMetricsUnitTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Core Class Structure diagram

[![Core Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-diagram-01.svg)

_Release README: [`infra/micrometer/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/micrometer/README.md)_

### Metric Collection Flow diagram

[![Metric Collection Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-diagram-02.svg)

_Release README: [`infra/micrometer/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/micrometer/README.md)_

### Micrometer Module Architecture diagram

[![Micrometer Module Architecture diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-diagram-03.svg)

_Release README: [`infra/micrometer/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/micrometer/README.md)_

### Retrofit2 Metric Collection Sequence diagram

[![Retrofit2 Metric Collection Sequence diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-sequence-01.svg)

_Release README: [`infra/micrometer/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/micrometer/README.md)_

### Coroutine Observation Flow diagram

[![Coroutine Observation Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-sequence-02.svg)

_Release README: [`infra/micrometer/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/micrometer/README.md)_

### Event Telemetry Sequence diagram

[![Event Telemetry Sequence diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-sequence-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-micrometer-sequence-03.svg)

_Release README: [`infra/micrometer/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/micrometer/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../infra/micrometer/README.md)
- [Module build](../../../../infra/micrometer/build.gradle.kts)
- [`KeyValueSupport`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/common/KeyValueSupport.kt)
- [`TimerExtensions`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/TimerExtensions.kt)
- [`Cache2kCacheMetrics`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/cache/Cache2kCacheMetrics.kt)
- [`MeasuredCall`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MeasuredCall.kt)
- [`MeasuredCallAdapter`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MeasuredCallAdapter.kt)
- [`MetricsRecorder`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MetricsRecorder.kt)
- [`MicrometerRetrofitMetricsFactory`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MicrometerRetrofitMetricsFactory.kt)
- [`MicrometerRetrofitMetricsRecorder`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/MicrometerRetrofitMetricsRecorder.kt)
- [`Outcome`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/Outcome.kt)
- [`RetrofitCallMetricsCollector`](../../../../infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/RetrofitCallMetricsCollector.kt)
- [`KeyValueSupportTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/common/KeyValueSupportTest.kt)
- [`AbstractMicrometerTest`](../../../../infra/micrometer/src/test/kotlin/io/bluetape4k/micrometer/instrument/AbstractMicrometerTest.kt)
