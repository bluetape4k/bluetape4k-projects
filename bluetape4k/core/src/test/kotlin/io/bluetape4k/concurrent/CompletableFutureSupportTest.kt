package io.bluetape4k.concurrent

import kotlinx.atomicfu.atomic
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlin.test.assertFails

/**
 * [CompletableFuture] 관련 함수를 테스트합니다.
 */
class CompletableFutureSupportTest {

    private val success: CompletableFuture<Int> = completableFutureOf(1)
    private val failed: CompletableFuture<Int> = failedCompletableFutureOf(IllegalArgumentException())
    private val emptyFutures: List<CompletableFuture<Int>> = emptyList()

    @Nested
    inner class Map {
        @Test
        fun `map success future should success`() {
            success.map { it + 1 }.get() shouldBeEqualTo 2
        }

        @Test
        fun `map failed future throw exception`() {
            assertFails {
                failed.map { it + 1 }.get()
            }.cause shouldBeInstanceOf IllegalArgumentException::class
        }
    }

    @Nested
    inner class FlatMap {
        @Test
        fun `flatMap success future should success`() {
            success.flatMap { r -> immediateFutureOf { r + 1 } }.get() shouldBeEqualTo 2
        }

        @Test
        fun `flatMap failed future should throw exception`() {
            assertFailsWith<ExecutionException> {
                failed.map { r -> immediateFutureOf { r + 1 } }.get()
            }.cause shouldBeInstanceOf IllegalArgumentException::class
        }
    }

    @Nested
    inner class Flatten {
        @Test
        fun `flatten success future`() {
            futureOf { success }.flatten().get() shouldBeEqualTo 1
        }

        @Test
        fun `flatten failed future`() {
            assertFailsWith<ExecutionException> {
                futureOf { failed }.flatten().get()
            }.cause shouldBeInstanceOf IllegalArgumentException::class
        }
    }

    @Nested
    inner class Filter {
        @Test
        fun `filter success future with match`() {
            success.filter { it == 1 }.get() shouldBeEqualTo 1
        }

        @Test
        fun `filter success future without match`() {
            assertFailsWith<ExecutionException> {
                success.filter { it == 2 }.get()
            }.cause shouldBeInstanceOf NoSuchElementException::class
        }

        @Test
        fun `filter failed future`() {
            assertFailsWith<ExecutionException> {
                failed.filter { it == 1 }.get()
            }.cause shouldBeInstanceOf IllegalArgumentException::class
        }
    }

    @Nested
    inner class Recover {
        @Test
        fun `recover success future`() {
            success.recover { 2 }.get() shouldBeEqualTo 1
        }

        @Test
        fun `recover failed future`() {
            failed.recover { 2 }.get() shouldBeEqualTo 2
        }
    }

    @Nested
    inner class RecoverWith {
        @Test
        fun `recoverWith success future`() {
            success.recoverWith { immediateFutureOf { 2 } }.get() shouldBeEqualTo 1
        }

        @Test
        fun `recoverWith failed future`() {
            failed.recoverWith { immediateFutureOf { 2 } }.get() shouldBeEqualTo 2
        }
    }

    @Nested
    inner class FallbackTo {
        @Test
        fun `fallbackTo success future`() {
            success.fallbackTo { immediateFutureOf { 2 } }.get() shouldBeEqualTo 1
        }

        @Test
        fun `fallbackTo failed future`() {
            failed.fallbackTo { immediateFutureOf { 2 } }.get() shouldBeEqualTo 2
        }
    }

    @Nested
    inner class MapError {
        @Test
        fun `mapError success future`() {
            success.mapError<Int, Exception> { IllegalStateException("mapError") }.get() shouldBeEqualTo 1
        }

        @Test
        fun `mapError failed future`() {
            assertFails {
                failed.mapError<Int, IllegalArgumentException> { UnsupportedOperationException() }.get()
            }.cause shouldBeInstanceOf UnsupportedOperationException::class
        }

        @Test
        fun `mapError failed future not expected exception`() {
            assertFails {
                failed.mapError<Int, ClassNotFoundException> { UnsupportedOperationException() }.get()
            }.cause shouldBeInstanceOf IllegalArgumentException::class
        }

        @Test
        fun `mapError handle supertype exception`() {
            assertFails {
                failed.mapError<Int, Exception> { UnsupportedOperationException() }.get()
            }.cause shouldBeInstanceOf UnsupportedOperationException::class
        }
    }

