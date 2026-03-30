package io.bluetape4k.bucket4j.ratelimit

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

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
    fun `error factory 에 null cause 를 전달하면 errorMessage 가 null 이다`() {
        val result = RateLimitResult.error(cause = null)

        result.status shouldBeEqualTo RateLimitStatus.ERROR
        result.consumedTokens shouldBeEqualTo 0
        result.availableTokens shouldBeEqualTo 0
        result.errorMessage.shouldBeNull()
        result.isError.shouldBeTrue()
    }

    @Test
    fun `error factory 는 예외 메시지가 없으면 예외 타입 정보를 보존한다`() {
        val result = RateLimitResult.error(IllegalStateException())

        result.status shouldBeEqualTo RateLimitStatus.ERROR
        result.errorMessage shouldBeEqualTo "java.lang.IllegalStateException"
    }

    @Test
    @Suppress("DEPRECATION")
    fun `deprecated 생성자는 consumedTokens 가 양수이면 consumed 로 해석한다`() {
        val result = RateLimitResult(consumedTokens = 3, availableTokens = 7)

        result.status shouldBeEqualTo RateLimitStatus.CONSUMED
        result.isConsumed.shouldBeTrue()
        result.consumedTokens shouldBeEqualTo 3
        result.availableTokens shouldBeEqualTo 7
    }

    @Test
    @Suppress("DEPRECATION")
    fun `deprecated 생성자는 consumedTokens 가 0이면 rejected 로 해석한다`() {
        val result = RateLimitResult(consumedTokens = 0, availableTokens = 4)

        result.status shouldBeEqualTo RateLimitStatus.REJECTED
        result.isRejected.shouldBeTrue()
    }

    @Test
    @Suppress("DEPRECATION")
    fun `deprecated 생성자는 음수 consumedTokens 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            RateLimitResult(consumedTokens = -1, availableTokens = 4)
        }
    }

    @Test
    fun `primary constructor 는 음수 availableTokens 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            RateLimitResult(
                status = RateLimitStatus.CONSUMED,
                consumedTokens = 1,
                availableTokens = -1,
            )
        }
    }

    @Test
    fun `toRateLimitResult 는 consumed 가 true 이면 CONSUMED 를 반환한다`() {
        val result = toRateLimitResult(consumed = true, requestedTokens = 5, availableTokens = 5)

        result.status shouldBeEqualTo RateLimitStatus.CONSUMED
        result.consumedTokens shouldBeEqualTo 5
        result.availableTokens shouldBeEqualTo 5
    }

    @Test
    fun `toRateLimitResult 는 consumed 가 false 이면 REJECTED 를 반환한다`() {
        val result = toRateLimitResult(consumed = false, requestedTokens = 10, availableTokens = 3)

        result.status shouldBeEqualTo RateLimitStatus.REJECTED
        result.consumedTokens shouldBeEqualTo 0
        result.availableTokens shouldBeEqualTo 3
    }
}
