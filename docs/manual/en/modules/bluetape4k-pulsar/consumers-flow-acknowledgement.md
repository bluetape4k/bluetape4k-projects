---
title: Consumers, Flow, and acknowledgements
description: Subscription configuration, an unbounded receive Flow, individual and cumulative ack, and processing failures.
manualId: bluetape4k-pulsar
chapterId: consumers-flow-acknowledgement
---

# Consumers, Flow, and acknowledgements

## Design the subscription first

A Consumer's subscription name and type define processing semantics together with its topic.

```kotlin
val consumer = client.consumer(orderSchema) {
    topic("persistent://public/default/orders")
    subscriptionName("order-fulfillment")
    subscriptionType(SubscriptionType.Shared)
}
```

The `consumer` helper applies setup to the native builder and calls synchronous `subscribe()`. Pulsar settings own subscription creation, receiver queues, ack timeout, and dead-letter policy.

## Single receive and individual ack

`receiveSuspend()` awaits `receiveAsync()`. Call `acknowledgeSuspend(message)` only after business processing succeeds.

```kotlin
val message = consumer.receiveSuspend()
process(message.value)
consumer.acknowledgeSuspend(message)
```

A crash between process and ack can cause redelivery. Design handler idempotency around a message ID or stable business key.

## receiveAsFlow lifecycle

`receiveAsFlow()` is a cold Flow that repeats `receiveAsync()` while its coroutine context remains active. It waits rather than completing when no message is available.

```kotlin
consumer.receiveAsFlow().collect { message ->
    process(message.value)
    consumer.acknowledgeSuspend(message)
}
```

The Flow neither owns nor closes the Consumer and does not acknowledge automatically. Recollection starts another receive loop on the same Consumer, so make multi-collector use an explicit decision.

## Cumulative acknowledgement constraint

`acknowledgeCumulativeSuspend(message)` acknowledges through the given position. Use it with Exclusive or Failover subscriptions. Release tests verify that a Shared subscription raises `PulsarClientException`.

Cumulative ack compresses several processing results into one position. Keep processing sequential and align the failure policy so a later ack cannot hide a failed earlier message.

## Cancellation and errors

When cancellation occurs while awaiting receive, Flow cancels the pending future and rethrows cancellation. Receive and ack failures propagate as Pulsar exceptions. The helpers add no automatic nack, retry, redelivery delay, or dead-letter action.

Flow does not close its Consumer. A `withConsumer` block or explicit owner must do so, and the 1.12.1 `withConsumer` helper does not guarantee close completion during cancellation.

## Operational signals

Observe subscription backlog, unacknowledged messages, redelivery, receive latency, handler latency, and ack failures separately. Increasing the receiver queue while handlers remain slow can increase memory and the unacknowledged window.

## Sources and tests

- [`ConsumerSupport.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/consumer/ConsumerSupport.kt)
- [`ConsumerExtensions.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/consumer/ConsumerExtensions.kt)
- [`ConsumerSupportTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/consumer/ConsumerSupportTest.kt)
- [`ConsumerExtensionsTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/consumer/ConsumerExtensionsTest.kt)
