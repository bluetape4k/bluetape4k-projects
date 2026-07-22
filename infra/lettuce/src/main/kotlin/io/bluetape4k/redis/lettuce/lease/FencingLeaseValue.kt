package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.codec.Base58
import io.bluetape4k.support.requirePositiveNumber
import java.io.InvalidObjectException
import java.io.Serializable
import java.nio.charset.StandardCharsets

/**
 * Identifies one Redis fencing-token ordering domain.
 *
 * Tokens are comparable only within the same [namespace] and [resourceName]. The [epoch] must be allocated by a
 * durable external authority and must never be lowered during rollback or recovery.
 *
 * @property namespace stable namespace used to derive the Redis Cluster hash tag
 * @property resourceName stable logical resource name used to derive the Redis Cluster hash tag
 * @property epoch positive generation allocated for this ordering domain
 */
data class LettuceFencingLeaseConfig(
    val namespace: String,
    val resourceName: String,
    val epoch: Long,
): Serializable {
    init {
        require(SAFE_COMPONENT.matches(namespace)) {
            "namespace must contain 1..128 ASCII letters, digits, dots, underscores, or hyphens."
        }
        require(SAFE_COMPONENT.matches(resourceName)) {
            "resourceName must contain 1..128 ASCII letters, digits, dots, underscores, or hyphens."
        }
        epoch.requirePositiveNumber("epoch")
    }

    private fun readResolve(): Any = restoreFencingSerializedValue("LettuceFencingLeaseConfig") {
        LettuceFencingLeaseConfig(namespace, resourceName, epoch)
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
        private val SAFE_COMPONENT = Regex("[A-Za-z0-9._-]{1,128}")
    }
}

/**
 * Opaque identifier for one logical fencing-lease acquisition attempt.
 *
 * Reuse an owner ID only when reconciling an ambiguous response from the same logical attempt. The raw value is
 * intentionally excluded from [toString].
 */
class FencingOwnerId private constructor(
    internal val value: String,
): Serializable {
    init {
        val byteCount = value.toByteArray(StandardCharsets.UTF_8).size
        require(value.isNotBlank() && byteCount in 1..MAX_UTF8_BYTES) {
            "Fencing owner ID must be non-blank and contain 1..256 UTF-8 bytes."
        }
    }

    override fun equals(other: Any?): Boolean = other is FencingOwnerId && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "FencingOwnerId(<redacted>)"

    private fun readResolve(): Any = restoreFencingSerializedValue("FencingOwnerId") {
        FencingOwnerId(value)
    }

    companion object {
        private const val serialVersionUID: Long = 1L
        private const val MAX_UTF8_BYTES = 256
        private const val RANDOM_LENGTH = 22

        /** Creates a cryptographically strong random owner ID for a new logical acquisition attempt. */
        fun random(): FencingOwnerId = FencingOwnerId(Base58.randomString(RANDOM_LENGTH))

        /**
         * Creates an owner ID from an externally managed acquisition-attempt identifier.
         *
         * The caller remains responsible for global collision resistance, secrecy, and safe persistence.
         */
        fun from(value: String): FencingOwnerId = FencingOwnerId(value)
    }
}

/**
 * Monotonic fencing token ordered lexicographically by [epoch] and then [sequence].
 *
 * The numeric tuple is intentionally excluded from [toString]. Downstream systems must persist the tuple together
 * with stable resource identity and reject tokens that are not strictly greater than the last accepted token.
 *
 * @property epoch positive ordering-domain generation
 * @property sequence positive sequence allocated within [epoch]
 */
data class FencingToken(
    val epoch: Long,
    val sequence: Long,
): Comparable<FencingToken>, Serializable {
    init {
        epoch.requirePositiveNumber("epoch")
        sequence.requirePositiveNumber("sequence")
    }

    override fun compareTo(other: FencingToken): Int {
        val epochComparison = epoch.compareTo(other.epoch)
        return if (epochComparison != 0) epochComparison else sequence.compareTo(other.sequence)
    }

    override fun toString(): String = "FencingToken(<redacted>)"

    private fun readResolve(): Any = restoreFencingSerializedValue("FencingToken") {
        FencingToken(epoch, sequence)
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal fun <T> restoreFencingSerializedValue(
    typeName: String,
    factory: () -> T,
): T = try {
    factory()
} catch (_: IllegalArgumentException) {
    invalidSerializedFencingValue(typeName)
} catch (_: NullPointerException) {
    invalidSerializedFencingValue(typeName)
}

private fun invalidSerializedFencingValue(typeName: String): Nothing =
    throw InvalidObjectException("Invalid serialized $typeName.")
