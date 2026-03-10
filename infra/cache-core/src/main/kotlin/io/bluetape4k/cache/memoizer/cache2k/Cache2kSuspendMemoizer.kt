package io.bluetape4k.cache.memoizer.cache2k

import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Cache2k를 사용하는 suspend memoizer를 생성합니다.
 *
 * ```kotlin
 * val memo = cache.suspendMemoizer<String, Int> { it.length }
 * // memo("abcd") == 4
 * ```
 */
fun <T: Any, R: Any> org.cache2k.Cache<T, R>.suspendMemoizer(
    @BuilderInference evaluator: suspend (T) -> R,
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
class Cache2kSuspendMemoizer<in T: Any, out R: Any>(
    private val cache: org.cache2k.Cache<T, R>,
    @BuilderInference private val evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val mutex = Mutex()

    override suspend fun invoke(input: T): R {
        return cache.get(input) ?: run {
            val result = evaluator(input)
            this@Cache2kSuspendMemoizer.cache.put(input, result)
            result
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
}
