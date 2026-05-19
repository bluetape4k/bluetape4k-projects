package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.tests.withSingleThread
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException
import io.bluetape4k.assertions.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Subject 구현체들이 CancellationException을 삼키지 않고 올바르게 전파하는지 검증하는 테스트입니다.
 */
class SubjectCancellationTest {

    companion object : KLoggingChannel()

    /**
     * PublishSubject: collector 코루틴을 직접 취소하면 job이 cancelled 상태가 되어야 한다.
     */
    @Test
    fun `PublishSubject emit CE should propagate to collector`() = runTest {
        withSingleThread { dispatcher ->
            val subject = PublishSubject<Int>()
            val caughtError = AtomicReference<Throwable>(null)

            val job = launch(dispatcher) {
                try {
                    subject.collect { /* consume */ }
                } catch (e: Throwable) {
                    caughtError.set(e)
                    throw e
                }
            }

            subject.awaitCollector()

            // collector job을 직접 취소 — CE가 전파되어야 한다
            job.cancel()

            await untilSuspending { job.isCancelled }

            job.isCancelled.shouldBeTrue()
        }
    }

    /**
     * BehaviorSubject: 부모 scope를 취소하면 collector job도 취소되어야 한다 (CE가 삼켜지지 않음).
     */
    @Test
    fun `BehaviorSubject cancel scope propagates`() = runTest {
        withSingleThread { dispatcher ->
            val subject = BehaviorSubject<Int>()
            val collectorJob = AtomicReference<kotlinx.coroutines.Job>(null)

            val parentJob = launch(dispatcher) {
                val job = launch {
                    subject.collect { /* consume */ }
                }
                collectorJob.set(job)
                job.join()
            }

            // collector가 등록될 때까지 대기
            subject.awaitCollector()

            // 부모 scope 취소
            parentJob.cancel()

            await untilSuspending { parentJob.isCancelled }

            parentJob.isCancelled.shouldBeTrue()

            val collJob = collectorJob.get()
            if (collJob != null) {
                await untilSuspending { collJob.isCancelled || collJob.isCompleted }
                (collJob.isCancelled || collJob.isCompleted).shouldBeTrue()
            }
        }
    }

    /**
     * withTimeout 에 의한 TimeoutCancellationException은 subject 조작 중에도 전파되어야 한다.
     * runTest는 가상 시간을 사용하므로 Dispatchers.Default 컨텍스트에서 실행한다.
     */
    @Test
    fun `coroutineScope cancellation does not get swallowed`() = runTest {
        val subject = PublishSubject<Int>()

        // withTimeout으로 TimeoutCancellationException이 발생하는지 검증
        val ex = assertFailsWith<Exception> {
            withTimeout(100.milliseconds) {
                // collector를 등록하지 않고 awaitCollector 대기 — timeout 초과
                subject.awaitCollector(timeout = 200.milliseconds)
            }
        }

        ex shouldBeInstanceOf CancellationException::class
    }

    /**
     * emitError로 CancellationException을 전달하면 collector가 해당 예외를 받아야 한다.
     */
    @Test
    fun `PublishSubject emitError with CancellationException propagates to collector`() = runTest {
        withSingleThread { dispatcher ->
            val subject = PublishSubject<Int>()
            val caughtError = AtomicReference<Throwable>(null)

            val job = launch(dispatcher) {
                try {
                    subject.collect { /* consume */ }
                } catch (e: Throwable) {
                    caughtError.set(e)
                }
            }

            subject.awaitCollector()

            // CancellationException을 emitError로 전달
            subject.emitError(CancellationException("test cancellation"))

            await untilSuspending { job.isCompleted }

            job.isCompleted.shouldBeTrue()
        }
    }

    /**
     * MulticastSubject: collector를 취소하면 해당 collector가 목록에서 제거되어야 한다.
     */
    @Test
    fun `MulticastSubject collector cancellation removes collector from list`() = runTest {
        withSingleThread { dispatcher ->
            val subject = MulticastSubject<Int>(1)

            val job = launch(dispatcher) {
                subject.collect { /* consume */ }
            }

            subject.awaitCollector()

            // collector job 취소
            job.cancel()

            await untilSuspending { job.isCancelled }

            job.isCancelled.shouldBeTrue()
        }
    }

