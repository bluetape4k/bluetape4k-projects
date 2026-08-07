---
manualId: bluetape4k-math
title: "Mathematical Utilities"
description: "A library providing a wide range of mathematical capabilities including statistical operations, interpolation, integration, equation solving, and clustering — built on Apache Commons Math3."
kind: library
group: utilities
learningOrder: 1230
---

# Mathematical Utilities

## Problem {#problem}

A library providing a wide range of mathematical capabilities including statistical operations, interpolation, integration, equation solving, and clustering — built on Apache Commons Math3. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-math` when the application needs input contracts, value semantics, algorithmic cost, and deterministic output. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-math")
}
```

Gradle project path: `:bluetape4k-math`. Source directory: `utils/math`.

## Concepts {#concepts}

The first source-level concepts to inspect are `Aggregation`, `BigDecimalHistogram`, `BigDecimalStatistics`, `CategoricalStatistics`, `ComparableHistogram`, `ComparableStatistics`, `Descriptives`, and `DoubleHistogram`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`Aggregation`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/Aggregation.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`Aggregation`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/Aggregation.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BigDecimalHistogram`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/BigDecimalHistogram.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BigDecimalStatistics`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/BigDecimalStatistics.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CategoricalStatistics`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/CategoricalStatistics.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ComparableHistogram`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/ComparableHistogram.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ComparableStatistics`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/ComparableStatistics.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Descriptives`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/Descriptives.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DoubleHistogram`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/DoubleHistogram.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DoubleStatistics`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/DoubleStatistics.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`GroupingSupport`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/GroupingSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Feature Structure**, **Class Diagram**, **Key Features**, **Statistics and Descriptive Statistics**, **Mathematical Functions**, **Interpolation and Integration**, **Equation Solving**, **Linear Algebra**, and **Machine Learning**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
compileOnly(project(":bluetape4k-cache-core"))
api(libs.commons.math3)
api(libs.commons.collections4)
compileOnly(libs.commons.digest3)
compileOnly(libs.commons.rng.simple)
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
./gradlew :bluetape4k-math:test --no-configuration-cache
```

Representative test anchors:

- [`AggregationTest`](../../../../utils/math/src/test/kotlin/io/bluetape4k/math/AggregationTest.kt)
- [`BigDecimalHistogramTest`](../../../../utils/math/src/test/kotlin/io/bluetape4k/math/BigDecimalHistogramTest.kt)
- [`BigDecimalStatisticsTest`](../../../../utils/math/src/test/kotlin/io/bluetape4k/math/BigDecimalStatisticsTest.kt)
- [`CategoricalStatisticsTest`](../../../../utils/math/src/test/kotlin/io/bluetape4k/math/CategoricalStatisticsTest.kt)
- [`ComparableHistogramTest`](../../../../utils/math/src/test/kotlin/io/bluetape4k/math/ComparableHistogramTest.kt)
- [`ComparableStatisticsTest`](../../../../utils/math/src/test/kotlin/io/bluetape4k/math/ComparableStatisticsTest.kt)
- [`DescriptivesTest`](../../../../utils/math/src/test/kotlin/io/bluetape4k/math/DescriptivesTest.kt)
- [`DoubleHistogramTest`](../../../../utils/math/src/test/kotlin/io/bluetape4k/math/DoubleHistogramTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Feature Structure diagram

[![Feature Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-math-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-math-diagram-01.svg)

_Release README: [`utils/math/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/math/README.md)_

### Math Class Structure diagram

[![Math Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-math-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-math-diagram-02.svg)

_Release README: [`utils/math/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/math/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../utils/math/README.md)
- [Module build](../../../../utils/math/build.gradle.kts)
- [`Aggregation`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/Aggregation.kt)
- [`BigDecimalHistogram`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/BigDecimalHistogram.kt)
- [`BigDecimalStatistics`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/BigDecimalStatistics.kt)
- [`CategoricalStatistics`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/CategoricalStatistics.kt)
- [`ComparableHistogram`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/ComparableHistogram.kt)
- [`ComparableStatistics`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/ComparableStatistics.kt)
- [`Descriptives`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/Descriptives.kt)
- [`DoubleHistogram`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/DoubleHistogram.kt)
- [`DoubleStatistics`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/DoubleStatistics.kt)
- [`GroupingSupport`](../../../../utils/math/src/main/kotlin/io/bluetape4k/math/GroupingSupport.kt)
- [`AggregationTest`](../../../../utils/math/src/test/kotlin/io/bluetape4k/math/AggregationTest.kt)
- [`BigDecimalHistogramTest`](../../../../utils/math/src/test/kotlin/io/bluetape4k/math/BigDecimalHistogramTest.kt)
