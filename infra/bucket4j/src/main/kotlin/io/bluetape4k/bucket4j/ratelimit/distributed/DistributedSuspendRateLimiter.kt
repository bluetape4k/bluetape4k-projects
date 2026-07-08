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
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

/**
 * Coroutine rate limiter for distributed Bucket4j async proxies.
 *
 * ## Contract
 * - [consume] attempts immediate token consumption without waiting for refill.
 * - Consumption outcome and remaining tokens are derived from one
 *   `ConsumptionProbe`.
 * - Coroutine cancellation is propagated unchanged.
 * - [defaultTimeout] converts slow async store operations into an
 *   [RateLimitResult.error] result; it is not a retry or refill wait timeout.
 *
 * ```kotlin
 * val rateLimiter = DistributedSuspendRateLimiter(asyncBucketProxyProvider)
 * val result: RateLimitResult = rateLimiter.consume("key", 1)
 *
 * when (result.status) {
 *     RateLimitStatus.CONSUMED -> Unit
 *     RateLimitStatus.REJECTED -> check(result.diagnostics.rejectionReason != null)
 *     RateLimitStatus.ERROR -> check(result.errorMessage != null)
 * }
 * ```
 *
 * @property asyncBucketProxyProvider provider for async bucket proxies backed by
 * a remote store such as Redis.
 */
class DistributedSuspendRateLimiter @JvmOverloads constructor(
    private val asyncBucketProxyProvider: AsyncBucketProxyProvider,
    private val defaultTimeout: Duration? = null,
): SuspendRateLimiter<String> {

    companion object: KLoggingChannel()

    /**
     * Attempts immediate consumption of [numToken] tokens for [key].
     *
     * @throws CancellationException when the coroutine is cancelled.
     */
    override suspend fun consume(key: String, numToken: Long): RateLimitResult {
        return consume(key, numToken, defaultTimeout)
    }

    /**
     * Attempts immediate token consumption with an optional timeout for the
     * underlying async distributed bucket operation.
     *
     * Timeout is reported as [RateLimitResult.error]. Coroutine cancellation is
     * propagated unchanged.
     *
     * This overload is intentionally available on the concrete distributed
     * implementation only. Code typed as [SuspendRateLimiter] should configure
     * [defaultTimeout] on the bean instead.
     */
    suspend fun consume(key: String, numToken: Long, timeout: Duration?): RateLimitResult {
        validateRateLimitRequest(key, numToken)
        log.debug { "rate limit for key=$key, numToken=$numToken" }

        return try {
            val bucketProxy = asyncBucketProxyProvider.resolveBucket(key)
            val probe = if (timeout == null) {
                bucketProxy.tryConsumeAndReturnRemaining(numToken).await()
            } else {
                withTimeoutOrNull(timeout) {
                    bucketProxy.tryConsumeAndReturnRemaining(numToken).await()
                } ?: return timeoutResult(key, timeout)
            }
            toRateLimitResult(probe, numToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn(e) { "Rate Limiter 적용에 실패했습니다. key=$key" }
            RateLimitResult.error(e)
        }
    }

    private fun timeoutResult(key: String, timeout: Duration): RateLimitResult {
        val cause = TimeoutException("Rate Limiter timed out. key=$key, timeout=$timeout")
        log.warn(cause) { "Rate Limiter timed out. key=$key, timeout=$timeout" }
        return RateLimitResult.error(cause)
    }
}
