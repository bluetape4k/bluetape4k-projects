# Issue #1080 Lettuce Lock Family Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Follow `bluetape-full-feature`, `bluetape-workflow`, `bluetape-kotlin-patterns`, `kotlin-coroutines-skill`, `test-driven-development`, `bluetape-writer`, and `bluetape-diagram`. Testcontainers-backed Redis tasks are serialized across worktrees.

**Goal:** Deliver the first approved #1080 PR as an additive, Redisson-shaped Lettuce Lock family with six lock objects, logical-owner reentrancy, generation-safe handles, fixed/watchdog leases, bounded waiting, typed failure/reconciliation, Redis Cluster safety, and blocking/async/suspend semantic parity.

**Architecture:** Public identity, handle, config, result, and six lock objects live under `io.bluetape4k.redis.lettuce.lock`. Redis-neutral key, script, deadline, runtime, task-registry, and sanitized protocol support lives under `io.bluetape4k.redis.lettuce.coordination.internal` without leaking into public signatures. Lock-specific Lua, result decoding, wait loops, fair/read-write admission, fencing, and multi-lock composition live under `lock.internal`. Existing `LettuceLock`, fencing lease, multi-key lease, and semaphore APIs remain source/binary compatible and unchanged in this delivery.

**Tech Stack:** Kotlin 2.3, Java 21, Lettuce, Redis Lua, Kotlin Coroutines, `CompletableFuture`, JUnit 5, `bluetape4k-assertions`, `bluetape4k-junit5`, `bluetape4k-testcontainers`, Gradle Kotlin DSL, SVG, CairoSVG.

**Approved design:** `docs/superpowers/specs/2026-07-25-issue-1080-lettuce-locks-synchronizers-design.md`

---

## 1. Delivery boundary and stop conditions

### 1.1 This plan implements only Delivery 1

This plan is intentionally limited to:

- `LettuceDistributedLock`
- `LettuceFairLock`
- `LettuceFencedLock`
- `LettuceReadWriteLock`
- `LettuceSpinLock`
- `LettuceMultiLock`
- their `LettuceSuspend*` counterparts
- the neutral internal coordination substrate required by those objects
- English/Korean Lock-family documentation and operator guidance
- focused contract, Redis, Cluster, concurrency, lifecycle, and performance evidence

It does not implement:

- `LettuceDistributedSemaphore`
- `LettucePermitExpirableSemaphore`
- `LettuceCountDownLatch`
- public cross-family type unification
- deprecation or removal of existing lock/lease/semaphore APIs
- a new coordination algorithm during the final convergence delivery

After this plan is implemented, reviewed, published as PR 1, merged with fresh user approval, and locally synchronized, Delivery 2 receives a new implementation plan derived from the merged `develop`. Delivery 3 receives its own plan only after both object families are merged.

### 1.2 GitHub and branch boundary

| Item | Fixed value |
|---|---|
| Repository | `bluetape4k/bluetape4k-projects` |
| Issue | `#1080` |
| PR 1 base | `develop` |
| PR 1 head | `codex/issue-1080-lettuce-locks-design` |
| PR 1 scope | approved spec, Lock implementation plan/review, Lock family implementation, Lock docs/evidence |
| PR creation | authorized only after this plan is approved and every implementation/review gate is green |
| Merge | always requires a separate fresh user approval after live CI/reviews/threads pass |
| Delivery 2 head | create after PR 1 merge/local sync; proposed `codex/issue-1080-lettuce-synchronizers` |
| Delivery 3 head | create after PR 2 merge/local sync; proposed `codex/issue-1080-coordination-convergence` |

Stop this planning stage after:

1. this plan and its independent plan-review artifact reach `P0=0 / P1=0`;
2. both documents are committed with the Lore protocol;
3. the worktree is clean;
4. no production Kotlin, Gradle, README, or test file has changed.

---

## 2. Fixed public contract

### 2.1 Public value and failure types

**Create:**

- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LockIdentity.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LockConfig.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LockFailure.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LockResult.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LockObservation.kt`

The public model is fixed before Redis implementation:

```kotlin
class LockOwnerId private constructor(
    internal val value: String,
): Serializable {
    companion object {
        @JvmStatic fun random(): LockOwnerId
        @JvmStatic fun from(value: String): LockOwnerId
    }
}

class LockRequestId private constructor(
    internal val value: String,
): Serializable {
    companion object {
        @JvmStatic fun random(): LockRequestId
        @JvmStatic fun from(value: String): LockRequestId
    }
}

data class LockGeneration(
    val value: Long,
): Comparable<LockGeneration>, Serializable {
    override fun compareTo(other: LockGeneration): Int = value.compareTo(other.value)
    override fun toString(): String = "LockGeneration(<redacted>)"
}

enum class LockKind {
    DISTRIBUTED,
    FAIR,
    FENCED,
    READ,
    WRITE,
    SPIN,
    MULTI,
}

sealed interface LeasePolicy: Serializable {
    data class Fixed(val leaseTime: Duration): LeasePolicy
    data class Watchdog(
        val ttl: Duration = Duration.ofSeconds(30),
        val renewalInterval: Duration = ttl.dividedBy(3),
        val maxLifetime: Duration = Duration.ofHours(24),
    ): LeasePolicy
}

data class LockHandle(
    val objectFingerprint: String,
    val ownerId: LockOwnerId,
    val generation: LockGeneration,
    val requestId: LockRequestId,
    val leasePolicy: LeasePolicy,
    val kind: LockKind,
): Serializable

data class FencedLockHandle(
    val lock: LockHandle,
    val epoch: Long,
    val fencingToken: Long,
): Serializable {
    override fun toString(): String = "FencedLockHandle(lock=$lock, fence=<redacted>)"
}

data class ReadLockHandle(val lock: LockHandle): Serializable
data class WriteLockHandle(val lock: LockHandle): Serializable
data class MultiLockHandle(
    val lock: LockHandle,
    val constituentCount: Int,
): Serializable

enum class FairWaiterStatus {
    QUEUED,
    REMOVED,
    ADMITTED,
}

data class FairWaiterState(
    val status: FairWaiterStatus,
    val enqueueSequence: Long,
    val remainingWaitMillis: Long,
): Serializable {
    override fun toString(): String =
        "FairWaiterState(status=$status, sequence=<redacted>, deadline=<redacted>)"
}
```

Every identity and handle:

- validates on construction and deserialization;
- is immutable and `Serializable` with `serialVersionUID`;
- uses a minimum 128-bit CSPRNG-generated value for `random()`;
- accepts external UTF-8 values only within 1..256 bytes;
- redacts raw owner, request, object, key, generation, epoch, and token data from `toString`;
- never treats owner/request IDs as authentication credentials.

Every public identity, config, handle, failure, and nested result variant defines an explicit `serialVersionUID`.
Every deserializable value with invariants implements validated `readResolve` following `FencingLeaseValue.kt` and
`FencingLeaseResult.kt`; malformed serialized state becomes `InvalidObjectException` without echoing field values.

`LockFailure.kt` defines stable, sanitized classifications:

```kotlin
enum class LockBackendFailureKind {
    CONNECTION,
    TIMEOUT,
    COMMAND,
}

enum class LockIntegrityFailureKind {
    MALFORMED_REPLY,
    INVALID_STATE,
    INVALID_GENERATION,
    PARTIAL_MULTI_LOCK,
    COUNTER_REGRESSION,
}

enum class LockRecoveryAction {
    RECONCILE_REQUEST,
    INSPECT_HANDLE,
    RETRY_SAME_HANDLE,
    STOP_AND_REACQUIRE,
}

data class LockBackendFailure(
    val kind: LockBackendFailureKind,
    val recoveryAction: LockRecoveryAction,
): Serializable

data class LockIntegrityFailure(
    val kind: LockIntegrityFailureKind,
): Serializable
```

`LockObservation.kt` defines a non-serializable callback over an allowlisted, identity-free event:

```kotlin
enum class LockOperation {
    ACQUIRE,
    INSPECT,
    RECONCILE,
    RENEW,
    RELEASE,
    DOWNGRADE,
    CLEANUP,
    CLOSE,
}

enum class LockOutcome {
    SUCCEEDED,
    CONTENDED,
    TIMED_OUT,
    CANCELLED,
    AMBIGUOUS,
    OWNERSHIP_LOST,
    CAPACITY_REJECTED,
    BACKEND_FAILED,
    INTEGRITY_FAILED,
    CLOSED,
}

enum class LockLeasePolicyKind {
    FIXED,
    WATCHDOG,
    NONE,
}

enum class LockFailureMetricKind {
    CONNECTION,
    TIMEOUT,
    COMMAND,
    MALFORMED_REPLY,
    INVALID_STATE,
    INVALID_GENERATION,
    PARTIAL_MULTI_LOCK,
    COUNTER_REGRESSION,
}

data class LockEvent(
    val objectKind: LockKind,
    val operation: LockOperation,
    val outcome: LockOutcome,
    val failureKind: LockFailureMetricKind?,
    val leasePolicy: LockLeasePolicyKind,
)

data class LockDimensions(
    val objectKind: LockKind,
    val operation: LockOperation,
    val outcome: LockOutcome,
    val failureKind: LockFailureMetricKind?,
    val leasePolicy: LockLeasePolicyKind,
)

enum class LockCounterName {
    OPERATION_TOTAL,
    RECONCILE_TOTAL,
    STALE_CLEANUP_TOTAL,
    OWNERSHIP_LOSS_TOTAL,
    WATCHDOG_LATE_TOTAL,
    WATCHDOG_MISSED_TOTAL,
    NOSCRIPT_FALLBACK_TOTAL,
    INTEGRITY_FAILURE_TOTAL,
    CAPACITY_REJECTION_TOTAL,
    CLEANUP_PENDING_TOTAL,
}

enum class LockGaugeName {
    ACTIVE_WATCHDOGS,
    WATCHDOG_DUE_BACKLOG,
    SCHEDULED_TASKS,
    QUEUED_WAITERS,
    COORDINATION_OBJECTS,
    ACTIVE_REQUEST_HOLDS,
}

enum class LockHistogramName {
    REDIS_COMMAND_LATENCY_MILLIS,
    CALLER_WAIT_LATENCY_MILLIS,
    RETRY_COUNT,
    CLEANUP_BATCH_SIZE,
}

sealed interface LockObservation {
    data class Counter(
        val name: LockCounterName,
        val delta: Long,
        val dimensions: LockDimensions,
    ): LockObservation

    data class Gauge(
        val name: LockGaugeName,
        val value: Long,
        val dimensions: LockDimensions,
    ): LockObservation

    data class Histogram(
        val name: LockHistogramName,
        val value: Double,
        val dimensions: LockDimensions,
    ): LockObservation

    data class Event(val event: LockEvent): LockObservation
}

