# Coordination Locks

English | [한국어](./CoordinationLocks.ko.md)

The coordination Lock family adopts the familiar “one object per coordination primitive” mental model while keeping
Lettuce-native blocking, `CompletableFuture`, and coroutine APIs. It is additive: the six legacy token mutex and lease
surfaces remain supported and are not deprecated in Delivery 1.

## Choose a Lock object

| Family | Choose it for | Specialized contract |
|---|---|---|
| `LettuceDistributedLock` | Reentrant single-resource exclusion | `LockHandle`, fixed or watchdog lease |
| `LettuceFairLock` | FIFO admission with bounded stale-waiter cleanup | `LockHandle`, `CleanupPending` |
| `LettuceFencedLock` | Downstream stale-writer rejection | `FencedLockHandle`, `(epoch, sequence)` token |
| `LettuceReadWriteLock` | Writer-preference read sharing | `ReadLockHandle`, `WriteLockHandle`, downgrade |
| `LettuceSpinLock` | Very short critical sections with bounded polling | `LockHandle`, bounded backoff and rate |
| `LettuceMultiLock` | Atomic all-or-nothing same-slot resource sets | `MultiLockHandle`, immutable normalized names |

Every family also has a suspend counterpart: `LettuceSuspendDistributedLock`, `LettuceSuspendFairLock`,
`LettuceSuspendFencedLock`, `LettuceSuspendReadWriteLock`, `LettuceSuspendSpinLock`, and
`LettuceSuspendMultiLock`. The blocking objects expose synchronous and
`CompletableFuture` methods; suspend objects expose suspending methods and a deliberately non-suspending `close()`.

![Architecture of the six Lettuce coordination Lock families and their shared runtime](../../docs/images/readme-diagrams/infra-lettuce-diagram-03.png)

The shared runtime owns bounded waiting, watchdog scheduling, sanitized observation, and Lua execution. Redis remains
the ownership authority. A lock object does not own the connection or an injected scheduler.

## Lifecycle and examples

![Lifecycle from acquisition through reconciliation, release, and local close](../../docs/images/readme-diagrams/infra-lettuce-sequence-02.png)

Create owner identity once per logical process or worker. Create one request identity for each acquisition request and
reuse that exact pair when retrying or reconciling the same request.

### Blocking acquisition

```kotlin
val lock = LettuceDistributedLock.create(connection, "orders")
val ownerId = LockOwnerId.from("checkout-worker-7")
val requestId = LockRequestId.random()
val result = lock.acquire(
    ownerId,
    requestId,
    Duration.ofSeconds(2),
    LeasePolicy.Fixed(Duration.ofSeconds(15)),
)

when (result) {
    is LockAcquireResult.Acquired -> try {
        processOrder()
    } finally {
        lock.release(result.handle)
    }
    is LockAcquireResult.Ambiguous -> lock.reconcile(result.ownerId, result.requestId)
    LockAcquireResult.TimedOut -> deferOrder()
    else -> recordRejectedAcquisition(result)
}
```

### Async acquisition

```kotlin
val ownerId = LockOwnerId.from("async-checkout")
val requestId = LockRequestId.random()
val future: CompletableFuture<LockAcquireResult<LockHandle>> =
    lock.acquireAsync(
        ownerId,
        requestId,
        Duration.ofSeconds(2),
        LeasePolicy.Fixed(Duration.ofSeconds(15)),
    )

future.thenCompose { result ->
    when (result) {
        is LockAcquireResult.Acquired -> lock.releaseAsync(result.handle)
        is LockAcquireResult.Reentered -> lock.releaseAsync(result.handle)
        else -> CompletableFuture.completedFuture(null)
    }
}
```

### Suspend acquisition and close

```kotlin
val suspendLock = LettuceSuspendDistributedLock.create(connection, "orders")
val ownerId = LockOwnerId.from("coroutine-checkout")
val requestId = LockRequestId.random()

when (val result = suspendLock.acquire(
    ownerId,
    requestId,
    Duration.ofSeconds(2),
    LeasePolicy.Fixed(Duration.ofSeconds(15)),
)) {
    is LockAcquireResult.Acquired -> suspendLock.release(result.handle)
    is LockAcquireResult.Ambiguous -> suspendLock.reconcile(result.ownerId, result.requestId)
    else -> Unit
}
suspendLock.close() // non-suspending: stops local registrations and new work
```

