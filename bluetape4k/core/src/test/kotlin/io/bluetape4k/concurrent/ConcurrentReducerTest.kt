package io.bluetape4k.concurrent

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith

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
    fun `task return null`() {
        val reducer = concurrentReducerOf<String>(1, 10)
        val promise = reducer.add(job(null))

        promise.isDone.shouldBeTrue()
        val exception = promise.getException()
        exception shouldBeInstanceOf NullPointerException::class
    }

    @Test
    fun `when job throw exception`() {
        val reducer = concurrentReducerOf<String>(1, 10)
        val promise = reducer.add { throw IllegalStateException("Boom!") }

        promise.isDone.shouldBeTrue()
        promise.getException() shouldBeInstanceOf IllegalStateException::class
    }

    @Test
    fun `when job return failure`() {
        val reducer = concurrentReducerOf<String>(1, 10)
        val promise = reducer.add {
            failedCompletableFutureOf(IllegalStateException("Boom!"))
        }

        promise.isDone.shouldBeTrue()
        promise.getException() shouldBeInstanceOf IllegalStateException::class
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

        val jobs = fastListOf<CountingJob>()
        val promises = fastListOf<CompletableFuture<String>>()

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
        val reducer = concurrentReducerOf<String>(10, 10)
        repeat(20) {
            reducer.add { CompletableFuture() }
        }

        val promise = reducer.add { CompletableFuture() }
        await until { promise.isDone }

        promise.isDone.shouldBeTrue()
        promise.getException() shouldBeInstanceOf ConcurrentReducer.CapacityReachedException::class
    }

    @Test
    fun `큐 사이즈를 초과하여 작업을 추가하면, 예외를 담은 Completable을 반환한다`() {
        val concurrency = 4
        val queueSize = 10
        val future = CompletableFuture<String>()
        val reducer = concurrentReducerOf<String>(concurrency, queueSize)

        repeat(20) {
            reducer.add { future }
        }

        reducer.activeCount shouldBeEqualTo concurrency
        reducer.queuedCount shouldBeEqualTo queueSize
        reducer.remainingActiveCapacity shouldBeEqualTo 0
        reducer.remainingQueueCapacity shouldBeEqualTo 0

        future.complete("")

        await until { reducer.activeCount == 0 }

        reducer.activeCount shouldBeEqualTo 0
        reducer.queuedCount shouldBeEqualTo 0
        reducer.remainingActiveCapacity shouldBeEqualTo concurrency
        reducer.remainingQueueCapacity shouldBeEqualTo queueSize
    }

    @Test
    fun `close 호출 시 큐가 비워지고 더 이상 작업이 실행되지 않는다`() {
        val reducer = concurrentReducerOf<String>(1, 10)
        val request = CompletableFuture<String>()

        // 활성 작업 1개, 큐에 대기 작업 2개 추가
        reducer.add { request }
        reducer.add { CompletableFuture() }
        reducer.add { CompletableFuture() }

        reducer.activeCount shouldBeEqualTo 1
        reducer.queuedCount shouldBeEqualTo 2

        // close 호출
        reducer.close()

        // 큐가 비워져야 한다
        reducer.queuedCount shouldBeEqualTo 0
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
