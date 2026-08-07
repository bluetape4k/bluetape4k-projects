---
title: Module boundary and client/record helpers
description: Defines the Kafka 3.x line, producer and consumer ownership, TopicPartition parsing, and metric helper contracts.
manualId: bluetape4k-kafka
chapterId: module-boundary-client-records
---

# Module boundary and client/record helpers

## A Kotlin helper layer for Kafka 3.x

The 1.12.1 `bluetape4k-kafka` module targets Kafka 3.9.x and Spring Kafka 3.x. It wraps Kafka clients, Reactor Kafka, and Spring Kafka APIs, but it does not create brokers, topics, consumer groups, or Spring beans. It has no main resources.

Kafka clients are API dependencies. Kafka Streams is `compileOnly`, so applications using the Streams helpers must provide it at runtime.

## Create producers and consumers

`producerOf` and `consumerOf` pass a `Map<String, Any?>` or `Properties` plus optional serializers/deserializers to `KafkaProducer` and `KafkaConsumer`.

```kotlin
val producer = producerOf<String, OrderEvent>(producerProps)
val consumer = consumerOf<String, OrderEvent>(consumerProps)

try {
    consumer.subscribe(listOf("orders"))
    // poll and process
} finally {
    consumer.close()
    producer.close()
}
```

The factories add no cache, retry policy, security defaults, or shutdown hook. The caller owns the client and network resources. If Spring already manages a `ProducerFactory`, `KafkaTemplate`, and listener containers, first decide whether a separate native client is needed.

## Configuration and serializer ownership

When serializer instances are omitted, Kafka creates them from class names in the configuration. If both styles are mixed, use the Kafka client constructor contract to determine which instance is used. Bootstrap servers, acknowledgments, idempotence, transaction IDs, group IDs, auto commit, and security settings remain caller-owned.

Do not log complete configs containing credentials. Keep client IDs and bounded topic/group data, and redact SASL passwords, tokens, and truststore credentials.

## Parse `TopicPartition`

`topicPartitionOf("order-events-12")` splits on the final `-`, so topic names may contain dashes.

```kotlin
val partition = "order-events-12".toTopicPartition()
check(partition.topic() == "order-events")
check(partition.partition() == 12)
```

Blank input and a missing delimiter cause `IllegalArgumentException`; a non-numeric suffix causes `NumberFormatException`. Negative partitions pass local parsing. Validate `partition >= 0` before sending untrusted input to Kafka.

## Metric helper

`Producer.getMetricValueOrNull(name)` finds the first metric whose key name matches and returns its value, or `null`. It does not distinguish metrics by group or tags. If a name occurs more than once, the helper cannot guarantee which entry is selected.

Operational code should inspect the complete Kafka `MetricName` or limit labels while exporting application metrics.

## Sources and tests

- [`build.gradle.kts`](../../../../../infra/kafka/build.gradle.kts)
- [`ProducerSupport.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/ProducerSupport.kt)
- [`ConsumerSupport.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/ConsumerSupport.kt)
- [`TopicPartitionSupport.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/TopicPartitionSupport.kt)
- [`TopicPartitionSupportTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/TopicPartitionSupportTest.kt)
