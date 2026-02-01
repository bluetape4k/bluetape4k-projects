package io.bluetape4k.bucket4j.ratelimit

import io.bluetape4k.bucket4j.bucketConfiguration
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration

abstract class AbstractSuspendRateLimiterTest {

    companion object: KLoggingChannel() {
        internal const val INITIAL_CAPACITY = 10L

        @JvmStatic
        val defaultBucketConfiguration by lazy {
            bucketConfiguration {
                addLimit { capacityStage ->
                    capacityStage
                        .capacity(INITIAL_CAPACITY)
                        .refillIntervally(INITIAL_CAPACITY, Duration.ofSeconds(10))
                }
            }
        }
    }

    abstract val rateLimiter: SuspendRateLimiter<String>

    protected fun randomKey(): String = "bucket-" + Base58.randomString(6)

    @Test
    fun `특정 키의 Rate Limit 를 적용한다`() = runTest {
        val key = randomKey()

        val token = 5L
        // 초기 Token = 10 개, 5개를 소모한다 
        val result = rateLimiter.consume(key, token)
        // 5개 소모, 5개 남음
        result shouldBeEqualTo RateLimitResult(token, INITIAL_CAPACITY - token)

        // 10개 소비를 요청 -> 5개만 남았으므로 10개 소피를 요청하는 것은 실패하고, 0개 소비한 것으로 반환
        val zeroConsumedResult = RateLimitResult(0, result.availableTokens)
        rateLimiter.consume(key, INITIAL_CAPACITY) shouldBeEqualTo zeroConsumedResult

        // 나머지 토큰 모두를 소비하면, 유효한 토큰이 0개임
        val allConsumedResult = RateLimitResult(result.availableTokens, 0)
        rateLimiter.consume(key, result.availableTokens) shouldBeEqualTo allConsumedResult
    }
}
