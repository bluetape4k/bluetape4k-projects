package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationDeadline
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationScheduledHandle
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationScheduler
import io.bluetape4k.redis.lettuce.lock.LockAcquireResult
import io.bluetape4k.redis.lettuce.lock.LockHandle
import io.bluetape4k.redis.lettuce.lock.toRedisMillisCeil
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport
import kotlin.time.toKotlinDuration

internal val DISTRIBUTED_LOCK_RETRY_DELAY: Duration = Duration.ofMillis(10)

internal class LockWaitSupport(
    private val registration: CoordinationRuntime.CoordinationObjectRegistration,
    private val isClosed: () -> Boolean,
) {
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
        if (isClosed()) return CompletableFuture.completedFuture(LockAcquireResult.Closed)
        val result = CompletableFuture<LockAcquireResult<LockHandle>>()

        lateinit var retry: () -> Unit
        retry = {
            if (isClosed()) {
                result.complete(LockAcquireResult.Closed)
            } else if (!result.isDone) {
                attempt().whenComplete { acquired, error ->
                    if (error != null) {
                        result.completeExceptionally(error)
                    } else if (acquired is LockAcquireResult.Contended) {
                        val remaining = deadline.remainingNanos()
                        if (remaining == 0L) {
                            result.complete(LockAcquireResult.TimedOut)
                        } else {
                            val delay = minOf(remaining, DISTRIBUTED_LOCK_RETRY_DELAY.toNanos())
                            registration.registerTask(Duration.ofNanos(delay).toKotlinDuration(), retry)
                        }
                    } else {
                        result.complete(acquired)
                    }
                }
            }
        }
        retry()
        return result
    }

    suspend fun acquireSuspending(
        waitTime: Duration,
        attempt: suspend () -> LockAcquireResult<LockHandle>,
    ): LockAcquireResult<LockHandle> {
        val deadline = deadline(waitTime)
        while (true) {
            currentCoroutineContext().ensureActive()
            when (val result = attempt()) {
                is LockAcquireResult.Contended -> {
                    val remaining = deadline.remainingNanos()
                    if (remaining == 0L) return LockAcquireResult.TimedOut
                    val retryDelay = Duration.ofNanos(
                        minOf(remaining, DISTRIBUTED_LOCK_RETRY_DELAY.toNanos()),
                    )
                    delay(retryDelay.toKotlinDuration())
                }
                else -> return result
            }
        }
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
