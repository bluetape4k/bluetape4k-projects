---
manualId: bluetape4k-mutiny
title: "Mutiny Reactive Extensions"
description: "Provides extension functions and utilities that make the SmallRye Mutiny reactive library easier to use in Kotlin."
kind: library
group: concurrency
learningOrder: 210
---

# Mutiny Reactive Extensions

## Problem {#problem}

Provides extension functions and utilities that make the SmallRye Mutiny reactive library easier to use in Kotlin. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-mutiny` when the application needs input contracts, value semantics, algorithmic cost, and deterministic output. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-mutiny")
}
```

Gradle project path: `:bluetape4k-mutiny`. Source directory: `utils/mutiny`.

## Concepts {#concepts}

The first source-level concepts to inspect are `CoroutineSupport`, `MultiSupport`, and `UniSupport`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`CoroutineSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/CoroutineSupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`CoroutineSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/CoroutineSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MultiSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/MultiSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`UniSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/UniSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Adding the Dependency**, **Key Features**, **Mutiny Type Diagram**, **Usage Examples**, **Create Uni**, **Transform Uni**, **Create Multi**, **Transform Multi**, and **Coroutine Interop**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
api(libs.mutiny)
api(libs.mutiny.kotlin)
api(project(":bluetape4k-coroutines"))
api(libs.kotlinx.coroutines.core)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Measure hot paths, bound input sizes, and monitor failures at the application boundary that calls the utility. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-mutiny:test --no-configuration-cache
```

Representative test anchors:

- [`CoroutineSupportTest`](../../../../utils/mutiny/src/test/kotlin/io/bluetape4k/mutiny/CoroutineSupportTest.kt)
- [`MultiSupportTest`](../../../../utils/mutiny/src/test/kotlin/io/bluetape4k/mutiny/MultiSupportTest.kt)
- [`UniSupportTest`](../../../../utils/mutiny/src/test/kotlin/io/bluetape4k/mutiny/UniSupportTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Mutiny Type Diagram diagram

[![Mutiny Type Diagram diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-mutiny-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-mutiny-diagram-01.svg)

_Release README: [`utils/mutiny/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/mutiny/README.md)_

### Mutiny Processing Flow diagram

[![Mutiny Processing Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-mutiny-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-mutiny-diagram-02.svg)

_Release README: [`utils/mutiny/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/mutiny/README.md)_

### Coroutine Interop Flow diagram

[![Coroutine Interop Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-mutiny-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-mutiny-sequence-01.svg)

_Release README: [`utils/mutiny/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/mutiny/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../utils/mutiny/README.md)
- [Module build](../../../../utils/mutiny/build.gradle.kts)
- [`CoroutineSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/CoroutineSupport.kt)
- [`MultiSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/MultiSupport.kt)
- [`UniSupport`](../../../../utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/UniSupport.kt)
- [`CoroutineSupportTest`](../../../../utils/mutiny/src/test/kotlin/io/bluetape4k/mutiny/CoroutineSupportTest.kt)
- [`MultiSupportTest`](../../../../utils/mutiny/src/test/kotlin/io/bluetape4k/mutiny/MultiSupportTest.kt)
- [`UniSupportTest`](../../../../utils/mutiny/src/test/kotlin/io/bluetape4k/mutiny/UniSupportTest.kt)
