package io.bluetape4k.bucket4j.ratelimit.local

import io.bluetape4k.bucket4j.local.LocalBucketProvider
import io.bluetape4k.bucket4j.ratelimit.RateLimitResult
import io.bluetape4k.bucket4j.ratelimit.RateLimiter
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank

/**
 * 로컬 환경에서 Rate Limiter 를 적용하는 Rate Limiter 구현체
 *
 * ```
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
 * result shouldBeEqualTo RateLimitResult(token, AbstractCoRateLimiterTest.INITIAL_CAPACITY - token)
 *
 * // 10개 소비를 요청 -> 5개만 남았으므로 0개 소비한 것으로 반환
 * val result2 = rateLimiter.consume(key, AbstractCoRateLimiterTest.INITIAL_CAPACITY)
 * result2 shouldBeEqualTo RateLimitResult(0, result.availableTokens)
 *
 * // 나머지 토큰 모두를 소비하면, 유효한 토큰이 0개임
 * val result3 = rateLimiter.consume(key, result.availableTokens)
 * result3 shouldBeEqualTo RateLimitResult(result.availableTokens, 0)
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
     * @param key      Rate Limit 적용 대상 Key
     * @param numToken 소비할 토큰 수
     * @return [RateLimitResult] 토큰 소비 결과
     */
    override fun consume(key: String, numToken: Long): RateLimitResult {
        key.requireNotBlank("key")
        log.debug { "rate limit for key=$key, numToken=$numToken" }

        return try {
            val bucketProxy = bucketProvider.resolveBucket(key)

            if (bucketProxy.tryConsume(numToken)) {
                RateLimitResult(numToken, bucketProxy.availableTokens)
            } else {
                RateLimitResult(0, bucketProxy.availableTokens)
            }
        } catch (e: Throwable) {
            log.warn(e) { "Rate Limiter 적용에 실패했습니다. key=$key" }
            RateLimitResult.ERROR
        }
    }
}
