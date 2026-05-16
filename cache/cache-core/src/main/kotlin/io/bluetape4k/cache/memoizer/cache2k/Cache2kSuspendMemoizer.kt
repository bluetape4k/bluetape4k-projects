package io.bluetape4k.cache.memoizer.cache2k

import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Cache2k를 사용하는 suspend memoizer를 생성합니다.
 *
 * ```kotlin
 * val memo = cache.suspendMemoizer<String, Int> { it.length }
 * // memo("abcd") == 4
 * ```
 */
fun <T: Any, R: Any> org.cache2k.Cache<T, R>.suspendMemoizer(
    evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> =
    Cache2kSuspendMemoizer(this, evaluator)

/**
 * suspend 함수를 Cache2k 기반 memoizer로 감쌉니다.
 *
 * ```kotlin
 * val memo = ({ key: String -> key.length }).withSuspendMemoizer(cache)
 * // memo("abc") == 3
 * ```
 */
fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemoizer(
    cache: org.cache2k.Cache<T, R>,
): SuspendMemoizer<T, R> =
    Cache2kSuspendMemoizer(cache, this)

/**
 * Cache2k 기반 [SuspendMemoizer] 구현체입니다.
 *
 * ```kotlin
 * val memo = SuspendCache2kMemoizer(cache) { key: String -> key.length }
 * // memo("abcd") == 4
 * ```
 */
/**
 * Cache2k-backed [SuspendMemoizer] that deduplicates concurrent evaluations per key.
 *
 * ## Recursion Safety
 * Holding a global Mutex during evaluator execution causes deadlocks for recursive memoizers
 * (e.g., factorial, fibonacci) because Kotlin's Mutex is not reentrant.
 * The per-key Deferred pattern runs the evaluator outside any lock, so recursive calls are safe.
 *
 * ## Write-after-clear Safety
 * A generation counter ensures that results from in-flight evaluations started before [clear]
 * are not written back to the cache after it has been invalidated. The caller always receives
 * the computed value regardless of the generation check.
 *
 * ```kotlin
 * val memo = Cache2kSuspendMemoizer(cache) { key: String -> key.length }
 * // memo("abcd") == 4
 * ```
 */
class Cache2kSuspendMemoizer<in T: Any, out R: Any>(
    private val cache: org.cache2k.Cache<T, R>,
    private val evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val inflightMap = ConcurrentHashMap<T, Deferred<R>>()
    private val clearMutex = Mutex()
    private val generation = AtomicLong(0)

    override suspend fun invoke(input: T): R {
        cache.get(input)?.let { return it }

        return coroutineScope {
            val capturedGen = generation.get()
            val deferred = inflightMap.computeIfAbsent(input) {
                async { evaluator(input) }
            }
            try {
                val result = deferred.await()
                if (generation.get() == capturedGen) {
                    this@Cache2kSuspendMemoizer.cache.put(input, result)
                }
                result
            } finally {
                if (deferred.isCompleted || deferred.isCancelled) {
                    inflightMap.remove(input, deferred)
                }
            }
        }
    }

    override suspend fun clear() {
        generation.incrementAndGet()
        clearMutex.withLock {
            inflightMap.clear()
            cache.clear()
        }
    }
}
