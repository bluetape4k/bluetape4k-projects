package io.bluetape4k.concurrent

import io.bluetape4k.assertions.assertFails
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.fail
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
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
        // H2 수정 검증: timeout 내 완료 시 정상 결과 반환
        val future = futureOf { Thread.sleep(50); 42 }
        future.join(500.milliseconds, 0) shouldBeEqualTo 42
    }

    @Test
    fun `join with defaultValue returns default on timeout`() {
        // H2 수정 검증: timeout 시 defaultValue 반환
        val future = futureOf { Thread.sleep(2000); 42 }
        future.join(100.milliseconds, -1) shouldBeEqualTo -1
    }

    @Test
    fun `join with defaultValue propagates non-timeout exceptions`() {
        // H2 수정 검증: TimeoutException 이외의 예외는 rethrow
        val future = failedCompletableFutureOf<Int>(IllegalStateException("비즈니스 오류"))
        assertFailsWith<IllegalStateException> { future.join(500.milliseconds, 0) }
    }

    @Test
    fun `joinOrNull returns result when completed in time`() {
        // H2 수정 검증: timeout 내 완료 시 정상 결과 반환
        val future = futureOf { Thread.sleep(50); 42 }
        future.joinOrNull(500.milliseconds) shouldBeEqualTo 42
    }

    @Test
    fun `joinOrNull returns null on timeout`() {
        // H2 수정 검증: timeout 시 null 반환
        val future = futureOf { Thread.sleep(2000); 42 }
        future.joinOrNull(100.milliseconds) shouldBeEqualTo null
    }

    @Test
    fun `joinOrNull propagates non-timeout exceptions`() {
        // H2 수정 검증: TimeoutException 이외의 예외는 rethrow
        val future = failedCompletableFutureOf<Int>(IllegalStateException("비즈니스 오류"))
        assertFailsWith<ExecutionException> { future.joinOrNull(500.milliseconds) }
            .cause shouldBeInstanceOf IllegalStateException::class
    }
}
