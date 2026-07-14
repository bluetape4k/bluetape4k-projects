---
title: JetStream streams and consumers
description: Use PublishAck, safe stream reconciliation, durable ConsumerContext, bounded fetches, and explicit control operations.
manualId: bluetape4k-nats
chapterId: jetstream-streams-consumers
---

# JetStream streams and consumers

## How JetStream differs from core NATS

JetStream stores subject messages in streams and tracks consumer state. Publishers can use `PublishAck` to confirm that the server accepted a message into a stream, while durable consumers can resume from their saved position.

```kotlin
val jetStream = connection.jetStream()
val ack = jetStream.publishSuspending(
    subject = "orders.created",
    body = "{\"orderId\":\"O-100\"}",
)
logger.debug { "stream=${ack.stream} sequence=${ack.seq}" }
```

`publishSuspending` awaits an asynchronous publish future. Coroutine cancellation and future failures propagate. An acknowledgment confirms storage, not completion of consumer business work.

## Stream configuration

The `streamConfiguration` DSL exposes the jNATS builder.

```kotlin
val config = streamConfiguration("ORDERS") {
    subjects("orders.*")
    storageType(StorageType.File)
    replicas(3)
    maxAge(Duration.ofDays(7))
}

val info = connection.jetStreamManagement().addStream(config)
```

Retention, discard policy, maximum bytes, replicas, and storage type affect durability and cost. Manage them as explicit operations requirements instead of relying on library defaults.

## Create, update, and replace

`createStream` creates a stream with `Memory` storage by default. `createStreamOrUpdateSubjects` creates a missing stream or appends only missing subjects while preserving current subject order. It skips the update call when every subject already exists.

In contrast, `createOrReplaceStream` deletes the stream and creates it again. Stored messages and consumer state can be lost. Prefer non-destructive updates during production startup, and restrict replacement to an explicit migration or test fixture.

## Durable consumers

`consumerContextOf(connection, streamName, consumerName)` builds a consumer configuration using the name as the durable name and calls `createOrUpdateConsumer`.

```kotlin
val consumer = consumerContextOf(
    connection,
    streamName = "ORDERS",
    consumerName = "billing",
)

val message = consumer.next()
try {
    bill(message)
    message.ack()
} catch (e: Exception) {
    message.nak()
    throw e
}
```

Acknowledgment policy, delivery policy, subject filter, maximum delivery count, and acknowledgment wait define the reprocessing contract. The shortcut sets only the durable name; build a `ConsumerConfiguration` and use the second overload when the other values matter.

## Bounded fetches

`fetchConsumeOptionsOf` defaults to 100 messages and a 1,000ms expiry, with an optional maximum byte count. The jNATS builder performs final validation.

```kotlin
val fetchOptions = fetchConsumeOptionsOf(
    maxMessages = 50,
    expiresInMillis = 2_000,
    maxBytes = 4L * 1024 * 1024,
)
```

Bound both message count and bytes so one large payload cannot monopolize a batch. Test acknowledgment behavior, partial batches, and redelivery order after a processing failure.

## Not-found handling

`getStreamInfoOrNull`, `streamExists`, `getConsumerInfoOrNull`, and `consumerExists` translate only JetStream not-found into `null` or `false`. Authorization, invalid configuration, timeout, and network failures propagate.

`forcedPurgeStream` and `forcedDelete*` follow the same rule. They tolerate an already absent target but do not decide whether an actual purge or deletion is safe.

## Sources and tests

- [`JetStream.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/JetStream.kt)
- [`JetStreamManagement.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/JetStreamManagement.kt)
- [`ConsumerContext.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/ConsumerContext.kt)
- [`FetchConsumeOptions.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/api/FetchConsumeOptions.kt)
- [`NatsManagementExtensionsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/NatsManagementExtensionsTest.kt)
- [`ContextExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/jetstream/simple/ContextExample.kt)
- [`FetchMessagesExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/jetstream/simple/FetchMessagesExample.kt)

Tests verify subject merge order, skipped no-op updates, and propagation of failures other than not-found. Cover retention and redelivery against a real server.