fun interface LockObservationSink {
    fun record(observation: LockObservation)
}
```

The default sink is no-op. Callers may adapt the sink to Micrometer/OpenTelemetry without this module taking a new
dependency. Counters are monotonic deltas, gauges are current runtime values, histograms use the unit fixed by the enum
name, and structured events carry the same bounded dimensions. Sink exceptions are isolated and cannot alter Redis
results or reschedule watchdog work. Every §7 risk signal has at least one counter/gauge/event source; no observation
contains object name, Redis key/hash tag, owner/request ID, waiter ID, generation, epoch, or fencing token.

Operation-specific sealed results are fixed as:

```kotlin
sealed interface LockAcquireResult<out H: Serializable>: Serializable {
    data class Acquired<H: Serializable>(val handle: H): LockAcquireResult<H>
    data class Reentered<H: Serializable>(
        val handle: H,
        val holdCount: Int,
    ): LockAcquireResult<H>
    data class Contended(val remainingTtlMillis: Long): LockAcquireResult<Nothing>
    data object TimedOut: LockAcquireResult<Nothing>
    data object CleanupPending: LockAcquireResult<Nothing>
    data object CapacityExceeded: LockAcquireResult<Nothing>
    data object Closed: LockAcquireResult<Nothing>
    data class BackendFailure(val failure: LockBackendFailure): LockAcquireResult<Nothing>
    data class IntegrityFailure(val failure: LockIntegrityFailure): LockAcquireResult<Nothing>
    data class Ambiguous(
        val ownerId: LockOwnerId,
        val requestId: LockRequestId,
        val recoveryAction: LockRecoveryAction,
    ): LockAcquireResult<Nothing>
}

sealed interface LockInspectResult<out H: Serializable>: Serializable {
    data class Owned<H: Serializable>(
        val handle: H,
        val holdCount: Int,
        val remainingTtlMillis: Long,
    ): LockInspectResult<H>
    data object Released: LockInspectResult<Nothing>
    data object Expired: LockInspectResult<Nothing>
    data object StaleGeneration: LockInspectResult<Nothing>
    data object OwnershipLost: LockInspectResult<Nothing>
    data object Closed: LockInspectResult<Nothing>
    data class BackendFailure(val failure: LockBackendFailure): LockInspectResult<Nothing>
    data class IntegrityFailure(val failure: LockIntegrityFailure): LockInspectResult<Nothing>
}

sealed interface LockReconcileResult<out H: Serializable>: Serializable {
    data class Owned<H: Serializable>(
        val handle: H,
        val holdCount: Int,
        val remainingTtlMillis: Long,
    ): LockReconcileResult<H>
    data class Queued(val waiter: FairWaiterState): LockReconcileResult<Nothing>
    data object Removed: LockReconcileResult<Nothing>
    data object Released: LockReconcileResult<Nothing>
    data object NotFound: LockReconcileResult<Nothing>
    data object StaleGeneration: LockReconcileResult<Nothing>
    data object Closed: LockReconcileResult<Nothing>
    data class BackendFailure(val failure: LockBackendFailure): LockReconcileResult<Nothing>
    data class IntegrityFailure(val failure: LockIntegrityFailure): LockReconcileResult<Nothing>
    data class Ambiguous(val recoveryAction: LockRecoveryAction): LockReconcileResult<Nothing>
}

sealed interface LockMutationResult<out H: Serializable>: Serializable {
    data class Renewed<H: Serializable>(
        val handle: H,
        val remainingTtlMillis: Long,
    ): LockMutationResult<H>
    data class Released(val remainingHoldCount: Int): LockMutationResult<Nothing>
    data object AlreadyReleased: LockMutationResult<Nothing>
    data object Expired: LockMutationResult<Nothing>
    data object StaleGeneration: LockMutationResult<Nothing>
    data object OwnershipLost: LockMutationResult<Nothing>
    data object Closed: LockMutationResult<Nothing>
    data class BackendFailure(val failure: LockBackendFailure): LockMutationResult<Nothing>
    data class IntegrityFailure(val failure: LockIntegrityFailure): LockMutationResult<Nothing>
    data class Ambiguous(val recoveryAction: LockRecoveryAction): LockMutationResult<Nothing>
}

sealed interface DowngradeResult: Serializable {
    data class Downgraded(val handle: ReadLockHandle): DowngradeResult
    data object Expired: DowngradeResult
    data object StaleGeneration: DowngradeResult
    data object OwnershipLost: DowngradeResult
    data object Closed: DowngradeResult
    data class BackendFailure(val failure: LockBackendFailure): DowngradeResult
    data class IntegrityFailure(val failure: LockIntegrityFailure): DowngradeResult
    data class Ambiguous(val recoveryAction: LockRecoveryAction): DowngradeResult
}
```

The covariant handle parameter keeps one result taxonomy while preserving Java/Kotlin handle safety:

- distributed/fair/spin methods return `LockAcquireResult<LockHandle>`;
- fenced methods return `LockAcquireResult<FencedLockHandle>`;
- read/write views return `LockAcquireResult<ReadLockHandle>` or `LockAcquireResult<WriteLockHandle>`;
- multi-lock methods return `LockAcquireResult<MultiLockHandle>`.

`LockInspectResult`, `LockReconcileResult`, and `LockMutationResult` use the same handle parameter. Negative singleton
variants implement the `Nothing` specialization and are usable through covariance. No unchecked handle cast is used.

`Closed` is the stable lifecycle result. After pre-dispatch argument validation, a post-close blocking call returns it,
`*Async` returns an already completed future containing it, and a suspend call returns it unless the coroutine is
already cancelled. Closing an object terminates its pending waits with `Closed`; an independently cancelled caller
retains the original `CancellationException`.

### 2.2 Public configuration and hard bounds

`LockConfig.kt` defines:

```kotlin
data class LockConfig(
    val namespace: String = "bt4k:coord:v1",
    val hashTag: String? = null,
    val maxReentrantHolds: Int = 10_000,
): Serializable

data class FairLockConfig(
    val lock: LockConfig = LockConfig(),
    val cleanupBatchSize: Int = 64,
    val maxQueueSize: Int = 10_000,
): Serializable

data class FencedLockConfig(
    val lock: LockConfig = LockConfig(),
    val epoch: Long,
): Serializable

data class ReadWriteLockConfig(
    val lock: LockConfig = LockConfig(),
    val cleanupBatchSize: Int = 64,
    val maxQueueSize: Int = 10_000,
): Serializable

data class SpinLockConfig(
    val lock: LockConfig = LockConfig(),
    val initialDelay: Duration = Duration.ofMillis(10),
    val multiplier: Double = 2.0,
    val maxDelay: Duration = Duration.ofSeconds(1),
    val jitterRatio: Double = 0.25,
    val maxAttemptsPerSecond: Int = 100,
): Serializable

data class MultiLockConfig(
    val lock: LockConfig = LockConfig(),
    val maxKeys: Int = 32,
): Serializable
```

Validation is synchronous for blocking and `*Async` methods and before coroutine suspension:

| Input | Contract |
|---|---|
| namespace | colon-separated `[A-Za-z0-9._-]{1,32}` components; total 1..128 bytes; at most 8 components |
| name/hash-tag component | `[A-Za-z0-9._-]{1,128}`; braces, colon, whitespace, and controls rejected |
| derived encoded key | at most 512 bytes |
| owner/request ID | UTF-8 1..256 bytes |
| `tryAcquire` wait | exactly zero |
| `acquire` wait | positive and at most 24 hours |
| fixed lease | 100 ms..24 hours |
| watchdog TTL | 3 seconds..24 hours |
| watchdog interval | 100 ms..TTL/3; default TTL/3 |
| watchdog max lifetime | positive, default 24 hours, at most 7 days |
| active request-bound reentrant holds | object maximum 10,000 |
| fair/read-write cleanup batch | 1..256 |
| fair/read-write queue | 1..10,000 |
| spin initial/max delay | positive; initial at most max |
| spin multiplier | finite and at least 1.0 |
| spin jitter | finite 0.0..0.25 |
| spin attempt rate | 1..100 per second |
| fenced epoch/sequence | positive and at most 9,007,199,254,740,991 for exact Redis Lua integer representation |
| multi-lock config/input | `maxKeys` fixed to 1..32; input 1..`maxKeys`, distinct, same codec-wire Redis slot |
| script response | at most 16 items; bulk item at most 256 bytes |

Positive nanosecond durations round up to one Redis millisecond. Overflow fails before dispatch.

### 2.3 Factories and operation symmetry

Every blocking and suspend class has standalone and Cluster `@JvmStatic create` overloads. Java callers receive an
explicit no-config overload where a safe default exists; they never depend on a Kotlin default argument.

| Public type pair | Resource argument | Config | No-config Java overload | Specialized operations |
|---|---|---|---|---|
| `LettuceDistributedLock` / `LettuceSuspendDistributedLock` | `name: String` | `LockConfig` | yes | common exclusive surface with `LockHandle` |
| `LettuceFairLock` / `LettuceSuspendFairLock` | `name: String` | `FairLockConfig` | yes | common surface + fair queued reconcile |
| `LettuceFencedLock` / `LettuceSuspendFencedLock` | `name: String` | required `FencedLockConfig` | no; epoch is mandatory | common surface with `FencedLockHandle` + bootstrap |
| `LettuceReadWriteLock` / `LettuceSuspendReadWriteLock` | `name: String` | `ReadWriteLockConfig` | yes | read/write views + downgrade |
| `LettuceSpinLock` / `LettuceSuspendSpinLock` | `name: String` | `SpinLockConfig` | yes | common exclusive surface with bounded retry |
| `LettuceMultiLock` / `LettuceSuspendMultiLock` | `names: Collection<String>` | `MultiLockConfig` | yes | common surface with `MultiLockHandle` |

For each row, the overload set is:

```kotlin
@JvmStatic fun create(connection: StandaloneConnection, resource: Resource): Type
@JvmStatic fun create(connection: StandaloneConnection, resource: Resource, config: Config): Type
@JvmStatic fun create(connection: ClusterConnection, resource: Resource): Type
@JvmStatic fun create(connection: ClusterConnection, resource: Resource, config: Config): Type
@JvmStatic fun create(
    connection: StandaloneConnection,
    resource: Resource,
    config: Config,
    scheduler: ScheduledExecutorService,
    observationSink: LockObservationSink,
): Type
@JvmStatic fun create(
    connection: ClusterConnection,
    resource: Resource,
    config: Config,
    scheduler: ScheduledExecutorService,
    observationSink: LockObservationSink,
): Type
```

`StandaloneConnection` means `StatefulRedisConnection<String, String>`, `ClusterConnection` means
`StatefulRedisClusterConnection<String, String>`, `Resource` is `String` except for MultiLock's
`Collection<String>`, and `Config`/`Type` are taken from the row. `LettuceFencedLock` omits both no-config overloads.
The suspend type has the identical overload set and shares the same underlying internal runtime registry.

Concrete distributed-lock example:

```kotlin
companion object {
    @JvmStatic
    fun create(
        connection: StatefulRedisConnection<String, String>,
        name: String,
    ): LettuceDistributedLock =
        create(connection, name, LockConfig())

    @JvmStatic
    fun create(
        connection: StatefulRedisConnection<String, String>,
        name: String,
        config: LockConfig,
    ): LettuceDistributedLock

    @JvmStatic
    fun create(
        connection: StatefulRedisClusterConnection<String, String>,
        name: String,
    ): LettuceDistributedLock =
        create(connection, name, LockConfig())

    @JvmStatic
    fun create(
        connection: StatefulRedisClusterConnection<String, String>,
        name: String,
        config: LockConfig,
    ): LettuceDistributedLock
}
```

Injected `ScheduledExecutorService` and `LockObservationSink` objects are not stored in serializable config. The
overload documents that the object never shuts down the injected scheduler.

The following is a signature template, not an extra public interface. Each concrete class substitutes the handle type
from the specialization table:

| Concrete surface | `H` |
|---|---|
| distributed, fair, spin | `LockHandle` |
| fenced | `FencedLockHandle` |
| read view | `ReadLockHandle` |
| write view | `WriteLockHandle` |
| multi-lock | `MultiLockHandle` |

```kotlin
fun tryAcquire(
    ownerId: LockOwnerId,
    requestId: LockRequestId,
    leasePolicy: LeasePolicy,
): LockAcquireResult<H>

