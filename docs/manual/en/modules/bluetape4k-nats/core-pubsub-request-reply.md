---
title: Core pub-sub and request-reply
description: Distinguish ephemeral core NATS delivery, publish and flush, request variants, timeouts, and no responders.
manualId: bluetape4k-nats
chapterId: core-pubsub-request-reply
---

# Core pub-sub and request-reply

## Core NATS delivery

Core NATS delivers a subject message to subscribers that currently express interest. It does not store the message in a stream for later replay. This fits short-lived notifications and cache invalidation where only the latest event matters.

```kotlin
connection.publish("catalog.changed", "A-100")
connection.flush(2.seconds)
```

The string overload converts the body to UTF-8 bytes and delegates to jNATS `publish`. It rejects a blank subject but does not validate payload schema, content type, or size.

## What `flush` confirms

`flush(Duration)` converts Kotlin `Duration` to Java `Duration`. The completed server round trip confirms that prior publish commands reached the server. It does not confirm subscriber processing, database changes, or JetStream persistence.

Use request-reply or a business acknowledgment subject when a subscriber must confirm processing. Use the `PublishAck` from `JetStream.publish` when storage acknowledgment is required.

## Request-reply

A request creates an inbox and waits for the first response from a responder on the target subject.

```kotlin
val response = connection.request(
    subject = "inventory.reserve",
    body = "{\"sku\":\"A-100\",\"qty\":1}",
    timeout = 500.milliseconds,
)

if (response == null) {
    // Decide whether timeout is a business failure at this boundary.
}
```

The synchronous overload returns `Message?`. The exact representation of timeout or no responders depends on the selected jNATS overload, so do not infer it only from the wrapper name.

## Futures and coroutines

`requestAsync` returns `CompletableFuture<Message>`. It calls jNATS `requestWithTimeout` when a timeout is present and the ordinary asynchronous request otherwise. `requestSuspending` and `requestWithTimeoutSuspending` await those futures.

```kotlin
val response = withTimeout(1.seconds) {
    connection.requestWithTimeoutSuspending(
        "inventory.reserve",
        payload,
        timeout = 800.milliseconds,
    )
}
```

When jNATS timeout and coroutine `withTimeout` are both used, either deadline may win. Pick one layer as the operational timeout authority and distinguish the source in metrics and logs.

Coroutine cancellation propagates from `await()`. It does not prove that a server handler stopped. A request may already have triggered an external side effect, so retries can require an idempotency key or a status query.

## Queue-group responders

Multiple service instances can use a queue subscription so one instance receives each request. This is load distribution, not a durable queue. Use a JetStream consumer or another workflow when requests must be retained while all responders are offline.

## Sources and examples

- [`ConnectionExtensions.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/ConnectionExtensions.kt)
- [`ConnectionExtensionsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/ConnectionExtensionsTest.kt)
- [`PubSubExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/PubSubExample.kt)
- [`RequestReplyExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/RequestReplyExample.kt)
- [`CoreReplyRequestPatterns.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/CoreReplyRequestPatterns.kt)

`RequestReplyExample` combines a dispatcher responder with synchronous and asynchronous requests. Test no-responder and timeout results against the exact jNATS API and server configuration used by the application.
