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
 * 로컬 메모리 버킷에 대해 즉시 소비 시도를 수행하는 coroutine rate limiter 구현체입니다.
 *
 * ## 동작/계약
 * - [consume]은 대기 없이 즉시 소비 가능 여부를 판정합니다.
 * - 내부적으로 `tryConsumeAndReturnRemaining`을 사용해 소비 결과와 잔여 토큰을 한 번에 계산합니다.
 * - `CancellationException`은 그대로 전파하고, 그 외 런타임 오류는 [RateLimitResult.error]로 변환합니다.
 *
 * ```kotlin
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
     * ```kotlin
     * val rateLimiter = LocalSuspendRateLimiter(bucketProvider)
     * val result = rateLimiter.consume("user-42", 1L)
     * // result.isConsumed == true (토큰 여유가 있는 경우)
     * // result.remainingTokens >= 0
     * ```
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
            toRateLimitResult(bucketProxy.tryConsumeAndReturnRemaining(numToken), numToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn(e) { "Rate Limiter 적용에 실패했습니다. key=$key" }
            RateLimitResult.error(e)
        }
    }
}
