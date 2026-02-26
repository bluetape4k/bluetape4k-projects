package io.bluetape4k.cache.memorizer.jcache

import io.bluetape4k.cache.jcache.getOrPut
import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun <T: Any, R: Any> javax.cache.Cache<T, R>.memorizer(
    evaluator: suspend (T) -> R,
): SuspendJCacheMemorizer<T, R> =
    SuspendJCacheMemorizer(this, evaluator)

fun <T: Any, R: Any> (suspend (T) -> R).withMemorizer(
    jcache: javax.cache.Cache<T, R>,
): SuspendJCacheMemorizer<T, R> =
    SuspendJCacheMemorizer(jcache, this)

class SuspendJCacheMemorizer<T: Any, R: Any>(
    private val jcache: javax.cache.Cache<T, R>,
    private val evaluator: suspend (T) -> R,
): SuspendMemorizer<T, R> {

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
