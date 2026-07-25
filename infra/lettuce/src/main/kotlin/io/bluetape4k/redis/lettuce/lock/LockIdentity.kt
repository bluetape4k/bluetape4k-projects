package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.codec.Base58
import java.io.InvalidObjectException
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Identifies the logical reentrancy domain of a lock caller.
 *
 * This identifier is not an authentication credential. Reuse it only for calls that intentionally share one
 * reentrant ownership domain. Its raw value is excluded from diagnostics.
 */
class LockOwnerId private constructor(
    internal val value: String,
): Serializable {
    init {
        validateOpaqueIdentity(value, "Lock owner ID")
    }

    override fun equals(other: Any?): Boolean = other is LockOwnerId && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "LockOwnerId(<redacted>)"

    private fun readResolve(): Any = restoreLockSerializedValue("LockOwnerId") { LockOwnerId(value) }

    /** Creates logical owner identifiers. */
    companion object {
        private const val serialVersionUID: Long = 1L

        /** Creates a cryptographically strong identifier for a new logical owner. */
        @JvmStatic
        fun random(): LockOwnerId = LockOwnerId(Base58.randomString(RANDOM_BASE58_LENGTH))

        /** Creates an identifier from a caller-managed logical owner value. */
        @JvmStatic
        fun from(value: String): LockOwnerId = LockOwnerId(value)
    }
}

/**
 * Identifies one logical lock operation across dispatch, retry, and reconciliation.
 *
 * A new operation uses a new request identifier. An ambiguous operation must retain the same identifier until its
 * terminal state is reconciled. Its raw value is excluded from diagnostics.
 */
class LockRequestId private constructor(
    internal val value: String,
): Serializable {
    init {
        validateOpaqueIdentity(value, "Lock request ID")
    }

    override fun equals(other: Any?): Boolean = other is LockRequestId && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "LockRequestId(<redacted>)"

    private fun readResolve(): Any = restoreLockSerializedValue("LockRequestId") { LockRequestId(value) }

    /** Creates logical request identifiers. */
    companion object {
        private const val serialVersionUID: Long = 1L

        /** Creates a cryptographically strong identifier for a new logical operation. */
        @JvmStatic
        fun random(): LockRequestId = LockRequestId(Base58.randomString(RANDOM_BASE58_LENGTH))

        /** Creates an identifier from a caller-managed logical operation value. */
        @JvmStatic
        fun from(value: String): LockRequestId = LockRequestId(value)
    }
}

/**
 * Represents a monotonic Redis ownership generation.
 *
 * Generations are positive, never reset within one lock authority domain, and excluded from diagnostics.
 */
