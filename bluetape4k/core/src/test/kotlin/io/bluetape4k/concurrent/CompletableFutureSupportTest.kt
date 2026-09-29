package io.bluetape4k.concurrent

import io.bluetape4k.assertions.assertFails
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.fail
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

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
        failed.flatMap { r -> immediateFutureOf { r + 1 } }.shouldCauseBe<IllegalArgumentException>()
    }

    @Test
    fun `mapResult exposes success and failure metadata`() {
        success.mapResult { value, error -> error == null && value == 1 }.get().shouldBeTrue()
        failed.mapResult { value, error -> value == null && error is IllegalArgumentException }
            .get().shouldBeTrue()
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
            fail("성공한 future에 대해 onFailure가 호출되면 안됩니다.", e)
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

        failed.onSuccess { error("onSuccess must not be called on a failed future") }.recover { 1 }
            .get() shouldBeEqualTo 1
    }

    @Test
    fun `onComplete with handlers fires appropriate callback`() {
        var onSuccessCalled = false;
        var onFailureCalled = false
        success.onComplete(
            DirectExecutor,
            successHandler = { onSuccessCalled = true },
            failureHandler = { onFailureCalled = true })
            .get() shouldBeEqualTo 1
        onSuccessCalled.shouldBeTrue(); onFailureCalled.shouldBeFalse()

        onSuccessCalled = false; onFailureCalled = false
        failed.onComplete(
            DirectExecutor,
            successHandler = { onSuccessCalled = true },
            failureHandler = { onFailureCalled = true })
            .recover { 1 }.get() shouldBeEqualTo 1
        onSuccessCalled.shouldBeFalse(); onFailureCalled.shouldBeTrue()
    }

    @Test
    fun `onComplete with completion callback fires appropriately`() {
        var onSuccessCalled = false;
        var onFailureCalled = false
        success.onComplete(DirectExecutor) { _, error ->
            if (error == null) onSuccessCalled = true else onFailureCalled = true
        }
            .get() shouldBeEqualTo 1
        onSuccessCalled.shouldBeTrue(); onFailureCalled.shouldBeFalse()

        onSuccessCalled = false; onFailureCalled = false
        failed.onComplete(DirectExecutor) { _, error ->
            if (error == null) onSuccessCalled = true else onFailureCalled = true
        }
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
        // 의도적인 blocking 경계: worker의 실제 지연과 Future.get 결과로 timeout 계약을 검증한다.
        // runTest나 가상 시간 tester로 치환하면 CompletableFuture scheduler 의미가 달라진다.
        futureWithTimeout(500L) { Thread.sleep(50); 42 }.get() shouldBeEqualTo 42
        futureWithTimeout(50L) { Thread.sleep(3000); 42 }.shouldCauseBe<TimeoutException>()
        futureWithTimeout(500.milliseconds) { Thread.sleep(50); "hello" }.get() shouldBeEqualTo "hello"
    }

    @Test
    fun `dereference unwraps nested completable future`() {
        futureOf { completableFutureOf(42) }.dereference().get() shouldBeEqualTo 42
        futureOf { failedCompletableFutureOf<Int>(RuntimeException("boom")) }.dereference()
            .shouldCauseBe<RuntimeException>()
    }

    @Test
    fun `join with defaultValue returns result when completed in time`() {
        completableFutureOf(42).join(500.milliseconds, 0) shouldBeEqualTo 42
    }

    @Test
    fun `join with defaultValue returns default on timeout`() {
        CompletableFuture<Int>().join(1.nanoseconds, -1) shouldBeEqualTo -1
    }

    @Test
    fun `join with defaultValue propagates non-timeout exceptions`() {
        // H2 수정 검증: TimeoutException 이외의 예외는 rethrow
        val future = failedCompletableFutureOf<Int>(IllegalStateException("비즈니스 오류"))
        assertFailsWith<IllegalStateException> {
            future.join(500.milliseconds, 0)
        }.message shouldBeEqualTo "비즈니스 오류"
    }

    @Test
    fun `join with defaultValue propagates business timeout exception`() {
        val businessTimeout = TimeoutException("business timeout")
        val future = failedCompletableFutureOf<Int>(businessTimeout)

        assertFailsWith<TimeoutException> {
            future.join(1.seconds, -1)
        }.message shouldBeEqualTo "business timeout"
    }

    @Test
    fun `joinOrNull returns result when completed in time`() {
        completableFutureOf(42).joinOrNull(500.milliseconds) shouldBeEqualTo 42
    }

    @Test
    fun `joinOrNull returns null on timeout`() {
        CompletableFuture<Int>().joinOrNull(1.nanoseconds) shouldBeEqualTo null
    }

    @Test
    fun `joinOrNull propagates non-timeout exceptions`() {
        // H2 수정 검증: TimeoutException 이외의 예외는 rethrow
        val future = failedCompletableFutureOf<Int>(IllegalStateException("비즈니스 오류"))
        assertFailsWith<IllegalStateException> {
            future.joinOrNull(500.milliseconds)
        }.message shouldBeEqualTo "비즈니스 오류"
    }

    @Test
    fun `nullable completed values follow each overload fallback contract`() {
        val completed = completableFutureOf<Int?>(42)
        completed.get(1.seconds) shouldBeEqualTo 42
        completed.get(1.seconds, -1) shouldBeEqualTo 42
        completed.getOrNull(1.seconds) shouldBeEqualTo 42
        completed.join(1.seconds) shouldBeEqualTo 42
        completed.join(1.seconds, -1) shouldBeEqualTo 42
        completed.joinOrNull(1.seconds) shouldBeEqualTo 42

        val completedNull = completableFutureOf<Int?>(null)
        completedNull.get(1.seconds) shouldBeEqualTo null
        completedNull.get(1.seconds, -1) shouldBeEqualTo null
        completedNull.getOrNull(1.seconds) shouldBeEqualTo null
        completedNull.join(1.seconds) shouldBeEqualTo null
        completedNull.join(1.seconds, -1) shouldBeEqualTo -1
        completedNull.joinOrNull(1.seconds) shouldBeEqualTo null
    }

    @Test
    fun `zero and negative duration return completed value and immediately time out pending future`() {
        val completed = completableFutureOf(42)
        completed.get(Duration.ZERO) shouldBeEqualTo 42
        completed.join(Duration.ZERO) shouldBeEqualTo 42
        completed.get(-1.nanoseconds) shouldBeEqualTo 42
        completed.join(-1.nanoseconds) shouldBeEqualTo 42

        val pending = CompletableFuture<Int>()
        assertFailsWith<TimeoutException> { pending.get(Duration.ZERO) }
        pending.get(Duration.ZERO, -1) shouldBeEqualTo -1
        pending.getOrNull(Duration.ZERO) shouldBeEqualTo null
        assertFailsWith<TimeoutException> { pending.join(Duration.ZERO) }
        pending.join(Duration.ZERO, -1) shouldBeEqualTo -1
        pending.joinOrNull(Duration.ZERO) shouldBeEqualTo null
        pending.isCancelled.shouldBeFalse()
    }

    @Test
    fun `duration infinite is passed to timed get as saturated nanoseconds`() {
        val future = RecordingFuture(42)

        future.get(Duration.INFINITE) shouldBeEqualTo 42
        future.timeout shouldBeEqualTo Long.MAX_VALUE
        future.unit shouldBeEqualTo TimeUnit.NANOSECONDS

        future.get(Duration.INFINITE, -1) shouldBeEqualTo 42
        future.getOrNull(Duration.INFINITE) shouldBeEqualTo 42
        future.join(Duration.INFINITE) shouldBeEqualTo 42
        future.join(Duration.INFINITE, -1) shouldBeEqualTo 42
        future.joinOrNull(Duration.INFINITE) shouldBeEqualTo 42
        future.timeout shouldBeEqualTo Long.MAX_VALUE
        future.unit shouldBeEqualTo TimeUnit.NANOSECONDS
    }

    @Test
    fun `only a wait timeout is converted and timed out future remains usable`() {
        val pending = CompletableFuture<Int>()
        assertFailsWith<TimeoutException> { pending.get(1.nanoseconds) }
        pending.isCancelled.shouldBeFalse()
        pending.complete(42).shouldBeTrue()
        pending.get() shouldBeEqualTo 42

        val businessTimeout = failedCompletableFutureOf<Int>(TimeoutException("business timeout"))
        assertFailsWith<TimeoutException> { businessTimeout.join(1.seconds, -1) }
            .message shouldBeEqualTo "business timeout"

        val cancelled = CompletableFuture<Int>().also { it.cancel(true) }
        assertFailsWith<CancellationException> { cancelled.get(1.seconds) }
        assertFailsWith<CancellationException> { cancelled.join(1.seconds) }
    }

    @Test
    fun `duration overloads preserve business failures and cancellation`() {
        val failure = IllegalStateException("business failure")
        val failed = failedCompletableFutureOf<Int>(failure)
        val getOperations: List<(CompletableFuture<Int>) -> Any?> = listOf(
            { it.get(1.seconds) },
            { it.get(1.seconds, -1) },
            { it.getOrNull(1.seconds) },
        )
        getOperations.forEach { operation ->
            assertFailsWith<ExecutionException> { operation(failed) }.cause shouldBeEqualTo failure
        }

        val joinOperations: List<(CompletableFuture<Int>) -> Any?> = listOf(
            { it.join(1.seconds) },
            { it.join(1.seconds, -1) },
            { it.joinOrNull(1.seconds) },
        )
        joinOperations.forEach { operation ->
            assertFailsWith<IllegalStateException> { operation(failed) }.message shouldBeEqualTo failure.message
        }

        val businessTimeout = failedCompletableFutureOf<Int>(TimeoutException("business timeout"))
        joinOperations.forEach { operation ->
            assertFailsWith<TimeoutException> { operation(businessTimeout) }
                .message shouldBeEqualTo "business timeout"
        }

        val cancelled = CompletableFuture<Int>().also { it.cancel(true) }
        (getOperations + joinOperations).forEach { operation ->
            assertFailsWith<CancellationException> { operation(cancelled) }
        }
    }

    @Test
    fun `all duration get and join extensions propagate waiting thread interruption`() {
        val operations: List<(CompletableFuture<Int>) -> Any?> = listOf(
            { it.get(1.seconds) },
            { it.get(1.seconds, -1) },
            { it.getOrNull(1.seconds) },
            { it.join(1.seconds) },
            { it.join(1.seconds, -1) },
            { it.joinOrNull(1.seconds) },
        )

        operations.forEach { operation ->
            val future = CompletableFuture<Int>()
            val entered = CountDownLatch(1)
            val failure = AtomicReference<Throwable?>()
            val waiter = Thread {
                entered.countDown()
                try {
                    operation(future)
                } catch (error: Throwable) {
                    failure.set(error)
                }
            }

            waiter.start()
            entered.await(5, TimeUnit.SECONDS).shouldBeTrue()
            waiter.interrupt()
            waiter.join(TimeUnit.SECONDS.toMillis(5))
            waiter.isAlive.shouldBeFalse()
            failure.get().shouldNotBeNull().shouldBeInstanceOf(InterruptedException::class)
        }
    }

    private class RecordingFuture<T>(private val result: T): CompletableFuture<T>() {
        var timeout: Long = 0
        var unit: TimeUnit? = null

        override fun get(timeout: Long, unit: TimeUnit): T {
            this.timeout = timeout
            this.unit = unit
            return result
        }
    }
}
