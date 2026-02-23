@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.bucket4j.ratelimit

import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requireNotBlank

/**
 * 요청당 허용하는 최대 토큰 수.
 *
 * 비정상적으로 큰 값에 의한 계산 오버플로우/비정상 요청을 방지하기 위한 상한선이다.
 */
const val MAX_TOKENS_PER_REQUEST: Long = 1_000_000_000_000L

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
