package io.bluetape4k.cache.memorizer.cache2k

import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun <T: Any, R: Any> org.cache2k.Cache<T, R>.suspendMemorizer(
    @BuilderInference evaluator: suspend (T) -> R,
): SuspendMemorizer<T, R> =
    SuspendCache2kMemorizer(this, evaluator)

fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemorizer(
    cache: org.cache2k.Cache<T, R>,
): SuspendMemorizer<T, R> =
    SuspendCache2kMemorizer(cache, this)

class SuspendCache2kMemorizer<in T: Any, out R: Any>(
    private val cache: org.cache2k.Cache<T, R>,
    @BuilderInference private val evaluator: suspend (T) -> R,
): SuspendMemorizer<T, R> {

    companion object: KLoggingChannel()

    private val mutex = Mutex()

    override suspend fun invoke(input: T): R {
        return cache.get(input) ?: run {
            val result = evaluator(input)
            this@SuspendCache2kMemorizer.cache.put(input, result)
            result
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
}
