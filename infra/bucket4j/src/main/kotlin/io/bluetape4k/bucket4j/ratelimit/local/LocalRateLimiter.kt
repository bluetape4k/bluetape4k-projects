package io.bluetape4k.bucket4j.ratelimit.local

import io.bluetape4k.bucket4j.local.LocalBucketProvider
import io.bluetape4k.bucket4j.ratelimit.RateLimitResult
import io.bluetape4k.bucket4j.ratelimit.RateLimiter
import io.bluetape4k.bucket4j.ratelimit.toRateLimitResult
import io.bluetape4k.bucket4j.ratelimit.validateRateLimitRequest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn

/**
 * 로컬 메모리 버킷에 대해 즉시 소비 시도를 수행하는 동기 rate limiter 구현체입니다.
 *
 * ## 동작/계약
 * - [consume]은 대기 없이 즉시 소비 가능 여부를 판정합니다.
 * - 내부적으로 `tryConsumeAndReturnRemaining`을 사용해 소비 여부와 잔여 토큰 수를 한 번에 계산합니다.
 * - 입력 검증 실패는 예외로 처리하고, 버킷 조회/소비 중 런타임 오류는 [RateLimitResult.error]로 변환합니다.
 *
 * ```kotlin
 * val bucketProvider by lazy {
 *     LocalBucketProvider(defaultBucketConfiguration)
 * }
 * override val rateLimiter: RateLimiter<String> by lazy {
 *     LocalRateLimiter(bucketProvider)
 * }
 *
 * val key = randomKey()
 * val token = 5L
 * // 초기 Token = 10 개, 5개를 소모한다
 *
 * val result = rateLimiter.consume(key, token)
 * // 5개 소모, 5개 남음
 * result.status shouldBeEqualTo RateLimitStatus.CONSUMED
 * result.consumedTokens shouldBeEqualTo token
 * result.availableTokens shouldBeEqualTo (INITIAL_CAPACITY - token)
 *
 * // 10개 소비를 요청 -> 5개만 남았으므로 0개 소비한 것으로 반환
 * val result2 = rateLimiter.consume(key, INITIAL_CAPACITY)
 * result2.status shouldBeEqualTo RateLimitStatus.REJECTED
 * result2.consumedTokens shouldBeEqualTo 0
 * result2.availableTokens shouldBeEqualTo result.availableTokens
 *
 * // 나머지 토큰 모두를 소비하면, 유효한 토큰이 0개임
 * val result3 = rateLimiter.consume(key, result.availableTokens)
 * result3.status shouldBeEqualTo RateLimitStatus.CONSUMED
 * result3.consumedTokens shouldBeEqualTo result.availableTokens
 * result3.availableTokens shouldBeEqualTo 0
 * ```
 *
 * @property bucketProvider [LocalBucketProvider] 인스턴스
 */
open class LocalRateLimiter(
    private val bucketProvider: LocalBucketProvider,
): RateLimiter<String> {

    companion object: KLogging()

    /**
     * [key] 기준으로 [numToken] 갯수만큼 소비합니다. 결과는 [RateLimitResult]로 반환됩니다.
     *
     * ```kotlin
     * val rateLimiter = LocalRateLimiter(bucketProvider)
     * val result = rateLimiter.consume("user-42", 1L)
     * // result.isConsumed == true (토큰 여유가 있는 경우)
     * // result.remainingTokens >= 0
     * ```
     *
     * @param key      Rate Limit 적용 대상 Key
     * @param numToken 소비할 토큰 수
     * @return [RateLimitResult] 토큰 소비 결과
     */
    override fun consume(key: String, numToken: Long): RateLimitResult {
        validateRateLimitRequest(key, numToken)
        log.debug { "rate limit for key=$key, numToken=$numToken" }

        return try {
            val bucketProxy = bucketProvider.resolveBucket(key)
            toRateLimitResult(bucketProxy.tryConsumeAndReturnRemaining(numToken), numToken)
        } catch (e: Exception) {
            log.warn(e) { "Rate Limiter 적용에 실패했습니다. key=$key" }
            RateLimitResult.error(e)
        }
    }
}
