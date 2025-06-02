package io.bluetape4k.cache.memorizer.ehcache

import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun <T: Any, R: Any> org.ehcache.Cache<T, R>.suspendMemorizer(
    evaluator: suspend (T) -> R,
): SuspendEhCacheMemorizer<T, R> =
    SuspendEhCacheMemorizer(this, evaluator)

fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemorizer(
    cache: org.ehcache.Cache<T, R>,
): SuspendEhCacheMemorizer<T, R> =
    SuspendEhCacheMemorizer(cache, this)

class SuspendEhCacheMemorizer<T: Any, R: Any>(
    private val cache: org.ehcache.Cache<T, R>,
    private val evaluator: suspend (T) -> R,
): SuspendMemorizer<T, R> {

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
