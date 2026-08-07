---
title: Producers and coroutine sends
description: Producer DSLs, value and message-builder sends, sequential Flow, cancellation, and failure boundaries.
manualId: bluetape4k-pulsar
chapterId: producers-coroutine-send
---

# Producers and coroutine sends

## Creating a Producer

`client.producer(schema) { ... }` applies setup to native `newProducer(schema)` and calls synchronous `create()`.

```kotlin
val producer = client.producer(Schema.STRING) {
    topic("persistent://public/default/orders")
    producerName("order-api")
    compressionType(CompressionType.LZ4)
}
```

Pulsar Client owns validation and defaults for topics, routing, batching, pending queues, and timeouts. The helper does not change them.

## Two single-send forms

Use `sendSuspend(message)` for a value. Use the message-builder overload for keys, properties, event time, or other metadata.

```kotlin
val first = producer.sendSuspend(order)

val second = producer.sendSuspend {
    value(order)
    key(order.id)
    property("source", "order-api")
}
```

Both await `sendAsync()` with `awaitSuspending()` and return the broker's `MessageId`. Pulsar exceptions are not translated into domain exceptions.

## sendAsFlow is sequential

`sendAsFlow(messages)` collects the upstream `Flow<T>`, awaits each `sendAsync`, and emits its `MessageId` before taking the next value.

```kotlin
val ids = producer.sendAsFlow(orders.asFlow()).toList()
```

It sends one message at a time in input order. Flow does not imply parallel sends or batching. For throughput, configure Pulsar batching and design bounded concurrency separately after deciding ordering requirements.

## Cold Flow and repeated collection

The returned Flow is cold. Each collection recollects upstream and resends its messages. Collecting the same flow twice can publish the same business messages twice, so control collection ownership.

Retries can also produce duplicates. A message key alone does not provide exactly-once delivery. Select consumer idempotency, Pulsar deduplication, or transaction policy at the application boundary.

## Cancellation and failures

If `sendAsFlow` receives `CancellationException` while awaiting a future, it calls `future.cancel(true)` and rethrows cancellation. Do not assume this rolls back a message already accepted by the broker.

One send failure terminates the Flow and prevents later upstream values from being sent. Record stable business identifiers and send results so operations can determine how far a batch progressed.

## Lifecycle and operations

`withProducer` attempts close for a bounded scope, but 1.12.1 does not guarantee non-cancellable cleanup. Reuse long-lived producers and observe send latency, pending queues, batching, compression, and failure rate.

## Sources and tests

- [`ProducerSupport.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/producer/ProducerSupport.kt)
- [`ProducerExtensions.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/producer/ProducerExtensions.kt)
- [`ProducerSupportTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/producer/ProducerSupportTest.kt)
- [`ProducerExtensionsTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/producer/ProducerExtensionsTest.kt)
