---
title: Commands and coroutines
description: Choose sync, async, or coroutine commands and preserve RedisFuture failure and cancellation.
manualId: bluetape4k-lettuce
chapterId: commands-and-coroutines
---

# Commands and coroutines

## Keep one execution model per path

`commands` is blocking, `asyncCommands` returns `RedisFuture`, and `coroutinesCommands` exposes Lettuce's experimental coroutine API. Sync calls can fit a blocking service boundary; coroutine services should remain async or suspending end to end.

```kotlin
val async = LettuceClients.asyncCommands(client)
val value = async.get("account:1").awaitSuspending()
val values = listOf(async.get("a"), async.get("b")).awaitAll()
```

`awaitAll()` preserves input order, returns an empty list for empty input, and propagates a failed future instead of constructing partial results.

## Cancellation is not a cache miss

`awaitSuspending()` follows `kotlinx.coroutines.future.await` cancellation rules. Suspended loaded maps also rethrow `CancellationException`. Converting cancellation into a Redis miss would invoke a database loader after the caller has already abandoned the work.

## Command capability checks

`RedisCommandSupports` caches `COMMAND INFO` per client and command and closes its temporary connection. An inspection failure returns `false`, so permissions and true server incompatibility may need separate operational diagnosis.

## Source and tests

- [`RedisFutureSupport.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupport.kt)
- [`RedisCommandSupports.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisCommandSupports.kt)
- [`RedisFutureSupportTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupportTest.kt)

Continue with [Codecs and serialization](./codecs-and-serialization.md).
