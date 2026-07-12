---
title: Async channel and lifecycle
description: Understand SharedFlow buffering, collection, scope ownership, close, and post-close events.
manualId: bluetape4k-logging
chapterId: async-channel
---

# Async channel and lifecycle

`KLoggingChannel` publishes events to a `MutableSharedFlow`; a background collector emits them to SLF4J. This separates caller latency from emission lifecycle at the cost of buffering and shutdown contracts.

![KLoggingChannel send, collect, close, and post-close flow](../../../assets/logging/async-channel-sequence.svg)

## Runtime model

- replay is 0;
- extra buffer capacity is 64;
- overflow policy is `SUSPEND`;
- default instances share `Dispatchers.IO + SupervisorJob + CoroutineName("logchannel")`;
- one JVM shutdown hook cancels the shared job.

`SUSPEND` does not mean unbounded asynchronous delivery. When a slow collector fills the buffer, `send` suspends in `emit`.

## Use and shutdown

```kotlin
class ImportWorker : AutoCloseable {
    private val logger = object : KLoggingChannel() {}

    suspend fun run(id: String) {
        logger.info { "Import started id=$id" }
    }

    override fun close() = logger.close()
}
```

`close()` is idempotent and cancels only this instance's collector. An injected `CoroutineScope` remains caller-owned. Tests and suspend lifecycle callbacks can use `closeAndJoin()` to wait for collector termination.

## Delivery boundary

Close currently cancels; it does not drain. Events buffered immediately before close are not guaranteed to reach the backend. `send` after close drops the event without blocking. This is not a transport for audit or must-persist events.

The collector catches an exception from an individual emission and attempts an error log. It rethrows `CancellationException` to preserve cancellation semantics.

## Decision table

| Condition | Choice |
| --- | --- |
| Ordinary request/service logs | `KLogging` |
| Measured appender bottleneck and suspend backpressure acceptable | consider `KLoggingChannel` |
| Every pending event must survive shutdown | durable queue or explicit drain design |
| Audit/security event | durable event pipeline, not a logging channel |

## Source and tests

- [`KLoggingChannel.kt`](../../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/coroutines/KLoggingChannel.kt)
- [`KLoggingChannelTest.kt`](../../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging/coroutines/KLoggingChannelTest.kt)

Finish with configuration and diagnostics in [Operations & recipes](./operations-recipes.md).
