---
title: Client와 분산 객체·Stream
description: Redisson client ownership, batch와 transaction, lock ID, Stream consumer-group helper의 계약을 설명합니다.
manualId: bluetape4k-redisson
chapterId: client-distributed-objects-streams
---

# Client와 분산 객체·Stream

## Client를 만든 쪽이 닫는다

`redissonClient {}`와 `redissonClientOf(config)`는 호출할 때마다 새 `RedissonClient`를 만들지만 자동으로 종료하지 않습니다. 직접 생성했다면 application lifecycle에서 한 번만 만들고 종료 hook에서 `shutdown()`합니다. `redissonReactiveClientOf`도 내부에서 만든 client의 reactive view를 반환하므로 같은 ownership 규칙을 적용합니다.

```kotlin
class RedisResources : AutoCloseable {
    val client = redissonClient {
        useSingleServer().address = "redis://127.0.0.1:6379"
    }

    override fun close() = client.shutdown()
}
```

Spring Boot starter가 client bean을 관리한다면 별도의 client를 만들지 않습니다. 두 client를 만들면 connection pool과 Netty thread도 두 벌이 됩니다.

## Batch와 transaction은 다른 도구다

`withBatch`는 여러 async command를 모아 한 번에 실행해 왕복을 줄입니다. command 전체의 원자성을 뜻하지 않습니다. `withTransaction`은 action이 끝나면 commit하고, 예외가 나면 rollback을 시도한 뒤 원래 예외를 다시 던집니다.

```kotlin
val result = client.withBatch {
    getBucket<String>("profile:42").setAsync("Debop")
    getAtomicLong("profile:revision").incrementAndGetAsync()
}

client.withTransaction {
    getMap<String, Long>("balances").put("42", 1_000L)
    getMap<String, String>("ledger").put("tx-1", "credited")
}
```

rollback도 remote operation이므로 실패할 수 있습니다. 1.12.1 helper는 rollback 실패로 원래 application failure를 덮지 않습니다. 충돌·timeout을 재시도하려면 transaction block 전체가 idempotent한지 먼저 확인합니다.

## 분산 객체와 lock owner

Redisson의 `RMap`, `RBucket`, `RLock`, `RAtomicLong`은 Redis object를 참조하는 proxy입니다. client를 닫은 뒤 proxy를 계속 사용할 수 없습니다. coroutine은 여러 thread를 오갈 수 있으므로 `getLockId(lockName)`은 thread ID 대신 Snowflake ID를 발급합니다.

```kotlin
val lockId = client.getLockId("invoice:42")
val lock = client.getLock("invoice:42")
val acquired = lock.tryLockAsync(1_000, 10_000, MILLISECONDS, lockId).await()
try {
    if (acquired) issueInvoice()
} finally {
    if (acquired) lock.unlockAsync(lockId).await()
}
```

lease time과 unlock은 같은 lock ID를 사용해야 합니다. cancellation이 발생해도 `finally`에서 unlock을 시도하도록 구조화합니다.

## Stream consumer group helper

`RStreamSupport`는 entry 생성과 여러 message의 ACK·claim을 한 호출로 묶습니다. `ackAllAsync`는 group name과 ID 목록을 검증하고 ACK된 개수를 반환합니다. `claimAllAsync`는 message body를, `fastClaimAllAsync`는 ID만 돌려줍니다.

```kotlin
val claimed = stream.claimAllAsync(
    groupName = "billing",
    consumerName = "worker-2",
    idleTime = Duration.ofSeconds(30),
    ids = pendingIds,
).await()

processIdempotently(claimed)
stream.ackAllAsync("billing", claimed.keys).await()
```

claim은 소유권을 옮길 뿐 exactly-once 처리를 만들지 않습니다. 처리 결과와 ACK 사이에 장애가 나면 같은 message가 다시 보일 수 있으므로 business operation을 idempotent하게 만듭니다.

## Source와 tests

- [`RedissonClientSupport.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt)
- [`RedissonClientExtensions.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientExtensions.kt)
- [`RStreamSupport.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RStreamSupport.kt)
- [`RedissonClientExtensionsTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonClientExtensionsTest.kt)
- [`RStreamSupportTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RStreamSupportTest.kt)
