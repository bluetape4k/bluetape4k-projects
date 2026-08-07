---
manualId: bluetape4k-measured
title: "Units of Measure"
description: "bluetape4k-measured represents compound units such as m/s and kgm/s^2 in a type-safe way, based on composable unit types (Units) and measured values (Measure)."
kind: library
group: utilities
learningOrder: 1240
---

# Units of Measure

## Problem {#problem}

bluetape4k-measured represents compound units such as m/s and kgm/s^2 in a type-safe way, based on composable unit types (Units) and measured values (Measure). This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-measured` when the application needs input contracts, value semantics, algorithmic cost, and deterministic output. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-measured")
}
```

Gradle project path: `:bluetape4k-measured`. Source directory: `utils/measured`.

## Concepts {#concepts}

The first source-level concepts to inspect are `Angle`, `Area`, `BinarySize`, `EnergyPower`, `Frequency`, `GraphicsLength`, `Length`, and `Mass`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`Angle`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Angle.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`Angle`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Angle.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Area`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Area.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BinarySize`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/BinarySize.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`EnergyPower`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/EnergyPower.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Frequency`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Frequency.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`GraphicsLength`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/GraphicsLength.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Length`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Length.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Mass`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Mass.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Motion`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Motion.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Pressure`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Pressure.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Core Concepts**, **Provided Units**, **Quick Example**, **Test**, **Class Diagram**, **Unit Composition Flow**, and **Compatibility Adapter for units**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
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
./gradlew :bluetape4k-measured:test --no-configuration-cache
```

Representative test anchors:

- [`AngleTest`](../../../../utils/measured/src/test/kotlin/io/bluetape4k/measured/AngleTest.kt)
- [`AreaTest`](../../../../utils/measured/src/test/kotlin/io/bluetape4k/measured/AreaTest.kt)
- [`BinarySizeTest`](../../../../utils/measured/src/test/kotlin/io/bluetape4k/measured/BinarySizeTest.kt)
- [`EnergyPowerTest`](../../../../utils/measured/src/test/kotlin/io/bluetape4k/measured/EnergyPowerTest.kt)
- [`FrequencyTest`](../../../../utils/measured/src/test/kotlin/io/bluetape4k/measured/FrequencyTest.kt)
- [`GraphicsLengthTest`](../../../../utils/measured/src/test/kotlin/io/bluetape4k/measured/GraphicsLengthTest.kt)
- [`LengthTest`](../../../../utils/measured/src/test/kotlin/io/bluetape4k/measured/LengthTest.kt)
- [`MassTest`](../../../../utils/measured/src/test/kotlin/io/bluetape4k/measured/MassTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### measured Class Structure diagram

[![measured Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-measured-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-measured-diagram-01.svg)

_Release README: [`utils/measured/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/measured/README.md)_

### Unit Composition Flow diagram

[![Unit Composition Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-measured-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-measured-diagram-02.svg)

_Release README: [`utils/measured/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/measured/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../utils/measured/README.md)
- [Module build](../../../../utils/measured/build.gradle.kts)
- [`Angle`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Angle.kt)
- [`Area`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Area.kt)
- [`BinarySize`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/BinarySize.kt)
- [`EnergyPower`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/EnergyPower.kt)
- [`Frequency`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Frequency.kt)
- [`GraphicsLength`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/GraphicsLength.kt)
- [`Length`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Length.kt)
- [`Mass`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Mass.kt)
- [`Motion`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Motion.kt)
- [`Pressure`](../../../../utils/measured/src/main/kotlin/io/bluetape4k/measured/Pressure.kt)
- [`AngleTest`](../../../../utils/measured/src/test/kotlin/io/bluetape4k/measured/AngleTest.kt)
- [`AreaTest`](../../../../utils/measured/src/test/kotlin/io/bluetape4k/measured/AreaTest.kt)
