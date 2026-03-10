package io.bluetape4k.cache.memoizer.ehcache

import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ehcache를 사용하는 suspend memoizer를 생성합니다.
 *
 * ```kotlin
 * val memo = cache.suspendMemoizer<String, Int> { it.length }
 * // memo("abcd") == 4
 * ```
 */
fun <T: Any, R: Any> org.ehcache.Cache<T, R>.suspendMemoizer(
    @BuilderInference evaluator: suspend (T) -> R,
): SuspendEhCacheMemoizer<T, R> =
    SuspendEhCacheMemoizer(this, evaluator)

/**
 * suspend 함수를 Ehcache 기반 memoizer로 감쌉니다.
 *
 * ```kotlin
 * val memo = ({ key: String -> key.length }).withSuspendMemoizer(cache)
 * // memo("abc") == 3
 * ```
 */
fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemoizer(
    cache: org.ehcache.Cache<T, R>,
): SuspendEhCacheMemoizer<T, R> =
    SuspendEhCacheMemoizer(cache, this)

/**
 * Ehcache 기반 [SuspendMemoizer] 구현체입니다.
 *
 * ```kotlin
 * val memo = SuspendEhCacheMemoizer(cache) { key: String -> key.length }
 * // memo("abcd") == 4
 * ```
 */
class SuspendEhCacheMemoizer<T: Any, R: Any>(
    private val cache: org.ehcache.Cache<T, R>,
    @BuilderInference private val evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> {

    companion object: KLogging()

    private val mutex = Mutex()

    override suspend fun invoke(input: T): R {
        return cache.get(input)
            ?: run {
                val result = evaluator(input)
                cache.put(input, result)
                result
            }
    }

    override suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
}
