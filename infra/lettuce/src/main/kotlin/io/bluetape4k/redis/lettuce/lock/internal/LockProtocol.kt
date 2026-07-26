package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationProtocol
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationProtocolException
import io.bluetape4k.redis.lettuce.lock.LeasePolicy
import io.bluetape4k.redis.lettuce.lock.LockAcquireResult
import io.bluetape4k.redis.lettuce.lock.LockConfig
import io.bluetape4k.redis.lettuce.lock.LockGeneration
import io.bluetape4k.redis.lettuce.lock.LockHandle
import io.bluetape4k.redis.lettuce.lock.LockInspectResult
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailure
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailureKind
import io.bluetape4k.redis.lettuce.lock.LockKind
import io.bluetape4k.redis.lettuce.lock.LockMutationResult
import io.bluetape4k.redis.lettuce.lock.LockOwnerId
import io.bluetape4k.redis.lettuce.lock.LockReconcileResult
import io.bluetape4k.redis.lettuce.lock.LockRequestId
import io.bluetape4k.redis.lettuce.lock.toRedisMillisCeil
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.codec.RedisCodec
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Duration

internal enum class DistributedLockOperation(
    val wireValue: String,
) {
    ACQUIRE("ACQUIRE"),
    INSPECT("INSPECT"),
    RECONCILE("RECONCILE"),
    RENEW("RENEW"),
    RELEASE("RELEASE"),
}

internal data class DistributedLockKeys(
    val state: String,
    val generation: String,
    val holds: String,
    val terminal: String,
    val fingerprint: String,
) {
    val all: Array<String> = arrayOf(state, generation, holds, terminal)
}

internal fun deriveDistributedLockKeys(
    name: String,
    config: LockConfig,
    codec: RedisCodec<String, String>,
): DistributedLockKeys {
    val resource = config.validateResourceName(name)
    val hashTag = config.hashTag ?: resource
    val prefix = "${config.namespace}:{$hashTag}:lock:$resource"
    val state = config.validateDerivedKey("$prefix:state")
    val generation = config.validateDerivedKey("$prefix:generation")
    val holds = config.validateDerivedKey("$prefix:holds")
    val terminal = config.validateDerivedKey("$prefix:terminal")
    val keys = listOf(state, generation, holds, terminal)
    require(keys.map { SlotHash.getSlot(codec.encodeKey(it)) }.toSet().size == 1) {
        "Derived distributed lock keys must share one Redis Cluster slot."
    }
    val digest = MessageDigest.getInstance("SHA-256").digest(codec.encodeKey(state).toByteArray())
    val fingerprint = digest.take(FINGERPRINT_BYTES).joinToString("") { byte -> "%02x".format(byte) }
    return DistributedLockKeys(state, generation, holds, terminal, fingerprint)
}

internal data class EncodedLeasePolicy(
    val wireValue: String,
    val ttlMillis: Long,
)

internal fun encodeLeasePolicy(policy: LeasePolicy): EncodedLeasePolicy =
    when (policy) {
        is LeasePolicy.Fixed -> {
            val ttl = policy.leaseTime.toRedisMillisCeil()
            EncodedLeasePolicy("F:$ttl", ttl)
        }
        is LeasePolicy.Watchdog -> {
            val ttl = policy.ttl.toRedisMillisCeil()
            val interval = policy.renewalInterval.toRedisMillisCeil()
            val lifetime = policy.maxLifetime.toRedisMillisCeil()
            EncodedLeasePolicy("W:$ttl:$interval:$lifetime", ttl)
        }
    }

internal fun decodeLeasePolicy(value: String): LeasePolicy {
    val fields = value.split(':')
    return when {
        fields.size == 2 && fields[0] == "F" ->
            LeasePolicy.Fixed(Duration.ofMillis(fields[1].canonicalPositiveLong()))
        fields.size == 4 && fields[0] == "W" ->
            LeasePolicy.Watchdog(
                ttl = Duration.ofMillis(fields[1].canonicalPositiveLong()),
                renewalInterval = Duration.ofMillis(fields[2].canonicalPositiveLong()),
                maxLifetime = Duration.ofMillis(fields[3].canonicalPositiveLong()),
            )
        else -> malformedReply()
    }
}