fun tryAcquireAsync(
    ownerId: LockOwnerId,
    requestId: LockRequestId,
    leasePolicy: LeasePolicy,
): CompletableFuture<LockAcquireResult<H>>

fun acquire(
    ownerId: LockOwnerId,
    requestId: LockRequestId,
    waitTime: Duration,
    leasePolicy: LeasePolicy,
): LockAcquireResult<H>

fun acquireAsync(
    ownerId: LockOwnerId,
    requestId: LockRequestId,
    waitTime: Duration,
    leasePolicy: LeasePolicy,
): CompletableFuture<LockAcquireResult<H>>

fun inspect(handle: H): LockInspectResult<H>
fun inspectAsync(handle: H): CompletableFuture<LockInspectResult<H>>
fun reconcile(ownerId: LockOwnerId, requestId: LockRequestId): LockReconcileResult<H>
fun reconcileAsync(
    ownerId: LockOwnerId,
    requestId: LockRequestId,
): CompletableFuture<LockReconcileResult<H>>
fun renew(handle: H, extension: Duration): LockMutationResult<H>
fun renewAsync(handle: H, extension: Duration): CompletableFuture<LockMutationResult<H>>
fun release(handle: H): LockMutationResult<H>
fun releaseAsync(handle: H): CompletableFuture<LockMutationResult<H>>
fun close()
```

The corresponding `LettuceSuspend*` class exposes the same operations as `suspend fun`, with the same public
identity/config/handle/result types, and an idempotent non-suspending `fun close()`. It delegates to the same internal
scripts and decoders, not to a blocking method. Suspend close cancels only local waits/watchdogs and never releases an
acquired Redis lock implicitly.

`LockResult.kt` also defines `FencedBootstrapResult` with `Initialized`, `AlreadyInitialized`, `Closed`,
`BackendFailure`, `IntegrityFailure`, and `Ambiguous` variants. Exact specialized signatures are:

```kotlin
fun LettuceFencedLock.bootstrapFencing(): FencedBootstrapResult
fun LettuceFencedLock.bootstrapFencingAsync(): CompletableFuture<FencedBootstrapResult>
suspend fun LettuceSuspendFencedLock.bootstrapFencing(): FencedBootstrapResult

fun LettuceReadWriteLock.readLock(): LettuceReadWriteLock.ReadLockView
fun LettuceReadWriteLock.writeLock(): LettuceReadWriteLock.WriteLockView
fun LettuceReadWriteLock.downgrade(handle: WriteLockHandle): DowngradeResult
fun LettuceReadWriteLock.downgradeAsync(handle: WriteLockHandle): CompletableFuture<DowngradeResult>

fun LettuceSuspendReadWriteLock.readLock(): LettuceSuspendReadWriteLock.ReadLockView
fun LettuceSuspendReadWriteLock.writeLock(): LettuceSuspendReadWriteLock.WriteLockView
suspend fun LettuceSuspendReadWriteLock.downgrade(handle: WriteLockHandle): DowngradeResult
```

Read/write view methods use the generic template with their exact handle specialization. No upgrade method exists.

---

## 3. Internal architecture and write-scope map

### 3.1 Neutral internal substrate

**Create:**

- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/coordination/internal/CoordinationKeyspace.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/coordination/internal/CoordinationDeadline.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/coordination/internal/CoordinationScriptExecutor.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/coordination/internal/CoordinationRuntime.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/coordination/internal/CoordinationProtocol.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/coordination/internal/CoordinationObservation.kt`

Responsibilities are non-overlapping:

| File | Sole responsibility |
|---|---|
| `CoordinationKeyspace.kt` | validated versioned names, derived keys, codec wire-byte slot proof, redacted fingerprint |
| `CoordinationDeadline.kt` | monotonic deadline, duration round-up/overflow bounds, injectable ticker |
| `CoordinationScriptExecutor.kt` | sync/async/suspend `RedisScriptRunner` bridge, NOSCRIPT-only fallback, cancellation propagation |
| `CoordinationRuntime.kt` | connection-identity runtime registry, one shared scheduler, object/task registration, hard caps, idempotent close |
| `CoordinationProtocol.kt` | bounded raw tagged-response validation and sanitized internal backend/integrity classifications |
| `CoordinationObservation.kt` | low-cardinality event sink and allowlisted dimension validation |

`CoordinationRuntime` uses a synchronized weak connection-identity registry. A runtime:

- owns one daemon `ScheduledThreadPoolExecutor` only when no scheduler was injected;
- never closes an injected scheduler;
- reference-counts registered lock objects;
- caps active watchdog and scheduled task registrations at 10,000;
- dispatches at most 256 due watchdog renewals per tick;
- while a due-renewal backlog exists, schedules the next drain tick within 25 ms instead of waiting for the normal
  renewal interval;
- enforces `ceil(activeWatchdogs / 256) * 25 ms <= ttl - renewalInterval - 1 second` at registration. With the hard
  bounds of 10,000 active watchdogs, 3-second minimum TTL, and interval at most TTL/3, the worst dispatch drain is
  1 second and retains at least a 1-second Redis-completion margin. A future cap/cadence change that violates the
  formula must reject registration rather than advertise an unserviceable watchdog;
- records due, dispatched, late, missed, and maximum-backlog values; a renewal not completed before its Redis TTL
  authority expires transitions to ownership loss and is never silently rescheduled;
- rejects new work after object/runtime close;
- cancels only the closing object's registrations;
- closes its owned scheduler when the last object unregisters or the connection is observed closed;
- verifies task ID plus object fingerprint plus generation before a late completion reschedules work.

No public signature references these internal types.

### 3.2 Lock internal protocol

**Create:**

- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/LockProtocol.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/LockCommandExecutor.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/LockWaitSupport.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/DistributedLockScript.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/FairLockScript.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/FencedLockScript.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/ReadWriteLockScript.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/MultiLockScript.kt`

`LockProtocol.kt` owns only internal request/reply DTOs, operation enums, and public-result mapping.
`LockCommandExecutor.kt` owns command construction, response decode, and expected failure classification.
`LockWaitSupport.kt` owns blocking park, async scheduled retry, suspend delay, absolute deadline, cancellation, and
same-identity reconciliation. Script files own Lua and exact tagged response decoding for one algorithm each.

Spin lock reuses `DistributedLockScript` and changes only the client-side wait policy. It does not get a separate Lua
state machine.

### 3.3 Redis state schemas

All keys use:

```text
bt4k:coord:v1:{<hash-tag>}:lock:<name>:state
bt4k:coord:v1:{<hash-tag>}:lock:<name>:generation
bt4k:coord:v1:{<hash-tag>}:lock:<name>:holds
bt4k:coord:v1:{<hash-tag>}:lock:<name>:terminal
bt4k:coord:v1:{<hash-tag>}:lock:<name>:queue
bt4k:coord:v1:{<hash-tag>}:lock:<name>:waiters
bt4k:coord:v1:{<hash-tag>}:lock:<name>:fence
bt4k:coord:v1:{<hash-tag>}:lock:<name>:readers
bt4k:coord:v1:{<hash-tag>}:lock:<name>:phase
bt4k:coord:v1:{<hash-tag>}:multilock:<group-fingerprint>:generation
```

State values are bounded fields in Redis hashes/sorted sets. Lua receives validated KEYS/ARGV only; caller strings
are never interpolated into script source.

Exclusive state invariant:

```text
owner=<LockOwnerId>
generation=<monotonic Redis counter>
holdCount=<positive integer>
kind=<stable numeric tag>
leaseDeadline=<Redis TTL authority>
holds=<bounded request-ID set; one active entry per successful logical acquire>
```

Acquisition semantics:

1. missing state allocates `INCR generation`, writes owner/generation/holdCount=1 plus the request-bound hold, applies TTL, returns
   `ACQUIRED`;
2. matching owner with a new request ID inserts one hold and increments hold count without changing generation;
3. replay of an existing acquisition request returns its current handle/hold count without incrementing;
4. `release(handle)` removes exactly the hold identified by `handle.requestId`; a retry cannot decrement twice;
5. non-final duplicate release returns `AlreadyReleased`; final release writes a bounded terminal marker before deleting
   state/holds so the same handle can distinguish `AlreadyReleased` from expiry;
6. missing state without the matching release marker is `Expired`; a newer counter is `StaleGeneration`;
7. non-matching owner returns `CONTENDED` with positive PTTL;
8. invalid/persistent/malformed state returns an integrity tag without mutation;
9. final release deletes state/holds but never deletes or resets generation/fencing counters.

The terminal marker contains only the last released generation/request identity and expires after the configured
maximum watchdog lifetime, capped at seven days. A fresh generation overwrites it. The active hold set is capped by
`LockConfig.maxReentrantHolds`; cap exhaustion returns `CapacityExceeded` without mutation.

`renew(handle, extension)` sets the remaining Redis TTL to the validated `extension`; it never adds the extension to
the prior TTL. Retrying the same handle/extension after ambiguous completion is therefore a replacement operation, not
a cumulative double extension. Watchdog renewals use the same replacement rule. Atomic downgrade consumes the
request-bound write hold once and returns the same request-bound read hold on replay.

Fair state adds enqueue sequence, a collision-free length-prefixed waiter member derived from the `(ownerId,
requestId)` tuple within the object-scoped queue, generation, and Redis-side deadline. The tuple is compared in full
for removal/reconciliation; a caller-supplied request ID is never assumed globally unique across owners.
Read-write state adds reader-owner hold counts plus one writer state and a phase boundary.
Multi-lock derives a stable group fingerprint from the ordered validated encoded-key set. Its group-generation key is
the sole generation authority; every constituent state stores that group fingerprint and generation. The script
applies the same request-bound hold transition to all constituents atomically.

### 3.4 Script response and failure channel

Every script returns at most 16 items:

```text
{TAG, generation, holdCount, pttlMillis, auxiliary1, auxiliary2}
```

The decoder validates outer type, item count, tag, arity, numeric range, and bulk length before constructing a public
result. Unknown or oversized response becomes `IntegrityFailure(MALFORMED_REPLY)`.

Pre-dispatch invalid inputs throw `IllegalArgumentException` synchronously.
Post-dispatch contention, timeout, stale state, capacity, backend, integrity, and ambiguity return sealed results.
Exceptional async/suspend completion is limited to:

- the caller's original `CancellationException`;
- programmer errors;
- JVM fatal errors or invariants outside the decoder contract.

No suspend path wraps a `CancellationException` in a result or catches it through `runCatching`.

---

## 4. Dependency order and execution ownership

Tasks are sequential when they depend on Redis state or public types. Independent review/document lanes may run in
parallel, but Testcontainers tasks remain serialized.

| Task | Depends on | Primary write scope |
|---|---|---|
| 1 | approved spec/plan | public value/config/failure/result files and unit tests |
| 2 | Task 1 | neutral coordination internal files and pure unit tests |
| 3 | Tasks 1-2 | distributed-lock script/facades/contracts |
| 4 | Tasks 1-3 | watchdog/runtime/cancellation/lifecycle tests |
| 5 | Tasks 1-4 | fair-lock script/facades/contracts |
| 6 | Tasks 1-4 | fenced-lock script/facades/contracts |
| 7 | Tasks 1-5 | read-write script/views/contracts |
| 8 | Tasks 1-4 | spin facades/backoff contracts |
| 9 | Tasks 1-4 | multi-lock script/facades/contracts |
| 10 | Tasks 3-9 | Cluster, ambiguity, concurrency, protocol, command-budget evidence |
| 11 | Tasks 3-10 | Gradle characterization tasks and report |
| 12 | Tasks 1-11 | English/Korean docs, KDoc examples, compatibility matrix |
| 13 | Tasks 1-12 | complete module verification and independent implementation review |
| 14 | Task 13 | commit/push/PR metadata and merge-ready report; no merge |

Implementation workers are not alone in the codebase. Each worker must preserve concurrent edits, remain inside its
assigned file set, and report a shared-file conflict instead of reverting another lane.

---

## 5. Task-by-task TDD plan

## Task 1: Lock the public model and validation contract

**Files:**

- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LockIdentity.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LockConfig.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LockFailure.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LockResult.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LockObservation.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockIdentityTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockConfigTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockResultTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockObservationTest.kt`

