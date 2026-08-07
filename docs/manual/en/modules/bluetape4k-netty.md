---
manualId: bluetape4k-netty
title: "Netty Extensions"
description: "Extension functions for working with the Netty framework."
kind: library
group: io
learningOrder: 430
---

# Netty Extensions

## Problem {#problem}

Extension functions for working with the Netty framework. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-netty` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-netty")
}
```

Gradle project path: `:bluetape4k-netty`. Source directory: `io/netty`.

## Concepts {#concepts}

The first source-level concepts to inspect are `NettyTransportSupport`, `BitBuf`, `BitBufImpl`, `ByteBufExtensions`, `ByteBufUtilSupport`, `Medium`, `SmallLong`, and `Smart`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`NettyTransportSupport`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/NettyTransportSupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`NettyTransportSupport`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/NettyTransportSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BitBuf`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/BitBuf.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BitBufImpl`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/BitBufImpl.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ByteBufExtensions`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/ByteBufExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ByteBufUtilSupport`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/ByteBufUtilSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Medium`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/Medium.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SmallLong`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/SmallLong.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Smart`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/Smart.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`UMedium`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/UMedium.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`USmallLong`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/USmallLong.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Key Features**, **Adding the Dependency**, **Architecture Diagrams**, **ByteBuf Extension API Structure**, **Smart Encoding Data Flow**, **ByteBuf Processing Flow**, **Basic Usage**, **1. Reading from ByteBuf**, and **2. Writing to ByteBuf**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-io"))
api(libs.netty.buffer)
api(libs.netty.all)
compileOnly(libs.jctools.core)
compileOnly(libs.kotlinx.coroutines.core)
compileOnly(libs.netty.transport.classes.epoll)
compileOnly(libs.netty.transport.classes.kqueue)
compileOnly(libs.netty.resolver.dns.classes.macos)
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
./gradlew :bluetape4k-netty:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractNettyTest`](../../../../io/netty/src/test/kotlin/io/bluetape4k/netty/AbstractNettyTest.kt)
- [`NettyTransportSupportTest`](../../../../io/netty/src/test/kotlin/io/bluetape4k/netty/NettyTransportSupportTest.kt)
- [`BitBufTest`](../../../../io/netty/src/test/kotlin/io/bluetape4k/netty/buffer/BitBufTest.kt)
- [`ByteBufByteArrayTest`](../../../../io/netty/src/test/kotlin/io/bluetape4k/netty/buffer/ByteBufByteArrayTest.kt)
- [`ByteBufByteTest`](../../../../io/netty/src/test/kotlin/io/bluetape4k/netty/buffer/ByteBufByteTest.kt)
- [`ByteBufMediumIntLongTest`](../../../../io/netty/src/test/kotlin/io/bluetape4k/netty/buffer/ByteBufMediumIntLongTest.kt)
- [`ByteBufShortAddTest`](../../../../io/netty/src/test/kotlin/io/bluetape4k/netty/buffer/ByteBufShortAddTest.kt)
- [`ByteBufSmartVarIntTest`](../../../../io/netty/src/test/kotlin/io/bluetape4k/netty/buffer/ByteBufSmartVarIntTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### bluetape4k-netty ByteBuf Extension API Structure diagram

[![bluetape4k-netty ByteBuf Extension API Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-netty-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-netty-diagram-01.svg)

_Release README: [`io/netty/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/netty/README.md)_

### bluetape4k-netty Smart Encoding Data Flow diagram

[![bluetape4k-netty Smart Encoding Data Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-netty-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-netty-diagram-02.svg)

_Release README: [`io/netty/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/netty/README.md)_

### bluetape4k-netty ByteBuf Processing Flow diagram

[![bluetape4k-netty ByteBuf Processing Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-netty-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-netty-diagram-03.svg)

_Release README: [`io/netty/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/netty/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../io/netty/README.md)
- [Module build](../../../../io/netty/build.gradle.kts)
- [`NettyTransportSupport`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/NettyTransportSupport.kt)
- [`BitBuf`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/BitBuf.kt)
- [`BitBufImpl`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/BitBufImpl.kt)
- [`ByteBufExtensions`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/ByteBufExtensions.kt)
- [`ByteBufUtilSupport`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/ByteBufUtilSupport.kt)
- [`Medium`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/Medium.kt)
- [`SmallLong`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/SmallLong.kt)
- [`Smart`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/Smart.kt)
- [`UMedium`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/UMedium.kt)
- [`USmallLong`](../../../../io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/USmallLong.kt)
- [`AbstractNettyTest`](../../../../io/netty/src/test/kotlin/io/bluetape4k/netty/AbstractNettyTest.kt)
- [`NettyTransportSupportTest`](../../../../io/netty/src/test/kotlin/io/bluetape4k/netty/NettyTransportSupportTest.kt)
