---
manualId: bluetape4k-jackson2
title: "Jackson 2 Serialization"
description: "bluetape4k-jackson2 is a module that wraps the Jackson 2.x library with Kotlin DSL and extension functions."
kind: library
group: io
learningOrder: 350
---

# Jackson 2 Serialization

## Problem {#problem}

bluetape4k-jackson2 is a module that wraps the Jackson 2.x library with Kotlin DSL and extension functions. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-jackson2` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-jackson2")
}
```

Gradle project path: `:bluetape4k-jackson2`. Source directory: `io/jackson2`.

## Concepts {#concepts}

The first source-level concepts to inspect are `Jackson`, `JacksonSerializer`, `JsonGeneratorExtensions`, `JsonMapperSupport`, `JsonNodeExtensions`, `AsyncJsonParser`, `SuspendJsonParser`, and `CborJacksonSerializer`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`Jackson`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/Jackson.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`Jackson`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/Jackson.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JacksonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JacksonSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JsonGeneratorExtensions`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JsonGeneratorExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JsonMapperSupport`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JsonMapperSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JsonNodeExtensions`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JsonNodeExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AsyncJsonParser`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/async/AsyncJsonParser.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendJsonParser`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/async/SuspendJsonParser.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CborJacksonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/binary/CborJacksonSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CborJsonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/binary/CborJsonSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`IonJacksonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/binary/IonJacksonSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Why Jackson2 in bluetape4k**, **Architecture**, **Class Structure**, **Jackson Serialization Pipeline**, **Field Encryption Flow (@JsonTinkEncrypt)**, **Recommended Usage Scenarios**, **Anti-Patterns**, **Key Features**, and **1. JsonMapper DSL**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.jackson.bom))
implementation(platform(libs.spring.boot.dependencies))
api(libs.jackson.core)
api(libs.jackson.databind)
api(libs.jackson.datatype.jdk8)
api(libs.jackson.datatype.jsr310)
api(libs.jackson.module.kotlin)
api(libs.jackson.module.parameter.names)
api(libs.jackson.module.blackbird)
compileOnly(libs.jackson.dataformat.properties)
compileOnly(libs.jackson.dataformat.yaml)
compileOnly(libs.jackson.dataformat.avro)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`com.fasterxml.jackson.databind.Module`](../../../../io/jackson2/src/main/resources/META-INF/services/com.fasterxml.jackson.databind.Module)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track payload size, allocation, latency, malformed-input rate, resource closure, and protocol errors. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-jackson2:test --no-configuration-cache
```

Representative test anchors:

- [`DisallowedTypedPayload`](../../../../io/jackson2/src/test/kotlin/com/example/disallowed/DisallowedTypedPayload.kt)
- [`AbstractJsonSerializerTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/AbstractJsonSerializerTest.kt)
- [`JacksonSerializerTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JacksonSerializerTest.kt)
- [`JacksonTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JacksonTest.kt)
- [`JsonGeneratorExtensionsTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JsonGeneratorExtensionsTest.kt)
- [`JsonMapperSupportTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JsonMapperSupportTest.kt)
- [`JsonNodeExtensionsTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JsonNodeExtensionsTest.kt)
- [`AsyncJsonParserTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/async/AsyncJsonParserTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Class Structure diagram

[![Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson2-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson2-diagram-01.svg)

_Release README: [`io/jackson2/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/jackson2/README.md)_

### Jackson Serialization Pipeline diagram

[![Jackson Serialization Pipeline diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson2-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson2-diagram-02.svg)

_Release README: [`io/jackson2/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/jackson2/README.md)_

### Field Encryption Flow (@JsonTinkEncrypt) diagram

[![Field Encryption Flow (@JsonTinkEncrypt) diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson2-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-jackson2-sequence-01.svg)

_Release README: [`io/jackson2/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/jackson2/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../io/jackson2/README.md)
- [Module build](../../../../io/jackson2/build.gradle.kts)
- [`Jackson`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/Jackson.kt)
- [`JacksonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JacksonSerializer.kt)
- [`JsonGeneratorExtensions`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JsonGeneratorExtensions.kt)
- [`JsonMapperSupport`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JsonMapperSupport.kt)
- [`JsonNodeExtensions`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JsonNodeExtensions.kt)
- [`AsyncJsonParser`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/async/AsyncJsonParser.kt)
- [`SuspendJsonParser`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/async/SuspendJsonParser.kt)
- [`CborJacksonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/binary/CborJacksonSerializer.kt)
- [`CborJsonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/binary/CborJsonSerializer.kt)
- [`IonJacksonSerializer`](../../../../io/jackson2/src/main/kotlin/io/bluetape4k/jackson/binary/IonJacksonSerializer.kt)
- [`DisallowedTypedPayload`](../../../../io/jackson2/src/test/kotlin/com/example/disallowed/DisallowedTypedPayload.kt)
- [`AbstractJsonSerializerTest`](../../../../io/jackson2/src/test/kotlin/io/bluetape4k/jackson/AbstractJsonSerializerTest.kt)
