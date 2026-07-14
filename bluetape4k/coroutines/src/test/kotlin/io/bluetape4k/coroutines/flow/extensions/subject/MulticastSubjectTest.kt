package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.junit5.coroutines.withSingleThread
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MulticastSubjectTest {

    companion object: KLoggingChannel()

    @Test
    fun `1개의 collector 가 등록될 때까지 producer가 대기합니다`() = runTest {
        val subject = MulticastSubject<Int>(1)
        val result = ConcurrentLinkedQueue<Int>()

        withSingleThread { dispatcher ->
            val job = launch(dispatcher) {
                subject
                    .onEach { delay(10.milliseconds) }
                    .log("#1")
                    .collect { result.add(it) }
            }.log("job")

            // collector가 등록되어 실행될 때까지 대기합니다.
            subject.awaitCollector()

            repeat(10) {
                subject.emit(it)
            }
            subject.complete()
            job.join()
        }
        result.toList() shouldBeEqualTo List(10) { it }
    }

    @Test
    fun `lot of items`() = runTest {
        val subject = MulticastSubject<Int>(1)
        val n = 1_000
        val counter = AtomicInteger(0)

        withSingleThread { dispatcher ->
            val job = launch(dispatcher) {
                subject.collect {
                    counter.incrementAndGet()
                }
            }.log("job1")

            subject.awaitCollector()

            repeat(n) {
                subject.emit(it)
            }
            subject.complete()
            job.join()
        }
        counter.get() shouldBeEqualTo n
    }

    @Test
    fun `concurrent producers broadcast all items to two collectors`() = runSuspendDefault(timeout = 10.seconds) {
        val subject = MulticastSubject<Int>(2)
        val producerWorkers = 8
        val rounds = 1024
        val expectedValues = (1..rounds).toList()
        val produced = AtomicInteger(0)
        val received1 = ConcurrentLinkedQueue<Int>()
        val received2 = ConcurrentLinkedQueue<Int>()

        val collectorJob1 = launch {
            subject.collect(received1::add)
        }.log("collectorJob1")
        val collectorJob2 = launch {
            subject.collect(received2::add)
        }.log("collectorJob2")

        subject.awaitCollectors(2)

        SuspendedJobTester()
            .workers(producerWorkers)
            .rounds(rounds)
            .add { subject.emit(produced.incrementAndGet()) }
            .run()
        subject.complete()
        collectorJob1.join()
        collectorJob2.join()

        produced.get() shouldBeEqualTo rounds
        received1.sorted() shouldBeEqualTo expectedValues
        received2.sorted() shouldBeEqualTo expectedValues
        received1.toList() shouldBeEqualTo received2.toList()
        subject.collectorCount shouldBeEqualTo 0
        subject.hasCollectors.shouldBeFalse()
    }

    @Test
    fun `terminal signal waits for in-flight multicast emit`() = runSuspendDefault(timeout = 10.seconds) {
        val subject = MulticastSubject<Int>(2)
        val received1 = ConcurrentLinkedQueue<Int>()
        val received2 = ConcurrentLinkedQueue<Int>()
        val firstValueStarted = CompletableDeferred<Unit>()
        val releaseFirstValue = CompletableDeferred<Unit>()

        val collectorJob1 = launch {
            subject.collect { value ->
                received1.add(value)
                if (value == 1) {
                    firstValueStarted.complete(Unit)
                    releaseFirstValue.await()
                }
            }
        }
        val collectorJob2 = launch {
            subject.collect(received2::add)
        }

        subject.awaitCollectors(2)
        subject.emit(1)
        firstValueStarted.await()

        val pendingEmit = async(start = CoroutineStart.UNDISPATCHED) {
            subject.emit(2)
        }
        val terminal = async(start = CoroutineStart.UNDISPATCHED) {
            subject.complete()
        }

        releaseFirstValue.complete(Unit)
        pendingEmit.await()
        terminal.await()
        collectorJob1.join()
        collectorJob2.join()

        received1.toList() shouldBeEqualTo listOf(1, 2)
        received2.toList() shouldBeEqualTo listOf(1, 2)
        subject.collectorCount shouldBeEqualTo 0
        subject.hasCollectors.shouldBeFalse()
    }

    @Test
    fun `first terminal signal wins`() = runTest {
        val failure = IllegalStateException("boom")
        val errorSubject = MulticastSubject<Int>(1)

        errorSubject.emitError(failure)
        errorSubject.complete()

        assertFailsWith<IllegalStateException> {
            errorSubject.collect {}
        } shouldBeEqualTo failure

        val completedSubject = MulticastSubject<Int>(1)
        completedSubject.complete()
        completedSubject.emitError(failure)

        completedSubject.collect {}
    }

    @Test
    fun `2개의 collector 가 등록될 때까지 producer는 대기합니다`() = runTest {
        val subject = MulticastSubject<Int>(2)
        val n = 1_000
        val counter1 = AtomicInteger(0)
        val counter2 = AtomicInteger(0)

        withSingleThread { dispatcher ->
            val job1 = launch(dispatcher) {
                subject.collect {
                    counter1.incrementAndGet()
                }
            }.log("job1")

            val job2 = launch(dispatcher) {
                subject.collect {
                    counter2.incrementAndGet()
                }
            }.log("job2")

            subject.awaitCollectors(2)

            repeat(n) {
                subject.emit(it)
            }

            subject.complete()
            job1.join()
            job2.join()
        }
        counter1.get() shouldBeEqualTo n
        counter2.get() shouldBeEqualTo n
    }
}
