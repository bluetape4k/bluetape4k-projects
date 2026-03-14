package io.bluetape4k.netty.util

import io.bluetape4k.netty.AbstractNettyTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * [ThorowableUtilSupport]의 기능을 검증하는 테스트 클래스입니다.
 */
class ThrowableUtilSupportTest : AbstractNettyTest() {
    @Test
    fun `stackTraceToString은 스택 트레이스 문자열을 반환한다`() {
        val ex = RuntimeException("테스트 예외 메시지")
        val result = ex.stackTraceToString()
        result.shouldNotBeNull()
        (result.isNotEmpty()).shouldBeTrue()
        result shouldContain "테스트 예외 메시지"
        result shouldContain "RuntimeException"
    }

    @Test
    fun `addSuppressed는 suppressed 예외를 추가한다`() {
        val primary = RuntimeException("주 예외")
        val suppressed1 = IllegalArgumentException("억제된 예외 1")
        val suppressed2 = IllegalStateException("억제된 예외 2")

        primary.addSuppressed(listOf(suppressed1, suppressed2))

        primary.suppressed.size shouldBeEqualTo 2
        primary.suppressed[0] shouldBeEqualTo suppressed1
        primary.suppressed[1] shouldBeEqualTo suppressed2
    }

    @Test
    fun `addSuppressedAndClear는 suppressed 예외를 주 예외에 추가한다`() {
        val primary = RuntimeException("주 예외")
        val suppressedList =
            mutableListOf<Throwable>(
                IllegalArgumentException("억제된 예외 1"),
                IllegalStateException("억제된 예외 2")
            )

        primary.addSuppressedAndClear(suppressedList)

        primary.suppressed.size shouldBeEqualTo 2
    }

    @Test
    fun `빈 suppressed 리스트로 addSuppressed를 호출해도 예외가 발생하지 않는다`() {
        val primary = RuntimeException("주 예외")
        primary.addSuppressed(emptyList())
        primary.suppressed.size shouldBeEqualTo 0
    }
}
