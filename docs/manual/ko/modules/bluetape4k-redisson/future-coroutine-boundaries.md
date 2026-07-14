---
title: Future와 coroutine 경계
description: RFuture, CompletableFuture, suspend batch와 transaction에서 실패와 cancellation을 보존하는 방법을 설명합니다.
manualId: bluetape4k-redisson
chapterId: future-coroutine-boundaries
---

# Future와 coroutine 경계

## 한 호출 경로에서 한 실행 모델을 쓴다

Redisson은 동기 API와 `RFuture` 기반 async API를 함께 제공합니다. coroutine service에서는 async API를 `await()`하고, 동기 `get()`이나 future의 blocking `get()`을 섞지 않습니다. coroutine 문법 안에서 blocking call을 실행한다고 non-blocking Redis I/O가 되지는 않습니다.

```kotlin
suspend fun loadProfiles(client: RedissonClient, ids: List<String>): List<Profile?> {
    val map = client.getMap<String, Profile>("profiles")
    return ids.map { map.getAsync(it) }.awaitAll()
}
```

`awaitAll()`은 빈 collection이면 빈 list를 반환하고, 입력 순서를 보존합니다. 하나라도 실패하면 완성된 일부 결과를 반환하지 않고 예외를 전파합니다.

## sequence와 dispatcher

`Iterable<RFuture<V>>.sequence()`는 `CompletableFuture<List<V>>`를 만들며 기본 executor로 virtual thread executor를 사용합니다. `awaitAll()`은 현재 coroutine dispatcher를 executor로 바꾸고, 없으면 `Dispatchers.Default`를 사용합니다. 둘 다 remote command를 새로 예약하는 API가 아니라 이미 만든 future를 모으는 adapter입니다.

동시에 너무 많은 command를 먼저 만들면 Redis connection과 caller memory에 부하가 쌓입니다. ID 목록이 크면 chunk로 나누거나 Redisson의 bulk operation을 먼저 검토합니다.

## Suspend batch

`withSuspendedBatch`는 DSL block에서 command를 등록한 뒤 `executeAsync().await()`합니다.

```kotlin
val result = client.withSuspendedBatch {
    getBucket<String>("a").setAsync("A")
    getBucket<String>("b").setAsync("B")
}
```

block 안에서는 async method를 호출해야 합니다. 여기서 반환된 future를 별도로 기다리지 않아도 batch execution이 command를 실행하고 결과를 모읍니다. batch는 network optimization이며 transaction이 아닙니다.

## Suspend transaction과 cancellation

`withSuspendedTransaction`은 commit과 rollback도 async operation으로 수행합니다. action 또는 commit이 실패하면 rollback을 시도하고 원래 예외를 던집니다. rollback을 기다리는 동안 `CancellationException`이 발생하면 cancellation을 보존합니다.

```kotlin
client.withSuspendedTransaction {
    getMap<String, Long>("balances").putAsync("42", 1_000L)
}
```

coroutine cancellation이 이미 Redis server에 전달된 command를 반드시 취소한다고 가정하면 안 됩니다. timeout 후 재시도할 operation은 request ID나 compare-and-set 같은 중복 방지 장치를 둡니다.

## 실패를 감추지 않는다

`runCatching { ... }.getOrNull()`로 Redis timeout이나 cancellation을 모두 cache miss로 바꾸면 장애가 정상 miss처럼 보입니다. optional cache lookup이라도 timeout, serialization failure, cancellation을 구분하고 metric을 남깁니다.

## Source와 tests

- [`RFutureSupport.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/coroutines/RFutureSupport.kt)
- [`RedissonClientCoroutine.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/coroutines/RedissonClientCoroutine.kt)
- [`RFutureSupportTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/coroutines/RFutureSupportTest.kt)
- [`RedissonClientCoroutineTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/coroutines/RedissonClientCoroutineTest.kt)
- [`GetLockIdTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/coroutines/GetLockIdTest.kt)