`close()` stops new work, waiting registrations, and owned local watchdog tasks. It never closes the Redis connection
or an injected scheduler, and it does not release Redis ownership. Release active holds through their handles or let
their bounded leases expire.

### Reentry is request-bound

Every successful acquisition request creates one request-bound hold that must be released exactly once. Replaying the
same request ID returns the same hold; a different request ID under the same owner creates a nested hold.

```kotlin
val ownerId = LockOwnerId.from("reentrant-worker")
val outerRequest = LockRequestId.random()
val innerRequest = LockRequestId.random()

val outer = lock.tryAcquire(ownerId, outerRequest, LeasePolicy.Fixed(Duration.ofSeconds(15)))
    as LockAcquireResult.Acquired
val inner = lock.tryAcquire(ownerId, innerRequest, LeasePolicy.Fixed(Duration.ofSeconds(15)))
    as LockAcquireResult.Reentered

check(lock.release(inner.handle) is LockMutationResult.Released)
check(lock.release(outer.handle) is LockMutationResult.Released)
check(lock.release(outer.handle) is LockMutationResult.AlreadyReleased)
```

## Policy-specific obligations

### Fixed lease and watchdog

Use `LeasePolicy.Fixed` when the critical section has a firm upper bound. Use `LeasePolicy.Watchdog` only with an
explicit TTL, renewal interval, and maximum lifetime; watchdog operation is never indefinite.

```kotlin
val fixed = LeasePolicy.Fixed(Duration.ofSeconds(15))
val watchdog = LeasePolicy.Watchdog(
    ttl = Duration.ofSeconds(12),
    renewalInterval = Duration.ofSeconds(3),
    maxLifetime = Duration.ofMinutes(2),
)
```

An `OwnershipLost` inspection or mutation result means the caller must stop protected work and reconcile with the
durable authority. The library does not claim that a stale process is forcibly stopped.

### Fair timeout cleanup

Fair acquisition cleans only a bounded waiter batch. `TimedOut` means the wait elapsed; `CleanupPending` means exact
waiter removal is still ambiguous. Reconcile the same owner/request identity before submitting a replacement request.

### Fencing

Call `bootstrapFencing()` before serving traffic and preserve the Redis counter across rollout, rollback, backup, and
restore. A downstream store must accept a token only when it is strictly greater than the last committed token:

```sql
UPDATE orders
SET status = :status, fence_epoch = :epoch, fence_sequence = :sequence
WHERE id = :id
  AND (fence_epoch, fence_sequence) < (:epoch, :sequence);
```

Fencing rejects stale writes only where the downstream system enforces this strict-greater guard.

### Read/write downgrade

Use `lock.readLock()` and `lock.writeLock()`; read-to-write upgrade is unsupported. `downgrade(writeHandle)` atomically replaces a
write hold with the returned read hold. Release that returned read handle, not the original write handle.

### Spin bounds

`SpinLockConfig` bounds initial/max delay, multiplier, jitter, and `maxAttemptsPerSecond`. The caller wait is also
bounded. Do not use spin locks for long critical sections or as an indefinite retry loop.

### Same-slot multi-lock

All names must resolve to one Redis Cluster slot. Configure a shared hash tag and pass logical resource names:

```kotlin
val config = MultiLockConfig(lock = LockConfig(hashTag = "sale-42"))
val multi = LettuceMultiLock.create(
    connection,
    listOf("inventory-a", "inventory-b"),
    config,
)
```

Cross-slot best effort is intentionally unsupported; acquisition is atomic or rejected.

<!-- coordination-locks:ambiguous-reconcile -->
## Ambiguous completion and reconciliation

Cancellation or transport failure after dispatch can produce `LockAcquireResult.Ambiguous`. Do not create a new
request ID. Call `reconcile(ownerId, requestId)` with the exact original identity until it yields an owned, released,
queued, removed, not-found, or explicit failure result. A recovered handle still needs exactly one release.

