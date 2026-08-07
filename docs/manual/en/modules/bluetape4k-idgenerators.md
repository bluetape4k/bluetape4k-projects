---
manualId: bluetape4k-idgenerators
title: "Distributed ID Generators"
description: "Generates unique IDs in distributed environments using a variety of algorithms. UUID (V1–V7), ULID, KSUID, Snowflake, Flake, and Hashids are exposed through a unified IdGenerator interface."
kind: library
group: utilities
learningOrder: 1200
---

# Distributed ID Generators

## Problem {#problem}

Generates unique IDs in distributed environments using a variety of algorithms. UUID (V1–V7), ULID, KSUID, Snowflake, Flake, and Hashids are exposed through a unified IdGenerator interface. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-idgenerators` when the application needs input contracts, value semantics, algorithmic cost, and deterministic output. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-idgenerators")
}
```

Gradle project path: `:bluetape4k-idgenerators`. Source directory: `utils/idgenerators`.

## Concepts {#concepts}

The first source-level concepts to inspect are `IdGenerator`, `LongIdGenerator`, `MachineIdSupport`, `Flake`, `Hashids`, `HashidsSupport`, `BitInputStream`, and `BitOutputStream`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`IdGenerator`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/IdGenerator.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`IdGenerator`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/IdGenerator.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LongIdGenerator`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/LongIdGenerator.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MachineIdSupport`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/MachineIdSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Flake`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/flake/Flake.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Hashids`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/hashids/Hashids.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HashidsSupport`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/hashids/HashidsSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BitInputStream`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/BitInputStream.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BitOutputStream`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/BitOutputStream.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BytesBase62`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/BytesBase62.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Ksuid`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/Ksuid.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Algorithm Selection Guide**, **Architecture**, **All Algorithms at a Glance**, **Class Diagram**, **Snowflake Bit Layout**, **Supported Algorithms**, **Usage Examples**, **Snowflake (Twitter-style)**, **UUID (Unified API)**, and **ULID (Universally Unique Lexicographically Sortable Identifier)**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
api(libs.java.uuid.generator)  // https://github.com/cowtowncoder/java-uuid-generator
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
./gradlew :bluetape4k-idgenerators:test --no-configuration-cache
```

Representative test anchors:

- [`IdSupportTest`](../../../../utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/IdSupportTest.kt)
- [`FlakeTest`](../../../../utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/flake/FlakeTest.kt)
- [`HashIdsSupportTest`](../../../../utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/hashids/HashIdsSupportTest.kt)
- [`HashidsTest`](../../../../utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/hashids/HashidsTest.kt)
- [`BytesBase62Test`](../../../../utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/ksuid/BytesBase62Test.kt)
- [`KsuidEdgeCasesTest`](../../../../utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/ksuid/KsuidEdgeCasesTest.kt)
- [`KsuidGeneratorTest`](../../../../utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/ksuid/KsuidGeneratorTest.kt)
- [`KsuidMillisTest`](../../../../utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/ksuid/KsuidMillisTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### All Algorithms at a Glance diagram

[![All Algorithms at a Glance diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-01.svg)

_Release README: [`utils/idgenerators/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/idgenerators/README.md)_

### ID Generators Class Structure diagram

[![ID Generators Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-02.svg)

_Release README: [`utils/idgenerators/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/idgenerators/README.md)_

### Snowflake Bit Layout diagram

[![Snowflake Bit Layout diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-03.svg)

_Release README: [`utils/idgenerators/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/idgenerators/README.md)_

### ULID (Universally Unique Lexicographically Sortable Identif. diagram

[![ULID (Universally Unique Lexicographically Sortable Identif. diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-04.svg)

_Release README: [`utils/idgenerators/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/idgenerators/README.md)_

### ULID (Universally Unique Lexicographically Sortable Identif. diagram

[![ULID (Universally Unique Lexicographically Sortable Identif. diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-05.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-05.svg)

_Release README: [`utils/idgenerators/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/idgenerators/README.md)_

### KSUID Layout diagram

[![KSUID Layout diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-06.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-06.svg)

_Release README: [`utils/idgenerators/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/idgenerators/README.md)_

### Flake Layout diagram

[![Flake Layout diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-07.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-idgenerators-diagram-07.svg)

_Release README: [`utils/idgenerators/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/idgenerators/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../utils/idgenerators/README.md)
- [Module build](../../../../utils/idgenerators/build.gradle.kts)
- [`IdGenerator`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/IdGenerator.kt)
- [`LongIdGenerator`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/LongIdGenerator.kt)
- [`MachineIdSupport`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/MachineIdSupport.kt)
- [`Flake`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/flake/Flake.kt)
- [`Hashids`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/hashids/Hashids.kt)
- [`HashidsSupport`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/hashids/HashidsSupport.kt)
- [`BitInputStream`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/BitInputStream.kt)
- [`BitOutputStream`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/BitOutputStream.kt)
- [`BytesBase62`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/BytesBase62.kt)
- [`Ksuid`](../../../../utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ksuid/Ksuid.kt)
- [`IdSupportTest`](../../../../utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/IdSupportTest.kt)
- [`FlakeTest`](../../../../utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/flake/FlakeTest.kt)
