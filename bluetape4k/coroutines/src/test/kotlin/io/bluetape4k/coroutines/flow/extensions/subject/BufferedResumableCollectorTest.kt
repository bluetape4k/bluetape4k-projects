package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.support.log
import io.bluetape4k.coroutines.tests.withSingleThread
import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class BufferedResumableCollectorTest {

    companion object: KLogging()

    @Test
    fun `capacity 만큼 버퍼링을 합니다`() = runTest {
        val bc = BufferedResumableCollector<Int>(32)
        val n = 10_000
        val counter = atomic(0)

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
        counter.value shouldBeEqualTo n
    }

    @Test
    fun `basic long operation with one capacity`() = runTest {
        val bc = BufferedResumableCollector<Int>(1)
        val n = 10_000
        val counter = atomic(0)
        val count by counter

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
        count shouldBeEqualTo n
    }

    @Test
    fun `basic long operations with 64 capacity`() = runTest {
        val bc = BufferedResumableCollector<Int>(64)
        val n = 100_000
        val counter = atomic(0)
        val count by counter

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
        count shouldBeEqualTo n
    }

    @Test
    fun `basic long operations with 256 capacity`() = runTest {
        val bc = BufferedResumableCollector<Int>(256)
        val n = 100_000
        val counter = atomic(0)
        val count by counter

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
        count shouldBeEqualTo n
    }
}
