package io.bluetape4k.coroutines.support

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DeferredSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `zip은 두 deferred 결과를 결합한다`() = runTest {
        val d1 = async { 10 }
        val d2 = async { 20 }

        val zipped = zip(d1, d2) { a, b -> a + b }
        zipped.await() shouldBeEqualTo 30
    }

    @Test
    fun `zipWith은 두 deferred 결과를 결합한다`() = runTest {
        val d1 = async { 10 }
        val d2 = async { 20 }

        val zipped = d1.zipWith(d2) { a, b -> a + b }
        zipped.await() shouldBeEqualTo 30
    }

    @Test
    fun `map, mapAll, concatMap은 deferred 결과를 변환한다`() = runTest {
        val source: Deferred<List<Int>> = async { listOf(1, 2, 3) }

        source.mapAll { listOf(it, it * 10) }.await() shouldBeEqualTo listOf(1, 10, 2, 20, 3, 30)
        source.concatMap { it * 2 }.await() shouldBeEqualTo listOf(2, 4, 6)
        source.map { it.sum() }.await() shouldBeEqualTo 6
    }

    @Test
    fun `awaitAny는 먼저 완료되는 deferred 값을 반환한다`() = runTest {
        val slow = async { delay(100.milliseconds); 2 }
        val fast = async { delay(10.milliseconds); 1 }

        awaitAny(slow, fast) shouldBeEqualTo 1
        listOf(slow, fast).awaitAny() shouldBeEqualTo 1
    }

    @Test
    fun `awaitAny는 첫 완료가 실패면 예외를 전파하고 나머지는 취소하지 않는다`() = runTest {
        val failure = IllegalStateException("boom")
        val first = CompletableDeferred<Int>()
        val second = CompletableDeferred<Int>()

        launch { first.completeExceptionally(failure) }

        val thrown = assertFailsWith<IllegalStateException> {
            listOf(first, second).awaitAny()
        }

        thrown.message shouldBeEqualTo failure.message
        second.isCancelled.shouldBeFalse()
    }

    @Test
    fun `awaitAny는 단일 deferred인 경우 바로 await 한다`() = runTest {
        val only = async { 7 }

        listOf(only).awaitAny() shouldBeEqualTo 7
        listOf(only).awaitAnyAndCancelOthers() shouldBeEqualTo 7
    }

    @Test
    fun `awaitAny 계열은 빈 입력을 허용하지 않는다`() = runTest {
        assertFailsWith<IllegalArgumentException> { awaitAny<Int>() }
        assertFailsWith<IllegalArgumentException> { emptyList<CompletableDeferred<Int>>().awaitAny() }
        assertFailsWith<IllegalArgumentException> { emptyList<CompletableDeferred<Int>>().awaitAnyAndCancelOthers() }
    }

    @Test
    fun `awaitAnyAndCancelOthers는 첫 완료값 반환 후 나머지를 취소한다`() = runTest {
        val first = CompletableDeferred(1)
        val second = CompletableDeferred<Int>()
        val third = CompletableDeferred<Int>()

        val result = listOf(first, second, third).awaitAnyAndCancelOthers()
        result shouldBeEqualTo 1

        second.isCancelled.shouldBeTrue()
        third.isCancelled.shouldBeTrue()
        first.isCompleted.shouldBeTrue()
    }

    @Test
    fun `awaitAnyAndCancelOthers는 첫 완료가 실패여도 나머지를 취소한다`() = runTest {
        val failure = IllegalStateException("boom")
        val first = CompletableDeferred<Int>()
        val second = CompletableDeferred<Int>()
        val third = CompletableDeferred<Int>()

        launch { first.completeExceptionally(failure) }

        val thrown = assertFailsWith<IllegalStateException> {
            listOf(first, second, third).awaitAnyAndCancelOthers()
        }

        thrown.message shouldBeEqualTo failure.message
        second.isCancelled.shouldBeTrue()
        third.isCancelled.shouldBeTrue()
    }

    @Test
    fun `awaitAnyAndCancelOthers는 첫 완료가 취소여도 나머지를 취소한다`() = runTest {
        val first = CompletableDeferred<Int>()
        val second = CompletableDeferred<Int>()
        val third = CompletableDeferred<Int>()

        launch { first.cancel(CancellationException("cancelled")) }

        assertFailsWith<CancellationException> {
            listOf(first, second, third).awaitAnyAndCancelOthers()
        }

        second.isCancelled.shouldBeTrue()
        third.isCancelled.shouldBeTrue()
    }

    @Test
    fun `awaitUntil returns completed values and propagates failures`() = runTest {
        CompletableDeferred(42).awaitUntil(1.seconds) shouldBeEqualTo 42

        val failure = IllegalStateException("source failed")
        val failed = CompletableDeferred<Int>().apply { completeExceptionally(failure) }
        assertFailsWith<IllegalStateException> { failed.awaitUntil(1.seconds) }
    }

    @Test
    fun `awaitUntil timeout does not cancel source and source can complete later`() = runTest {
        val source = CompletableDeferred<Int>()

        assertFailsWith<TimeoutCancellationException> { source.awaitUntil(100.milliseconds) }

        source.isCancelled.shouldBeFalse()
        source.complete(42).shouldBeTrue()
        source.awaitUntil(1.seconds) shouldBeEqualTo 42
    }

    @Test
    fun `awaitUntilOrNull returns null for timeout and nullable source values`() = runTest {
        val pending = CompletableDeferred<Int?>()
        val actualNull = CompletableDeferred<Int?>().apply { complete(null) }

        pending.awaitUntilOrNull(100.milliseconds) shouldBeEqualTo null
        actualNull.awaitUntilOrNull(1.seconds) shouldBeEqualTo null
        pending.isCancelled.shouldBeFalse()
        pending.complete(42).shouldBeTrue()
    }

    @Test
    fun `default timeout is five seconds`() = runTest {
        val timed = CompletableDeferred<Int>()
        val nullableTimed = CompletableDeferred<Int?>()
        val timedWaiter = async { timed.awaitUntil() }
        val nullableWaiter = async { nullableTimed.awaitUntilOrNull() }

        advanceTimeBy(5.seconds)
        runCurrent()

        assertFailsWith<TimeoutCancellationException> { timedWaiter.await() }
        nullableWaiter.await() shouldBeEqualTo null
        timed.isCancelled.shouldBeFalse()
        nullableTimed.isCancelled.shouldBeFalse()
    }

    @Test
    fun `infinite timeout waits until the source completes`() = runTest {
        val source = CompletableDeferred<Int>()
        val waiter = async { source.awaitUntil(Duration.INFINITE) }

        source.complete(42).shouldBeTrue()
        waiter.await() shouldBeEqualTo 42
    }

    @Test
    fun `caller cancellation cancels only the await waiter`() = runTest {
        val source = CompletableDeferred<Int>()
        val waiter = launch { source.awaitUntil(Duration.INFINITE) }

        waiter.cancelAndJoin()

        source.isCancelled.shouldBeFalse()
        source.complete(42).shouldBeTrue()
    }

    @Test
    fun `already cancelled source preserves cancellation`() = runTest {
        val source = CompletableDeferred<Int>().apply { cancel(CancellationException("source cancelled")) }

        assertFailsWith<CancellationException> { source.awaitUntil(1.seconds) }
    }

    @Test
    fun `zero negative and nested timeouts cancel only the waiter`() = runTest {
        val source = CompletableDeferred<Int>()

        assertFailsWith<TimeoutCancellationException> { source.awaitUntil(Duration.ZERO) }
        assertFailsWith<TimeoutCancellationException> { source.awaitUntil(-1.milliseconds) }
        source.awaitUntilOrNull(Duration.ZERO) shouldBeEqualTo null

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(100.milliseconds) { source.awaitUntil(Duration.INFINITE) }
        }

        source.isCancelled.shouldBeFalse()
        source.complete(42).shouldBeTrue()
    }
}
