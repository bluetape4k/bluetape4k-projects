package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.coroutines.tests.withSingleThread
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class MulticastSubjectTest {

    companion object: KLoggingChannel()

    @Test
    fun `1개의 collector 가 등록될 때까지 producer가 대기합니다`() = runTest {
        val subject = MulticastSubject<Int>(1)
        val result = CopyOnWriteArrayList<Int>()

        withSingleThread { dispatcher ->
            val job = launch(dispatcher) {
                subject
                    .onEach { delay(10) }
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
        result shouldBeEqualTo fastList(10) { it }
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
