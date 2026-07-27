package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.codec.Base58
import java.io.InvalidObjectException
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.time.Duration

private const val BASE58_LENGTH = 22
private const val MAX_ID_BYTES = 256
private const val MAX_NAMESPACE_BYTES = 128
private const val MAX_PERMITS = 1_000_000
private const val MAX_LATCH_COUNT = 1_000_000

/** Stable, caller-supplied semaphore owner identity. Its value is redacted from [toString]. */
class SemaphoreOwnerId private constructor(internal val value: String): Serializable {
    init { validateIdentity(value, "Semaphore owner ID") }
    override fun equals(other: Any?): Boolean = other is SemaphoreOwnerId && value == other.value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = "SemaphoreOwnerId(<redacted>)"
    private fun readResolve(): Any = restore("SemaphoreOwnerId") { SemaphoreOwnerId(value) }
    companion object {
        private const val serialVersionUID = 1L
        @JvmStatic fun random(): SemaphoreOwnerId = SemaphoreOwnerId(Base58.randomString(BASE58_LENGTH))
        @JvmStatic fun from(value: String): SemaphoreOwnerId = SemaphoreOwnerId(value)
    }
}

/** Idempotency identity for one logical semaphore operation. Reuse it only for reconciliation. */
class SemaphoreRequestId private constructor(internal val value: String): Serializable {
    init { validateIdentity(value, "Semaphore request ID") }
    override fun equals(other: Any?): Boolean = other is SemaphoreRequestId && value == other.value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = "SemaphoreRequestId(<redacted>)"
    private fun readResolve(): Any = restore("SemaphoreRequestId") { SemaphoreRequestId(value) }
    companion object {
        private const val serialVersionUID = 1L
        @JvmStatic fun random(): SemaphoreRequestId = SemaphoreRequestId(Base58.randomString(BASE58_LENGTH))
        @JvmStatic fun from(value: String): SemaphoreRequestId = SemaphoreRequestId(value)
    }
}

/** Idempotency identity for one logical latch mutation or wait. */
class LatchRequestId private constructor(internal val value: String): Serializable {
    init { validateIdentity(value, "Latch request ID") }
    override fun equals(other: Any?): Boolean = other is LatchRequestId && value == other.value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = "LatchRequestId(<redacted>)"
    private fun readResolve(): Any = restore("LatchRequestId") { LatchRequestId(value) }
    companion object {
        private const val serialVersionUID = 1L
        @JvmStatic fun random(): LatchRequestId = LatchRequestId(Base58.randomString(BASE58_LENGTH))
        @JvmStatic fun from(value: String): LatchRequestId = LatchRequestId(value)
    }
}

/**
 * Generation-bound proof of a successful semaphore allocation.
 *
 * Persist the complete handle until release or reconciliation. Diagnostic rendering redacts every identity.
 */
data class PermitHandle(
    val objectFingerprint: String,
    val ownerId: SemaphoreOwnerId,
    val generation: Long,
    val requestId: SemaphoreRequestId,
    val permits: Int,
    val token: String,
): Serializable {
    init {
        validateIdentity(objectFingerprint, "Object fingerprint")
        require(generation > 0)
        require(permits in 1..MAX_PERMITS)
        validateIdentity(token, "Permit token")
    }
    override fun toString(): String = "PermitHandle(<redacted>)"
    private fun readResolve(): Any = restore("PermitHandle") { copy() }
    private companion object { private const val serialVersionUID = 1L }
}

/** Redis-time expiry identity for one permit unit inside an expirable allocation. */
data class ExpirablePermitLease(
    val permitId: String,
    val deadlineMillis: Long,
): Serializable {
    init {
        validateIdentity(permitId, "Permit ID")
        require(deadlineMillis > 0)
    }
    override fun toString(): String = "ExpirablePermitLease(<redacted>)"
    private fun readResolve(): Any = restore("ExpirablePermitLease") { copy() }
    private companion object { private const val serialVersionUID = 1L }
}

/**
 * Complete expirable allocation handle.
 *
 * All unit leases belong to the same atomic allocation and must be renewed or released together.
 */
data class ExpirablePermitHandle(
    val permit: PermitHandle,
    val leases: List<ExpirablePermitLease>,
): Serializable {
    init {
        require(leases.size == permit.permits) { "One lease identity is required for each permit." }
        require(leases.map { it.permitId }.distinct().size == leases.size) { "Permit IDs must be unique." }
    }
    override fun toString(): String = "ExpirablePermitHandle(<redacted>)"
    private fun readResolve(): Any = restore("ExpirablePermitHandle") { copy(leases = leases.toList()) }
    private companion object { private const val serialVersionUID = 1L }
}

