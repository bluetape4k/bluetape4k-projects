---
manualId: bluetape4k-jackson3
title: "Jackson 3 Serialization"
description: "bluetape4k-jackson3 is a module that wraps the Jackson 3.x library with Kotlin DSL and extension functions."
kind: library
group: io
learningOrder: 360
---

# Jackson 3 Serialization

## Problem {#problem}

bluetape4k-jackson3 is a module that wraps the Jackson 3.x library with Kotlin DSL and extension functions. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-jackson3` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-jackson3")
}
```

Gradle project path: `:bluetape4k-jackson3`. Source directory: `io/jackson3`.

## Concepts {#concepts}

The first source-level concepts to inspect are `Jackson`, `JacksonSerializer`, `JsonGeneratorExtensions`, `JsonMapperSupport`, `JsonNodeExtensions`, `AsyncJsonParser`, `SuspendJsonParser`, and `CborJacksonSerializer`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`Jackson`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/Jackson.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`Jackson`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/Jackson.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JacksonSerializer`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JacksonSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JsonGeneratorExtensions`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JsonGeneratorExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JsonMapperSupport`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JsonMapperSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JsonNodeExtensions`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JsonNodeExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AsyncJsonParser`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/async/AsyncJsonParser.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendJsonParser`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/async/SuspendJsonParser.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CborJacksonSerializer`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/binary/CborJacksonSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CborJsonSerializer`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/binary/CborJsonSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`IonJacksonSerializer`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/binary/IonJacksonSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Why Jackson3 in bluetape4k**, **Architecture Diagrams**, **Jackson 2.x vs 3.x Module Comparison**, **Class Structure**, **Jackson 3.x Module Registration Flow**, **Recommended Usage Scenarios**, **Anti-Patterns**, **Jackson 2.x vs 3.x**, and **Key Features**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.jackson3.bom))
implementation(platform(libs.spring.boot.dependencies))
api(libs.jackson3.core)
api(libs.jackson3.databind)
compileOnly(libs.jackson3.datatype.json.org)
compileOnly(libs.jackson3.datatype.javax.money)
compileOnly(libs.jackson3.datatype.moneta)
api(libs.jackson3.module.kotlin)
compileOnly(libs.jackson3.module.blackbird)
compileOnly(libs.jackson3.module.no.ctor.deser)
api(project(":bluetape4k-json"))
api(project(":bluetape4k-io"))
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`tools.jackson.databind.JacksonModule`](../../../../io/jackson3/src/main/resources/META-INF/services/tools.jackson.databind.JacksonModule)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track payload size, allocation, latency, malformed-input rate, resource closure, and protocol errors. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-jackson3:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractJsonSerializerTest`](../../../../io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/AbstractJsonSerializerTest.kt)
- [`JacksonSerializerTest`](../../../../io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/JacksonSerializerTest.kt)
- [`JacksonTest`](../../../../io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/JacksonTest.kt)
- [`JsonGeneratorExtensionsTest`](../../../../io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/JsonGeneratorExtensionsTest.kt)
- [`JsonMapperSupportTest`](../../../../io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/JsonMapperSupportTest.kt)
- [`JsonNodeExtensionsTest`](../../../../io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/JsonNodeExtensionsTest.kt)
- [`AsyncJsonParserTest`](../../../../io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/async/AsyncJsonParserTest.kt)
- [`SuspendJsonParserTest`](../../../../io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/async/SuspendJsonParserTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Jackson 2.x vs 3.x Module Comparison diagram

[![Jackson 2.x vs 3.x Module Comparison diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson3-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson3-diagram-01.svg)

_Release README: [`io/jackson3/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/jackson3/README.md)_

### Class Structure diagram

[![Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson3-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson3-diagram-02.svg)

_Release README: [`io/jackson3/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/jackson3/README.md)_

### Jackson 3.x Module Registration Flow diagram

[![Jackson 3.x Module Registration Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson3-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson3-sequence-01.svg)

_Release README: [`io/jackson3/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/jackson3/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../io/jackson3/README.md)
- [Module build](../../../../io/jackson3/build.gradle.kts)
- [`Jackson`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/Jackson.kt)
- [`JacksonSerializer`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JacksonSerializer.kt)
- [`JsonGeneratorExtensions`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JsonGeneratorExtensions.kt)
- [`JsonMapperSupport`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JsonMapperSupport.kt)
- [`JsonNodeExtensions`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JsonNodeExtensions.kt)
- [`AsyncJsonParser`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/async/AsyncJsonParser.kt)
- [`SuspendJsonParser`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/async/SuspendJsonParser.kt)
- [`CborJacksonSerializer`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/binary/CborJacksonSerializer.kt)
- [`CborJsonSerializer`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/binary/CborJsonSerializer.kt)
- [`IonJacksonSerializer`](../../../../io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/binary/IonJacksonSerializer.kt)
- [`AbstractJsonSerializerTest`](../../../../io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/AbstractJsonSerializerTest.kt)
- [`JacksonSerializerTest`](../../../../io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/JacksonSerializerTest.kt)
