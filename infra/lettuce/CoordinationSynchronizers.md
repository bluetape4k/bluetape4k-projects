# Redis Synchronizers

English | [한국어](./CoordinationSynchronizers.ko.md)

The synchronizer package adds three Redis-authoritative coordination objects without changing the existing
`LettuceSemaphore` API. Every object validates derived keys by codec wire bytes before dispatch, executes multi-key
mutations atomically with Lua, and exposes typed blocking, `CompletableFuture`, and suspending results.

## Selection

| Object | Capacity source | Failure recovery | Identity |
|---|---|---|---|
| `LettuceDistributedSemaphore` | Fixed Redis count | Caller releases the complete handle | owner + request + generation + allocation token |
| `LettucePermitExpirableSemaphore` | Fixed Redis count with unit deadlines | Redis-time cleanup restores one allocation exactly once | owner + request + generation + allocation + unit permit IDs |
| `LettuceCountDownLatch` | Monotonic-generation Redis count | Bounded await observes completion, deletion, or generation change | generation + request |

## Basic contracts

```kotlin
val semaphore = LettuceDistributedSemaphore.create(connection, "image-workers")
val initialized = semaphore.trySetPermits(8)
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

Request IDs are idempotency identities, not tracing labels. Keep the same ID when reconciling one ambiguous operation;
use a new ID for a new logical mutation. Never log owner, request, allocation, or permit IDs.

## Redis and operations

- Use ACL rules that allow the required key commands, `EVALSHA`, `EVAL`, and `TIME`. Configure TLS and credentials on
  the caller-owned Lettuce connection; synchronizer factories never create or close that connection.
- Give each tenant or environment a separate namespace and shared hash tag. All keys for one object must encode to one
  Redis Cluster slot. Custom codecs are validated from their encoded wire bytes before any command is dispatched.
- Alert on backend failures, integrity failures, unavailable/time-out rates, stale generations, and cleanup backlog.
  Dimensions must be bounded and must not contain Redis keys or identity values.
- Async polling uses the connection-owned bounded coordination runtime; `close()` terminates pending waits.
- `LatchConfig.maxWaiters` defaults to 10,000. Redis-time waiter deadlines are cleaned before register, inspect,
  and delete decisions.
- Roll back callers before deleting state. Delete an object's versioned keys only after all owners, expirable
  allocations, and latch waiters are gone. Never delete a latch generation key in isolation.
- Closing a facade stops local use but does not close the caller-owned connection and does not implicitly release a
  successful handle. Resolve or release owned handles before shutdown.

## Migration

`LettuceSemaphore` remains supported. Migrate only when request reconciliation, generation-bound handles, Redis-time
expiry, or latch lifecycle semantics are required. Do not mix legacy and new semaphore keys for the same logical
resource. Delivery 3 API convergence remains separate from this additive delivery.
