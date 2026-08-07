---
title: Failures, testing, operations, and ecosystem boundaries
description: Classify input and server failures, test with NATS Testcontainers, observe production behavior, and retain Spring ownership.
manualId: bluetape4k-nats
chapterId: failures-testing-operations-ecosystem
---

# Failures, testing, operations, and ecosystem boundaries

## Separate four failure classes

Avoid collapsing every NATS integration failure into one messaging exception.

| Class | Examples | Handling rule |
| --- | --- | --- |
| Caller input | blank subject, stream, or bucket; negative timeout | immediate `IllegalArgumentException`; do not retry |
| Transient communication | disconnect, timeout, `IOException` | retry only after checking operation meaning and idempotency |
| Server policy | authorization, invalid stream configuration, storage limit | fix policy or configuration; do not hide it with automatic retry |
| Expected absence | missing stream, consumer, or bucket | selected `get*OrNull`, `exists*`, and `forcedDelete*` paths translate it |

Management helpers rethrow every JetStream error except `JET_STREAM_NOT_FOUND`. Also keep missing business data separate from a missing infrastructure resource.

## Retries and duplicates

A publish or request timeout does not prove that the server rejected the message. The network may have failed before the response arrived. Put a business idempotency key on retryable commands and make consumers tolerate duplicates.

JetStream redelivery is expected. A handler can update a database and fail before acknowledging the message. Define side-effect and acknowledgment ordering with an application transaction or inbox/outbox pattern.

## Unit and server tests

`ConnectionExtensionsTest`, options tests, and service builder tests use mocks to verify delegation and input validation. A NATS server is required to prove subject routing, dispatchers, request-reply, publish acknowledgments, and storage.

```bash
./gradlew :bluetape4k-nats:test --no-configuration-cache
```

The module's `AbstractNatsTest` shares a `NatsServer.Launcher.nats` Testcontainer. It is not part of the published artifact. Applications should build a fixture with production-like options and run it sequentially with other Testcontainers suites.

## Failure scenarios

Cover more than the happy path:

- stop and restore the server, then verify reconnect and subscription recovery;
- compare synchronous, asynchronous, and suspending request results with no responder;
- fail a JetStream handler before acknowledgment and verify redelivery and maximum delivery policy;
- create a slow consumer and observe pending limits and error listeners;
- prove that stream purge or replacement and bucket deletion run only in approved environments.

## Metrics and logs

For core connections, record connection events, reconnects, asynchronous errors, slow consumers, request latency, and timeouts. Avoid raw subject names as unbounded metric labels; normalize them to a small business category set.

For JetStream, add stream message and byte counts, publish acknowledgment latency, consumer pending and redelivery counts, acknowledgment floor, and processing latency. Separate handler exception, negative acknowledgment, terminal acknowledgment, and timeout to find reprocessing loops.

Do not log credentials, tokens, or full message payloads by default. Use bounded correlation headers and message IDs after reviewing sensitive data and header cardinality.

## Spring and other messaging modules

`bluetape4k-nats` exposes `nats-spring` only as a `compileOnly` API edge. It provides no Spring Boot auto-configuration, property binding, actuator health, or bean lifecycle. A Spring application adds these pieces and the coroutine runtime when required.

Do not force a partition-log workload with a mature consumer-group ecosystem into core queue subscriptions or KeyValue. Conversely, simple subject routing and request-reply do not always need a larger log platform. Choose by retention, replay, ordering, throughput, and operations tooling.

## 1.12.1 validation scope

The 1.12.1 production source has no serializer abstraction, schema registry, retry policy, tracing hook, or Spring auto-configuration. Jackson and compressor dependencies used by README examples are test payload tools, not a runtime serialization contract of this module.

## Sources and tests

- [`build.gradle.kts`](../../../../../infra/nats/build.gradle.kts)
- [`JetStreamApiException.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/JetStreamApiException.kt)
- [`JetStreamManagement.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/JetStreamManagement.kt)
- [`NatsManagementExtensionsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/NatsManagementExtensionsTest.kt)
- [`ConnectionExtensionsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/ConnectionExtensionsTest.kt)
- [`SimplePublishExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/SimplePublishExample.kt)
- [`RecreateConsumerExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/RecreateConsumerExample.kt)

Documentation checks validate source links and contracts. The full NATS Testcontainers suite need not be repeated for documentation-only changes; run it sequentially at the integration gate selected by the parent workflow.