internal fun acquireArgs(
    ownerId: LockOwnerId,
    requestId: LockRequestId,
    leasePolicy: LeasePolicy,
    maxReentrantHolds: Int,
): List<String> {
    val encoded = encodeLeasePolicy(leasePolicy)
    return listOf(
        ownerId.value,
        requestId.value,
        encoded.wireValue,
        encoded.ttlMillis.toString(),
        maxReentrantHolds.toString(),
        TERMINAL_TTL_MILLIS.toString(),
    )
}

internal fun handleArgs(handle: LockHandle): List<String> =
    listOf(handle.ownerId.value, handle.requestId.value, handle.generation.value.toString())

internal fun renewArgs(handle: LockHandle, extension: Duration): List<String> =
    handleArgs(handle) + extension.toRedisMillisCeil().toString()

internal fun reconcileArgs(ownerId: LockOwnerId, requestId: LockRequestId): List<String> =
    listOf(ownerId.value, requestId.value)

internal fun decodeAcquire(
    raw: Any?,
    keys: DistributedLockKeys,
    ownerId: LockOwnerId,
    requestId: LockRequestId,
): LockAcquireResult<LockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "ACQUIRED" to 5,
            "REPLAY" to 5,
            "REENTERED" to 5,
            "CONTENDED" to 2,
            "CAPACITY" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "ACQUIRED", "REPLAY", "REENTERED" -> {
            val generation = LockGeneration(frame.positiveLong(0))
            val holdCount = frame.positiveInt(1)
            val ttl = frame.nonNegativeLong(2)
            val policy = decodeLeasePolicy(frame.field(3))
            val handle = lockHandle(keys, ownerId, requestId, generation, policy)
            if (frame.tag == "REENTERED" || frame.tag == "REPLAY" && holdCount > 1) {
                LockAcquireResult.Reentered(handle, holdCount)
            } else {
                LockAcquireResult.Acquired(handle)
            }
        }
        "CONTENDED" -> LockAcquireResult.Contended(frame.nonNegativeLong(0))
        "CAPACITY" -> LockAcquireResult.CapacityExceeded
        "INTEGRITY" -> LockAcquireResult.IntegrityFailure(INVALID_STATE)
        else -> malformedReply()
    }
}

internal fun decodeInspect(
    raw: Any?,
    keys: DistributedLockKeys,
    handle: LockHandle,
): LockInspectResult<LockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "OWNED" to 5,
            "RELEASED" to 1,
            "EXPIRED" to 1,
            "STALE" to 1,
            "LOST" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "OWNED" -> {
            val generation = LockGeneration(frame.positiveLong(0))
            val holdCount = frame.positiveInt(1)
            val ttl = frame.nonNegativeLong(2)
            val policy = decodeLeasePolicy(frame.field(3))
            LockInspectResult.Owned(
                lockHandle(keys, handle.ownerId, handle.requestId, generation, policy),
                holdCount,
                ttl,
            )
        }
        "RELEASED" -> LockInspectResult.Released
        "EXPIRED" -> LockInspectResult.Expired
        "STALE" -> LockInspectResult.StaleGeneration
        "LOST" -> LockInspectResult.OwnershipLost
        "INTEGRITY" -> LockInspectResult.IntegrityFailure(INVALID_STATE)
        else -> malformedReply()
    }
}

internal fun decodeReconcile(
    raw: Any?,
    keys: DistributedLockKeys,
    ownerId: LockOwnerId,
    requestId: LockRequestId,
): LockReconcileResult<LockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "OWNED" to 5,
            "RELEASED" to 1,
            "NOT_FOUND" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "OWNED" -> {
            val generation = LockGeneration(frame.positiveLong(0))
            val holdCount = frame.positiveInt(1)
            val ttl = frame.nonNegativeLong(2)
            val policy = decodeLeasePolicy(frame.field(3))
            LockReconcileResult.Owned(
                lockHandle(keys, ownerId, requestId, generation, policy),
                holdCount,
                ttl,
            )
        }
        "RELEASED" -> LockReconcileResult.Released
        "NOT_FOUND" -> LockReconcileResult.NotFound
        "INTEGRITY" -> LockReconcileResult.IntegrityFailure(INVALID_STATE)
        else -> malformedReply()
    }
}

