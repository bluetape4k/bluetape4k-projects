package io.bluetape4k.bucket4j.ratelimit

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.Duration

class RateLimitResultTest {
    @Test
    fun `consumed factory 는 consumed 상태와 잔여 토큰을 유지한다`() {
        val result = RateLimitResult.consumed(consumedTokens = 2, availableTokens = 8)

        result.status shouldBeEqualTo RateLimitStatus.CONSUMED
        result.consumedTokens shouldBeEqualTo 2
        result.availableTokens shouldBeEqualTo 8
        result.diagnostics shouldBeEqualTo RateLimitDiagnostics.EMPTY
        result.retryAfter.shouldBeNull()
        result.isConsumed.shouldBeTrue()
        result.isRejected.shouldBeFalse()
        result.isError.shouldBeFalse()
    }

    @Test
    fun `rejected factory 는 consumedTokens 를 0으로 고정한다`() {
        val result = RateLimitResult.rejected(
            availableTokens = 3,
            diagnostics = RateLimitDiagnostics.rejected(
                nanosToWaitForRefill = 1_000,
                nanosToWaitForReset = 10_000,
            ),
        )

        result.status shouldBeEqualTo RateLimitStatus.REJECTED
        result.consumedTokens shouldBeEqualTo 0
        result.availableTokens shouldBeEqualTo 3
        result.diagnostics.nanosToWaitForRefill shouldBeEqualTo 1_000
        result.diagnostics.nanosToWaitForReset shouldBeEqualTo 10_000
        result.diagnostics.rejectionReason shouldBeEqualTo RateLimitRejectionReason.INSUFFICIENT_TOKENS
        result.retryAfter shouldBeEqualTo Duration.ofNanos(1_000)
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
    fun `error factory 는 예외 메시지가 없으면 예외 타입 이름을 보존한다`() {
        val result = RateLimitResult.error(IllegalStateException())

        result.status shouldBeEqualTo RateLimitStatus.ERROR
        result.errorMessage shouldBeEqualTo "java.lang.IllegalStateException"
    }

    @Test
    fun `error factory 는 URI credential 을 redaction 한다`() {
        val result = RateLimitResult.error(
            IllegalStateException("redis://user:secret@localhost:6379 is unavailable")
        )

        result.errorMessage.shouldNotContain("secret")
        result.errorMessage shouldBeEqualTo "redis://<redacted>@localhost:6379 is unavailable"
    }

    @Test
    fun `error factory 는 at sign 을 포함한 URI credential 을 redaction 한다`() {
        val result = RateLimitResult.error(
            IllegalStateException("redis://user:p@ss@localhost:6379 is unavailable")
        )

        result.errorMessage.shouldNotContain("p@ss")
        result.errorMessage shouldBeEqualTo "redis://<redacted>@localhost:6379 is unavailable"
    }

    @Test
    fun `error factory 는 public message 를 256자로 제한한다`() {
        val result = RateLimitResult.error(IllegalStateException("x".repeat(300)))

        result.errorMessage?.length shouldBeEqualTo 256
    }

    @Test
    fun `primary constructor 는 음수 consumedTokens 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            RateLimitResult(
                status = RateLimitStatus.CONSUMED,
                consumedTokens = -1,
                availableTokens = 4,
            )
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
    fun `rejectionReason 은 rejected 결과에만 허용된다`() {
        assertFailsWith<IllegalArgumentException> {
            RateLimitResult.consumed(
                consumedTokens = 1,
                availableTokens = 1,
                diagnostics = RateLimitDiagnostics(
                    rejectionReason = RateLimitRejectionReason.INSUFFICIENT_TOKENS
                ),
            )
        }
    }

    @Test
    fun `retryAfter 는 refill nanos 가 0이면 null 이다`() {
        val result = RateLimitResult.rejected(
            availableTokens = 0,
            diagnostics = RateLimitDiagnostics.rejected(
                nanosToWaitForRefill = 0,
                nanosToWaitForReset = 5_000,
            ),
        )

        result.retryAfter.shouldBeNull()
    }

    @Test
    fun `diagnostics 는 음수 nanos 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            RateLimitDiagnostics(nanosToWaitForRefill = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            RateLimitDiagnostics(nanosToWaitForReset = -1)
        }
    }

    @Test
    fun `result 와 diagnostics 는 Java serialization round trip 을 지원한다`() {
        val source = RateLimitResult.rejected(
            availableTokens = 0,
            diagnostics = RateLimitDiagnostics.rejected(
                nanosToWaitForRefill = 1_000,
                nanosToWaitForReset = 10_000,
            ),
        )

        val bytes = ByteArrayOutputStream().use { bytes ->
            ObjectOutputStream(bytes).use { it.writeObject(source) }
            bytes.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject() as RateLimitResult
        }

        restored shouldBeEqualTo source
        restored.errorMessage.shouldBeNull()
        restored.toString() shouldContain "RateLimitResult"
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
        result.diagnostics.rejectionReason shouldBeEqualTo RateLimitRejectionReason.INSUFFICIENT_TOKENS
    }
}
