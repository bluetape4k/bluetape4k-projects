package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.Memoizer
import io.bluetape4k.cache.memoizer.SingleFlight
import io.bluetape4k.logging.KLogging
import java.util.concurrent.ConcurrentHashMap

/**
 * Creates an [InMemoryMemoizer] for this blocking evaluator.
 *
 * ```kotlin
 * val memo = ({ key: String -> key.length }).memoizer()
 * val result = memo("hello")
 * // result == 5
 * val result2 = memo("hello")  // returned immediately from the cache
 * // result2 == 5, returned from the cache
 * ```
 */
fun <T: Any, R: Any> ((T) -> R).memoizer(): InMemoryMemoizer<T, R> =
    InMemoryMemoizer(this)

/**
 * In-memory [Memoizer] that stores successful evaluator results in a local [ConcurrentHashMap].
 *
 * Same-key concurrent cache misses share one in-flight evaluator through [SingleFlight].
 * [clear] invalidates both cached values and in-flight write tokens. A caller whose evaluator
 * started before [clear] still receives its computed value, but that stale value is not written
 * back into the cache.
 *
 * ```kotlin
 * val memo = InMemoryMemoizer<String, Int> { key -> key.length }
 * val result = memo("hello")
 * // result == 5
 * ```
 */
class InMemoryMemoizer<in T: Any, out R: Any>(
    private val evaluator: (T) -> R,
): Memoizer<T, R> {

    companion object: KLogging()

    private val resultCache = ConcurrentHashMap<T, R>()
    private val singleFlight = SingleFlight<@UnsafeVariance T, @UnsafeVariance R>()

    override fun invoke(input: T): R {
        val cache = resultCache
        val flights = singleFlight

        cache[input]?.let { return it }

        return flights.run(input) { token ->
            cache[input]?.let { cached ->
                cached
            } ?: evaluator(input).also { result ->
                if (flights.isCurrent(token)) {
                    cache[input] = result
                }
            }
        }
    }

    override fun clear() {
        singleFlight.clear()
        resultCache.clear()
    }
}