- [ ] Write failing serialization/validation/redaction tests for every public value and result variant.
- [ ] Prove generated owner/request values contain at least 128 bits of CSPRNG entropy through decoded byte length and
      collision-free repeated samples; do not claim statistical randomness from a unit test.
- [ ] Prove external IDs use UTF-8 byte bounds, not Kotlin character count.
- [ ] Prove nanosecond-positive durations round up, invalid/overflow durations fail, config hard caps fail closed, and
      `toString` never includes raw identity/token values.
- [ ] Prove the default colon-separated namespace is accepted while braces, blank segments, whitespace, controls,
      oversized components, and more than eight segments are rejected.
- [ ] Prove `FencedLockConfig.epoch` rejects non-positive and non-exact Lua integers on construction/deserialization.
- [ ] Prove `MultiLockConfig.maxKeys` accepts only 1..32 and rejects a larger input collection before copying, codec
      slot hashing, or Redis dispatch.
- [ ] Prove observer events contain only allowlisted enum values, observer failure cannot change an operation result,
      and no public observation payload accepts caller-controlled labels.
- [ ] Implement the exact public model in §2 with English KDoc and `readResolve` validation.
- [ ] Add compile tests that Java-visible companions expose `@JvmStatic` factories for owner/request generation.

Run RED:

```bash
./gradlew :bluetape4k-lettuce:test \
  --tests '*LockIdentityTest' \
  --tests '*LockConfigTest' \
  --tests '*LockResultTest' \
  --tests '*LockObservationTest'
```

Expected RED: tests fail to compile because the public model does not exist.

Run GREEN:

```bash
./gradlew :bluetape4k-lettuce:test \
  --tests '*LockIdentityTest' \
  --tests '*LockConfigTest' \
  --tests '*LockResultTest' \
  --tests '*LockObservationTest'
./gradlew :bluetape4k-lettuce:compileKotlin
```

Expected GREEN: all focused tests pass and the module compiles.

Commit:

```text
Fix lock ownership semantics before Redis state exists

Constraint: Blocking, async, and suspend variants must share one serializable public contract
Rejected: Thread identity and Boolean results | They cannot express lease loss or reconciliation
Confidence: high
Scope-risk: moderate
Directive: Preserve identity redaction and operation-specific result channels
Tested: Lock identity, config, result, serialization, and compile tests
Not-tested: Redis scripts and lifecycle behavior
```

## Task 2: Build the neutral coordination substrate with pure tests

**Files:**

- Create: the six `coordination/internal` files listed in §3.1
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/coordination/internal/CoordinationKeyspaceTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/coordination/internal/CoordinationDeadlineTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/coordination/internal/CoordinationProtocolTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/coordination/internal/CoordinationRuntimeTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/coordination/internal/CoordinationObservationTest.kt`

- [ ] Write failing codec-wire key-slot tests using `StringCodec` and a custom key codec whose encoded bytes differ
      from source text.
- [ ] Write failing bounded response tests for unknown tag, wrong arity, negative/overflow number, more than 16 items,
      and more than 256 bytes.
- [ ] Use `runTest` with an injected monotonic ticker for deadline and scheduled-task state tests; no real delay.
- [ ] Prove runtime registration cap, per-tick dispatch cap, owned versus injected scheduler close, idempotent object
      close, last-object shutdown, connection-closed termination, late completion generation rejection, 25-ms
      due-backlog drain cadence, and the registration service-capacity formula.
- [ ] Prove the observation layer rejects raw/high-cardinality labels and accepts only `object_kind`, `operation`,
      `outcome`, `failure_kind`, and `lease_policy`.
- [ ] Prove counter/gauge/histogram/event emission for operation outcomes, reconciliation, stale cleanup,
      `CleanupPending`, ownership loss, watchdog late/missed renewal, NOSCRIPT fallback, integrity/capacity rejection,
      active watchdogs/due backlog/tasks/waiters/objects/request holds, Redis/wait latency, retries, and cleanup batch
      size.
- [ ] Map every risk-table signal to a concrete observation name and alert predicate: non-decreasing queued waiters,
      nonzero post-close tasks/watchdogs, ownership-loss increment, `CleanupPending` persistence, capacity rejection,
      integrity/NOSCRIPT spikes, and missing progress despite active waiters.
- [ ] Prove a throwing sink cannot change a returned result, exceptional completion, cancellation identity, task
      registration, or Redis mutation count.
- [ ] Implement the neutral substrate. Reuse `RedisScriptRunner`; do not duplicate EVALSHA/NOSCRIPT logic.
- [ ] Verify `coordination.internal` imports no public `lock`, `lease`, `semaphore`, or future `synchronizer` type.

Run RED then GREEN:

```bash
./gradlew :bluetape4k-lettuce:test \
  --tests '*CoordinationKeyspaceTest' \
  --tests '*CoordinationDeadlineTest' \
  --tests '*CoordinationProtocolTest' \
  --tests '*CoordinationRuntimeTest' \
  --tests '*CoordinationObservationTest'
```

Expected RED: missing internal substrate.
Expected GREEN after implementation: all pure tests pass without starting Redis.

Boundary check:

```bash
if rg -n '^import io\\.bluetape4k\\.redis\\.lettuce\\.(lock|lease|semaphore|synchronizer)' \
  infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/coordination/internal; then
  exit 1
fi
```

Expected: no matches.

Commit:

```text
Share coordination mechanics without creating a public cross-family API

Constraint: Lock and synchronizer public models remain independent until convergence
Rejected: Public coordination facade | It would freeze the first delivery's vocabulary
Confidence: high
Scope-risk: moderate
Directive: Keep neutral internals free of public family dependencies
Tested: Keyspace, deadline, protocol, runtime, lifecycle, and observation unit tests
Not-tested: Redis lock state transitions
```

## Task 3: Implement the reentrant distributed lock contract

**Files:**

- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/LockProtocol.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/LockCommandExecutor.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/LockWaitSupport.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/DistributedLockScript.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceDistributedLock.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceSuspendDistributedLock.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockContract.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/DistributedLockScriptTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceDistributedLockTest.kt`

- [ ] Define one `LockContract` adapter for blocking, async, and suspend surfaces.
- [ ] Write RED cases for immediate acquire, contention, bounded timeout, same-owner reentry, same generation,
      hold-count increment, non-final/final release, renew, inspect, request reconciliation, stale generation,
      expiry/takeover, malformed Redis state, sync validation before async dispatch, and standalone/Cluster factories.
- [ ] Prove same-request acquire replay does not increment, new-request reentry increments once, ambiguous release
      retry with hold count greater than one cannot decrement twice, final release replay returns `AlreadyReleased`,
      missing terminal evidence returns `Expired`, and renew retry replaces rather than cumulatively adds TTL.
- [ ] Prove caller-order safety with distinct outer/inner handles: releasing the inner then outer reaches zero;
      releasing outer twice does not consume inner; each handle is single-use for one request-bound hold.
- [ ] Use deterministic barriers for concurrent one-winner proof.
- [ ] Implement one Lua command per warm operation and `RedisScriptRunner` NOSCRIPT-only fallback.
- [ ] Store generation in a monotonic counter that release/expiry cleanup never deletes.
- [ ] Keep existing `LettuceLock` and `LettuceSuspendLock` untouched.
- [ ] Implement blocking wait with `LockSupport.parkNanos`, async wait with the shared scheduler, and suspend wait with
      `delay` plus `ensureActive`.
- [ ] Preserve the same owner/request identity across every retry and reconciliation.

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-lettuce:test \
  --tests '*DistributedLockScriptTest' \
  --tests '*LettuceDistributedLockTest'
```

Expected RED: public facades/scripts are absent.
Expected GREEN: the shared contract passes for blocking, async, and suspend adapters.

Commit:

```text
Make logical ownership authoritative for the base Lettuce lock

