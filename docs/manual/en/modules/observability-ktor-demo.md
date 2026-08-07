---
manualId: observability-ktor-demo
title: "Ktor Observability Example"
description: "Runnable Ktor 3 application that shows application-owned Prometheus metrics routing, opt-in OpenTelemetry tracing, and bluetape4k event telemetry helpers."
kind: example
group: examples
learningOrder: 1490
---

# Ktor Observability Example

## Problem {#problem}

Runnable Ktor 3 application that shows application-owned Prometheus metrics routing, opt-in OpenTelemetry tracing, and bluetape4k event telemetry helpers. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `observability-ktor-demo` when the application needs the runnable entry point, required services, expected behavior, and the production pattern demonstrated. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

This example project is not published as a Maven artifact. Run it from the repository and inspect its Gradle tasks before choosing a command.

Gradle project path: `:observability-ktor-demo`. Source directory: `examples/ktor/observability-ktor-demo`.

## Concepts {#concepts}

The first source-level concepts to inspect are `ObservabilityKtorApplication`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

List the project tasks before running the example or benchmark:

```bash
./gradlew :observability-ktor-demo:tasks --all
```

Then use the command documented by the module README and keep required external services isolated.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`ObservabilityKtorApplication`](../../../../examples/ktor/observability-ktor-demo/src/main/kotlin/io/bluetape4k/examples/ktor/observability/ObservabilityKtorApplication.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Example Scenario**, **Architecture**, **Sequence Diagram**, **Dependencies**, **Configuration**, **Run**, **Verify**, and **Test**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(project(":bluetape4k-ktor-core"))
implementation(project(":bluetape4k-ktor-observability"))
implementation(project(":bluetape4k-micrometer"))
implementation(libs.ktor.server.core)
implementation(libs.ktor.server.cio)
implementation(libs.micrometer.registry.prometheus)
implementation(libs.opentelemetry.api)
runtimeOnly(libs.logback.classic)
runtimeOnly(libs.opentelemetry.ktor)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Run the example in an isolated environment and observe startup, dependency health, requests, and shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :observability-ktor-demo:test --no-configuration-cache
```

Representative test anchors:

- [`ObservabilityKtorApplicationTest`](../../../../examples/ktor/observability-ktor-demo/src/test/kotlin/io/bluetape4k/examples/ktor/observability/ObservabilityKtorApplicationTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Ktor observability demo architecture

[![Ktor observability demo architecture](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-ktor-observability-ktor-demo-architecture-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-ktor-observability-ktor-demo-architecture-01.svg)

_Release README: [`examples/ktor/observability-ktor-demo/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/examples/ktor/observability-ktor-demo/README.md)_

### Ktor observability demo sequence

[![Ktor observability demo sequence](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-ktor-observability-ktor-demo-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-ktor-observability-ktor-demo-sequence-01.svg)

_Release README: [`examples/ktor/observability-ktor-demo/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/examples/ktor/observability-ktor-demo/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../examples/ktor/observability-ktor-demo/README.md)
- [Module build](../../../../examples/ktor/observability-ktor-demo/build.gradle.kts)
- [`ObservabilityKtorApplication`](../../../../examples/ktor/observability-ktor-demo/src/main/kotlin/io/bluetape4k/examples/ktor/observability/ObservabilityKtorApplication.kt)
- [`ObservabilityKtorApplicationTest`](../../../../examples/ktor/observability-ktor-demo/src/test/kotlin/io/bluetape4k/examples/ktor/observability/ObservabilityKtorApplicationTest.kt)
