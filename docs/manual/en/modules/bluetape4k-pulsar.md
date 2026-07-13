---
manualId: bluetape4k-pulsar
title: "bluetape4k-pulsar"
description: "Apache Pulsar client extensions for Kotlin — coroutine-first, DSL-friendly, Jackson2/Jackson3 schema support."
kind: library
group: infrastructure
---

# bluetape4k-pulsar

## Problem {#problem}

Apache Pulsar client extensions for Kotlin — coroutine-first, DSL-friendly, Jackson2/Jackson3 schema support. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-pulsar` when the application needs client lifecycle, reconnect policy, backpressure, retries, and observability. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-pulsar")
}
```

Gradle project path: `:bluetape4k-pulsar`. Source directory: `infra/pulsar`.

## Concepts {#concepts}

The first source-level concepts to inspect are `PulsarClientSupport`, `Jackson3Schema`, `JacksonSchema`, `ConsumerExtensions`, `ConsumerSupport`, `ProducerExtensions`, and `ProducerSupport`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`PulsarClientSupport`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/PulsarClientSupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`PulsarClientSupport`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/PulsarClientSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Jackson3Schema`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/codec/Jackson3Schema.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JacksonSchema`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/codec/JacksonSchema.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ConsumerExtensions`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/consumer/ConsumerExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ConsumerSupport`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/consumer/ConsumerSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ProducerExtensions`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/producer/ProducerExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ProducerSupport`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/producer/ProducerSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ReaderExtensions`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/reader/ReaderExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ReaderSupport`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/reader/ReaderSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Features**, **Dependency**, **Examples**, **Client lifecycle**, **Producer**, **Consumer**, **Reader (no subscription, no ack)**, **Custom JSON Schema**, and **Compression**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
api(project(":bluetape4k-coroutines"))
api(libs.pulsar.client)
compileOnly(project(":bluetape4k-jackson2"))
compileOnly(libs.jackson.databind)
compileOnly(project(":bluetape4k-jackson3"))
compileOnly(libs.jackson3.databind)
implementation(libs.kotlinx.coroutines.core)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track connection state, queue depth, retries, timeouts, remote errors, and graceful shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-pulsar:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractPulsarTest`](../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/AbstractPulsarTest.kt)
- [`PulsarClientSupportTest`](../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/PulsarClientSupportTest.kt)
- [`Jackson3SchemaTest`](../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/codec/Jackson3SchemaTest.kt)
- [`JacksonSchemaTest`](../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/codec/JacksonSchemaTest.kt)
- [`ConsumerExtensionsTest`](../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/consumer/ConsumerExtensionsTest.kt)
- [`ConsumerSupportTest`](../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/consumer/ConsumerSupportTest.kt)
- [`ProducerExtensionsTest`](../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/producer/ProducerExtensionsTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../infra/pulsar/README.md)
- [Module build](../../../../infra/pulsar/build.gradle.kts)
- [`PulsarClientSupport`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/PulsarClientSupport.kt)
- [`Jackson3Schema`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/codec/Jackson3Schema.kt)
- [`JacksonSchema`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/codec/JacksonSchema.kt)
- [`ConsumerExtensions`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/consumer/ConsumerExtensions.kt)
- [`ConsumerSupport`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/consumer/ConsumerSupport.kt)
- [`ProducerExtensions`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/producer/ProducerExtensions.kt)
- [`ProducerSupport`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/producer/ProducerSupport.kt)
- [`ReaderExtensions`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/reader/ReaderExtensions.kt)
- [`ReaderSupport`](../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/reader/ReaderSupport.kt)
- [`AbstractPulsarTest`](../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/AbstractPulsarTest.kt)
