# Redis 동기화 Primitive

[English](./CoordinationSynchronizers.md) | 한국어

Synchronizer package는 기존 `LettuceSemaphore` API를 바꾸지 않고 Redis-authoritative 조정 객체 세 가지를
추가합니다. 모든 객체는 dispatch 전에 codec wire byte로 derived key를 검증하고, multi-key mutation을 Lua로
원자적으로 실행하며, typed blocking, `CompletableFuture`, suspend 결과를 제공합니다.

## 선택 기준

| 객체 | 용량 기준 | 장애 복구 | Identity |
|---|---|---|---|
| `LettuceDistributedSemaphore` | 고정 Redis count | 호출자가 전체 handle을 release | owner + request + generation + allocation token |
| `LettucePermitExpirableSemaphore` | unit deadline을 가진 고정 Redis count | Redis-time cleanup이 allocation을 정확히 한 번 복구 | owner + request + generation + allocation + unit permit ID |
| `LettuceCountDownLatch` | 단조 generation의 Redis count | 제한된 await가 완료, 삭제, generation 변경을 관찰 | generation + request |

## 기본 계약

```kotlin
val semaphore = LettuceDistributedSemaphore.create(connection, "image-workers")
semaphore.trySetPermits(8)
val acquired = semaphore.tryAcquire(
    SemaphoreOwnerId.random(),
    SemaphoreRequestId.random(),
    permits = 2,
)
if (acquired is PermitAcquireResult.Acquired) {
    try {
        processImages()
    } finally {
        semaphore.release(acquired.handle)
    }
}
```

```kotlin
val expirable = LettucePermitExpirableSemaphore.create(
    connection,
    "transcode-workers",
    ExpirableSemaphoreConfig(leaseTime = Duration.ofSeconds(30)),
)
expirable.trySetPermits(4)
val allocation = expirable.tryAcquire(SemaphoreOwnerId.random(), SemaphoreRequestId.random(), permits = 2)
if (allocation is PermitAcquireResult.Acquired) {
    expirable.renew(allocation.handle, Duration.ofSeconds(30))
    expirable.release(allocation.handle)
}
```

```kotlin
val latch = LettuceCountDownLatch.create(connection, "batch-42")
val generation = (latch.trySetCount(3, LatchRequestId.random()) as LatchSetCountResult.Created).generation
latch.countDown(generation, LatchRequestId.random())
val outcome = latch.await(generation, LatchRequestId.random(), Duration.ofSeconds(10))
```

Request ID는 tracing label이 아니라 idempotency identity입니다. 하나의 ambiguous operation을 재조정할 때는
같은 ID를 유지하고, 새로운 logical mutation에는 새 ID를 사용하세요. owner, request, allocation, permit ID를
로그에 기록하지 마세요.

## Redis와 운영

- 필요한 key command, `EVALSHA`, `EVAL`, `TIME`을 ACL로 허용하세요. TLS와 credential은 호출자가 소유한
  Lettuce connection에 설정합니다. Synchronizer factory는 connection을 만들거나 닫지 않습니다.
- tenant와 환경마다 namespace와 shared hash tag를 분리하세요. 한 객체의 모든 key는 같은 Redis Cluster
  slot으로 encode되어야 합니다. Custom codec은 command dispatch 전에 encoded wire byte로 검증됩니다.
- backend/integrity failure, unavailable/timeout 비율, stale generation, cleanup backlog를 경보 대상으로
  두세요. Metric dimension은 제한된 값만 사용하고 Redis key나 identity를 포함하지 마세요.
- 비동기 polling은 connection이 소유하는 bounded coordination runtime을 사용하며, `close()`는 대기 중인
  작업을 종료합니다.
- `LatchConfig.maxWaiters` 기본값은 10,000입니다. Redis 시간 기준 waiter deadline은 register, inspect,
  delete 판단 전에 정리됩니다.
- 먼저 caller를 rollback한 뒤 state를 삭제하세요. 모든 owner, expirable allocation, latch waiter가 없을 때만
  versioned key를 삭제합니다. Latch generation key만 따로 삭제하면 안 됩니다.
- Facade close는 caller-owned connection을 닫거나 성공한 handle을 암묵적으로 release하지 않습니다.
  종료 전에 소유 handle을 reconcile하거나 release하세요.

## Migration

`LettuceSemaphore`는 계속 지원됩니다. Request reconciliation, generation-bound handle, Redis-time expiry,
latch lifecycle이 필요할 때만 이동하세요. 같은 logical resource에 legacy key와 새 semaphore key를 섞지
마세요. Delivery 3 API convergence는 이 additive delivery와 분리되어 있습니다.
