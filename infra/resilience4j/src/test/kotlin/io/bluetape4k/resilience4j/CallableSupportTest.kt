package io.bluetape4k.resilience4j

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.Callable

class CallableSupportTest {

    companion object : KLoggingChannel()

    @Test
    fun `andThen - 결과를 변환한다`() {
        val callable = Callable { 42 }
        val result = callable.andThen { it + 1 }.call()
        result shouldBeEqualTo 43
    }

    @Test
    fun `andThen with result and error - 성공 시 결과를 처리한다`() {
        val callable = Callable { 42 }
        val result = callable.andThen { r, _ -> r + 1 }.call()
        result shouldBeEqualTo 43
    }

    @Test
    fun `andThen with resultHandler and exceptionHandler - 성공 시 결과를 처리한다`() {
        val callable = Callable { 42 }
        val result = callable.andThen({ it + 1 }, { -1 }).call()
        result shouldBeEqualTo 43
    }

    @Test
    fun `andThen with resultHandler and exceptionHandler - 예외 발생 시 exceptionHandler를 실행한다`() {
        val callable = Callable<Int> { throw RuntimeException("error") }
        val result = callable.andThen({ it + 1 }, { -1 }).call()
        result shouldBeEqualTo -1
    }

    @Test
    fun `recover - 예외 발생 시 기본값을 반환한다`() {
        val callable = Callable<Int> { throw RuntimeException("error") }
        val result = callable.recover { -1 }.call()
        result shouldBeEqualTo -1
    }

    @Test
    fun `recover - 성공 시 원래 결과를 반환한다`() {
        val callable = Callable { 42 }
        val result = callable.recover { -1 }.call()
        result shouldBeEqualTo 42
    }

    @Test
    fun `recover with predicate - 조건 충족 시 결과를 교체한다`() {
        val callable = Callable { 42 }
        val result = callable.recover({ it == 42 }, { it + 1 }).call()
        result shouldBeEqualTo 43
    }

    @Test
    fun `recover with predicate - 조건 미충족 시 원래 결과를 반환한다`() {
        val callable = Callable { 10 }
        val result = callable.recover({ it == 42 }, { it + 1 }).call()
        result shouldBeEqualTo 10
    }

    @Test
    fun `recover with exceptionTypes list - 매칭 예외 발생 시 복구한다`() {
        val callable = Callable<Int> { throw IOException("io error") }
        val result = callable.recover(listOf(IOException::class.java)) { -1 }.call()
        result shouldBeEqualTo -1
    }

    @Test
    fun `recover with exceptionType KClass - 매칭 예외 발생 시 복구한다`() {
        val callable = Callable<Int> { throw IOException("io error") }
        val result = callable.recover(IOException::class) { -1 }.call()
        result shouldBeEqualTo -1
    }

    @Test
    fun `recover with exceptionType KClass - 비매칭 예외는 rethrow한다`() {
        val callable = Callable<Int> { throw RuntimeException("other") }
        assertFailsWith<RuntimeException> {
            callable.recover(IOException::class) { -1 }.call()
        }
    }
}
