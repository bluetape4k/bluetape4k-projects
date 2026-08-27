package io.bluetape4k.examples.redisson.coroutines

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.redisson.api.RFuture
import org.redisson.misc.CompletableFutureWrapper
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * [AbstractRedissonCoroutineTest.awaitRedis]의 취소 및 timeout 경계를 합성 future로 검증합니다.
 */
class AwaitRedisTest : AbstractRedissonCoroutineTest() {

    private val subject = TestSubject()

    @Test
    fun `completed future returns value without cancellation`() = runTest {
        val future = RecordingRFuture<Int>()
        future.complete(42)

        subject.await(future) shouldBeEqualTo 42
        future.cancelCalls shouldBeEqualTo 0
    }

    @Test
    fun `failed future propagates original failure without cancellation`() = runTest {
        val failure = IllegalStateException("synthetic Redis failure")
        val future = RecordingRFuture<Int>()
        future.toCompletableFuture().completeExceptionally(failure)

        val observed = assertFailsWith<IllegalStateException> {
            subject.await(future)
        }

        observed.message shouldBeEqualTo failure.message
        future.cancelCalls shouldBeEqualTo 0
    }

    @Test
    fun `timeout cancels pending future with non interrupting flag`() = runTest {
        val future = RecordingRFuture<Int>()

        assertFailsWith<TimeoutCancellationException> {
            subject.await(future, 1.milliseconds)
        }

        future.cancelCalls shouldBeEqualTo 1
        future.lastMayInterruptIfRunning.shouldBeFalse()
        future.isCancelled.shouldBeTrue()
    }

    @Test
    fun `caller cancellation rethrows cancellation and cancels pending future`() = runTest {
        val future = RecordingRFuture<Int>()
        val observed = CompletableDeferred<Throwable>()
        val cancellation = CancellationException("caller cancelled Redis await")

        val pending = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                subject.await(future, 1.seconds)
            } catch (cause: Throwable) {
                observed.complete(cause)
                throw cause
            }
        }

        pending.cancel(cancellation)
        pending.join()

        val observedCancellation = observed.await().shouldBeInstanceOf<CancellationException>()
        observedCancellation.message shouldBeEqualTo cancellation.message
        future.cancelCalls shouldBeEqualTo 1
        future.lastMayInterruptIfRunning.shouldBeFalse()
        future.isCancelled.shouldBeTrue()
    }

    private class TestSubject : AbstractRedissonCoroutineTest() {
        suspend fun <T> await(future: RFuture<T>, timeout: Duration = 5.seconds): T =
            awaitRedis(future, timeout)
    }

    private class RecordingRFuture<T> : CompletableFutureWrapper<T>(CompletableFuture<T>()) {
        var cancelCalls: Int = 0
            private set

        var lastMayInterruptIfRunning: Boolean? = null
            private set

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            cancelCalls++
            lastMayInterruptIfRunning = mayInterruptIfRunning
            return super.cancel(mayInterruptIfRunning)
        }
    }
}
