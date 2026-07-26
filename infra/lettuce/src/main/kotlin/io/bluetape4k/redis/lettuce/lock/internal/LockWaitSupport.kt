package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationCapacityException
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationDeadline
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationScheduledHandle
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationScheduler
import io.bluetape4k.redis.lettuce.lock.LockAcquireResult
import io.bluetape4k.redis.lettuce.lock.LockHandle
import io.bluetape4k.redis.lettuce.lock.toRedisMillisCeil
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.future.await
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.LockSupport
import kotlin.time.toKotlinDuration

internal val DISTRIBUTED_LOCK_RETRY_DELAY: Duration = Duration.ofMillis(10)

internal class LockWaitSupport(
    private val registration: CoordinationRuntime.CoordinationObjectRegistration,
    private val isClosed: () -> Boolean,
) {
    private val closing = AtomicBoolean()
    private val pendingAsync =
        ConcurrentHashMap.newKeySet<CompletableFuture<LockAcquireResult<LockHandle>>>()
    private val pendingSuspensions = ConcurrentHashMap.newKeySet<CompletableFuture<RetrySignal>>()

    init {
        registration.onClose(::close)
    }

    fun acquire(
        waitTime: Duration,
        attempt: () -> LockAcquireResult<LockHandle>,
    ): LockAcquireResult<LockHandle> {
        val deadline = deadline(waitTime)
        while (true) {
            when (val result = attempt()) {
                is LockAcquireResult.Contended -> {
                    val remaining = deadline.remainingNanos()
                    if (remaining == 0L) return LockAcquireResult.TimedOut
                    LockSupport.parkNanos(minOf(remaining, DISTRIBUTED_LOCK_RETRY_DELAY.toNanos()))
                }
                else -> return result
            }
        }
    }

    fun acquireAsync(
        waitTime: Duration,
        attempt: () -> CompletableFuture<LockAcquireResult<LockHandle>>,
    ): CompletableFuture<LockAcquireResult<LockHandle>> {
        val deadline = deadline(waitTime)
        if (isLifecycleClosed()) return CompletableFuture.completedFuture(LockAcquireResult.Closed)
        val result = CompletableFuture<LockAcquireResult<LockHandle>>()
        val scheduled = AtomicReference<CoordinationRuntime.CoordinationTaskRegistration?>()
        val inFlight = AtomicReference<CompletableFuture<LockAcquireResult<LockHandle>>?>()
        pendingAsync += result

        result.whenComplete { _, _ ->
            pendingAsync -= result
            scheduled.getAndSet(null)?.close()
            inFlight.getAndSet(null)?.let { pending ->
                if (!pending.isDone) {
                    pending.cancel(false)
                }
            }
        }

        lateinit var retry: () -> Unit
        fun scheduleRetry(delay: Duration) {
            if (result.isDone) return
            try {
                val task = registration.registerTask(delay.toKotlinDuration(), retry)
                scheduled.getAndSet(task)?.close()
                if (result.isDone) {
                    scheduled.getAndSet(null)?.close()
                }
            } catch (_: CoordinationCapacityException) {
                result.complete(LockAcquireResult.CapacityExceeded)
            } catch (_: IllegalStateException) {
                result.complete(LockAcquireResult.Closed)
            }
        }
        retry = {
            scheduled.getAndSet(null)?.close()
            if (isLifecycleClosed()) {
                result.complete(LockAcquireResult.Closed)
            } else if (!result.isDone) {
                val pending = try {
                    attempt()
                } catch (error: Throwable) {
                    result.completeExceptionally(error)
                    null
                }
                if (pending != null) {
                    inFlight.set(pending)
                    if (result.isDone) {
                        inFlight.getAndSet(null)?.cancel(false)
                    }
                    pending.whenComplete { acquired, error ->
                        inFlight.compareAndSet(pending, null)
                        if (result.isDone) {
                            return@whenComplete
                        }
                        if (error != null) {
                            result.completeExceptionally(error)
                        } else if (acquired is LockAcquireResult.Contended) {
                            val remaining = deadline.remainingNanos()
                            if (remaining == 0L) {
                                result.complete(LockAcquireResult.TimedOut)
                            } else {
                                val delay = minOf(remaining, DISTRIBUTED_LOCK_RETRY_DELAY.toNanos())
                                scheduleRetry(Duration.ofNanos(delay))
                            }
                        } else {
                            result.complete(acquired)
                        }
                    }
                }
            }
        }
        scheduleRetry(Duration.ZERO)
        return result
    }

    suspend fun acquireSuspending(
        waitTime: Duration,
        attempt: suspend () -> LockAcquireResult<LockHandle>,
    ): LockAcquireResult<LockHandle> {
        val deadline = deadline(waitTime)
        while (true) {
            currentCoroutineContext().ensureActive()
            if (isLifecycleClosed()) return LockAcquireResult.Closed
            when (val result = attempt()) {
                is LockAcquireResult.Contended -> {
                    val remaining = deadline.remainingNanos()
                    if (remaining == 0L) return LockAcquireResult.TimedOut
                    val retryDelay = Duration.ofNanos(
                        minOf(remaining, DISTRIBUTED_LOCK_RETRY_DELAY.toNanos()),
                    )
                    when (awaitRetry(retryDelay)) {
                        RetrySignal.RETRY -> Unit
                        RetrySignal.CLOSED -> return LockAcquireResult.Closed
                        RetrySignal.CAPACITY_EXCEEDED -> return LockAcquireResult.CapacityExceeded
                    }
                }
                else -> return result
            }
        }
    }

    fun close() {
        if (closing.compareAndSet(false, true)) {
            pendingAsync.forEach { pending ->
                pending.complete(LockAcquireResult.Closed)
            }
            pendingSuspensions.forEach { pending ->
                pending.complete(RetrySignal.CLOSED)
            }
        }
    }

    private suspend fun awaitRetry(delay: Duration): RetrySignal {
        if (isLifecycleClosed()) return RetrySignal.CLOSED
        val signal = CompletableFuture<RetrySignal>()
        val scheduled = AtomicReference<CoordinationRuntime.CoordinationTaskRegistration?>()
        pendingSuspensions += signal
        signal.whenComplete { _, _ ->
            pendingSuspensions -= signal
            scheduled.getAndSet(null)?.close()
        }
        if (isLifecycleClosed()) {
            signal.complete(RetrySignal.CLOSED)
        } else {
            try {
                val task = registration.registerTask(delay.toKotlinDuration()) {
                    signal.complete(RetrySignal.RETRY)
                }
                scheduled.set(task)
                if (signal.isDone) {
                    scheduled.getAndSet(null)?.close()
                }
            } catch (_: CoordinationCapacityException) {
                signal.complete(RetrySignal.CAPACITY_EXCEEDED)
            } catch (_: IllegalStateException) {
                signal.complete(RetrySignal.CLOSED)
            }
        }
        return signal.await()
    }

    private fun isLifecycleClosed(): Boolean = closing.get() || isClosed() || registration.isClosed

    private enum class RetrySignal {
        RETRY,
        CLOSED,
        CAPACITY_EXCEEDED,
    }

    private fun deadline(waitTime: Duration): CoordinationDeadline {
        waitTime.toRedisMillisCeil()
        require(waitTime <= MAX_DISTRIBUTED_LOCK_WAIT) {
            "Lock wait time must not exceed $MAX_DISTRIBUTED_LOCK_WAIT."
        }
        return CoordinationDeadline.after(waitTime.toKotlinDuration())
    }
}

internal class ScheduledExecutorCoordinationScheduler(
    private val executor: ScheduledExecutorService,
): CoordinationScheduler {
    override val isShutdown: Boolean
        get() = executor.isShutdown

    override fun schedule(
        delay: kotlin.time.Duration,
        task: () -> Unit,
    ): CoordinationScheduledHandle {
        val future = executor.schedule(task, delay.inWholeNanoseconds, TimeUnit.NANOSECONDS)
        return CoordinationScheduledHandle { future.cancel(false) }
    }

    override fun shutdown() {
        // The caller owns injected schedulers.
    }
}

private val MAX_DISTRIBUTED_LOCK_WAIT: Duration = Duration.ofHours(24)
