@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.bucket4j.ratelimit

import io.bluetape4k.bucket4j.MAX_TOKENS_PER_REQUEST
import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requireNotBlank
import io.github.bucket4j.ConsumptionProbe


internal inline fun validateRateLimitRequest(key: String, numToken: Long) {
    key.requireNotBlank("key")
    numToken.requireInRange(1, MAX_TOKENS_PER_REQUEST, "numToken")
}

internal inline fun toRateLimitResult(
    consumed: Boolean,
    requestedTokens: Long,
    availableTokens: Long,
): RateLimitResult {
    return if (consumed) {
        RateLimitResult.consumed(requestedTokens, availableTokens)
    } else {
        RateLimitResult.rejected(availableTokens)
    }
}

internal inline fun toRateLimitResult(
    probe: ConsumptionProbe,
    requestedTokens: Long,
): RateLimitResult {
    return if (probe.isConsumed) {
        RateLimitResult.consumed(
            consumedTokens = requestedTokens,
            availableTokens = probe.remainingTokens,
            diagnostics = RateLimitDiagnostics(
                nanosToWaitForRefill = 0,
                nanosToWaitForReset = probe.nanosToWaitForReset,
            ),
        )
    } else {
        RateLimitResult.rejected(
            availableTokens = probe.remainingTokens,
            diagnostics = RateLimitDiagnostics.rejected(
                nanosToWaitForRefill = probe.nanosToWaitForRefill,
                nanosToWaitForReset = probe.nanosToWaitForReset,
            ),
        )
    }
}
