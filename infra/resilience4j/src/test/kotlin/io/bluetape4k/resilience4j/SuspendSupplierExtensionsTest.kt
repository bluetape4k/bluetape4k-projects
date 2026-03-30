package io.bluetape4k.resilience4j

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CancellationException
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.test.assertFailsWith

class SuspendSupplierExtensionsTest {

    companion object: KLoggingChannel()

    @Nested
    inner class AndThenTests {

        @Test
        fun `andThen - 성공 시 결과를 변환한다`() = runSuspendTest {
            val fn: suspend () -> Int = { 42 }
            val mapped = fn.andThen { it.toString() }

            mapped() shouldBeEqualTo "42"
        }

        @Test
        fun `andThen with handler - 성공 시 result에 값이 전달된다`() = runSuspendTest {
            val fn: suspend () -> Int = { 42 }
            val mapped = fn.andThen { result, ex ->
                ex shouldBeEqualTo null
                result.toString()
            }

            mapped() shouldBeEqualTo "42"
        }

        @Test
        fun `andThen with handler - 예외 발생 시 ex에 예외가 전달된다`() = runSuspendTest {
            val fn: suspend () -> Int = { throw IOException("error") }
            val mapped = fn.andThen { result, ex ->
                if (ex != null) -1 else result!!
            }

            mapped() shouldBeEqualTo -1
        }

        @Test
        fun `andThen with resultHandler and exceptionHandler - 성공 시 resultHandler 호출`() = runSuspendTest {
            val fn: suspend () -> Int = { 10 }
            val mapped = fn.andThen(
                resultHandler = { (it * 2).toString() },
                exceptionHandler = { "error" }
            )

            mapped() shouldBeEqualTo "20"
        }

        @Test
        fun `andThen with resultHandler and exceptionHandler - 예외 시 exceptionHandler 호출`() = runSuspendTest {
            val fn: suspend () -> Int = { throw RuntimeException("fail") }
            val mapped = fn.andThen(
                resultHandler = { it.toString() },
                exceptionHandler = { "fallback" }
            )

            mapped() shouldBeEqualTo "fallback"
        }

        @Test
        fun `andThen with handler - 취소 예외는 그대로 전파한다`() = runSuspendTest {
            val fn: suspend () -> Int = { throw CancellationException("cancelled") }
            val mapped = fn.andThen { _: Int?, _: Throwable? -> -1 }

            assertFailsWith<CancellationException> {
                mapped()
            }
        }
    }

    @Nested
    inner class RecoverTests {

        @Test
        fun `recover with exceptionHandler - 예외가 발생하면 대체 값을 반환한다`() = runSuspendTest {
            val fn: suspend () -> Int = { throw RuntimeException("err") }
            val safe = fn.recover { -1 }

            safe() shouldBeEqualTo -1
        }

        @Test
        fun `recover with exceptionHandler - 예외가 없으면 원래 값을 반환한다`() = runSuspendTest {
            val fn: suspend () -> Int = { 42 }
            val safe = fn.recover { -1 }

            safe() shouldBeEqualTo 42
        }

        @Test
        fun `recover with resultPredicate - 조건 충족 시 대체 값을 반환한다`() = runSuspendTest {
            val fn: suspend () -> String = { "" }
            val safe = fn.recover(
                resultPredicate = { it.isEmpty() },
                resultHandler = { "default" }
            )

            safe() shouldBeEqualTo "default"
        }

        @Test
        fun `recover with resultPredicate - 조건 미충족 시 원래 값을 반환한다`() = runSuspendTest {
            val fn: suspend () -> String = { "hello" }
            val safe = fn.recover(
                resultPredicate = { it.isEmpty() },
                resultHandler = { "default" }
            )

            safe() shouldBeEqualTo "hello"
        }

        @Test
        fun `recover with exceptionType - 해당 예외 타입이면 대체 값을 반환한다`() = runSuspendTest {
            val fn: suspend () -> Int = { throw IOException("io error") }
            val safe = fn.recover(IOException::class) { -1 }

            safe() shouldBeEqualTo -1
        }

        @Test
        fun `recover with exceptionType - 다른 예외 타입이면 rethrow 된다`() = runSuspendTest {
            val fn: suspend () -> Int = { throw IllegalArgumentException("not io") }
            val safe = fn.recover(IOException::class) { -1 }

            val result = runCatching { safe() }
            result.isFailure.shouldBeTrue()
            (result.exceptionOrNull() is IllegalArgumentException).shouldBeTrue()
        }

        @Test
        fun `recover with exceptionTypes - 목록 예외 타입이면 대체 값을 반환한다`() = runSuspendTest {
            val fn: suspend () -> Int = { throw IOException("io error") }
            // 타입 파라미터 명시로 추론 해결
            val exTypes: Iterable<Class<IOException>> = listOf(IOException::class.java)
            val safe = fn.recover(exTypes) { -1 }

            safe() shouldBeEqualTo -1
        }

        @Test
        fun `recover with exceptionTypes - 목록 외 예외 타입이면 rethrow 된다`() = runSuspendTest {
            val fn: suspend () -> Int = { throw RuntimeException("other") }
            val exTypes: Iterable<Class<IOException>> = listOf(IOException::class.java)
            val safe = fn.recover(exTypes) { -1 }

            val result = runCatching { safe() }
            result.isFailure.shouldBeTrue()
            (result.exceptionOrNull() is RuntimeException).shouldBeTrue()
        }

        @Test
        fun `recover with exceptionHandler - 취소 예외는 복구하지 않고 전파한다`() = runSuspendTest {
            val fn: suspend () -> Int = { throw CancellationException("cancelled") }
            val safe = fn.recover { -1 }

            assertFailsWith<CancellationException> {
                safe()
            }
        }

        @Test
        fun `recover with exceptionType - 취소 예외는 타입이 일치해도 전파한다`() = runSuspendTest {
            val fn: suspend () -> Int = { throw CancellationException("cancelled") }
            val safe = fn.recover(CancellationException::class) { -1 }

            assertFailsWith<CancellationException> {
                safe()
            }
        }
    }

    @Nested
    inner class CompositionTests {

        @Test
        fun `andThen 후 recover 체인 동작 확인`() = runSuspendTest {
            val fn: suspend () -> Int = { 10 }
            val composed = fn
                .andThen { it * 2 }  // 20
                .recover { 0 }       // 예외 없으므로 20

            composed() shouldBeEqualTo 20
        }

        @Test
        fun `예외 발생 시 recover가 동작한다`() = runSuspendTest {
            var touched = false
            val fn: suspend () -> Int = { throw RuntimeException("err") }
            val composed = fn.recover { touched = true; -1 }

            composed() shouldBeEqualTo -1
            touched.shouldBeTrue()
        }

        @Test
        fun `결과 predicate 로 조건부 분기가 동작한다`() = runSuspendTest {
            val fn: suspend () -> Int = { 0 }
            val composed = fn.recover(
                resultPredicate = { it == 0 },
                resultHandler = { 99 }
            )

            composed() shouldBeEqualTo 99
        }

        @Test
        fun `정상 결과는 predicate 분기에 걸리지 않는다`() = runSuspendTest {
            val fn: suspend () -> Int = { 42 }
            val composed = fn.recover(
                resultPredicate = { it == 0 },
                resultHandler = { 99 }
            )

            composed() shouldBeEqualTo 42
        }
    }
}
