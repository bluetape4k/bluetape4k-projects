---
manualId: bluetape4k-ktor-resilience4j
title: "Ktor Resilience4j Integration"
description: "Route-scoped Resilience4j helpers for Ktor server applications in the bluetape4k ecosystem."
kind: library
group: web
learningOrder: 830
---

# Ktor Resilience4j Integration

## Problem {#problem}

Route-scoped Resilience4j helpers for Ktor server applications in the bluetape4k ecosystem. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-ktor-resilience4j` when the application needs request lifecycle, cancellation, routing, context propagation, and test boundaries. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-ktor-resilience4j")
}
```

Gradle project path: `:bluetape4k-ktor-resilience4j`. Source directory: `ktor/resilience4j`.

## Concepts {#concepts}

The first source-level concepts to inspect are `KtorResiliencePolicies`, `KtorResilienceStatusPages`, and `KtorResilienceSupport`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`KtorResiliencePolicies`](../../../../ktor/resilience4j/src/main/kotlin/io/bluetape4k/ktor/resilience4j/KtorResiliencePolicies.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`KtorResiliencePolicies`](../../../../ktor/resilience4j/src/main/kotlin/io/bluetape4k/ktor/resilience4j/KtorResiliencePolicies.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KtorResilienceStatusPages`](../../../../ktor/resilience4j/src/main/kotlin/io/bluetape4k/ktor/resilience4j/KtorResilienceStatusPages.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KtorResilienceSupport`](../../../../ktor/resilience4j/src/main/kotlin/io/bluetape4k/ktor/resilience4j/KtorResilienceSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Route Flow Diagram**, **Features**, **Dependency**, **Usage**, **Error Mapping**, and **Non-goals**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-ktor-core"))
api(project(":bluetape4k-resilience4j"))
api(libs.ktor.server.core)
api(libs.ktor.server.status.pages)
implementation(libs.kotlinx.coroutines.core)
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
./gradlew :bluetape4k-ktor-resilience4j:test --no-configuration-cache
```

Representative test anchors:

- [`KtorResilienceSupportTest`](../../../../ktor/resilience4j/src/test/kotlin/io/bluetape4k/ktor/resilience4j/KtorResilienceSupportTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Ktor Resilience4j Route Flow

[![Ktor Resilience4j Route Flow](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/ktor-resilience4j-flow-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/ktor-resilience4j-flow-01.svg)

_Release README: [`ktor/resilience4j/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/ktor/resilience4j/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../ktor/resilience4j/README.md)
- [Module build](../../../../ktor/resilience4j/build.gradle.kts)
- [`KtorResiliencePolicies`](../../../../ktor/resilience4j/src/main/kotlin/io/bluetape4k/ktor/resilience4j/KtorResiliencePolicies.kt)
- [`KtorResilienceStatusPages`](../../../../ktor/resilience4j/src/main/kotlin/io/bluetape4k/ktor/resilience4j/KtorResilienceStatusPages.kt)
- [`KtorResilienceSupport`](../../../../ktor/resilience4j/src/main/kotlin/io/bluetape4k/ktor/resilience4j/KtorResilienceSupport.kt)
- [`KtorResilienceSupportTest`](../../../../ktor/resilience4j/src/test/kotlin/io/bluetape4k/ktor/resilience4j/KtorResilienceSupportTest.kt)
