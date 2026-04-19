package io.bluetape4k.concurrent

import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFails
import kotlin.time.Duration.Companion.milliseconds

/**
 * [CompletableFuture] 관련 함수를 테스트합니다.
 */
class CompletableFutureSupportTest {

    private val success: CompletableFuture<Int> = completableFutureOf(1)
    private val failed: CompletableFuture<Int> = failedCompletableFutureOf(IllegalArgumentException())

    private inline fun <reified T: Throwable> CompletableFuture<*>.shouldCauseBe() {
        assertFailsWith<ExecutionException> { get() }.cause shouldBeInstanceOf T::class
    }

    @Test
    fun `map transforms success and propagates failure`() {
        success.map { it + 1 }.get() shouldBeEqualTo 2
        assertFails { failed.map { it + 1 }.get() }.cause shouldBeInstanceOf IllegalArgumentException::class
    }

    @Test
    fun `flatMap transforms success and propagates failure`() {
        success.flatMap { r -> immediateFutureOf { r + 1 } }.get() shouldBeEqualTo 2
        failed.map { r -> immediateFutureOf { r + 1 } }.shouldCauseBe<IllegalArgumentException>()
    }

    @Test
    fun `flatten unwraps nested future`() {
        futureOf { success }.flatten().get() shouldBeEqualTo 1
        futureOf { failed }.flatten().shouldCauseBe<IllegalArgumentException>()
    }

    @Test
    fun `filter keeps matching value and throws on mismatch or failure`() {
        success.filter { it == 1 }.get() shouldBeEqualTo 1
        success.filter { it == 2 }.shouldCauseBe<NoSuchElementException>()
        failed.filter { it == 1 }.shouldCauseBe<IllegalArgumentException>()
    }

    @Test
    fun `recover and recoverWith return fallback on failure`() {
        success.recover { 2 }.get() shouldBeEqualTo 1
        failed.recover { 2 }.get() shouldBeEqualTo 2
        success.recoverWith { immediateFutureOf { 2 } }.get() shouldBeEqualTo 1
        failed.recoverWith { immediateFutureOf { 2 } }.get() shouldBeEqualTo 2
    }

    @Test
    fun `fallbackTo returns primary on success and fallback on failure`() {
        success.fallbackTo { immediateFutureOf { 2 } }.get() shouldBeEqualTo 1
        failed.fallbackTo { immediateFutureOf { 2 } }.get() shouldBeEqualTo 2
    }

    @Test
    fun `mapError transforms matching exception type`() {
        success.mapError<Int, Exception> { IllegalStateException("mapError") }.get() shouldBeEqualTo 1
        assertFails {
            failed.mapError<Int, IllegalArgumentException> { UnsupportedOperationException() }.get()
        }.cause shouldBeInstanceOf UnsupportedOperationException::class
        assertFails {
            failed.mapError<Int, ClassNotFoundException> { UnsupportedOperationException() }.get()
        }.cause shouldBeInstanceOf IllegalArgumentException::class
        assertFails {
            failed.mapError<Int, Exception> { UnsupportedOperationException() }.get()
        }.cause shouldBeInstanceOf UnsupportedOperationException::class
    }

    @Test
    fun `onFailure callback fires only on failure`() {
        success.onFailure(DirectExecutor) { e ->
            Assertions.fail("성공한 future에 대해 onFailure가 호출되면 안됩니다.", e)
        }.get() shouldBeEqualTo 1

        var capturedThrowable: Throwable? = null
        failed.onFailure(DirectExecutor) { capturedThrowable = it }.recover { 1 }.get() shouldBeEqualTo 1
        capturedThrowable.shouldNotBeNull().shouldBeInstanceOf(IllegalArgumentException::class)
    }

    @Test
    fun `onSuccess callback fires only on success`() {
        val capturedResult = AtomicInteger(0)
        success.onSuccess(DirectExecutor) { capturedResult.set(it) }.get()
        capturedResult.get() shouldBeEqualTo 1

        failed.onSuccess { error("onSuccess must not be called on a failed future") }.recover { 1 }.get() shouldBeEqualTo 1
    }

    @Test
    fun `onComplete with handlers fires appropriate callback`() {
        var onSuccessCalled = false; var onFailureCalled = false
        success.onComplete(DirectExecutor, successHandler = { onSuccessCalled = true }, failureHandler = { onFailureCalled = true })
            .get() shouldBeEqualTo 1
        onSuccessCalled.shouldBeTrue(); onFailureCalled.shouldBeFalse()

        onSuccessCalled = false; onFailureCalled = false
        failed.onComplete(DirectExecutor, successHandler = { onSuccessCalled = true }, failureHandler = { onFailureCalled = true })
            .recover { 1 }.get() shouldBeEqualTo 1
        onSuccessCalled.shouldBeFalse(); onFailureCalled.shouldBeTrue()
    }

    @Test
    fun `onComplete with completion callback fires appropriately`() {
        var onSuccessCalled = false; var onFailureCalled = false
        success.onComplete(DirectExecutor) { _, error -> if (error == null) onSuccessCalled = true else onFailureCalled = true }
            .get() shouldBeEqualTo 1
        onSuccessCalled.shouldBeTrue(); onFailureCalled.shouldBeFalse()

        onSuccessCalled = false; onFailureCalled = false
        failed.onComplete(DirectExecutor) { _, error -> if (error == null) onSuccessCalled = true else onFailureCalled = true }
            .recover { 1 }.get() shouldBeEqualTo 1
        onSuccessCalled.shouldBeFalse(); onFailureCalled.shouldBeTrue()
    }

    @Test
    fun `zip combines two futures`() {
        success.zip(success).get() shouldBeEqualTo (1 to 1)
        success.zip(immediateFutureOf { "Success" }).get() shouldBeEqualTo (1 to "Success")
        failed.zip(failed) { a, b -> a + b }.shouldCauseBe<IllegalArgumentException>()
        success.zip(failed) { a, b -> a + b }.shouldCauseBe<IllegalArgumentException>()
        failed.zip(success) { a, b -> a + b }.shouldCauseBe<IllegalArgumentException>()
    }

    @Test
    fun `isSuccess and isFailed reflect completion state`() {
        success.isSuccess.shouldBeTrue(); success.isFailed.shouldBeFalse()
        failed.isFailed.shouldBeTrue(); failed.isSuccess.shouldBeFalse()
        val pending = CompletableFuture<Int>()
        pending.isSuccess.shouldBeFalse(); pending.isFailed.shouldBeFalse()
        val cancelled = CompletableFuture<Int>().also { it.cancel(true) }
        cancelled.isSuccess.shouldBeFalse(); cancelled.isFailed.shouldBeTrue()
    }

    @Test
    fun `futureWithTimeout completes within limit or throws TimeoutException`() {
        futureWithTimeout(500L) { Thread.sleep(50); 42 }.get() shouldBeEqualTo 42
        futureWithTimeout(50L) { Thread.sleep(3000); 42 }.shouldCauseBe<TimeoutException>()
        futureWithTimeout(500.milliseconds) { Thread.sleep(50); "hello" }.get() shouldBeEqualTo "hello"
    }

    @Test
    fun `dereference unwraps nested completable future`() {
        futureOf { completableFutureOf(42) }.dereference().get() shouldBeEqualTo 42
        futureOf { failedCompletableFutureOf<Int>(RuntimeException("boom")) }.dereference().shouldCauseBe<RuntimeException>()
    }
}
