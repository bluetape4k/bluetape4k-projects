---
title: Clients and connections
description: Define ownership for Lettuce clients, cached connections, shared resources, and pipelines.
manualId: bluetape4k-lettuce
chapterId: clients-and-connections
---

# Clients and connections

## What a default client owns

`LettuceClients.clientOf` uses the process-wide `DEFAULT_CLIENT_RESOURCES` and applies keep-alive, TCP_NODELAY, and a connection timeout. Clients avoid creating separate event-loop pools, but the shared resource now has process lifetime.

```kotlin
val client = LettuceClients.clientOf("redis://redis:6379")
val connection = LettuceClients.connect(client)
check(connection.sync().ping() == "PONG")
LettuceClients.shutdown(client)
```

The same client and codec reuse an open cached connection. A closed connection is recreated under a per-client `ReentrantLock`. This cache is not a connection pool; concurrent callers share Lettuce's thread-safe connection.

## Shutdown order

`shutdown(client)` closes that client's default and codec connections before shutting down the client. Parameterless `shutdown()` closes shared `ClientResources`, so call it only when every client is finished. Objects that call `client.connect(codec)` directly, including loaded maps, own and close those connections themselves.

## Pipelines only issue commands

```kotlin
val futures = connection.withPipeline { commands ->
    (1..100).map { commands.set("item:$it", "v$it") }
}
futures.awaitAll()
```

`withPipeline` disables auto-flush, flushes once after the block, and restores auto-flush in `finally`. Awaiting inside the block can wait for a result that has not been flushed.

## Source and tests

- [`LettuceClients.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceClients.kt)
- [`LettuceClientsTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/LettuceClientsTest.kt)

Continue with [Commands and coroutines](./commands-and-coroutines.md).
