package io.bluetape4k.redis.lettuce.lease

import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.codec.RedisCodec
import java.time.Duration

internal data class ValidatedLeaseInput(
    val keys: List<String>,
    val ttlMillis: Long?,
)

internal fun validateLeaseInput(
    keys: Collection<String>,
    ownerToken: String,
    ttl: Duration?,
    config: LettuceMultiKeyLeaseConfig,
    codec: RedisCodec<String, String>,
): ValidatedLeaseInput {
    require(ownerToken.isNotBlank()) { "ownerToken must not be blank." }
    val snapshot = snapshotKeys(keys, config.maxKeys)
    requireSameSlot(snapshot, codec)
    return ValidatedLeaseInput(snapshot, ttl?.requirePositiveMillis())
}

internal fun decodeAcquire(vector: List<Long>): MultiKeyAcquireResult {
    val decoded = decodeVector(vector, MultiKeyLeaseOperation.ACQUIRE, ttlStatus = 11)
    return when (decoded.status) {
        10 -> decoded.requireShape(decoded.allMissing) { MultiKeyAcquireResult.Acquired }
        11 -> decoded.requireShape(decoded.allOwned) { MultiKeyAcquireResult.AlreadyOwned(decoded.minimumPttlMillis) }
        12 -> decoded.requireShape(decoded.partialWithoutMismatch) {
            MultiKeyAcquireResult.PartialOwnership(decoded.counts)
        }
        13 -> decoded.requireShape(decoded.hasMismatch) { MultiKeyAcquireResult.Conflicted(decoded.counts) }
        90 -> decoded.throwIntegrity()
        else -> invalidVector()
    }
}

internal fun decodeInspect(vector: List<Long>): MultiKeyInspectResult {
    val decoded = decodeVector(vector, MultiKeyLeaseOperation.INSPECT, ttlStatus = 20)
    return when (decoded.status) {
        20 -> decoded.requireShape(decoded.allOwned) { MultiKeyInspectResult.Owned(decoded.minimumPttlMillis) }
        21 -> decoded.requireShape(decoded.allMissing) { MultiKeyInspectResult.Lost }
        22 -> decoded.requireShape(decoded.partialWithoutMismatch) {
            MultiKeyInspectResult.PartialOwnership(decoded.counts)
        }
        23 -> decoded.requireShape(decoded.hasMismatch) { MultiKeyInspectResult.Conflicted(decoded.counts) }
        90 -> decoded.throwIntegrity()
        else -> invalidVector()
    }
}

internal fun decodeRenew(vector: List<Long>): MultiKeyRenewResult {
    val decoded = decodeVector(vector, MultiKeyLeaseOperation.RENEW)
    return when (decoded.status) {
        40 -> decoded.requireShape(decoded.allOwned) { MultiKeyRenewResult.Renewed }
        41 -> decoded.requireShape(decoded.partialWithoutMismatch) { MultiKeyRenewResult.PartialLoss(decoded.counts) }
        42 -> decoded.requireShape(decoded.allMissing) { MultiKeyRenewResult.Lost }
        43 -> decoded.requireShape(decoded.hasMismatch) { MultiKeyRenewResult.OwnershipMismatch(decoded.counts) }
        90 -> decoded.throwIntegrity()
        else -> invalidVector()
    }
}

internal fun decodeRelease(vector: List<Long>): MultiKeyReleaseResult {
    val decoded = decodeVector(vector, MultiKeyLeaseOperation.RELEASE)
    return when (decoded.status) {
        50 -> decoded.requireShape(decoded.allOwned) { MultiKeyReleaseResult.Released }
        51 -> decoded.requireShape(decoded.partialWithoutMismatch) { MultiKeyReleaseResult.PartialRelease(decoded.counts) }
        52 -> decoded.requireShape(decoded.allMissing) { MultiKeyReleaseResult.Lost }
        53 -> decoded.requireShape(decoded.hasMismatch) { MultiKeyReleaseResult.OwnershipMismatch(decoded.counts) }
        else -> invalidVector()
    }
}

