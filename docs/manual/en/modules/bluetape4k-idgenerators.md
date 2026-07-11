---
manualId: bluetape4k-idgenerators
title: "bluetape4k-idgenerators"
description: "Generates unique IDs in distributed environments using a variety of algorithms. UUID (V1–V7), ULID, KSUID, Snowflake, Flake, and Hashids are exposed through a unified IdGenerator…"
kind: library
group: utilities
---

# bluetape4k-idgenerators

## Problem {#problem}

Generates unique IDs in distributed environments using a variety of algorithms. UUID (V1–V7), ULID, KSUID, Snowflake, Flake, and Hashids are exposed through a unified IdGenerator… This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-idgenerators` when the application needs input contracts, value semantics, algorithmic cost, and deterministic output. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
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
