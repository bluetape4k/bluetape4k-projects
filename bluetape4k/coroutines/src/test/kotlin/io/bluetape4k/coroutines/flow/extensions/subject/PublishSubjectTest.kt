package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.coroutines.tests.withSingleThread
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertFailsWith

class PublishSubjectTest {

    companion object: KLoggingChannel()

    @Test
    fun `multicast values to one or more flow collectors`() = runTest {
        val subject = PublishSubject<Int>()

        val result1 = CopyOnWriteArrayList<Int>()
        val result2 = CopyOnWriteArrayList<Int>()

        coroutineScope {
            launch {
                subject
                    .log("#1")
                    .collect { result1.add(it) }
            }.log("job1")
            yield()

            launch {
                subject
                    .log("#2")
                    .collect { result2.add(it) }
            }.log("job2") // job1의 collector 만 시작해도 다음으로 넘어간다 (job2는 시작 안했을 수 있다)
            yield()

            // collector가 실행될 때까지 대기합니다.
            subject.awaitCollectors(2)

            repeat(10) {
                log.trace { "emit: ${it + 1}" }
                subject.emit(it + 1)
            }
            subject.complete()
        }

        val expected = (1..10).toFastList()
        result1 shouldBeEqualTo expected
        result2 shouldBeEqualTo expected
    }

    @Test
    fun `basic create`() = runTest {
        val result1 = CopyOnWriteArrayList<Int>()
        val result2 = CopyOnWriteArrayList<Int>()

        withSingleThread { dispatcher ->
            val subject = PublishSubject<Int>()

            val job1 = launch(dispatcher) {
                subject
                    .onEach { delay(10) }
                    .log("#1")
                    .collect { result1.add(it) }
            }.log("job1")

            val job2 = launch(dispatcher) {
                subject
                    .onEach { delay(20) }
                    .log("#2")
                    .collect { result2.add(it) }
            }

            subject.awaitCollectors(2)

            subject.emit(1)
            subject.emit(2)
            subject.emit(3)
            subject.emit(4)
            subject.emit(5)
            subject.complete()

            job1.join()
            job2.join()
        }
        val expected = listOf(1, 2, 3, 4, 5)
        result1 shouldBeEqualTo expected
        result2 shouldBeEqualTo expected
    }

    @Test
    fun `emit and collect many items`() = runTest {
        withSingleThread { dispatcher ->
            val subject = PublishSubject<Int>()
            val n = 10_000
            val counter = AtomicInteger(0)

            val job = launch(dispatcher) {
                subject.collect {
                    counter.incrementAndGet()
                }
            }

            subject.awaitCollectors(1)

            repeat(n) { i ->
                subject.emit(i)
            }
            subject.complete()
            job.join()

            counter.get() shouldBeEqualTo n
        }
    }

    @Test
    fun `예외를 emit 한 경우 try-catch로 잡을 수 있다`() = runTest {
        withSingleThread { dispatcher ->
            val subject = PublishSubject<Int>()
            val counter = AtomicInteger(0)
            val error = AtomicReference<Throwable>(null)

            val job = launch(dispatcher) {
                try {
                    subject
                        .log("#1")
                        .collect { counter.incrementAndGet() }
                } catch (e: Throwable) {
                    error.set(e)
                }
            }.log("job")

            subject.awaitCollectors(1)

            subject.emitError(RuntimeException("Boom!"))
            subject.complete()

            job.join()

            counter.get() shouldBeEqualTo 0
            error.get() shouldBeInstanceOf RuntimeException::class
        }
    }

    @Test
    fun `multiple collectors`() = runTest {
        val subject = PublishSubject<Int>()
        val n = 10_000
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

    @Test
    fun `multiple consumer with different delay`() = runTest {
        val subject = PublishSubject<Int>()
        val n = 10

        val counter1 = AtomicInteger(0)
        val counter2 = AtomicInteger(0)

        coroutineScope {
            launch {
                subject
                    .onEach { delay(1) }
                    .log("#1")
                    .collect { counter1.incrementAndGet() }
            }.log("job1")

            launch {
                subject
                    .onEach { delay(3) }
                    .log("#2")
                    .collect { counter2.incrementAndGet() }

            }.log("job2")

            subject.awaitCollectors(2)

            repeat(n) {
                subject.emit(it)
            }
            subject.complete()
        }

        counter1.get() shouldBeEqualTo n
        counter2.get() shouldBeEqualTo n
    }

    @Test
    fun `multiple consumer with take operation`() = runTest {
        val subject = PublishSubject<Int>()
        val n = 100

        val counter1 = AtomicInteger(0)
        val counter2 = AtomicInteger(0)

        coroutineScope {
            launch {
                subject
                    .onEach { delay(1) }
                    .log("#1")
                    .collect { counter1.incrementAndGet() }
            }.log("job1")

            launch {
                subject.take(n / 2)
                    .onEach { delay(2) }
                    .log("#2")
                    .collect { counter2.incrementAndGet() }
            }.log("job2")

            subject.awaitCollectors(2)

            repeat(n) {
                subject.emit(it)
            }
            subject.complete()
        }

        counter1.get() shouldBeEqualTo n
        counter2.get() shouldBeEqualTo n / 2
    }

    @Test
    fun `이미 completed 된 subject는 collect 할 수 없다`() = runTest {
        val subject = PublishSubject<Int>()
        subject.complete()

        val counter = AtomicInteger(0)

        // subject가 이미 completed 되었으므로, collect 를 수행하지 않습니다.
        subject
            .log("#1")
            .collect { counter.incrementAndGet() } // completed 이후에는 collect 가 동작하지 않는다

        counter.get() shouldBeEqualTo 0
    }

    @Test
    fun `이미 예외가 emit 된 경우 collect 할 수 없다`() = runTest {
        val subject = PublishSubject<Int>()
        subject.emitError(RuntimeException("Boom!"))

        val counter = AtomicInteger(0)

        // subject 에서 예외를 emit 했으므로, collect 시에 emit된 예외가 발생합니다.
        assertFailsWith<RuntimeException> {
            subject
                .log("#1")
                .collect { counter.incrementAndGet() }
        } // completed 이후에는 collect 가 동작하지 않는다

        counter.get() shouldBeEqualTo 0
    }

    @Test
    fun `collect 시 예외가 발생하면 collector 를 제거한다`() = runTest {
        coroutineScope {
            val subject = PublishSubject<Int>()
            val n = 10
            val expected = 3

            val counter1 = AtomicInteger(0)

            val job = launch {
                subject
                    .onEach { delay(10) }
                    .log("#1")
                    .collect {
                        if (counter1.incrementAndGet() == expected) {
                            throw CancellationException()
                        }
                    }
            }.log("job1")

            subject.awaitCollector()

            repeat(n) {
                subject.emit(it)
            }
            subject.complete()

            await untilSuspending { job.isCancelled }

            job.isCancelled.shouldBeTrue()
            subject.collectorCount shouldBeEqualTo 0
            counter1.get() shouldBeEqualTo expected
        }
    }
}