/** Monotonic latch lifecycle generation used to reject stale callers after delete and recreate. */
data class LatchGeneration(val value: Long): Comparable<LatchGeneration>, Serializable {
    init { require(value > 0) }
    override fun compareTo(other: LatchGeneration): Int = value.compareTo(other.value)
    override fun toString(): String = "LatchGeneration(<redacted>)"
    private fun readResolve(): Any = restore("LatchGeneration") { copy() }
    private companion object { private const val serialVersionUID = 1L }
}

/** Transport-side failure category. Timeout and connection failures may make a mutation ambiguous. */
enum class SynchronizerBackendFailureKind { TIMEOUT, CONNECTION, COMMAND }
/** Redis reply or state invariant that could not be trusted. Integrity failures must fail closed. */
enum class SynchronizerIntegrityFailureKind { MALFORMED_REPLY, INVALID_NUMBER, CROSS_SLOT, STATE_MISMATCH }
/** Caller action recommended by a typed failure or ambiguous result. */
enum class SynchronizerRecoveryAction { RETRY, RECONCILE_REQUEST, INSPECT_HANDLE }

/** Backend failure with the recovery boundary callers should follow. */
data class SynchronizerBackendFailure(
    val kind: SynchronizerBackendFailureKind,
    val recoveryAction: SynchronizerRecoveryAction,
): Serializable

/** Fail-closed protocol or state-integrity failure. */
data class SynchronizerIntegrityFailure(
    val kind: SynchronizerIntegrityFailureKind,
): Serializable

/** Result of initializing a semaphore capacity without resetting an existing generation. */
sealed interface SemaphoreInitializationResult: Serializable {
    data class Initialized(val generation: Long): SemaphoreInitializationResult { init { require(generation > 0) } }
    data object AlreadyInitialized: SemaphoreInitializationResult
    data object InvalidCapacity: SemaphoreInitializationResult
    data object Closed: SemaphoreInitializationResult
    data class BackendFailure(val failure: SynchronizerBackendFailure): SemaphoreInitializationResult
    data class IntegrityFailure(val failure: SynchronizerIntegrityFailure): SemaphoreInitializationResult
}

/**
 * Result of acquiring permits.
 *
 * [Ambiguous] means Redis may have committed the request; reconcile the same owner/request identity before retrying.
 */
sealed interface PermitAcquireResult<out H: Serializable>: Serializable {
    data class Acquired<H: Serializable>(val handle: H): PermitAcquireResult<H>
    data object Unavailable: PermitAcquireResult<Nothing>
    data object TimedOut: PermitAcquireResult<Nothing>
    data object CapacityExceeded: PermitAcquireResult<Nothing>
    data object Closed: PermitAcquireResult<Nothing>
    data class BackendFailure(val failure: SynchronizerBackendFailure): PermitAcquireResult<Nothing>
    data class IntegrityFailure(val failure: SynchronizerIntegrityFailure): PermitAcquireResult<Nothing>
    data class Ambiguous(val requestId: SemaphoreRequestId): PermitAcquireResult<Nothing> {
        override fun toString(): String = "PermitAcquireResult.Ambiguous(requestId=<redacted>)"
    }
}

/**
 * Result of releasing or otherwise mutating an allocation.
 *
 * [Ambiguous] requires request reconciliation; stale, released, and expired outcomes are terminal for the handle.
 */
sealed interface PermitMutationResult<out H: Serializable>: Serializable {
    data class Released<H: Serializable>(val handle: H, val remainingPermits: Int): PermitMutationResult<H> {
        init { require(remainingPermits >= 0) }
    }
    data object AlreadyReleased: PermitMutationResult<Nothing>
    data object Expired: PermitMutationResult<Nothing>
    data object StaleGeneration: PermitMutationResult<Nothing>
    data object Closed: PermitMutationResult<Nothing>
    data class BackendFailure(val failure: SynchronizerBackendFailure): PermitMutationResult<Nothing>
    data class IntegrityFailure(val failure: SynchronizerIntegrityFailure): PermitMutationResult<Nothing>
    data class Ambiguous(val requestId: SemaphoreRequestId): PermitMutationResult<Nothing> {
        override fun toString(): String = "PermitMutationResult.Ambiguous(requestId=<redacted>)"
    }
}

