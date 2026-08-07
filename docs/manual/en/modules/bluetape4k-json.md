---
manualId: bluetape4k-json
title: "JSON Abstractions"
description: "bluetape4k-json defines the small JSON serialization SPI shared by the Jackson 2, Jackson 3, and Fastjson2 modules."
kind: library
group: io
learningOrder: 330
---

# JSON Abstractions

## Problem {#problem}

bluetape4k-json defines the small JSON serialization SPI shared by the Jackson 2, Jackson 3, and Fastjson2 modules. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-json` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-json")
}
```

Gradle project path: `:bluetape4k-json`. Source directory: `io/json`.

## Concepts {#concepts}

The first source-level concepts to inspect are `JsonSerializationException`, and `JsonSerializer`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`JsonSerializationException`](../../../../io/json/src/main/kotlin/io/bluetape4k/json/JsonSerializationException.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`JsonSerializationException`](../../../../io/json/src/main/kotlin/io/bluetape4k/json/JsonSerializationException.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JsonSerializer`](../../../../io/json/src/main/kotlin/io/bluetape4k/json/JsonSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Architecture**, **JsonSerializer Class Structure**, **Serializer Call Flow**, **Key Features**, **JsonSerializer SPI**, **Supported Methods**, **Failure Policy**, **Kotlin Reified Extension Functions**, and **Implementations**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(libs.jakarta.json.api)
implementation(project(":bluetape4k-core"))
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track payload size, allocation, latency, malformed-input rate, resource closure, and protocol errors. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-json:test --no-configuration-cache
```

Representative test anchors:

- [`JsonSerializationExceptionTest`](../../../../io/json/src/test/kotlin/io/bluetape4k/json/JsonSerializationExceptionTest.kt)
- [`JsonSerializerTest`](../../../../io/json/src/test/kotlin/io/bluetape4k/json/JsonSerializerTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### JsonSerializer Class Structure diagram

[![JsonSerializer Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-json-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-json-diagram-01.svg)

_Release README: [`io/json/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/json/README.md)_

### Serializer Call Flow diagram

[![Serializer Call Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-json-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-json-diagram-02.svg)

_Release README: [`io/json/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/json/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../io/json/README.md)
- [Module build](../../../../io/json/build.gradle.kts)
- [`JsonSerializationException`](../../../../io/json/src/main/kotlin/io/bluetape4k/json/JsonSerializationException.kt)
- [`JsonSerializer`](../../../../io/json/src/main/kotlin/io/bluetape4k/json/JsonSerializer.kt)
- [`JsonSerializationExceptionTest`](../../../../io/json/src/test/kotlin/io/bluetape4k/json/JsonSerializationExceptionTest.kt)
- [`JsonSerializerTest`](../../../../io/json/src/test/kotlin/io/bluetape4k/json/JsonSerializerTest.kt)