internal fun decodeRenew(
    raw: Any?,
    handle: LockHandle,
): LockMutationResult<LockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "RENEWED" to 2,
            "ALREADY_RELEASED" to 1,
            "EXPIRED" to 1,
            "STALE" to 1,
            "LOST" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "RENEWED" -> LockMutationResult.Renewed(handle, frame.nonNegativeLong(0))
        "ALREADY_RELEASED" -> LockMutationResult.AlreadyReleased
        "EXPIRED" -> LockMutationResult.Expired
        "STALE" -> LockMutationResult.StaleGeneration
        "LOST" -> LockMutationResult.OwnershipLost
        "INTEGRITY" -> LockMutationResult.IntegrityFailure(INVALID_STATE)
        else -> malformedReply()
    }
}

internal fun decodeRelease(raw: Any?): LockMutationResult<LockHandle> {
    val frame = CoordinationProtocol.decode(
        raw,
        mapOf(
            "RELEASED" to 2,
            "ALREADY_RELEASED" to 1,
            "EXPIRED" to 1,
            "STALE" to 1,
            "LOST" to 1,
            "INTEGRITY" to 1,
        ),
    )
    return when (frame.tag) {
        "RELEASED" -> LockMutationResult.Released(frame.nonNegativeInt(0))
        "ALREADY_RELEASED" -> LockMutationResult.AlreadyReleased
        "EXPIRED" -> LockMutationResult.Expired
        "STALE" -> LockMutationResult.StaleGeneration
        "LOST" -> LockMutationResult.OwnershipLost
        "INTEGRITY" -> LockMutationResult.IntegrityFailure(INVALID_STATE)
        else -> malformedReply()
    }
}

internal fun malformedIntegrityFailure(): LockIntegrityFailure =
    LockIntegrityFailure(LockIntegrityFailureKind.MALFORMED_REPLY)

private fun lockHandle(
    keys: DistributedLockKeys,
    ownerId: LockOwnerId,
    requestId: LockRequestId,
    generation: LockGeneration,
    leasePolicy: LeasePolicy,
): LockHandle =
    LockHandle(
        objectFingerprint = keys.fingerprint,
        ownerId = ownerId,
        generation = generation,
        requestId = requestId,
        leasePolicy = leasePolicy,
        kind = LockKind.DISTRIBUTED,
    )

private fun io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFrame.positiveLong(index: Int): Long =
    nonNegativeLong(index).also { require(it > 0L) { "response number must be positive" } }

private fun io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFrame.positiveInt(index: Int): Int =
    positiveLong(index).also { require(it <= Int.MAX_VALUE) }.toInt()

private fun io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFrame.nonNegativeInt(index: Int): Int =
    nonNegativeLong(index).also { require(it <= Int.MAX_VALUE) }.toInt()

private fun String.canonicalPositiveLong(): Long {
    if (!matches(POSITIVE_DECIMAL)) {
        malformedReply()
    }
    return toLongOrNull()?.takeIf { it > 0L } ?: malformedReply()
}

private fun ByteBuffer.toByteArray(): ByteArray =
    duplicate().let { copy -> ByteArray(copy.remaining()).also(copy::get) }

private fun malformedReply(): Nothing =
    throw CoordinationProtocolException(
        io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFailureClassification.INTEGRITY,
        "distributed lock response is malformed",
    )

private val INVALID_STATE = LockIntegrityFailure(LockIntegrityFailureKind.INVALID_STATE)
private val POSITIVE_DECIMAL = Regex("[1-9][0-9]{0,18}")
private const val FINGERPRINT_BYTES = 8
private const val TERMINAL_TTL_MILLIS = 7L * 24L * 60L * 60L * 1_000L
