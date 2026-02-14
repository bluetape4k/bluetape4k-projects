package io.bluetape4k.bucket4j.ratelimit

import java.io.Serializable

enum class RateLimitStatus {
    CONSUMED,
    REJECTED,
    ERROR
}

/**
 * Rate Limit 토큰 소비 결과
 *
 * @property status           소비 결과 상태
 * @property consumedTokens   소비한 토큰 수
 * @property availableTokens  남아 있는 유효한 토큰 수
 * @property errorMessage     오류 발생 시 오류 메시지
 */
data class RateLimitResult(
    val status: RateLimitStatus,
    val consumedTokens: Long = 0,
    val availableTokens: Long,
    val errorMessage: String? = null,
): Serializable {

    @Deprecated(
        message = "Use status-based factory methods or the primary constructor with explicit status.",
        replaceWith = ReplaceWith("RateLimitResult.consumed(consumedTokens, availableTokens)")
    )
    constructor(consumedTokens: Long, availableTokens: Long): this(
        status = if (consumedTokens > 0) RateLimitStatus.CONSUMED else RateLimitStatus.REJECTED,
        consumedTokens = consumedTokens,
        availableTokens = availableTokens,
    )

    val isConsumed: Boolean get() = status == RateLimitStatus.CONSUMED
    val isRejected: Boolean get() = status == RateLimitStatus.REJECTED
    val isError: Boolean get() = status == RateLimitStatus.ERROR

    companion object {
        @JvmStatic
        fun consumed(consumedTokens: Long, availableTokens: Long): RateLimitResult =
            RateLimitResult(RateLimitStatus.CONSUMED, consumedTokens, availableTokens)

        @JvmStatic
        fun rejected(availableTokens: Long): RateLimitResult =
            RateLimitResult(RateLimitStatus.REJECTED, 0, availableTokens)

        @JvmStatic
        fun error(cause: Throwable? = null): RateLimitResult =
            RateLimitResult(
                status = RateLimitStatus.ERROR,
                consumedTokens = 0,
                availableTokens = 0,
                errorMessage = cause?.message
            )
    }
}
