package io.bluetape4k.bucket4j.ratelimit

/**
 * 코루틴 환경에서 토큰 소비를 제공하는 suspend rate limiter 인터페이스입니다.
 *
 * ## 동작/계약
 * - [consume]은 대기 없이 즉시 소비를 시도하고 결과를 반환합니다.
 * - 성공/거절/오류 상태는 [RateLimitResult]로 표현됩니다.
 * - 코루틴 취소는 구현체가 가로채지 않고 호출자에게 전파해야 합니다.
 *
 * ```kotlin
 * val result = suspendRateLimiter.consume("user:1", 1)
 * // result.isConsumed || result.isRejected || result.isError
 * ```
 */
interface SuspendRateLimiter<K> {

    /**
     * 지정한 [key] 버킷에서 [numToken] 만큼 토큰을 비동기로 소비 시도합니다.
     *
     * ## 동작/계약
     * - [numToken] 기본값은 `1`입니다.
     * - 소비 성공 시 consumed, 토큰 부족 시 rejected 결과를 반환합니다.
     * - 코루틴 취소 시 `CancellationException`이 그대로 전파됩니다.
     *
     * ```kotlin
     * val result = suspendRateLimiter.consume("api-key", 2)
     * // result.status != RateLimitStatus.ERROR || result.errorMessage != null
     * ```
     */
    suspend fun consume(key: K, numToken: Long = 1): RateLimitResult

}