private fun snapshotKeys(keys: Collection<String>, maxKeys: Int): List<String> {
    val snapshot = ArrayList<String>(minOf(maxKeys, 32))
    val distinctKeys = HashSet<String>(minOf(maxKeys, 32))
    val iterator = keys.iterator()
    require(iterator.hasNext()) { "keys must not be empty." }
    while (iterator.hasNext()) {
        require(snapshot.size < maxKeys) { "keys exceed the configured limit." }
        val key = iterator.next()
        require(key.isNotBlank()) { "keys must not contain blank values." }
        require(distinctKeys.add(key)) { "keys must not contain duplicates." }
        snapshot += key
    }
    return snapshot
}

private fun requireSameSlot(keys: List<String>, codec: RedisCodec<String, String>) {
    val slots = HashSet<Int>()
    keys.forEach { key -> slots += SlotHash.getSlot(codec.encodeKey(key)) }
    if (slots.size != 1) {
        throw MultiKeyLeaseCrossSlotException(slots.size)
    }
}

private fun Duration.requirePositiveMillis(): Long {
    val millis = toMillis()
    require(millis > 0) { "ttl must be at least one millisecond." }
    return millis
}

private data class DecodedVector(
    val status: Int,
    val operation: MultiKeyLeaseOperation,
    val counts: MultiKeyLeaseCounts,
    val invalidTtl: Int,
    val minimumPttlMillis: Long,
) {
    val allOwned: Boolean get() = counts.ownedKeys == counts.requestedKeys
    val allMissing: Boolean get() = counts.missingKeys == counts.requestedKeys
    val partialWithoutMismatch: Boolean
        get() = counts.ownedKeys > 0 && counts.missingKeys > 0 && counts.mismatchedKeys == 0
    val hasMismatch: Boolean get() = counts.mismatchedKeys > 0

    inline fun <T> requireShape(condition: Boolean, result: () -> T): T {
        if (!condition || invalidTtl != 0) invalidVector()
        return result()
    }

    fun throwIntegrity(): Nothing {
        if (invalidTtl <= 0) invalidVector()
        throw MultiKeyLeaseIntegrityException(operation, counts.requestedKeys, invalidTtl)
    }
}

private fun decodeVector(
    vector: List<Long>,
    operation: MultiKeyLeaseOperation,
    ttlStatus: Int? = null,
): DecodedVector {
    if (vector.size != VECTOR_SIZE) invalidVector()
    val status = vector[0].toIntExact()
    val requested = vector[1].toCount()
    val owned = vector[2].toCount()
    val missing = vector[3].toCount()
    val mismatched = vector[4].toCount()
    val invalidTtl = vector[5].toCount()
    val minimumPttl = vector[6]

    if (requested <= 0) invalidVector()
    val observed = owned.toLong() + missing.toLong() + mismatched.toLong()
    if (observed != requested.toLong() || invalidTtl > owned) invalidVector()
    if (status == ttlStatus) {
        if (minimumPttl < 1) invalidVector()
    } else if (minimumPttl != NO_PTTL) {
        invalidVector()
    }

    return DecodedVector(
        status,
        operation,
        MultiKeyLeaseCounts(requested, owned, missing, mismatched),
        invalidTtl,
        minimumPttl,
    )
}

private fun Long.toCount(): Int {
    if (this < 0 || this > Int.MAX_VALUE) invalidVector()
    return toInt()
}

private fun Long.toIntExact(): Int {
    if (this < Int.MIN_VALUE || this > Int.MAX_VALUE) invalidVector()
    return toInt()
}

private fun invalidVector(): Nothing = throw IllegalStateException("Malformed multi-key lease response.")

private const val VECTOR_SIZE = 7
private const val NO_PTTL = -1L
