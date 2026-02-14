package io.bluetape4k.coroutines.support

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class FutureSupportTest {

    companion object: KLoggingChannel() {
        private const val ITEM_COUNT = 128
        private const val DELAY_TIME = 100L
    }

    @Test
    fun `Massive future as CompletableFuture in multi-threads`() {
        val counter = AtomicInteger(0)

        MultithreadingTester()
            .workers(16)
            .rounds(ITEM_COUNT / 4)
            .add {
                val task: FutureTask<Int> = FutureTask {
                    Thread.sleep(Random.nextLong(10))
                    log.trace { "counter=${counter.get()}" }
                    counter.incrementAndGet()
                }
                task.run()
                val result = task.get()
                log.trace { "result=$result" }
            }
            .run()

        counter.get() shouldBeEqualTo 16 * ITEM_COUNT / 4
    }

    @Test
    fun `Massive Future as CompletableFuture in Coroutines`() = runSuspendDefault {
        val counter = AtomicInteger(0)

        SuspendedJobTester()
            .workers(16)
            .rounds(16 * ITEM_COUNT / 4)
            .add {
                val task = future(Dispatchers.Default, start = CoroutineStart.DEFAULT) {
                    delay(Random.nextLong(10))
                    log.trace { "counter=${counter.get()}" }
                    counter.incrementAndGet()
                }
                val result = task.suspendAwait()
                log.trace { "result=$result" }
            }
            .run()

        counter.get() shouldBeEqualTo 16 * ITEM_COUNT / 4
    }
}
