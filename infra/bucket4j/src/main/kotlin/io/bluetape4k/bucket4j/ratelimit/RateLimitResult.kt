package io.bluetape4k.bucket4j.ratelimit

import java.io.Serializable
import java.time.Duration

/**
 * Result status for a rate-limit token consumption attempt.
 *
 * ## Contract
 * - [CONSUMED] means the requested tokens were consumed.
 * - [REJECTED] means the bucket did not have enough available tokens.
 * - [ERROR] means the provider failed while resolving or consuming from a bucket.
 */
enum class RateLimitStatus {
    /** Requested tokens were consumed. */
    CONSUMED,

    /** Consumption was rejected because the bucket had insufficient tokens. */
    REJECTED,

    /** Provider or bucket processing failed. */
    ERROR
}

/**
 * Stable rejection reason owned by bluetape4k.
 *
 * Values are additive-only: existing names must not be renamed or reordered
 * because callers may persist or exhaustively match them.
 */
enum class RateLimitRejectionReason {
    /** The bucket had fewer available tokens than requested. */
    INSUFFICIENT_TOKENS
}

/**
 * Diagnostic data derived from Bucket4j probes without exposing upstream types.
 *
 * ## Contract
 * - [nanosToWaitForRefill] is positive only when a rejected caller can retry
 *   after waiting for token refill.
 * - [nanosToWaitForReset] is the observed time until the bucket is fully
 *   refilled when the upstream probe provides it.
 * - [rejectionReason] is set only for [RateLimitStatus.REJECTED] results.
 */
data class RateLimitDiagnostics(
    val nanosToWaitForRefill: Long = 0,
    val nanosToWaitForReset: Long = 0,
    val rejectionReason: RateLimitRejectionReason? = null,
): Serializable {

    init {
        require(nanosToWaitForRefill >= 0) {
            "nanosToWaitForRefill must be greater than or equal to 0. nanosToWaitForRefill=$nanosToWaitForRefill"
        }
        require(nanosToWaitForReset >= 0) {
            "nanosToWaitForReset must be greater than or equal to 0. nanosToWaitForReset=$nanosToWaitForReset"
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        @JvmField
        val EMPTY: RateLimitDiagnostics = RateLimitDiagnostics()

        @JvmStatic
        fun rejected(
            nanosToWaitForRefill: Long,
            nanosToWaitForReset: Long,
            rejectionReason: RateLimitRejectionReason = RateLimitRejectionReason.INSUFFICIENT_TOKENS,
        ): RateLimitDiagnostics =
            RateLimitDiagnostics(nanosToWaitForRefill, nanosToWaitForReset, rejectionReason)
    }
}

/**
 * Value object returned by bluetape4k rate limiters.
 *
 * ## Contract
 * - [status] defines how [consumedTokens], [availableTokens], [diagnostics], and
 *   [errorMessage] should be interpreted.
 * - [isConsumed], [isRejected], and [isError] are status predicates.
 * - Factory methods are the preferred construction path.
 */
data class RateLimitResult(
    /** Token consumption result status. */
    val status: RateLimitStatus,
    /** Number of tokens consumed by this attempt. */
    val consumedTokens: Long = 0,
    /** Remaining tokens observed after the attempt. */
    val availableTokens: Long,
    /** Public, sanitized error message for logging or metric tags. */
    val errorMessage: String? = null,
    /** Probe diagnostics for retry-after and rejection reporting. */
    val diagnostics: RateLimitDiagnostics = RateLimitDiagnostics.EMPTY,
): Serializable {

    init {
        require(consumedTokens >= 0) { "consumedTokens must be greater than or equal to 0. consumedTokens=$consumedTokens" }
        require(availableTokens >= 0) { "availableTokens must be greater than or equal to 0. availableTokens=$availableTokens" }
        require(status == RateLimitStatus.REJECTED || diagnostics.rejectionReason == null) {
            "rejectionReason is meaningful only for REJECTED results. status=$status, rejectionReason=${diagnostics.rejectionReason}"
        }
    }

    /** Whether this attempt consumed tokens. */
    val isConsumed: Boolean get() = status == RateLimitStatus.CONSUMED

    /** Whether this attempt was rejected. */
    val isRejected: Boolean get() = status == RateLimitStatus.REJECTED

    /** Whether this attempt failed with provider or bucket error. */
    val isError: Boolean get() = status == RateLimitStatus.ERROR

    /** Caller-friendly retry delay for rejected attempts, or `null` when absent. */
    val retryAfter: Duration?
        get() = diagnostics.nanosToWaitForRefill
            .takeIf { status == RateLimitStatus.REJECTED && it > 0 }
            ?.let(Duration::ofNanos)

    companion object {
        private const val serialVersionUID: Long = 1L
        private const val MAX_ERROR_MESSAGE_LENGTH = 256

        private val URI_CREDENTIALS = Regex("([a-zA-Z][a-zA-Z0-9+.-]*://)([^/\\s]+)@")

        /** Creates a consumed result. */
        @JvmStatic
        fun consumed(
            consumedTokens: Long,
            availableTokens: Long,
            diagnostics: RateLimitDiagnostics = RateLimitDiagnostics.EMPTY,
        ): RateLimitResult =
            RateLimitResult(RateLimitStatus.CONSUMED, consumedTokens, availableTokens, diagnostics = diagnostics)

        /** Creates a rejected result. */
        @JvmStatic
        fun rejected(
            availableTokens: Long,
            diagnostics: RateLimitDiagnostics = RateLimitDiagnostics.rejected(
                nanosToWaitForRefill = 0,
                nanosToWaitForReset = 0,
            ),
        ): RateLimitResult =
            RateLimitResult(RateLimitStatus.REJECTED, 0, availableTokens, diagnostics = diagnostics)

        /** Creates an error result with a public sanitized message. */
        @JvmStatic
        fun error(cause: Throwable? = null): RateLimitResult =
            RateLimitResult(
                status = RateLimitStatus.ERROR,
                consumedTokens = 0,
                availableTokens = 0,
                errorMessage = sanitizeErrorMessage(cause)
            )

        internal fun sanitizeErrorMessage(cause: Throwable?): String? {
            if (cause == null) return null

            val source = cause.message
                ?.takeIf { it.isNotBlank() }
                ?: cause.javaClass.name

            return source
                .replace(URI_CREDENTIALS) { matchResult ->
                    "${matchResult.groupValues[1]}<redacted>@"
                }
                .take(MAX_ERROR_MESSAGE_LENGTH)
        }
    }
}
