---
manualId: bluetape4k-avro
title: "Apache Avro Serialization"
description: "A module providing a high-level API for Apache Avro serialization and deserialization."
kind: library
group: io
learningOrder: 370
---

# Apache Avro Serialization

## Problem {#problem}

A module providing a high-level API for Apache Avro serialization and deserialization. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-avro` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-avro")
}
```

Gradle project path: `:bluetape4k-avro`. Source directory: `io/avro`.

## Concepts {#concepts}

The first source-level concepts to inspect are `AvroGenericRecordSerializer`, `AvroReflectSerializer`, `AvroSpecificRecordSerializer`, `CodecFactorySupport`, `DefaultAvroGenericRecordSerializer`, `DefaultAvroReflectSerializer`, and `DefaultAvroSpecificRecordSerializer`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`AvroGenericRecordSerializer`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/AvroGenericRecordSerializer.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`AvroGenericRecordSerializer`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/AvroGenericRecordSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AvroReflectSerializer`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/AvroReflectSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AvroSpecificRecordSerializer`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/AvroSpecificRecordSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CodecFactorySupport`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/CodecFactorySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DefaultAvroGenericRecordSerializer`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroGenericRecordSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DefaultAvroReflectSerializer`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroReflectSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DefaultAvroSpecificRecordSerializer`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroSpecificRecordSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Architecture Diagrams**, **Avro Serializer Class Structure**, **Avro Serialization/Deserialization Flow**, **Compression Codec Selection Guide**, **Serializer Types**, **AvroGenericRecordSerializer**, **AvroSpecificRecordSerializer**, **AvroReflectSerializer**, and **Compression Codec Support**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-io"))
api(libs.avro)
api(libs.avro.kotlin)
runtimeOnly(libs.snappy.java)
runtimeOnly(libs.lz4.java)
runtimeOnly(libs.zstd.jni)
runtimeOnly(libs.xz)
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
./gradlew :bluetape4k-avro:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractAvroTest`](../../../../io/avro/src/test/kotlin/io/bluetape4k/avro/AbstractAvroTest.kt)
- [`CodecFactorySupportTest`](../../../../io/avro/src/test/kotlin/io/bluetape4k/avro/CodecFactorySupportTest.kt)
- [`TestMessageProvider`](../../../../io/avro/src/test/kotlin/io/bluetape4k/avro/TestMessageProvider.kt)
- [`DefaultAvroGenericRecordSerializerTest`](../../../../io/avro/src/test/kotlin/io/bluetape4k/avro/impl/DefaultAvroGenericRecordSerializerTest.kt)
- [`DefaultAvroReflectSerializerTest`](../../../../io/avro/src/test/kotlin/io/bluetape4k/avro/impl/DefaultAvroReflectSerializerTest.kt)
- [`DefaultAvroSpecificRecordSerializerTest`](../../../../io/avro/src/test/kotlin/io/bluetape4k/avro/impl/DefaultAvroSpecificRecordSerializerTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Avro Serializer Class Structure diagram

[![Avro Serializer Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-avro-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-avro-diagram-01.svg)

_Release README: [`io/avro/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/avro/README.md)_

### Compression Codec Selection Guide diagram

[![Compression Codec Selection Guide diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-avro-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-avro-diagram-02.svg)

_Release README: [`io/avro/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/avro/README.md)_

### Avro Serialization/Deserialization Flow diagram

[![Avro Serialization/Deserialization Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-avro-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-avro-sequence-01.svg)

_Release README: [`io/avro/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/avro/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../io/avro/README.md)
- [Module build](../../../../io/avro/build.gradle.kts)
- [`AvroGenericRecordSerializer`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/AvroGenericRecordSerializer.kt)
- [`AvroReflectSerializer`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/AvroReflectSerializer.kt)
- [`AvroSpecificRecordSerializer`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/AvroSpecificRecordSerializer.kt)
- [`CodecFactorySupport`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/CodecFactorySupport.kt)
- [`DefaultAvroGenericRecordSerializer`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroGenericRecordSerializer.kt)
- [`DefaultAvroReflectSerializer`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroReflectSerializer.kt)
- [`DefaultAvroSpecificRecordSerializer`](../../../../io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroSpecificRecordSerializer.kt)
- [`AbstractAvroTest`](../../../../io/avro/src/test/kotlin/io/bluetape4k/avro/AbstractAvroTest.kt)
- [`CodecFactorySupportTest`](../../../../io/avro/src/test/kotlin/io/bluetape4k/avro/CodecFactorySupportTest.kt)
- [`TestMessageProvider`](../../../../io/avro/src/test/kotlin/io/bluetape4k/avro/TestMessageProvider.kt)
- [`DefaultAvroGenericRecordSerializerTest`](../../../../io/avro/src/test/kotlin/io/bluetape4k/avro/impl/DefaultAvroGenericRecordSerializerTest.kt)
- [`DefaultAvroReflectSerializerTest`](../../../../io/avro/src/test/kotlin/io/bluetape4k/avro/impl/DefaultAvroReflectSerializerTest.kt)
