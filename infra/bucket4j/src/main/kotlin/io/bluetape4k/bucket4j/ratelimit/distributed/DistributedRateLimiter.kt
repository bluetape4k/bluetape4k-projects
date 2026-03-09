package io.bluetape4k.bucket4j.ratelimit.distributed

import io.bluetape4k.bucket4j.distributed.BucketProxyProvider
import io.bluetape4k.bucket4j.ratelimit.RateLimitResult
import io.bluetape4k.bucket4j.ratelimit.RateLimiter
import io.bluetape4k.bucket4j.ratelimit.toRateLimitResult
import io.bluetape4k.bucket4j.ratelimit.validateRateLimitRequest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn

/**
 * 분산 버킷에 대해 즉시 소비 시도를 수행하는 동기 rate limiter 구현체입니다.
 *
 * ## 동작/계약
 * - Redis 등 분산 저장소를 사용하는 [BucketProxyProvider] 기반 버킷에서 토큰을 소비합니다.
 * - 내부적으로 `tryConsumeAndReturnRemaining`을 사용해 소비 결과와 잔여 토큰을 한 번에 계산합니다.
 * - 입력 검증 실패는 예외로 처리하고, 분산 저장소 장애는 [RateLimitResult.error]로 변환합니다.
 *
 * ```
 * val rateLimiter = DistributedRateLimiter(bucketProxyProvider)
 * val result: RateLimitResult = rateLimiter.consume("key", 1)
 *
 * when (result.status) {
 *     RateLimitStatus.CONSUMED -> {
 *         // Rate Limit 적용 성공
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
 * @property bucketProxyProvider [BucketProxyProvider] 인스턴스
 */
class DistributedRateLimiter(
    private val bucketProxyProvider: BucketProxyProvider,
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
        validateRateLimitRequest(key, numToken)

        log.debug { "rate limit for key=$key, numToken=$numToken" }

        return try {
            val bucketProxy = bucketProxyProvider.resolveBucket(key)
            toRateLimitResult(bucketProxy.tryConsumeAndReturnRemaining(numToken), numToken)
        } catch (e: Exception) {
            log.warn(e) { "Rate Limiter 적용에 실패했습니다. key=$key" }
            RateLimitResult.error(e)
        }
    }
}