/** Redis-authoritative ownership state for a generation-bound permit handle. */
sealed interface PermitInspectResult<out H: Serializable>: Serializable {
    data class Owned<H: Serializable>(val handle: H, val remainingPermits: Int): PermitInspectResult<H> {
        init { require(remainingPermits >= 0) }
    }
    data object Released: PermitInspectResult<Nothing>
    data object Expired: PermitInspectResult<Nothing>
    data object StaleGeneration: PermitInspectResult<Nothing>
    data object Closed: PermitInspectResult<Nothing>
    data class BackendFailure(val failure: SynchronizerBackendFailure): PermitInspectResult<Nothing>
    data class IntegrityFailure(val failure: SynchronizerIntegrityFailure): PermitInspectResult<Nothing>
}

/** Result of reconciling the original owner/request identity after an ambiguous acquire. */
sealed interface PermitReconcileResult<out H: Serializable>: Serializable {
    data class Owned<H: Serializable>(val handle: H, val remainingPermits: Int): PermitReconcileResult<H>
    data object Released: PermitReconcileResult<Nothing>
    data object NotFound: PermitReconcileResult<Nothing>
    data object StaleGeneration: PermitReconcileResult<Nothing>
    data object Closed: PermitReconcileResult<Nothing>
    data class BackendFailure(val failure: SynchronizerBackendFailure): PermitReconcileResult<Nothing>
    data class IntegrityFailure(val failure: SynchronizerIntegrityFailure): PermitReconcileResult<Nothing>
}

/**
 * Result of renewing an expirable allocation.
 *
 * [OwnershipLost] is distinct from normal release or expiry and requires caller-side ownership recovery.
 */
sealed interface PermitRenewResult<out H: Serializable>: Serializable {
    data class Renewed<H: Serializable>(val handle: H): PermitRenewResult<H>
    data object Released: PermitRenewResult<Nothing>
    data object Expired: PermitRenewResult<Nothing>
    data object OwnershipLost: PermitRenewResult<Nothing>
    data object StaleGeneration: PermitRenewResult<Nothing>
    data object Closed: PermitRenewResult<Nothing>
    data class BackendFailure(val failure: SynchronizerBackendFailure): PermitRenewResult<Nothing>
    data class IntegrityFailure(val failure: SynchronizerIntegrityFailure): PermitRenewResult<Nothing>
    data class Ambiguous(val requestId: SemaphoreRequestId): PermitRenewResult<Nothing> {
        override fun toString(): String = "PermitRenewResult.Ambiguous(requestId=<redacted>)"
    }
}

/** Result of creating a latch generation or observing an already active generation. */
sealed interface LatchSetCountResult: Serializable {
    data class Created(val generation: LatchGeneration): LatchSetCountResult
    data class ActiveGeneration(val generation: LatchGeneration, val count: Long): LatchSetCountResult {
        init { require(count >= 0) }
    }
    data object InvalidCount: LatchSetCountResult
    data object Closed: LatchSetCountResult
    data class BackendFailure(val failure: SynchronizerBackendFailure): LatchSetCountResult
    data class IntegrityFailure(val failure: SynchronizerIntegrityFailure): LatchSetCountResult
}

/** Generation-bound latch count, completion, deletion, and waiter state. */
sealed interface LatchCountResult: Serializable {
    data class Active(
        val generation: LatchGeneration,
        val count: Long,
        val waiters: Int,
    ): LatchCountResult {
        init {
            require(count > 0)
            require(waiters >= 0)
        }
    }
    data class Completed(val generation: LatchGeneration): LatchCountResult
    data object Deleted: LatchCountResult
    data object StaleGeneration: LatchCountResult
    data object Closed: LatchCountResult
    data class BackendFailure(val failure: SynchronizerBackendFailure): LatchCountResult
    data class IntegrityFailure(val failure: SynchronizerIntegrityFailure): LatchCountResult
}

/** Alias used by inspection methods; it intentionally has the same exhaustive result matrix as [LatchCountResult]. */
typealias LatchInspectResult = LatchCountResult

/**
 * Result of a bounded latch wait.
 *
 * [CapacityExceeded] protects the Redis waiter set, while [Ambiguous] preserves the request for reconciliation.
 */
sealed interface LatchAwaitResult: Serializable {
    data object Completed: LatchAwaitResult
    data object TimedOut: LatchAwaitResult
    data object Deleted: LatchAwaitResult
    data object StaleGeneration: LatchAwaitResult
    data object CapacityExceeded: LatchAwaitResult
    data object Closed: LatchAwaitResult
    data class BackendFailure(val failure: SynchronizerBackendFailure): LatchAwaitResult
    data class IntegrityFailure(val failure: SynchronizerIntegrityFailure): LatchAwaitResult
    data class Ambiguous(val requestId: LatchRequestId): LatchAwaitResult {
        override fun toString(): String = "LatchAwaitResult.Ambiguous(requestId=<redacted>)"
    }
}

