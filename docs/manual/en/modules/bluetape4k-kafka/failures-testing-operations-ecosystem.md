---
title: Failures, testing, operations, and ecosystem
description: Organizes delivery ambiguity, offset/codec failures, server-free and Testcontainers tests, and Kafka 4/Logback boundaries.
manualId: bluetape4k-kafka
chapterId: failures-testing-operations-ecosystem
---

# Failures, testing, operations, and ecosystem

## Separate failure classes

At minimum, distinguish:

- serialization, deserialization, and type-allowlist failures;
- local producer timeouts, broker rejection, and unknown delivery results;
- consumer poll/rebalance and offset-commit failures;
- coroutine cancellation and application shutdown;
- Streams processing, state-store, and restoration failures.

One retry policy across all of them can create duplicates, poison-pill loops, or worse rebalances. Define retry eligibility, idempotency, and DLQ handling per class.

## Send results and business atomicity

A successful `suspendSend` means a broker acknowledgment was received; it does not commit a database transaction. Use an outbox when database changes and event publication must be coordinated. Classify timeout and cancellation as an unknown result, not proof that no send occurred.

## Processing and offsets

Committing before processing can lose records after failure. Committing after processing can repeat work, so handlers should be idempotent. Exactly-once APIs do not automatically include external HTTP or database side effects in the Kafka transaction.

## Fast unit tests

The following areas run without a broker:

- `TopicPartitionSupportTest` for parsing and invalid input;
- codec tests for byte/string round trips, allowlists, and poison pills;
- coroutine producer mocks for callback failures and future cancellation;
- listener-adapter delegation tests;
- `KStreamDslTest` for Streams parameter factories.

```bash
./gradlew :bluetape4k-kafka:test \
  --tests "io.bluetape4k.kafka.TopicPartitionSupportTest" \
  --tests "io.bluetape4k.kafka.codec.*" \
  --tests "io.bluetape4k.kafka.streams.kstream.KStreamDslTest" \
  --no-configuration-cache
```

Producer/consumer, Spring template, and actual Flow tests use the `KafkaServer.Launcher.kafka` Testcontainer. Run heavyweight infrastructure tests sequentially.

## Application verification

1. Cross-test stored records with old and new codecs.
2. Verify duplicate handling and idempotency under retry and timeout.
3. Test processing/commit ordering and graceful shutdown during rebalance.
4. Test TLS/SASL, ACLs, and credential rotation with production-equivalent settings.
5. Measure Streams topology upgrades and state restoration.
6. Ensure DLQ replay does not recreate the same poison-pill loop.

## Operational metrics

Separate producer errors/retries/request latency/buffers, consumer lag/rebalance/poll/commit, DLQ and codec rejection, and Streams task/state restoration. Bound topic and group labels; never use record keys, exception messages, or payloads as metric labels.

## Neighboring artifacts

| Need | Artifact | Boundary |
| --- | --- | --- |
| Kafka 3.9.x, Spring Kafka 3.x, Jackson 2 | `bluetape4k-kafka` | This manual |
| Kafka 4.2.x, Spring Kafka 4.x, Jackson 3 | [`bluetape4k-kafka4`](../bluetape4k-kafka4.md) | Same packages; choose one line |
| Export Logback events to Kafka | [`bluetape4k-kafka-logback`](../bluetape4k-kafka-logback.md) | Appender/exporter only, not a general producer replacement |
| Coroutine and Flow foundations | [`bluetape4k-coroutines`](../bluetape4k-coroutines.md) | Kafka delivery semantics remain here |
| Shared Kafka Testcontainers support | [`bluetape4k-testcontainers`](../bluetape4k-testcontainers.md) | Caller owns application fixtures/lifecycle |

## 1.12.1 versus the development branch

After the release, Kafka tests gained per-module temporary directories, and consumer-template close gained warnings for non-`AutoCloseable` receivers plus logged/suppressed close failures. This manual uses the 1.12.1 contract: conditional close, no warning, and direct close-exception propagation.

## Sources and tests

- [`AbstractKafkaTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/AbstractKafkaTest.kt)
- [`ProducerSupportTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/coroutines/ProducerSupportTest.kt)
- [`SuspendKafkaConsumerTemplateTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/spring/core/SuspendKafkaConsumerTemplateTest.kt)
- [`KStreamDslTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/streams/kstream/KStreamDslTest.kt)
- [`kafka4/build.gradle.kts`](../../../../../infra/kafka4/build.gradle.kts)
- [`kafka-logback/build.gradle.kts`](../../../../../infra/kafka-logback/build.gradle.kts)
