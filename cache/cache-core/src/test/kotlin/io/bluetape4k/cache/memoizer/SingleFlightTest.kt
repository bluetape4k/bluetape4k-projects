package io.bluetape4k.cache.memoizer

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SingleFlightTest {

    @Test
    fun `blocking same key calls share one evaluator`() {
        val singleFlight = SingleFlight<String, Int>()
        val evalCount = AtomicInteger(0)
        val evalStarted = CountDownLatch(1)
        val secondReady = CountDownLatch(1)
        val evalProceed = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val first = executor.submit<Int> {
                singleFlight.run("same") {
                    evalCount.incrementAndGet()
                    evalStarted.countDown()
                    evalProceed.await()
                    42
                }
            }

            evalStarted.await()
            val second = executor.submit<Int> {
                secondReady.countDown()
                singleFlight.run("same") {
                    evalCount.incrementAndGet()
                    99
                }
            }

            secondReady.await()
            Thread.sleep(250)
            evalCount.get() shouldBeEqualTo 1

            evalProceed.countDown()

            first.get(10, TimeUnit.SECONDS) shouldBeEqualTo 42
            second.get(10, TimeUnit.SECONDS) shouldBeEqualTo 42
            evalCount.get() shouldBeEqualTo 1
        } finally {
            evalProceed.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `async null completion fails and allows retry`() {
        val singleFlight = SingleFlight<String, Int>()
        val evalCount = AtomicInteger(0)

        val failed = singleFlight.runAsync("same") {
            evalCount.incrementAndGet()
            @Suppress("UNCHECKED_CAST")
            CompletableFuture.completedFuture(null) as CompletableFuture<Int>
        }

        assertFailsWith<ExecutionException> {
            failed.get(2, TimeUnit.SECONDS)
        }

        singleFlight.runAsync("same") {
            evalCount.incrementAndGet()
            CompletableFuture.completedFuture(7)
        }.get(2, TimeUnit.SECONDS) shouldBeEqualTo 7

        evalCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `clear invalidates in-flight token without failing caller`() {
        val singleFlight = SingleFlight<String, Int>()
        val evaluatorFuture = CompletableFuture<Int>()
        lateinit var token: SingleFlightToken

        val result = singleFlight.runAsync("same") { currentToken ->
            token = currentToken
            evaluatorFuture
        }

        singleFlight.clear()
        singleFlight.isCurrent(token).shouldBeFalse()

        evaluatorFuture.complete(9)
        result.get(2, TimeUnit.SECONDS) shouldBeEqualTo 9
    }

    @Test
    fun `cancelled suspend flight is removed for retry`() = runSuspendDefault {
        val singleFlight = SingleFlight<String, Int>()
        val evalCount = AtomicInteger(0)
        val started = CompletableDeferred<Unit>()

        val job = async {
            singleFlight.runSuspend("same") {
                evalCount.incrementAndGet()
                started.complete(Unit)
                awaitCancellation()
            }
        }

        started.await()
        job.cancelAndJoin()

        singleFlight.runSuspend("same") {
            evalCount.incrementAndGet()
            11
        } shouldBeEqualTo 11

        evalCount.get() shouldBeEqualTo 2
    }
}
