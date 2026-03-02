package io.bluetape4k.bucket4j.ratelimit

import java.io.Serializable

/**
 * 토큰 소비 시도 결과 상태를 나타냅니다.
 *
 * ## 동작/계약
 * - [CONSUMED]는 요청 토큰이 정상 소비된 상태입니다.
 * - [REJECTED]는 가용 토큰 부족으로 소비가 거절된 상태입니다.
 * - [ERROR]는 소비 처리 중 예외가 발생한 상태입니다.
 *
 * ```kotlin
 * val status = RateLimitStatus.CONSUMED
 * // status.name == "CONSUMED"
 * ```
 */
enum class RateLimitStatus {
    /** 요청 토큰이 정상 소비됨 */
    CONSUMED,
    /** 토큰 부족으로 소비 거절됨 */
    REJECTED,
    /** 처리 중 오류가 발생함 */
    ERROR
}

/**
 * Rate limit 토큰 소비 결과를 나타내는 값 객체입니다.
 *
 * ## 동작/계약
 * - [status]에 따라 [consumedTokens], [availableTokens], [errorMessage] 해석이 달라집니다.
 * - [isConsumed]/[isRejected]/[isError]는 [status] 비교 결과를 그대로 반환합니다.
 * - `consumed/rejected/error` 팩토리 메서드는 상태별 권장 생성 경로입니다.
 *
 * ```kotlin
 * val result = RateLimitResult.consumed(consumedTokens = 1, availableTokens = 9)
 * // result.isConsumed == true
 * // result.availableTokens == 9
 * ```
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
        /** 소비 성공 결과를 생성합니다. */
        @JvmStatic
        fun consumed(consumedTokens: Long, availableTokens: Long): RateLimitResult =
            RateLimitResult(RateLimitStatus.CONSUMED, consumedTokens, availableTokens)

        /** 소비 거절 결과를 생성합니다. */
        @JvmStatic
        fun rejected(availableTokens: Long): RateLimitResult =
            RateLimitResult(RateLimitStatus.REJECTED, 0, availableTokens)

        /** 오류 결과를 생성합니다. */
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