    @Nested
    inner class OnFailure {
        @Test
        fun `onFailure callback for success future`() {
            success
                .onFailure(DirectExecutor) { e ->
                    Assertions.fail("성공한 future에 대해 onFailure가 호출되면 안됩니다.", e)
                }
                .get() shouldBeEqualTo 1
        }

        @Test
        fun `onFailure callback for failed future`() {
            var capturedThrowable: Throwable? = null

            failed
                .onFailure(DirectExecutor) { capturedThrowable = it }
                .recover { 1 }
                .get() shouldBeEqualTo 1

            capturedThrowable.shouldNotBeNull().shouldBeInstanceOf(IllegalArgumentException::class)
        }
    }

    @Nested
    inner class OnSuccess {
        @Test
        fun `onSuccess callback with success future`() {
            val capturedResult = atomic(0)
            success.onSuccess(DirectExecutor) { capturedResult.value = it }.get()
            capturedResult.value shouldBeEqualTo 1
        }

        @Test
        fun `onSucess callback with failed future`() {
            failed
                .onSuccess { error("onSuccess must not be called on a failed future") }
                .recover { 1 }
                .get() shouldBeEqualTo 1
        }
    }

    @Nested
    inner class OnComplete {
        @Test
        fun `onComplete callback with success future`() {
            var onSuccessCalled = false
            var onFailureCalled = false

            success.onComplete(
                DirectExecutor,
                successHandler = { onSuccessCalled = true },
                failureHandler = { onFailureCalled = true })
                .get() shouldBeEqualTo 1

            onSuccessCalled.shouldBeTrue()
            onFailureCalled.shouldBeFalse()
        }

        @Test
        fun `onComplete callback with failed future`() {
            var onSuccessCalled = false
            var onFailureCalled = false

            failed.onComplete(
                DirectExecutor,
                successHandler = { onSuccessCalled = true },
                failureHandler = { onFailureCalled = true })
                .recover { 1 }
                .get() shouldBeEqualTo 1

            onSuccessCalled.shouldBeFalse()
            onFailureCalled.shouldBeTrue()
        }

        @Test
        fun `onComplete completion callback with success future`() {
            var onSuccessCalled = false
            var onFailureCalled = false

            success.onComplete(DirectExecutor) { _, error ->
                when (error) {
                    null -> onSuccessCalled = true
                    else -> onFailureCalled = true
                }
            }
                .get() shouldBeEqualTo 1

            onSuccessCalled.shouldBeTrue()
            onFailureCalled.shouldBeFalse()
        }

        @Test
        fun `onComplete completion callback with failed future`() {
            var onSuccessCalled = false
            var onFailureCalled = false

            failed.onComplete(DirectExecutor) { _, error ->
                when (error) {
                    null -> onSuccessCalled = true
                    else -> onFailureCalled = true
                }
            }
                .recover { 1 }
                .get() shouldBeEqualTo 1

            onSuccessCalled.shouldBeFalse()
            onFailureCalled.shouldBeTrue()
        }
    }

    @Nested
    inner class Zip {
        @Test
        fun `zip with success futures`() {
            success.zip(success).get() shouldBeEqualTo (1 to 1)
            success.zip(immediateFutureOf { "Success" }).get() shouldBeEqualTo (1 to "Success")
        }

        @Test
        fun `zip with failed future`() {
            assertFailsWith<ExecutionException> {
                failed.zip(failed) { a, b -> a + b }.get()
            }.cause shouldBeInstanceOf IllegalArgumentException::class

            assertFailsWith<ExecutionException> {
                success.zip(failed) { a, b -> a + b }.get()
            }.cause shouldBeInstanceOf IllegalArgumentException::class

            assertFailsWith<ExecutionException> {
                failed.zip(success) { a, b -> a + b }.get()
            }.cause shouldBeInstanceOf IllegalArgumentException::class
        }
    }
}
