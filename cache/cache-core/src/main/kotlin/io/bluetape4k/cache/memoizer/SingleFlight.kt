package io.bluetape4k.cache.memoizer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

/**
 * Coordinates same-key in-flight loading for blocking, [CompletableFuture], and suspend evaluators.
 *
 * ## Contract
 * - At most one evaluator runs for a key while its flight remains registered.
 * - [clear] increments the generation before removing in-flight entries. Callers that finish after
 *   [clear] can still receive their computed result, but [isCurrent] lets memoizers skip stale cache writes.
 * - Failed and cancelled evaluators are removed from the in-flight map so later calls can retry.
 * - Java futures that complete with `null` fail with [NullPointerException] because memoizer values are non-null.
 */
internal class SingleFlight<K: Any, V: Any> {

    private val generation = AtomicLong(0)
    private val blockingFlights = ConcurrentHashMap<K, BlockingFlight<V>>()
    private val asyncFlights = ConcurrentHashMap<K, CompletableFuture<V>>()
    private val suspendFlights = ConcurrentHashMap<K, CompletableDeferred<V>>()

    fun token(): SingleFlightToken =
        SingleFlightToken(generation.get())

    fun isCurrent(token: SingleFlightToken): Boolean =
        generation.get() == token.generation

    fun clear() {
        generation.incrementAndGet()
        blockingFlights.clear()
        asyncFlights.clear()
        suspendFlights.clear()
    }

    fun run(key: K, evaluator: (SingleFlightToken) -> V): V {
        val token = token()
        val flight = BlockingFlight<V>()
        val existing = blockingFlights.putIfAbsent(key, flight)
        if (existing != null) return existing.await()

        try {
            val result = evaluator(token)
            flight.complete(result)
            return result
        } catch (e: Throwable) {
            flight.completeExceptionally(e)
            throw e
        } finally {
            blockingFlights.remove(key, flight)
        }
    }

    fun runAsync(key: K, evaluator: (SingleFlightToken) -> CompletableFuture<V>): CompletableFuture<V> {
        val token = token()
        val promise = CompletableFuture<V>()
        val existing = asyncFlights.putIfAbsent(key, promise)
        if (existing != null) return existing

        try {
            evaluator(token).whenComplete { result, error ->
                asyncFlights.remove(key, promise)
                when {
                    error != null -> promise.completeExceptionally(error)
                    result == null -> promise.completeExceptionally(
                        NullPointerException("evaluator returned null for input $key")
                    )
                    else -> promise.complete(result)
                }
            }
        } catch (e: Throwable) {
            asyncFlights.remove(key, promise)
            promise.completeExceptionally(e)
        }

        return promise
    }

    suspend fun runSuspend(key: K, evaluator: suspend (SingleFlightToken) -> V): V {
        val token = token()
        val deferred = CompletableDeferred<V>()
        val existing = suspendFlights.putIfAbsent(key, deferred)
        if (existing != null) return existing.await()

        try {
            val result = evaluator(token)
            deferred.complete(result)
            return result
        } catch (e: CancellationException) {
            deferred.completeExceptionally(e)
            throw e
        } catch (e: Throwable) {
            deferred.completeExceptionally(e)
            throw e
        } finally {
            suspendFlights.remove(key, deferred)
        }
    }

    private class BlockingFlight<V: Any> {

        private val latch = CountDownLatch(1)

        @Volatile
        private var result: V? = null

        @Volatile
        private var error: Throwable? = null

        fun complete(value: V) {
            result = value
            latch.countDown()
        }

        fun completeExceptionally(cause: Throwable) {
            error = cause
            latch.countDown()
        }

        fun await(): V {
            try {
                latch.await()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw e
            }

            error?.let { throw it }
            @Suppress("UNCHECKED_CAST")
            return result as V
        }
    }
}

internal class SingleFlightToken internal constructor(
    internal val generation: Long,
)