<!-- coordination-locks:watchdog-leak -->
## Watchdog leak prevention

Bound `ttl`, `renewalInterval`, `maxLifetime`, active watchdog registrations, and scheduler tasks. Always close unused
lock objects, alert on capacity rejection and renewal failure, and drain active handles before process termination.
Object close removes local registrations but never implies unlock.

<!-- coordination-locks:observability -->
## Observability

Supply a `LockObservationSink` when metrics are required. Events are sanitized and do not expose owner IDs, request
IDs, resource names, namespaces, raw Redis replies, or credentials. Track operation, family, topology, outcome,
recovery action, and bounded latency.

<!-- coordination-locks:alerts -->
## Metrics and alerts

Alert on sustained `BackendFailure`, `IntegrityFailure`, `Ambiguous`, `CleanupPending`, `CapacityExceeded`,
`OwnershipLost`, watchdog renewal failure, queue saturation, and p95/p99 acquisition latency. A single contention
result is demand, not an incident; use rates and service-specific thresholds.

<!-- coordination-locks:acl-tls -->
## ACL, TLS, and credentials

Grant the application only the keyspace and commands required by the deployed lock family: `EVALSHA`, `EVAL`,
`SCRIPT LOAD`, and the Redis key commands invoked inside the scripts (`GET`, `SET`, `DEL`, `PTTL`, `PEXPIRE`, hashes,
sorted sets, and publish/stream commands where the selected wait path uses them). Validate the exact command set
against deployment tests. Require TLS where traffic crosses a trust boundary, keep credentials in a secret manager,
rotate them without logging connection strings, and use separate principals for applications and operators.

<!-- coordination-locks:namespace-migration -->
## Versioned namespace migration

Use an explicit versioned namespace such as `bt4k:coord:v1`. Never point two incompatible protocols at one namespace.
For a namespace change, stop new acquisitions, drain or expire active holds, preserve fencing generations and
counters, deploy readers/writers for the new namespace, then remove old keys with a bounded audited cleanup.

<!-- coordination-locks:rollout-rollback -->
## Rollout and rollback

Canary the new object behind a caller-side switch. During rollout, compare outcome/latency signals without acquiring
the old and new lock for the same critical section. Roll back callers to the previous API without deleting state,
resetting fencing counters, or reusing request IDs. Keep the new namespace readable until all ambiguous requests are
reconciled and active leases have expired.

<!-- coordination-locks:drain-cleanup -->
## Drain and cleanup

Stop new acquisitions, wait for known handles to release or bounded leases to expire, close local objects, reconcile
ambiguous requests, and delete stale queue/request records in bounded batches. Preserve generation and fencing-counter
state until rollback is impossible. Never run unbounded wildcard deletion in production.

## Migration from existing primitives

| Existing supported API | New family when stronger semantics are needed |
|---|---|
| `LettuceLock` | `LettuceDistributedLock` |
| `LettuceSuspendLock` | `LettuceSuspendDistributedLock` |
| `LettuceFencingLease` | `LettuceFencedLock` |
| `LettuceSuspendFencingLease` | `LettuceSuspendFencedLock` |
| `LettuceMultiKeyLease` | `LettuceMultiLock` |
| `LettuceSuspendMultiKeyLease` | `LettuceSuspendMultiLock` |

The existing APIs are compatibility token mutexes/leases. They remain supported and are not deprecated in Delivery 1.
Migrate only when typed outcomes, explicit identity, reconciliation, reentry, watchdog bounds, specialized handles, or
the new object model provide value.

## Non-goals

- No Java thread ownership: ownership is explicit `LockOwnerId`/`LockRequestId`.
- No indefinite wait or watchdog: every wait, lease, renewal, queue, and task path is bounded.
- No cross-slot best effort: multi-lock requires one Redis Cluster slot.
- No read-to-write upgrade: only write-to-read downgrade is supported.
- No exactly-once or automatic stale-work-stop claim: callers reconcile and downstream systems enforce fencing.
- No implicit unlock on `close()`: ownership is released by handle or lease expiry.
