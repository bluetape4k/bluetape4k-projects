package io.bluetape4k.concurrent

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 비동기 작업들을 세마포어를 이용하여 동시 실행을 제한하는 [ConcurrentReducer]를 테스트합니다.
 */
class ConcurrentReducerTest {

    companion object: KLoggingChannel()

    @Test
    fun `invalid max concurrency`() {
        // max concurrency 값은 양수이어야 합니다.
        assertFailsWith<IllegalArgumentException> {
            concurrentReducerOf<Any>(0, 10)
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
    fun `add는 task invocation 없이 호출자에게 즉시 promise를 반환한다`() {
        val reducer = concurrentReducerOf<String>(1, 10)
        val taskStarted = CountDownLatch(1)
        val releaseTask = CountDownLatch(1)
        val invocation = CompletableFuture.supplyAsync<CompletableFuture<String>> {
            reducer.add {
                taskStarted.countDown()
                releaseTask.await()
                completableFutureOf("done")
            }
        }
        var promise: CompletableFuture<String>? = null

        try {
            taskStarted.await(1, TimeUnit.SECONDS).shouldBeTrue()
            invocation.isDone.shouldBeTrue()

            promise = invocation.get(1, TimeUnit.SECONDS)
            promise?.isDone.shouldBeFalse()
        } finally {
            releaseTask.countDown()
            promise = promise ?: invocation.get(1, TimeUnit.SECONDS)
            promise?.get(1, TimeUnit.SECONDS)
            reducer.close()
        }
    }

    @Test
    fun `task return null`() {
        val reducer = concurrentReducerOf<String>(1, 10)
        val promise = reducer.add(job(null))

        await until { promise.isDone }
        promise.isDone.shouldBeTrue()
        val exception = promise.getException()
        exception shouldBeInstanceOf NullPointerException::class
        reducer.close()
    }

    @Test
    fun `when job throw exception`() {
        val reducer = concurrentReducerOf<String>(1, 10)
        val promise = reducer.add { throw IllegalStateException("Boom!") }

        await until { promise.isDone }
        promise.isDone.shouldBeTrue()
        promise.getException() shouldBeInstanceOf IllegalStateException::class
        reducer.close()
    }

    @Test
    fun `when job return failure`() {
        val reducer = concurrentReducerOf<String>(1, 10)
        val promise = reducer.add {
            failedCompletableFutureOf(IllegalStateException("Boom!"))
        }

        await until { promise.isDone }
        promise.isDone.shouldBeTrue()
        promise.getException() shouldBeInstanceOf IllegalStateException::class
        reducer.close()
    }

    @Test
    fun `when job canceled`() {
        val reducer = concurrentReducerOf<String>(2, 10)
        val request1 = CompletableFuture<String>()
        val request2 = CompletableFuture<String>()

        val promise1 = reducer.add(job(request1))
        val promise2 = reducer.add(job(request2))

        val wasInvoked = AtomicBoolean(false)
        val promise3 = reducer.add {
            wasInvoked.set(true)
            null
        }

        promise3.cancel(false)
        await until { promise3.isDone && reducer.activeCount == 2 }

        // 1 and 2 are in progress, 3 is cancelled
        promise1.isDone.shouldBeFalse()
        promise2.isDone.shouldBeFalse()
        promise3.isDone.shouldBeTrue()
        reducer.activeCount shouldBeEqualTo 2
        reducer.queuedCount shouldBeEqualTo 1

        request2.complete("2")
        await until { promise2.isDone && reducer.activeCount == 1 }

        promise1.isDone.shouldBeFalse()
        promise2.isDone.shouldBeTrue()
        promise3.isDone.shouldBeTrue()
        reducer.activeCount shouldBeEqualTo 1
        reducer.queuedCount shouldBeEqualTo 0

        request1.complete("1")
        await until { promise1.isDone && reducer.activeCount == 0 }

        promise1.isDone.shouldBeTrue()
        promise2.isDone.shouldBeTrue()
        promise3.isDone.shouldBeTrue()
        reducer.activeCount shouldBeEqualTo 0
        reducer.queuedCount shouldBeEqualTo 0

        wasInvoked.get().shouldBeFalse()
    }

    @Test
    fun `3개의 짧은 작업을 2개만 동시 실행으로 제한할 때`() {
        val reducer = concurrentReducerOf<String>(2, 10)
        val request1 = CompletableFuture<String>()
        val request2 = CompletableFuture<String>()
        val request3 = CompletableFuture<String>()

        val promise1 = reducer.add { request1 }
        val promise2 = reducer.add { request2 }
        val promise3 = reducer.add { request3 }

        request3.complete("3")

        await until { reducer.activeCount == 2 && reducer.queuedCount == 1 }

        // 1 and 2 are in progress, 3 is still blocked
        promise1.isDone.shouldBeFalse()
        promise2.isDone.shouldBeFalse()
        promise3.isDone.shouldBeFalse()
        reducer.activeCount shouldBeEqualTo 2
        reducer.queuedCount shouldBeEqualTo 1

        request2.complete("2")
        await until { promise2.isDone }
        await until { reducer.activeCount == 1 }

        promise1.isDone.shouldBeFalse()
        promise2.isDone.shouldBeTrue()
        promise3.isDone.shouldBeTrue()
        reducer.activeCount shouldBeEqualTo 1      // request3 이 이미 완료된 놈이므로
        reducer.queuedCount shouldBeEqualTo 0

        request1.complete("1")
        await until { promise1.isDone && reducer.activeCount == 0 }

        promise1.isDone.shouldBeTrue()
        promise2.isDone.shouldBeTrue()
        promise3.isDone.shouldBeTrue()
        reducer.activeCount shouldBeEqualTo 0
        reducer.queuedCount shouldBeEqualTo 0
    }

    @Test
    fun `concurrency 보다 많은 작업이 실행될 떄`() {
        val activeCounter = AtomicInteger(0)
        val maxCounter = AtomicInteger(0)
        val queueSize = 6
        val maxConcurrency = 5
        val reducer = concurrentReducerOf<String>(maxConcurrency, queueSize)

        val jobs = mutableListOf<CountingJob>()
        val promises = mutableListOf<CompletableFuture<String>>()

        repeat(queueSize) {
            val job = CountingJob(reducer::activeCount, maxCounter)
            jobs.add(job)
            promises += reducer.add(job)
        }

        await until { reducer.activeCount == maxConcurrency && reducer.queuedCount == queueSize - maxConcurrency }

        jobs.forEachIndexed { index, job ->
            if (index % 2 == 0) {
                job.future.complete("success")
            } else {
                job.future.completeExceptionally(IllegalStateException("Boom!"))
            }
        }

        await until { reducer.activeCount == 0 }

        promises.all { it.isDone }.shouldBeTrue()
        activeCounter.get() shouldBeEqualTo 0
        reducer.activeCount shouldBeEqualTo 0
        reducer.queuedCount shouldBeEqualTo 0
        reducer.remainingActiveCapacity shouldBeEqualTo maxConcurrency
        reducer.remainingQueueCapacity shouldBeEqualTo queueSize
        maxCounter.get() shouldBeEqualTo maxConcurrency
    }

    @Test
    fun `큐 사이즈를 초과해 작업을 추가하면 CapacityReachedException이 발생한다`() {
        val concurrency = 10
        val queueSize = 10
        val future = CompletableFuture<String>()
        val reducer = concurrentReducerOf<String>(concurrency, queueSize)

        repeat(concurrency) {
            reducer.add { future }
        }
        await until { reducer.activeCount == concurrency && reducer.queuedCount == 0 }

        repeat(queueSize) {
            reducer.add { future }
        }
        await until { reducer.queuedCount == queueSize }

        val promise = reducer.add { future }
        promise.isDone.shouldBeTrue()
        promise.getException() shouldBeInstanceOf ConcurrentReducer.CapacityReachedException::class

        future.complete("")
        await until { reducer.activeCount == 0 && reducer.queuedCount == 0 }
        reducer.close()
    }

    @Test
    fun `큐 사이즈를 초과하여 작업을 추가하면, 예외를 담은 Completable을 반환한다`() {
        val concurrency = 4
        val queueSize = 10
        val future = CompletableFuture<String>()
        val reducer = concurrentReducerOf<String>(concurrency, queueSize)

        repeat(concurrency) {
            reducer.add { future }
        }

        await until { reducer.activeCount == concurrency && reducer.queuedCount == 0 }

        repeat(queueSize) {
            reducer.add { future }
        }

        await until { reducer.queuedCount == queueSize }
        reducer.activeCount shouldBeEqualTo concurrency
        reducer.queuedCount shouldBeEqualTo queueSize
        reducer.remainingActiveCapacity shouldBeEqualTo 0
        reducer.remainingQueueCapacity shouldBeEqualTo 0

        val overflow = reducer.add { future }
        overflow.isCompletedExceptionally.shouldBeTrue()
        overflow.getException() shouldBeInstanceOf ConcurrentReducer.CapacityReachedException::class

        future.complete("")

        await until { reducer.activeCount == 0 }

        reducer.activeCount shouldBeEqualTo 0
        reducer.queuedCount shouldBeEqualTo 0
        reducer.remainingActiveCapacity shouldBeEqualTo concurrency
        reducer.remainingQueueCapacity shouldBeEqualTo queueSize
        reducer.close()
    }

    @Test
    fun `close 호출 시 큐가 비워지고 더 이상 작업이 실행되지 않는다`() {
        val reducer = concurrentReducerOf<String>(1, 10)
        val request = CompletableFuture<String>()
        val queued1Invoked = AtomicBoolean(false)
        val queued2Invoked = AtomicBoolean(false)

        // 활성 작업 1개, 큐에 대기 작업 2개 추가
        reducer.add { request }
        val queued1 = reducer.add {
            queued1Invoked.set(true)
            CompletableFuture()
        }
        val queued2 = reducer.add {
            queued2Invoked.set(true)
            CompletableFuture()
        }

        await until { reducer.activeCount == 1 && reducer.queuedCount == 2 }
        reducer.activeCount shouldBeEqualTo 1
        reducer.queuedCount shouldBeEqualTo 2

        // close 호출
        reducer.close()

        // 큐가 비워져야 한다
        reducer.queuedCount shouldBeEqualTo 0
        queued1.isCancelled.shouldBeTrue()
        queued2.isCancelled.shouldBeTrue()
        queued1Invoked.get().shouldBeFalse()
        queued2Invoked.get().shouldBeFalse()
    }

    @Test
    fun `close는 실행 중인 stage를 취소하고 permit을 정확히 한 번 회수한다`() {
        val reducer = concurrentReducerOf<String>(1, 1)
        val source = CompletableFuture<String>()
        val taskStarted = CountDownLatch(1)
        val promise = reducer.add {
            taskStarted.countDown()
            source
        }

        try {
            taskStarted.await(1, TimeUnit.SECONDS).shouldBeTrue()
            await until { reducer.activeCount == 1 }

            reducer.close()

            promise.isCancelled.shouldBeTrue()
            source.isCancelled.shouldBeTrue()
            reducer.activeCount shouldBeEqualTo 0
            reducer.remainingActiveCapacity shouldBeEqualTo 1
            reducer.queuedCount shouldBeEqualTo 0

            // close가 source callback과 경합해도 permit은 중복 회수되지 않아야 한다.
            source.complete("late").shouldBeFalse()
            reducer.close()
            reducer.activeCount shouldBeEqualTo 0
            reducer.remainingActiveCapacity shouldBeEqualTo 1
        } finally {
            source.cancel(false)
            reducer.close()
        }
    }

    @Test
    fun `close 이후 작업 추가는 실패한 CompletableFuture를 반환한다`() {
        val reducer = concurrentReducerOf<String>(1, 10)

        reducer.close()

        val promise = reducer.add { completableFutureOf("done") }

        promise.isCompletedExceptionally.shouldBeTrue()
        promise.getException() shouldBeInstanceOf RejectedExecutionException::class
    }

    @Test
    fun `add와 close 동시 실행에서도 promise가 누수되지 않는다`() {
        repeat(32) {
            val reducer = concurrentReducerOf<String>(1, 1)
            val start = CountDownLatch(1)
            val addInvocation = CompletableFuture.supplyAsync<CompletableFuture<String>> {
                start.await()
                reducer.add { completableFutureOf("done") }
            }
            val closeInvocation = CompletableFuture.runAsync {
                start.await()
                reducer.close()
            }

            start.countDown()
            val promise = addInvocation.get(1, TimeUnit.SECONDS)
            closeInvocation.get(1, TimeUnit.SECONDS)

            await until { promise.isDone }
            promise.isDone.shouldBeTrue()
            reducer.close()
        }
    }

    @Test
    fun `close 후 use 패턴으로 안전하게 리소스를 정리할 수 있다`() {
        val result = concurrentReducerOf<String>(2, 10).use { reducer ->
            val promise = reducer.add { completableFutureOf("done") }
            await until { promise.isDone }
            promise.get()
        }
        result shouldBeEqualTo "done"
    }

    private fun job(future: CompletionStage<String>?): () -> CompletionStage<String>? = { future }

    private class CountingJob(
        private val activeCount: () -> Int,
        private val maxCount: AtomicInteger,
    ): () -> CompletionStage<String>? {

        companion object: KLoggingChannel()

        val future = CompletableFuture<String>()

        override fun invoke(): CompletionStage<String> {
            val count = activeCount()
            log.trace { "Active count=$count, maxCount=${maxCount.get()}" }
            if (count > maxCount.get()) {
                maxCount.set(count)
            }
            return future
        }
    }
}
