---
title: Coroutine producer
description: Covers native Producer callback awaiting, cancellation, Flow concurrency, and the normal-completion flush boundary.
manualId: bluetape4k-kafka
chapterId: coroutine-producer
---

# Coroutine producer

## Await callbacks with `suspendSend`

`Producer.suspendSend(record)` waits for the Kafka callback with `suspendCancellableCoroutine`. It propagates callback failures and returns `RecordMetadata` on success.

```kotlin
val metadata = producer.suspendSend(
    ProducerRecord("orders", order.id, order)
)
```

If the waiting coroutine is cancelled, the implementation calls `cancel(true)` on the Kafka future. This attempts to stop local waiting. It does not confirm that the broker did not write the record, so account for duplicates when retrying after cancellation or timeout.

## `sendAsFlow`

`sendAsFlow(records)` applies `buffer()` and bluetape4k `Flow.async`, then returns `Flow<RecordMetadata>`.

```kotlin
producer.sendAsFlow(records).collect { metadata ->
    audit(metadata.topic(), metadata.partition(), metadata.offset())
}
```

Read the `Flow.async` contract for concurrency and ordering. Kafka preserves the recorded order within a partition, but completion order and global order across partitions are different contracts. When business ordering matters, use a stable key/partition and test both send completions and consumed records.

## Return only the last result

`sendAsFlowParallel(records)` performs the same buffered async sends and returns `.last()`. Empty input throws `NoSuchElementException`. Do not treat the final metadata as the business result for every preceding record; document that one send failure fails the collection.

## What fire-and-forget means here

Despite its name, `sendAndForget` collects the complete flow and awaits each `suspendSend`; it only discards metadata. Failures still propagate.

```kotlin
producer.sendAndForget(records, needFlush = true)
```

In 1.12.1 it optionally calls `flush()` only after normal completion. It avoids flushing during failure or cancellation, where flush could block on failed sends or hide the original exception. The caller still closes the client.

## Throughput and backpressure

Buffered async sends can improve throughput, but they also interact with producer buffers, `max.in.flight.requests.per.connection`, and application memory. Bound concurrency for large upstreams and observe timeouts and producer buffer metrics.

## Sources and tests

- [`ProducerCoroutines.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/coroutines/ProducerCoroutines.kt)
- [`ProducerSupportTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/coroutines/ProducerSupportTest.kt)

The tests verify callback failure propagation and future cancellation without a broker; actual Flow sends use Testcontainers.
