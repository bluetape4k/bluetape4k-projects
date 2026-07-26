package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.junit5.coroutines.withSingleThread
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class BufferedResumableCollectorTest {

    companion object: KLoggingChannel()

    @Test
    fun `capacity 만큼 버퍼링을 합니다`() = runTest {
        val bc = BufferedResumableCollector<Int>(32)
        val n = 10_000
        val counter = AtomicInteger(0)

        withSingleThread { dispatcher ->
            val job = launch(dispatcher) {
                repeat(n) {
                    bc.next(it)
                }
                bc.complete()
            }.log("job")

            yield()

            val collector = FlowCollector<Int> {
                counter.incrementAndGet()
            }

            bc.drain(collector)
            job.join()
        }
        counter.get() shouldBeEqualTo n
    }

    @Test
    fun `basic long operation with one capacity`() = runTest {
        val bc = BufferedResumableCollector<Int>(1)
        val n = 10_000
        val counter = AtomicInteger(0)

        withSingleThread { dispatcher ->
            val job = launch(dispatcher) {
                repeat(n) {
                    bc.next(it)
                }
                bc.complete()
            }.log("job")
            yield()

            val collector = FlowCollector<Int> {
                counter.incrementAndGet()
            }

            bc.drain(collector)
            job.join()
        }
        counter.get() shouldBeEqualTo n
    }

    @Test
    fun `basic long operations with 64 capacity`() = runTest {
        val bc = BufferedResumableCollector<Int>(64)
        val n = 100_000
        val counter = AtomicInteger(0)

        withSingleThread { dispatcher ->
            val job = launch(dispatcher) {
                repeat(n) {
                    bc.next(it)
                }
                bc.complete()
            }.log("job")
            yield()

            val collector = FlowCollector<Int> { counter.incrementAndGet() }
            bc.drain(collector)

            job.join()
        }
        counter.get() shouldBeEqualTo n
    }

    @Test
    fun `basic long operations with 256 capacity`() = runTest {
        val bc = BufferedResumableCollector<Int>(256)
        val n = 100_000
        val counter = AtomicInteger(0)

        withSingleThread { dispatcher ->
            val job = launch(dispatcher) {
                repeat(n) {
                    bc.next(it)
                }
                bc.complete()
            }.log("job")
            yield()

            val collector = FlowCollector<Int> { counter.incrementAndGet() }
            bc.drain(collector)
            job.join()
        }
        counter.get() shouldBeEqualTo n
    }

    @Test
    fun `suspended concurrent producers drain all values with small capacity`() =
        runSuspendDefault(timeout = 10.seconds) {
            val bc = BufferedResumableCollector<Int>(1)
            val producerWorkers = 8
            val rounds = 512
            val expectedValues = (1..rounds).toList()
            val expectedCount = expectedValues.size
            val produced = AtomicInteger(0)
            val received = mutableListOf<Int>()

            val producerJob = launch {
                SuspendedJobTester()
                    .workers(producerWorkers)
                    .rounds(rounds)
                    .add {
                        bc.next(produced.incrementAndGet())
                    }
                    .run()
                bc.complete()
            }.log("producerJob")

            yield()

            val collector = FlowCollector<Int> {
                received += it
            }
            bc.drain(collector)
            producerJob.join()

            produced.get() shouldBeEqualTo expectedCount
            received.sorted() shouldBeEqualTo expectedValues
        }

    @Test
    fun `suspended concurrent producers drain all values with buffered capacity`() =
        runSuspendDefault(timeout = 10.seconds) {
            val bc = BufferedResumableCollector<Int>(64)
            val producerWorkers = 8
            val rounds = 1024
            val expectedValues = (1..rounds).toList()
            val expectedCount = expectedValues.size
            val produced = AtomicInteger(0)
            val received = mutableListOf<Int>()

            val producerJob = launch {
                SuspendedJobTester()
                    .workers(producerWorkers)
                    .rounds(rounds)
                    .add {
                        bc.next(produced.incrementAndGet())
                    }
                    .run()
                bc.complete()
            }.log("producerJob")

            yield()

            val collector = FlowCollector<Int> {
                received += it
            }
            bc.drain(collector)
            producerJob.join()

            produced.get() shouldBeEqualTo expectedCount
            received.sorted() shouldBeEqualTo expectedValues
        }
}
