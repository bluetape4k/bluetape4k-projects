package io.bluetape4k.bucket4j.ratelimit

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class RateLimitResultTest {

    @Test
    fun `consumed factory 는 consumed 상태와 잔여 토큰을 유지한다`() {
        val result = RateLimitResult.consumed(consumedTokens = 2, availableTokens = 8)

        result.status shouldBeEqualTo RateLimitStatus.CONSUMED
        result.consumedTokens shouldBeEqualTo 2
        result.availableTokens shouldBeEqualTo 8
        result.isConsumed.shouldBeTrue()
        result.isRejected.shouldBeFalse()
        result.isError.shouldBeFalse()
    }

    @Test
    fun `rejected factory 는 consumedTokens 를 0으로 고정한다`() {
        val result = RateLimitResult.rejected(availableTokens = 3)

        result.status shouldBeEqualTo RateLimitStatus.REJECTED
        result.consumedTokens shouldBeEqualTo 0
        result.availableTokens shouldBeEqualTo 3
        result.isConsumed.shouldBeFalse()
        result.isRejected.shouldBeTrue()
        result.isError.shouldBeFalse()
    }

    @Test
    fun `error factory 는 errorMessage 를 보존한다`() {
        val result = RateLimitResult.error(IllegalStateException("redis unavailable"))

        result.status shouldBeEqualTo RateLimitStatus.ERROR
        result.consumedTokens shouldBeEqualTo 0
        result.availableTokens shouldBeEqualTo 0
        result.errorMessage shouldBeEqualTo "redis unavailable"
        result.isConsumed.shouldBeFalse()
        result.isRejected.shouldBeFalse()
        result.isError.shouldBeTrue()
    }

    @Test
    @Suppress("DEPRECATION")
    fun `deprecated 생성자는 consumedTokens 가 0이면 rejected 로 해석한다`() {
        val result = RateLimitResult(consumedTokens = 0, availableTokens = 4)

        result.status shouldBeEqualTo RateLimitStatus.REJECTED
        result.isRejected.shouldBeTrue()
    }
}
