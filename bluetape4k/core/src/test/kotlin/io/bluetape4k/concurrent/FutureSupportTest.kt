package io.bluetape4k.concurrent

import io.bluetape4k.concurrent.virtualthread.VirtualFuture
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.concurrency.VirtualthreadTester
import io.bluetape4k.junit5.coroutines.MultijobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class FutureSupportTest {

    companion object: KLogging() {
        private const val ITEM_COUNT = 100
        private const val DELAY_TIME = 100L
    }

    @Test
    fun `Future as CompletableFuture`() {
        val future1 = FutureTask {
            Thread.sleep(DELAY_TIME)
            "value1"
        }
        future1.run()

        val future2 = FutureTask {
            Thread.sleep(DELAY_TIME)
            "value2"
        }
        future2.run()

        val result1 = future1.asCompletableFuture()
        val result2 = future2.asCompletableFuture()
        result1.join() shouldBeEqualTo "value1"
        result2.join() shouldBeEqualTo "value2"
    }

    @Test
    fun `Massive Future as CompletableFuture`() {
        val futures = List(ITEM_COUNT) {
            FutureTask {
                Thread.sleep(Random.nextLong(10))
                "value$it"
            }.apply { run() }
        }.map { it.asCompletableFuture() }

        val results = futures.sequence().get()
        results.size shouldBeEqualTo ITEM_COUNT
    }

    @Test
    fun `Massive Future as CompletaboeFuture in Multiple Thread`() {
        val counter = AtomicInteger(0)

        MultithreadingTester()
            .numThreads(Runtimex.availableProcessors * 2)
            .roundsPerThread(ITEM_COUNT / 4)
            .add {
                val task = CompletableFuture.supplyAsync {
                    Thread.sleep(Random.nextLong(10))
                    counter.incrementAndGet()
                }
                val result = task.asCompletableFuture().get()
                log.trace { "result=$result" }
            }
            .run()

        counter.get() shouldBeEqualTo Runtimex.availableProcessors * 2 * ITEM_COUNT / 4

    }

    @Test
    fun `Massive Future as CompletaboeFuture in Multiple Virtual Thread`() {
        val counter = AtomicInteger(0)

        VirtualthreadTester()
            .numThreads(Runtimex.availableProcessors * 2)
            .roundsPerThread(ITEM_COUNT / 4)
            .add {
                val task: VirtualFuture<Int> = virtualFuture {
                    Thread.sleep(Random.nextLong(10))
                    counter.incrementAndGet()
                }
                val result = task.await()
                log.trace { "result=$result" }
            }
            .run()

        counter.get() shouldBeEqualTo Runtimex.availableProcessors * 2 * ITEM_COUNT / 4
    }

    @Test
    fun `Massive Future as CompletaboeFuture in Structured`() {
        val counter = AtomicInteger(0)

        StructuredTaskScopeTester()
            .roundsPerTask(Runtimex.availableProcessors * 2 * ITEM_COUNT / 4)
            .add {
                val task: VirtualFuture<Int> = virtualFuture {
                    Thread.sleep(Random.nextLong(10))
                    counter.incrementAndGet()
                }
                val result = task.await()
                log.trace { "result=$result" }
            }
            .run()

        counter.get() shouldBeEqualTo Runtimex.availableProcessors * 2 * ITEM_COUNT / 4
    }

    @Test
    fun `Massive Future as CompletaboeFuture in Multiple Jobs`() = runSuspendDefault {
        val counter = AtomicInteger(0)

        MultijobTester()
            .numThreads(Runtimex.availableProcessors * 2)
            .roundsPerJob(ITEM_COUNT / 4)
            .add {
                val task = async(Dispatchers.Default) {
                    delay(Random.nextLong(10))
                    counter.incrementAndGet()
                }
                val result = task.await()
                log.trace { "result=$result" }
            }
            .run()

        counter.get() shouldBeEqualTo Runtimex.availableProcessors * 2 * ITEM_COUNT / 4
    }
}
