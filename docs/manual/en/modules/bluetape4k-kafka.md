---
manualId: bluetape4k-kafka
title: "Kafka Client Extensions"
description: "Kafka 3.x clients, coroutine producers, Spring Kafka and Reactor Kafka adapters, and Kafka Streams Kotlin factories, based on the 1.12.1 sources."
kind: library
group: messaging
learningOrder: 700
---

# Kafka Client Extensions

## What it provides {#problem}

`bluetape4k-kafka` is not another messaging framework. It collects small Kotlin helpers around Kafka 3.x: producer and consumer creation, records and `TopicPartition`, combined serializer/deserializer codecs, coroutine sends, Spring Kafka and Reactor Kafka adapters, and Kafka Streams parameter factories.

Kafka and the selected client framework still own the broker protocol, partition assignment, consumer groups, delivery guarantees, and retry policy. This manual covers both the code removed by the helpers and the contracts the application still owns.

## Decide before adoption {#when-to-use}

Answer these questions first:

- Is the application on the Kafka 3.x/Spring Kafka 3.x line or Kafka 4.x/Spring Kafka 4.x?
- Does it only need to await a native `Producer`, or also Spring `KafkaOperations` and Reactor Kafka templates?
- Will payload types be restored from headers, and which packages are trusted?
- Are offsets managed by auto acknowledgment, manual acknowledgment, or a transaction?
- If Kafka Streams is used, is `kafka-streams` present at runtime?

Use [`bluetape4k-kafka4`](./bluetape4k-kafka4.md) for Kafka 4.x. The two artifacts share package and class names, so do not place both on one application classpath.

## Add the dependency {#coordinates}

Consumers only select the ecosystem BOM version.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-kafka")
}
```

Gradle project path: `:bluetape4k-kafka`. Source directory: `infra/kafka`. Do not pin a separate artifact version; use the versions aligned by `bluetape4k-dependencies`.

## Learning path {#concepts}

Read the chapters in task order:

1. [Module boundary and client/record helpers](./bluetape4k-kafka/module-boundary-client-records.md)
2. [Codecs, wire format, and security](./bluetape4k-kafka/codecs-wire-format-security.md)
3. [Coroutine producer](./bluetape4k-kafka/coroutine-producer.md)
4. [Spring Kafka templates and listener adapters](./bluetape4k-kafka/spring-kafka-templates-listeners.md)
5. [Kafka Streams DSL factories](./bluetape4k-kafka/kafka-streams-dsl.md)
6. [Failures, testing, operations, and ecosystem](./bluetape4k-kafka/failures-testing-operations-ecosystem.md)

Each chapter contains examples and exact source/test links. Lock the producer and wire-format decisions in the first three chapters, then continue with the Spring or Streams chapter used by the application.

## First producer {#quick-start}

```kotlin
val producer = producerOf(
    configs = mapOf(
        "bootstrap.servers" to bootstrapServers,
        "acks" to "all",
        "key.serializer" to StringSerializer::class.java,
        "value.serializer" to StringSerializer::class.java,
    ),
)

