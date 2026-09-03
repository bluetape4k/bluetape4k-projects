package io.bluetape4k.concurrent

import io.bluetape4k.exceptions.BluetapeException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.utils.Runtimex
import kotlinx.atomicfu.atomic
import java.io.Closeable
import java.io.Serializable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Semaphore

/**
 * concurrentReducerOf 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val reducer = concurrentReducerOf<Int> { a, b -> a + b }
 * val result = reducer.reduce(listOf(1, 2, 3))
 * // result == 6
 * ```
 */
fun <T> concurrentReducerOf(
    maxConcurrency: Int = Runtimex.availableProcessors * 2,
    maxQueueSize: Int = 1000,
): ConcurrentReducer<T> {
    maxConcurrency.requirePositiveNumber("maxConcurrency")
    maxQueueSize.requirePositiveNumber("maxQueueSize")

    return ConcurrentReducer(maxConcurrency, maxQueueSize)
}

/**
 * 복수의 비동기 작업들을 [Semaphore]를 이용하여 제한된 숫자만큼만 동시에 실행되게끔 합니다.
 *
 * @property maxConcurrency 최대 동시 실행 가능한 작업 수 (Semaphore 수)
 * @property maxQueueSize   최대 큐 사이즈
 */
class ConcurrentReducer<T> internal constructor(
    private val maxConcurrency: Int,
    private val maxQueueSize: Int,
): Closeable {
    companion object: KLogging()

    private val queue: BlockingQueue<Job<T>> = ArrayBlockingQueue(maxQueueSize)
    private val limit: Semaphore = Semaphore(maxConcurrency)
    private val pumpExecutor = Executors.newSingleThreadExecutor()
    private val closed = atomic(false)
    private val pumpScheduled = atomic(false)
    private val admissionLock = Any()
    private val activeJobs = mutableSetOf<Job<T>>()

    val queuedCount: Int get() = queue.size
    val activeCount: Int get() = maxConcurrency - limit.availablePermits()
    val remainingQueueCapacity: Int get() = queue.remainingCapacity()
    val remainingActiveCapacity: Int get() = limit.availablePermits()

    /**
     * 비동기 작업을 추가합니다.
     * 큐가 꽉 찬 경우에는 [CapacityReachedException]이 발생합니다.
     * task invocation과 queue polling은 전용 executor에서 수행하므로 이 함수는 enqueue 후 즉시 반환합니다.
     *
     * @param task 작업을 수행할 람다
     * @return 작업 결과를 받아볼 [CompletableFuture] 인스턴스
     */
    fun add(task: () -> CompletionStage<T>?): CompletableFuture<T> {
        val promise = CompletableFuture<T>()
        val job = Job(task, promise)

        return synchronized(admissionLock) {
            when {
                closed.value ->
                    failedCompletableFutureOf(RejectedExecutionException("ConcurrentReducer is already closed."))

                !queue.offer(job) -> failedCompletableFutureOf(
                    CapacityReachedException("Queue size has reached capacity: $maxQueueSize"),
                )

                else -> {
                    schedulePump()
                    promise
                }
            }
        }
    }

    private fun pump() {
        if (closed.value) return

        do {
            val job = grabJob()
            if (job != null) {
                if (!cancelJob(job, requirePromiseCancellation = true)) {
                    run(job)
                }
            }
        } while (job != null)
    }

    private fun pollNextJob(predicate: (Job<T>) -> Boolean): Job<T>? {
        while (true) {
            val job = queue.poll() ?: return null
            if (predicate(job)) continue
            return job
        }
    }

    private fun grabJob(): Job<T>? = synchronized(admissionLock) {
        if (closed.value || !limit.tryAcquire()) {
            null
        } else {
            pollNextJob { it.promise.isCancelled }
                .also { job ->
                    if (job == null) {
                        limit.release()
                    } else {
                        activeJobs += job
                    }
                }
        }
    }

    private fun schedulePump() {
        if (!pumpScheduled.compareAndSet(false, true)) return

        try {
            pumpExecutor.execute {
                try {
                    pump()
                } finally {
                    pumpScheduled.value = false
                    if (!closed.value && queue.isNotEmpty() && limit.availablePermits() > 0) {
                        schedulePump()
                    }
                }
            }
        } catch (e: RejectedExecutionException) {
            pumpScheduled.value = false
            if (!closed.value) {
                log.warn(e) { "pump executor rejected pump task." }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun run(job: Job<T>) {
        if (!job.tryStart()) return

        var invocationFailed = false
        val future: CompletionStage<T>? = try {
            job.task.invoke()
        } catch (e: Throwable) {
            invocationFailed = true
            complete(job, error = e)
            log.warn(e) { "task failed. job=$job" }
            null
        }

        if (!invocationFailed) {
            if (future == null) {
                log.debug { "task result is null." }
                complete(job, error = NullPointerException("task result is null."))
            } else {
                job.stage = future
                if (job.isCancellationRequested) {
                    cancelStage(job)
                } else {
                    future.whenComplete { result, error ->
                        complete(job, result, error)

                        // whenComplete()는 현재 스레드에서 실행될 수 있으므로 coalesced pump를 executor에 위임한다.
                        if (!closed.value) schedulePump()
                    }
                }
            }
        }
    }

    private fun complete(job: Job<T>, result: T? = null, error: Throwable? = null) {
        val completed = synchronized(admissionLock) {
            if (!job.tryComplete()) {
                false
            } else {
                activeJobs.remove(job)
                limit.release()
                true
            }
        }
        if (!completed) return

        if (error != null) {
            job.promise.completeExceptionally(error)
        } else {
            job.promise.complete(result)
        }
    }

    private fun cancelJob(job: Job<T>, requirePromiseCancellation: Boolean): Boolean {
        val cancelled = synchronized(admissionLock) {
            cancelJobLocked(job, requirePromiseCancellation)
        }
        if (cancelled) {
            job.promise.cancel(false)
            cancelStage(job)
        }
        return cancelled
    }

    private fun cancelJobLocked(job: Job<T>, requirePromiseCancellation: Boolean): Boolean {
        val canCancel = !requirePromiseCancellation || job.promise.isCancelled
        val cancelled = canCancel && job.tryCancel()
        if (cancelled) {
            if (activeJobs.remove(job)) {
                limit.release()
            }
        }
        return cancelled
    }

    @Suppress("TooGenericExceptionCaught")
    private fun cancelStage(job: Job<T>) {
        val stage = job.stage ?: return
        if (!job.tryCancelStage()) return

        try {
            stage.toCompletableFuture().cancel(false)
        } catch (e: RuntimeException) {
            log.warn(e) { "task stage cancellation failed." }
        }
    }

    /**
     * 내부 리소스를 정리합니다.
     * 큐에 남아있거나 실행 중인 작업은 취소되고, pump executor를 종료합니다.
     */
    override fun close() {
        val jobsToCancel = synchronized(admissionLock) {
            if (!closed.compareAndSet(false, true)) return

            val queuedJobs = mutableListOf<Job<T>>()
            while (true) {
                queuedJobs += queue.poll() ?: break
            }

            val activeJobs = activeJobs.toList()
            val jobs = queuedJobs + activeJobs
            val cancelledJobs = jobs.filter { cancelJobLocked(it, requirePromiseCancellation = false) }
            pumpExecutor.shutdown()
            cancelledJobs
        }

        jobsToCancel.forEach { job ->
            job.promise.cancel(false)
            cancelStage(job)
        }
    }

    private data class Job<T>(
        val task: () -> CompletionStage<T>?,
        val promise: CompletableFuture<T>,
    ): Serializable {
        companion object {
            private const val NEW = 0
            private const val RUNNING = 1
            private const val CANCELLED = 2
            private const val COMPLETED = 3
            private const val serialVersionUID: Long = 1L
        }

        private val state = atomic(NEW)
        private val stageCancellationRequested = atomic(false)

        @Volatile
        var stage: CompletionStage<T>? = null

        val isCancellationRequested: Boolean
            get() = state.value == CANCELLED

        fun tryStart(): Boolean = state.compareAndSet(NEW, RUNNING)

        fun tryComplete(): Boolean = state.compareAndSet(RUNNING, COMPLETED)

        fun tryCancelStage(): Boolean = stageCancellationRequested.compareAndSet(false, true)

        fun tryCancel(): Boolean {
            while (true) {
                when (val current = state.value) {
                    NEW, RUNNING -> if (state.compareAndSet(current, CANCELLED)) return true
                    else -> return false
                }
            }
        }

    }

    class CapacityReachedException: BluetapeException {
        constructor(): super()
        constructor(message: String): super(message)
        constructor(message: String, cause: Throwable): super(message, cause)
        constructor(cause: Throwable?): super(cause = cause)
    }
}
