package io.bluetape4k.bucket4j.ratelimit

import io.bluetape4k.bucket4j.bucketConfiguration
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

abstract class AbstractRateLimiterTest {

    companion object: KLogging() {
        internal const val INITIAL_CAPACITY = 10L

        @JvmStatic
        val defaultBucketConfiguration by lazy {
            bucketConfiguration {
                addLimit {
                    it
                        .capacity(INITIAL_CAPACITY)
                        .refillIntervally(INITIAL_CAPACITY, Duration.ofSeconds(10))
                }
            }
        }
    }

    abstract val rateLimiter: RateLimiter<String>

    protected fun randomKey(): String = "bucket-" + Base58.randomString(6)

    @Test
    fun `특정 키의 Rate Limit 를 적용한다`() {
        val key = randomKey()

        val token = 5L
        // 초기 Token = 10 개, 5개를 소모한다
        val result = rateLimiter.consume(key, token)
        // 5개 소모, 5개 남음
        result.status shouldBeEqualTo RateLimitStatus.CONSUMED
        result.consumedTokens shouldBeEqualTo token
        result.availableTokens shouldBeEqualTo (INITIAL_CAPACITY - token)

        // 10개 소비를 요청 -> 5개만 남았으므로 consume에 실패하고, 0개 소비한 것으로 반환
        val result2 = rateLimiter.consume(key, INITIAL_CAPACITY)
        result2.status shouldBeEqualTo RateLimitStatus.REJECTED
        result2.consumedTokens shouldBeEqualTo 0
        result2.availableTokens shouldBeEqualTo result.availableTokens

        // 나머지 토큰 모두를 소비하면, 유효한 토큰이 0개임
        val result3 = rateLimiter.consume(key, result.availableTokens)
        result3.status shouldBeEqualTo RateLimitStatus.CONSUMED
        result3.consumedTokens shouldBeEqualTo result.availableTokens
        result3.availableTokens shouldBeEqualTo 0
    }

    @Test
    fun `빈 key 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            rateLimiter.consume(" ", 1)
        }
    }

    @Test
    fun `0 이하 token 소비 요청은 허용하지 않는다`() {
        val key = randomKey()

        assertFailsWith<IllegalArgumentException> {
            rateLimiter.consume(key, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            rateLimiter.consume(key, -1)
        }
    }

    @Test
    fun `허용 상한을 초과한 token 소비 요청은 허용하지 않는다`() {
        val key = randomKey()
        assertFailsWith<IllegalArgumentException> {
            rateLimiter.consume(key, MAX_TOKENS_PER_REQUEST + 1)
        }
    }
}
