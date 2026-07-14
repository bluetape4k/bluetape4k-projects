---
title: Client와 connection
description: Lettuce client, cached connection, 공유 resource와 pipeline의 소유권을 정합니다.
manualId: bluetape4k-lettuce
chapterId: clients-and-connections
---

# Client와 connection

## 기본 client가 만드는 것

`LettuceClients.clientOf`는 모든 기본 client가 공유하는 `DEFAULT_CLIENT_RESOURCES`를 사용하고 keep-alive, TCP_NODELAY, connect timeout을 적용합니다. client마다 event-loop pool을 만들지는 않지만, 공유 resource는 process 수명과 묶입니다.

```kotlin
val client = LettuceClients.clientOf("redis://redis:6379")
val connection = LettuceClients.connect(client)
check(connection.sync().ping() == "PONG")
LettuceClients.shutdown(client)
```

같은 client와 같은 codec으로 다시 연결하면 열린 cached connection을 재사용합니다. 닫힌 connection은 client별 `ReentrantLock` 아래에서 다시 만듭니다. 이 cache는 pool이 아닙니다. 여러 요청이 하나의 thread-safe connection을 공유하는 구조입니다.

## 종료 순서

`shutdown(client)`는 해당 client의 기본·codec connection을 닫고 client를 종료합니다. 인자 없는 `shutdown()`은 공유 `ClientResources`를 닫습니다. 여러 client가 살아 있는 동안 공유 resource를 먼저 닫지 않습니다. `LettuceLoadedMap`처럼 직접 `client.connect(codec)`한 객체는 자체 `close()`를 호출해야 합니다.

## pipeline에서는 발행만 한다

```kotlin
val futures = connection.withPipeline { commands ->
    (1..100).map { commands.set("item:$it", "v$it") }
}
futures.awaitAll()
```

`withPipeline`은 auto flush를 끄고 block이 끝날 때 한 번 flush한 뒤 `finally`에서 복원합니다. block 안에서 `await`하거나 `get()`하면 flush 전 결과를 기다리므로 멈출 수 있습니다.

## Source와 tests

- [`LettuceClients.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceClients.kt)
- [`LettuceClientsTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/LettuceClientsTest.kt)

다음은 [Command와 coroutine](./commands-and-coroutines.md)입니다.
