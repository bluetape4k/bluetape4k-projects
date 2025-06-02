package io.bluetape4k.cache.memorizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


fun <T: Any, R: Any> Cache<T, R>.suspendMemorizer(
    evaluator: suspend (T) -> R,
): SuspendCaffeineMemorizer<T, R> =
    SuspendCaffeineMemorizer(this, evaluator)

fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemorizer(cache: Cache<T, R>): SuspendCaffeineMemorizer<T, R> =
    SuspendCaffeineMemorizer(cache, this)

class SuspendCaffeineMemorizer<T: Any, R: Any>(
    private val cache: Cache<T, R>,
    private val evaluator: suspend (T) -> R,
): SuspendMemorizer<T, R> {

    companion object: KLoggingChannel()

    private val mutex = Mutex()

    override suspend fun invoke(input: T): R {
        return cache.getIfPresent(input)
            ?: run {
                val result = evaluator(input)
                cache.put(input, result)
                result
            }
    }

    override suspend fun clear() {
        mutex.withLock {
            cache.cleanUp()
        }
    }
}