data class LockGeneration(
    val value: Long,
): Comparable<LockGeneration>, Serializable {
    init {
        require(value > 0L) { "Lock generation must be positive." }
    }

    override fun compareTo(other: LockGeneration): Int = value.compareTo(other.value)

    override fun toString(): String = "LockGeneration(<redacted>)"

    private fun readResolve(): Any = restoreLockSerializedValue("LockGeneration") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Identifies the concrete lock algorithm associated with a handle or observation. */
enum class LockKind {
    DISTRIBUTED,
    FAIR,
    FENCED,
    READ,
    WRITE,
    SPIN,
    MULTI,
}

/** Describes whether a lock uses a fixed lease or a bounded watchdog renewal policy. */
sealed interface LeasePolicy: Serializable {

    /** Uses one fixed Redis TTL and never starts automatic renewal. */
    data class Fixed(
        val leaseTime: Duration,
    ): LeasePolicy {
        init {
            validateFixedLease(leaseTime)
        }

        private fun readResolve(): Any = restoreLockSerializedValue("LeasePolicy.Fixed") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Uses a bounded shared-runtime watchdog to renew ownership while it remains valid. */
    data class Watchdog(
        val ttl: Duration = Duration.ofSeconds(30),
        val renewalInterval: Duration = ttl.dividedBy(3),
        val maxLifetime: Duration = Duration.ofHours(24),
    ): LeasePolicy {
        init {
            validateWatchdogLease(ttl, renewalInterval, maxLifetime)
        }

        private fun readResolve(): Any = restoreLockSerializedValue("LeasePolicy.Watchdog") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Opaque ownership capability returned by an exclusive lock acquisition.
 *
 * One handle represents one request-bound hold. Raw object, owner, request, generation, and lease data are redacted.
 */
data class LockHandle(
    val objectFingerprint: String,
    val ownerId: LockOwnerId,
    val generation: LockGeneration,
    val requestId: LockRequestId,
    val leasePolicy: LeasePolicy,
    val kind: LockKind,
): Serializable {
    init {
        validateObjectFingerprint(objectFingerprint)
    }

    override fun toString(): String = "LockHandle(kind=$kind, identity=<redacted>, lease=<redacted>)"

    private fun readResolve(): Any = restoreLockSerializedValue("LockHandle") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** A fenced-lock handle that adds a monotonic epoch and fencing token to [lock]. */
data class FencedLockHandle(
    val lock: LockHandle,
    val epoch: Long,
    val fencingToken: Long,
): Serializable {
    init {
        require(lock.kind == LockKind.FENCED) { "Fenced lock handle requires FENCED kind." }
        validateLuaExactPositive(epoch, "Fenced lock epoch")
        validateLuaExactPositive(fencingToken, "Fencing token")
    }

    override fun toString(): String = "FencedLockHandle(lock=$lock, fence=<redacted>)"

    private fun readResolve(): Any = restoreLockSerializedValue("FencedLockHandle") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** A read-lock ownership handle. */
data class ReadLockHandle(
    val lock: LockHandle,
): Serializable {
    init {
        require(lock.kind == LockKind.READ) { "Read lock handle requires READ kind." }
    }

    private fun readResolve(): Any = restoreLockSerializedValue("ReadLockHandle") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** A write-lock ownership handle. */
data class WriteLockHandle(
    val lock: LockHandle,
): Serializable {
    init {
        require(lock.kind == LockKind.WRITE) { "Write lock handle requires WRITE kind." }
    }

    private fun readResolve(): Any = restoreLockSerializedValue("WriteLockHandle") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** An all-or-nothing multi-lock ownership handle. */
data class MultiLockHandle(
    val lock: LockHandle,
    val constituentCount: Int,
): Serializable {
    init {
        require(lock.kind == LockKind.MULTI) { "Multi-lock handle requires MULTI kind." }
        require(constituentCount in 1..MAX_MULTI_LOCK_KEYS) {
            "Multi-lock constituent count must be between 1 and $MAX_MULTI_LOCK_KEYS."
        }
    }

    private fun readResolve(): Any = restoreLockSerializedValue("MultiLockHandle") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Identifies the observable lifecycle state of one fair-lock waiter. */
enum class FairWaiterStatus {
    QUEUED,
    REMOVED,
    ADMITTED,
}

/** Describes one fair-lock waiter without exposing its owner or request identity. */
data class FairWaiterState(
    val status: FairWaiterStatus,
    val enqueueSequence: Long,
    val remainingWaitMillis: Long,
): Serializable {
    init {
        validateLuaExactPositive(enqueueSequence, "Fair waiter sequence")
        require(remainingWaitMillis >= 0L) { "Remaining wait must not be negative." }
    }

    override fun toString(): String =
        "FairWaiterState(status=$status, sequence=<redacted>, deadline=<redacted>)"

    private fun readResolve(): Any = restoreLockSerializedValue("FairWaiterState") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal const val MAX_LUA_EXACT_INTEGER: Long = 9_007_199_254_740_991L
internal const val MAX_MULTI_LOCK_KEYS: Int = 32

private const val MAX_IDENTITY_UTF8_BYTES = 256
private const val RANDOM_BASE58_LENGTH = 22

private fun validateOpaqueIdentity(value: String, label: String) {
    val byteCount = value.toByteArray(StandardCharsets.UTF_8).size
    require(value.isNotBlank() && byteCount in 1..MAX_IDENTITY_UTF8_BYTES) {
        "$label must be non-blank and contain 1..$MAX_IDENTITY_UTF8_BYTES UTF-8 bytes."
    }
}

private fun validateObjectFingerprint(value: String) {
    val byteCount = value.toByteArray(StandardCharsets.UTF_8).size
    require(value.isNotBlank() && byteCount in 1..MAX_IDENTITY_UTF8_BYTES) {
        "Lock object fingerprint must be non-blank and contain 1..$MAX_IDENTITY_UTF8_BYTES UTF-8 bytes."
    }
}

internal fun validateLuaExactPositive(value: Long, label: String) {
    require(value in 1..MAX_LUA_EXACT_INTEGER) {
        "$label must be a positive exact Redis Lua integer."
    }
}

internal fun <T> restoreLockSerializedValue(
    typeName: String,
    factory: () -> T,
): T = try {
    factory()
} catch (_: IllegalArgumentException) {
    invalidSerializedLockValue(typeName)
} catch (_: NullPointerException) {
    invalidSerializedLockValue(typeName)
} catch (_: ArithmeticException) {
    invalidSerializedLockValue(typeName)
}

private fun invalidSerializedLockValue(typeName: String): Nothing =
    throw InvalidObjectException("Invalid serialized $typeName.")
