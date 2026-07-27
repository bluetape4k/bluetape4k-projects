package io.bluetape4k.redis.lettuce.synchronizer.internal

import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationCapacityException
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.toKotlinDuration

/**
 * Runs synchronizer retries on the connection-owned coordination runtime.
 *
 * Every pending retry consumes one bounded runtime registration. Closing the object completes
 * pending operations and cancels their scheduled and in-flight work.
 */
internal class SynchronizerAsyncPoller(
    private val registration: CoordinationRuntime.CoordinationObjectRegistration,
) : AutoCloseable {
    private val pending = ConcurrentHashMap<CompletableFuture<*>, () -> Unit>()

    init {
        registration.onClose {
            pending.values.toList().forEach { it() }
        }
    }

    fun <T> poll(
        deadlineNanos: Long,
        interval: Duration,
        closedResult: T,
        capacityResult: T,
        timedOutResult: T,
        shouldRetry: (T) -> Boolean,
        attempt: () -> CompletableFuture<T>,
    ): CompletableFuture<T> {
        val result = CompletableFuture<T>()
        val scheduled = AtomicReference<CoordinationRuntime.CoordinationTaskRegistration?>()
        val inFlight = AtomicReference<CompletableFuture<T>?>()
        val closeAction = { result.complete(closedResult); Unit }
        pending[result] = closeAction
        result.whenComplete { _, _ ->
            pending.remove(result)
            scheduled.getAndSet(null)?.close()
            inFlight.getAndSet(null)?.cancel(false)
        }

        lateinit var retry: () -> Unit
        fun schedule(delay: Duration) {
            if (result.isDone) return
            try {
                val task = registration.registerTask(delay.toKotlinDuration(), retry)
                scheduled.getAndSet(task)?.close()
                if (result.isDone) scheduled.getAndSet(null)?.close()
            } catch (_: CoordinationCapacityException) {
                result.complete(capacityResult)
            } catch (_: IllegalStateException) {
                result.complete(closedResult)
            }
        }
        retry = {
            scheduled.getAndSet(null)?.close()
            if (registration.isClosed) {
                result.complete(closedResult)
            } else if (!result.isDone) {
                val future = try {
                    attempt()
                } catch (error: Throwable) {
                    result.completeExceptionally(error)
                    null
                }
                if (future != null) {
                    inFlight.set(future)
                    if (result.isDone) inFlight.getAndSet(null)?.cancel(false)
                    future.whenComplete { value, error ->
                        inFlight.compareAndSet(future, null)
                        when {
                            result.isDone -> Unit
                            error != null -> result.completeExceptionally(error)
                            !shouldRetry(value) -> result.complete(value)
                            System.nanoTime() >= deadlineNanos -> result.complete(timedOutResult)
                            else -> schedule(interval)
                        }
                    }
                }
            }
        }
        schedule(Duration.ZERO)
        return result
    }

    override fun close() {
        registration.close()
    }
}
