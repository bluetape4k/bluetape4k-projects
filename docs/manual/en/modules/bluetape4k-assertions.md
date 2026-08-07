---
manualId: bluetape4k-assertions
title: "Fluent Test Assertions"
description: "한국어"
kind: library
group: testing
learningOrder: 1110
---

# Fluent Test Assertions

## Problem {#problem}

한국어 This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-assertions` when the application needs fixture ownership, isolation, deterministic cleanup, and failure diagnostics. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-assertions")
}
```

Gradle project path: `:bluetape4k-assertions`. Source directory: `testing/assertions`.

## Concepts {#concepts}

The first source-level concepts to inspect are `Arrays`, `Basic`, `CharSequences`, `Collections`, `DateTimesInstantZoned`, `DateTimesLegacy`, `DateTimesLocal`, and `Exceptions`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`Arrays`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Arrays.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`Arrays`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Arrays.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Basic`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Basic.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CharSequences`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/CharSequences.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Collections`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Collections.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DateTimesInstantZoned`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/DateTimesInstantZoned.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DateTimesLegacy`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/DateTimesLegacy.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DateTimesLocal`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/DateTimesLocal.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Exceptions`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Exceptions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Maps`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Maps.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Numerical`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Numerical.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Features**, **Quick Start**, **Gradle**, **Example Usage**, **API Reference**, **Basic Assertions**, **Numerical**, **Collections & Arrays**, and **Exceptions**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(platform(libs.junit.bom))
api(libs.junit.jupiter.api)
api(libs.kotlinx.coroutines.core)
compileOnly(libs.turbine)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Keep fixtures isolated, bound resource use, expose diagnostics, and close shared services deterministically. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-assertions:test --no-configuration-cache
```

Representative test anchors:

- [`ArraysTest`](../../../../testing/assertions/src/test/kotlin/io/bluetape4k/assertions/ArraysTest.kt)
- [`BasicTest`](../../../../testing/assertions/src/test/kotlin/io/bluetape4k/assertions/BasicTest.kt)
- [`CharSequencesTest`](../../../../testing/assertions/src/test/kotlin/io/bluetape4k/assertions/CharSequencesTest.kt)
- [`CollectionsTest`](../../../../testing/assertions/src/test/kotlin/io/bluetape4k/assertions/CollectionsTest.kt)
- [`CompatibilitySmokeTest`](../../../../testing/assertions/src/test/kotlin/io/bluetape4k/assertions/CompatibilitySmokeTest.kt)
- [`DateTimesTest`](../../../../testing/assertions/src/test/kotlin/io/bluetape4k/assertions/DateTimesTest.kt)
- [`ExceptionsTest`](../../../../testing/assertions/src/test/kotlin/io/bluetape4k/assertions/ExceptionsTest.kt)
- [`MapsTest`](../../../../testing/assertions/src/test/kotlin/io/bluetape4k/assertions/MapsTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### assertions Architecture diagram

[![assertions Architecture diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-assertions-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-assertions-diagram-01.svg)

_Release README: [`testing/assertions/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/assertions/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../testing/assertions/README.md)
- [Module build](../../../../testing/assertions/build.gradle.kts)
- [`Arrays`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Arrays.kt)
- [`Basic`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Basic.kt)
- [`CharSequences`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/CharSequences.kt)
- [`Collections`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Collections.kt)
- [`DateTimesInstantZoned`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/DateTimesInstantZoned.kt)
- [`DateTimesLegacy`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/DateTimesLegacy.kt)
- [`DateTimesLocal`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/DateTimesLocal.kt)
- [`Exceptions`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Exceptions.kt)
- [`Maps`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Maps.kt)
- [`Numerical`](../../../../testing/assertions/src/main/kotlin/io/bluetape4k/assertions/Numerical.kt)
- [`ArraysTest`](../../../../testing/assertions/src/test/kotlin/io/bluetape4k/assertions/ArraysTest.kt)
- [`BasicTest`](../../../../testing/assertions/src/test/kotlin/io/bluetape4k/assertions/BasicTest.kt)