producer.use {
    val metadata = it.suspendSend(ProducerRecord("orders", "order-1", "created"))
    println("partition=${metadata.partition()}, offset=${metadata.offset()}")
}
```

`producerOf` creates a new `KafkaProducer`; it does not register a singleton or Spring bean. The caller closes a directly created producer. `suspendSend` cancels the Kafka future when the coroutine is cancelled, but that does not prove that the broker did not receive the record.

## API map {#api-by-task}

| Task | Start with | Chapter |
| --- | --- | --- |
| Create native producers/consumers | `producerOf`, `consumerOf` | [Module boundary](./bluetape4k-kafka/module-boundary-client-records.md) |
| Metrics and topic-partition helpers | `getMetricValueOrNull`, `topicPartitionOf` | [Module boundary](./bluetape4k-kafka/module-boundary-client-records.md) |
| String, byte array, JSON, binary codecs | `KafkaCodec`, `KafkaCodecs` | [Codecs and security](./bluetape4k-kafka/codecs-wire-format-security.md) |
| Await or stream native producer sends | `suspendSend`, `sendAsFlow`, `sendAndForget` | [Coroutine producer](./bluetape4k-kafka/coroutine-producer.md) |
| Send with Spring `KafkaOperations` | `io.bluetape4k.kafka.spring.suspendSend` | [Spring integration](./bluetape4k-kafka/spring-kafka-templates-listeners.md) |
| Use Reactor Kafka producer/consumer | `SuspendKafkaProducerTemplate`, `SuspendKafkaConsumerTemplate` | [Spring integration](./bluetape4k-kafka/spring-kafka-templates-listeners.md) |
| Build Streams parameters | `consumedOf`, `materializedOf`, `streamJoinedOf` | [Streams DSL](./bluetape4k-kafka/kafka-streams-dsl.md) |

## Recommended patterns {#patterns}

- Create producers and consumers at the owning component boundary and close them with that component.
- Document the wire format and type allowlist as a topic contract, then cross-test old and new producers and consumers.
- Decide whether send success is part of the business transaction or delivery is handled by an outbox.
- Make the processing-to-offset-commit order explicit. The caller commits or aborts exactly-once batches.
- Bound `Flow` concurrency and buffering, and test ordering within a partition when key ordering matters.
- Use bounded metric labels such as topic, consumer group, and error class. Do not use record keys or payloads as labels.

## Integrations {#integrations}

The 1.12.1 build exposes Kafka clients as API dependencies and uses Spring Kafka and Reactor Kafka as implementation dependencies. Kafka Streams, Spring Kafka test, resilience4j, Kryo/Fory, and several compressors are optional edges. Add the corresponding runtime dependency before using those APIs.

This artifact is the Kafka 3.9.x/Spring Kafka 3.x/Jackson 2 line. Choose `bluetape4k-kafka4` for Kafka 4.2.x/Spring Kafka 4.x/Jackson 3. Sending Logback events to Kafka belongs to the separate [`bluetape4k-kafka-logback`](./bluetape4k-kafka-logback.md) artifact.

## Configuration {#configuration}

There are no main resources or auto-configuration classes. Native clients use a `Map` or `Properties`; Reactor Kafka templates use `SenderOptions` and `ReceiverOptions`; Spring Kafka uses application-owned `KafkaOperations` and listener containers.

Serializers, idempotence, acknowledgments, transaction IDs, consumer groups, offset reset, poll intervals, security protocols, and TLS/SASL remain Kafka client settings. The helpers do not replace them with policy defaults.

## Failure behavior {#failures}

Native and Spring send adapters propagate callback or future failures to the suspending caller. Cancellation attempts to cancel local waiting and the future; it cannot decide whether delivery happened. Design idempotent publishing before retrying.

`AbstractKafkaCodec.deserialize` logs ordinary `Exception`s and returns `null`. It rethrows `CancellationException` and JVM `Error`. Distinguish `null` from valid payloads and route poison pills through metrics, `ErrorHandlingDeserializer`, and a DLQ.

## Operations {#operations}

Observe producer send errors, retries, request latency, and buffer availability; consumer lag, rebalance, poll intervals, and commit failures; and Streams state-store restoration. The application owns shutdown ordering: stop input, finish or cancel in-flight work, commit/flush as required, and close clients.

The 1.12.1 build excludes the vulnerable `org.lz4:lz4-java` artifact and exposes the compatible `at.yawk.lz4` implementation. Check the deployed dependency tree to ensure the old artifact is not reintroduced.

## Testing {#testing}

Run server-free helpers first:

```bash
./gradlew :bluetape4k-kafka:test \
  --tests "io.bluetape4k.kafka.TopicPartitionSupportTest" \
  --tests "io.bluetape4k.kafka.codec.*" \
  --tests "io.bluetape4k.kafka.streams.kstream.KStreamDslTest" \
  --no-configuration-cache
```

The full task includes Testcontainers-backed Kafka tests:

```bash
./gradlew :bluetape4k-kafka:test --no-configuration-cache
```

## Workshops {#workshops}

No dedicated workshop is registered in the manual manifest. Use the README to scan the API shape and the `coroutines`, `spring/core`, and `streams/kstream` tests as runnable study material. Add application tests with production-equivalent serializers, security, and topic settings.

## 1.12.1 scope {#limitations}

This manual targets release `1.12.1`, commit `7cf0b73646af05c0f8872cc4f6a16983949c4e3e`. The development branch later added per-test temporary-directory isolation and diagnostics/error handling around `SuspendKafkaConsumerTemplate.close()`. In 1.12.1 only an `AutoCloseable` receiver is closed, a non-closeable receiver produces no warning, and a close failure propagates directly.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Kafka API Structure

[![Kafka API Structure](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-diagram-01.svg)

_Release README: [`infra/kafka/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/kafka/README.md)_

### Kafka Streams Processing Flow diagram

[![Kafka Streams Processing Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-diagram-02.svg)

_Release README: [`infra/kafka/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/kafka/README.md)_

### Producer/Consumer Message Flow diagram

[![Producer/Consumer Message Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-kafka-sequence-01.svg)

_Release README: [`infra/kafka/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/kafka/README.md)_

<!-- release-readme-diagrams:end -->

## Sources and tests {#sources}

- [Module README](../../../../infra/kafka/README.md)
- [1.12.1 build](../../../../infra/kafka/build.gradle.kts)
- [`ProducerCoroutines.kt`](../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/coroutines/ProducerCoroutines.kt)
- [`KafkaCodec.kt`](../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt)
- [`SuspendKafkaProducerTemplate.kt`](../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/spring/core/SuspendKafkaProducerTemplate.kt)
- [`SuspendKafkaConsumerTemplate.kt`](../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/spring/core/SuspendKafkaConsumerTemplate.kt)
- [`KStreamDslTest.kt`](../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/streams/kstream/KStreamDslTest.kt)