Constraint: Redis mutation must remain one atomic script with monotonic generations
Rejected: Adapting the legacy non-reentrant token lock | It cannot satisfy hold-count semantics atomically
Confidence: high
Scope-risk: broad
Directive: Reuse this state contract for non-fair exclusive lock variants
Tested: Distributed lock script and blocking, async, suspend contracts
Not-tested: Watchdog and specialized lock algorithms
```

## Task 4: Add watchdog, cancellation, reconciliation, and lifecycle safety

**Files:**

- Modify: `CoordinationRuntime.kt`, `LockCommandExecutor.kt`, `LockWaitSupport.kt`
- Modify: `LettuceDistributedLock.kt`, `LettuceSuspendDistributedLock.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockWatchdogTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockCancellationTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockLifecycleTest.kt`

- [ ] Write RED cases for fixed lease without renewal, watchdog renewal cadence/jitter, maximum lifetime, 10,000
      registration cap, 256-per-tick cap, 25-ms backlog drain, the worst-case 10,000-watchdog service formula,
      rejection of an unserviceable future cap/cadence configuration, zero late/missed renewals inside the accepted
      envelope, ownership loss after a deliberately missed deadline, renew backend failure, and no automatic caller
      work interruption.
- [ ] Write RED cases for cancellation before dispatch, during wait, after Redis dispatch, async future cancellation,
      and suspend cancellation. Assert original cancellation identity and same-request reconciliation metadata.
- [ ] Write RED cases for object close, runtime last-object close, caller-owned scheduler survival, owned scheduler
      termination, pending future/coroutine termination, connection close, and late completion that must not reschedule.
- [ ] Prove post-close blocking/async/suspend calls return `Closed` after input validation, pending waits terminate with
      `Closed`, and an independently cancelled coroutine/future retains its original cancellation identity.
- [ ] Use fake script execution and `runTest` virtual time for scheduler state. Use Testcontainers only for
      post-dispatch ambiguity and authoritative Redis inspection.
- [ ] Implement watchdog registration only after successful acquisition and cancel it after final release/loss/close.
- [ ] Never use `runCatching` around suspend calls; rethrow `CancellationException`.

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-lettuce:test \
  --tests '*LockWatchdogTest' \
  --tests '*LockCancellationTest' \
  --tests '*LockLifecycleTest' \
  --tests '*LettuceDistributedLockTest'
```

Expected: all lifecycle registries reach zero after each test; no real-delay scheduler test is used where virtual time
can prove the transition.

Commit:

```text
Bound lock renewal and cancellation ambiguity by runtime lifecycle

Constraint: Cancellation cannot prove a dispatched Redis mutation did not execute
Rejected: Handle-owned executors and blind retries | They leak resources and duplicate ambiguous work
Confidence: high
Scope-risk: broad
Directive: Preserve same-identity reconciliation and late-completion guards
Tested: Watchdog, cancellation, lifecycle, and distributed-lock regression tests
Not-tested: Fair, read-write, fenced, spin, and multi-lock behavior
```

## Task 5: Implement bounded FIFO fair lock

**Files:**

- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/FairLockScript.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceFairLock.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceSuspendFairLock.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/FairLockScriptTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceFairLockTest.kt`

- [ ] Write RED cases for Redis enqueue sequence FIFO, reentrancy without a second queue entry, timeout/cancellation
      removal, stale head without process cooperation, cleanup batch 64/default and 256/max, `CleanupPending` no-bypass,
      queue capacity 10,000, unique waiter identity, compare-delete reconciliation, and lost notification recovery.
- [ ] Prove two different owners using the same caller-supplied request ID retain separate queue members and cannot
      remove/reconcile each other. Also prove same-owner/same-request replay is one waiter while same-owner/new-request
      reentry follows the existing owner without a duplicate queue admission.
- [ ] Prove fairness is Redis enqueue order by recording returned sequence; do not assert caller wall-clock order.
- [ ] Implement enqueue and attempt in one script. Stale cleanup visits at most the configured batch.
- [ ] When cleanup budget is exhausted, return `CleanupPending`; do not admit any later waiter.
- [ ] Keep Redis queue state authoritative. Polling must recover if an optional wakeup hint is lost.
- [ ] Use same request identity for queue removal and reconcile.

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-lettuce:test \
  --tests '*FairLockScriptTest' \
  --tests '*LettuceFairLockTest'
```

Expected: FIFO/state assertions pass without timing-only sleeps.

Commit:

```text
Preserve Redis-observed FIFO without unbounded stale-waiter work

Constraint: Dead waiters must not block progress or permit queue bypass
Rejected: Client-only queue and unbounded cleanup | Neither survives process loss safely
Confidence: high
Scope-risk: broad
Directive: Keep CleanupPending fail-closed and compare waiter generation on removal
Tested: Fair script, queue cleanup, cancellation, and API parity tests
Not-tested: Read-write phase fairness
```

## Task 6: Implement fenced lock on the proven fencing invariant

**Files:**

- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/FencedLockScript.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceFencedLock.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceSuspendFencedLock.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/FencedLockScriptTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceFencedLockTest.kt`
- Modify only if extraction proves smaller and behavior-neutral:
  `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLeaseSupport.kt`
- Modify only with that extraction:
  existing fencing lease focused tests

- [ ] Write RED cases for explicit bootstrap, missing counter, fresh acquisition token increment, strict monotonic token,
      reentrant token stability, epoch isolation, overflow, stale token/generation release, takeover, malformed counter,
      and downstream strict-greater example.
- [ ] Reuse the existing fencing lease counter/epoch invariant and decoder fixture. If code extraction would expand the
      old primitive's risk, keep its implementation unchanged and reuse the invariant/tests rather than forcing a
      shared abstraction.
- [ ] Allocate a fencing token only for a fresh generation. Reentry returns the original epoch/token.
- [ ] Never claim Redis fencing alone gives exactly-once or stops stale work.
- [ ] Run every existing fencing lease test if its support file changes.

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-lettuce:test \
  --tests '*FencedLock*' \
  --tests '*FencingLease*'
```

Expected: new fenced lock and all existing fencing lease regressions pass.

Commit:

```text
Expose fencing as a reentrant lock without weakening token ordering

Constraint: Only a fresh generation may allocate a new fencing token
Rejected: Token-per-reentry | It breaks stable ownership and downstream ordering
Confidence: high
Scope-risk: broad
Directive: Keep downstream strict-greater guidance and epoch authority explicit
Tested: Fenced lock and existing fencing lease suites
Not-tested: Read-write and multi-lock algorithms
```

## Task 7: Implement phase-fair read-write lock

**Files:**

- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/ReadWriteLockScript.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceReadWriteLock.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceSuspendReadWriteLock.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/ReadWriteLockScriptTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceReadWriteLockTest.kt`

- [ ] Write RED cases for concurrent compatible readers, exclusive writer, same-owner reentry per mode, blocked writer
      boundary, bounded reader phase, one-writer admission, both-side starvation prevention, expired holder cleanup,
      unsupported upgrade compile/API proof, atomic downgrade, stale-generation downgrade, and lifecycle parity.
- [ ] Prove an ambiguous downgrade replay returns the same request-bound read hold without consuming a second write
      hold or incrementing read ownership twice.
- [ ] Use deterministic phase barriers and operation sequences rather than sleep-based ordering.
- [ ] Implement reader-owner hold counts and writer hold count in one slot.
- [ ] Admit queued readers only up to the existing writer boundary. After that reader phase drains, admit one writer.
- [ ] Perform write-to-read downgrade in one Lua script. Do not expose or implement read-to-write upgrade.
- [ ] Apply the fair cleanup cap and `CleanupPending` no-bypass rule.

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-lettuce:test \
  --tests '*ReadWriteLockScriptTest' \
  --tests '*LettuceReadWriteLockTest'
```

Expected: read/write compatibility and phase progress pass under blocking, async, and suspend views.

Commit:

```text
Bound reader and writer progress with one Redis phase boundary

Constraint: Neither readers nor writers may starve under sustained arrivals
Rejected: Reader preference and read-to-write upgrade | They permit starvation or deadlock
Confidence: high
Scope-risk: broad
Directive: Keep downgrade atomic and preserve the queued writer boundary
Tested: Read-write script, phase fairness, downgrade, and API parity tests
Not-tested: Spin and multi-lock behavior
```

## Task 8: Implement bounded spin lock as a wait policy

**Files:**

- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceSpinLock.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceSuspendSpinLock.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceSpinLockTest.kt`
- Modify: `LockWaitSupport.kt`

- [ ] Write RED pure tests using injected jitter source/ticker for delay sequence 10 ms, 20 ms, 40 ms up to 1 second,
      multiplier validation, jitter 0..25%, jitter disabled at zero, deadline clipping, max 100 attempts/second, and
      cancellation before the next Redis attempt.
- [ ] Write Redis contract cases for exclusive reentrant behavior and timeout.
- [ ] Reuse `DistributedLockScript`; do not create a spin-specific Redis schema.
- [ ] Use scheduled Redis retries, never CPU busy looping, pub/sub, or a fair queue.
- [ ] Apply the same request identity and result/failure contract as distributed lock.

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-lettuce:test --tests '*LettuceSpinLockTest'
```

Expected: virtual-time backoff tests and Redis semantic tests pass.

Commit:

```text
Make spin contention a bounded Redis retry policy

Constraint: Spin means scheduled Redis attempts, never a CPU busy loop
Rejected: Dedicated spin state and unbounded retry | They add no ownership safety
Confidence: high
Scope-risk: moderate
Directive: Keep retry rate, jitter, deadline, and cancellation bounds testable
Tested: Spin backoff and distributed-lock semantic tests
Not-tested: Multi-lock atomicity
```

## Task 9: Implement same-slot atomic multi-lock

**Files:**

- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/internal/MultiLockScript.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceMultiLock.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceSuspendMultiLock.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/MultiLockScriptTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LettuceMultiLockTest.kt`
- Modify only if extraction proves smaller and behavior-neutral:
  `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeaseSupport.kt`
- Modify only with that extraction:
  existing multi-key lease focused tests

- [ ] Write RED cases for codec wire-byte same-slot validation, distinct names, bounded max keys, all-or-nothing acquire,
      all-key reentry with one generation, renewal/release, partial ownership integrity failure, persistent same-owner
      key integrity failure, conflict without mutation, expiry/takeover, cancellation/reconcile, and Cluster factory.
- [ ] Prove the ordered encoded-key set derives one stable group fingerprint/generation authority and that a changed
      constituent set cannot reuse or mutate the prior group's handle.
- [ ] Reuse the existing multi-key lease slot and partial-state fixture. Avoid forced extraction if it increases old
      primitive risk.
- [ ] Validate all encoded keys before dispatch.
- [ ] Mutate every constituent state in one Lua script and return no partial success.
- [ ] Reject different-slot input; never implement sequential best-effort locking.
- [ ] Run every existing multi-key lease test if its support file changes.

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-lettuce:test \
  --tests '*MultiLock*' \
  --tests '*MultiKeyLease*'
```

Expected: multi-lock and existing multi-key lease regressions pass.

Commit:

```text
Keep multi-lock ownership atomic within one Redis slot

Constraint: Every constituent key must share owner and generation in one script
Rejected: Cross-slot sequential locking | It cannot provide all-or-nothing semantics
Confidence: high
Scope-risk: broad
Directive: Treat partial or persistent same-owner state as integrity failure
Tested: Multi-lock and existing multi-key lease suites
Not-tested: Cross-family synchronizer reuse
```

