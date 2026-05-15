package io.bluetape4k.resilience4j

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Test
import java.io.IOException

class SupplierSupportTest {

    companion object : KLoggingChannel()

    @Test
    fun `andThen - 결과를 변환한다`() {
        val fn = { 42 }
        val result = fn.andThen { it + 1 }.invoke()
        result shouldBeEqualTo 43
    }

    @Test
    fun `andThen with result and error - 성공 시 결과를 처리한다`() {
        val fn = { 42 }
        val result = fn.andThen { r, _ -> r!! + 1 }.invoke()
        result shouldBeEqualTo 43
    }

    @Test
    fun `andThen with result and error - 예외 발생 시 대체값을 반환한다`() {
        val fn: () -> Int = { throw RuntimeException("error") }
        val result = fn.andThen { r, e -> if (e != null) -1 else r!! }.invoke()
        result shouldBeEqualTo -1
    }

    @Test
    fun `andThen with resultHandler and exceptionHandler - 성공 시 결과를 처리한다`() {
        val fn = { 42 }
        val result = fn.andThen({ it + 1 }, { -1 }).invoke()
        result shouldBeEqualTo 43
    }

    @Test
    fun `andThen with resultHandler and exceptionHandler - 예외 발생 시 exceptionHandler를 실행한다`() {
        val fn: () -> Int = { throw RuntimeException("error") }
        val result = fn.andThen({ it + 1 }, { -1 }).invoke()
        result shouldBeEqualTo -1
    }

    @Test
    fun `recover - 예외 발생 시 기본값을 반환한다`() {
        val fn: () -> Int = { throw RuntimeException("error") }
        val result = fn.recover { -1 }.invoke()
        result shouldBeEqualTo -1
    }

    @Test
    fun `recover - 성공 시 원래 결과를 반환한다`() {
        val fn = { 42 }
        val result = fn.recover { -1 }.invoke()
        result shouldBeEqualTo 42
    }

    @Test
    fun `recover with predicate - 조건 충족 시 결과를 교체한다`() {
        val fn: () -> String = { "" }
        val result = fn.recover(
            { s: String -> s.isEmpty() },
            { _: String -> "default" }
        ).invoke()
        result shouldBeEqualTo "default"
    }

    @Test
    fun `recover with predicate - 조건 미충족 시 원래 결과를 반환한다`() {
        val fn: () -> String = { "value" }
        val result = fn.recover(
            { s: String -> s.isEmpty() },
            { _: String -> "default" }
        ).invoke()
        result shouldBeEqualTo "value"
    }

    @Test
    fun `recover with KClass - 특정 예외 타입을 복구한다`() {
        val fn: () -> Int = { throw IOException("io error") }
        val result = fn.recover(IOException::class) { -1 }.invoke()
        result shouldBeEqualTo -1
    }

    @Test
    fun `recover with KClass - 비매칭 예외는 rethrow한다`() {
        val fn: () -> Int = { throw RuntimeException("other") }
        assertFailsWith<RuntimeException> {
            fn.recover(IOException::class) { -1 }.invoke()
        }
    }

    @Test
    fun `recover with exceptionTypes list - 매칭 예외를 복구한다`() {
        val fn: () -> Int = { throw IOException("io error") }
        val result = fn.recover(listOf(IOException::class.java)) { -1 }.invoke()
        result shouldBeEqualTo -1
    }

    @Test
    fun `recover with exceptionTypes list - 비매칭 예외는 rethrow한다`() {
        val fn: () -> Int = { throw RuntimeException("other") }
        assertFailsWith<RuntimeException> {
            fn.recover(listOf(IOException::class.java)) { -1 }.invoke()
        }
    }

    @Test
    fun `recover with resultHandler and exceptionHandler - 성공 시 변환한다`() {
        val fn = { 42 }
        val result = fn.recover(
            resultHandler = { n: Int -> n * 2 },
            exceptionHandler = { _: Throwable? -> -1 }
        ).invoke()
        result shouldBeEqualTo 84
    }

    @Test
    fun `recover with resultHandler and exceptionHandler - 예외 발생 시 exceptionHandler를 실행한다`() {
        val fn: () -> Int = { throw RuntimeException("error") }
        val result = fn.recover(
            resultHandler = { n: Int -> n * 2 },
            exceptionHandler = { _: Throwable? -> -1 }
        ).invoke()
        result shouldBeEqualTo -1
    }
}
