---
manualId: bluetape4k-javatimes
title: "Java Time Utilities"
description: "An advanced time-operations library for the Java Time API (java.time)."
kind: library
group: utilities
learningOrder: 1210
---

# Java Time Utilities

## Problem {#problem}

An advanced time-operations library for the Java Time API (java.time). This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-javatimes` when the application needs input contracts, value semantics, algorithmic cost, and deterministic output. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-javatimes")
}
```

Gradle project path: `:bluetape4k-javatimes`. Source directory: `utils/javatimes`.

## Concepts {#concepts}

The first source-level concepts to inspect are `AbstractTemporalInterval`, `IntervalTypealias`, `MutableTemporalInterval`, `ReadableTemporalInterval`, `TemporalInterval`, `TemporalIntervalSupport`, `TemporalIntervalWindowed`, and `ITimeBlock`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`AbstractTemporalInterval`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/AbstractTemporalInterval.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`AbstractTemporalInterval`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/AbstractTemporalInterval.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`IntervalTypealias`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/IntervalTypealias.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MutableTemporalInterval`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/MutableTemporalInterval.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ReadableTemporalInterval`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/ReadableTemporalInterval.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TemporalInterval`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/TemporalInterval.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TemporalIntervalSupport`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/TemporalIntervalSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TemporalIntervalWindowed`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/TemporalIntervalWindowed.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ITimeBlock`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/period/ITimeBlock.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ITimeCalendar`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/period/ITimeCalendar.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ITimePeriod`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/period/ITimePeriod.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Feature Overview**, **Class Hierarchy — Period Framework**, **PeriodRelation — How Two Periods Relate**, **Core Features (from bluetape4k-core)**, **Features**, **Temporal Interval (interval/)**, **Period Framework (period/)**, **TimeBlock and TimeRange**, and **DateAdd — Business Day Calculations**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
implementation(project(":bluetape4k-coroutines"))
implementation(libs.kotlinx.coroutines.core)
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
./gradlew :bluetape4k-javatimes:test --no-configuration-cache
```

Representative test anchors:

- [`TemporalIntervalSupportTest`](../../../../utils/javatimes/src/test/kotlin/io/bluetape4k/javatimes/interval/TemporalIntervalSupportTest.kt)
- [`TemporalIntervalTest`](../../../../utils/javatimes/src/test/kotlin/io/bluetape4k/javatimes/interval/TemporalIntervalTest.kt)
- [`TemporalIntervalWindowedTest`](../../../../utils/javatimes/src/test/kotlin/io/bluetape4k/javatimes/interval/TemporalIntervalWindowedTest.kt)
- [`AbstractPeriodTest`](../../../../utils/javatimes/src/test/kotlin/io/bluetape4k/javatimes/period/AbstractPeriodTest.kt)
- [`PeriodRelationTest`](../../../../utils/javatimes/src/test/kotlin/io/bluetape4k/javatimes/period/PeriodRelationTest.kt)
- [`TimeBlockTest`](../../../../utils/javatimes/src/test/kotlin/io/bluetape4k/javatimes/period/TimeBlockTest.kt)
- [`TimeCalendarConfigTest`](../../../../utils/javatimes/src/test/kotlin/io/bluetape4k/javatimes/period/TimeCalendarConfigTest.kt)
- [`TimeCalendarTest`](../../../../utils/javatimes/src/test/kotlin/io/bluetape4k/javatimes/period/TimeCalendarTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Feature Overview diagram

[![Feature Overview diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-javatimes-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-javatimes-diagram-01.svg)

_Release README: [`utils/javatimes/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/javatimes/README.md)_

### Class Hierarchy — Period Framework diagram

[![Class Hierarchy — Period Framework diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-javatimes-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-javatimes-diagram-02.svg)

_Release README: [`utils/javatimes/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/javatimes/README.md)_

### PeriodRelation — How Two Periods Relate diagram

[![PeriodRelation — How Two Periods Relate diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-javatimes-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-javatimes-diagram-03.svg)

_Release README: [`utils/javatimes/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/javatimes/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../utils/javatimes/README.md)
- [Module build](../../../../utils/javatimes/build.gradle.kts)
- [`AbstractTemporalInterval`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/AbstractTemporalInterval.kt)
- [`IntervalTypealias`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/IntervalTypealias.kt)
- [`MutableTemporalInterval`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/MutableTemporalInterval.kt)
- [`ReadableTemporalInterval`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/ReadableTemporalInterval.kt)
- [`TemporalInterval`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/TemporalInterval.kt)
- [`TemporalIntervalSupport`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/TemporalIntervalSupport.kt)
- [`TemporalIntervalWindowed`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/interval/TemporalIntervalWindowed.kt)
- [`ITimeBlock`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/period/ITimeBlock.kt)
- [`ITimeCalendar`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/period/ITimeCalendar.kt)
- [`ITimePeriod`](../../../../utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/period/ITimePeriod.kt)
- [`TemporalIntervalSupportTest`](../../../../utils/javatimes/src/test/kotlin/io/bluetape4k/javatimes/interval/TemporalIntervalSupportTest.kt)
- [`TemporalIntervalTest`](../../../../utils/javatimes/src/test/kotlin/io/bluetape4k/javatimes/interval/TemporalIntervalTest.kt)