## Task 10: Prove cross-cutting Cluster, ambiguity, concurrency, and protocol behavior

**Files:**

- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockClusterTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockTopologyRecoveryTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockConcurrencyTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockFailureTest.kt`
- Modify: `infra/lettuce/build.gradle.kts`
- Modify: focused production/internal files only when a failing case proves a defect

- [ ] Add literal `coordination-lock-topology` to the default test exclusions, annotate only
      `LockTopologyRecoveryTest` with `@Tag("coordination-lock-topology")`, and register
      `coordinationLockTopologyRecoveryTest` with `includeTags("coordination-lock-topology")`, no cache/up-to-date
      reuse, and `shouldRunAfter(test)`.
- [ ] Prove standalone and Cluster same-slot success for all applicable locks.
- [ ] Prove cross-slot failure occurs before command dispatch and contains only slot count, never raw key/name.
- [ ] Inject NOSCRIPT and prove cold path is exactly EVALSHA+EVAL while warm path is one EVALSHA.
- [ ] Inject unknown tag/arity/oversized response and prove fail-closed integrity result.
- [ ] Exercise MOVED/ASK/topology recovery. A post-dispatch redirect/timeout becomes ambiguous and preserves identity;
      it never auto-retries with a new owner/request.
- [ ] Prove one-winner exclusivity, reentrant hold-count consistency, fair FIFO, phase progress, fencing monotonicity,
      spin attempt bound, and multi-lock atomicity under deterministic concurrent actors.
- [ ] Prefer `MultithreadingTester`/`SuspendedJobTester` where they preserve ordering evidence; otherwise document the
      deterministic barrier needed by the test.
- [ ] Verify logs/events/metrics contain only allowlisted dimensions and redacted digest.

Run sequentially:

```bash
./gradlew :bluetape4k-lettuce:tasks --all | rg 'coordinationLockTopologyRecoveryTest'
repo-test-summary -- ./gradlew :bluetape4k-lettuce:test \
  --tests '*LockClusterTest' \
  --tests '*LockConcurrencyTest' \
  --tests '*LockFailureTest'
repo-test-summary -- ./gradlew :bluetape4k-lettuce:coordinationLockTopologyRecoveryTest
```

Expected: all fault and concurrency cases pass with no raw secret/key output.

Commit:

```text
Fail lock coordination closed across Redis protocol and topology changes

Constraint: Redirects and transport loss after dispatch may hide a completed mutation
Rejected: New-identity automatic retry | It can duplicate ownership changes
Confidence: high
Scope-risk: broad
Directive: Preserve bounded decode and same-identity recovery evidence
Tested: Cluster, topology, protocol, ambiguity, concurrency, and redaction tests
Not-tested: Characterization workload
```

## Task 11: Add command-budget and bounded performance characterization

**Files:**

- Modify: `infra/lettuce/build.gradle.kts`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockPerformanceTest.kt`
- Create: `infra/lettuce/scripts/validate-lock-performance.py`
- Create: `infra/lettuce/scripts/test_validate_lock_performance.py`
- Generated, not committed unless repository convention requires:
  `infra/lettuce/build/reports/coordination-lock-performance/results.json`

- [ ] Add literal `coordination-lock-performance` to the default test exclusions. Annotate only
      `LockPerformanceTest` with `@Tag("coordination-lock-performance")`; do not use the existing generic
      `@Tag("performance")`.
- [ ] Register `coordinationLockPerformanceTest` with `includeTags("coordination-lock-performance")`, a declared JSON
      output, `doFirst` deletion of the prior report, no up-to-date/cache reuse, and `shouldRunAfter(test)`.
- [ ] Write characterization for warm/cold command count, fair cleanup batch, spin attempt rate, watchdog per-tick
      dispatch and due-backlog drain, hot-lock p50/p95 wait/command latency, bounded retained-state growth,
      errors/timeouts, and a separate PING responsiveness probe.
- [ ] Pin retained-state proof to 10,000 warmup operations followed by 50,000 measured contended attempts from eight
      workers. Capture baseline/peak/final runtime task, watchdog, waiter, queue-entry, and request-hold counts. Require
      final counts to equal baseline, peak counts not to exceed their configured caps, zero busy-loop attempts, and no
      monotonic count growth across five equal measurement windows. Record these values in JSON. This is a retained
      state/leak bound, not a claim about total JVM allocation bytes.
- [ ] Use a dedicated Redis server/connections/executors and explicit `finally` cleanup, following
      `LettuceMultiKeyLeasePerformanceTest`.
- [ ] Assert semantic bounds and normalized within-run ratios, not machine-specific absolute latency.
- [ ] Write the report to a sibling temporary file, fsync/close it, and atomically move it to `results.json`. Include
      schema version, run ID, Redis/JVM/OS metadata, workload dimensions, command counts, percentiles, errors/timeouts,
      responsiveness, baseline/peak/final retained-state counts, watchdog tick cadence, due/dispatched/late/missed
      renewals, maximum due backlog, calculated dispatch drain, Redis-completion safety margin, cap values, and cleanup
      state. Do not serialize environment variables or credentials.
- [ ] Add a dependency-free Python validator and negative-fixture tests. Validate baseline/peak/final count arithmetic,
      every peak against its cap, five-window non-growth, warm=1/cold=2 command budgets, watchdog tick at most 256,
      backlog drain cadence at most 25 ms, due renewals equal dispatched renewals, zero late/missed renewals, maximum
      backlog within the calculated service envelope, at least a 1-second completion safety margin, spin at most 100
      attempts/second, finite positive latency/responsiveness values, zero errors/timeouts, complete metadata, atomic
      report schema version, cleanup, and absence of identity/key/credential fields.
- [ ] Include a deterministic 10,000-registration runtime scenario that advances the injected ticker through the full
      backlog and a Redis-backed representative-load scenario. The first proves the hard-cap formula without wall-clock
      flakiness; the second proves the report/alert path with zero late or missed renewal.
- [ ] Verify the task names through Gradle task listing before executing them.

Run:

```bash
./gradlew :bluetape4k-lettuce:tasks --all | \
  rg 'coordinationLock(Performance|TopologyRecovery)Test'
repo-test-summary -- ./gradlew :bluetape4k-lettuce:coordinationLockPerformanceTest
python3 infra/lettuce/scripts/test_validate_lock_performance.py
python3 infra/lettuce/scripts/validate-lock-performance.py \
  infra/lettuce/build/reports/coordination-lock-performance/results.json
```

Expected: both task names exist; characterization passes and writes non-empty JSON.

Commit:

```text
Measure lock command and scheduler bounds before publishing capability claims

Constraint: Redis timing is environment-dependent while command and progress bounds are contractual
Rejected: Absolute latency threshold | It would be flaky across CI and developer machines
Confidence: high
Scope-risk: moderate
Directive: Re-run characterization after Lua, cleanup, retry, or watchdog default changes
Tested: Gradle task discovery, command-budget fixture, characterization report, and cleanup
Not-tested: Long-duration production traffic
```

## Task 12: Publish English/Korean guidance and compile-tested examples

**Files:**

- Modify: `infra/lettuce/README.md`
- Modify: `infra/lettuce/README.ko.md`
- Create: `infra/lettuce/CoordinationLocks.md`
- Create: `infra/lettuce/CoordinationLocks.ko.md`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockDocumentationTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lock/LockApiSurfaceTest.kt`
- Create: `infra/lettuce/src/test/java/io/bluetape4k/redis/lettuce/lock/LettuceLockJavaDocumentationTest.java`
- Create: `docs/images/readme-diagrams/infra-lettuce-diagram-03.svg`
- Create: `docs/images/readme-diagrams/infra-lettuce-diagram-03.png`
- Create: `docs/images/readme-diagrams/infra-lettuce-diagram-03-ko.svg`
- Create: `docs/images/readme-diagrams/infra-lettuce-diagram-03-ko.png`
- Create: `docs/images/readme-diagrams/infra-lettuce-sequence-02.svg`
- Create: `docs/images/readme-diagrams/infra-lettuce-sequence-02.png`
- Create: `docs/images/readme-diagrams/infra-lettuce-sequence-02-ko.svg`
- Create: `docs/images/readme-diagrams/infra-lettuce-sequence-02-ko.png`
- Create: `docs/superpowers/reviews/2026-07-25-issue-1080-lock-diagram-review.md`
- Modify: `scripts/validate-readme-diagram-assets.mjs`
- Modify: `scripts/validate-readme-diagram-assets_test.mjs`
- Modify: all new public Kotlin files for final English KDoc

- [ ] Add the six Lock types and suspend counterparts to both feature tables.
- [ ] Add a capability/selection matrix covering distributed, fair, fenced, read-write, spin, and multi-lock.
- [ ] Add a Lock-family architecture diagram directly beside the capability/selection matrix. It must answer “which
      Lock object should I choose, and what infrastructure is shared?” by showing all six object families, blocking /
      async / suspend surfaces, the neutral coordination runtime, Lua/Redis Cluster state, observation sink, and the
      explicit legacy compatibility boundary.
- [ ] Add a Lock lifecycle sequence diagram before the first acquisition example. It must show caller, public facade,
      coordination runtime, Lua/Redis participants and the ordered acquire/reentry-or-contention, wait, watchdog,
      ambiguous cancellation, same-identity reconcile, release, and close-without-implicit-unlock paths. Keep
      alternatives chronological instead of turning the sequence into a generic flowchart.
- [ ] Use separate localized assets. `README.md` and `CoordinationLocks.md` embed
      `infra-lettuce-diagram-03.png` and `infra-lettuce-sequence-02.png`; their Korean companions embed the `-ko.png`
      assets. English and Korean SVGs share geometry and technical identifiers but use natural locale-specific
      reader-facing labels.
- [ ] Build each of the four SVG/PNG pairs one at a time after Tasks 3-10 are green: re-read the final README section
      and implementing source, edit one SVG, normalize/parse, render its PNG with CairoSVG `-s 2`, run common plus
      architecture/sequence audits, inspect the full-size PNG, and record evidence before moving to the next asset.
- [ ] Instantiate `DIA-01` through `DIA-08`, every applicable `DIA-COM-*`, and the matching `DIA-ARC-*` or
      `DIA-SEQ-*` rows for every asset in `2026-07-25-issue-1080-lock-diagram-review.md`. Record source/readme paths,
      two sequence reference PNGs, the nearest Lettuce architecture reference, XML/render commands, PNG dimensions,
      text hazards, connector/card/label counts, endpoint/mixed-corner/sequence results, full-size inspection, README
      embeds, locale parity, and `Blocked=0`. `WEAK`, `UNAVAILABLE`, zero meaningful counts, or SVG-only success does
      not pass.
- [ ] Extend the existing README diagram validator with an exact-filename `DIAGRAM_VALIDATION_TARGETS` filter. The
      unset/default behavior must remain the current full repository scan. Add positive, missing-target, duplicate,
      and default-compatibility tests; do not hide target failures behind the repository's pre-existing full-scan
      failures.
- [ ] Add compile-tested blocking, async, and suspend examples with explicit owner/request lifecycle.
- [ ] Add a nested reentry example that stores the outer and inner acquisition handles, releases the inner handle once,
      then the outer handle once, proves duplicate release is `AlreadyReleased`, and explains that every successful
      acquisition request produces one request-bound hold that must be released exactly once.
- [ ] Add Java compile fixtures for all six standalone and Cluster factory/config shapes, `LockOwnerId.random()`,
      `LockRequestId.random()`, `instanceof LockAcquireResult.Acquired<?>`, specialized handles, async
      `CompletableFuture`, negative/`Closed` variants, and handle-based release.
- [ ] Add a reflection/source compile matrix for every standalone/Cluster/no-config/config/scheduler/sink factory,
      specialized generic handle return, bootstrap, read/write view, downgrade, async counterpart, suspend counterpart,
      and non-suspending close.
- [ ] Add fixed/watchdog examples, `close` ownership, ownership-loss response, cancellation/ambiguous reconciliation,
      fair timeout cleanup, downstream strict-greater fencing guard, read-write downgrade, spin bounds, and same-slot
      hash-tag multi-lock.
- [ ] Show suspend-object `close()` explicitly and state that it stops local registrations/new work but does not release
      Redis ownership.
- [ ] Add an old/new migration table. State that existing classes remain supported and are not deprecated in Delivery 1.
- [ ] Put `Coordination primitives` before legacy lock examples. Label `LettuceLock`/`LettuceSuspendLock` as
      compatibility token mutexes, and map all six legacy surfaces:
      `LettuceLock`, `LettuceSuspendLock`, `LettuceFencingLease`, `LettuceSuspendFencingLease`,
      `LettuceMultiKeyLease`, and `LettuceSuspendMultiKeyLease`.
- [ ] Add operator guidance for ACL minimum script/key commands, TLS/credentials, versioned namespace, metrics/alerts,
      rollout, rollback, drain, bounded cleanup, generation/fencing-counter preservation, and safe reconcile.
- [ ] Make `LockDocumentationTest` validate stable markers
      `coordination-locks:observability`, `coordination-locks:acl-tls`,
      `coordination-locks:rollout-rollback`, `coordination-locks:drain-cleanup`,
      `coordination-locks:ambiguous-reconcile`, `coordination-locks:watchdog-leak`,
      `coordination-locks:namespace-migration`, and `coordination-locks:alerts`, plus required fragments for every
      documented operator action and §7 signal.
- [ ] Make `LockDocumentationTest` verify the exact locale-specific architecture/sequence PNG embeds and non-empty,
      reader-facing alt text in both READMEs and both long-form guides.
- [ ] Keep `README.md` and public KDoc in English; keep the `.ko.md` companion semantically equivalent.
- [ ] State non-goals: no Java thread ownership, no indefinite wait/watchdog, no cross-slot best effort, no read upgrade,
      no exactly-once/stale-work-stop claim, and no implicit unlock on close.

Run:

```bash
./gradlew :bluetape4k-lettuce:test \
  --tests '*LockDocumentationTest' \
  --tests '*LockApiSurfaceTest' \
  --tests '*LettuceLockJavaDocumentationTest'
