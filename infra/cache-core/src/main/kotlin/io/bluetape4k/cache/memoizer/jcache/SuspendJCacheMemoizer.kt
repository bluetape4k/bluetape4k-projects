package io.bluetape4k.cache.memoizer.jcache

import io.bluetape4k.cache.jcache.getOrPut
import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * JCache를 사용하는 suspend memoizer를 생성합니다.
 *
 * ```kotlin
 * val cachingProvider = Caching.getCachingProvider()
 * val cacheManager = cachingProvider.cacheManager
 * val cache = cacheManager.getCache<String, Int>("myCache")
 * val memo = cache.suspendMemoizer { key -> key.length }
 * val result = memo("hello")
 * // result == 5
 * ```
 */
fun <T: Any, R: Any> javax.cache.Cache<T, R>.suspendMemoizer(
    evaluator: suspend (T) -> R,
): SuspendJCacheMemoizer<T, R> =
    SuspendJCacheMemoizer(this, evaluator)

/**
 * suspend 함수를 JCache 기반 memoizer로 감쌉니다.
 *
 * ```kotlin
 * val cachingProvider = Caching.getCachingProvider()
 * val cacheManager = cachingProvider.cacheManager
 * val cache = cacheManager.getCache<String, Int>("myCache")
 * val memo = (suspend { key: String -> key.length }).withSuspendMemoizer(cache)
 * val result = memo("hello")
 * // result == 5
 * ```
 */
fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemoizer(
    jcache: javax.cache.Cache<T, R>,
): SuspendJCacheMemoizer<T, R> =
    SuspendJCacheMemoizer(jcache, this)

/**
 * [javax.cache.Cache]를 저장소로 사용하는 [SuspendMemoizer] 구현체입니다.
 *
 * ```kotlin
 * val cachingProvider = Caching.getCachingProvider()
 * val cacheManager = cachingProvider.cacheManager
 * val cache = cacheManager.getCache<String, Int>("myCache")
 * val memo = SuspendJCacheMemoizer(cache) { key -> key.length }
 * val result = memo("hello")
 * // result == 5
 * ```
 */
class SuspendJCacheMemoizer<T: Any, R: Any>(
    private val jcache: javax.cache.Cache<T, R>,
    private val evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val mutex = Mutex()

    override suspend fun invoke(input: T): R {
        return jcache.getOrPut(input) { evaluator(input) }
    }

    override suspend fun clear() {
        mutex.withLock {
            jcache.clear()
        }
    }
}
