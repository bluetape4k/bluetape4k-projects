---
manualId: bluetape4k-protobuf
title: "Module bluetape4k-protobuf"
description: "A Kotlin extension library for working with Google Protocol Buffers messages."
kind: library
group: io
---

# Module bluetape4k-protobuf

## Problem {#problem}

A Kotlin extension library for working with Google Protocol Buffers messages. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-protobuf` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-protobuf")
}
```

Gradle project path: `:bluetape4k-protobuf`. Source directory: `io/protobuf`.

## Concepts {#concepts}

The first source-level concepts to inspect are `DateTimeSupport`, `DurationSupport`, `MessageSupport`, `MoneySupport`, `TimestampSupport`, `TypeAlias`, `ProtobufSerializer`, and `LettuceProtobufCodecs`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`DateTimeSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/DateTimeSupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`DateTimeSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/DateTimeSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DurationSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/DurationSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MessageSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MessageSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MoneySupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MoneySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TimestampSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/TimestampSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TypeAlias`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/TypeAlias.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ProtobufSerializer`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceProtobufCodecs`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufCodecs.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RedissonProtobufCodec`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodec.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RedissonProtobufCodecs`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodecs.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Architecture**, **Protobuf Class Structure**, **Protobuf Type Conversion Flow**, **ProtobufSerializer Allowlist Sequence**, **Key Features**, **Security: ProtobufSerializer Allowlist**, **Usage Examples**, **1. Type Aliases**, and **2. Timestamp Conversion**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(libs.protobuf.java)
api(libs.protobuf.java.util)
api(libs.protobuf.kotlin)
api(libs.proto.google.common.protos)
api(project(":bluetape4k-io"))
compileOnly(project(":bluetape4k-lettuce"))
compileOnly(project(":bluetape4k-redisson"))
compileOnly(libs.lz4.java)
compileOnly(libs.snappy.java)
compileOnly(libs.zstd.jni)
compileOnly(project(":bluetape4k-money"))
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
./gradlew :bluetape4k-protobuf:test --no-configuration-cache
```

Representative test anchors:

- [`DateTimeSupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/DateTimeSupportTest.kt)
- [`DurationSupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/DurationSupportTest.kt)
- [`DynamicMessageExamples`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/DynamicMessageExamples.kt)
- [`MessageSupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/MessageSupportTest.kt)
- [`MoneySupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/MoneySupportTest.kt)
- [`TimestampSupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/TimestampSupportTest.kt)
- [`ProtobufSerializerSecurityTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerSecurityTest.kt)
- [`ProtobufSerializerTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../io/protobuf/README.md)
- [Module build](../../../../io/protobuf/build.gradle.kts)
- [`DateTimeSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/DateTimeSupport.kt)
- [`DurationSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/DurationSupport.kt)
- [`MessageSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MessageSupport.kt)
- [`MoneySupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MoneySupport.kt)
- [`TimestampSupport`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/TimestampSupport.kt)
- [`TypeAlias`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/TypeAlias.kt)
- [`ProtobufSerializer`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializer.kt)
- [`LettuceProtobufCodecs`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufCodecs.kt)
- [`RedissonProtobufCodec`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodec.kt)
- [`RedissonProtobufCodecs`](../../../../io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodecs.kt)
- [`DateTimeSupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/DateTimeSupportTest.kt)
- [`DurationSupportTest`](../../../../io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/DurationSupportTest.kt)
