---
manualId: bluetape4k-okio
title: "Okio Extensions"
description: "bluetape4k-okio is a high-performance I/O extension module built on Square's Okio library."
kind: library
group: io
learningOrder: 310
---

# Okio Extensions

## Problem {#problem}

bluetape4k-okio is a high-performance I/O extension module built on Square's Okio library. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-okio` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-okio")
}
```

Gradle project path: `:bluetape4k-okio`. Source directory: `io/okio`.

## Concepts {#concepts}

The first source-level concepts to inspect are `BufferSupport`, `BufferedSourceExtensions`, `ByteStringSupport`, `InputStreamSource`, `OkioConsts`, `OutputStreamSink`, `SinkSupport`, and `SourceSupport`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`BufferSupport`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/BufferSupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`BufferSupport`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/BufferSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BufferedSourceExtensions`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/BufferedSourceExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ByteStringSupport`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/ByteStringSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`InputStreamSource`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/InputStreamSource.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`OkioConsts`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/OkioConsts.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`OutputStreamSink`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/OutputStreamSink.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SinkSupport`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/SinkSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SourceSupport`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/SourceSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TimeoutSupport`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/TimeoutSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AbstractBase64Sink`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/base64/AbstractBase64Sink.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Why Okio**, **Sequence Diagrams**, **Compression Sink (One-Shot) — compress on close**, **Compression Sink (Streaming) — compress incrementally**, **Decompression Source (One-Shot) — decompress on first read**, **Tink Encryption + Compression Combined Flow**, **Coroutines Async File I/O Flow**, **Recommended Usage Scenarios**, and **Anti-Patterns**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
api(project(":bluetape4k-io"))
compileOnly(project(":bluetape4k-tink"))
api(libs.okio)
compileOnly(libs.commons.codec)
compileOnly(project(":bluetape4k-coroutines"))
compileOnly(libs.kotlinx.coroutines.core)
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
./gradlew :bluetape4k-okio:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractOkioTest`](../../../../io/okio/src/test/kotlin/io/bluetape4k/okio/AbstractOkioTest.kt)
- [`BufferCursorKotlinTest`](../../../../io/okio/src/test/kotlin/io/bluetape4k/okio/BufferCursorKotlinTest.kt)
- [`BufferCursorTest`](../../../../io/okio/src/test/kotlin/io/bluetape4k/okio/BufferCursorTest.kt)
- [`BufferFactory`](../../../../io/okio/src/test/kotlin/io/bluetape4k/okio/BufferFactory.kt)
- [`BufferKotlinTest`](../../../../io/okio/src/test/kotlin/io/bluetape4k/okio/BufferKotlinTest.kt)
- [`BufferSupportTest`](../../../../io/okio/src/test/kotlin/io/bluetape4k/okio/BufferSupportTest.kt)
- [`BufferTest`](../../../../io/okio/src/test/kotlin/io/bluetape4k/okio/BufferTest.kt)
- [`BufferedSinkTest`](../../../../io/okio/src/test/kotlin/io/bluetape4k/okio/BufferedSinkTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Sink / Source Adapter Hierarchy diagram

[![Sink / Source Adapter Hierarchy diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-diagram-01.svg)

_Release README: [`io/okio/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/okio/README.md)_

### NIO Channel Adapter Hierarchy diagram

[![NIO Channel Adapter Hierarchy diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-diagram-02.svg)

_Release README: [`io/okio/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/okio/README.md)_

### Coroutines Async I/O Hierarchy diagram

[![Coroutines Async I/O Hierarchy diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-diagram-03.svg)

_Release README: [`io/okio/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/okio/README.md)_

### Compression Factory (Compressable) diagram

[![Compression Factory (Compressable) diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-diagram-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-diagram-04.svg)

_Release README: [`io/okio/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/okio/README.md)_

### Compression Sink (One-Shot) — compress on close diagram

[![Compression Sink (One-Shot) — compress on close diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-sequence-01.svg)

_Release README: [`io/okio/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/okio/README.md)_

### Compression Sink (Streaming) — compress incrementally diagram

[![Compression Sink (Streaming) — compress incrementally diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-sequence-02.svg)

_Release README: [`io/okio/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/okio/README.md)_

### Decompression Source (One-Shot) — decompress on first read diagram

[![Decompression Source (One-Shot) — decompress on first read diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-sequence-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-sequence-03.svg)

_Release README: [`io/okio/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/okio/README.md)_

### Tink Encryption + Compression Combined Flow diagram

[![Tink Encryption + Compression Combined Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-sequence-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-sequence-04.svg)

_Release README: [`io/okio/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/okio/README.md)_

### Coroutines Async File I/O Flow diagram

[![Coroutines Async File I/O Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-sequence-05.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-okio-sequence-05.svg)

_Release README: [`io/okio/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/okio/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../io/okio/README.md)
- [Module build](../../../../io/okio/build.gradle.kts)
- [`BufferSupport`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/BufferSupport.kt)
- [`BufferedSourceExtensions`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/BufferedSourceExtensions.kt)
- [`ByteStringSupport`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/ByteStringSupport.kt)
- [`InputStreamSource`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/InputStreamSource.kt)
- [`OkioConsts`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/OkioConsts.kt)
- [`OutputStreamSink`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/OutputStreamSink.kt)
- [`SinkSupport`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/SinkSupport.kt)
- [`SourceSupport`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/SourceSupport.kt)
- [`TimeoutSupport`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/TimeoutSupport.kt)
- [`AbstractBase64Sink`](../../../../io/okio/src/main/kotlin/io/bluetape4k/okio/base64/AbstractBase64Sink.kt)
- [`AbstractOkioTest`](../../../../io/okio/src/test/kotlin/io/bluetape4k/okio/AbstractOkioTest.kt)
- [`BufferCursorKotlinTest`](../../../../io/okio/src/test/kotlin/io/bluetape4k/okio/BufferCursorKotlinTest.kt)
