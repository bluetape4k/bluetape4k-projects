package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.RedisException
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.codec.RedisCodec
import java.security.MessageDigest
import java.time.Duration
import java.util.Collections
import java.util.HexFormat
import java.util.IdentityHashMap
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

internal data class FencingLeaseKeys(
    val lease: String,
    val counter: String,
)

internal enum class FencingLeaseOperation {
    BOOTSTRAP,
    ACQUIRE,
    INSPECT,
    RENEW,
    RELEASE,
}

internal class FencingLeaseProtocolException: IllegalStateException(
    "Malformed fencing lease response.",
) {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal object FencingLeaseSupportLogger: KLogging() {
    fun backendFailure(
        operation: FencingLeaseOperation,
        failure: FencingLeaseBackendFailure,
        exceptionClassName: String,
        domainFingerprint: String?,
    ) {
        val domain = domainFingerprint ?: "unavailable"
        log.warn {
            "Fencing lease backend failure operation=$operation kind=${failure.kind} " +
                "exception=$exceptionClassName domain=$domain"
        }
    }
}

internal fun deriveFencingLeaseKeys(
    config: LettuceFencingLeaseConfig,
    codec: RedisCodec<String, String>,
): FencingLeaseKeys {
    val tag = "${config.namespace}:${config.resourceName}"
    val prefix = "fence:{$tag}:${config.epoch}"
    val keys = FencingLeaseKeys("$prefix:lease", "$prefix:counter")
    require(SlotHash.getSlot(codec.encodeKey(keys.lease)) == SlotHash.getSlot(codec.encodeKey(keys.counter))) {
        "Derived fencing lease keys must share one Redis Cluster slot."
    }
    return keys
}

internal fun requireCanonicalFencingDecimal(value: String): String {
    val canonical = value == "0" ||
        value.firstOrNull() in '1'..'9' && value.all { character -> character in '0'..'9' }
    require(canonical) { "Invalid decimal value." }
    require(value.length <= MAX_LONG_DECIMAL.length) { "Decimal value is out of range." }
    require(compareCanonicalFencingDecimalsWithoutValidation(value, MAX_LONG_DECIMAL) <= 0) {
        "Decimal value is out of range."
    }
    return value
}

internal fun compareCanonicalFencingDecimals(left: String, right: String): Int {
    requireCanonicalFencingDecimal(left)
    requireCanonicalFencingDecimal(right)
    return compareCanonicalFencingDecimalsWithoutValidation(left, right)
}

internal fun parseCanonicalFencingLong(value: String): Long = requireCanonicalFencingDecimal(value).toLong()

internal fun Duration.requireFencingLeaseMillis(): Long {
    val millis = try {
        toMillis()
    } catch (_: ArithmeticException) {
        throw IllegalArgumentException(INVALID_LEASE_TIME_MESSAGE)
    }
    require(millis > 0 && this == Duration.ofMillis(millis)) { INVALID_LEASE_TIME_MESSAGE }
    return millis
}

internal fun requireFencingTokenEpoch(
    config: LettuceFencingLeaseConfig,
    token: FencingToken,
): FencingToken {
    require(token.epoch == config.epoch) {
        "Fencing token epoch must match the configured ordering domain."
    }
    return token
}

internal fun Throwable.unwrapFencingCompletionCause(): Throwable {
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    var current = this
    repeat(MAX_COMPLETION_DEPTH) {
        if (current is CancellationException) {
            throw current
        }
        if (!seen.add(current)) {
            return current
        }
        val next = when (current) {
            is CompletionException,
            is ExecutionException,
            -> current.cause
            else -> null
        }
        if (next == null || next === current) {
            return current
        }
        current = next
    }
    if (current is CancellationException) {
        throw current
    }
    return current
}

internal fun classifyFencingBackendFailure(
    operation: FencingLeaseOperation,
    error: Throwable,
    domainFingerprint: String? = null,
): FencingLeaseBackendFailure {
    val cause = error.unwrapFencingCompletionCause()
    val kind = when (cause) {
        is RedisConnectionException -> FencingBackendFailureKind.CONNECTION
        is RedisCommandTimeoutException,
        is TimeoutException,
        -> FencingBackendFailureKind.TIMEOUT
        is RedisException -> FencingBackendFailureKind.COMMAND
        else -> throw cause
    }
    return FencingLeaseBackendFailure(kind).also { failure ->
        FencingLeaseSupportLogger.backendFailure(
            operation,
            failure,
            cause.javaClass.name,
            domainFingerprint,
        )
    }
}

internal fun LettuceFencingLeaseConfig.domainFingerprint(): String {
    val source = "$namespace\u0000$resourceName\u0000$epoch".toByteArray(Charsets.UTF_8)
    val digest = MessageDigest.getInstance("SHA-256").digest(source)
    return HexFormat.of().formatHex(digest, 0, DOMAIN_FINGERPRINT_BYTES)
}

internal fun decodeFencingBootstrap(frame: List<String>): FencingBootstrapResult {
    val decoded = decodeFencingFrame(frame)
    decoded.integrityFailureOrNull()?.let { failure ->
        return FencingBootstrapResult.IntegrityFailure(failure)
    }
    decoded.requireUnusedValues()
    return when (decoded.status) {
        "INITIALIZED" -> FencingBootstrapResult.Initialized
        "ALREADY_INITIALIZED" -> FencingBootstrapResult.AlreadyInitialized
        else -> malformedFencingReply()
    }
}

internal fun decodeFencingAcquire(frame: List<String>): FencingAcquireResult {
    val decoded = decodeFencingFrame(frame)
    decoded.integrityFailureOrNull()?.let { failure ->
        return FencingAcquireResult.IntegrityFailure(failure)
    }
    return when (decoded.status) {
        "ACQUIRED" -> {
            decoded.requireTtlSentinel()
            FencingAcquireResult.Acquired(decoded.token())
        }
        "ALREADY_OWNED" -> FencingAcquireResult.AlreadyOwned(decoded.token(), decoded.ttl())
        "CONTENDED" -> {
            decoded.requireUnusedToken()
            FencingAcquireResult.Contended(decoded.ttl())
        }
        "COUNTER_UNAVAILABLE" -> decoded.withUnusedValues { FencingAcquireResult.CounterUnavailable }
        "SEQUENCE_EXHAUSTED" -> decoded.withUnusedValues { FencingAcquireResult.SequenceExhausted }
        else -> malformedFencingReply()
    }
}

internal fun decodeFencingInspect(frame: List<String>): FencingInspectResult {
    val decoded = decodeFencingFrame(frame)
    decoded.integrityFailureOrNull()?.let { failure ->
        return FencingInspectResult.IntegrityFailure(failure)
    }
    return when (decoded.status) {
        "OWNED" -> FencingInspectResult.Owned(decoded.token(), decoded.ttl())
        "LOST" -> decoded.withUnusedValues { FencingInspectResult.Lost }
        "CONTENDED" -> {
            decoded.requireUnusedToken()
            FencingInspectResult.Contended(decoded.ttl())
        }
        else -> malformedFencingReply()
    }
}

internal fun decodeFencingRenew(frame: List<String>): FencingRenewResult {
    val decoded = decodeFencingFrame(frame)
    decoded.integrityFailureOrNull()?.let { failure ->
        return FencingRenewResult.IntegrityFailure(failure)
    }
    decoded.requireUnusedValues()
    return when (decoded.status) {
        "RENEWED" -> FencingRenewResult.Renewed
        "LOST" -> FencingRenewResult.Lost
        "OWNERSHIP_MISMATCH" -> FencingRenewResult.OwnershipMismatch
        else -> malformedFencingReply()
    }
}

internal fun decodeFencingRelease(frame: List<String>): FencingReleaseResult {
    val decoded = decodeFencingFrame(frame)
    decoded.integrityFailureOrNull()?.let { failure ->
        return FencingReleaseResult.IntegrityFailure(failure)
    }
    decoded.requireUnusedValues()
    return when (decoded.status) {
        "RELEASED" -> FencingReleaseResult.Released
        "LOST" -> FencingReleaseResult.Lost
        "OWNERSHIP_MISMATCH" -> FencingReleaseResult.OwnershipMismatch
        else -> malformedFencingReply()
    }
}

private data class DecodedFencingFrame(
    val status: String,
    val value1: String,
    val value2: String,
    val ttl: String,
) {
    fun token(): FencingToken = decodeFencingField {
        FencingToken(
            parseCanonicalFencingLong(value1),
            parseCanonicalFencingLong(value2),
        )
    }

    fun ttl(): Long = decodeFencingField { parseCanonicalFencingLong(ttl) }

    fun integrityFailureOrNull(): FencingLeaseIntegrityFailure? {
        if (status != "INTEGRITY_FAILURE") {
            return null
        }
        if (value2 != UNUSED_VALUE || ttl != UNUSED_TTL) {
            malformedFencingReply()
        }
        val kind = when (value1) {
            "MALFORMED_LEASE" -> FencingIntegrityFailureKind.MALFORMED_LEASE
            "INVALID_COUNTER" -> FencingIntegrityFailureKind.INVALID_COUNTER
            "COUNTER_BEHIND_LEASE" -> FencingIntegrityFailureKind.COUNTER_BEHIND_LEASE
            else -> malformedFencingReply()
        }
        return FencingLeaseIntegrityFailure(kind)
    }

    fun requireUnusedToken() {
        if (value1 != UNUSED_VALUE || value2 != UNUSED_VALUE) {
            malformedFencingReply()
        }
    }

    fun requireTtlSentinel() {
        if (ttl != UNUSED_TTL) {
            malformedFencingReply()
        }
    }

    fun requireUnusedValues() {
        requireUnusedToken()
        requireTtlSentinel()
    }

    inline fun <T> withUnusedValues(result: () -> T): T {
        requireUnusedValues()
        return result()
    }
}

private fun decodeFencingFrame(frame: List<String>): DecodedFencingFrame {
    if (frame.size != FENCING_FRAME_SIZE) {
        malformedFencingReply()
    }
    return DecodedFencingFrame(frame[0], frame[1], frame[2], frame[3])
}

private inline fun <T> decodeFencingField(block: () -> T): T = try {
    block()
} catch (_: IllegalArgumentException) {
    malformedFencingReply()
}

private fun compareCanonicalFencingDecimalsWithoutValidation(left: String, right: String): Int {
    val lengthComparison = left.length.compareTo(right.length)
    return if (lengthComparison != 0) lengthComparison else left.compareTo(right)
}

private fun malformedFencingReply(): Nothing = throw FencingLeaseProtocolException()

private const val FENCING_FRAME_SIZE = 4
private const val UNUSED_VALUE = "0"
private const val UNUSED_TTL = "-1"
private const val MAX_COMPLETION_DEPTH = 8
private const val DOMAIN_FINGERPRINT_BYTES = 12
private const val INVALID_LEASE_TIME_MESSAGE = "leaseTime must fit positive whole milliseconds."
private val MAX_LONG_DECIMAL = Long.MAX_VALUE.toString()
