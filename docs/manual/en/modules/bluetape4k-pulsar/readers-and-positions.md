---
title: Readers and positions
description: Subscription-free Reader usage, MessageId start positions, single reads, and finite readAsFlow behavior.
manualId: bluetape4k-pulsar
chapterId: readers-and-positions
---

# Readers and positions

## A different purpose from Consumer

A Reader reads a topic from a selected position without creating a subscription. It sends no acknowledgement. Use it for replay, data inspection, migration, and tools that do not maintain consumption state as a subscription.

```kotlin
val reader = client.reader(Schema.STRING) {
    topic("persistent://public/default/orders")
    startMessageId(MessageId.earliest)
}
```

The helper applies setup to the native builder and calls synchronous `create()`. Pulsar Client validates missing or invalid positions.

## Start-position semantics

`MessageId.earliest` starts from the earliest retained position. `MessageId.latest` starts at the end observed when the Reader is created. A stored `MessageId` can act as a replay checkpoint, but partition identity, retention, and deletion policy also constrain it.

Recreating a Reader at the same position can read the same messages again. Reader stores no application processing checkpoint.

## Single reads

`readNextSuspend()` awaits `readNextAsync()` and returns the next `Message<T>`.

```kotlin
val message = reader.readNextSuspend()
inspect(message.value)
```

It can wait when no message is available. Define caller timeout and cancellation policy.

## readAsFlow drains the current backlog

`readAsFlow()` emits `readNextAsync()` results only while `hasMessageAvailable()` is true.

```kotlin
client.withReader(Schema.STRING, {
    topic(topic)
    startMessageId(MessageId.earliest)
}) {
    readAsFlow().collect { inspect(it.value) }
}
```

The Flow completes normally as soon as `hasMessageAvailable()` returns false. It is not a tailing stream that waits for future messages. Release tests read pre-published messages and return an empty list from a latest-position Reader on an empty topic.

A message arriving just after the availability check may not appear in that collection. Use Consumer `receiveAsFlow()` for a continuous stream or design an explicit polling boundary.

## Cancellation and ownership

If cancellation occurs while awaiting a read future, `readAsFlow` calls `future.cancel(true)` and rethrows cancellation. Flow does not close Reader. A `withReader` block or the caller closes it and its parent client.

The 1.12.1 `withReader` close is not non-cancellable cleanup. A long-running tool should own its shutdown deadline and observe close results directly.

## Sources and tests

- [`ReaderSupport.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/reader/ReaderSupport.kt)
- [`ReaderExtensions.kt`](../../../../../infra/pulsar/src/main/kotlin/io/bluetape4k/pulsar/reader/ReaderExtensions.kt)
- [`ReaderSupportTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/reader/ReaderSupportTest.kt)
- [`ReaderExtensionsTest.kt`](../../../../../infra/pulsar/src/test/kotlin/io/bluetape4k/pulsar/reader/ReaderExtensionsTest.kt)