./gradlew :bluetape4k-lettuce:dokkaGenerate
node scripts/validate-readme-diagram-assets_test.mjs
DIAGRAM_VALIDATION_TARGETS='infra-lettuce-diagram-03.svg,infra-lettuce-diagram-03-ko.svg,infra-lettuce-sequence-02.svg,infra-lettuce-sequence-02-ko.svg' \
  DIAGRAM_VALIDATION_REPORT=infra/lettuce/build/reports/coordination-lock-diagrams.json \
  node scripts/validate-readme-diagram-assets.mjs

DIAGRAM_SKILLS_ROOT="${CODEX_HOME:-$HOME/.codex}/skills/bluetape-diagram/scripts"

python3 "$DIAGRAM_SKILLS_ROOT/diagram-svg-text-normalize.py" \
  docs/images/readme-diagrams/infra-lettuce-diagram-03.svg
xmllint --noout docs/images/readme-diagrams/infra-lettuce-diagram-03.svg
cairosvg docs/images/readme-diagrams/infra-lettuce-diagram-03.svg \
  -o docs/images/readme-diagrams/infra-lettuce-diagram-03.png -s 2
python3 "$DIAGRAM_SKILLS_ROOT/diagram-connector-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-diagram-03.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-geometry-audit.py" --fail-diagonal \
  docs/images/readme-diagrams/infra-lettuce-diagram-03.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-endpoint-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-diagram-03.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-mixed-corner-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-diagram-03.svg

python3 "$DIAGRAM_SKILLS_ROOT/diagram-svg-text-normalize.py" \
  docs/images/readme-diagrams/infra-lettuce-diagram-03-ko.svg
xmllint --noout docs/images/readme-diagrams/infra-lettuce-diagram-03-ko.svg
cairosvg docs/images/readme-diagrams/infra-lettuce-diagram-03-ko.svg \
  -o docs/images/readme-diagrams/infra-lettuce-diagram-03-ko.png -s 2
python3 "$DIAGRAM_SKILLS_ROOT/diagram-connector-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-diagram-03-ko.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-geometry-audit.py" --fail-diagonal \
  docs/images/readme-diagrams/infra-lettuce-diagram-03-ko.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-endpoint-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-diagram-03-ko.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-mixed-corner-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-diagram-03-ko.svg

python3 "$DIAGRAM_SKILLS_ROOT/diagram-svg-text-normalize.py" \
  docs/images/readme-diagrams/infra-lettuce-sequence-02.svg
xmllint --noout docs/images/readme-diagrams/infra-lettuce-sequence-02.svg
cairosvg docs/images/readme-diagrams/infra-lettuce-sequence-02.svg \
  -o docs/images/readme-diagrams/infra-lettuce-sequence-02.png -s 2
python3 "$DIAGRAM_SKILLS_ROOT/diagram-connector-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-sequence-02.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-geometry-audit.py" --fail-diagonal \
  docs/images/readme-diagrams/infra-lettuce-sequence-02.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-endpoint-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-sequence-02.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-mixed-corner-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-sequence-02.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-sequence-style-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-sequence-02.svg

python3 "$DIAGRAM_SKILLS_ROOT/diagram-svg-text-normalize.py" \
  docs/images/readme-diagrams/infra-lettuce-sequence-02-ko.svg
xmllint --noout docs/images/readme-diagrams/infra-lettuce-sequence-02-ko.svg
cairosvg docs/images/readme-diagrams/infra-lettuce-sequence-02-ko.svg \
  -o docs/images/readme-diagrams/infra-lettuce-sequence-02-ko.png -s 2
python3 "$DIAGRAM_SKILLS_ROOT/diagram-connector-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-sequence-02-ko.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-geometry-audit.py" --fail-diagonal \
  docs/images/readme-diagrams/infra-lettuce-sequence-02-ko.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-endpoint-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-sequence-02-ko.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-mixed-corner-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-sequence-02-ko.svg
python3 "$DIAGRAM_SKILLS_ROOT/diagram-sequence-style-audit.py" \
  docs/images/readme-diagrams/infra-lettuce-sequence-02-ko.svg

git diff --check
```

Expected: examples compile/run, Dokka succeeds, locale content has the same capability rows, and Markdown has no
whitespace errors. All four SVGs parse and pass the triggered audits, all four canonical `-s 2` PNGs exist and were
inspected full-size after the last coordinate change, README/guides embed the correct locale assets, the repository
diagram validator's exact four-file target reports `total=4 failed=0`, its default full-scan behavior remains
compatible, and the diagram ledger reports every applicable `DIA-*` row with `Blocked=0`.

Commit:

```text
Make lock selection and recovery obligations explicit to Lettuce callers

Constraint: New locks are additive and do not replace existing primitives in this delivery
Rejected: Redisson semantic-parity claim | Only the object mental model is adopted
Confidence: high
Scope-risk: moderate
Directive: Keep English and Korean capability, recovery, and operator guidance aligned
Tested: Documentation examples, Dokka, locale parity, four diagram ledgers, CairoSVG renders, visual audits, README embeds, and diff check
Not-tested: External documentation rendering
```

## Task 13: Run complete verification and six-lens implementation review

**Files:**

- Modify only defects proven by verification/review.
- Create:
  `docs/superpowers/reviews/2026-07-25-issue-1080-lock-implementation-review.md`

Run the smallest proof first, then the full affected module:

```bash
repo-test-summary -- ./gradlew :bluetape4k-lettuce:test
repo-test-summary -- ./gradlew :bluetape4k-lettuce:coordinationLockTopologyRecoveryTest
repo-test-summary -- ./gradlew :bluetape4k-lettuce:coordinationLockPerformanceTest
./gradlew :bluetape4k-lettuce:check
./gradlew :bluetape4k-lettuce:dokkaGenerate
./gradlew detekt
node scripts/validate-readme-diagram-assets_test.mjs
DIAGRAM_VALIDATION_TARGETS='infra-lettuce-diagram-03.svg,infra-lettuce-diagram-03-ko.svg,infra-lettuce-sequence-02.svg,infra-lettuce-sequence-02-ko.svg' \
  DIAGRAM_VALIDATION_REPORT=infra/lettuce/build/reports/coordination-lock-diagrams.json \
  node scripts/validate-readme-diagram-assets.mjs
rg -n 'Required checks: [1-9][0-9]*/[1-9][0-9]*; N/A: [0-9]+; Blocked: 0' \
  docs/superpowers/reviews/2026-07-25-issue-1080-lock-diagram-review.md
git diff --check
```

Expected:

- targeted and full module tests pass;
- topology and performance evidence tasks pass sequentially;
- check, Dokka, and Detekt pass;
- the targeted README diagram validator reports `total=4 failed=0`, its default behavior regression tests pass, all
  four Lock diagram assets have complete evidence ledgers, and the final full-size PNG inspection reports no open
  visual defect;
- no unresolved diagnostics or deprecation warnings exist in touched Kotlin;
- working diff contains only Delivery 1 artifacts.

Launch six independent read-only implementation reviews:

| Lens | Required proof |
|---|---|
| Performance | command budgets, bounded cleanup/retry/watchdog, allocation/task growth, characterization integrity |
| Stability | reentrancy, generations, cancellation, reconciliation, lifecycle, late completion, phase/FIFO progress |
| Security | entropy/bounds, KEYS/ARGV safety, protocol bounds, redaction, ACL/TLS boundary, capacity DoS |
| Operator | metrics/events, rollout/rollback/drain/cleanup, namespace, alerts, ownership-loss response |
| Developer/API | Java/Kotlin factories, blocking/async/suspend parity, result/failure taxonomy, package boundaries |
| User/Caller | object selection, identity lifecycle, handle use, recovery decision path, migration/non-goals |

Record every finding with file/line/evidence. Fix every P0/P1 and rerun the affected reviewer. The gate passes only when
all six lenses and main-session integration report:

```text
P0=0
P1=0
```

Commit review corrections separately when they span a prior task:

```text
Close lock delivery blockers before requesting external review

