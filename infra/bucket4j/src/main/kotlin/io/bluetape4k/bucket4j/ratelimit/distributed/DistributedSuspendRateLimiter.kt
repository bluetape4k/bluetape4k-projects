package io.bluetape4k.bucket4j.ratelimit.distributed

import io.bluetape4k.bucket4j.distributed.AsyncBucketProxyProvider
import io.bluetape4k.bucket4j.ratelimit.RateLimitResult
import io.bluetape4k.bucket4j.ratelimit.SuspendRateLimiter
import io.bluetape4k.bucket4j.ratelimit.toRateLimitResult
import io.bluetape4k.bucket4j.ratelimit.validateRateLimitRequest
import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import kotlinx.coroutines.future.await

/**
 * 분산 환경에서 Rate Limiter 를 적용하는 Coroutine Rate Limiter 구현체
 *
 * ```
 * val rateLimiter = DistributedSuspendRateLimiter(asyncBucketProxyProvider)
 * val result: RateLimitResult = rateLimiter.consume("key", 1)
 *
 * if(result.consumedTokens > 0) {
 *      // Rate Limit 적용 성공
 * }
 * ```
 *
 * @property asyncBucketProxyProvider [AsyncBucketProxyProvider] 인스턴스
 */
class DistributedSuspendRateLimiter(
    private val asyncBucketProxyProvider: AsyncBucketProxyProvider,
): SuspendRateLimiter<String> {

    companion object: KLoggingChannel()

    /**
     * [key] 기준으로 [numToken] 갯수만큼 소비합니다. 결과는 [RateLimitResult]로 반환됩니다.
     *
     * @param key      Rate Limit 적용 대상 Key
     * @param numToken 소비할 토큰 수
     * @return [RateLimitResult] 토큰 소비 결과
     */
    override suspend fun consume(key: String, numToken: Long): RateLimitResult {
        validateRateLimitRequest(key, numToken)
        log.debug { "rate limit for key=$key, numToken=$numToken" }

        return try {
            val bucketProxy = asyncBucketProxyProvider.resolveBucket(key)
            toRateLimitResult(
                consumed = bucketProxy.tryConsume(numToken).suspendAwait(),
                requestedTokens = numToken,
                availableTokens = bucketProxy.availableTokens.await()
            )
        } catch (e: Exception) {
            log.warn(e) { "Rate Limiter 적용에 실패했습니다. key=$key" }
            RateLimitResult.error(e)
        }
    }
}
