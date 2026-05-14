package io.bluetape4k.bucket4j.ratelimit

/**
 * Coroutine-based rate limiter interface.
 *
 * ## Contract
 * - [consume] attempts immediate token consumption and returns without waiting
 *   for token refill.
 * - Success, rejection, and provider failure are represented by [RateLimitResult].
 * - Coroutine cancellation must be propagated to the caller unchanged.
 * - Distributed implementations may provide implementation-specific timeout
 *   configuration for the underlying async store operation.
 *
 * ```kotlin
 * val result = suspendRateLimiter.consume("user:1", 1)
 * // result.isConsumed || result.isRejected || result.isError
 * ```
 */
interface SuspendRateLimiter<K> {

    /**
     * Attempts to consume [numToken] tokens from the bucket identified by [key].
     *
     * ## Contract
     * - [numToken] defaults to `1`.
     * - Returns a consumed result on success and a rejected result when tokens
     *   are insufficient.
     * - Propagates `CancellationException` unchanged.
     *
     * ```kotlin
     * val result = suspendRateLimiter.consume("api-key", 2)
     * // result.status != RateLimitStatus.ERROR || result.errorMessage != null
     * ```
     */
    suspend fun consume(key: K, numToken: Long = 1): RateLimitResult

}