Constraint: Type A implementation requires six independent P0 and P1 clean perspectives
Rejected: Deferring known contract gaps to convergence | Convergence cannot invent missing safety
Confidence: high
Scope-risk: broad
Directive: Keep convergence limited to source-compatible naming and duplication correction
Tested: Full Lettuce check, topology, characterization, Dokka, Detekt, and six-lens review
Not-tested: Live GitHub CI environment
```

## Task 14: Publish PR 1 and stop at the merge gate

**Preconditions:**

- implementation plan approval exists;
- all Task 13 commands are fresh and green;
- implementation review is `P0=0 / P1=0`;
- branch contains no unrelated change;
- user has not revoked the PR creation authorization recorded in §1.2.

- [ ] Rebase/update from current `origin/develop` before final verification when needed; do not rewrite unrelated user
      work.
- [ ] Push `codex/issue-1080-lettuce-locks-design`.
- [ ] Create an English PR against `develop` linking `#1080`.
- [ ] Mirror issue assignee, milestone, and labels.
- [ ] Keep `## DoD Status` as the final PR-body section.
- [ ] Include the three-delivery boundary and state that this PR closes only the Lock delivery, not the whole issue.
- [ ] Include the four Lock diagram asset pairs and the completed `DIA-01`..`DIA-08`, `DIA-COM-*`, and applicable
      `DIA-ARC-*` / `DIA-SEQ-*` evidence ledger in `## DoD Status`; do not summarize them as an unfalsifiable
      “diagram checklist passed.”
- [ ] Verify the live rendered PR body, metadata, checks, review threads, and required human-review artifacts.
- [ ] Address CI/review defects within Delivery 1, rerun affected validation, and update review evidence.
- [ ] Report the exact PR/head SHA as merge-ready and stop for fresh user merge approval.

The PR must not:

- close #1080 before all three deliveries complete;
- include Synchronizer implementation;
- deprecate legacy classes without convergence evidence;
- enable auto-merge;
- merge without fresh user approval.

---

## 6. Acceptance traceability

| Approved Lock requirement | Task/proof |
|---|---|
| Six lock objects | Tasks 3, 5-9 |
| Blocking/async/suspend parity | shared `LockContract`, Tasks 3-9 |
| Logical-owner reentrancy and hold count | Tasks 1, 3 |
| Stable acquisition generation and stale protection | Tasks 1, 3, 10 |
| Fixed lease and watchdog | Tasks 3-4 |
| Fair FIFO and stale-head progress | Task 5 |
| Fencing token monotonicity and stable reentry | Task 6 |
| Read/write compatibility and phase fairness | Task 7 |
| Bounded spin backoff | Task 8 |
| Same-slot atomic MultiLock | Task 9 |
| Validation before dispatch | Tasks 1-3, 9-10 |
| Expected post-dispatch sealed results | Tasks 1, 3-10 |
| Cancellation identity and reconciliation | Tasks 3-5, 9-10 |
| EVALSHA/NOSCRIPT command budget | Tasks 2-3, 10-11 |
| Bounded protocol response | Tasks 2, 10 |
| Runtime/scheduler ownership and leak prevention | Tasks 2, 4 |
| Low-cardinality/redacted observability | Tasks 2, 10, 12 |
| Cluster slot and topology ambiguity | Tasks 9-10 |
| Compatibility with existing primitives | Tasks 3, 6, 9, 12-13 |
| English/Korean docs and runbook | Task 12 |
| Reader-facing Lock architecture and lifecycle diagrams | Task 12 localized SVG/PNG pairs, visual audits, full-size inspection, and embed tests |
| Performance/stability/security/operator/API/caller review | Task 13 |

---

## 7. Risk prediction, signals, mitigation, rollback

| Risk | Early signal | Prevention/mitigation | Rollback or rerun point |
|---|---|---|---|
| Reentrant ABA changes a newer owner | stale release changes state | monotonic non-reset generation checked in every mutation | revert affected algorithm commit; rerun Tasks 3/10 |
| Reentrant release decrements twice | ambiguous retry lowers hold count again | one request-bound hold per acquire; single-use release; bounded terminal marker | stop new objects; reconcile generation; rerun Tasks 3/10 |
| Hold ledger grows without bound | active-hold gauge reaches cap | 10,000 hard cap, final-release cleanup, retained-state test | reject new holds; drain owner; rerun Tasks 1/3/11 |
| Cancellation duplicates acquisition | new request appears after timeout | preserve request ID; ambiguous result plus reconcile | stop rollout; drain new namespace; rerun Tasks 3-5/10 |
| Fair head blocks forever | queue gauge grows, no admissions | Redis deadline, bounded cleanup, `CleanupPending` no-bypass | disable new FairLock creation; TTL/drain; rerun Task 5 |
| Reader or writer starvation | phase boundary stops progressing | deterministic phase-fair script and progress tests | disable ReadWriteLock opt-in; rerun Task 7 |
| Watchdog leaks tasks | task/watchdog gauges stay nonzero after close | registry cap, task ID/generation guard, owned scheduler lifecycle | close runtime, stop new objects, rerun Tasks 2/4 |
| Watchdog backlog misses TTL | due backlog grows or late/missed counter increments | admission formula, 25-ms drain tick, 256 dispatch cap, 1-second completion margin | reject new watchdogs; mark ownership loss; rerun Tasks 2/4/11 |
| Watchdog masks lease loss | caller continues after ownership loss | explicit observer/result and fenced downstream guidance | stop non-fenced critical workload; reconcile/replace owner |
| Fencing counter regression | token not strictly increasing | explicit bootstrap/epoch and non-deleted counter | stop FencedLock rollout; preserve counter; rerun Task 6 |
| MultiLock partial ownership | integrity result or partial keys | one same-slot Lua script; fail closed | stop new MultiLock; inspect durable authority; rerun Task 9 |
| Cluster redirect repeats mutation | duplicate generation/hold count | same-identity reconcile; no blind retry | stop affected path; inspect handle/request; rerun Task 10 |
| Protocol/memory DoS | oversized reply/queue/task growth | strict input/reply/capacity bounds | close runtime, isolate namespace, rerun Tasks 1-2/10 |
| Metric cardinality or secret leak | raw name/ID in labels/logs | allowlist and redaction tests | disable observation sink; rotate credentials if exposed |
| README diagrams drift from implementation | source/read markers, locale embed test, or visual audit fails | create only after Tasks 3-10; source-backed localized assets; one-asset render/inspect loop | block docs/PR gate; regenerate the affected asset; rerun Task 12 |
| Legacy behavior regression | existing lease/lock suite fails | additive files; optional extraction only with full regression | revert extraction; retain semantic reuse only |
| PR becomes issue-wide monolith | Synchronizer files appear in diff | path/scope audit and PR wording | remove out-of-scope changes before push |

Rollback never deletes active owner state, generation counters, or fencing counters. Stop creating new Lock objects,
retain the versioned `bt4k:coord:v1` namespace, wait the maximum fixed/watchdog/wait TTL, inspect for empty
owner/waiter state, then perform bounded operator cleanup. Existing legacy APIs remain available throughout rollback.

---

## 8. Final scope and consistency checks

Before implementation completion:

```bash
git diff --name-only origin/develop...HEAD
git diff --check origin/develop...HEAD
rg -n 'TODO|FIXME|TBD|placeholder|implement later' \
  infra/lettuce/src/main \
  infra/lettuce/src/test \
  infra/lettuce/README.md \
  infra/lettuce/README.ko.md \
  infra/lettuce/CoordinationLocks.md \
  infra/lettuce/CoordinationLocks.ko.md \
  docs/images/readme-diagrams/infra-lettuce-diagram-03.svg \
  docs/images/readme-diagrams/infra-lettuce-diagram-03-ko.svg \
  docs/images/readme-diagrams/infra-lettuce-sequence-02.svg \
  docs/images/readme-diagrams/infra-lettuce-sequence-02-ko.svg
DIAGRAM_VALIDATION_TARGETS='infra-lettuce-diagram-03.svg,infra-lettuce-diagram-03-ko.svg,infra-lettuce-sequence-02.svg,infra-lettuce-sequence-02-ko.svg' \
  node scripts/validate-readme-diagram-assets.mjs
```

Expected:

- no Synchronizer implementation path;
- no unrelated module path;
- no whitespace error;
- no placeholder marker in production, test, or documentation;
- four locale-correct Lock SVG/PNG pairs are embedded, rendered, visually inspected, and validator-clean;
- existing legacy public classes still present and not newly deprecated.

Public-surface parity check:

```bash
./gradlew :bluetape4k-lettuce:test \
  --tests '*LockApiSurfaceTest' \
  --tests '*LettuceLockJavaDocumentationTest'
rg -n 'fun (tryAcquire|acquire|inspect|reconcile|renew|release)(Async)?\\(' \
  infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lock
```

The compile/reflection fixtures are authoritative for factories, generic handle returns, bootstrap, read/write views,
downgrade, close, and Java source compatibility. Review the grep output as a secondary matrix: each supported blocking
operation has one `*Async`; every suspend counterpart returns the same public result type. Differences are allowed only
for fenced bootstrap, read/write views/downgrade, and handle specialization explicitly listed in §2.

After merge approval and merge, but before Delivery 2 planning:

```bash
git -C /Users/debop/work/bluetape4k/bluetape4k-projects fetch origin
git -C /Users/debop/work/bluetape4k/bluetape4k-projects switch develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects pull --ff-only origin develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects rev-parse HEAD
git -C /Users/debop/work/bluetape4k/bluetape4k-projects rev-parse origin/develop
worktree-list
```

Expected: local root `develop` equals `origin/develop`; the Delivery 1 worktree/branch cleanup target is identified.
Destructive worktree/branch deletion is performed only after merged-state and clean-tree proof.

---

## 9. Execution choice after plan approval

### Option A — Subagent-driven execution (recommended)

Pros:

- bounded specialist ownership keeps Lua/runtime/API/test work reviewable;
- independent review is preserved for concurrency, security, and caller/API contracts;
- main session can integrate evidence while non-overlapping pure-test/document lanes progress.

Cons:

- shared public/internal files require strict write-scope coordination;
- Redis/Testcontainers and final verification still run sequentially;
- more handoff evidence is needed at task boundaries.

### Option B — Inline single-owner execution

Pros:

- one continuous implementation context;
- no shared-file coordination overhead;
- simpler commit sequencing.

Cons:

- slower across the six algorithms;
- higher context pressure and greater risk of review fatigue;
- independent implementation judgment arrives only at the final review gate.

Recommendation: approve Option A, but keep Tasks 1-4 and all shared-file integration under one primary executor.
Delegate only isolated algorithm tests/implementations after the shared contract is green, and serialize every Redis
verification task.
