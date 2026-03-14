package io.bluetape4k.retrofit2

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeSameInstanceAs
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * [toIOException] 확장 함수 단위 테스트입니다.
 */
class ExceptionSupportTest {

    companion object: KLogging()

    @Test
    fun `IOException은 그대로 반환된다`() {
        val original = IOException("original")
        val result = original.toIOException()
        result shouldBeSameInstanceAs original
    }

    @Test
    fun `비IOException은 IOException으로 래핑된다`() {
        val cause = IllegalStateException("boom")
        val result = cause.toIOException()

        result shouldBeInstanceOf IOException::class
        result.cause shouldBeSameInstanceAs cause
        result.message shouldBeEqualTo "boom"
    }

    @Test
    fun `메시지가 없는 예외는 toString을 메시지로 사용한다`() {
        val cause = object: RuntimeException() {
            override val message: String? = null
            override fun toString(): String = "custom-toString"
        }
        val result = cause.toIOException()

        result shouldBeInstanceOf IOException::class
        result.message shouldBeEqualTo "custom-toString"
        result.cause shouldBeSameInstanceAs cause
    }

    @Test
    fun `NullPointerException도 IOException으로 래핑된다`() {
        val cause = NullPointerException("null ref")
        val result = cause.toIOException()

        result shouldBeInstanceOf IOException::class
        result.cause shouldNotBeNull()
        result.cause shouldBeInstanceOf NullPointerException::class
    }
}
