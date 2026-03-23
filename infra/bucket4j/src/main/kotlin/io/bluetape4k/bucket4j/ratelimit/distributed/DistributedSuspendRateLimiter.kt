package io.bluetape4k.bucket4j.ratelimit.distributed

import io.bluetape4k.bucket4j.distributed.AsyncBucketProxyProvider
import io.bluetape4k.bucket4j.ratelimit.RateLimitResult
import io.bluetape4k.bucket4j.ratelimit.SuspendRateLimiter
import io.bluetape4k.bucket4j.ratelimit.toRateLimitResult
import io.bluetape4k.bucket4j.ratelimit.validateRateLimitRequest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await

/**
 * 분산 환경에서 즉시 소비 시도 계약을 제공하는 코루틴용 rate limiter 구현체입니다.
 *
 * ## 동작/계약
 * - [consume]은 원격 버킷에 대해 대기 없는 즉시 소비를 시도합니다.
 * - 소비 결과와 잔여 토큰은 `ConsumptionProbe` 한 번의 조회 결과로 해석합니다.
 * - 코루틴 취소는 `ERROR`로 감싸지 않고 그대로 전파합니다.
 *
 * ```
 * val rateLimiter = DistributedSuspendRateLimiter(asyncBucketProxyProvider)
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
 * @property asyncBucketProxyProvider [AsyncBucketProxyProvider] 인스턴스.
 * 원격 저장소(Redis 등)에 연결된 async bucket proxy를 제공합니다.
 */
class DistributedSuspendRateLimiter(
    private val asyncBucketProxyProvider: AsyncBucketProxyProvider,
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
            val bucketProxy = asyncBucketProxyProvider.resolveBucket(key)
            toRateLimitResult(bucketProxy.tryConsumeAndReturnRemaining(numToken).await(), numToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn(e) { "Rate Limiter 적용에 실패했습니다. key=$key" }
            RateLimitResult.error(e)
        }
    }
}
