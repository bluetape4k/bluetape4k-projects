package io.bluetape4k.bucket4j.ratelimit.local

import io.bluetape4k.bucket4j.coroutines.SuspendLocalBucket
import io.bluetape4k.bucket4j.local.LocalSuspendBucketProvider
import io.bluetape4k.bucket4j.ratelimit.RateLimitResult
import io.bluetape4k.bucket4j.ratelimit.SuspendRateLimiter
import io.bluetape4k.bucket4j.ratelimit.toRateLimitResult
import io.bluetape4k.bucket4j.ratelimit.validateRateLimitRequest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import kotlinx.coroutines.CancellationException

/**
 * 로컬 환경에서 Rate Limiter 를 적용하는 Coroutine Rate Limiter 구현체
 *
 * ```
 * val rateLimiter = LocalSuspendRateLimiter(bucketProvider)
 * val result: RateLimitResult = rateLimiter.consume("key", 1)
 *
 * when (result.status) {
 *     RateLimitStatus.CONSUMED -> {
 *         // Rate Limit 적용 성공
 *         // result.availableTokens: 남아 있는 유효한 토큰 수
 *     }
 *     RateLimitStatus.REJECTED -> {
 *         // 토큰 부족으로 거절
 *     }
 *     RateLimitStatus.ERROR -> {
 *         // 오류 발생 시 result.errorMessage 확인
 *     }
 * }
 * ```
 *
 * @property bucketProvider [LocalSuspendBucketProvider] 인스턴스
 */
class LocalSuspendRateLimiter(
    private val bucketProvider: LocalSuspendBucketProvider,
): SuspendRateLimiter<String> {

    companion object: KLoggingChannel()

    /**
     * [key] 기준으로 [numToken] 갯수만큼 즉시 소비 시도합니다. 결과는 [RateLimitResult]로 반환됩니다.
     *
     * @param key      Rate Limit 적용 대상 Key
     * @param numToken 소비할 토큰 수
     * @return [RateLimitResult] 토큰 소비 결과
     *
     * @throws CancellationException 코루틴 취소 시 그대로 전파
     */
    override suspend fun consume(key: String, numToken: Long): RateLimitResult {
        validateRateLimitRequest(key, numToken)
        log.debug { "rate limit for key=$key, numToken=$numToken" }

        return try {
            val bucketProxy: SuspendLocalBucket = bucketProvider.resolveBucket(key)
            toRateLimitResult(
                bucketProxy.tryConsume(numToken),
                numToken,
                bucketProxy.availableTokens
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn(e) { "Rate Limiter 적용에 실패했습니다. key=$key" }
            RateLimitResult.error(e)
        }
    }
}
