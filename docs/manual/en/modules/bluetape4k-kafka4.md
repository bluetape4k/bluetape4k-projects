---
manualId: bluetape4k-kafka4
title: "Kafka 4 Client Extensions"
description: "bluetape4k-kafka4 is the Kafka 4.x line of the bluetape4k Kafka utilities."
kind: library
group: messaging
learningOrder: 710
---

# Kafka 4 Client Extensions

## Problem {#problem}

bluetape4k-kafka4 is the Kafka 4.x line of the bluetape4k Kafka utilities. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-kafka4` when the application needs client lifecycle, reconnect policy, backpressure, retries, and observability. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-kafka4")
}
```

Gradle project path: `:bluetape4k-kafka4`. Source directory: `infra/kafka4`.

## Concepts {#concepts}

The first source-level concepts to inspect are `ConsumerSupport`, `ProducerSupport`, `TopicPartitionSupport`, `BinaryKafkaCodecs`, `ByteArrayKafkaCodec`, `JacksonKafkaCodec`, `KafkaCodec`, and `KafkaCodecs`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`ConsumerSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/ConsumerSupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`ConsumerSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/ConsumerSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ProducerSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/ProducerSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TopicPartitionSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/TopicPartitionSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BinaryKafkaCodecs`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecs.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ByteArrayKafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/ByteArrayKafkaCodec.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JacksonKafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/JacksonKafkaCodec.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`KafkaCodecs`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodecs.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`StringKafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/StringKafkaCodec.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ProducerCoroutines`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/coroutines/ProducerCoroutines.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Compatibility**, **Features**, **Dependency**, **Gradle Kotlin DSL**, **Maven**, **Dependency Boundary**, **Producer**, **Coroutine Producer**, **Spring Kafka**, and **Jackson 3 Codec**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-annotations"))
api(project(":bluetape4k-core"))
api(project(":bluetape4k-io"))
compileOnly(project(":bluetape4k-resilience4j"))
api(libs.kafka4.clients)
compileOnly(libs.kafka4.streams)
compileOnly(libs.kafka4.generator)
implementation(libs.spring.kafka4)
compileOnly(libs.spring.kafka4.test)
implementation(project(":bluetape4k-spring-boot-core"))
implementation("org.springframework.data:spring-data-commons")
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
./gradlew :bluetape4k-kafka4:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractKafkaTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/AbstractKafkaTest.kt)
- [`ConsumerSupportTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/ConsumerSupportTest.kt)
- [`ProducerSupportTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/ProducerSupportTest.kt)
- [`TopicPartitionSupportTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/TopicPartitionSupportTest.kt)
- [`AbstractKafkaCodecPoisonPillTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/AbstractKafkaCodecPoisonPillTest.kt)
- [`AbstractKafkaCodecTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/AbstractKafkaCodecTest.kt)
- [`ByteArrayKafkaCodecTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/ByteArrayKafkaCodecTest.kt)
- [`JacksonKafkaCodecSecurityTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/JacksonKafkaCodecSecurityTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Dependency Boundary diagram

[![Dependency Boundary diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka4-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka4-diagram-01.svg)

_Release README: [`infra/kafka4/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/kafka4/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../infra/kafka4/README.md)
- [Module build](../../../../infra/kafka4/build.gradle.kts)
- [`ConsumerSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/ConsumerSupport.kt)
- [`ProducerSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/ProducerSupport.kt)
- [`TopicPartitionSupport`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/TopicPartitionSupport.kt)
- [`BinaryKafkaCodecs`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecs.kt)
- [`ByteArrayKafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/ByteArrayKafkaCodec.kt)
- [`JacksonKafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/JacksonKafkaCodec.kt)
- [`KafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt)
- [`KafkaCodecs`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodecs.kt)
- [`StringKafkaCodec`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/StringKafkaCodec.kt)
- [`ProducerCoroutines`](../../../../infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/coroutines/ProducerCoroutines.kt)
- [`AbstractKafkaTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/AbstractKafkaTest.kt)
- [`ConsumerSupportTest`](../../../../infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/ConsumerSupportTest.kt)
