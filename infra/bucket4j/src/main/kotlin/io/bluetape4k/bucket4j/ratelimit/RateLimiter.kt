package io.bluetape4k.bucket4j.ratelimit

/**
 * 동기 방식 토큰 소비를 제공하는 rate limiter 인터페이스입니다.
 *
 * ## 동작/계약
 * - 구현체는 [key]별로 토큰 버킷을 관리합니다.
 * - [consume]은 대기 없이 즉시 소비를 시도하고 결과를 반환해야 합니다.
 * - 입력 검증(빈 키/토큰 범위)은 구현체 정책에 따릅니다.
 *
 * ```kotlin
 * val result = rateLimiter.consume("user:1", 1)
 * // result.isConsumed || result.isRejected || result.isError
 * ```
 */
interface RateLimiter<K> {

    /**
     * 지정한 [key] 버킷에서 [numToken] 만큼 토큰을 소비 시도합니다.
     *
     * ## 동작/계약
     * - 성공 시 [RateLimitStatus.CONSUMED], 부족 시 [RateLimitStatus.REJECTED]를 반환합니다.
     * - [numToken] 기본값은 `1`입니다.
     * - 구현체에 따라 잘못된 입력은 예외를 발생시킬 수 있습니다.
     *
     * ```kotlin
     * val result = rateLimiter.consume("api-key", 2)
     * // result.consumedTokens == 2 || result.status != RateLimitStatus.CONSUMED
     * ```
     */
    fun consume(key: K, numToken: Long = 1): RateLimitResult

}
