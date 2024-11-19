package io.bluetape4k.bucket4j.ratelimit

import io.bluetape4k.bucket4j.bucketConfiguration
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

abstract class AbstractCoRateLimiterTest {

    companion object: KLogging() {
        internal const val INITIAL_CAPACITY = 10L

        @JvmStatic
        val defaultBucketConfiguration by lazy {
            bucketConfiguration {
                addLimit {
                    it.capacity(INITIAL_CAPACITY).refillIntervally(INITIAL_CAPACITY, 10.seconds.toJavaDuration())
                }
            }
        }
    }

    abstract val rateLimiter: CoRateLimiter<String>

    protected fun randomKey(): String = "bucket-" + Base58.randomString(6)

    @Test
    fun `특정 키의 Rate Limit 를 적용한다`() = runTest {
        val key = randomKey()

        val token = 5L
        // 초기 Token = 10 개, 5개를 소모한다 
        val result = rateLimiter.coConsume(key, token)
        // 5개 소모, 5개 남음
        result shouldBeEqualTo RateLimitResult(token, INITIAL_CAPACITY - token)

        // 10개 소비를 요청 -> 5개만 남았으므로 0개 소비한 것으로 반환
        rateLimiter.coConsume(key, INITIAL_CAPACITY) shouldBeEqualTo RateLimitResult(0, result.availableTokens)

        // 나머지 토큰 모두를 소비하면, 유효한 토큰이 0개임 
        rateLimiter.coConsume(key, result.availableTokens) shouldBeEqualTo RateLimitResult(result.availableTokens, 0)
    }
}
