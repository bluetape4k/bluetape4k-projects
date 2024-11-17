package io.bluetape4k.concurrent

import io.bluetape4k.exceptions.BluetapeException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.sync.Semaphore
import java.io.Serializable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit

/**
 * 복수의 비동기 작업들을 [Semaphore]를 이용하여 제한된 숫자만큼만 동시에 실행되게끔 합니다.
 *
 * @property maxConcurrency 최대 동시 실행 가능한 작업 수 (Semaphore 수)
 * @property maxQueueSize   최대 큐 사이즈
 */
class ConcurrentReducer<T> private constructor(
    private val maxConcurrency: Int,
    private val maxQueueSize: Int,
) {

    companion object: KLogging() {
        @JvmStatic
        operator fun <T> invoke(
            maxConcurrency: Int = Runtimex.availableProcessors * 2,
            maxQueueSize: Int = 1000,
        ): ConcurrentReducer<T> {
            maxConcurrency.requirePositiveNumber("maxConcurrency")
            maxQueueSize.requirePositiveNumber("maxQueueSize")

            return ConcurrentReducer(maxConcurrency, maxQueueSize)
        }
    }

    private val queue: BlockingQueue<Job<T>> = ArrayBlockingQueue(maxQueueSize)
    private val limit: Semaphore = Semaphore(maxConcurrency)

    val queuedCount: Int get() = queue.size
    val activeCount: Int get() = maxConcurrency - limit.availablePermits
    val remainingQueueCapacity: Int get() = queue.remainingCapacity()
    val remainingActiveCapacity: Int get() = limit.availablePermits

    /**
     * 비동기 작업을 추가합니다.
     * 큐가 꽉 찬 경우에는 [CapacityReachedException]이 발생합니다.
     *
     * @param task 작업을 수행할 람다
     * @return 작업 결과를 받아볼 [CompletableFuture] 인스턴스
     */
    fun add(task: () -> CompletionStage<T>?): CompletableFuture<T> {
        val promise = CompletableFuture<T>()
        val job = Job(task, promise)

        if (!queue.offer(job)) {
            return failedCompletableFutureOf(CapacityReachedException("Queue size has reached capacity: $maxQueueSize"))
        }
        pump()
        return promise
    }

    private fun pump() {
        var job = grabJob()
        while (job != null) {
            if (job.promise.isCancelled) {
                limit.release()
            } else {
                run(job)
            }
            job = grabJob()
        }
    }

    private fun grabJob(): Job<T>? {
        if (!limit.tryAcquire()) {
            return null
        }
        val job = queue.poll(10, TimeUnit.MILLISECONDS)
        if (job != null) {
            return job
        }
        limit.release()
        return null
    }

    private fun run(job: Job<T>) {
        val future: CompletionStage<T>?
        try {
            future = job.task.invoke()
            if (future == null) {
                log.debug { "task result is null." }
                limit.release()
                job.promise.completeExceptionally(NullPointerException("task result is null."))
                return
            }
        } catch (e: Throwable) {
            limit.release()
            job.promise.completeExceptionally(e)
            log.warn(e) { "task failed. job=$job" }
            return
        }

        future.whenComplete { result, error ->
            limit.release()
            if (error != null) {
                job.promise.completeExceptionally(error)
            } else {
                job.promise.complete(result)
            }
            pump()
        }
    }

    private data class Job<T>(
        val task: () -> CompletionStage<T>?,
        val promise: CompletableFuture<T>,
    ): Serializable

    class CapacityReachedException: BluetapeException {
        constructor(): super()
        constructor(message: String): super(message)
        constructor(message: String, cause: Throwable): super(message, cause)
        constructor(cause: Throwable): super(cause)
    }
}
