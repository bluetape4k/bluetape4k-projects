package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.SingleFlight
import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import java.util.concurrent.ConcurrentHashMap

/**
 * 이 suspend evaluator에 대한 [InMemorySuspendMemoizer]를 생성합니다.
 *
 * ```kotlin
 * val memo = (suspend { key: String -> key.length }).suspendMemoizer()
 * val result = memo("hello")
 * // result == 5
 * val result2 = memo("hello")  // returned immediately from the cache
 * // result2 == 5
 * ```
 */
fun <T: Any, R: Any> (suspend (T) -> R).suspendMemoizer(): InMemorySuspendMemoizer<T, R> =
    InMemorySuspendMemoizer(this)

/**
 * In-memory [SuspendMemoizer] that stores successful suspend evaluator results locally.
 *
 * ## Concurrency
 * Same-key concurrent cache misses share one suspend evaluator through [SingleFlight].
 * Failed or cancelled evaluators are removed from the in-flight map, so later calls can retry.
 *
 * ## Clear Semantics
 * [clear] invalidates cached values and in-flight write tokens. A caller whose evaluator started
 * before [clear] still receives its computed value, but that stale value is not written back into
 * the cache.
 *
 * ```kotlin
 * val memo = InMemorySuspendMemoizer<String, Int> { key -> key.length }
 * val result = memo("hello")
 * // result == 5
 * ```
 */
class InMemorySuspendMemoizer<in T: Any, out R: Any>(
    private val evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val resultCache = ConcurrentHashMap<T, R>()
    private val singleFlight = SingleFlight<@UnsafeVariance T, @UnsafeVariance R>()

    override suspend fun invoke(input: T): R {
        val cache = resultCache
        val flights = singleFlight

        cache[input]?.let { return it }

        return flights.runSuspend(input) { token ->
            cache[input]?.let { cached ->
                cached
            } ?: run {
                log.trace { "Cache miss for key: $input, evaluating..." }
                evaluator(input).also { result ->
                    if (flights.isCurrent(token)) {
                        cache[input] = result
                    }
                }
            }
        }
    }

    override suspend fun clear() {
        singleFlight.clear()
        resultCache.clear()
        log.trace { "Cleared in-memory cache." }
    }
}
