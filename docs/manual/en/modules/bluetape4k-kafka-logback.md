---
manualId: bluetape4k-kafka-logback
title: "Kafka Logback Appender"
description: "한국어"
kind: library
group: messaging
learningOrder: 720
---

# Kafka Logback Appender

## Problem {#problem}

한국어 This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-kafka-logback` when the application needs client lifecycle, reconnect policy, backpressure, retries, and observability. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-kafka-logback")
}
```

Gradle project path: `:bluetape4k-kafka-logback`. Source directory: `infra/kafka-logback`.

## Concepts {#concepts}

The first source-level concepts to inspect are `AbstractKafkaAppender`, `KafkaAppender`, `KafkaProducerConfigDiagnostics`, `DefaultKafkaExporter`, `ExportExceptionHandler`, `KafkaExporter`, `NoopExportExceptionHandler`, and `AbstractKafkaKeyProvider`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`AbstractKafkaAppender`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/AbstractKafkaAppender.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`AbstractKafkaAppender`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/AbstractKafkaAppender.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KafkaAppender`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/KafkaAppender.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KafkaProducerConfigDiagnostics`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/KafkaProducerConfigDiagnostics.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DefaultKafkaExporter`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/exporter/DefaultKafkaExporter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ExportExceptionHandler`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/exporter/ExportExceptionHandler.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KafkaExporter`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/exporter/KafkaExporter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`NoopExportExceptionHandler`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/exporter/NoopExportExceptionHandler.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AbstractKafkaKeyProvider`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/keyprovider/AbstractKafkaKeyProvider.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ContextNameKafkaKeyProvider`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/keyprovider/ContextNameKafkaKeyProvider.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HostnameKafkaKeyProvider`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/keyprovider/HostnameKafkaKeyProvider.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Append Sequence**, **Features**, **Usage**, **logback.xml**, **Custom Key Provider**, and **Dependencies**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
api(libs.logback.classic)
api(libs.kafka.clients)
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
./gradlew :bluetape4k-kafka-logback:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractKafkaIntegrationTest`](../../../../infra/kafka-logback/src/test/kotlin/io/bluetape4k/kafka/logback/AbstractKafkaIntegrationTest.kt)
- [`KafkaAppenderIT`](../../../../infra/kafka-logback/src/test/kotlin/io/bluetape4k/kafka/logback/KafkaAppenderIT.kt)
- [`KafkaAppenderTest`](../../../../infra/kafka-logback/src/test/kotlin/io/bluetape4k/kafka/logback/KafkaAppenderTest.kt)
- [`LogbackIntegrationTest`](../../../../infra/kafka-logback/src/test/kotlin/io/bluetape4k/kafka/logback/LogbackIntegrationTest.kt)
- [`DefaultKafkaExporterTest`](../../../../infra/kafka-logback/src/test/kotlin/io/bluetape4k/kafka/logback/exporter/DefaultKafkaExporterTest.kt)
- [`AbstractKafkaKeyProviderTest`](../../../../infra/kafka-logback/src/test/kotlin/io/bluetape4k/kafka/logback/keyprovider/AbstractKafkaKeyProviderTest.kt)
- [`ContextNameKafkaKeyProviderTest`](../../../../infra/kafka-logback/src/test/kotlin/io/bluetape4k/kafka/logback/keyprovider/ContextNameKafkaKeyProviderTest.kt)
- [`HostnameKafkaKeyProviderTest`](../../../../infra/kafka-logback/src/test/kotlin/io/bluetape4k/kafka/logback/keyprovider/HostnameKafkaKeyProviderTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Kafka Logback Class Structure

[![Kafka Logback Class Structure](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-logback-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-logback-diagram-01.svg)

_Release README: [`infra/kafka-logback/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/kafka-logback/README.md)_

### Kafka Logback Append Flow

[![Kafka Logback Append Flow](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-logback-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-logback-sequence-01.svg)

_Release README: [`infra/kafka-logback/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/kafka-logback/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../infra/kafka-logback/README.md)
- [Module build](../../../../infra/kafka-logback/build.gradle.kts)
- [`AbstractKafkaAppender`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/AbstractKafkaAppender.kt)
- [`KafkaAppender`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/KafkaAppender.kt)
- [`KafkaProducerConfigDiagnostics`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/KafkaProducerConfigDiagnostics.kt)
- [`DefaultKafkaExporter`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/exporter/DefaultKafkaExporter.kt)
- [`ExportExceptionHandler`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/exporter/ExportExceptionHandler.kt)
- [`KafkaExporter`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/exporter/KafkaExporter.kt)
- [`NoopExportExceptionHandler`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/exporter/NoopExportExceptionHandler.kt)
- [`AbstractKafkaKeyProvider`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/keyprovider/AbstractKafkaKeyProvider.kt)
- [`ContextNameKafkaKeyProvider`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/keyprovider/ContextNameKafkaKeyProvider.kt)
- [`HostnameKafkaKeyProvider`](../../../../infra/kafka-logback/src/main/kotlin/io/bluetape4k/kafka/logback/keyprovider/HostnameKafkaKeyProvider.kt)
- [`AbstractKafkaIntegrationTest`](../../../../infra/kafka-logback/src/test/kotlin/io/bluetape4k/kafka/logback/AbstractKafkaIntegrationTest.kt)
- [`KafkaAppenderIT`](../../../../infra/kafka-logback/src/test/kotlin/io/bluetape4k/kafka/logback/KafkaAppenderIT.kt)
