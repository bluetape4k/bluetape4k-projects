package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationCapacityException
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationDeadline
import io.bluetape4k.redis.lettuce.coordination.internal.MonotonicTicker
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationScheduledHandle
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationScheduler
import io.bluetape4k.redis.lettuce.lock.LockAcquireResult
import io.bluetape4k.redis.lettuce.lock.LockHandle
import io.bluetape4k.redis.lettuce.lock.LockOutcome
import io.bluetape4k.redis.lettuce.lock.SpinLockConfig
import io.bluetape4k.redis.lettuce.lock.toRedisMillisCeil
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.future.await
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.LockSupport
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.time.toKotlinDuration

internal val DISTRIBUTED_LOCK_RETRY_DELAY: Duration = Duration.ofMillis(10)

internal fun interface LockRetryPolicy {
    fun delay(attempt: Int, remainingNanos: Long): Duration
}

internal object DistributedLockRetryPolicy: LockRetryPolicy {
    override fun delay(attempt: Int, remainingNanos: Long): Duration {
        require(attempt > 0) { "Retry attempt must be positive." }
        require(remainingNanos > 0L) { "Retry deadline must have remaining time." }
        return Duration.ofNanos(minOf(remainingNanos, DISTRIBUTED_LOCK_RETRY_DELAY.toNanos()))
    }
}

internal class SpinLockRetryPolicy(
    private val config: SpinLockConfig,
    private val jitterSource: () -> Double = { ThreadLocalRandom.current().nextDouble() },
): LockRetryPolicy {
    private val initialNanos = config.initialDelay.saturatedNanos()
    private val maxNanos = config.maxDelay.saturatedNanos()
    private val minimumAttemptIntervalNanos =
        ceil(NANOS_PER_SECOND.toDouble() / config.maxAttemptsPerSecond).toLong()

    init {
        require(maxNanos >= minimumAttemptIntervalNanos) {
            "Spin maximum delay must permit the configured attempt-rate bound."
        }
    }

    override fun delay(attempt: Int, remainingNanos: Long): Duration {
        require(attempt > 0) { "Retry attempt must be positive." }
        require(remainingNanos > 0L) { "Retry deadline must have remaining time." }
        val exponential = initialNanos.toDouble() * config.multiplier.pow((attempt - 1).coerceAtMost(1_024))
        val baseNanos = maxOf(
            minimumAttemptIntervalNanos,
            minOf(maxNanos.toDouble(), exponential).toLong(),
        )
        val jitteredNanos =
            if (config.jitterRatio == 0.0) {
                baseNanos
            } else {
                val sample = jitterSource()
                require(sample.isFinite() && sample in 0.0..1.0) {
                    "Spin jitter source must return a finite value between 0.0 and 1.0."
                }
                minOf(
                    maxNanos.toDouble(),
                    baseNanos.toDouble() * (1.0 + config.jitterRatio * sample),
                ).toLong()
            }
        return Duration.ofNanos(minOf(remainingNanos, jitteredNanos))
    }
}

