package io.bluetape4k.concurrent

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.trace
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import kotlin.test.assertFailsWith

class ConcurrentReducerTest {

    companion object: KLogging()

    @Test
    fun `invalid max concurrency`() {
        // max concurrency 값은 양수이어야 합니다.
        assertFailsWith<IllegalArgumentException> {
            ConcurrentReducer<Any>(0, 10)
        }
    }

    @Test
    fun `invalid max queue size`() {
        // max queue size 값은 양수이어야 합니다.
        assertFailsWith<IllegalArgumentException> {
            ConcurrentReducer<Any>(10, 0)
        }
    }

    @Test
    fun `task return null`() {
        val reducer = ConcurrentReducer<String>(1, 10)
        val promise = reducer.add(job(null))

        promise.isDone.shouldBeTrue()
        val exception = promise.getException()
        exception shouldBeInstanceOf NullPointerException::class
    }

    @Test
    fun `when job throw exception`() {
        val reducer = ConcurrentReducer<String>(1, 10)
        val promise = reducer.add { throw IllegalStateException("Boom!") }

        promise.isDone.shouldBeTrue()
        promise.getException() shouldBeInstanceOf IllegalStateException::class
    }

    @Test
    fun `when job return failure`() {
        val reducer = ConcurrentReducer<String>(1, 10)
        val promise = reducer.add {
            failedCompletableFutureOf(IllegalStateException("Boom!"))
        }

        promise.isDone.shouldBeTrue()
        promise.getException() shouldBeInstanceOf IllegalStateException::class
    }

    @Test
    fun `when job canceled`() {
        val reducer = ConcurrentReducer<String>(2, 10)
        val request1 = CompletableFuture<String>()
        val request2 = CompletableFuture<String>()

        val promise1 = reducer.add(job(request1))
        val promise2 = reducer.add(job(request2))

        val wasInvoked = atomic(false)
        val promise3 = reducer.add {
            wasInvoked.value = true
            null
        }

        promise3.cancel(false)

        // 1 and 2 are in progress, 3 is cancelled
        promise1.isDone.shouldBeFalse()
        promise2.isDone.shouldBeFalse()
        promise3.isDone.shouldBeTrue()
        reducer.activeCount shouldBeEqualTo 2
        reducer.queuedCount shouldBeEqualTo 1

        request2.complete("2")

        promise1.isDone.shouldBeFalse()
        promise2.isDone.shouldBeTrue()
        promise3.isDone.shouldBeTrue()
        reducer.activeCount shouldBeEqualTo 1
        reducer.queuedCount shouldBeEqualTo 0

        request1.complete("1")

        promise1.isDone.shouldBeTrue()
        promise2.isDone.shouldBeTrue()
        promise3.isDone.shouldBeTrue()
        reducer.activeCount shouldBeEqualTo 0
        reducer.queuedCount shouldBeEqualTo 0

        wasInvoked.value.shouldBeFalse()
    }

    @Test
    fun `when simple result`() {
        val reducer = ConcurrentReducer<String>(2, 10)
        val request1 = CompletableFuture<String>()
        val request2 = CompletableFuture<String>()
        val request3 = CompletableFuture<String>()

        val promise1 = reducer.add { request1 }
        val promise2 = reducer.add { request2 }
        val promise3 = reducer.add { request3 }

        request3.complete("3")

        // 1 and 2 are in progress, 3 is still blocked
        promise1.isDone.shouldBeFalse()
        promise2.isDone.shouldBeFalse()
        promise3.isDone.shouldBeFalse()
        reducer.activeCount shouldBeEqualTo 2
        reducer.queuedCount shouldBeEqualTo 1

        request2.complete("2")

        promise1.isDone.shouldBeFalse()
        promise2.isDone.shouldBeTrue()
        promise3.isDone.shouldBeTrue()
        reducer.activeCount shouldBeEqualTo 1      // request3 이 이미 완료된 놈이므로
        reducer.queuedCount shouldBeEqualTo 0

        request1.complete("1")

        promise1.isDone.shouldBeTrue()
        promise2.isDone.shouldBeTrue()
        promise3.isDone.shouldBeTrue()
        reducer.activeCount shouldBeEqualTo 0
        reducer.queuedCount shouldBeEqualTo 0
    }

    @Test
    fun `when task long running`() {
        val activeCounter = atomic(0)
        val maxCounter = atomic(0)
        val queueSize = 6
        val maxConcurrency = 5
        val reducer = ConcurrentReducer<String>(maxConcurrency, queueSize)

        val jobs = mutableListOf<CountingJob>()
        val promises = mutableListOf<CompletableFuture<String>>()

        repeat(queueSize) {
            val job = CountingJob(reducer::activeCount, maxCounter)
            jobs.add(job)
            promises += reducer.add(job)
        }

        jobs.forEachIndexed { index, job ->
            if (index % 2 == 0) {
                job.future.complete("success")
            } else {
                job.future.completeExceptionally(IllegalStateException("Boom!"))
            }
        }

        promises.all { it.isDone }.shouldBeTrue()
        activeCounter.value shouldBeEqualTo 0
        reducer.activeCount shouldBeEqualTo 0
        reducer.queuedCount shouldBeEqualTo 0
        reducer.remainingActiveCapacity shouldBeEqualTo maxConcurrency
        reducer.remainingQueueCapacity shouldBeEqualTo queueSize
        maxCounter.value shouldBeEqualTo maxConcurrency
    }

    @Test
    fun `when exceed queue size`() {
        val reducer = ConcurrentReducer<String>(10, 10)
        repeat(20) {
            reducer.add { CompletableFuture() }
        }

        val promise = reducer.add { CompletableFuture() }
        promise.isDone.shouldBeTrue()
        promise.getException() shouldBeInstanceOf ConcurrentReducer.CapacityReachedException::class
    }

    @Test
    fun `verify queue size`() {
        val concurrency = 4
        val queueSize = 10
        val future = CompletableFuture<String>()
        val reducer = ConcurrentReducer<String>(concurrency, queueSize)

        repeat(20) {
            reducer.add { future }
        }

        reducer.activeCount shouldBeEqualTo concurrency
        reducer.queuedCount shouldBeEqualTo queueSize
        reducer.remainingActiveCapacity shouldBeEqualTo 0
        reducer.remainingQueueCapacity shouldBeEqualTo 0

        future.complete("")

        reducer.activeCount shouldBeEqualTo 0
        reducer.queuedCount shouldBeEqualTo 0
        reducer.remainingActiveCapacity shouldBeEqualTo concurrency
        reducer.remainingQueueCapacity shouldBeEqualTo queueSize
    }

    private fun job(future: CompletionStage<String>?): () -> CompletionStage<String>? = { future }

    private class CountingJob(
        private val activeCount: () -> Int,
        private val maxCount: AtomicInt,
    ): () -> CompletionStage<String>? {

        companion object {
            private val log = KotlinLogging.logger { }
        }

        val future = CompletableFuture<String>()

        override fun invoke(): CompletionStage<String>? {
            val count = activeCount()
            log.trace { "Active count=$count, maxCount=${maxCount.value}" }
            if (count > maxCount.value) {
                maxCount.value = count
            }
            return future
        }
    }
}
