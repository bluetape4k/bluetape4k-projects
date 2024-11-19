package io.bluetape4k.bucket4j.ratelimit

import java.io.Serializable

/**
 * Rate Limit 토큰 소비 결과
 *
 * @property consumedTokens   소비한 토큰 수
 * @property availableTokens  남아 있는 유효한 토큰 수
 */
data class RateLimitResult(
    val consumedTokens: Long = 1,
    val availableTokens: Long,
): Serializable {

    companion object {
        /**
         * Rate Limit 작업 시에 실패한 경우에 반환된다.
         */
        @JvmStatic
        val ERROR = RateLimitResult(Long.MIN_VALUE, Long.MIN_VALUE)
    }
}