/** Result of generation-bound count-down or delete mutations. */
sealed interface LatchMutationResult: Serializable {
    data class Decremented(val remaining: Long): LatchMutationResult { init { require(remaining >= 0) } }
    data object Completed: LatchMutationResult
    data object AlreadyCompleted: LatchMutationResult
    data object Deleted: LatchMutationResult
    data object NotFound: LatchMutationResult
    data object StaleGeneration: LatchMutationResult
    data class ActiveWaiters(val count: Int): LatchMutationResult { init { require(count > 0) } }
    data object Closed: LatchMutationResult
    data class BackendFailure(val failure: SynchronizerBackendFailure): LatchMutationResult
    data class IntegrityFailure(val failure: SynchronizerIntegrityFailure): LatchMutationResult
    data class Ambiguous(val requestId: LatchRequestId): LatchMutationResult {
        override fun toString(): String = "LatchMutationResult.Ambiguous(requestId=<redacted>)"
    }
}

/**
 * Shared distributed-semaphore configuration.
 *
 * [hashTag] is required when callers need an explicit Redis Cluster same-slot boundary.
 */
data class SemaphoreConfig(
    val namespace: String = "bt4k:coord:v1",
    val hashTag: String? = null,
    val maxPermits: Int = MAX_PERMITS,
    val pollInterval: Duration = Duration.ofMillis(25),
): Serializable {
    init {
        validateNamespace(namespace)
        hashTag?.let { validateIdentity(it, "hashTag") }
        require(maxPermits in 1..MAX_PERMITS)
        requirePositive(pollInterval, "pollInterval")
    }
}

/**
 * Expirable-semaphore limits.
 *
 * Cleanup is Redis-time-based and processes at most [cleanupBatchLimit] expired allocations per command.
 */
data class ExpirableSemaphoreConfig(
    val semaphore: SemaphoreConfig = SemaphoreConfig(),
    val leaseTime: Duration = Duration.ofSeconds(30),
    val maxPermitsPerAcquire: Int = 32,
    val cleanupBatchLimit: Int = 256,
): Serializable {
    init {
        requirePositive(leaseTime, "leaseTime")
        require(leaseTime.toMillis() >= 100)
        require(maxPermitsPerAcquire in 1..64)
        require(cleanupBatchLimit in 1..1_024)
    }
}

/**
 * Latch lifecycle and waiter limits.
 *
 * [maxWaiters] bounds live Redis waiter state; [waiterCleanupGrace] bounds cleanup tolerance after a wait ends.
 */
data class LatchConfig(
    val namespace: String = "bt4k:coord:v1",
    val hashTag: String? = null,
    val maxCount: Long = MAX_LATCH_COUNT.toLong(),
    val pollInterval: Duration = Duration.ofMillis(25),
    val maxWaiters: Int = 10_000,
    val waiterCleanupGrace: Duration = Duration.ofSeconds(5),
): Serializable {
    init {
        validateNamespace(namespace)
        hashTag?.let { validateIdentity(it, "hashTag") }
        require(maxCount in 1..MAX_LATCH_COUNT.toLong())
        requirePositive(pollInterval, "pollInterval")
        require(maxWaiters in 1..10_000)
        requirePositive(waiterCleanupGrace, "waiterCleanupGrace")
        require(waiterCleanupGrace <= Duration.ofMinutes(5))
    }
}

private fun validateIdentity(value: String, label: String) {
    val size = value.toByteArray(StandardCharsets.UTF_8).size
    require(value.isNotBlank() && size in 1..MAX_ID_BYTES) { "$label must contain 1..$MAX_ID_BYTES UTF-8 bytes." }
    require(value.none { it == '|' || it == ',' || it == '{' || it == '}' }) {
        "$label contains a reserved delimiter."
    }
}

private fun validateNamespace(value: String) {
    val size = value.toByteArray(StandardCharsets.UTF_8).size
    require(value.isNotBlank() && size in 1..MAX_NAMESPACE_BYTES) {
        "Namespace must contain 1..$MAX_NAMESPACE_BYTES UTF-8 bytes."
    }
    require(value.matches(Regex("[A-Za-z0-9][A-Za-z0-9:._-]*"))) { "Namespace has an invalid format." }
}

internal fun requirePositive(value: Duration, label: String) {
    require(!value.isZero && !value.isNegative) { "$label must be positive." }
}

private fun <T> restore(type: String, factory: () -> T): T = try {
    factory()
} catch (_: RuntimeException) {
    throw InvalidObjectException("Invalid serialized $type.")
}
