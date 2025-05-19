package io.bluetape4k.coroutines.support

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.FutureTask
import kotlin.random.Random

class FutureSupportTest {

    companion object: KLoggingChannel() {
        private const val ITEM_COUNT = 128
        private const val DELAY_TIME = 100L
    }

    @Test
    fun `Massive future as CompletableFuture in multi-threads`() {
        val counter = atomic(0)

        MultithreadingTester()
            .numThreads(16)
            .roundsPerThread(ITEM_COUNT / 4)
            .add {
                val task: FutureTask<Int> = FutureTask {
                    Thread.sleep(Random.nextLong(10))
                    log.trace { "counter=${counter.value}" }
                    counter.incrementAndGet()
                }
                task.run()
                val result = task.get()
                log.trace { "result=$result" }
            }
            .run()

        counter.value shouldBeEqualTo 16 * ITEM_COUNT / 4
    }

    @Test
    fun `Massive Future as CompletableFuture in Coroutines`() = runSuspendTest(Dispatchers.Default) {
        val counter = atomic(0)

        SuspendedJobTester()
            .numThreads(16)
            .roundsPerJob(16 * ITEM_COUNT / 4)
            .add {
                val task = future(Dispatchers.Default, start = CoroutineStart.DEFAULT) {
                    delay(Random.nextLong(10))
                    log.trace { "counter=${counter.value}" }
                    counter.incrementAndGet()
                }
                val result = task.coAwait()
                log.trace { "result=$result" }
            }
            .run()

        counter.value shouldBeEqualTo 16 * ITEM_COUNT / 4
    }
}