internal class LockWaitSupport(
    private val registration: CoordinationRuntime.CoordinationObjectRegistration,
    private val isClosed: () -> Boolean,
    private val retryPolicy: LockRetryPolicy = DistributedLockRetryPolicy,
    private val ticker: MonotonicTicker = MonotonicTicker.SYSTEM,
    private val releaseAbandoned: (LockHandle) -> Unit = {},
    private val waitObservation: LockWaitObservation? = null,
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
        val observation = waitObservation?.begin()
        var retryAttempt = 0
        var outcome = LockOutcome.CANCELLED
        try {
            while (true) {
                when (val result = attempt()) {
                    is LockAcquireResult.Contended -> {
                        observation?.onContended()
                        val remaining = deadline.remainingNanos()
                        if (remaining == 0L) {
                            outcome = LockOutcome.TIMED_OUT
                            return LockAcquireResult.TimedOut
                        }
                        LockSupport.parkNanos(retryPolicy.delay(++retryAttempt, remaining).toNanos())
                    }
                    else -> {
                        outcome = result.observationOutcome()
                        return result
                    }
                }
            }
        } finally {
            observation?.complete(outcome)
        }
    }

    fun acquireAsync(
        waitTime: Duration,
        abandonedRelease: (LockHandle) -> Unit = releaseAbandoned,
        attempt: () -> CompletableFuture<LockAcquireResult<LockHandle>>,
    ): CompletableFuture<LockAcquireResult<LockHandle>> {
        val deadline = deadline(waitTime)
        if (isLifecycleClosed()) return CompletableFuture.completedFuture(LockAcquireResult.Closed)
        val observation = waitObservation?.begin()
        val result = CompletableFuture<LockAcquireResult<LockHandle>>()
        val scheduled = AtomicReference<CoordinationRuntime.CoordinationTaskRegistration?>()
        val inFlight = AtomicReference<CompletableFuture<LockAcquireResult<LockHandle>>?>()
        val attempts = AtomicInteger()
        pendingAsync += result

        result.whenComplete { value, error ->
            pendingAsync -= result
            scheduled.getAndSet(null)?.close()
            inFlight.getAndSet(null)?.let { pending ->
                if (!pending.isDone) {
                    pending.cancel(false)
                }
            }
            observation?.complete(
                when {
                    result.isCancelled -> LockOutcome.CANCELLED
                    error != null -> LockOutcome.BACKEND_FAILED
                    else -> value.observationOutcome()
                },
            )
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
                            acquired.acquiredHandleOrNull()?.let(abandonedRelease)
                            return@whenComplete
                        }
                        if (error != null) {
                            result.completeExceptionally(error)
                        } else if (acquired is LockAcquireResult.Contended) {
                            observation?.onContended()
                            val remaining = deadline.remainingNanos()
                            if (remaining == 0L) {
                                result.complete(LockAcquireResult.TimedOut)
                            } else {
                                scheduleRetry(retryPolicy.delay(attempts.incrementAndGet(), remaining))
                            }
                        } else {
                            if (!result.complete(acquired)) {
                                acquired.acquiredHandleOrNull()?.let(abandonedRelease)
                            }
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
        val observation = waitObservation?.begin()
        var retryAttempt = 0
        var outcome = LockOutcome.CANCELLED
        try {
            while (true) {
                currentCoroutineContext().ensureActive()
                if (isLifecycleClosed()) {
                    outcome = LockOutcome.CLOSED
                    return LockAcquireResult.Closed
                }
                when (val result = attempt()) {
                    is LockAcquireResult.Contended -> {
                        observation?.onContended()
                        val remaining = deadline.remainingNanos()
                        if (remaining == 0L) {
                            outcome = LockOutcome.TIMED_OUT
                            return LockAcquireResult.TimedOut
                        }
                        val retryDelay = retryPolicy.delay(++retryAttempt, remaining)
                        when (awaitRetry(retryDelay)) {
                            RetrySignal.RETRY -> Unit
                            RetrySignal.CLOSED -> {
                                outcome = LockOutcome.CLOSED
                                return LockAcquireResult.Closed
                            }
                            RetrySignal.CAPACITY_EXCEEDED -> {
                                outcome = LockOutcome.CAPACITY_REJECTED
                                return LockAcquireResult.CapacityExceeded
                            }
                        }
                    }
                    else -> {
                        outcome = result.observationOutcome()
                        return result
                    }
                }
            }
        } finally {
            observation?.complete(outcome)
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
        return CoordinationDeadline.after(waitTime.toKotlinDuration(), ticker)
    }
}

internal fun LockAcquireResult<*>?.observationOutcome(): LockOutcome =
    when (this) {
        is LockAcquireResult.Acquired,
        is LockAcquireResult.Reentered,
        -> LockOutcome.SUCCEEDED
        is LockAcquireResult.Contended -> LockOutcome.CONTENDED
        LockAcquireResult.TimedOut -> LockOutcome.TIMED_OUT
        LockAcquireResult.CleanupPending -> LockOutcome.CONTENDED
        LockAcquireResult.CapacityExceeded -> LockOutcome.CAPACITY_REJECTED
        LockAcquireResult.Closed, null -> LockOutcome.CLOSED
        is LockAcquireResult.BackendFailure -> LockOutcome.BACKEND_FAILED
        is LockAcquireResult.IntegrityFailure -> LockOutcome.INTEGRITY_FAILED
        is LockAcquireResult.Ambiguous -> LockOutcome.AMBIGUOUS
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
private const val NANOS_PER_SECOND: Long = 1_000_000_000L

private fun Duration.saturatedNanos(): Long =
    try {
        toNanos()
    } catch (_: ArithmeticException) {
        Long.MAX_VALUE
    }

private fun LockAcquireResult<LockHandle>?.acquiredHandleOrNull(): LockHandle? =
    when (this) {
        is LockAcquireResult.Acquired -> handle
        is LockAcquireResult.Reentered -> handle
        else -> null
    }
