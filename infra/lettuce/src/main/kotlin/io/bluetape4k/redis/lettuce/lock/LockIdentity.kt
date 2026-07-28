package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.codec.Base58
import java.io.InvalidObjectException
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * lock 호출자의 논리적 reentrancy domain을 식별합니다.
 *
 * 이 식별자는 인증 credential이 아닙니다. 하나의 reentrant ownership domain을 의도적으로 공유하는 호출에만
 * 재사용하십시오. 원본 값은 진단 출력에서 제외됩니다.
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

    /** 논리적 owner 식별자를 생성합니다. */
    companion object {
        private const val serialVersionUID: Long = 1L

        /** 새 논리적 owner를 위한 암호학적으로 강한 식별자를 생성합니다. */
        @JvmStatic
        fun random(): LockOwnerId = LockOwnerId(Base58.randomString(RANDOM_BASE58_LENGTH))

        /** 호출자가 관리하는 논리적 owner 값에서 식별자를 생성합니다. */
        @JvmStatic
        fun from(value: String): LockOwnerId = LockOwnerId(value)
    }
}

/**
 * dispatch, retry, reconciliation 전반에서 하나의 논리적 lock 작업을 식별합니다.
 *
 * 새 작업은 새 request 식별자를 사용합니다. 모호한 작업은 terminal state가 조정될 때까지 같은 식별자를
 * 유지해야 합니다. 원본 값은 진단 출력에서 제외됩니다.
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

    /** 논리적 request 식별자를 생성합니다. */
    companion object {
        private const val serialVersionUID: Long = 1L

        /** 새 논리적 작업을 위한 암호학적으로 강한 식별자를 생성합니다. */
        @JvmStatic
        fun random(): LockRequestId = LockRequestId(Base58.randomString(RANDOM_BASE58_LENGTH))

        /** 호출자가 관리하는 논리적 작업 값에서 식별자를 생성합니다. */
        @JvmStatic
        fun from(value: String): LockRequestId = LockRequestId(value)
    }
}

/**
 * 단조 증가하는 Redis ownership generation을 표현합니다.
 *
 * generation은 양수이며, 하나의 lock authority domain 안에서는 reset되지 않고, 진단 출력에서 제외됩니다.
 *
 * @property value Redis ownership generation의 원본 숫자 값입니다. 양수여야 합니다.
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

/** handle 또는 observation과 연결된 구체적인 lock algorithm을 식별합니다. */
enum class LockKind {
    DISTRIBUTED,
    FAIR,
    FENCED,
    READ,
    WRITE,
    SPIN,
    MULTI,
}

/** lock이 fixed lease를 쓰는지 bounded watchdog renewal policy를 쓰는지 설명합니다. */
sealed interface LeasePolicy: Serializable {

    /** 하나의 고정 Redis TTL을 사용하며 automatic renewal을 시작하지 않습니다. */
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
    val hasSafeProtocolCharacters = value.none { it == '|' || it.isISOControl() }
    require(value.isNotBlank() && byteCount in 1..MAX_IDENTITY_UTF8_BYTES && hasSafeProtocolCharacters) {
        "$label must be non-blank, contain 1..$MAX_IDENTITY_UTF8_BYTES UTF-8 bytes, " +
                "and exclude protocol delimiters and control characters."
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