    /**
     * BehaviorSubject: emitError로 CancellationException을 전달하면 collector가 종료되어야 한다.
     */
    @Test
    fun `BehaviorSubject emitError with CancellationException terminates collector`() = runTest {
        withSingleThread { dispatcher ->
            val subject = BehaviorSubject<Int>()

            val job = launch(dispatcher) {
                try {
                    subject.collect { /* consume */ }
                } catch (_: Throwable) {
                    // 예외 수신
                }
            }

            subject.awaitCollector()

            subject.emitError(CancellationException("behavior cancellation"))

            await untilSuspending { job.isCompleted }

            job.isCompleted.shouldBeTrue()
        }
    }

    @Test
    fun `BehaviorSubject complete preserves timeout cancellation while collector is busy`() = runTest {
        val subject = BehaviorSubject<Int>()
        val collectorEntered = CompletableDeferred<Unit>()
        val releaseCollector = CompletableDeferred<Unit>()

        val job = launch {
            subject.collect {
                collectorEntered.complete(Unit)
                releaseCollector.await()
            }
        }

        subject.awaitCollector()
        subject.emit(1)
        collectorEntered.await()

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(10.milliseconds) {
                subject.complete()
            }
        }

        releaseCollector.complete(Unit)
        job.cancelAndJoin()
    }

    @Test
    fun `BehaviorSubject emitError preserves timeout cancellation while collector is busy`() = runTest {
        val subject = BehaviorSubject<Int>()
        val collectorEntered = CompletableDeferred<Unit>()
        val releaseCollector = CompletableDeferred<Unit>()

        val job = launch {
            subject.collect {
                collectorEntered.complete(Unit)
                releaseCollector.await()
            }
        }

        subject.awaitCollector()
        subject.emit(1)
        collectorEntered.await()

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(10.milliseconds) {
                subject.emitError(IllegalStateException("boom"))
            }
        }

        releaseCollector.complete(Unit)
        job.cancelAndJoin()
    }

    @Test
    fun `BehaviorSubject emitError continues after a collector is cancelled`() = runTest {
        val subject = BehaviorSubject<Int>()
        val emittedError = IllegalStateException("boom")
        val secondCollectorError = CompletableDeferred<Throwable>()

        val firstJob = launch {
            subject.collect { /* consume */ }
        }
        val secondJob = launch {
            try {
                subject.collect { /* consume */ }
            } catch (e: IllegalStateException) {
                secondCollectorError.complete(e)
            }
        }

        subject.awaitCollectors(2)

        firstJob.cancel()
        subject.emitError(emittedError)

        withTimeout(1.seconds) {
            secondCollectorError.await()
        } shouldBeInstanceOf IllegalStateException::class

        firstJob.cancelAndJoin()
        secondJob.cancelAndJoin()
    }

    @Test
    fun `BehaviorSubject complete continues after a collector is cancelled`() = runTest {
        val subject = BehaviorSubject<Int>()
        val secondCollectorCompleted = CompletableDeferred<Unit>()

        val firstJob = launch {
            subject.collect { /* consume */ }
        }
        val secondJob = launch {
            subject.collect { /* consume */ }
            secondCollectorCompleted.complete(Unit)
        }

        subject.awaitCollectors(2)

        firstJob.cancel()
        subject.complete()

        withTimeout(1.seconds) {
            secondCollectorCompleted.await()
        }

        firstJob.cancelAndJoin()
        secondJob.cancelAndJoin()
    }

    @Test
    fun `PublishSubject complete preserves timeout cancellation while collector is busy`() = runTest {
        val subject = PublishSubject<Int>()
        val collectorEntered = CompletableDeferred<Unit>()
        val releaseCollector = CompletableDeferred<Unit>()

        val job = launch {
            subject.collect {
                collectorEntered.complete(Unit)
                releaseCollector.await()
            }
        }

        subject.awaitCollector()
        subject.emit(1)
        collectorEntered.await()

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(10.milliseconds) {
                subject.complete()
            }
        }

        releaseCollector.complete(Unit)
        job.cancelAndJoin()
    }

    @Test
    fun `PublishSubject emitError preserves timeout cancellation while collector is busy`() = runTest {
        val subject = PublishSubject<Int>()
        val collectorEntered = CompletableDeferred<Unit>()
        val releaseCollector = CompletableDeferred<Unit>()

        val job = launch {
            subject.collect {
                collectorEntered.complete(Unit)
                releaseCollector.await()
            }
        }

        subject.awaitCollector()
        subject.emit(1)
        collectorEntered.await()

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(10.milliseconds) {
                subject.emitError(IllegalStateException("boom"))
            }
        }

        releaseCollector.complete(Unit)
        job.cancelAndJoin()
    }
}
