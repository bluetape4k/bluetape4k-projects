package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.coroutines.tests.withSingleThread
import io.bluetape4k.logging.coroutines.KLoggingChannel
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
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class BehaviorSubjectTest {

    companion object: KLoggingChannel()

    @Test
    fun `Behavior subject examples`() = runTest {
        val subject = BehaviorSubject<Int>()

        coroutineScope {
            subject.emit(1)
            subject.emit(2)
            subject.emit(3)

            // OR
            // val behaviorSubject = BehaviorSubject<Int>(1)

            val collected1 = fastListOf<Int>()
            launch {
                subject
                    .log("#1")
                    .collect { collected1.add(it) }
            }.log("Job1")

            val collected2 = fastListOf<Int>()
            launch {
                subject
                    .log("#2")
                    .collect { collected2.add(it) }
            }.log("Job2")

            subject.awaitCollectors(2)

            // HINT: collector 가 등록되지 않았어도, emit 한 요소중 마지막 요소를 저장해둔다
            // collect 되는 값: 3
            yield()
            collected1 shouldBeEqualTo listOf(3)
            collected2 shouldBeEqualTo listOf(3)

            // collect 되는 값: 3, 1, 2, 3
            subject.emit(1)
            subject.emit(2)
            subject.emit(3)

            subject.complete()        // 3, 1, 2, 3
            collected1 shouldBeEqualTo listOf(3, 1, 2, 3)
            collected2 shouldBeEqualTo listOf(3, 1, 2, 3)
        }
    }

    @Test
    fun `일반적인 Subject 작동 예`() = runTest {
        withSingleThread { executor ->
            val subject = BehaviorSubject<Int>()
            val result = fastListOf<Int>()

            val job = launch(executor) {
                subject
                    .onEach { delay(100) }
                    .log("#1")
                    .collect { result.add(it) }
            }
            subject.awaitCollector()

            repeat(5) {
                subject.emit(it + 1)
            }
            yield()

            subject.complete()
            job.join()

            result shouldBeEqualTo listOf(1, 2, 3, 4, 5)
        }
    }

    @Test
    fun `많은 수의 Item을 제공`() = runTest {
        val subject = BehaviorSubject<Int>()
        val counter = AtomicInteger(0)
        val n = 100_000

        val job = launch {
            subject
                // .log("#1")
                .collect { counter.incrementAndGet() }
        }.log("Job")

        subject.awaitCollector()

        // 많은 수의 item을 emit 한다
        repeat(n) {
            subject.emit(it)
        }

        subject.complete()
        job.join()

        counter.get() shouldBeEqualTo n
    }

    @Test
    fun `예외를 emit 하면 예외가 전달된다`() = runTest {
        val subject = BehaviorSubject<Int>()
        val counter = AtomicInteger(0)
        val error = AtomicReference<Throwable>(null)

        withSingleThread { executor ->
            val job = launch(executor) {
                try {
                    subject
                        .log("#1")
                        .collect { counter.incrementAndGet() }
                } catch (e: Throwable) {
                    error.set(e)
                }
            }.log("job")

            subject.awaitCollector()

            subject.emit(1)
            subject.emit(2)
            subject.emitError(RuntimeException("Boom!"))
            subject.emit(3)
            subject.complete()

            job.join()
        }
        counter.get() shouldBeEqualTo 2
        error.get() shouldBeInstanceOf RuntimeException::class
    }

    @Test
    fun `복수의 Consumer 가 있는 경우 같은 아이템이 collect 된다`() = runTest {
        val subject = BehaviorSubject<Int>()

        val n = 1000
        val counter1 = AtomicInteger(0)
        val counter2 = AtomicInteger(0)

        val job1 = launch {
            subject.collect { counter1.incrementAndGet() }
        }.log("job1")

        val job2 = launch {
            subject.collect { counter2.incrementAndGet() }
        }.log("job2")

        subject.awaitCollectors(2)

        repeat(n) {
            subject.emit(it)
        }

        subject.complete()
        job1.join()
        job2.join()

        counter1.get() shouldBeEqualTo n
        counter2.get() shouldBeEqualTo n
    }

    @Test
    fun `복수의 Consumer 가 다른 take를 적용하면 그 요소까지만 수행되고 Cancellation을 발생한다`() = runTest {
        val subject = BehaviorSubject<Int>()

        val n = 100
        val counter1 = AtomicInteger(0)
        val counter2 = AtomicInteger(0)

        val job1 = launch {
            subject
                .log("#1")
                .take(n / 2)
                .collect { counter1.incrementAndGet() }
        }.log("job1")

        val job2 = launch {
            subject
                .log("#2")
                .take(n / 5)
                .collect { counter2.incrementAndGet() }
        }.log("job2")

        subject.awaitCollectors(2)

        repeat(n) {
            subject.emit(it)
        }

        subject.complete()
        job1.join()
        job2.join()

        counter1.get() shouldBeEqualTo n / 2        // take 한 갯수와 일치
        counter2.get() shouldBeEqualTo n / 5
    }

    @Test
    fun `초기 값이 주어진 경우`() = runTest {
        // 초기 값 0 가 주어진다.
        val subject = BehaviorSubject(0)
        val result = CopyOnWriteArrayList<Int>()

        withSingleThread { executor ->
            val job = launch(executor) {
                subject
                    .onEach { delay(100) }
                    .log("collector")
                    .collect { result.add(it) }
            }.log("job")

            subject.awaitCollector()

            repeat(5) {
                subject.emit(it + 1)
            }

            subject.complete()
            job.join()
        }
        result shouldBeEqualTo listOf(0, 1, 2, 3, 4, 5)   // 초기 값 : 0
    }

    @Test
    fun `collector 가 CancellationException 이 발생하면 collect 작업이 중단된다`() = runTest {
        val subject = BehaviorSubject<Int>()

        val n = 10
        val counter1 = AtomicInteger(0)
        val counter2 = AtomicInteger(0)
        val expected = 3

        withSingleThread { executor ->
            val job1 = launch(executor) {
                subject
                    .log("#1")
                    .collect {
                        if (counter1.incrementAndGet() == expected) {
                            throw kotlinx.coroutines.CancellationException()
                        }
                    }
            }.log("job1")

            val job2 = launch(executor) {
                subject
                    .log("#2")
                    .collect {
                        counter2.incrementAndGet()
                    }
            }.log("job2")

            subject.awaitCollectors(2)

            repeat(n) {
                subject.emit(it)
            }

            subject.complete()

            job1.join()
            job1.isCancelled.shouldBeTrue()

            job2.join()
            job2.isCompleted.shouldBeTrue()
        }

        counter1.get() shouldBeEqualTo expected
        counter2.get() shouldBeEqualTo n
    }
}
